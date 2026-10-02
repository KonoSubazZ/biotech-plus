#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把项目用到的 iconify 图标抓成本地静态资源（离线包）。

为什么需要：
  前端渲染 iconify 类型的图标（icon 值写成 `mdi:xxx`、`material-symbols:xxx`）时，
  SvgIcon 走 @iconify/vue 的运行时 <Icon>，默认请求 https://api.iconify.design 。
  生产/内网环境访问不到外网 → 这些图标就是空白（不是报错，是静默不显示，很难发现）。
  模板已经留好了离线开关：src/plugins/iconify.ts 的 addAPIProvider('', {resources:[VITE_ICONIFY_URL]})。
  本脚本负责：扫描用到的图标 → 从 api.iconify.design 下载裁剪过的集合 JSON → 落到
  template-frontend/public/iconify/<集合>.json，配合 .env 的 VITE_ICONIFY_URL=/iconify 离线使用。

用法（仓库根目录执行）：
  python3 tools/gen-iconify-offline.py            # 扫描 + 下载 + 报告
  python3 tools/gen-iconify-offline.py --check    # 只扫描核对，不联网不落盘
  python3 tools/gen-iconify-offline.py --prune    # 顺带删掉 public/iconify 里已不再需要的集合

新增页面用了新图标后**重跑一次**即可（幂等）。--check 适合放进 CI：发现新图标未离线化就报错。
退出码：0 全部就绪；1 有未离线化的图标（仅 --check 时）。
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from collections import defaultdict

API = "https://api.iconify.design"
ICON_RE = re.compile(r"""["'`]([a-z0-9][a-z0-9-]{1,29}:[a-z0-9][a-z0-9-]{0,49})["'`]""")
SRC_EXT = (".vue", ".ts", ".js", ".tsx", ".mts", ".css", ".scss")
# 明显不是 iconify 集合的前缀（命中就跳过，省一次网络请求）
NOT_COLLECTION = {"aspect", "ratio", "zh", "en", "lang", "type", "http", "https", "data",
                  "min", "max", "background", "font", "grid", "flex", "text", "color", "theme",
                  "update", "virtual", "sm", "lt-sm", "html", "css", "import"}


def collect(fe_root: str) -> dict[str, set[str]]:
    """扫描 template-frontend 源码 + .env，返回 {集合: {图标名}}"""
    found: dict[str, set[str]] = defaultdict(set)
    src = os.path.join(fe_root, "src")
    for dirpath, dirs, files in os.walk(src):
        dirs[:] = [d for d in dirs if d not in ("node_modules", ".git")]
        for f in files:
            if not f.endswith(SRC_EXT):
                continue
            txt = open(os.path.join(dirpath, f), encoding="utf-8", errors="ignore").read()
            for m in ICON_RE.finditer(txt):
                prefix, name = m.group(1).split(":", 1)
                if prefix not in NOT_COLLECTION:
                    found[prefix].add(name)
    # .env 里的默认图标（不在 src 里，容易漏）
    for envf in (".env", ".env.dev", ".env.prod", ".env.test"):
        p = os.path.join(fe_root, envf)
        if os.path.isfile(p):
            for line in open(p, encoding="utf-8"):
                m = re.match(r"\s*VITE_MENU_ICON\s*=\s*([a-z0-9-]+:[a-z0-9-]+)", line)
                if m:
                    prefix, name = m.group(1).split(":", 1)
                    found[prefix].add(name)
    return found


def fetch_collection(prefix: str, names: list[str]) -> dict | None:
    """按集合下载裁剪后的 iconify JSON；集合不存在返回 None"""
    url = f"{API}/{prefix}.json?icons={','.join(sorted(names))}"
    # iconify 会对空/默认 UA 返回 403，必须带 UA（curl 默认能过，urllib 不能）
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (iconify-offline-fetch)"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            obj = json.loads(r.read().decode())
            # 误判的 prefix（如 "06" 这种时间片段）可能返回非对象，一律按"无此集合"处理
            return obj if isinstance(obj, dict) else None
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        raise
    except Exception as e:  # noqa: BLE001
        print(f"    !! 下载失败 {prefix}: {e!r}", file=sys.stderr)
        return None


def main() -> int:
    ap = argparse.ArgumentParser(description="iconify 图标离线化")
    ap.add_argument("--root", default=None, help="仓库根目录（默认向上查找 ai-templates/）")
    ap.add_argument("--check", action="store_true", help="只核对，不联网下载")
    ap.add_argument("--prune", action="store_true", help="删除 public/iconify 下不再需要的集合文件")
    args = ap.parse_args()

    root = args.root
    if not root:
        cur = os.path.dirname(os.path.abspath(__file__))
        while cur != "/" and not os.path.isdir(os.path.join(cur, "ai-templates")):
            cur = os.path.dirname(cur)
        root = cur
    fe = os.path.join(root, "template-frontend")
    out_dir = os.path.join(fe, "public", "iconify")

    found = collect(fe)
    total = sum(len(v) for v in found.values())
    print(f"扫描到 {total} 个 iconify 图标，跨 {len(found)} 个集合")

    existing = set()
    if os.path.isdir(out_dir):
        existing = {f[:-5] for f in os.listdir(out_dir) if f.endswith(".json")}

    ready, missing_prefix, bad_names = set(), [], {}
    for prefix in sorted(found):
        names = sorted(found[prefix])
        if args.check:
            if prefix in existing:
                local = json.load(open(os.path.join(out_dir, f"{prefix}.json"), encoding="utf-8"))
                have = set((local.get("icons") or {})) | set((local.get("aliases") or {}))
                gone = [n for n in names if n not in have]
                if gone:
                    bad_names[prefix] = gone
                ready.add(prefix)
            else:
                missing_prefix.append((prefix, names))
            continue

        data = fetch_collection(prefix, names)
        if data is None:
            print(f"  - {prefix:<20} 跳过（iconify 无此集合 → 判定为扫描误判）")
            continue
        got = set((data.get("icons") or {})) | set((data.get("aliases") or {}))
        gone = [n for n in names if n not in got]
        if gone:
            bad_names[prefix] = gone
        os.makedirs(out_dir, exist_ok=True)
        with open(os.path.join(out_dir, f"{prefix}.json"), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
        size = os.path.getsize(os.path.join(out_dir, f"{prefix}.json"))
        print(f"  ✓ {prefix:<20} {len(got):>2} 个图标  {size / 1024:>6.1f} KB"
              + (f"   （请求里 {len(gone)} 个名字集合内不存在：{', '.join(gone)}）" if gone else ""))
        ready.add(prefix)

    if args.prune and not args.check and os.path.isdir(out_dir):
        for f in os.listdir(out_dir):
            if f.endswith(".json") and f[:-5] not in found:
                os.remove(os.path.join(out_dir, f))
                print(f"  prune: 删除已不再需要的 {f}")

    if args.check:
        print()
        if missing_prefix or bad_names:
            for p, names in missing_prefix:
                print(f"  !! 集合 {p} 尚未离线化（{len(names)} 个图标：{', '.join(names)}）")
            for p, gone in bad_names.items():
                print(f"  !! {p}.json 里缺：{', '.join(gone)}")
            print("\n跑一次 python3 tools/gen-iconify-offline.py 即可补上")
            return 1
        print("离线包与当前代码一致 ✅")
        return 0

    print(f"\n完成：{len(ready)} 个集合 → {os.path.relpath(out_dir, root)}/")
    print("记得 .env 里要有 VITE_ICONIFY_URL=/iconify（src/plugins/iconify.ts 会用它替换在线 API）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
