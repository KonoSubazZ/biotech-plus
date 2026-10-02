#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""new_crud.py — 按 ai-templates 生成一个 CRUD 模块（后端 + 前端）

一条命令完成「建目录 → 复制 → 改名（内容+文件名）→ 注册 Maven 模块 → 自检」，
替代手工 cp/sed —— 手工最容易漏的两步：① 忘改文件名（class 名与文件名不一致，编译不过）
② 忘在父 pom 注册（编译不过 / 接口 404）。

用法（在仓库根目录执行）：

    # 只给名字，字段用模板示例（之后自己改）
    python3 tools/new_crud.py --module biotech --entity ProductConfig --table product_config --title 产品配置

    # 带字段定义（推荐）——列名:类型:中文标签[:req][:最大长度]
    python3 tools/new_crud.py --module biotech --entity QcRecord --table qc_record --title 质控记录 \
        --fields "subbarcode:string:样本条码:req:80,qc_item:string:质控项目:req:100,\
qc_result:string:质控结果:50,status:string:人工状态:req:20,remark:string:备注:500"

    # 先看会做什么，不落盘
    python3 tools/new_crud.py ... --dry-run

类型：string | text | int | long | decimal | date | bool | datetime
  req  → string/text 加 @NotBlank，其余加 @NotNull
  :N   → string/text 加 @Size(max=N)

退出码：0 成功；1 有文件已存在等冲突；2 参数错误
"""

import argparse
import os
import re
import shutil
import sys

TEMPLATE_DIR = os.path.join("ai-templates", "backend-module")
FE_TEMPLATE_DIR = os.path.join("ai-templates", "frontend-module")

# 模板里出现过的示例名（脚本按这些做替换；顺序重要：长串在前）
EX_ENTITY = "ProductConfig"
EX_TABLE = "product_config"
EX_PKG = "org.dromara.biotech"
EX_MODULE = "biotech"
EX_ARG = "ruoyi-biotech"
EX_TITLE = "产品配置"

TYPE_MAP = {
    "string": "String", "text": "String",
    "int": "Integer", "long": "Long", "decimal": "BigDecimal",
    "date": "Date", "datetime": "Date", "bool": "Boolean",
}


def pascal(s):
    return "".join(p[:1].upper() + p[1:] for p in re.split(r"[_\-\s]+", s) if p)


def camel(s):
    p = pascal(s)
    return p[:1].lower() + p[1:]


def kebab(s):
    return re.sub(r"(?<!^)(?=[A-Z])", "-", s).lower()


def snake_to_camel(s):
    parts = s.split("_")
    return parts[0] + "".join(p[:1].upper() + p[1:] for p in parts[1:])


def find_root(start):
    cur = os.path.abspath(start)
    while cur != "/":
        if os.path.isdir(os.path.join(cur, "ai-templates")):
            return cur
        cur = os.path.dirname(cur)
    return None


# ------------------------------------------------------------------ 字段生成
def parse_fields(spec):
    """'列名:类型:标签[:req][:长度],...' -> [dict]"""
    out = []
    for item in (spec or "").split(","):
        item = item.strip()
        if not item:
            continue
        parts = item.split(":")
        col = parts[0].strip()
        typ = (parts[1].strip().lower() if len(parts) > 1 and parts[1].strip() else "string")
        label = parts[2].strip() if len(parts) > 2 and parts[2].strip() else col
        rest = [p.strip() for p in parts[3:]]
        if typ not in TYPE_MAP:
            raise SystemExit(f"类型不支持：{typ}（支持 {'/'.join(TYPE_MAP)}）")
        maxlen = next((int(p) for p in rest if p.isdigit()), None)
        out.append({
            "col": col, "field": snake_to_camel(col), "label": label,
            "type": TYPE_MAP[typ], "scalar": typ in ("string", "text"),
            "req": "req" in [r.lower() for r in rest], "max": maxlen,
        })
    return out


def gen_domain_block(fields):
    lines = []
    for f in fields:
        lines.append(f"    /** {f['label']} */")
        lines.append(f"    private {f['type']} {f['field']};")
        lines.append("")
    return "\n".join(lines).rstrip("\n")


def gen_bo_block(fields):
    lines = []
    for f in fields:
        lines.append(f"    /** {f['label']} */")
        if f["req"]:
            ann = "NotBlank" if f["scalar"] else "NotNull"
            lines.append(f'    @{ann}(message = "{f["label"]}不能为空")')
        if f["scalar"] and f["max"]:
            lines.append(f'    @Size(max = {f["max"]}, message = "{f["label"]}长度不能超过 {f["max"]}")')
        elif f["scalar"] and f["req"]:
            pass
        lines.append(f"    private {f['type']} {f['field']};")
        lines.append("")
    return "\n".join(lines).rstrip("\n")


def gen_vo_block(fields):
    lines = []
    for f in fields:
        lines.append(f"    /** {f['label']} */")
        if f["type"] == "Date":
            lines.append('    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")')
        lines.append(f"    private {f['type']} {f['field']};")
        lines.append("")
    return "\n".join(lines).rstrip("\n")


def replace_fields(text, block):
    """替换 // @fields:start ... // @fields:end 之间的内容（保留 end 标记的缩进）。"""
    pat = re.compile(r"(// @fields:start\n)(.*?)([ \t]*// @fields:end)", re.DOTALL)
    if not pat.search(text):
        return text, False
    return pat.sub(lambda m: m.group(1) + block + "\n" + m.group(3), text), True


# ------------------------------------------------------------------ 主流程
def main():
    ap = argparse.ArgumentParser(description="按 ai-templates 生成 CRUD 模块")
    ap.add_argument("--module", required=True, help="业务模块名（小写，如 biotech）")
    ap.add_argument("--entity", required=True, help="实体名 PascalCase（如 ProductConfig）")
    ap.add_argument("--table", required=True, help="数据库表名 snake_case（如 product_config）")
    ap.add_argument("--title", default=None, help="中文名（用于注释与前端标题）")
    ap.add_argument("--fields", default=None, help="列名:类型:标签[:req][:长度]，逗号分隔")
    ap.add_argument("--author", default=None, help="写在 @author 里（默认不改）")
    ap.add_argument("--no-frontend", action="store_true", help="只生成后端")
    ap.add_argument("--dry-run", action="store_true", help="只打印计划，不落盘")
    ap.add_argument("--force", action="store_true", help="覆盖已存在的文件")
    ap.add_argument("--root", default=None, help="仓库根目录（默认自动向上查找 ai-templates/）")
    args = ap.parse_args()

    root = args.root or find_root(os.path.dirname(os.path.abspath(__file__)))
    if not root:
        print("错误：找不到仓库根目录（应包含 ai-templates/），用 --root 指定", file=sys.stderr)
        return 2

    mod = args.module.strip()
    ent = args.entity.strip()
    tbl = args.table.strip()
    title = (args.title or ent).strip()
    arg = f"ruoyi-{mod}"
    ent_camel = camel(ent)
    ent_kebab = kebab(ent)
    pkg = f"org.dromara.{mod}"
    mod_pascal = pascal(mod)
    perm = f"{mod}:{ent_camel}"

    # 替换表：长串优先，避免子串互相吃掉。
    # 注意模板里同一个实体有三种大小写形态，必须分别处理，否则会漏：
    #   ProductConfig（类名/文件名）、productConfig（驼峰：权限点位、URL）、
    #   product-config（短横线：前端 service 目录与 import 路径）
    subs = [
        # 前端 typings 是嵌套 namespace 写法（declare namespace Api { namespace Biotech { … } }），
        # 不存在 "Api.Biotech.ProductConfig" 这样的连续串，必须单独替换 namespace 名 ——
        # 漏了它，生成的类型会一直挂在 Api.Biotech 下（与 api.ts / index.vue 引用不上）。
        ("namespace Biotech {", f"namespace {mod_pascal} {{"),
        ("Api.Biotech.ProductConfig", f"Api.{mod_pascal}.{ent}"),
        (f"org.dromara.{EX_MODULE}", pkg),
        (f"{EX_MODULE}:{camel(EX_ENTITY)}", perm),          # 权限点位 biotech:productConfig:*
        (f"{EX_MODULE}:{EX_ENTITY}", perm),
        (f"{EX_MODULE}/{kebab(EX_ENTITY)}", f"{mod}/{ent_kebab}"),   # 前端 import 路径
        (f"{EX_MODULE}/{camel(EX_ENTITY)}", f"{mod}/{ent_camel}"),   # 请求 URL 前缀
        (f"{EX_MODULE}/{EX_ENTITY}", f"{mod}/{ent_kebab}"),
        (EX_ARG, arg),
        # 前端文件名与 import 路径是短横线形态（product-config-search.vue），
        # PascalCase 那几条替换覆盖不到，必须单独列，否则生成的 import 指向不存在的文件。
        (kebab(EX_ENTITY), ent_kebab),
        (camel(EX_ENTITY), ent_camel),
        (EX_ENTITY, ent),
        (EX_TABLE, tbl),
        (EX_TITLE, title),
        (f"{EX_MODULE} 业务模块", f"{mod} 业务模块"),
    ]
    if args.author:
        subs.append(("<你的名字>", args.author))

    def rewrite(text):
        for a, b in subs:
            text = text.replace(a, b)
        return text

    fields = parse_fields(args.fields)
    be_root = os.path.join(root, "template-backend", "ruoyi-modules", arg)
    be_java = os.path.join(be_root, "src", "main", "java", *pkg.split("."))
    be_xml = os.path.join(be_root, "src", "main", "resources", "mapper", mod)

    tpl = os.path.join(root, TEMPLATE_DIR)
    if not os.path.isdir(tpl):
        print(f"错误：模板目录不存在 {tpl}", file=sys.stderr)
        return 2

    # (源文件相对模板目录, 目标绝对路径, 是否 java 类文件)
    jobs = [
        (f"{EX_ENTITY}.java", os.path.join(be_java, "domain", f"{ent}.java"), True),
        (f"{EX_ENTITY}Bo.java", os.path.join(be_java, "domain", "bo", f"{ent}Bo.java"), True),
        (f"{EX_ENTITY}Vo.java", os.path.join(be_java, "domain", "vo", f"{ent}Vo.java"), True),
        (f"{EX_ENTITY}Controller.java", os.path.join(be_java, "controller", f"{ent}Controller.java"), True),
        (f"I{EX_ENTITY}Service.java", os.path.join(be_java, "service", f"I{ent}Service.java"), True),
        (f"{EX_ENTITY}ServiceImpl.java", os.path.join(be_java, "service", "impl", f"{ent}ServiceImpl.java"), True),
        (f"{EX_ENTITY}Mapper.java", os.path.join(be_java, "mapper", f"{ent}Mapper.java"), True),
        (f"{EX_ENTITY}Mapper.xml", os.path.join(be_xml, f"{ent}Mapper.xml"), False),
        ("pom.xml", os.path.join(be_root, "pom.xml"), False),
    ]

    fe_jobs = []
    if not args.no_frontend:
        fe = os.path.join(root, "template-frontend", "src")
        # 页面必须放 views/<模块目录>/<实体>/ 两层 —— 与菜单层级一致。
        # 若放一层 views/<实体>/，前端算出的路由名里没有下划线，会被 elegant/transform.ts 的
        # isFirstLevelRoute(name)=!name.includes('_') 误判成一级路由 → 要求 layout.base$view.xxx
        # 形式的 component → 动态菜单给不出 → throw 被 catch 吞掉 → 路由根本不注册，
        # 表现为「菜单能显示、点击报 No match for {...}」。菜单的 component 同步写 "<mod>/<实体短横线>/index"。
        fe_jobs = [
            ("index.vue", os.path.join(fe, "views", mod, ent_kebab, "index.vue")),
            ("search.vue",
             os.path.join(fe, "views", mod, ent_kebab, "modules", f"{ent_kebab}-search.vue")),
            ("operate-drawer.vue",
             os.path.join(fe, "views", mod, ent_kebab, "modules", f"{ent_kebab}-operate-drawer.vue")),
            ("api.ts", os.path.join(fe, "service", "api", mod, f"{ent_kebab}.ts")),
            ("types.d.ts", os.path.join(fe, "typings", "api", f"{mod}.api.d.ts")),
        ]

    # ---- 冲突检查（先全查再动手，避免生成一半）
    conflicts = []
    for _, dst, _ in jobs:
        if os.path.exists(dst) and not args.force and os.path.basename(dst) != "pom.xml":
            conflicts.append(dst)
    for src, dst in fe_jobs:
        # typings 是「一模块一文件」：同模块的第二个实体必然已存在，
        # 不算硬冲突，交给写入阶段提示手工合并 namespace
        if src != "types.d.ts" and os.path.exists(dst) and not args.force:
            conflicts.append(dst)
    if conflicts:
        print("以下文件已存在，停止（确认要覆盖请加 --force）：", file=sys.stderr)
        for c in conflicts:
            print("  " + os.path.relpath(c, root), file=sys.stderr)
        return 1

    print("=" * 66)
    print(f"CRUD 脚手架：{mod} / {ent} / {tbl} / {title}")
    print(f"权限点位前缀：{perm}:*     后端包：{pkg}     Maven 模块：{arg}")
    if args.dry_run:
        print("（--dry-run：只打印，不落盘）")
    print("=" * 66)

    created = []
    for src_name, dst, is_java in jobs:
        if os.path.basename(dst) == "pom.xml" and os.path.exists(dst):
            print(f"  跳过（已存在，不覆盖）：{os.path.relpath(dst, root)}")
            continue
        text = open(os.path.join(tpl, src_name), encoding="utf-8").read()
        text = rewrite(text)
        if fields and is_java:
            if src_name == f"{EX_ENTITY}.java":
                text, _ = replace_fields(text, gen_domain_block(fields))
            elif src_name == f"{EX_ENTITY}Bo.java":
                text, _ = replace_fields(text, gen_bo_block(fields))
            elif src_name == f"{EX_ENTITY}Vo.java":
                text, _ = replace_fields(text, gen_vo_block(fields))
        if not args.dry_run:
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(dst, "w", encoding="utf-8") as f:
                f.write(text)
        created.append((os.path.relpath(dst, root), src_name))

    for src, dst in fe_jobs:
        text = open(os.path.join(root, FE_TEMPLATE_DIR, src), encoding="utf-8").read()
        if src == "types.d.ts" and os.path.exists(dst) and not args.force:
            # typings 是「一模块一文件」，已存在时不能覆盖 → 提示手工合并
            print(f"  ⚠ 已存在，需手工把新实体的 namespace 合并进去："
                  f"{os.path.relpath(dst, root)}")
            continue
        text = rewrite(text)
        if not args.dry_run:
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(dst, "w", encoding="utf-8") as f:
                f.write(text)
        created.append((os.path.relpath(dst, root), f"frontend/{src}"))

    # ---- Maven 注册
    # 三处都要动，缺任何一处都编译不过
    pom_root = os.path.join(root, "template-backend", "pom.xml")
    pom_mods = os.path.join(root, "template-backend", "ruoyi-modules", "pom.xml")
    pom_admin = os.path.join(root, "template-backend", "ruoyi-admin", "pom.xml")
    pom_notes = []
    if not args.dry_run:
        # 1) 根 pom 的 <dependencyManagement>：声明新模块的版本。
        #    漏这一步 → ruoyi-admin 引用该模块时 Maven 报
        #    "'dependencies.dependency.version' for org.dromara:<arg>:jar is missing"
        if os.path.exists(pom_root):
            s = open(pom_root, encoding="utf-8").read()
            if f"<artifactId>{arg}</artifactId>" not in s:
                block = ("            <dependency>\n"
                         "                <groupId>org.dromara</groupId>\n"
                         f"                <artifactId>{arg}</artifactId>\n"
                         "                <version>${revision}</version>\n"
                         "            </dependency>\n\n")
                m = re.search(r"            <dependency>\n                <groupId>org\.dromara</groupId>\n"
                              r"                <artifactId>ruoyi-", s)
                if m:
                    s = s[:m.start()] + block + s[m.start():]
                else:
                    s = s.replace("    <dependencyManagement>", "    <dependencyManagement>\n" + block, 1)
                open(pom_root, "w", encoding="utf-8").write(s)
                pom_notes.append(f"根 pom.xml 已在 dependencyManagement 里声明 {arg} 的版本")
            else:
                pom_notes.append(f"根 pom.xml 已有 {arg}，跳过")
        # 2) ruoyi-modules/pom.xml：声明子模块
        if os.path.exists(pom_mods):
            s = open(pom_mods, encoding="utf-8").read()
            if f"<module>{arg}</module>" not in s:
                s = s.replace("    <modules>\n", f"    <modules>\n        <module>{arg}</module>\n", 1)
                open(pom_mods, "w", encoding="utf-8").write(s)
                pom_notes.append(f"ruoyi-modules/pom.xml 已加 <module>{arg}</module>")
            else:
                pom_notes.append(f"ruoyi-modules/pom.xml 已有 {arg}，跳过")
        if os.path.exists(pom_admin):
            s = open(pom_admin, encoding="utf-8").read()
            if f"<artifactId>{arg}</artifactId>" not in s:
                block = ("        <dependency>\n"
                         "            <groupId>org.dromara</groupId>\n"
                         f"            <artifactId>{arg}</artifactId>\n"
                         "        </dependency>\n\n")
                s = s.replace("    </dependencies>", block + "    </dependencies>", 1)
                open(pom_admin, "w", encoding="utf-8").write(s)
                pom_notes.append(f"ruoyi-admin/pom.xml 已加对 {arg} 的依赖")
            else:
                pom_notes.append(f"ruoyi-admin/pom.xml 已有 {arg}，跳过")
    else:
        pom_notes.append(f"（dry-run）将注册 {arg} 到 ruoyi-modules/pom.xml 与 ruoyi-admin/pom.xml")

    # ---- 报告
    print(f"\n生成 {len(created)} 个文件：")
    for rel, src in created:
        print(f"  {rel}")
    print("\nMaven 注册：")
    for n in pom_notes:
        print(f"  {n}")

    if fields:
        print(f"\n字段（{len(fields)} 个，已写入 domain/Bo/Vo）：")
        for f in fields:
            flag = "必填" if f["req"] else "可空"
            print(f"  {f['col']:24} {f['type']:9} {flag}  {f['label']}")

    # ---- 自检
    print("\n自检：")
    # 只有「目标值与模板示例值不同」的名字才需要查残留；
    # 例如 module=biotech 时 org.dromara.biotech 就是正确包名，
    # 用户把实体正好起名 ProductConfig / 表正好叫 product_config 时同理
    watch = []
    # 三种大小写变体都要监测：漏掉任何一种都会在生成物里留残（如权限点位
    # biotech:productConfig:list 只改类名时会原样留下 productConfig）
    if ent != EX_ENTITY:
        watch += [EX_ENTITY, camel(EX_ENTITY), kebab(EX_ENTITY)]
    if tbl != EX_TABLE:
        watch.append(EX_TABLE)
    if pkg != EX_PKG:
        watch.append(EX_PKG)
    if arg != EX_ARG:
        watch.append(EX_ARG)
    if title != EX_TITLE:
        watch.append(EX_TITLE)

    residue = 0
    for dirpath, _, filenames in os.walk(be_root):
        for fn in filenames:
            p = os.path.join(dirpath, fn)
            try:
                t = open(p, encoding="utf-8").read()
            except Exception:
                continue
            for bad in watch:
                if bad in t:
                    print(f"  ⚠ 残留 {bad!r} → {os.path.relpath(p, root)}")
                    residue += 1
    print("  ✓ 无模板名残留" if not residue else f"  ⚠ {residue} 处残留")
    bad_cls = 0
    for dirpath, _, filenames in os.walk(be_java):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            p = os.path.join(dirpath, fn)
            m = re.search(r"\b(class|interface)\s+([A-Za-z0-9_]+)", open(p, encoding="utf-8").read())
            if m and fn[:-5] != m.group(2):
                print(f"  ⚠ 类名≠文件名：{os.path.relpath(p, root)} 声明 {m.group(2)}")
                bad_cls += 1
    print("  ✓ 类名与文件名一致" if not bad_cls else f"  ⚠ {bad_cls} 处不一致")

    print("\n下一步：")
    print(f"  1. 按实际表结构调整字段（若没给 --fields，现在是模板示例字段）")
    print(f"  2. 建表 SQL：设计文档只有字段表，仓库里没有业务表 DDL，需自行确认")
    print(f"  3. 前端：src/views/{ent_kebab}/index.vue + src/service/api/{mod}/ + typings")
    print(f"  4. 菜单与权限点位：把 {perm}:list/add/edit/remove 加进 sys_menu")
    print(f"  5. 验证：cd template-backend && mvn -B -ntp -pl ruoyi-modules/{arg} -am validate")
    print(f"  6. 闸门：python3 tools/readability_check.py --changed-only origin/main")
    return 0


if __name__ == "__main__":
    sys.exit(main())
