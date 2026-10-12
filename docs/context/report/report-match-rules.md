# 报告解读 · 位点匹配规则（对齐 report_en7 生产口径）

> 目的：把 report_en7（生产在跑的 Java 实现）里的**位点→知识库→药物证据**规则完整抽出来，作为
> biotech-plus「报告解读 → Tab④ 报告预览」匹配逻辑的改造依据。
> 出处：`~/project/report-system`（report_en7 服务端源码），下文所有规则都标了文件:行号。
> 本文只描述规则，不含代码改动；确认后再动实现。

---

## 0. 一句话结论

en7 的匹配是 **5 个环节**：

```
位点(gene, variant, ori_variant)
  → ① 解析成知识库节点 gene_variant_id（四级兜底 + 功能判定 effect）
  → ② 节点集合 = 自身 + 一层父级（Exon 级 / Active·Inactive 功能级）
  → ③ 癌种范围 = 本癌种 + 祖先链 + 子孙链（再按性别 / 实体瘤·血液瘤剔除）
  → ④ 证据取数 = Approved 且 关系∈{敏感性增加,有益的,抗药性} 且 分期>非药物研究 且 类型=治疗 且 药物未作废 且 癌种已审核
  → ⑤ 输出 = 获益药(A/B/C/D 级) / 耐药药 / 临床试验 / 失活·扩增·缺失描述模板
```

**我当前实现（biotech-plus InterpretationPreviewServiceImpl）只做了其中一部分**：
精确节点匹配（没有四级兜底）、只用祖先癌种（没有子孙链、没有性别/瘤种剔除）、
证据查询**没有加任何过滤条件**（Approved / 关系 / 分期 / 类型 / 药物状态 / 癌种状态全缺）、
没有 effect（激活/失活）与父级节点兜底。差异清单见第 5 节。

---

## 1. 出处索引（report_en7）

| 规则 | 出处 |
| --- | --- |
| 位点→节点ID 四级兜底 | `src/com/novo/report/service/impl/ReportCrServiceImpl.java:578-600`（`getMutationID`） |
| fs 归一化（截到最后一个数字） | 同上 `:547-557`（`getVariant`） |
| 融合 KDD 判定（基因→激酶结构域外显子区间） | `src/com/novo/report/service/impl/VariantServiceImpl.java:194-235`（`isFusionKDDVariant`） |
| 节点集合 = 自身 + 一层父级 | `ReportCrServiceImpl.java:602-617`（`getMutIdList`）+ `AnalysisReportDao.java:184-187`（`getParentMutationId`） |
| 父级为空时的失活兜底（肠癌分支） | `src/com/novo/report/service/impl/ComplexMutationServiceImpl.java:353-369`（`getParentMutId`） |
| 父级名称查询 | `AnalysisReportDao.java:195-196`（`getParentVariant`，视图 `nkb.gene_variant_parent_vw`） |
| 癌种范围（本+祖先+子孙） | `ComplexMutationServiceImpl.java:753-766`（`getDiseaseList`） |
| 性别 / 实体瘤·血液瘤剔除 | `ComplexMutationServiceImpl.java:789-827`（`solidTumorFiltration`） |
| 证据过滤条件（视图 where） | `nkb.snkb_var_drug_anno_view` 视图定义（见 4.1） |
| 本癌种证据取数 | `AnalysisReportDao.xml:341-358`（`getDrugListByIdList`） |
| 其他癌种 A 类药（获批上市） | `AnalysisReportDao.xml:571-602`（`getOtherADrugListByIdList`） |
| 失活/扩增/缺失描述模板 | `ReportCrServiceImpl.java:170-178` |
| 历史/审核基准迁移（键与比较口径） | `scripts/migrate_en7_to_drug_audit_history.py` |

---

## 2. NKB 对象清单

| 对象 | 用途 | 关键列 | 本地可用性 |
| --- | --- | --- | --- |
| `gene_variant` / `gene_variant_evw` | 位点节点、父级、effect、审核状态 | `gene_variant_id, gene_id, gene_variant, ori_variant, effect_id, checking_status_id` | 基表 ✓；`gene_variant_evw` 因本机 `only_full_group_by` 报错，用等价自建查询替代 |
| `gene_variant_parent` / `gene_variant_parent_vw` | 父子关系（9264 条；全部注释中 4197 条挂在父节点上） | `gene_variant_id, parent_variant_id` | ✓ |
| `variant_drug_annotation` / `variant_drug_annotation_evw` | 用药证据 | `gene_variant_id, drug_id, disease_id, relationship_id, evidence_type_id, evidence_phase_id, evidence_ranking_id, mutation_type, checking_status_id, annotation_chinese` | ✓（用基表 + 显式过滤；视图不含药物/癌种状态列，故不用） |
| `drug` | 药物名、状态 | `drug_name_chinese, checking_status_id` | ✓ |
| `disease` + `disease_hierarchy` | 癌种、祖先/子孙 | `do_id, disease_name_chinese, checking_status_id`；`disease_hierarchy(do_id, is_a)` | ✓ |
| `approved_drug` | 获批机构（取 approving_agency） | `drug_id, disease_id, approving_agency` | ✓ |
| `relationship` | 用药关系字典 | 1 敏感性增加 / 2 敏感性降低 / 3 未明 / 4 有益的 / 5 无益的 / 6 抗药性 | ✓ |
| `evidence_type` | 证据类型字典 | 1 诊断 / **2 治疗** / 3 预后 / 4 风险 | ✓ |
| `evidence_phase` | 证据分期字典 | 11 非药物研究 / 12 临床前研究 / 13-22 临床试验各期 / 23 指南推荐 / 24 获批上市 | ✓ |
| `effect` | 功能判定字典 | 1 激活 / 2 失活 / 3 未知 / 4 无影响 | ✓ |
| `variant_drug_anno_trial` / `clinical_trial` | 临床试验 | `annotation_id, clinical_trial_id` | ✓ |

`gene_variant.effect_id` 分布：失活 5296 / 激活 3000 / 未知 2124 / 无影响 84。
`variant_drug_annotation.mutation_type` 分布：S（体细胞）8107 / G（胚系）261 / S/G（通用）485。

---

## 3. 环节①：位点 → 知识库节点（四级兜底）

出处：`ReportCrServiceImpl.getMutationID(gene, variant)`（578-600 行），伪代码：

```
id = query(gene, variant, checking_status='Approved')            # ① 精确
if id == null and variant 含 "fs":
    tmp = getVariant(variant 前缀) + "fs"                        # ② fs 归一：截到 fs 前最后一个数字
    id = query(gene, tmp)                                        #    例 p.R611Qfs*12 → R611fs
if variant 含 "Fusion" and isFusionKDDVariant(ori_variant):
    id = query(gene, "KDD Mutation")                             # ③ 融合落在激酶结构域 → KDD 节点
if id == null and variant 含 fs / * / + / - 且不含 "Fusion":
    id = query(gene, "Inactive Mutation")                        # ④ 截断类突变 → 失活节点
return id
```

- `getVariant(v)`：从右往左找第一个数字，截到它为止（`ReportCrServiceImpl:547-557`）。
- `isFusionKDDVariant(oriVariant)`：拿「基因-基因 Fusion 首字母+起始外显子:首字母+结束外显子」拼串比对
  （`VariantServiceImpl:194-235`），内置 20 个基因的激酶结构域外显子区间，例如
  `KIT exon11–exon20`、`EGFR exon18–exon25`、`ALK exon20–exon28`、`RET exon12–exon18`、`MET exon16–exon21`、
  `BRAF exon12–exon18`、`FLT3 exon14–exon23`、`ERBB2 exon19–exon25`、`ROS1 exon36–exon42`、`PIK3CA exon14–exon21` 等。
- 例：KIT V559D → ① 精确命中节点 `gene_variant_id=2627`；effect = 激活。

---

## 3.1 专题：`Active Mutation` / `Inactive Mutation` 到底怎么判断

**结论：这不是算法推断，只有「失活」有一条字符串兜底规则；「激活」完全靠知识库数据。**

### 3.1.1 代码里的事实

| 事实 | 证据 |
| --- | --- |
| `Active Mutation` 在 en7 代码中**零引用** | `grep -rn "Active Mutation" src WebContent scripts` → 0 命中 |
| `effect` 字典（激活/失活）在 en7 代码中**零使用** | `grep -rn "effect_id\|\.effect\|effect_chinese" src` → 0 命中 |
| `Inactive Mutation` 只有 4 处引用，都是同一条规则 | `ReportCrServiceImpl.java:593-597`、`ComplexMutationServiceImpl.java:353-369`、`VariantUtils.java:58-60`（已注释的旧版） |

失活兜底规则（两处实现逻辑一致）：

```
variant 含 "fs" | "*" | "+" | "-"   且   variant 不含 "Fusion"
        → getMutationId(gene, "Inactive Mutation")
```

含义：**精确匹配失败后**，若位点形似截断型突变（移码/终止/插入/缺失），按失活处理。
它不是「判断这个位点失活」，而是「知识库没收录、形态像截断 → 借用基因级失活节点」。

`Active Mutation` 之所以能出现在结果里，唯一路径是：位点被 NKB 收录，且 `gene_variant_parent`
把它挂到了 `Active Mutation` 节点下，再经「自身 + 一层父级」带出父节点证据。en7 不做
「这个点突变是否激活」的推断。

### 3.1.2 数据里的事实（NKB 自带标注）

| 父级节点 | 涉及基因数 | 节点行数 |
| --- | --- | --- |
| `Active Mutation` | 70 | 70 |
| `Inactive Mutation` | 153 | 153 |
| `Act Mutation` | 9 | 9 |
| `Truncating Mutation` | 6 | 6 |

子位点的 `effect`（1 激活/2 失活/3 未知/4 无影响）分布 —— 说明 NKB 自己标了功能且与父级基本自洽：

| 挂在 | 激活 | 失活 | 未知 | 空 |
| --- | --- | --- | --- | --- |
| `Active Mutation` 下 | 1772 | 38 | 4 | 1 |
| `Inactive Mutation` 下 | 19 | 4463 | 15 | 1 |

抽样形态：
- `Inactive Mutation` 下既有截断型（`K267fs*9`、`R130fs`），也有失活点突变（`R882S/R882C/R882H/R361C/D537Y`）
- `Active Mutation` 下多为经典激活点突变（`L576P/K642E/D816H/Q61K/Q61R`）

### 3.1.3 对我们样本的两侧边界

| 位点 | 是否 NKB 收录 | 形态 | 结果 |
| --- | --- | --- | --- |
| `KIT V559D` | 收录（节点 2627，effect=激活） | 错义 | 走父级 `Active Mutation` 多拿 1 条证据（阿伐替尼） |
| `KIT C58R` | **未收录**（0 命中） | 错义、外显子 2（不在 KIT 激酶结构域 exon11–20） | 三条兜底全不适用 → 无证据；只能**人工改靶**指定父级 |

### 3.1.4 「失活兜底」的触发条件（4 条同时满足）

代码：`ReportCrServiceImpl.getMutationID`（578-600 行，CR 主链路），判定字段是 **`variant`（短位点串）**，
不是 `ori_variant`。

| # | 条件 | 说明 |
| --- | --- | --- |
| 1 | 精确匹配先失败 | `getMutationId(gene, variant)` 返回 null（NKB 无该 gene+variant 的 Approved 节点） |
| 2 | 中间两档也没救回来 | ② `variant` 含 `fs` → 截到 fs 前最后一个数字再查（`getVariant`）；③ `variant` 含 `Fusion` 且落在激酶结构域 → 查 `KDD Mutation`。执行完 `mutationId` 仍为 null |
| 3 | 字符串形态命中 | `variant` 含 `fs` **或** `*` **或** `+` **或** `-`，**且不含** `Fusion` |
| 4 | 该基因在 NKB 里有失活节点 | `getMutationId(gene, "Inactive Mutation") != null`；否则 `mutationId` 保持 null＝不触发 |

另一处实现（`ComplexMutationServiceImpl.getParentMutId` 353-369，用于父级名称列表）顺序略有不同：
**先**取 NKB 父级列表 `getParentVariant(gene, variant)`，**只有当它为空**时，才做同样的字符串判断并把
`Inactive Mutation` 追加为兜底父级。

触发后的走向：`mutationId` = 该基因的 `Inactive Mutation` 节点 → 节点集合 = 自身 + 一层父级
→ 按癌种范围 + en7 证据过滤查注释。**拿到的是「基因级失活证据」，不是该位点特异证据。**

### 3.1.5 这条兜底的实际效果（本地 nkb 实测）

| 指标 | 数值 |
| --- | --- |
| 有 `Inactive Mutation` 节点的基因数 | **153** |
| 这些节点上符合 en7 过滤的用药证据 | **950 条** |
| 单基因证据数 TOP | BRCA2 167 / BRCA1 154 / TP53 71 / PALB2 39 / ATM 31 / RAD51C 23 / BRIP1 23 / CHEK2 21 |
| 例（BRCA1 失活节点） | 他拉唑帕利（卵巢癌 临床试验I期 / 乳腺癌 临床研究 / 前列腺癌 / 肺鳞癌）、瑞波西利（HER2阴性乳腺癌） |

**我们样本为什么触发不了（双重不满足）**：
- 条件 4 不满足：**KIT 没有 `Inactive Mutation` 节点**。KIT 的功能级节点只有
  `Active Mutation`(激活)、`Exon8/9/11/13/14/17/18 Mutation`、`Exon11 Deletion Mutation`(激活)、
  `Mutation`(未知)、`No Mutation Detected`。
- 条件 3 不满足：两个位点 `variant` 是 `C58R` / `V559D`，都是错义，不含 fs / \* / + / -。

同一个形态判断还用于**描述文案**（`ReportCrServiceImpl:170-178`）：fs / \* / + / - 且非 Fusion
→ 「该变异为失活突变，可能会导致蛋白功能缺失。」——匹配与文案共用同一套字符串判定。

结论：**若要求「激活/失活」判断，来源只能是 NKB（`effect_id` + 父级节点）或人工改靶，
不能由我们自行推断**；代码层面唯一能自己做的只有 en7 那条「截断型 → 失活」的兜底。

---

## 4. 环节②③④：节点集合 / 癌种范围 / 证据取数

### 4.1 节点集合 = 自身 + 一层父级

```
mutationIdList = [mutationId] + select parent_variant_id from gene_variant_parent where gene_variant_id = mutationId
```
出处 `ReportCrServiceImpl:602-617`。注释写明「一个突变可能有多个父级」，**只取一层**。

实测（我们的样本）：
```
KIT V559D(2627) → 父级 [2807 Exon11 Mutation, 4877 Active Mutation]
```

### 4.2 癌种范围 = 本癌种 + 祖先链 + 子孙链

出处 `ComplexMutationServiceImpl.getDiseaseList`（753-766 行）：
- `parentdiseaseIdList` = 本癌种 + 全部祖先
- `diseaseIdList` = 全部祖先 + 本癌种 + **全部子孙**

再按 `solidTumorFiltration(gender, diseaseIdList)`（789-827 行）剔除：
- 男性 → 剔除 `120`（女性生殖器官肿瘤）及其全部子孙；女性 → 剔除 `3856`（男性生殖器官肿瘤）及其子孙
- `diseaseIdList` 含 `10000003`（实体瘤）→ 剔除 `2531`（血液肿瘤）及其子孙；反之亦然

### 4.3 证据取数（本癌种 / 其他癌种分开）

视图 `snkb_var_drug_anno_view` 的内置过滤（这是「什么算一条可用证据」的权威定义）：

| 条件 | 说明 |
| --- | --- |
| `variant_drug_annotation.checking_status_id = 2` | 注释已审核 Approved |
| `relationship_id IN (1,4,6)` | 敏感性增加 / 有益的 / 抗药性（排除 2 敏感性降低、3 未明、5 无益的） |
| `evidence_phase_id > 11` | 排除「非药物研究」 |
| `evidence_type_id = 2` | 只要「治疗」类证据 |
| `drug.checking_status_id <> 3` | 药物未作废 |
| `disease.checking_status_id = 2` | 癌种已审核 |

本癌种证据（`AnalysisReportDao.xml:341-358`）：
```sql
FROM nkb_var_drug_anno_view a
LEFT JOIN nkb.approved_drug b ON a.drug_id = b.drug_id AND a.anno_disease_id = b.disease_id  -- 取 approving_agency
WHERE a.gene_variant_id IN (mutationIdList)
  AND a.anno_disease_id IN (diseaseIdList)
  AND a.lang = 1
  AND a.mutation_type IN (#{mutation_type}, 'S/G')     -- 体细胞=S, 胚系=G, 通用=S/G
```

其他癌种的 A 类药（`AnalysisReportDao.xml:571-602`）：
```sql
WHERE a.evidence_phase_id = 24                    -- 获批上市
  AND a.gene_variant_id IN (mutationIdList)
  AND a.anno_disease_id NOT IN (diseaseIdList)
  AND a.anno_disease_id NOT IN (sonIdList)        -- 性别/瘤种剔除集合
  AND a.mutation_type IN (#{mutation_type}, 'S/G')
```

临床试验：`getClinicalTrial(annotation_id, drug_name, parentdiseaseIdList, lang)`（`ReportCrServiceImpl:1268+`）。
知识库新鲜度：`getUpdateByIdList` 用 `evidence_phase_id > 22 or disease_id in (...)` 取 `max(update_date)`（`AnalysisReportDao.xml:359-371`）。

### 4.4 药物分级 A/B/C/D（`getDrugLevel`，完整映射）

出处：`ReportCrServiceImpl.getDrugLevel` 1354-1395 行；输出映射在 `fetchNkbDrugInfo` 719-729 行。
`approve_range` 是内部等级码：**1-4 = 获益 A/B/C/D，5-8 = 耐药 A/B/C/D，9 = 其他（不输出）**。

**获益药（`relationship` ≠ `Resistant`）**

| `evidence_phase_id` | 分期含义 | 等级码 | 输出字段 |
| --- | --- | --- | --- |
| > 22 | 23 指南推荐 / 24 获批上市 | 1 | `drugsA` |
| 18–22 | II 期 / II-III 期 / III 期 / IV 期 | 2 | `drugsB` |
| 16–17 | I 期 / I-II 期 | 3 | `drugsC` |
| = 14（临床研究） | 看 `getGive`：2 → 3；1 → 4；其他 → 9 | 3 / 4 / — | `drugsC` / `drugsD` |
| 12–13 | 临床前研究 / 临床试验0期 | 4 | `drugsD` |
| ≤ 11 或 give=0 | 非药物研究等 | 9 | 不输出 |

**耐药药（`relationship` = `Resistant` 且 `evidence_phase_id ≥ 12`）**

| `evidence_phase_id` | 等级码 | 输出字段 |
| --- | --- | --- |
| > 22 | 5 | `resistant_drugsA` |
| 18–22 | 6 | `resistant_drugsB` |
| 16–17 | 7 | `resistant_drugsC` |
| = 14（临床研究） | `getGive`：2 → 7；1 → 8；其他 → 9 | `resistant_drugsC` / `resistant_drugsD` |
| 12–13 | 8 | `resistant_drugsD` |
| ≤ 11 | 9 | 不输出 |

`getGive(annotation_id, parentdiseaseIdList)`（1409-1420 行）——只对 `evidence_phase_id = 14`（临床研究）生效：
1. 该注释在**父级癌种**下有临床试验（`getClinicalNumber > 0`）→ `"2"`（升为 C 级 / 耐药 7）
2. 否则 `has_previous_clinical_result` 为空或 `'Y'` → `"1"`（落在 D 级 / 耐药 8）
3. 否则 → `"0"`（→ 等级 9，不输出）

其它相关处理：
- `drugList` 先按 `evidence_phase_id` **降序**排（678 行）
- `filterDrugList(drugList)`：同一药物在多癌种下有多条时**只保留最高等级**那条（745 行注释）
- `setOrderNum`（251+）排序：有靶向药的位点排最前（`resultTypeDesc = 靶向药物` +70000/+90000，复杂突变 +50000）

### 4.5 其他癌种的获批药 → 作为 C 级药输出

出处：`fetchNkbDrugInfo` 692-712 行，注释原文「**添加其他癌种A级证据，并输出为C级药物**」。

```java
// 其他癌种：evidence_phase_id = 24（获批上市）、gene_variant_id ∈ mutationIdList、
//           anno_disease_id NOT IN diseaseIdList、且 NOT IN sonIdList
otherADrugList = getOtherADrugListByIdList(mutationIdList, diseaseIdList, lang, mutation_type, sonIdList);

// 去重：本癌种已有 A/B 级（approve_range ∈ {1,2,5,6}）的同一个 drug_id，不再重复输出
keep = otherADrugList where drug_id NOT IN {本癌种 approve_range ∈ 1,2,5,6 的 drug_id}

// 映射为 C 级
if (relationship == "Resistant")  approve_range = "7"   // resistant_drugsC（耐药C）
else                              approve_range = "3"   // drugsC（获益C）
drugList.addAll(keep)
```

要点：**其他癌种的获批药无论原本是几类证据，一律降格成 C 级（耐药关系则进耐药C）**；
本癌种已经有 A/B 级的同一药物不重复出现。

### 4.6 位点（变异）分级 = I 类 / II 类 / III 类

出处：`PyReportServiceImpl` 1104-1108 行（注释原文「**有A、B药物为I类，C、D为II类**」）、
1397 / 2064 行（`III类`）、1730-1734 行；60 基因重肿模板另有一套等价的 `variationGrading1/2/3`（910-921 行）。

| 分级 | 判定条件 | 说明 |
| --- | --- | --- |
| `I类` | A 级获益药 **或** B 级获益药 **或** 耐药A/耐药B 非空 | 有高等级用药证据 |
| `II类` | 有用药证据，但只有 C/D 级 | 有药但等级低 |
| `III类` | 无用药证据、无临床试验，但有 VUS 记录（`rpUnknownVar`） | 临床意义未明 |
| `-` | 基因 = `多靶点循证`（共突变组合位点） | 不参与分级 |

输出落点（`PyReportServiceImpl` 5282-5320）：`变异分级提示` / `体细胞变异分级提示` /
`vus分级提示`（III 类，没有用药）/ `共突变（多靶点）分级提示`，按 `variationClass` 分组渲染。

> 注意区分：`PyReportServiceImpl:2419` 的「只保留 1 2 等级」是**化疗药物证据等级**（`evidence` 字段 1-4），
> 与上面的变异分级不是一套，别混用。

### 4.7 与 I/II/III 类平行的「结果类型」

`resultTypeVal` / `resultTypeDesc`：有位点命中用药证据 → `1 / 靶向药物`；
无证据走 `getUnknownVarInfo` → `未知临床意义`（`ReportCrServiceImpl:735-745`、`269-271`）。

---

## 5. 环节⑤：失活 / 扩增 / 缺失描述模板

出处 `ReportCrServiceImpl:170-178`：

| 条件 | 输出文案 |
| --- | --- |
| `variant = "Amplification"` | 该变异为基因扩增，可能导致蛋白表达增加。 |
| `variant = "Loss"` | 该变异为基因缺失，可能导致蛋白表达增加。（原文如此） |
| `variant` 含 fs / * / + / - 且不含 "Fusion" | 该变异为失活突变，可能会导致蛋白功能缺失。 |
| 其他 | 该突变临床意义未明，若导致蛋白功能异常，可能影响下游信号通路，参与肿瘤发生发展。 |

同文件 `:175` 附近还有：`if (variant.indexOf("fs") > -1 || ...)` 判断统一用于「是否失活」，
与 ③④ 节点兜底用的是同一套字符串判定（fs / * / + / -）。

---

## 6. 与当前 biotech-plus 实现的差异对照

| 环节 | report_en7 | 我现在的实现 | 影响 |
| --- | --- | --- | --- |
| ① 节点解析 | 四级兜底（精确 / fs 归一 / KDD / Inactive）+ effect | 体细胞只有精确（`gene_variant` 相等 / `ori_variant` 相等 / `LIKE '%(variant)'`）；胚系额外做了 Class5/Class4 兜底（**非 en7 口径，是本项目自选**） | 截断突变、融合 KDD 类位点会「无证据」 |
| ② 节点集合 | 自身 + 一层父级 | 只有自身 | 漏掉挂在 Exon/Active·Inactive 父节点上的证据 |
| ③ 癌种范围 | 本癌种 + 祖先 + 子孙，且性别/瘤种剔除 | 只有祖先链 | 范围偏窄；男性样本可能给出女性生殖器肿瘤证据 |
| ④ 证据过滤 | Approved + 关系{1,4,6} + 分期>11 + 类型=2 + 药物/癌种状态 + mutation_type(S/G) | **一个都没加** | 证据条数与内容口径不同，可能混入未审核 / 非治疗 / 耐药关系不当的行 |
| ⑤ 描述模板 | 失活/扩增/缺失/未明四段固定文案 | 没有 | 模板里没有可用的解释文案 |
| 功能判定 | `effect`（激活/失活/未知/无影响）+ 父级功能级节点 | 没有 | 无法输出「激活突变」语境 |

---

## 7. 实测对照（我们样本的真实数据）

### 7.1 KIT V559D（体细胞，报出）

节点链：`V559D(2627)` + 父级 `Exon11 Mutation(2807)`、`Active Mutation(4877)`。

| 口径 | 条数 | 明细 |
| --- | --- | --- |
| 我现在（精确节点 + 祖先癌种，无过滤） | 4 | 实体瘤：伊马替尼 / 氟马替尼 / 普纳替尼（分期「体外研究」类）+ 1 条 |
| en7 过滤后（**未分级前**） | 5 | V559D 节点 4 条 + 父级 Active Mutation 1 条（阿伐替尼） |
| en7 **最终输出**（走完 `getDrugLevel` 分级） | **4** | 都是 D 级：实体瘤 · 临床前研究（伊马替尼/氟马替尼/普纳替尼/达沙替尼） |

⚠️ 关键更正（我第一版文档与口头结论写错过，这里按实测修正）：
阿伐替尼那条虽然过了证据过滤，但它的 `evidence_phase_id = 14`（临床研究）需要走 `getGive`：
该注释关联的试验既非「招募中」、`has_previous_clinical_result = 'N'` → `give = "0"` → `getDrugLevel` 返回 **9（其他）**，
而 9 只在 `drugsA..D` / `resistant_drugsA..D` 之外，**en7 直接不输出**。
所以 en7 最终也只输出 4 条（与我现在实现一致）；「父级节点能补证据」这件事本身成立，
但能否输出还要过 give/分级这一关。

实测我实现的结果（`/tmp/bp_page5_en7_probe.py`）：
```
KIT V559D → 命中节点=V559D effect=激活 位点分级=II类 inNkb=true MATCHED
  分组名串 drugsD = 普纳替尼;达沙替尼;伊马替尼;氟马替尼
  证据 4 条（[D/4] 实体瘤·临床前研究·敏感性增加）
KIT C58R  → inNkb=false 无证据 + 未明文案
BRCA1 Gln1756Profs*74 → 命中节点=Inactive Mutation（fs 兜底）effect=失活 位点分级=II类
  证据 19 条 = 本癌种（实体瘤）4 条 + 其他癌种获批降格 C 15 条
  描述：该变异为失活突变，可能会导致蛋白功能缺失。
```

### 7.2 KIT C58R（体细胞，报出）

- NKB 里 `gene_variant` / `ori_variant` 都查不到 C58R（0 命中），en7 的四级兜底对它也不生效
  （C58R 不含 fs/*/+/-，不是 Fusion，基因级「Inactive Mutation」也不适用）。
- 结论：这类「知识库没有的位点」在 en7 口径下同样输出「无证据」+ 未明描述模板；
  要出证据只能靠人工改靶指定父级节点（设计书 `file_*.parent_mutation_id` 的用途）。

### 7.3 BRCA1 c.5266dup（胚系，报出）

- 胚系与体细胞**走同一套匹配**（同一套节点四级兜底 + 父级链 + 癌种范围 + 证据过滤），
  唯一区别是 `mutation_type` 传 `G`（体细胞传 `S`），即 `mutation_type IN ('G','S/G')`。
- **更正一处我先前的说法**：en7 Java 代码里**没有**「Class5(致病)」这类节点匹配
  （grep `Class4/Class5/clnsig/clinical_significance` 在 `ReportCrServiceImpl`、`GeneticMarkerVwServiceImpl` 均 0 命中）；
  NKB 里 `gene_variant = 'Class4(疑似致病)/Class5(致病)'` 是知识库自身的数据形态，被我上一版实现拿来当胚系兜底，
  属于**我自己的选择**，不是 en7 口径。en7 里和「致病性」相关的判断出现在面板/HRD 规则里，用的是
  `clinical_significance` 文本（`GeneMarkerVwController.java:538` 等：「致病性变异」/「可能致病性变异」）。
- 我们的样本 BRCA1 c.5266dup 在 NKB 无精确节点、也不含 fs/*/+/-（不是 Inactive 兜底对象），
  按 en7 口径应输出「无证据 + 未明文案」；用我现在的 Class5 兜底口径则会命中
  乳腺癌症 / 卵巢癌 / 腹膜癌症 / 输卵管癌 的注释，但那些**不在结直肠癌链内**，本癌种证据仍为 0。

---

## 8. 历史 / 冻结语义对照

| 维度 | report_en7 | 设计书（biotech-plus 目标） |
| --- | --- | --- |
| 结果落地 | 本地表 `rp_var_drug_en7`（按 record_id 复写）+ 知识库更新时间戳判定是否刷新 | `history_somatic` / `history_germline` 按 `match_key`(SHA-256) 冻结，命中原样复用 |
| 匹配键口径（en7 迁移脚本） | `source_key = sha256(SOURCE_TYPE|gene|variant|disease_id|lang|gender)`；`variant_key = sha256(gene|variant)` | 基因 + 规范位点 + 原始位点 + 癌种ID/名称 + 性别 + 客户 + 产品 + 来源类型 + 突变类型 + 合子 + 人工父级 |
| 文本规范化 | 去首尾/压缩空白、`lower()`、药名去 `* # %` | 设计书未细说，我实现为 trim 后拼接 |
| 比较口径 | `compare_hash = sha256(benefitDrugs/resistantDrugs/clinicalTrials 排序去重后的 JSON)` | `match_result` 冻结整条 Item JSON |

含义：en7 的「复用」靠本地表 + 时间戳，设计书的「冻结」靠 match_key；**两者都要求同样的匹配输入**，
所以第 3~5 节的规则可以直接搬，历史语义仍按设计书的 match_key 走。
（en7 迁移脚本还区分 `benefitDrugs` / `resistantDrugs` / `clinicalTrials` 三类，A/B/C/D 级；
若后续要「审核基准」，可照这个结构扩展 `match_result`。）

---

## 9. 待确认问题（需要你拍板）

1. **节点四级兜底**：`fs 归一` / `KDD Mutation` / `Inactive Mutation` 三条兜底要不要全上？
   （都会让更多位点从「无证据」变成「有证据」，但它们匹配到的是**父级/功能级节点**，不是位点本身）
   另：胚系要不要保留我现在这版「Class5/Class4 兜底」（en7 没有这条；去掉的话 BRCA1 类位点在本癌种下必然 0 条）。
2. **父级链层级**：en7 只取一层；是否也只取一层（我倾向一致：一层）。
3. **癌种范围**：是否按 en7 用「本癌种 + 祖先 + 子孙」，并加性别剔除（男剔 120、女剔 3856）
   与实体瘤↔血液瘤互斥（10000003 ↔ 2531）？我现在的「只取祖先」会漏掉子癌种证据。
4. **证据过滤**：Approved + 关系{1,4,6} + 分期>11 + 类型=2 + 药物未作废 + 癌种已审核 + `mutation_type∈{S或G, S/G}`。
   加了会让条数变化（V559D 例：4→5 但内容口径更严），要不要全上？
5. **展示范围**：是否也要「其他癌种 A 类药（`evidence_phase_id=24` 获批上市）」单独一组展示？
   （en7 会输出，我现在只出本癌种证据）
6. **描述模板**：失活/扩增/缺失/未明四段文案是否直接写进预览 item（给模板用）？
7. **药物分级（4.4）**：预览是否按 en7 口径输出 `approve_range` 1-8（获益 A/B/C/D + 耐药 A/B/C/D）与
   `give`（父级癌种有临床试验则升级）？还是先只出「获益/耐药 + 分期」不落等级码？
8. **其他癌种获批药（4.5）**：是否按 en7「其他癌种获批药一律降格成 C 级、且本癌种已有 A/B 级的同一药物不重复」
   输出？还是单独一组「其他癌种获批药」并列展示（便于审核，不降格）？
9. **位点分级（4.6）**：是否要输出 I/II/III 类（有 A/B 药→I类；只有 C/D→II类；无药仅 VUS→III类）？
   这套 en7 只用在 60 基因重肿模板的「变异分级提示」里，不是所有模板都出。

---

## 10. 附带修复记录（本机环境，非代码）

本机 nkb 库的 `*_evw` / `snkb_*` 视图原本全部查不了：
`ERROR 1449`（definer `'novo'@'localhost'` / `'novo'@'%'` 不存在）→ 补用户后又 `ERROR 1143/1356`（definer 无权限）。

处理（只动用户/权限，未动数据）：
```sql
CREATE USER IF NOT EXISTS 'novo'@'localhost' IDENTIFIED BY 'novo';
CREATE USER IF NOT EXISTS 'novo'@'%'         IDENTIFIED BY 'novo';
GRANT SELECT ON nkb.* TO 'novo'@'localhost';
GRANT SELECT ON nkb.* TO 'novo'@'%';
FLUSH PRIVILEGES;
```
结果：`variant_drug_annotation_evw`、`snkb_var_drug_anno_view`、`snkb_gene_variant_parent_evw`、
`snkb_disease_parent_evw` 等均可查；`gene_variant_evw` 仍因本机 `sql_mode` 含 `only_full_group_by` 报 1055，
故实现时改用等价自建查询（基表 + `effect` + `checking_status='Approved'` + 父级 `GROUP_CONCAT`）。

---

## 11. 若确认改造：落地清单（预估）

后端（ruoyi-report）：
- `NkbEvidenceMapper`：新增 ① 节点解析（四级）、② 父级ID、③ 疾病祖先/子孙展开、④ 证据查询（带 en7 过滤）
- `InterpretationPreviewServiceImpl`：`queryEvidence` 改为「节点链 × 癌种范围 × 过滤」；item 增加
  `effectText`（激活/失活/未知/无影响）、`matchedNode`（命中的是自身还是父级哪个节点）、`description`（四段模板）
- 过滤条件、疾病剔除集合（120/3856/2531/10000003）写成常量 + 注释，便于复刻
前后端（tab-preview）：
- 证据卡片加「证据分期 / 关系 / 是否获批机构 / 命中节点（父级时标注）」；位点行加 effect 标签
- 若按 4.4 落地：item 增加 `approveRange`（1-8）与分级名（获益A..D / 耐药A..D），按等级分组渲染
- 若按 4.5 落地：其他癌种获批药降格为 C 级（耐药则耐药C），并对本癌种已有 A/B 级的同一药物去重
- 若按 4.6 落地：位点增加 `variationClass`（I类/II类/III类/-），支撑「变异分级提示」分组

验证：KIT V559D 期望 5 条（含父级阿伐替尼）；KIT C58R 期望 0 条 + 未明文案；
胚系 BRCA1 期望本癌种 0 条（癌种链不含乳腺/卵巢）——三条都作为探针断言。
