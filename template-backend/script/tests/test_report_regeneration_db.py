"""在专用 MySQL 8 容器中验证真实迁移和 Mapper XML；不接触已有业务库。

预期：重复迁移成功；V1 重试不占号；成功发送后 V2；跨租户隔离；复审只分配原审核人。
运行：python script/tests/test_report_regeneration_db.py --keep-db
容器名来自 REPORT_TEST_MYSQL_CONTAINER，默认 codex-report-regeneration-mysql。
"""
import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor

ROOT = Path(__file__).resolve().parents[3]
DATABASE = "codex_report_regeneration_test"
CONTAINER = os.environ.get("REPORT_TEST_MYSQL_CONTAINER", "codex-report-regeneration-mysql")
ASSERTIONS = 0


def mysql_command():
    return ["docker", "exec", "-i", CONTAINER, "sh", "-c",
            'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; '
            'exec mysql -uroot --default-character-set=utf8mb4 --batch --skip-column-names --unbuffered']


def mysql(sql, database=True):
    command = mysql_command()
    prefix = "USE " + DATABASE + ";\n" if database else ""
    result = subprocess.run(command, input=prefix + sql, encoding="utf-8", capture_output=True)
    if result.returncode:
        raise AssertionError(result.stderr)
    return result.stdout.strip()


def check(condition, label):
    global ASSERTIONS
    assert condition, label
    ASSERTIONS += 1
    print("PASS:", label)


def create_statement(source, table):
    pattern = r"create table(?: if not exists)? `?" + re.escape(table)
    pattern += r"`?\s*\(.*?\)\s*engine[^;]*;"
    match = re.search(pattern, source, re.I | re.S)
    assert match, "missing source DDL: " + table
    return match.group(0)


def quote(value):
    if value is None:
        return "NULL"
    if isinstance(value, int):
        return str(value)
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def mapped(statement, values):
    mapper = ROOT / "template-backend/ruoyi-modules/ruoyi-report/src/main/resources/mapper/report"
    document = ET.parse(mapper / "ReportRevisionMapper.xml")
    node = document.find(".//*[@id='" + statement + "']")
    assert node is not None, statement
    sql = "".join(node.itertext())
    return re.sub(r"#\{([\w.]+)\}", lambda match: quote(values[match.group(1)]), sql)


def execute(statement, values):
    return mysql(mapped(statement, values))


def setup():
    check(mysql("SELECT VERSION();", False).startswith("8."), "MySQL 8 runtime")
    mysql("DROP DATABASE IF EXISTS " + DATABASE + "; CREATE DATABASE " + DATABASE
          + " CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;", False)
    business = (ROOT / "template-backend/script/sql/business/report-interpretation.sql").read_text("utf-8")
    for table in ("analysis_data", "analysis_report", "data_file_status", "analysis_report_email_log"):
        mysql(create_statement(business, table))
    updates = ROOT / "template-backend/script/sql/update"
    flow = (updates / "update_5.6.20-report-sample-flow.sql").read_text("utf-8")
    mysql(flow[:flow.index("-- 5. 病理师固定账号")])
    mysql((updates / "update_5.6.21-report-sign-start-time.sql").read_text("utf-8"))
    revision = next(updates.glob("update_*-report-regeneration.sql")).read_text("utf-8")
    mysql(revision)
    mysql(revision)
    check(True, "new migration is repeatable over actual report DDL")
    system = (ROOT / "template-backend/script/sql/ry_vue_5.X.sql").read_text("utf-8")
    for table in ("sys_user", "sys_role", "sys_menu", "sys_user_role", "sys_role_menu"):
        ddl = create_statement(system, table)
        mysql(ddl)
        if table == "sys_user":
            if "tenant_id" not in ddl:
                mysql("ALTER TABLE sys_user ADD tenant_id varchar(20) NOT NULL DEFAULT '000000';")
            mysql("ALTER TABLE sys_user MODIFY tenant_id varchar(20) NOT NULL DEFAULT '000000';")
            if "idx_tenant_id" not in ddl:
                mysql("ALTER TABLE sys_user ADD KEY idx_tenant_id(tenant_id);")
        if table == "sys_user_role" and "tenant_id" not in ddl:
            mysql("ALTER TABLE sys_user_role ADD tenant_id varchar(20) NOT NULL DEFAULT '000000', "
                  "ADD KEY idx_tenant_id(tenant_id);")


def versions():
    key = {"row.tenantId": "fixture-a", "row.client": "client", "row.subbarcode": "sample",
           "row.templateId": 3, "row.reportId": 7, "row.reportVersion": 1, "row.sequenceId": 1}
    mysql("INSERT INTO analysis_report (report_id, analysis_id, analysis_date, subbarcode, product, "
          "tenant_id, report_status, template_id, report_checked_by, report_signed_file_path, report_sign_flow_no) "
          "VALUES (7,1,'20261018','sample','product','fixture-a',52,3,2,'old-signed.pdf','old-contract');")
    execute("ensureSequence", key)
    execute("rememberBaseName", {"id": 1, "baseName": "client_sample_original"})
    execute("rememberBaseName", {"id": 1, "baseName": "client_sample_changed"})
    check(mysql("SELECT base_name FROM report_version_sequence WHERE id=1;") == "client_sample_original",
          "first base name is immutable across regenerations and later versions")
    execute("claimSequence", {"id": 1, "reportId": 7})
    execute("startGeneration", key)
    state = mysql("SELECT report_version, report_generation_no, report_reviewer_id, report_status, "
                  "report_signed_file_path IS NULL, report_sign_flow_no IS NULL FROM analysis_report WHERE report_id=7;")
    check(state == "1\t1\t2\t31\t1\t1", "sign failure regeneration clears old signature and keeps reviewer")
    execute("startGeneration", key)
    check(mysql("SELECT report_version, report_generation_no FROM analysis_report WHERE report_id=7;") == "1\t2",
          "regeneration increments attempt only, preserves V1")
    check(mysql("SELECT sent_version FROM report_version_sequence WHERE id=1;") == "0",
          "unsent and failed attempts do not consume a version")
    mysql("UPDATE analysis_report SET report_status=60 WHERE report_id=7;")
    mysql("START TRANSACTION; " + mapped("completeVersion", {"reportId": 7}) + "; "
          + mapped("markSent", {"reportId": 7, "userId": 2}) + "; COMMIT;")
    check(mysql("SELECT sent_version, active_report_id IS NULL FROM report_version_sequence WHERE id=1;") == "1\t1",
          "successful send consumes V1 and releases owner")
    check(mysql("SELECT report_status FROM analysis_report WHERE report_id=7;") == "62", "sent report is completed")
    check(execute("lockSequence", key).split("\t")[1] == "1", "next allocation reads last sent version for V2")
    other = dict(key)
    other["row.tenantId"] = "fixture-b"
    execute("ensureSequence", other)
    check(mysql("SELECT COUNT(*), SUM(sent_version) FROM report_version_sequence;") == "2\t1",
          "same sample and template are isolated by tenant")
    check(mysql("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='" + DATABASE
                + "' AND table_name='report_version_sequence' AND index_name='uk_report_version_sequence';") == "4",
          "unique sequence key covers tenant client sample template")


def reviewers():
    mysql("INSERT INTO sys_user(user_id,user_name,nick_name,status,tenant_id) VALUES "
          "(1,'fixture-platform','platform','0','000000'),(2,'fixture-reviewer','reviewer','0','fixture-a'),"
          "(3,'fixture-disabled','disabled','1','fixture-a'),(4,'fixture-other','other','0','fixture-b');")
    mysql("INSERT INTO sys_role(role_id,role_name,role_key,role_sort,status) "
          "VALUES(2,'fixture role','fixture-reviewer',1,'0');")
    mysql("INSERT INTO sys_menu(menu_id,menu_name,menu_type,perms) "
          "VALUES(2,'fixture review','F','report:interpretation:review');")
    mysql("INSERT INTO sys_user_role(user_id,role_id) VALUES(2,2),(3,2),(4,2); "
          "INSERT INTO sys_role_menu(role_id,menu_id) VALUES(2,2);")
    check(execute("countReviewer", {"userId": 2, "tenantId": "fixture-a"}) == "1", "normal reviewer status 0 accepted")
    check(execute("countReviewer", {"userId": 1, "tenantId": "fixture-a"}) == "1", "platform reviewer accepted")
    check(execute("countReviewer", {"userId": 4, "tenantId": "fixture-a"}) == "0", "other tenant reviewer rejected")
    check(execute("countReviewer", {"userId": 3, "tenantId": "fixture-a"}) == "0", "disabled reviewer rejected")
    mysql("UPDATE analysis_report SET report_status=32 WHERE report_id=7;")
    execute("submitReview", {"reportId": 7, "reviewerId": 2})
    check(mysql("SELECT report_status,report_review_claimed_by FROM analysis_report WHERE report_id=7;") == "41\t2",
          "resubmit goes directly to original reviewer queue")
    mysql("UPDATE analysis_report SET report_status=33 WHERE report_id=7;")
    execute("submitReview", {"reportId": 7, "reviewerId": 2})
    check(mysql("SELECT report_status FROM analysis_report WHERE report_id=7;") == "33", "failed generation cannot submit")


def concurrent_owners():
    key = {"row.tenantId": "fixture-a", "row.client": "client", "row.subbarcode": "sample", "row.templateId": 3}
    lock_sql = mapped("lockSequence", key)
    first = subprocess.Popen(mysql_command(), stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE, encoding="utf-8")
    try:
        first.stdin.write("USE " + DATABASE + "; START TRANSACTION; " + lock_sql + "; SELECT 'LOCKED';\n")
        first.stdin.flush()
        while True:
            line = first.stdout.readline().strip()
            if line == "LOCKED":
                break
            assert first.poll() is None, "first transaction did not acquire sequence lock"
        with ThreadPoolExecutor(max_workers=1) as executor:
            second = executor.submit(mysql, "START TRANSACTION; " + lock_sql + "; COMMIT;")
            first.stdin.write(mapped("claimSequence", {"id": 1, "reportId": 7}) + "; COMMIT;\n")
            first.stdin.flush()
            result = second.result(timeout=15)
            check(result.split("\t")[2] == "7", "concurrent allocator observes committed owner after row lock")
    finally:
        first.stdin.close()
        first.wait(timeout=10)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--keep-db", action="store_true")
    args = parser.parse_args()
    assert CONTAINER.startswith("codex-report-"), "only dedicated codex-report containers are allowed"
    try:
        setup()
        versions()
        reviewers()
        concurrent_owners()
        print("PASS: MySQL migration and Mapper regression, assertions=" + str(ASSERTIONS))
    finally:
        if not args.keep_db:
            mysql("DROP DATABASE IF EXISTS " + DATABASE + ";", False)


if __name__ == "__main__":
    main()
