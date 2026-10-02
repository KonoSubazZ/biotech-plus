#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""structure_check.py — 前端页面结构契约闸门

为什么需要这个：readability_check.py 管的是「代码写得好不好读」（行长/嵌套/any/console），
它**不检查页面结构**。于是多人（含多个 agent 会话）各自生成的页面，
只要没人违反通用规则，结构跑偏（少了 defineOptions、把类型写在组件里、
import 了不存在的 modules 组件、不用项目 hooks 自己造轮子）**一条都拦不住**。
本闸门把这些「结构契约」变成可机械判定的规则。

契约来源（规则只写一处，此处是它的可执行版本）：
  ai-templates/frontend-module/README.md   页面文件构成与目录落位
  ai-rules/03-collaboration.md             「一致性靠模板 + 机械闸门」
  template-frontend/DESIGN.md              UI 令牌（不新增页面级阴影/主题色）
  template-frontend/UX-CONTRACT.md         组件能力归属（NSelect / 表格 hooks / message）

用法（在仓库根目录执行）：

    python3 tools/structure_check.py                      # 全量扫描 template-frontend/src
    python3 tools/structure_check.py --changed-only origin/main   # 只查本次改动触及的页面
    python3 tools/structure_check.py --rules              # 打印规则清单
    python3 tools/structure_check.py --json out.json      # 机器可读输出
    python3 tools/structure_check.py path/to/views        # 只扫指定目录

退出码：0 无 error；1 有 error（warn 不影响退出码）。
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys

# ------------------------------------------------------------------ 判据常量
LIST_TABLE_RE = re.compile(r"<NDataTable\b")
# 注意：不能用 [^>]* 取标签内容 —— 属性值里就有 '>'（:row-key="row => row.id"），
# 会把标签截断在 remote 之前造成误报。改为「同一行内」或「独立成行」两个形态。
REMOTE_ATTR_RE = re.compile(r"<NDataTable\b[^\n]*\bremote\b|^\s*remote\s*$", re.MULTILINE)
TSX_SETUP_RE = re.compile(r"<script\s+setup\s+lang=[\"']tsx[\"']")
REL_IMPORT_RE = re.compile(r"from\s+[\"'](\.[^\"']+)[\"']")
INLINE_TYPE_RE = re.compile(r"\b(?:interface|type)\s+(\w+(?:List|SearchParams|Form|Vo|OperateParams))\b")
ALERT_RE = re.compile(r"(?<![\w.])alert\s*\(")
EXPORT_FN_RE = re.compile(r"export\s+(?:async\s+)?function\s+(\w+)")
HAS_AUTH_RE = re.compile(r"hasAuth\(\s*[\"']([\w:]+)[\"']")
SA_PERM_RE = re.compile(r"@SaCheckPermission\(\s*[\"']([\w:]+)[\"']")
HEX_COLOR_RE = re.compile(r"#[0-9a-fA-F]{3,8}\b")
BUSINESS_TYPE_SUFFIX = ("List", "SearchParams", "Form", "Vo", "OperateParams")
# 组件内允许就地声明的类型（工作服：Props/Emits/Model/RuleKey 之类）
LOCAL_TYPE_ALLOW = re.compile(r"(Props|Emits|Model|RuleKey|Rule|Columns|Options|Tabs|Status)$")

RULES_TEXT = """页面结构契约闸门规则清单（error 必须改；warn 需作者给出理由）
依据：ai-templates/frontend-module/README.md + ai-rules/03-collaboration.md + DESIGN.md/UX-CONTRACT.md

[页面结构 — error]
  vue-ref-missing        .vue 里相对 import 的目标文件不存在（含 ./modules/*.vue）
  list-no-tsx            含 NDataTable 的页面必须 <script setup lang="tsx">
  list-no-remote         NDataTable 必须带 remote（服务端分页）
  inline-business-type   .vue 里就地声明业务类型（应放 src/typings/api/<模块>.api.d.ts）
  alert-used             禁止 alert()
  direct-request-in-view views/ 下直接 import '@/service/request'（必须走 service/api 层）
  api-func-naming        service/api 的导出函数必须以 fetch 开头

[页面结构 — warn]
  list-no-define-options 有分页表格的页面应有 defineOptions({ name: 'XxxList' })（keep-alive 依赖）
  list-no-paginated-hook 有 remote 表格但未使用 useNaivePaginatedTable / useTableOperate
  page-hardcoded-style   页面内硬编码色值（DESIGN.md 要求用主题令牌，不新增页面级样式）
  perm-not-in-backend    前端 hasAuth('a:b:c') 在后端 @SaCheckPermission 里找不到

同时建议跑 readability_check.py（可读性）——两者互补，不是替代关系。
"""

WEB_EXT = {".vue"}
API_DIR = os.path.join("src", "service", "api")
VIEWS_DIR = os.path.join("src", "views")


class Finding:
    def __init__(self, path, line, level, rule, message):
        self.path, self.line, self.level, self.rule, self.message = path, line, level, rule, message

    def as_dict(self):
        return {"path": self.path, "line": self.line, "level": self.level,
                "rule": self.rule, "message": self.message}


def line_of(text, index):
    return text.count("\n", 0, index) + 1


def rel(path, root):
    try:
        return os.path.relpath(path, root)
    except ValueError:
        return path


# ------------------------------------------------------------------ 单文件检查
def check_vue(path, root, findings):
    src = open(path, encoding="utf-8").read()
    rp = rel(path, root)
    cur_dir = os.path.dirname(path)

    # 只把 src/views 下的业务页面当「页面」；components/ layouts/ _builtin/ 属框架自带，不套业务契约
    norm = path.replace("\\", "/")
    is_page = "/src/views/" in norm and "/_builtin/" not in norm

    # 1) 相对 import 必须存在（生成器最常踩：import 了没生成的 modules 组件）
    for m in REL_IMPORT_RE.finditer(src):
        spec = m.group(1)
        base = os.path.normpath(os.path.join(cur_dir, spec))
        candidates = [base, base + ".vue", base + ".ts", base + ".tsx", base + ".js",
                      os.path.join(base, "index.vue"), os.path.join(base, "index.ts")]
        if not any(os.path.isfile(c) for c in candidates):
            findings.append(Finding(rp, line_of(src, m.start()), "error", "vue-ref-missing",
                                    f"import 目标不存在：{spec}（同目录 modules/ 下缺该文件？）"))

    has_table = bool(LIST_TABLE_RE.search(src))
    has_hook = "useNaivePaginatedTable" in src
    is_list = is_page and has_table

    # 2) 列表页脚本语言：columns 里要写 JSX，必须 tsx
    if is_list and not TSX_SETUP_RE.search(src):
        findings.append(Finding(rp, 1, "error", "list-no-tsx",
                                '含 NDataTable 的页面必须 <script setup lang="tsx">'))

    # 3) 服务端分页：只有走统一 hooks 的列表页才要求 remote（树表等特例不要求）
    if is_list and has_hook and not REMOTE_ATTR_RE.search(src):
        findings.append(Finding(rp, 1, "error", "list-no-remote",
                                "NDataTable 缺少 remote（列表页一律服务端分页）"))

    # 4) 业务类型不许就地声明（要在 typings/api/<模块>.api.d.ts）
    for m in INLINE_TYPE_RE.finditer(src):
        name = m.group(1)
        if LOCAL_TYPE_ALLOW.search(name):
            continue
        findings.append(Finding(rp, line_of(src, m.start()), "error", "inline-business-type",
                                f"业务类型 {name} 应放 src/typings/api/<模块>.api.d.ts，不要写在组件里"))

    # 5) alert 与直接请求
    for m in ALERT_RE.finditer(src):
        findings.append(Finding(rp, line_of(src, m.start()), "error", "alert-used",
                                "禁止 alert()，提示统一用 window.$message"))
    if re.search(r"from\s+[\"']@/service/request[\"']", src) and f"{os.sep}views{os.sep}" in path:
        findings.append(Finding(rp, 1, "error", "direct-request-in-view",
                                "views/ 下不要直接 import request，接口统一放 src/service/api/"))

    # 6) warn：keep-alive 名 / 表格 hooks
    if is_list and has_hook and "defineOptions(" not in src:
        findings.append(Finding(rp, 1, "warn", "list-no-define-options",
                                "有分页表格但缺 defineOptions({ name: 'XxxList' })，keep-alive 会失效"))
    if is_list and not has_hook and os.path.basename(path) == "index.vue":
        findings.append(Finding(rp, 1, "warn", "list-no-paginated-hook",
                                "列表页未使用 useNaivePaginatedTable（树表等特例需在评审里说明理由）"))

    # 7) warn：页面级硬编码色值（DESIGN.md），只管业务页面
    style = re.search(r"<style[^>]*>(.*?)</style>", src, re.DOTALL)
    if is_page and style and (HEX_COLOR_RE.search(style.group(1)) or "box-shadow" in style.group(1)):
        findings.append(Finding(rp, line_of(src, style.start()), "warn", "page-hardcoded-style",
                                "页面内硬编码色值/阴影，DESIGN.md 要求复用主题令牌"))


def check_api(path, root, findings):
    src = open(path, encoding="utf-8").read()
    rp = rel(path, root)
    for m in EXPORT_FN_RE.finditer(src):
        name = m.group(1)
        if not name.startswith("fetch"):
            findings.append(Finding(rp, line_of(src, m.start()), "error", "api-func-naming",
                                    f"接口函数 {name} 应以 fetch 开头（fetchGetXxxList / fetchCreateXxx …）"))


def collect_has_auth(files):
    out = {}
    for path in files:
        if not path.endswith(".vue"):
            continue
        src = open(path, encoding="utf-8").read()
        for m in HAS_AUTH_RE.finditer(src):
            out.setdefault(m.group(1), path)
    return out


def collect_backend_perms(root):
    perms = set()
    be = os.path.join(root, "template-backend")
    for dirpath, _dirs, files in os.walk(be):
        if os.sep + "target" + os.sep in dirpath:
            continue
        for f in files:
            if f.endswith(".java"):
                src = open(os.path.join(dirpath, f), encoding="utf-8", errors="ignore").read()
                perms.update(SA_PERM_RE.findall(src))
    return perms


# ------------------------------------------------------------------ 入口
SKIP_PATH_PARTS = ("ai-templates", ".git", "node_modules", "dist", "target", "__pycache__")


def _is_skipped_path(path: str) -> bool:
    """模板骨架 / 依赖 / 构建产物不参与「业务页面契约」检查。

    ai-templates/ 里的页面是生成前的骨架：import 指向的是生成之后才存在的文件名，
    权限点位也是占位示例（如 biotech:productConfig:*）—— 拿业务契约去查它必然误报
    （实测：模板 index.vue 报 vue-ref-missing + perm-not-in-backend 共 5 条）。
    """
    norm = "/" + path.replace("\\", "/").strip("/") + "/"
    return any(f"/{part}/" in norm for part in SKIP_PATH_PARTS)


def gather(root, explicit):
    files = []
    if explicit:
        for p in explicit:
            if os.path.isfile(p):
                files.append(os.path.abspath(p))
            else:
                for dirpath, _d, fs in os.walk(p):
                    files += [os.path.join(dirpath, f) for f in fs if f.endswith(".vue")]
        return files
    src_root = os.path.join(root, "template-frontend", "src")
    for dirpath, _d, fs in os.walk(src_root):
        if "node_modules" in dirpath:
            continue
        for f in fs:
            full = os.path.join(dirpath, f)
            if os.path.splitext(f)[1] in WEB_EXT:
                files.append(full)
            elif f.endswith(".ts") and f"{os.sep}{API_DIR}{os.sep}" in full:
                files.append(full)
    return sorted(files)


def changed_only(root, ref):
    # 传 "A...B" 比两点之间的提交；传单个 ref（HEAD / origin/main）连带工作区未提交改动
    # —— pre-commit 用 --changed-only HEAD 就是「本次要提交的东西」。
    try:
        out = subprocess.run(["git", "-C", root, "diff", "--name-only", ref],
                             capture_output=True, text=True, check=True).stdout
    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        print(f"错误：--changed-only 取不到改动清单（{e}）", file=sys.stderr)
        return None
    return [os.path.join(root, l.strip()) for l in out.splitlines() if l.strip()]


def main():
    ap = argparse.ArgumentParser(description="前端页面结构契约闸门")
    ap.add_argument("path", nargs="*", help="只扫这些路径（默认 template-frontend/src）")
    ap.add_argument("--root", default=None, help="仓库根目录")
    ap.add_argument("--changed-only", metavar="REF", default=None, help="只查相对 REF 的改动文件")
    ap.add_argument("--rules", action="store_true", help="打印规则清单")
    ap.add_argument("--json", dest="json_out", nargs="?", const="-", help="JSON 输出（默认 stdout）")
    args = ap.parse_args()

    if args.rules:
        print(RULES_TEXT)
        return 0

    root = args.root
    if not root:
        cur = os.path.dirname(os.path.abspath(__file__))
        while cur != "/" and not os.path.isdir(os.path.join(cur, "ai-templates")):
            cur = os.path.dirname(cur)
        root = cur

    explicit = None
    if args.changed_only:
        changed = changed_only(root, args.changed_only)
        if changed is None:
            return 2
        explicit = [
            p for p in changed
            if (p.endswith(".vue") or f"{os.sep}{API_DIR}{os.sep}" in p) and not _is_skipped_path(p)
        ]
        if not explicit:
            print("--changed-only：本次改动没触及前端页面，跳过")
            return 0
    elif args.path:
        explicit = args.path

    files = gather(root, explicit)
    findings = []
    for path in files:
        if path.endswith(".vue"):
            check_vue(path, root, findings)
        elif f"{os.sep}{API_DIR}{os.sep}" in path:
            check_api(path, root, findings)

    # 跨端权限点位一致性（warn）：前端用了、后端 @SaCheckPermission 里没有
    has_auth = collect_has_auth(files)
    if has_auth:
        backend = collect_backend_perms(root)
        if backend:
            for perm, path in sorted(has_auth.items()):
                if perm not in backend:
                    findings.append(Finding(rel(path, root), 1, "warn", "perm-not-in-backend",
                                            f"前端权限点位 {perm} 在后端 @SaCheckPermission 里找不到"))

    errors = [f for f in findings if f.level == "error"]
    warns = [f for f in findings if f.level == "warn"]

    if args.json_out is not None:
        payload = {"root": root, "files": len(files),
                   "errors": len(errors), "warns": len(warns),
                   "findings": [f.as_dict() for f in findings]}
        text = json.dumps(payload, ensure_ascii=False, indent=2)
        if args.json_out == "-":
            print(text)
        else:
            open(args.json_out, "w", encoding="utf-8").write(text)
            print(f"已写入 {args.json_out}：error {len(errors)} / warn {len(warns)}")
        return 1 if errors else 0

    print("=" * 72)
    print(f"页面结构契约闸门：扫描 {len(files)} 个文件（{root}）")
    print("=" * 72)
    if not findings:
        print("未发现问题 ✅")
    for level, group in (("error", errors), ("warn", warns)):
        if not group:
            continue
        print(f"\n[{level}] {len(group)} 条")
        for f in group:
            print(f"  {f.path}:{f.line}  {f.rule}  {f.message}")
    print(f"\n合计：error {len(errors)} / warn {len(warns)}")
    if errors:
        print("error 必须修复；规则清单见 python3 tools/structure_check.py --rules")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
