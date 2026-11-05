#!/usr/bin/env python3
"""模拟上游三个数据源把数据推进本系统 —— 三条接入（CRM / LIMS / 生信）目前都还没实现，
本脚本按『将来要接的接口形状』先把数据灌进来，用于联调下游模块。

三个动作（都可重复执行，幂等）
  ① 样本信息  （CRM 接口，未实现）
       源：LIMS 库 192.168.20.131:8806 / omics.sample_file（按 BARCODE 取）
       落：本系统 cms_sample —— 本仓「样本信息」表就是生产录单表 myapp_webcrmsample 的
           122 列全量镜像；本仓/本地库里没有名为 web_crm_sample 的表（已全仓 grep 确认）。
  ② 湿实验质控（LIMS 接口，未实现）
       源：<root>/<date>/<product>/<barcode>/<barcode>T/10.Upload/<barcode>T.qc.txt（样本）
           + <barcode>T.G.qc.txt（对照）
       落：POST /qc/wetLabQc/receive（固定 token，无需登录）
  ③ 生信质控  （生信接口，未实现）
       源：同目录 <barcode>T.qc.txt（只有样本本身；对照qc不入 bioinfo_qc，表里没有对照列）
       落：POST /qc/bioinfo/receive（固定 token，无需登录）

接口现状（实测，本脚本 --check-interfaces 可复验）
  · 湿实验质控接收  POST /qc/wetLabQc/receive   权限点 qc:wetLabQc:receive  已存在
  · 生信质控接收    POST /qc/bioinfo/receive    权限点 qc:bioinfo:receive   已存在
  · 样本信息        只有 POST /report/sampleInfo（登录态）、/importData（Excel 导入）、PUT/DELETE；
                    没有「上游推送接收」接口 → 本脚本第①步只能直接写库（接口缺口的补偿）

用法
  python3 simulate-upstream-push.py                     # 干跑（默认）：只打印将要写入的内容
  python3 simulate-upstream-push.py --apply             # 真写
  python3 simulate-upstream-push.py --apply --barcode TKHS260114250-1A
  python3 simulate-upstream-push.py --check-interfaces   # 只探两个接收接口是否已具备
依赖：pymysql（系统 python3 已装 2.2.8）

口径（2026-10-12 定）
  · 归属：租户 0000001「圣域药企-测试」；cms_sample.customer_name 写「圣域药企-测试」
    （生信接收按 customer_name → sys_tenant 解析药企，写别的名字会整行拒收）
  · 缺项补值：qc_standard 配了阈值但 qc.txt 里没有的项编一个达标值（终文库浓度、peak_size、
    probe_capture_ratio、血液样本缺的 Q30），输出里打 [编] 标记；--no-fabricate 可关掉
  · 不映射的源列（barcode/client/disease/disease_type/loaded_date/sample_id/status）留 NULL
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import re
import sys
import urllib.error
import urllib.request

try:
    import pymysql
except ImportError:  # pragma: no cover
    sys.exit("缺少依赖：pip install pymysql")

# ---------------------------------------------------------------- 配置
DATA_ROOT = os.environ.get("SIM_DATA_ROOT", "/home/liushangzhi/project/data")
LIMS = dict(host="192.168.20.131", port=8806, user="novo", password="GodIsLove",
            database="omics", charset="utf8mb4")
LOCAL = dict(host="127.0.0.1", port=3306, user="root", password="root",
             database="ry-vue", charset="utf8mb4", ssl_disabled=True)
DEFAULT_TENANT = "0000001"                        # 圣域药企-测试
DEFAULT_CUSTOMER_NAME = "圣域药企-测试"            # 生信接收按 customer_name → sys_tenant 解析租户
API_BASE = os.environ.get("BP_BASE", "http://127.0.0.1:8080")
CLIENT_ID = "e5cd7e4891bf95d1d19206ce24a7b32e"
YML = "/home/liushangzhi/project/biotech-plus/template-backend/ruoyi-admin/src/main/resources/application.yml"
WET_RECEIVE = "/qc/wetLabQc/receive"
BIO_RECEIVE = "/qc/bioinfo/receive"

# ---------------------------------------------------------------- 映射表
# ① LIMS omics.sample_file 列 → 本系统 cms_sample 列（同名列/规范化后同名的不再重复写）
SAMPLE_COL_MAP = {
    "subbarcode": "subbarcode",              # 业务键（源表 BARCODE 与它同值）
    "person_name": "patient_name",
    "gender": "sex",
    "birthday": "birth_day",
    "cancertype": "cancer_type",             # 录单癌种（进入解读时会带到报告 business 表）
    "clinicalremark": "clinical_remark",
    "clinicalstages": "clinical_stages",
    "pathologicaltype": "pathological_type",
    "doctorname": "doctor_name",
    "collect_date": "collect_date",
    "received_date": "dy_date",              # 到样日期
    "commission_date": "enter_date",         # 委托日期
    "sample_type": "sample_type",
    "specimen_quantity": "specimen_num",
    "specimen_type": "sample_attribute",
    "specimenno": "specimen_no",
    "serial_number": "serial_number",
    "from_organ": "from_organ",
    "sample_source": "sample_source",
    "sampleremark": "sample_remark",
    "hospital": "admission_hospital",
    "locationname": "location_name",
    "sales_contact": "sales_man",
    "saleremail": "saler_email",
    "emailaddress": "email_address",
    "supportemail": "support_email",
    "manageremail": "manager_email",
    "pmemail": "pm_email",
    "laboratoryname": "laboratory_name",
    "recordercode": "recorder_code",
    "report_receiver": "report_receiver",
    "receiv_ertele_phone": "receiver_telephone",
    "mailingaddress": "mailing_address",
    "PCODE": "pcode",
    "birthplace": "birthplace",
    "fastcode": "fast_code",
    "customertype": "customer_type",
    "customer": "custom_desc",               # LIMS 客户单位 → 客户名称（customer_name 留给租户解析，见下）
    "product_name": "erp_test_name",
    "firsttreatment": "first_treatment",
    "secondtreatment": "second_treatment",
    "thirdtreatment": "third_treatment",
    "FAMILYFIRST": "family_first",
    "FAMILYSECOND": "family_second",
}
# 同名直接搬的（源列名小写 = 本系统列名）
SAMPLE_SAME_NAME = ["age", "patient_phone", "gene_type", "gene_result", "sample_remark"]

# ② 湿实验质控：qc.txt / G.qc.txt 的中文标签 → wet_lab_qc 列（= qc_standard.qc_param）
WET_FROM_SAMPLE = {
    "肿瘤細胞含量": "pathoTumorPurity",
    "DNA 总量（ng）": "dnaTotalNg",
    "DNA 降解程度": "dnaDegradation",
    "预文库总量（ng）": "preLibTotalNg",
    # final_lib_conc（终文库浓度）qc.txt 里没有 → 不写（库里保留原值）
}
WET_FROM_CONTROL = {
    "DNA 总量（ng）": "dnaTotalNgNormal",
    "预文库总量（ng）": "preLibTotalNgNormal",
}
# ③ 生信质控：qc.txt 中文标签 → bioinfo_qc 列（= qc_standard.qc_param）
BIO_FROM_SAMPLE = {
    "下机数据量": "rawBase",
    "平均测序深度": "averageDepth",
    "覆盖均一性": "coverageUniformity02xadOnTarget",
    "目标区域覆盖度": "coverageTarget",
    "基因组比对率": "mappingRate",
    "碱基质量 Q30 占比": "rawQ30",
}
# 缺项补值（口径：2026-10-12 用户「没有就随便编 / 随便给」）——
# 只补「qc_standard 里配了阈值、但 qc.txt 里没有该行」的项，让判定有完整依据；
# 这些值是编的，脚本会单独打 [编] 标记，不与实测值混淆。
WET_FABRICATE = {"finalLibConc": "2.4", "finalLibConcNormal": "1.7"}
BIO_FABRICATE = {"rawQ30": 96.8, "peakSize": 180.0, "probeCaptureRatio": 75.0}
EMPTY_TOKENS = {"", "-", "--", "不适用", "N/A", "NA", "无", "null", "NULL"}


# ---------------------------------------------------------------- 工具
def conn(**kw):
    return pymysql.connect(connect_timeout=10, **kw)


def api_token() -> str:
    """固定 token 只配一处：application.yml 的 api.token.value（支持 ${API_TOKEN:默认值}）"""
    for line in open(YML, encoding="utf-8"):
        if line.strip().startswith("value:") and "API_TOKEN" in line:
            return line.rsplit(":", 1)[1].strip().rstrip("}")
    raise SystemExit("没在 application.yml 里找到 api.token.value")


def clean(value):
    """空/占位 → None；去掉 % 与 G 单位；其余原样字符串"""
    if value is None:
        return None
    text = str(value).strip()
    if text in EMPTY_TOKENS:
        return None
    return text


def clean_num(value):
    """数值型（bioinfo 是 decimal）：空 → None，其余去 % / G 后转 float"""
    text = clean(value)
    if text is None:
        return None
    text = text.rstrip("%").rstrip("G").rstrip("g").strip()
    try:
        return float(text)
    except ValueError:
        return None


def discover_samples(root: str):
    """扫上传根目录，按 <date>/<product>/<barcode>/<barcode>T/10.Upload 还原样本"""
    found = []
    for upload in sorted(glob.glob(os.path.join(root, "*", "*", "*", "*", "10.Upload"))):
        parts = upload.rstrip("/").split(os.sep)
        inner, barcode, product, date = parts[-2], parts[-3], parts[-4], parts[-5]
        # 质控文件：<条码T>.qc.txt = 样本自身，<条码T>.G.qc.txt = 对照（注意别用 [!G] 通配，
        # 那是「首字符不是 G」，会把 .G.qc.txt 也匹配进来）
        own = os.path.join(upload, inner + ".qc.txt")
        ctrl = os.path.join(upload, inner + ".G.qc.txt")
        found.append(dict(analysis_date=date, product=product, barcode=barcode,
                          subbarcode=barcode, upload_dir=upload,
                          qc_txt=own if os.path.exists(own) else None,
                          qc_ctrl=ctrl if os.path.exists(ctrl) else None))
    return found


def parse_qc(path: str):
    """qc.txt / G.qc.txt：制表符分隔的『标签 <tab> 值』"""
    data = {}
    if not path:
        return data
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.rstrip("\n")
            if "\t" not in line:
                continue
            label, _, value = line.partition("\t")
            data[label.strip()] = value.strip()
    return data


def local_columns(table: str) -> set:
    with conn(**LOCAL) as cn, cn.cursor() as cur:
        cur.execute("SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                    "WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s", (LOCAL["database"], table))
        return {row[0] for row in cur.fetchall()}


# ---------------------------------------------------------------- ① 样本信息
def fetch_lims_sample(subbarcode: str):
    with conn(**LIMS) as cn, cn.cursor(pymysql.cursors.DictCursor) as cur:
        cur.execute("SELECT * FROM cms_sample WHERE BARCODE=%s OR subbarcode=%s "
                    "ORDER BY sample_id DESC LIMIT 1", (subbarcode, subbarcode))
        return cur.fetchone()


def build_sample_row(lims: dict, tenant: str, customer_name: str, cols: set):
    """LIMS 行 → 本系统 cms_sample 的一行（只填映射得到且本表存在的列）"""
    row, used, unmapped = {}, [], []
    for src, dst in SAMPLE_COL_MAP.items():
        if src in lims and dst in cols:
            value = clean(lims.get(src))
            if value is not None:
                row[dst] = value
                used.append((src, dst, value))
    for src in SAMPLE_SAME_NAME:
        if src in lims and src in cols:
            value = clean(lims.get(src))
            if value is not None:
                row[src] = value
                used.append((src, src, value))
    for src, value in lims.items():
        if value in (None, ""):
            continue
        if src in SAMPLE_COL_MAP or src in SAMPLE_SAME_NAME:
            continue
        unmapped.append(src)
    # 租户：湿实验质控接收按 cms_sample.tenant_id 落库；
    #       生信质控接收按 cms_sample.customer_name → sys_tenant(tenant_id 或 tenant_name) 解析租户。
    # 所以两者都指到目标药企（customer_name 用 sys_tenant.tenant_name，如「圣域药企-测试」→ 0000001）。
    row["tenant_id"] = tenant
    row["customer_name"] = customer_name
    row.setdefault("create_by", 1)
    return row, used, sorted(unmapped)


def upsert_sample(row: dict, apply: bool):
    if not apply:
        return "dry-run"
    cols = list(row)
    placeholders = ", ".join(["%s"] * len(cols))
    updates = ", ".join("%s=VALUES(%s)" % (c, c) for c in cols if c not in ("tenant_id", "subbarcode"))
    sql = ("INSERT INTO cms_sample (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s, update_time=NOW()"
           % (", ".join(cols), placeholders, updates))
    with conn(**LOCAL) as cn:
        with cn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM cms_sample WHERE tenant_id=%%s AND subbarcode=%%s"
                        % (), (row["tenant_id"], row["subbarcode"]))
            existed = cur.fetchone()[0] > 0
            cur.execute(sql, [row[c] for c in cols])
        cn.commit()
    return "updated" if existed else "inserted"


# ---------------------------------------------------------------- ②③ 质控推送
def post_receive(path: str, items: list, token: str):
    body = json.dumps({"items": items}, ensure_ascii=False).encode()
    request = urllib.request.Request(
        API_BASE + path, data=body, method="POST",
        headers={"X-Api-Token": token, "clientid": CLIENT_ID, "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=60) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read().decode() or "{}")


def build_wet_item(sample: dict, fabricate: bool = True):
    """qc.txt + G.qc.txt → wet_lab_qc 推送行；返回 (item, 编造的项)"""
    own, ctrl = parse_qc(sample["qc_txt"]), parse_qc(sample["qc_ctrl"])
    item = {"subbarcode": sample["subbarcode"]}
    for label, field in WET_FROM_SAMPLE.items():
        value = clean(own.get(label))
        if value is not None:
            item[field] = value.strip("%") if field == "pathoTumorPurity" else value
    for label, field in WET_FROM_CONTROL.items():
        value = clean(ctrl.get(label))
        if value is not None:
            item[field] = value
    made = []
    if fabricate:
        for field, value in WET_FABRICATE.items():
            if field not in item:
                item[field] = value
                made.append((field, value))
    return item, made


def build_bio_item(sample: dict, fabricate: bool = True):
    """qc.txt → bioinfo_qc 推送行；返回 (item, 编造的项)"""
    own = parse_qc(sample["qc_txt"])
    item = {"subbarcode": sample["subbarcode"]}
    for label, field in BIO_FROM_SAMPLE.items():
        value = clean_num(own.get(label))
        if value is not None:
            item[field] = value
    made = []
    if fabricate:
        for field, value in BIO_FABRICATE.items():
            if field not in item:
                item[field] = value
                made.append((field, value))
    return item, made


# ---------------------------------------------------------------- 核验
def verify(subbarcode: str):
    sql = [
        ("analysis_data", "SELECT analysis_id,analysis_date,product,drive_status FROM analysis_data "
                          "WHERE subbarcode=%s"),
        ("cms_sample", "SELECT id,patient_name,sex,cancer_type,erp_test_name,customer_name,tenant_id "
                        "FROM cms_sample WHERE subbarcode=%s"),
        ("wet_lab_qc", "SELECT id,patho_tumor_purity,dna_total_ng,dna_degradation,pre_lib_total_ng,"
                       "dna_total_ng_normal,pre_lib_total_ng_normal,status,tenant_id FROM wet_lab_qc "
                       "WHERE subbarcode=%s"),
        ("bioinfo_qc", "SELECT id,raw_base,average_depth,coverage_target,mapping_rate,raw_q30,status,tenant_id "
                       "FROM bioinfo_qc WHERE subbarcode=%s"),
    ]
    out = {}
    with conn(**LOCAL) as cn, cn.cursor(pymysql.cursors.DictCursor) as cur:
        for table, query in sql:
            cur.execute(query, (subbarcode,))
            out[table] = cur.fetchall()
    return out


# ---------------------------------------------------------------- 接口现状
def check_interfaces(token: str):
    print("\n== 接口现状实探 ==")
    for path, perm in ((WET_RECEIVE, "qc:wetLabQc:receive"), (BIO_RECEIVE, "qc:bioinfo:receive")):
        status, body = post_receive(path, [], token)
        ok = body.get("msg") == "items 不能为空"
        print("  %-26s %-24s http=%s msg=%s  → %s"
              % (path, perm, status, body.get("msg"), "已具备（token 通过、只差载荷）" if ok else "异常"))
    print("  %-26s %-24s %s" % ("/report/sampleInfo", "report:sampleInfo:add",
                                "已存在（登录态新增，非上游推送）"))
    print("  %-26s %-24s %s" % ("/report/sampleInfo/importData", "report:sampleInfo:import",
                                "已存在（Excel 导入，122 列按序）"))
    print("  缺：样本信息的『上游推送接收』接口 —— 本脚本第①步直接写 cms_sample 兜住这一步")


# ---------------------------------------------------------------- main
def main():
    parser = argparse.ArgumentParser(description="模拟 CRM / LIMS / 生信 三条上游把数据推进本系统")
    parser.add_argument("--apply", action="store_true", help="真写（默认干跑）")
    parser.add_argument("--barcode", action="append", help="只处理指定样本编号（可多次）")
    parser.add_argument("--tenant", default=DEFAULT_TENANT, help="样本归属租户，默认 %s（圣域）" % DEFAULT_TENANT)
    parser.add_argument("--customer-name", default=DEFAULT_CUSTOMER_NAME,
                        help="cms_sample.customer_name（生信接收靠它解析药企），默认 %s" % DEFAULT_CUSTOMER_NAME)
    parser.add_argument("--no-fabricate", action="store_true", help="缺项不编值，qc.txt 没有的就不写")
    parser.add_argument("--data-root", default=DATA_ROOT, help="上传根目录，默认 %s" % DATA_ROOT)
    parser.add_argument("--check-interfaces", action="store_true", help="只探接收接口")
    args = parser.parse_args()

    token = api_token()
    if args.check_interfaces:
        check_interfaces(token)
        return

    samples = discover_samples(args.data_root)
    if args.barcode:
        wanted = set(args.barcode)
        samples = [s for s in samples if s["barcode"] in wanted or s["subbarcode"] in wanted]
    if not samples:
        sys.exit("没在 %s 下扫到样本（期望 <date>/<product>/<barcode>/<barcode>T/10.Upload）" % args.data_root)

    print("模式：%s   租户：%s   样本数：%d\n" % ("APPLY（真写）" if args.apply else "干跑（--apply 才写）",
                                              args.tenant, len(samples)))
    cols = local_columns("cms_sample")

    summary = []
    for s in samples:
        bc = s["subbarcode"]
        print("=" * 78)
        print("样本 %s   产品=%s   分析日期=%s" % (bc, s["product"], s["analysis_date"]))

        # ---------- ① 样本信息：LIMS omics.sample_file → 本系统 cms_sample
        lims = fetch_lims_sample(bc)
        if not lims:
            print("  [①] LIMS 里查不到该条码 → 跳过整条样本")
            continue
        row, used, unmapped = build_sample_row(lims, args.tenant, args.customer_name, cols)
        outcome = upsert_sample(row, args.apply)
        print("  [①] 样本信息 ← %s:%s/%s.sample_file   →  cms_sample (%s)" % (
            LIMS["host"], LIMS["port"], LIMS["database"], outcome))
        print("      写入 %d 列：%s" % (len(used), ", ".join("%s=%s" % (d, str(v)[:22]) for _, d, v in used)))
        print("      未映射（保留 NULL）%d 列：%s" % (len(unmapped), ", ".join(unmapped)))

        # ---------- ② 湿实验质控：qc.txt + G.qc.txt → /qc/wetLabQc/receive
        wet, made_wet = build_wet_item(s, fabricate=not args.no_fabricate)
        if args.apply:
            status, body = post_receive(WET_RECEIVE, [wet], token)
            print("  [②] 湿实验质控 → POST %s  http=%s code=%s inserted=%s updated=%s skipped=%s"
                  % (WET_RECEIVE, status, body.get("code"),
                     (body.get("data") or {}).get("inserted"), (body.get("data") or {}).get("updated"),
                     (body.get("data") or {}).get("skipped")))
        else:
            print("  [②] 湿实验质控 → POST %s\n      %s" % (WET_RECEIVE, json.dumps(wet, ensure_ascii=False)))
        if made_wet:
            print("      [编] qc.txt 没有的项，按口径补值：%s"
                  % ", ".join("%s=%s" % (f, v) for f, v in made_wet))

        # ---------- ③ 生信质控：qc.txt → /qc/bioinfo/receive
        bio, made_bio = build_bio_item(s, fabricate=not args.no_fabricate)
        if args.apply:
            status, body = post_receive(BIO_RECEIVE, [bio], token)
            print("  [③] 生信质控   → POST %s  http=%s code=%s inserted=%s updated=%s skipped=%s"
                  % (BIO_RECEIVE, status, body.get("code"),
                     (body.get("data") or {}).get("inserted"), (body.get("data") or {}).get("updated"),
                     (body.get("data") or {}).get("skipped")))
        else:
            print("  [③] 生信质控   → POST %s\n      %s" % (BIO_RECEIVE, json.dumps(bio, ensure_ascii=False)))
        if made_bio:
            print("      [编] qc.txt 没有的项，按口径补值：%s"
                  % ", ".join("%s=%s" % (f, v) for f, v in made_bio))

        summary.append(bc)

    if args.apply:
        print("\n" + "=" * 78)
        print("落库核验")
        for bc in summary:
            print("-- %s" % bc)
            for table, rows in verify(bc).items():
                for r in rows or [{"(无记录)": ""}]:
                    print("   %-14s %s" % (table, json.dumps(r, ensure_ascii=False, default=str)))


if __name__ == "__main__":
    main()
