#!/usr/bin/env bash
# 安装本仓库的 git 钩子（规范见 ai-rules/05-git-commit.md）
#
#   pre-commit  跑三道闸门（只查本次改动的文件）
#   commit-msg  校验提交信息格式
#
# 用法： bash tools/install-git-hooks.sh
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOOKS="$ROOT/.git/hooks"

if [ ! -d "$HOOKS" ]; then
  echo "找不到 $HOOKS —— 请在仓库根目录（含 .git）里执行" >&2
  exit 1
fi

# ---------- pre-commit ----------
cat > "$HOOKS/pre-commit" <<'EOF'
#!/bin/sh
# 只查本次改动的文件（ai-rules/03 §7.1 + ai-rules/05 §5）
cd "$(git rev-parse --show-toplevel)" || exit 1

python3 tools/readability_check.py --changed-only HEAD --fail-on error || {
  echo ""
  echo "提交被拦截：可读性闸门有 error。修完再提交（紧急绕过：git commit --no-verify，并在 PR 里说明）"
  exit 1
}

python3 tools/structure_check.py --changed-only HEAD || {
  echo ""
  echo "提交被拦截：页面结构契约不通过（文件落位 / import 目标 / 统一 hooks / 类型位置 / 权限点位）"
  exit 1
}

# 只有动了 SQL / 建表脚本时才查表结构，避免每次提交都连库
if git diff --cached --name-only | grep -qE '\.sql$|script/sql/'; then
  python3 tools/check_db_schema.py || {
    echo ""
    echo "提交被拦截：数据库表规范体检有 error（缺 tenant_id / 缺索引 / 主键没自增 / 排序规则不一致）"
    exit 1
  }
fi
EOF

# ---------- commit-msg ----------
cat > "$HOOKS/commit-msg" <<'EOF'
#!/bin/sh
# 校验提交信息格式（ai-rules/05-git-commit.md）
cd "$(git rev-parse --show-toplevel)" || exit 1
python3 tools/check_commit.py --file "$1" || {
  echo ""
  echo "提交被拦截：提交信息不符规范。规则见 python3 tools/check_commit.py --rules"
  exit 1
}
EOF

chmod +x "$HOOKS/pre-commit" "$HOOKS/commit-msg"

echo "已安装 git 钩子："
echo "  .git/hooks/pre-commit   （readability / structure / db 三道闸门，按改动范围）"
echo "  .git/hooks/commit-msg   （提交信息格式校验）"
echo ""
echo "自测："
echo "  python3 tools/check_commit.py --message 'feat(qc): 演示一条合规提交'"
echo "  python3 tools/check_commit.py --message 'update'          # 应当被拒绝"
