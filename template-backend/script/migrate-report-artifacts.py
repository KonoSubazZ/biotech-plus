#!/usr/bin/env python3
"""制品根迁移：/home/liushangzhi/docker/biotech-plus/artifacts  ->  /home/liushangzhi/project/reports

背景：容器制品挂点从 /tmp/report-artifacts 改成 /app/reports，库里老记录的路径前缀必须一起改写，
否则新 artifactRoot 下越界校验会拒读。本脚本：
  ① 把库里登记过的老文件按新结构归位（<type>/{docx,pdf,signed}/<日期>/ 或 <type>/json/）；
  ② 把两张表里登记的制品路径前缀 /tmp/report-artifacts/ 改成 /app/reports/。

用法（本机/宿主）：
    python3 template-backend/script/migrate-report-artifacts.py            # 干跑：只打印计划
    python3 template-backend/script/migrate-report-artifacts.py --apply    # 执行：复制文件 + 改库
"""
import argparse
import pathlib
import re
import subprocess
import sys
import time

OLD_ROOT = "/home/liushangzhi/docker/biotech-plus/artifacts"
NEW_ROOT = "/home/liushangzhi/project/reports"
OLD_PREFIX = "/tmp/report-artifacts/"
NEW_PREFIX = "/app/reports/"

# 查询用（带 -N -B，便于按行解析）
MYSQL_QUERY = ["docker", "exec", "-i", "biotech-plus-mysql", "mysql",
               "-uroot", "-proot", "--default-character-set=utf8mb4", "-N", "-B", "ry-vue"]
# 执行 SQL 走 stdin（不加 -e），库名是最后一个参数
MYSQL_STDIN = ["docker", "exec", "-i", "biotech-plus-mysql", "mysql",
               "-uroot", "-proot", "--default-character-set=utf8mb4", "ry-vue"]

# 库里登记制品路径的列
COLUMNS = {
    "analysis_report": ["report_json_path", "report_file_raw_path",
                        "report_file_path", "report_signed_file_path"],
    "qc_failed_notify": ["json_path", "raw_docx_path", "pdf_path"],
}

# 文件名里的 yyyyMMddHHmmss（取最后一个 14 位数字段，避开样本号里的短数字）
TS = re.compile(r"(\d{14})")


def run_sql(query: str) -> str:
    out = subprocess.run(MYSQL_QUERY + ["-e", query], capture_output=True, check=True)
    return out.stdout.decode("utf-8", "replace")


def existing_columns(table: str) -> set:
    q = ("SELECT COLUMN_NAME FROM information_schema.COLUMNS "
         f"WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '{table}'")
    return {line.strip() for line in run_sql(q).splitlines() if line.strip()}


def collect_registered_paths() -> list:
    paths = set()
    for table, cols in COLUMNS.items():
        have = existing_columns(table)
        for col in cols:
            if col not in have:
                continue  # 列还没建（如 update_5.6.17 未执行）→ 跳过
            q = f"SELECT `{col}` FROM `{table}` WHERE `{col}` IS NOT NULL AND `{col}` <> ''"
            paths.update(line.strip() for line in run_sql(q).splitlines() if line.strip())
    return sorted(paths)


def find_old_file(name: str):
    """老根是平铺 + 历史 3/ 子目录（与库路径错位），按文件名递归找"""
    for p in pathlib.Path(OLD_ROOT).rglob(name):
        if p.is_file():
            return p
    return None


def target_rel_path(rel: str, mtime: float) -> str:
    """老相对路径（平铺文件名）-> 新结构下的相对路径"""
    name = pathlib.Path(rel).name
    doc_type = "notify" if name.startswith("qc-failed-notify-") else "report"
    ext = pathlib.Path(name).suffix.lower().lstrip(".")
    if ext == "json":
        return f"{doc_type}/json/{name}"
    stage = ext  # docx / pdf
    stamps = TS.findall(pathlib.Path(name).stem)
    date = stamps[-1][:8] if stamps else time.strftime("%Y%m%d", time.localtime(mtime))
    return f"{doc_type}/{stage}/{date}/{name}"


def copy_as_root(moves) -> None:
    """老文件可能是 root:root 0600（宿主用户读不了）：用 root 容器（同时挂老根与新根）完成复制并放开读权限。"""
    lines = ["set -e"]
    for src, dst, _old, _new in moves:
        src_rel = pathlib.Path(src).relative_to(OLD_ROOT)
        dst_rel = pathlib.Path(dst).relative_to(NEW_ROOT)
        lines.append(f'mkdir -p "/new/{dst_rel.parent}"')
        lines.append(f'cp -n "/old/{src_rel}" "/new/{dst_rel}"')
        lines.append(f'chmod a+r "/new/{dst_rel}"')
    script_path = "/tmp/migrate-report-artifacts-cp.sh"
    pathlib.Path(script_path).write_text("\n".join(lines) + "\n", encoding="utf-8")
    subprocess.run(["docker", "run", "--rm",
                    "-v", f"{OLD_ROOT}:/old:ro",
                    "-v", f"{NEW_ROOT}:/new",
                    "-v", f"{script_path}:/migrate-cp.sh:ro",
                    "--entrypoint", "bash",
                    "maven:3.9-eclipse-temurin-17", "/migrate-cp.sh"], check=True)


def esc(value: str) -> str:
    return value.replace("'", "''")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true", help="真正执行（默认只干跑）")
    args = ap.parse_args()

    registered = collect_registered_paths()
    if not registered:
        print("库里没有登记的制品路径，无需迁移。")
        return 0

    moves = []   # (src, dst, old_path, new_path)
    skipped = 0
    for old_path in registered:
        if not old_path.startswith(OLD_PREFIX):
            skipped += 1
            continue  # 已是新前缀（重跑）或非本根，跳过
        rel = old_path[len(OLD_PREFIX):]
        src = find_old_file(pathlib.Path(rel).name)
        if src is None:
            print(f"[缺源文件] {old_path}")
            continue
        new_rel = target_rel_path(rel, src.stat().st_mtime)
        moves.append((src, pathlib.Path(NEW_ROOT) / new_rel, old_path, NEW_PREFIX + new_rel))

    print(f"登记路径 {len(registered)} 条；待迁移 {len(moves)} 条；跳过（已是新前缀/非本根）{skipped} 条")
    for src, dst, old_path, new_path in moves:
        print(f"  {src}\n    -> {dst}")
    if not args.apply:
        print("\n干跑结束。加 --apply 执行。")
        return 0

    copy_as_root(moves)

    # 改库：老路径 -> 新路径（每列一条 UPDATE，逐条幂等）
    lines = ["SET NAMES utf8mb4;"]
    for _src, _dst, old_path, new_path in moves:
        for table, cols in COLUMNS.items():
            have = existing_columns(table)
            for col in cols:
                if col not in have:
                    continue
                lines.append(f"UPDATE `{table}` SET `{col}` = '{esc(new_path)}' "
                             f"WHERE `{col}` = '{esc(old_path)}';")
    sql_text = "\n".join(lines) + "\n"
    pathlib.Path("/tmp/migrate-report-artifacts.sql").write_text(sql_text, encoding="utf-8")
    subprocess.run(MYSQL_STDIN, input=sql_text.encode("utf-8"), check=True)
    print(f"已复制 {len(moves)} 个文件并改写库路径（SQL 存档 /tmp/migrate-report-artifacts.sql）。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
