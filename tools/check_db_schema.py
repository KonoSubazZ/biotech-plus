#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""check_db_schema.py — 数据库表规范体检（一条命令出报告）

规则依据：ai-rules/04-db-schema.md（规则只写一处，本脚本是它的检查器）
可复制模板：ai-templates/db/table-template.sql

为什么需要：
  租户拦截器是**强制**的（TenantLineHandlerImpl 无条件注册，除白名单外所有表按 tenant_id 过滤），
  主键策略是**全局**的（idType=AUTO，主键列必须 AUTO_INCREMENT）。这两条一旦违反，
  不是"性能差一点"，而是模块直接报错（Unknown column / doesn't have a default value）。
  靠人记不如靠闸门。

用法（仓库根目录执行）：

  python3 tools/check_db_schema.py                       # 本机默认（docker 里的 biotech-plus-mysql）
  python3 tools/check_db_schema.py --database ry-vue
  python3 tools/check_db_schema.py --mysql-cmd "mysql -h127.0.0.1 -P3306 -uroot"   # 直连（密码从 MYSQL_PWD 环境变量读）
  python3 tools/check_db_schema.py --rules               # 打印规则清单
  python3 tools/check_db_schema.py --json db.json        # 机器可读

退出码：0 无 error；1 有 error（warn 不影响）。
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import subprocess
import sys

# ------------------------------------------------------------------ 约定常量
AUDIT_COLUMNS = ["create_by", "create_time", "update_by", "update_time"]
TENANT_COLUMN = "tenant_id"
TENANT_INDEX = "idx_tenant_id"
GOOD_COLLATION = "utf8mb4_general_ci"
SYSTEM_PREFIXES = ("sys_", "gen_", "sj_")   # 模板自带的系统表/生成器表/跳过的表
HANDLER_REL = os.path.join(
    "template-backend", "ruoyi-common", "ruoyi-common-mybatis", "src", "main", "java",
    "org", "dromara", "common", "mybatis", "handler", "TenantLineHandlerImpl.java")

RULES_TEXT = """数据库表规范体检规则（error 必须改；warn 需作者给出理由）
依据：ai-rules/04-db-schema.md   模板：ai-templates/db/table-template.sql

[error]
  bad-pk                   单列主键不是 bigint，或不是 AUTO_INCREMENT（全局 idType=AUTO，不自增会 INSERT 失败）
  missing-tenant-column    隔离表缺 tenant_id，或不是 varchar(20) NOT NULL DEFAULT '000000'
  missing-tenant-index     隔离表的 tenant_id 没有索引（或索引首列不是 tenant_id）
  bad-collation            排序规则不是 utf8mb4_general_ci（会与外键/关联表 JOIN 报 Illegal mix of collations）

[warn]
  shared-table-has-tenant  白名单（共享表）却带了 tenant_id —— 与 TenantLineHandlerImpl.SHARED_TABLES 冲突
  missing-audit-column     缺 create_by / create_time / update_by / update_time
  bad-index-name           索引/唯一键不是 idx_* / uk_* / PRIMARY（MySQL 自动命名的要改名）
  composite-pk-input       复合主键表 —— 实体必须 @TableId(type = IdType.INPUT)，否则插入报错

白名单（共享表，不做租户隔离）直接从 TenantLineHandlerImpl.java 的 SHARED_TABLES 解析，改代码即改规则。
"""


def find_root(start: str) -> str:
    cur = os.path.abspath(start)
    while cur != "/":
        if os.path.isdir(os.path.join(cur, "ai-templates")):
            return cur
        cur = os.path.dirname(cur)
    return os.getcwd()


def load_shared_tables(root: str) -> tuple[set[str], str]:
    """从 Java 源码解析 SHARED_TABLES —— 保证与运行期规则同源"""
    path = os.path.join(root, HANDLER_REL)
    if not os.path.isfile(path):
        return set(), f"未找到 {HANDLER_REL}（跳过白名单解析）"
    src = open(path, encoding="utf-8").read()
    m = re.search(r"SHARED_TABLES\s*=\s*Set\.of\((.*?)\)", src, re.S)
    if not m:
        return set(), "SHARED_TABLES 解析失败"
    tables = set(re.findall(r'"([a-z0-9_]+)"', m.group(1)))
    return tables, f"从 TenantLineHandlerImpl.java 解析到 {len(tables)} 张共享表"


def build_cmd(args) -> list[str]:
    if args.mysql_cmd:
        return shlex.split(args.mysql_cmd)
    # 注意 -i：SQL 走 stdin 传进去，少了它 docker exec 不接 stdin（会静默返回空结果）
    return ["docker", "exec", "-i", "-e", f"MYSQL_PWD={args.password}", args.container,
            "mysql", f"-u{args.user}", "-N", "-B", "--default-character-set=utf8mb4", args.database]


def query(cmd: list[str], sql: str) -> list[list[str]]:
    """SQL 走 stdin，避开 shell 对反引号的解释（踩过这个坑）"""
    p = subprocess.run(cmd, input=sql, capture_output=True, text=True, timeout=120)
    if p.returncode != 0:
        raise SystemExit(f"查询失败（{p.returncode}）：{p.stderr.strip()[:300]}")
    return [line.split("\t") for line in p.stdout.splitlines() if line.strip()]


class Finding:
    def __init__(self, level, rule, table, message):
        self.level, self.rule, self.table, self.message = level, rule, table, message

    def as_dict(self):
        return {"level": self.level, "rule": self.rule, "table": self.table, "message": self.message}


def main() -> int:
    ap = argparse.ArgumentParser(description="数据库表规范体检")
    ap.add_argument("--root", default=None, help="仓库根目录")
    ap.add_argument("--database", default="ry-vue")
    ap.add_argument("--container", default="biotech-plus-mysql")
    ap.add_argument("--user", default="root")
    ap.add_argument("--password", default="root")
    ap.add_argument("--mysql-cmd", default=None, help="自定义 mysql 命令（覆盖默认的 docker exec 方式）")
    ap.add_argument("--rules", action="store_true")
    ap.add_argument("--json", dest="json_out", nargs="?", const="-")
    args = ap.parse_args()

    if args.rules:
        print(RULES_TEXT)
        return 0

    root = args.root or find_root(os.path.dirname(os.path.abspath(__file__)))
    shared, note = load_shared_tables(root)
    cmd = build_cmd(args)
    db = args.database

    tables = query(cmd, f"""
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION FROM information_schema.TABLES
WHERE TABLE_SCHEMA = '{db}' AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME;""")
    cols = query(cmd, f"""
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, IFNULL(COLUMN_DEFAULT,'~NULL~'), EXTRA
FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '{db}';""")
    idx = query(cmd, f"""
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, NON_UNIQUE
FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = '{db}';""")

    col_by_table: dict[str, dict[str, dict]] = {}
    for t, c, ctype, nullable, default, extra in cols:
        col_by_table.setdefault(t, {})[c] = {
            "type": ctype.lower(), "nullable": nullable, "default": default, "extra": extra.lower()}

    idx_by_table: dict[str, list[tuple[str, str, int, str]]] = {}
    for t, name, col, seq, nonuniq in idx:
        idx_by_table.setdefault(t, []).append((name, col, int(seq), nonuniq))

    findings: list[Finding] = []
    stats = {"tables": len(tables), "shared": 0, "isolated": 0}

    for tname, engine, collation in tables:
        tcols = col_by_table.get(tname, {})
        tidx = idx_by_table.get(tname, [])
        is_shared = tname in shared or tname.startswith("sj_")
        stats["shared" if is_shared else "isolated"] += 1

        # 主键
        pk = sorted([(seq, c) for n, c, seq, _ in tidx if n == "PRIMARY"])
        pk_cols = [c for _, c in pk]
        if len(pk_cols) == 1:
            c = pk_cols[0]
            info = tcols.get(c, {})
            if "bigint" not in info.get("type", "") or "auto_increment" not in info.get("extra", ""):
                findings.append(Finding(
                    "error", "bad-pk", tname,
                    f"单列主键 {c} 应为 bigint + AUTO_INCREMENT（现：{info.get('type')} / {info.get('extra') or '无自增'}）"))
        elif len(pk_cols) > 1:
            findings.append(Finding(
                "warn", "composite-pk-input", tname,
                f"复合主键 ({', '.join(pk_cols)}) —— 实体必须 @TableId(type = IdType.INPUT)"))

        # tenant_id：隔离表必须有；共享表不该有（除非它自己就是租户目录，tenant_id 是主键）
        tinfo = tcols.get(TENANT_COLUMN)
        if not is_shared:
            if not tinfo:
                findings.append(Finding("error", "missing-tenant-column", tname,
                                        "隔离表缺 tenant_id —— 拦截器会让读写直接抛 Unknown column 'tenant_id'"))
            else:
                bad = []
                if tinfo["type"] != "varchar(20)":
                    bad.append(f"类型是 {tinfo['type']}，应为 varchar(20)")
                if tinfo["nullable"] != "NO":
                    bad.append("应为 NOT NULL")
                if tinfo["default"] != "000000":
                    bad.append(f"默认值应为 '000000'（现 {tinfo['default']}）")
                if bad:
                    findings.append(Finding("error", "missing-tenant-column", tname,
                                            "tenant_id 定义不符：" + "；".join(bad)))
                first_col_of_some_index = any(c == TENANT_COLUMN and seq == 1 for _, c, seq, _ in tidx)
                has_named = any(n == TENANT_INDEX for n, _, _, _ in tidx)
                if not first_col_of_some_index:
                    findings.append(Finding("error", "missing-tenant-index", tname,
                                            f"tenant_id 没有索引 —— 需 KEY {TENANT_INDEX} (tenant_id)"))
                elif not has_named:
                    findings.append(Finding("warn", "bad-index-name", tname,
                                            f"tenant_id 的索引不叫 {TENANT_INDEX}，建议按约定改名"))
        else:
            # 白名单表带 tenant_id 仅当它是「标识列」时才合理（如 sys_tenant.tenant_id 是它的唯一标识）；
            # 否则说明这张表其实需要隔离，却被塞进了白名单。
            if tinfo:
                is_identity = (TENANT_COLUMN in pk_cols) or any(
                    n != "PRIMARY" and c == TENANT_COLUMN and seq == 1 and nonuniq == "0"
                    for n, c, seq, nonuniq in tidx)
                if not is_identity:
                    findings.append(Finding("warn", "shared-table-has-tenant", tname,
                                            "共享表（白名单）带 tenant_id 但不是标识列 —— 与 "
                                            "TenantLineHandlerImpl.SHARED_TABLES 语义冲突"))

        # 审计字段：只要求业务表。模板自带的 sys_/gen_ 系统表（登录日志、关系表等）历史上就没有，不该报。
        is_business = (not is_shared) and (not tname.startswith(SYSTEM_PREFIXES))
        missing = [c for c in AUDIT_COLUMNS if c not in tcols]
        if is_business and missing:
            findings.append(Finding("warn", "missing-audit-column", tname,
                                    f"业务表缺审计字段：{', '.join(missing)}"))

        # 排序规则
        if collation and collation.lower() != GOOD_COLLATION:
            findings.append(Finding("error", "bad-collation", tname,
                                    f"排序规则 {collation}，应为 {GOOD_COLLATION}（否则 JOIN 报 Illegal mix of collations）"))

        # 索引命名（MySQL 自动命名 = 名字等于列名，或 列名_2 这种）
        for name, col, seq, nonuniq in tidx:
            if name == "PRIMARY":
                continue
            auto_named = (name == col) or re.fullmatch(rf"{re.escape(col)}_\d+", name or "")
            if auto_named:
                findings.append(Finding("warn", "bad-index-name", tname,
                                        f"索引 {name} 是 MySQL 自动命名，应改成 "
                                        f"{'uk_' if nonuniq == '0' else 'idx_'}{tname.replace('-', '_')}_{col} 之类的语义名"))

    errors = [f for f in findings if f.level == "error"]
    warns = [f for f in findings if f.level == "warn"]

    if args.json_out is not None:
        payload = {"database": db, "note": note, "stats": stats,
                   "errors": len(errors), "warns": len(warns),
                   "findings": [f.as_dict() for f in findings]}
        text = json.dumps(payload, ensure_ascii=False, indent=2)
        if args.json_out == "-":
            print(text)
        else:
            open(args.json_out, "w", encoding="utf-8").write(text)
            print(f"已写入 {args.json_out}：error {len(errors)} / warn {len(warns)}")
        return 1 if errors else 0

    print("=" * 74)
    print(f"数据库表规范体检：{db}（{stats['tables']} 张表：隔离 {stats['isolated']} / 共享 {stats['shared']}）")
    print(f"规则来源：ai-rules/04-db-schema.md   {note}")
    print("=" * 74)
    if not findings:
        print("未发现问题 ✅")
    for level, group in (("error", errors), ("warn", warns)):
        if group:
            print(f"\n[{level}] {len(group)} 条")
            for f in group:
                print(f"  {f.table:<20} {f.rule:<24} {f.message}")
    print(f"\n合计：error {len(errors)} / warn {len(warns)}")
    if errors:
        print("规则清单：python3 tools/check_db_schema.py --rules")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
