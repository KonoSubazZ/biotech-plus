#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""readability_check.py — 可读性闸门（Java + Vue/TS）

统一规则 + 一条命令 + 检测报告。规则白皮书：ai-rules/01-readability.md

用法（在仓库根目录执行）：
    python3 tools/readability_check.py                       # 扫默认范围
    python3 tools/readability_check.py template-backend      # 扫指定目录
    python3 tools/readability_check.py --changed-only origin/main   # 只查改动（PR/CI）
    python3 tools/readability_check.py --json out.json       # 结果另存 JSON
    python3 tools/readability_check.py --fail-on warn        # 严格模式
    python3 tools/readability_check.py --rules               # 打印规则清单

退出码：0 通过；1 命中失败阈值；2 用法/环境错误

实现说明（重要，别把它当编译器）：
  · Java 没有内置 AST（不给第三方依赖），方法长度/嵌套深度用**花括号配对**估算，
    已做字符串与注释剥离。结果是启发式，偶有误报——报告里会给文件:行号供人工确认。
  · 只做机械可判定的规则；主观判断（命名好不好、抽象合不合理）交给评审，见
    ai-rules/02-review-contract.md。
"""

import argparse
import ast
import json
import os
import re
import subprocess
import sys
from collections import defaultdict

CONFIG = {
    "default_paths": ["template-backend", "template-frontend/src"],
    "max_file_lines": 500,
    "max_method_lines": 50,
    "max_block_depth": 3,     # 控制流嵌套（定义为相对方法体的花括号深度 - 1）
    "max_params": 6,
    "max_chain": 5,           # 单行方法调用链长度（启发式，只报明显过长的链）
    "max_line_len": 120,
    "max_any": 0,             # 显式 any 的次数上限（超过即报）
    "samples_per_rule": 5,
}

SKIP_DIRS = {".git", ".venv", "venv", "node_modules", "target", "dist", "build",
             "__pycache__", ".mypy_cache", ".pytest_cache", ".codegraph",
             ".ai_state", ".claude", ".codex", ".husky", "site-packages",
             # elegant-router 的生成物目录：routes.ts / imports.ts / transform.ts 都由插件生成、
             # 且一有变化就被整份覆盖，不套「人写的代码」的可读性规则
             "elegant"}

# 工具生成的声明文件（同样不是人写的，改了会丢）
SKIP_FILE_SUFFIXES = ("elegant-router.d.ts", "components.d.ts", "auto-imports.d.ts")

JAVA_EXT = {".java"}
WEB_EXT = {".ts", ".tsx", ".vue", ".js", ".jsx", ".mjs"}

_CRED_LABELS = ("password", "passwd", "pwd", "secret", "token",
                "api_key", "apikey", "access_key", "privatekey")
_CRED_ALT = "|".join(w.replace("_", "[_-]?") for w in _CRED_LABELS)
# 标签形如 password / api-key / accessKey=...（拆成两步匹配，避免正则本身写成一行赋值）
_CRED_LABEL_PAT = re.compile("(?i)\\b(?:" + _CRED_ALT + ")\\b")
# 必须紧跟标签：可选闭合引号 + : 或 = + 引号值。用 match(ln, pos) 保证相邻，
# 否则 'pwd-login': 'xxx' 这类对象键会被误判成 pwd = xxx
_CRED_VALUE_PAT = re.compile("[\"']?\\s*[:=]\\s*[\"']([^\"']{3,})[\"']")
IP_PAT = re.compile(r"[\"']\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(?::\d+)?[\"']")
TODO_PAT = re.compile(r"(?://|#|/\*)\s*(TODO|FIXME|XXX|HACK)\b")
# MyBatis 在 Java 里写 SQL —— 后端 Checkstyle 也拦，这里保留以形成单一闸门
SQL_ANNO_PAT = re.compile(r"@(Select|Insert|Update|Delete|SelectKey)\b|@\w*Provider\b")


class Finding:
    __slots__ = ("rule", "sev", "path", "line", "msg")

    def __init__(self, rule, sev, path, line, msg):
        self.rule, self.sev, self.path, self.line, self.msg = rule, sev, path, line, msg

    def as_dict(self):
        return {"rule": self.rule, "severity": self.sev, "file": self.path,
                "line": self.line, "msg": self.msg}


# ------------------------------------------------------------------ 文本预处理
def strip_code(text, lang):
    """剥离字符串、字符、注释，供花括号配对使用。保持行数不变（用空格替换）。"""
    out = []
    i, n = 0, len(text)
    in_str = None      # 当前字符串定界符
    in_block = False   # 多行注释
    while i < n:
        c = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if in_block:
            if c == "*" and nxt == "/":
                out.append("  "); i += 2; in_block = False; continue
            out.append("\n" if c == "\n" else " "); i += 1; continue
        if in_str:
            if c == "\\":
                out.append("  "); i += 2; continue
            if c == in_str:
                out.append(" "); i += 1; in_str = None; continue
            out.append("\n" if c == "\n" else " "); i += 1; continue
        if c == "/" and nxt == "/":
            while i < n and text[i] != "\n":
                out.append(" "); i += 1
            continue
        if c == "/" and nxt == "*":
            out.append("  "); i += 2; in_block = True; continue
        if c in "\"'`":
            in_str = c; out.append(" "); i += 1; continue
        if c == "'" and lang == "java":
            in_str = c; out.append(" "); i += 1; continue
        out.append(c); i += 1
    return "".join(out)


def line_starts(s):
    """返回每行在字符串中的起始偏移。"""
    starts, pos = [0], 0
    for ln in s.split("\n"):
        pos += len(ln) + 1
        starts.append(pos)
    return starts


def offset_to_line(starts, off):
    lo, hi = 0, len(starts) - 1
    while lo < hi:
        mid = (lo + hi + 1) // 2
        if starts[mid] <= off:
            lo = mid
        else:
            hi = mid - 1
    return lo + 1


# ------------------------------------------------------------------ 通用块分析
def analyze_blocks(clean, starts, sig_pat, path, out, cfg, label):
    """按签名正则找方法/函数体，用花括号配对算长度与嵌套深度。"""
    for m in sig_pat.finditer(clean):
        # 找签名后的第一个 '{'；注意签名本身可能已含 '{'（K&R 同行写法），故从 end-1 起搜
        brace = clean.find("{", m.end() - 1)
        if brace < 0:
            continue
        # 若中间出现 ';' 说明是抽象/接口声明，跳过
        if ";" in clean[m.end():brace]:
            continue
        depth, i, end = 1, brace + 1, -1
        max_depth = 1
        while i < len(clean):
            if clean[i] == "{":
                depth += 1
                max_depth = max(max_depth, depth)
            elif clean[i] == "}":
                depth -= 1
                if depth == 0:
                    end = i
                    break
            i += 1
        if end < 0:
            continue
        wl = offset_to_line(starts, m.start())
        wl_end = offset_to_line(starts, end)
        length = wl_end - wl + 1
        nesting = max_depth - 1

        if length > cfg["max_method_lines"]:
            out.append(Finding(f"{label}-method-too-long", "warn", path, wl,
                               f"{m.group('name')}() {length} 行 > {cfg['max_method_lines']}，"
                               f"拆成小方法"))
        if nesting > cfg["max_block_depth"]:
            out.append(Finding(f"{label}-too-nested", "error", path, wl,
                               f"{m.group('name')}() 嵌套 {nesting} 层 > {cfg['max_block_depth']}，"
                               f"用提前 return / continue 拉平"))

        sig = m.group(0)
        params = sig[sig.find("(") + 1:sig.rfind(")")]
        np = 0 if params.strip() in ("", "void") else params.count(",") + 1
        if np > cfg["max_params"]:
            out.append(Finding(f"{label}-too-many-params", "warn", path, wl,
                               f"{m.group('name')}() 有 {np} 个参数 > {cfg['max_params']}，"
                               f"改传一个显式的 BO/DTO"))


JAVA_SIG = re.compile(
    r"^[ \t]*(?:(?:public|protected|private|static|final|synchronized|abstract|default|native)\s+)*"
    r"(?:<[^>]+>\s*)?[\w<>\[\],.?\s]*?"
    r"\b(?!if\b|for\b|while\b|switch\b|catch\b|return\b|new\b|do\b|else\b|try\b|throw\b)"
    r"(?P<name>[A-Za-z]\w*)\s*\([^;{)]*\)\s*"
    r"(?:throws\s+[\w,.\s]+)?\{?\s*$", re.MULTILINE)

WEB_SIG = re.compile(
    r"^[ \t]*(?:export\s+)?(?:async\s+)?function\s+(?P<name>\w+)\s*\([^;{)]*\)\s*\{?"
    r"|^[ \t]*(?:const|let)\s+(?P<name2>\w+)\s*=\s*(?:async\s*)?\([^;{)]*\)\s*=>\s*\{?"
    r"|^[ \t]*(?:async\s+)?(?P<name3>\w+)\s*\([^;{)]*\)\s*(?::[^{;]*)?\{",
    re.MULTILINE)


def check_java(path, out):
    raw = open(path, "r", encoding="utf-8", errors="replace").read()
    lines = raw.split("\n")
    cfg = CONFIG

    if len(lines) > cfg["max_file_lines"]:
        out.append(Finding("java-file-too-long", "warn", path, 1,
                           f"文件 {len(lines)} 行 > {cfg['max_file_lines']}，按职责拆分"))

    clean = strip_code(raw, "java")
    starts = line_starts(clean)
    analyze_blocks(clean, starts, JAVA_SIG, path, out, cfg, "java")

    clines = clean.split("\n")
    for i, ln in enumerate(lines, 1):
        code = _code_line(clines, i)          # 剥离字符串/注释后的同一行
        s = code.strip()
        if len(ln) > cfg["max_line_len"]:
            out.append(Finding("line-too-long", "warn", path, i,
                               f"第 {i} 行 {len(ln)} 字符 > {cfg['max_line_len']}"))
        if re.match(r"\s*import\s+[\w.]*\.\*\s*;", code):
            out.append(Finding("wildcard-import", "warn", path, i,
                               "通配符导入：历史代码保留（见 backend-code-standard.md），"
                               "新代码请显式列出用到的类"))
        if "printStackTrace()" in code or re.search(r"System\.(out|err)\.print", code):
            out.append(Finding("print-debug", "warn", path, i,
                               "调试输出：用项目统一日志（SLF4J），并在日志中保留异常堆栈"))
        if SQL_ANNO_PAT.search(code):
            out.append(Finding("sql-in-java", "error", path, i,
                               "MyBatis SQL 注解：手写 SQL 放 Mapper XML"))
        if re.search(r"\breturn\s+null\s*;", code):
            out.append(Finding("return-null", "warn", path, i,
                               "返回 null：缺数据应抛明确异常或返回空集合；"
                               "确有必要请在此处注释说明"))
        if _ternary_count(code) >= 2:
            out.append(Finding("nested-ternary", "warn", path, i,
                               "嵌套三元表达式：拆成独立 if，或在分支内读取上下文"))
        chains = len(re.findall(r"\.\w+\s*\(", code))
        if chains > cfg["max_chain"] and not s.startswith(("import", "package", "@")):
            out.append(Finding("deep-chain", "warn", path, i,
                               f"第 {i} 行 {chains} 个方法调用串在一行（阈值 {cfg['max_chain']}），"
                               f"先声明 Wrapper 再分段设条件，或拆中间变量"))

    # 空 catch / 只注释的 catch
    for m in re.finditer(r"catch\s*\([^)]*\)\s*\{", clean):
        body_start = m.end()
        depth, i = 1, body_start
        while i < len(clean) and depth:
            if clean[i] == "{":
                depth += 1
            elif clean[i] == "}":
                depth -= 1
            i += 1
        body = clean[body_start:i - 1]
        if body.strip() == "":
            out.append(Finding("silent-catch", "error", path,
                               offset_to_line(starts, m.start()),
                               "catch 块为空：异常被静默吞掉，至少记日志或重新抛出"))
        elif len(body.strip()) < 40 and "throw" not in body and "log" not in body.lower():
            out.append(Finding("thin-catch", "warn", path,
                               offset_to_line(starts, m.start()),
                               "catch 块过薄：确认已记日志/转换异常类型，不要只留一句注释"))

    _common_checks(path, lines, out)


def check_web(path, out):
    raw = open(path, "r", encoding="utf-8", errors="replace").read()
    lines = raw.split("\n")
    cfg = CONFIG

    if len(lines) > cfg["max_file_lines"]:
        out.append(Finding("web-file-too-long", "warn", path, 1,
                           f"文件 {len(lines)} 行 > {cfg['max_file_lines']}，拆组件/拆模块"))

    clean = strip_code(raw, "js")
    starts = line_starts(clean)

    # function/const arrow/method 三种签名分开跑，避免正则互相干扰
    for pat in (WEB_SIG,):
        for m in pat.finditer(clean):
            name = m.group("name") or m.group("name2") or m.group("name3") or "?"
            brace = clean.find("{", m.end() - 1)
            if brace < 0:
                continue
            if ";" in clean[m.end() - 1:brace]:
                continue
            depth, i, end, max_depth = 1, brace + 1, -1, 1
            while i < len(clean):
                if clean[i] == "{":
                    depth += 1
                    max_depth = max(max_depth, depth)
                elif clean[i] == "}":
                    depth -= 1
                    if depth == 0:
                        end = i
                        break
                i += 1
            if end < 0:
                continue
            wl, wl_end = offset_to_line(starts, m.start()), offset_to_line(starts, end)
            if wl_end - wl + 1 > cfg["max_method_lines"]:
                out.append(Finding("web-func-too-long", "warn", path, wl,
                                   f"{name}() {wl_end - wl + 1} 行 > {cfg['max_method_lines']}"))
            if max_depth - 1 > cfg["max_block_depth"]:
                out.append(Finding("web-too-nested", "error", path, wl,
                                   f"{name}() 嵌套 {max_depth - 1} 层 > {cfg['max_block_depth']}"))

    clines = clean.split("\n")
    for i, ln in enumerate(lines, 1):
        code = _code_line(clines, i)          # 剥离字符串/注释后的同一行
        s = code.strip()
        if len(ln) > cfg["max_line_len"]:
            out.append(Finding("line-too-long", "warn", path, i,
                               f"第 {i} 行 {len(ln)} 字符 > {cfg['max_line_len']}"))
        if re.search(r"(?<![\w.])var\s+\w", code):
            out.append(Finding("js-var", "error", path, i, "用 var：改 const / let"))
        if re.search(r"[^=!<>]==[^=]|[^=!<>]!=[^=]", code):
            out.append(Finding("js-loose-eq", "warn", path, i, "宽松比较 ==/!= ：改 ===/!=="))
        if re.search(r"(?<![\w.])eval\s*\(|new\s+Function\s*\(", code):
            out.append(Finding("js-eval", "error", path, i, "eval / new Function 有注入风险"))
        if re.search(r"\bdebugger\b", code):
            out.append(Finding("js-debugger", "error", path, i, "提交前删掉 debugger"))
        if re.search(r"console\.(log|debug|info)\s*\(", code):
            out.append(Finding("js-console", "warn", path, i,
                               "console 调试输出残留：删除或走统一日志"))
        if re.search(r":\s*any\b|<any>|as\s+any\b", code):
            out.append(Finding("ts-any", "warn", path, i,
                               "显式 any：写具体类型或 unknown + 收窄，类型定义放 typings/"))
        if _ternary_count(code) >= 2 and "=>" not in s:
            out.append(Finding("nested-ternary", "warn", path, i,
                               "嵌套三元：改 if/else 或查表"))
        chains = len(re.findall(r"\.\w+\s*\(", code))
        if chains > cfg["max_chain"] and not s.startswith(("import", "export")):
            out.append(Finding("deep-chain", "warn", path, i,
                               f"第 {i} 行 {chains} 个方法调用串在一行（阈值 {cfg['max_chain']}）"))

    _common_checks(path, lines, out)


def _looks_like_credential(value):
    """值的形态是否像真凭据：够长，且含数字或符号。i18n 标签/格式常量会被挡掉。"""
    if len(value) < 12:
        return False
    has_digit = any(ch.isdigit() for ch in value)
    has_symbol = any((not ch.isalnum()) and ch != " " for ch in value)
    return has_digit or has_symbol


def _common_checks(path, lines, out):
    p = path.replace("\\", "/")
    basename = os.path.basename(p)
    is_const_file = basename.endswith("Constants.java") or basename.endswith("Constants.ts")
    is_i18n = "/locales/" in p or "/i18n/" in p
    for i, ln in enumerate(lines, 1):
        if "readability-check: allow" in ln:
            continue
        is_comment = ln.strip().startswith(("//", "*", "#", "/*"))
        lm = _CRED_LABEL_PAT.search(ln)
        vm = _CRED_VALUE_PAT.match(ln, lm.end()) if (lm and not is_comment) else None
        if lm and vm:
            # 只有「值确实像凭据」且不在常量/i18n 文件里才算 error，其余降级为提示
            sev = "error" if (_looks_like_credential(vm.group(1))
                              and not is_const_file and not is_i18n) else "warn"
            out.append(Finding("hardcoded-secret", sev, p, i,
                               "疑似硬编码口令/密钥：确认不是凭据（i18n 标签/格式常量请加 "
                               "`readability-check: allow` 注释说明）"))
        elif IP_PAT.search(ln):
            out.append(Finding("hardcoded-host", "warn", p, i,
                               "硬编码 IP：抽到配置"))
        if TODO_PAT.search(ln):
            out.append(Finding("todo-left", "warn", p, i,
                               "遗留 TODO/FIXME：提交前处理或转 issue"))


def _code_line(clines, i):
    """取剥离字符串/注释后的同一行，用于判断代码内容（避免字符串/注释误报）。"""
    return clines[i - 1] if 0 <= i - 1 < len(clines) else ""


def _ternary_count(code_ln):
    """真正的三元运算符个数：排除 ?? (空值合并) 和 ?. / ?: (可选链/TS 可选)。"""
    cleaned = code_ln.replace("??", "").replace("?.", "").replace("?:", "")
    return cleaned.count("?")


# ------------------------------------------------------------------ 遍历
def _is_skipped(path: str) -> bool:
    """路径任一段命中 SKIP_DIRS，或文件名命中生成物清单 → 跳过。

    changed-only 模式传进来的是**具体文件路径**（git diff --name-only），不经过 os.walk，
    所以必须在文件粒度再判一次，否则 SKIP_DIRS 形同虚设
    （实测：src/router/elegant/transform.ts 被当业务代码，报「函数 129 行 > 50」）。
    """
    norm = path.replace("\\", "/")
    if any(part in SKIP_DIRS for part in norm.split("/")):
        return True
    return any(norm.endswith(suffix) for suffix in SKIP_FILE_SUFFIXES)


def iter_files(paths):
    for p in paths:
        if _is_skipped(p):
            continue
        if os.path.isfile(p):
            yield p
            continue
        for root, dirs, files in os.walk(p):
            dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
            for fn in files:
                full = os.path.join(root, fn)
                if not _is_skipped(full):
                    yield full


def changed_files(base):
    got = []
    for c in (["git", "diff", "--name-only", "--diff-filter=ACMR", base],
              ["git", "ls-files", "--others", "--exclude-standard"]):
        try:
            r = subprocess.run(c, capture_output=True, text=True, check=False)
            if r.returncode == 0:
                got += [x for x in r.stdout.splitlines() if x.strip()]
        except FileNotFoundError:
            print("错误：找不到 git", file=sys.stderr)
            sys.exit(2)
    return sorted(set(got))


def main():
    ap = argparse.ArgumentParser(description="可读性闸门（Java + Vue/TS）")
    ap.add_argument("paths", nargs="*")
    ap.add_argument("--changed-only", metavar="BASE")
    ap.add_argument("--json", metavar="OUT")
    ap.add_argument("--fail-on", choices=["error", "warn", "none"], default="error")
    ap.add_argument("--rules", action="store_true")
    args = ap.parse_args()

    if args.rules:
        print(RULES_TEXT)
        return 0

    if args.changed_only:
        # changed-only 传进来的是具体文件路径（不进 os.walk），必须在这里再过一次排除，
        # 否则 SKIP_DIRS / 生成物清单对「改了哪些文件」这条路径完全无效
        files = [f for f in changed_files(args.changed_only) if os.path.isfile(f) and not _is_skipped(f)]
    else:
        paths = [p for p in (args.paths or CONFIG["default_paths"]) if os.path.exists(p)]
        if not paths:
            print("错误：没有可扫描的路径", file=sys.stderr)
            return 2
        files = [f for f in iter_files(paths) if os.path.isfile(f)]

    out, scanned = [], 0
    for f in files:
        ext = os.path.splitext(f)[1].lower()
        try:
            if ext in JAVA_EXT:
                check_java(f, out); scanned += 1
            elif ext in WEB_EXT:
                check_web(f, out); scanned += 1
        except Exception as e:                      # 单文件失败不影响整体
            out.append(Finding("scan-error", "warn", f, 0, f"扫描失败：{e}"))

    errors = [x for x in out if x.sev == "error"]
    warns = [x for x in out if x.sev == "warn"]

    print("=" * 70)
    print(f"可读性闸门报告   扫描 {scanned} 个文件    error {len(errors)}   warn {len(warns)}")
    print("=" * 70)

    by_rule = defaultdict(list)
    for x in out:
        by_rule[x.rule].append(x)
    order = sorted(by_rule, key=lambda r: (-sum(1 for y in by_rule[r] if y.sev == "error"),
                                           -len(by_rule[r])))
    for rule in order:
        items = by_rule[rule]
        sev = "error" if any(i.sev == "error" for i in items) else "warn"
        print(f"\n[{sev}] {rule}   {len(items)} 处")
        for it in items[:CONFIG["samples_per_rule"]]:
            print(f"    {it.path}:{it.line}  {it.msg}")
        if len(items) > CONFIG["samples_per_rule"]:
            print(f"    ... 其余 {len(items) - CONFIG['samples_per_rule']} 处见 JSON")

    if not out:
        print("\n通过：没有命中任何规则。")

    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump({"scanned": scanned, "error": len(errors), "warn": len(warns),
                       "findings": [x.as_dict() for x in out]}, f,
                      ensure_ascii=False, indent=2)
        print(f"\nJSON 报告：{args.json}")

    if args.fail_on == "none":
        return 0
    if args.fail_on == "warn":
        return 1 if out else 0
    return 1 if errors else 0


RULES_TEXT = """可读性闸门规则清单（error 必须改；warn 需作者给出理由）
规则依据：ai-rules/01-readability.md

[通用]
  line-too-long        单行 > %(max_line_len)s 字符
  hardcoded-secret     硬编码口令/密钥
  hardcoded-host       硬编码 IP
  todo-left            遗留 TODO/FIXME
  scan-error           文件扫描失败

[Java — 结构]
  java-file-too-long   单文件 > %(max_file_lines)s 行
  java-method-too-long 单方法 > %(max_method_lines)s 行
  java-too-nested      方法内嵌套 > %(max_block_depth)s 层
  java-too-many-params 参数 > %(max_params)s 个
  deep-chain           链式调用 > %(max_chain)s 层
  nested-ternary       嵌套三元表达式

[Java — 风险与约定]
  wildcard-import      import x.*
  sql-in-java          @Select/@Insert/@Update/@Delete/@SelectKey/*Provider
  silent-catch         catch 块为空（静默吞异常）
  thin-catch           catch 块过薄（未记日志/未转换异常）
  print-debug          printStackTrace / System.out.print
  return-null          return null（未说明理由）

[Vue/TS — 结构]
  web-file-too-long    单文件 > %(max_file_lines)s 行
  web-func-too-long    单函数 > %(max_method_lines)s 行
  web-too-nested       嵌套 > %(max_block_depth)s 层

[Vue/TS — 语法与类型]
  js-var               var
  js-loose-eq          == / !=
  js-eval              eval / new Function
  js-debugger          debugger
  js-console           console.log/debug/info 残留
  ts-any               显式 any
""" % CONFIG


if __name__ == "__main__":
    sys.exit(main())
