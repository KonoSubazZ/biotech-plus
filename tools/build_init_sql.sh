#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
SQLDIR=$ROOT/template-backend/script/sql
DB=bp_init_build
OUT=$SQLDIR/init.sql
MYSQL="docker exec -i biotech-plus-mysql mysql -uroot -proot --default-character-set=utf8mb4"

echo "[1] 重建临时库 $DB"
$MYSQL -e "DROP DATABASE IF EXISTS \`$DB\`; CREATE DATABASE \`$DB\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

run_file() {
  local f="$1"
  if ! sed '/^USE `/d' "$f" | $MYSQL "$DB" 2>/tmp/init_err.log; then
    echo "FAILED: $f"
    tail -5 /tmp/init_err.log
    exit 1
  fi
  echo "  ok $(basename "$f")"
}

echo "[2] 基础表：结构 + 数据"
for f in "$SQLDIR/ry_vue_5.X.sql" \
         "$SQLDIR/update/update_5.6.2-rbac-only.sql" \
         "$SQLDIR/update/update_5.6.2-tenant-version.sql" \
         "$SQLDIR/update/update_5.6.2-menu-icons.sql"; do
  run_file "$f"
done

echo "[3] 记录基础表清单"
$MYSQL -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema='$DB' ORDER BY table_name" > /tmp/init_base_tables.txt
wc -l < /tmp/init_base_tables.txt

echo "[4] 业务表：结构（+菜单/字典等基础数据）"
# 业务脚本之间存在建表依赖（如 bioinfo-qc 依赖 qc_standard），按文件名顺序不一定能一次过；
# 多轮重试，直到没有进展为止（脚本本身都是幂等的 IF NOT EXISTS / NOT EXISTS）。
mapfile -t pending < <(ls "$SQLDIR"/business/*.sql | sort)
for pass in 1 2 3 4 5; do
  next=()
  for f in "${pending[@]}"; do
    # zz-migrated-data 是历史数据回填，init.sql 业务表只要结构，跳过
    if [ "$(basename "$f")" = "zz-migrated-data.sql" ]; then
      echo "  skip $(basename "$f") (data-only)"
      continue
    fi
    # report-interpretation 后半段是产品/模板种子（引用旧 product_config.code），
    # init.sql 不带业务数据：只保留建表/菜单部分，截到「种子：报告模板」之前
    if [ "$(basename "$f")" = "report-interpretation.sql" ]; then
      awk '/-- 种子：报告模板/{exit} {print}' "$f" | sed '/^USE `/d' | $MYSQL "$DB" 2>/tmp/init_err.log && { echo "  ok $(basename "$f") (structure/menu only, pass $pass)"; continue; }
    else
      sed '/^USE `/d' "$f" | $MYSQL "$DB" 2>/tmp/init_err.log && { echo "  ok $(basename "$f") (pass $pass)"; continue; }
    fi
    next+=("$f")
  done
  if [ ${#next[@]} -eq 0 ]; then break; fi
  if [ ${#next[@]} -eq ${#pending[@]} ]; then
    echo "NO PROGRESS at pass $pass; remaining: ${next[*]}"
    tail -5 /tmp/init_err.log
    exit 1
  fi
  pending=("${next[@]}")
done

echo "[5] 清空业务表数据（保留基础表数据）"
$MYSQL -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema='$DB' ORDER BY table_name" > /tmp/init_all_tables.txt
comm -13 /tmp/init_base_tables.txt /tmp/init_all_tables.txt > /tmp/init_business_tables.txt
echo "  业务表数: $(wc -l < /tmp/init_business_tables.txt)"
{
  echo "SET FOREIGN_KEY_CHECKS=0;"
  while read -r t; do [ -n "$t" ] && echo "TRUNCATE TABLE \`$t\`;"; done < /tmp/init_business_tables.txt
  echo "SET FOREIGN_KEY_CHECKS=1;"
} | $MYSQL "$DB"

echo "[6] 导出 init.sql"
docker exec biotech-plus-mysql mysqldump -uroot -proot --default-character-set=utf8mb4 \
  --single-transaction --routines --triggers --no-tablespaces --skip-add-locks --complete-insert \
  --add-drop-table "$DB" > /tmp/init_body.sql 2>/tmp/dump2.err
{
  echo "-- 数据库初始化脚本（由模板 base + business/*.sql 生成）"
  echo "-- 基础表（sys_* / 租户 / 权限 / 字典等）：结构 + 数据"
  echo "-- 业务表（样本 / 质控 / 报告 / 项目 / 合规 / 备份等）：仅结构，无业务数据"
  echo "-- 重新生成：bash tools/build_init_sql.sh（或见仓库说明）"
  echo
  echo "SET NAMES utf8mb4;"
  echo
  cat /tmp/init_body.sql
} > "$OUT"

echo "[7] 清理临时库"
$MYSQL -e "DROP DATABASE \`$DB\`;"
ls -lh "$OUT"
echo "[8] 校验"
grep -c '^CREATE TABLE' "$OUT" || true
