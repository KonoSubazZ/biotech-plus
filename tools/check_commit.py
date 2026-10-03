#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""check_commit.py — 提交信息规范闸门（规则见 ai-rules/05-git-commit.md）

用法：
  python3 tools/check_commit.py --message "feat(qc): 新增质控标准模块"   # 校验一条（commit-msg 钩子用）
  python3 tools/check_commit.py --file .git/COMMIT_EDITMSG              # 校验 message 文件（钩子亦可传文件）
  python3 tools/check_commit.py --latest 5                              # 校验最近 5 条
  python3 tools/check_commit.py --range origin/main..HEAD                # 校验一个区间
  python3 tools/check_commit.py --rules                                 # 打印规则清单

退出码：0 通过；1 有不合规。
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys

TYPES = ["feat", "fix", "refactor", "perf", "style", "docs", "test", "build", "ci", "chore", "revert"]
SCOPES = ["backend", "frontend", "db", "system", "qc", "project", "report", "tools", "rules", "deps"]
# subject 里出现这些（且没有别的信息量）就等于没写
BAD_SUBJECTS = {"update", "updates", "fix", "fixes", "fix bug", "bugfix", "修改", "更新", "提交", "wip", "test", "临时"}

HEADER_RE = re.compile(r"^(?P<type>[a-z]+)(?:\((?P<scope>[a-z0-9_\-]+)\))?(?P<breaking>!)?: (?P<subject>\S.*)$")
# 这些系统生成/特殊提交直接放行
SKIP_RE = re.compile(r"^(Merge |Revert |fixup!|squash!|Initial commit|init: init)")

RULES_TEXT = """提交信息规范（依据 ai-rules/05-git-commit.md）

格式： <type>(<scope>): <中文简述>
  · 首行 ≤ 50 字（含 type/scope 最多 72），中文，动宾短语，不加句号
  · type 必填且在白名单：feat fix refactor perf style docs test build ci chore revert
  · scope 必填且在白名单：backend frontend db system qc project report tools rules deps
    （一个 commit 只用一个 scope；跨模块请拆成多个 commit）
  · subject 要有信息量，禁止 update / fix / 修改 / 提交 / WIP 这类空话
  · 破坏性改动：type(scope)!: ...，并在 body 写 BREAKING CHANGE:

提交前四步（缺一不算完成）：
  1. 跑闸门  readability_check / structure_check / check_db_schema
  2. 看 diff  git diff --cached（确认没夹带调试残留、密钥、生成物）
  3. 分组    git add <具体文件>（不要 git add .）
  4. 自检    python3 tools/check_commit.py --message "..."

装钩子： bash tools/install-git-hooks.sh
"""


def check_message(msg: str) -> list[str]:
    """返回问题清单；空列表 = 合规"""
    text = msg.strip()
    if not text:
        return ["提交信息为空"]

    lines = text.splitlines()
    header = lines[0].strip()

    if SKIP_RE.match(header):
        return []

    m = HEADER_RE.match(header)
    if not m:
        return [
            f"首行格式不符：{header[:60]!r}",
            "  应为 `<type>(<scope>): <中文简述>`，例如 feat(qc): 新增质控标准模块",
        ]

    errs: list[str] = []
    typ = m.group("type")
    scope = m.group("scope")
    subject = m.group("subject").strip()

    if typ not in TYPES:
        errs.append(f"type {typ!r} 不在白名单：{' '.join(TYPES)}")
    if not scope:
        errs.append("缺少 scope（一个 commit 归属哪个模块：backend/frontend/db/system/qc/project/tools/rules/deps）")
    elif scope not in SCOPES:
        errs.append(f"scope {scope!r} 不在白名单：{' '.join(SCOPES)}（跨模块请拆成多个 commit）")
    if len(header) > 72:
        errs.append(f"首行 {len(header)} 字符，太长了（subject 控制在 50 字内）")
    if len(subject) < 4:
        errs.append(f"subject 只有 {len(subject)} 个字，写清「改了什么」")
    if subject.rstrip().endswith("。"):
        errs.append("subject 结尾不要加句号")
    if subject.strip().lower() in BAD_SUBJECTS:
        errs.append(f"subject {subject!r} 没有信息量（禁止 update / fix / 修改 / 提交 / WIP 这类写法）")
    if m.group("breaking") and "BREAKING CHANGE" not in text:
        errs.append("用了 `!` 标破坏性改动，body 里请补一行 BREAKING CHANGE: ...")

    return errs


def git(*args: str) -> str:
    return subprocess.run(["git", *args], capture_output=True, text=True, cwd=_cwd()).stdout.strip()


def _cwd() -> str | None:
    import os

    cur = os.path.dirname(os.path.abspath(__file__))
    while cur != "/":
        if os.path.isdir(os.path.join(cur, ".git")):
            return cur
        cur = os.path.dirname(cur)
    return None


def main() -> int:
    ap = argparse.ArgumentParser(description="提交信息规范闸门")
    ap.add_argument("--message", help="直接校验一条提交信息")
    ap.add_argument("--file", help="校验一个提交信息文件（如 .git/COMMIT_EDITMSG）")
    ap.add_argument("--latest", type=int, help="校验最近 N 条提交")
    ap.add_argument("--range", dest="rev_range", help="校验一个区间，如 origin/main..HEAD")
    ap.add_argument("--rules", action="store_true", help="打印规则清单")
    args = ap.parse_args()

    if args.rules:
        print(RULES_TEXT)
        return 0

    if args.message is not None:
        items = [("<命令行传入>", args.message)]
    elif args.file:
        items = [(args.file, open(args.file, encoding="utf-8", errors="ignore").read())]
    elif args.latest or args.rev_range:
        # %x1f 分隔 hash 与正文，%x1e 分隔各条记录（否则正文里的换行会把记录切乱）
        rev = args.rev_range or f"-{args.latest}"
        out = git("log", rev, "--pretty=format:%h%x1f%B%x1e")
        items = []
        for rec in out.split("\x1e"):
            if not rec.strip():
                continue
            h, _, body = rec.partition("\x1f")
            items.append((h.strip(), body.strip()))
    else:
        print("用法见 --help，或 --rules 看规则清单", file=sys.stderr)
        return 2

    bad = 0
    print("=" * 72)
    print(f"提交信息规范检查：{len(items)} 条")
    print("=" * 72)
    for ref, msg in items:
        errs = check_message(msg)
        head = (msg.strip().splitlines() or [""])[0]
        if errs:
            bad += 1
            print(f"\n✗ {ref}  {head[:60]}")
            for e in errs:
                print(f"    - {e}")
        else:
            print(f"✓ {ref}  {head[:60]}")
    print(f"\n合计：{len(items)} 条，不合规 {bad} 条")
    if bad:
        print("规则：python3 tools/check_commit.py --rules")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
