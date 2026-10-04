# 圣域（pharma-shengyu）报告模板落位与变量核对

> 来源：参考工程 `/home/liushangzhi/project/biotech/backend/cool-admin-8.0.0.jar`
> → `BOOT-INF/classes/report-templates/pharma-shengyu/v1/{template.docx,payload.example.json}`
> 落位：`template-backend/ruoyi-modules/ruoyi-report/src/main/resources/report-templates/pharma-shengyu/v1/`
> 落位文件与参考工程**逐字节一致**：`sha256 = 2518E0A35427833FB9A26570129C215BAEAFF57A5644A2909A4065F45B5E396D`
> （与 `biotech` 库 `report_template.template_sha256` 登记值完全一致）

## 1. 模板配置（report_template）

| 字段 | 值 | 来源 |
|---|---|---|
| template_code | `pharma-shengyu` | 不变 |
| template_name | `同源重组修复（HRR）通路基因检测报告-圣域` | 参考工程 |
| customer_code | `圣域` | 参考工程 |
| report_type | `HRR` | 参考工程 |
| module_code | `SHENGYU_SOMATIC_VARIANTS_V1;SHENGYU_GERMLINE_VARIANTS_V1;SHENGYU_QC_V1` | 参考工程（顺序即执行顺序） |
| template_path | `report-templates/pharma-shengyu/v1/template.docx` | 与 classpath 落位一致 |
| template_sha256 | `2518E0A3…396D` | 落位文件实测 |
| report_name（本仓新增） | 暂空 = 生成时用默认命名 | 待定圣域命名口径 |

## 2. 模板变量核对（docx 标签 → 契约提供方）

模板里共 40 个去重标签/语句，逐个对上本仓 JSON 契约（`ReportTemplateData` + 三个 Handler）：

| 模板标签 | 提供方 |
|---|---|
| `{{reportInfo.reportDate}}` / `{{reportInfo.specimentType}}` | `CommonReportModuleAssembler`（tissue / blood 两值，与模板 `{% if %}` 比较的字符串一致） |
| `{{sampleInfo.researchCenterName}}` `.gender` `.disease` `.tissueCollectionDate` | `CommonReportModuleAssembler`（其余 sampleInfo 字段模板未引用） |
| `{%tr for it in shengyuSomaticVariants %}` + `{{it.gene}}` `{{it.mutationType}}` `{{it.result}}` `{{it.abundanceOrCopyNumber}}` `{{it.classification}}` | `ShengyuSomaticVariantsV1ModuleHandler` → `SomaticVariant` 的 5 个字段，逐一对应 |
| `{%tr for it in shengyuGermlineVariants %}` + `{{it.gene}}` `{{it.mutationType}}` `{{it.result}}` `{{it.zygosity}}` `{{it.classification}}` | `ShengyuGermlineVariantsV1ModuleHandler` → `GermlineVariant` 的 5 个字段，逐一对应 |
| `{% set q=qualityControl %}` `{% set s=q.sampleQc %}` `{% set c=q.controlQc %}` + `{{s.xxx}}` `{{c.xxx}}`（各 8 项，`\|d('/',true)` 兜空） | `ShengyuQcV1ModuleHandler` → `qualityControl.sampleQc` / `.controlQc`；8 个 key 全部在 Handler 的 `KEY` 清单里（tumorCellContent / dnaTotal / dnaDegradation / preLibraryTotal / meanDepth / coverageUniformity / genomeAlignmentRate / baseQualityQ30Rate） |
| `{%tr if reportInfo.specimentType == "tissue" %}` / `{% elif == "blood" %}` | 同上，行级条件展示 |

**结论：模板用到的变量 100% 有契约覆盖，docx 可直接用于本仓生成链路，标签不需要改。**

## 3. 模板未引用、但契约/样例里存在的字段（不需要动）

- 公共节 `somaticVariants` / `germlineVariants`（预览用完整结构，圣域模板改走 `shengyu*` 过滤后的两节）
- `qualityControl.status` / `qualityControl.assessment`（参考样例里有；本仓按「阈值与合格判定文案写在 DOCX 里」的口径不输出这两个键，模板也没引用）
- `qualityControl.sampleQc.sequencingDataVolume` / `.targetRegionCoverage`（Handler 会输出，模板未展示）
- `sampleInfo.participantNumber` / `.birthYear` / `.sampleCode` / `.sampleType` / `.receivedDate` / `.visitCycle` / `.sectionDate` / `.bloodCollectionDate`：模板里这些行只有中文标签、没有取值标签（受试者编号 / 出生年份 / 样本编码 / 样本类型 / 切片日期 / 全血样本采集日期 / 接收日期），本仓契约里都有值可供引用

## 4. 已验证 / 未验证

- 已验证：落位文件 sha256 与参考工程登记值逐字一致；40 个标签逐个对上契约；升级脚本执行后 `report_template` 行的 name/customer_code/report_type/module_code/sha256 全部对齐
- 未验证：本机渲染一遍（缺 `docxtpl`）。参考工程已用这份模板 + 同一份样例产出过真实制品：
  `project/biotech/data/assets/report-artifacts/18/report-18-pharma-shengyu-*.docx`（2026-09-30）
- 若要让模板也显示上面第 3 节那几项基本信息，需要改 docx → sha256 会变，改动方式与命名口径一起确认后再做
