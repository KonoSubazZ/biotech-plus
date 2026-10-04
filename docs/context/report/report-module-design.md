# 报告模块设计书

> 目标：让新的开发 Agent 可以根据本文复现“数据驱动 → 解读 → 内存预览 → 生成 → 审核 → 发送”的完整流程。
>
> 技术基线：MySQL 5.7、Spring Boot、MyBatis/JdbcTemplate、Python、docxtpl/Jinja2 和 OnlyOffice。
>
> 设计原则：关联关系由业务代码维护，数据库不建立物理外键；`report_template.module_code` 只负责选择 Java 业务处理器；模板不保存复杂业务规则；完整预览结果不落库，只保存可复用的位点匹配历史，并在正式生成时冻结最终 JSON。

## 0. 当前实现权威快照（2026-09-14）

本节是给后续重写 Agent 的实现基线。若本文后续“目标草案”或“分阶段实施”与本节冲突，以本节、当前 Java 代码及 Flyway 迁移为准。

### 0.1 模板与模块流水线

- 公共字段不需要配置模块，由 `CommonReportModuleAssembler` 无条件写入 `reportInfo`、`sampleInfo`、公共体细胞/胚系预览、样本类型开关和告警。
- `report_template.module_code` 是可空的、以分号分隔的有序个性化模块列表，不再限制为单个处理器。
- `ReportModuleHandlerRegistry` 按配置顺序解析模块；空配置表示只有公共字段；重复或未注册编码立即报错。
- 当前 `pharma-shengyu` 的配置为：

```text
SHENGYU_SOMATIC_VARIANTS_V1;SHENGYU_GERMLINE_VARIANTS_V1;SHENGYU_QC_V1
```

- 当前 JSON 协议版本为 `1.2`。个性化结果分别写入 `shengyuSomaticVariants`、`shengyuGermlineVariants` 和圣域质控字段，公共预览仍保留在根对象中供页面使用。

### 0.2 圣域 1238 业务规则

- 癌种先解析为 NKB 当前节点及全部父级节点，不能只按 LIMS 文本做完全相等比较。
- 乳腺癌、卵巢癌、前列腺癌及其 NKB 子类：输出体细胞（项目历史中也称“体系”）结果和胚系结果。
- 其他实体瘤：圣域个性化体细胞结果为空，只输出胚系结果。
- 两类结果都按当前产品启用的 `product_gene` 过滤和补空行；产品未配置启用基因时结果为空并返回告警，不得静默当作“全部基因”。
- `novopm2_tis_1238` 与 `novopm2_blo_1238` 使用同一模板 `pharma-shengyu` 和同一组 8 个基因：`BRCA1`、`BRCA2`、`PALB2`、`RAD51C`、`RAD51D`、`BRIP1`、`BARD1`、`FANCD2`。
- 数据库中的稳定产品编码分别为 `SYN-818-102_novopm2_tis_1238`、`SYN-818-102_novopm2_blo_1238`；`analysis_data.product` 当前保存短产品名。`analysis_data.product_id` 为空时，查询必须兼容按 `product_code` 或 `product_name` 定位产品。

### 0.3 胚系历史、临床意义与改靶

- 胚系来源是 `file_CR_ALL` 中 `is_reported = 1` 的记录；所有报出胚系位点都进入 `history_germline`/NKB 的 `G` 类匹配，不再依赖 `omics.rp_cr.has_drug` 或 `rp_cr.Clinical_significance`。
- 药物匹配历史键包含：基因、规范位点、原始位点、NKB 癌种 ID/名称、性别、客户、产品、来源类型、突变类型、合子状态和人工父级 `parent_mutation_id`。
- 客户和产品相同并不代表无条件复用；癌种、性别和位点等全部维度也必须一致。`source_analysis_id`、`source_report_id`、`source_variant_id` 只用于审计，不进入跨报告复用键。
- `history_germline.clinical_significance` 保存人工确认的五级临床意义：`1=致病`、`2=可能致病`、`3=未知临床意义`、`4=可能良性`、`5=良性`，默认值为 3。
- 页面允许对每个胚系位点执行“修改临床意义”和“改靶/调整改靶”。保存临床意义要求该位点已经通过预览建立历史记录。
- 改靶会更新 `file_CR_ALL.parent_mutation_id`，因此药物匹配生成新的技术历史键；新历史记录必须继承同一客户、产品、癌种、性别、位点和合子状态下最近一条临床意义，不能恢复为默认值 3。
- 临床意义继承明确忽略 `parent_mutation_id`，但不忽略癌种和性别；药物证据复用仍包含 `parent_mutation_id`，两种语义不可合并成一个宽松键。

### 0.4 报告 JSON 生命周期与存储位置

- 预览 JSON 只存在于 Java 对象和 HTTP 响应，不写数据库，也不创建预览文件。
- 正式生成时服务端重新查询最新 LIMS、报出位点、改靶、临床意义、产品基因和模板配置，不接受前端回传的预览 JSON。
- 当前实现把最终 JSON 写入报告制品目录：`<artifact-root>/<reportId>/report-<reportId>-<templateCode>-<时间>-<随机串>.json`。
- DOCX 与 JSON 使用相同文件名前缀；`analysis_report.report_json_path` 保存 JSON 路径，`report_file_raw_path` 保存 DOCX 路径，`template_id/template/report_generated_by/report_generated_at` 同时更新。
- 再次生成会重新组装数据并创建新的唯一 JSON/DOCX 文件，因此内容可能随最新业务数据变化；数据库路径会指向最新制品，旧 JSON 文件不会被原地修改。
- 当前运行代码不把 JSON 内容写入 `analysis_report.report_detail`，也未使用 `report_json_hash`。本文后续出现的 `report_detail`/哈希方案属于旧目标草案，重写时不得当作已实现行为。

### 0.5 当前接口

```text
POST /admin/report/interpretation/preview
GET  /admin/report/interpretation/drug-preview
GET  /admin/report/interpretation/mutation-candidates
POST /admin/report/interpretation/target
POST /admin/report/interpretation/germline-clinical-significance
POST /admin/report/template/data
POST /admin/report/template/generate
```

`/admin/report/template/generate` 当前直接返回生成的 DOCX 文件资源；OnlyOffice PDF 转换、完整审核状态机和冻结 JSON 哈希仍不是当前生成链的一部分。

### 0.6 SQL 权威来源

| 迁移/文件 | 作用 |
| --------- | ---- |
| `V7__variant_parent_mutation.sql` | 给体细胞、CNV、Fusion、CR_ALL 增加人工父级和索引 |
| `V8__report_match_history.sql` | 创建 `history_somatic`、`history_germline` |
| `V10__report_product_gene_master_data.sql` | 创建产品、基因、产品基因、产品模板关系 |
| `V12__report_artifact_paths.sql` | 增加 `analysis_report.report_json_path` |
| `V14__personalized_report_modules.sql` | 将 `module_code` 定义为可空的分号模块列表并配置圣域流水线 |
| `V15__control_qc.sql` | 增加对照样本质控表 |
| `V18__history_germline_clinical_significance.sql` | 增加胚系五级临床意义字段 |
| `V19__sync_1238_product_report_configuration.sql` | 幂等固化组织/血液 1238 产品、8 基因和圣域模板关系 |
| `sql/init/init.sql`、`sql/init/file_tables.sql` | 旧式独立文件明细表基线，已同步质控对照表及人工改靶字段；完整结构仍以 Flyway 顺序为准 |

已执行过的 Flyway 迁移不可回改。已有环境通过后续版本增量升级；新语言实现可以按版本顺序复现，或据此生成一份等价的全量初始化脚本。

## 1. 架构结论

报告模块分为四层：

1. 数据层：`scanner.py` 负责把文件写入 `data_file_status` 和各 `file_*` 明细表。
2. 解读层：Java 动态读取 LIMS、文件、位点、产品和模板，并允许修改共享的 `file_* .is_reported`。
3. 匹配层：Java 先组装公共字段，再按 `report_template.module_code` 的分号顺序执行个性化处理器；精确命中 `history_somatic/history_germline` 时复用冻结的位点匹配，否则查询最新知识库并首次写入历史。
4. 生成层：点击生成时重新执行匹配，把最终 JSON 和 DOCX 写入制品目录，并分别把路径登记到 `analysis_report.report_json_path` 和 `report_file_raw_path`。

核心对象语义：

- `analysis_data` 表示一次分析批次，不表示报告。
- `analysis_report` 表示一份报告或一次重出版本，同一 `analysis_id` 可以对应多条报告。
- `file_* .is_reported` 是分析批次共享的当前选择，不按报告隔离。
- 当前最终 JSON 是 `report_json_path` 指向的文件；再次生成创建新文件并更新路径，不原地覆盖旧 JSON。
- `history_somatic`、`history_germline` 只冻结单个位点匹配结果，不是报告预览快照。
- `report_template.module_code` 是 Java 处理器的稳定编码，不是外键。
- 一个模板可以配置零个或多个以分号分隔的个性化 `module_code`；公共字段无条件生成。
- 不增加模块表和模板模块关系表，也不在模板 JSON 中维护癌种、基因和质控规则。
- 最终 JSON 直接使用 `sampleInfo.age`、`somaticVariants` 等短路径，不增加 `modules.sampleInfo` 包装层。

## 2. 当前实现与目标边界

### 2.1 已实现部分

| 阶段      | 真实代码/表                                                           | 当前行为                                       |
| --------- | --------------------------------------------------------------------- | ---------------------------------------------- |
| 数据上传  | `docs/report-driver/scanner.py`                                       | 解析路径并启动文件扫描                         |
| 分析批次  | `core/file_scanner.py`、`analysis_data`                               | 按样本、产品、分析日期创建或复用 `analysis_id` |
| 文件记录  | `data_file_status`                                                    | 保存文件路径、类型、状态、解析信息             |
| 文件明细  | `file_Somatic_SNV_Indel`、`file_CNV`、`file_Fusion`、`file_CR_ALL` 等 | Processor 按 `file_id` 写入对应子表            |
| 报告入口  | `ReportService`、`analysis_report`                                    | 点击解读创建或进入 `report_id`                 |
| LIMS 展示 | `ReportMapper.selectLimsInfo`                                         | `subbarcode = BARCODE`，按最新记录动态读取     |
| 集群对接  | `ReportMapper.selectFiles`                                            | 按 `analysis_id` 展示文件                      |
| 位点筛选  | `ReportService.updateVariantReportStatus`                             | 直接更新 `file_* .is_reported`                 |
| 靶药预览  | `ReportDrugPreviewService`                                            | 根据癌种、性别和报出位点匹配 NKB               |
| 匹配历史  | `ReportMatchHistoryService`、`history_somatic/history_germline`       | 同业务键复用首次结果，不同条件读取最新 NKB      |
| 模板 JSON 组装 | `ReportTemplateDataService`、`ReportTemplateData`                  | 已根据 `report_template.module_code` 选择 Handler 并生成短路径 JSON |
| 圣域模板资源 | `resources/report-templates/pharma-shengyu/v1/`                    | 已保存 DOCX 和结果示例；复杂 manifest 已删除，3 个样本字段可渲染，结果与质控循环待改造 |

### 2.2 本阶段设计重点

- 在 `report_template` 增加 `module_code`，由它选择 Java 报告处理器。
- 把圣域业务拆为体细胞、胚系、质控三个可组合的个性化处理器。
- 明确预览 JSON 只存在 Java 对象和 HTTP 响应中。
- 明确正式生成必须重新查询数据，不能信任前端回传的旧预览。
- 简化产品、基因、模板及报告版本表，不引入快照、模块配置表、模板模块关系表、生成任务或制品明细表。
- 删除运行时对复杂 `manifest.json` 的依赖；项目中的 `payload.example.json` 只用于说明和测试，不是业务配置。
- 把旧 Python `gen_reports.py` 从“业务编排器”收缩为“文档渲染器”。

## 3. 逻辑数据模型

### 3.1 业务关系

```text
analysis_data 1 --- N data_file_status 1 --- N file_* 明细
      |
      +--- N analysis_report

product N --- N gene            通过 product_gene
product N --- N report_template 通过 product_template

report_template.module_code --- Java ReportModuleHandler.code() 逻辑匹配

analysis_report 1 --- N analysis_report_email_log
analysis_report N --- 0..1 analysis_report 通过 source_report_id 表示重出来源

file_Somatic_SNV_Indel/file_CNV/file_Fusion --- history_somatic 通过来源 ID 逻辑追踪
file_CR_ALL --- history_germline 通过来源 ID 逻辑追踪
```

以上均为逻辑关系。服务层负责检查 ID 是否存在、记录是否启用、报告和分析批次是否匹配；数据库只使用主键、唯一索引和查询索引。

### 3.2 ID 校验规则

| 关联字段                                  | 写入前逻辑校验                                   | 删除规则                       |
| ----------------------------------------- | ------------------------------------------------ | ------------------------------ |
| `data_file_status.analysis_id`            | `analysis_data` 存在且未删除                     | 分析数据存在文件时禁止物理删除 |
| `analysis_report.analysis_id`             | 分析数据存在且驱动状态可解读                     | 报告存在时分析数据只能逻辑删除 |
| `analysis_report.product_id`              | 产品存在且启用，并与分析数据产品一致             | 被报告引用的产品禁止物理删除   |
| `analysis_report.template_id`             | 模板存在且启用，`module_code` 已注册，且 `product_template` 允许该组合 | 被报告引用的模板禁止物理删除   |
| `analysis_report.source_report_id`        | 来源报告存在、属于同一 `analysis_id` 且已驳回    | 来源报告永久保留               |
| `product_gene.product_id/gene_id`         | 产品和基因均存在且启用                           | 有关联时先停用再清理关系       |
| `product_template.product_id/template_id` | 产品和模板均存在且启用                           | 有报告使用时只允许停用         |
| `analysis_report_email_log.report_id`     | 报告存在且状态允许发送                           | 邮件日志不随报告删除           |

所有跨表写入都放在同一事务中。逻辑删除字段统一使用 `deleted` 或 `status`，不执行级联删除。

## 4. 表结构设计

> 以下是 MySQL 5.7 目标草案。正式落地前先检查现有 `product` 和 `analysis_report` 的真实字段，再通过新的 Flyway 迁移增量调整。

### 4.1 产品

```sql
CREATE TABLE `product` (
  `product_id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_code` VARCHAR(120) NOT NULL COMMENT '稳定产品编码，对应驱动产品名',
  `product_name` VARCHAR(255) NOT NULL COMMENT '产品中文名称',
  `product_name_en` VARCHAR(255) DEFAULT NULL,
  `description` VARCHAR(1000) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_by` VARCHAR(80) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(80) DEFAULT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_product_status` (`status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测产品';
```

### 4.2 基因

```sql
CREATE TABLE `gene` (
  `gene_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `gene_symbol` VARCHAR(64) NOT NULL,
  `gene_name` VARCHAR(255) DEFAULT NULL,
  `aliases_json` JSON,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_by` VARCHAR(80) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(80) DEFAULT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`gene_id`),
  UNIQUE KEY `uk_gene_symbol` (`gene_symbol`),
  KEY `idx_gene_status` (`status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基因主数据';
```

### 4.3 报告模板

模板文件允许被多个产品使用。模板更新直接更新受控文件和校验值；本方案不建设模板版本表。

```sql
CREATE TABLE `report_template` (
  `template_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `template_code` VARCHAR(120) NOT NULL,
  `template_name` VARCHAR(255) NOT NULL,
  `customer_code` VARCHAR(120) DEFAULT NULL,
  `report_type` VARCHAR(40) NOT NULL,
  `module_code` VARCHAR(500) DEFAULT NULL COMMENT '有序个性化Java模块编码，分号分隔；公共模块无需配置',
  `template_path` VARCHAR(1000) NOT NULL,
  `template_sha256` CHAR(64) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_by` VARCHAR(80) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(80) DEFAULT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_report_template_code` (`template_code`),
  KEY `idx_report_template_customer` (`customer_code`, `status`, `deleted`),
  KEY `idx_report_template_module` (`module_code`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告模板';
```

`module_code` 约束如下：

- 字段只保存有序的个性化模块编码，使用分号分隔；空值表示只输出公共字段。
- 同一编码在一条流水线中不得重复，所有非空编码都必须已经注册。
- 多个版式不同但业务数据一致的模板可以复用同一条模块流水线。
- 同一版式需要不同业务口径时新增专属模块，不要在公共组装器中堆叠大量 `template_code` 判断。
- 癌种显示条件、基因顺序、质控标准等由对应 Java 模块维护，不进入模板 JSON。

示例数据：

```text
template_code     module_code                                                                 template_path
pharma-shengyu    SHENGYU_SOMATIC_VARIANTS_V1;SHENGYU_GERMLINE_VARIANTS_V1;SHENGYU_QC_V1     report-templates/pharma-shengyu/v1/template.docx
common-template   NULL                                                                        report-templates/common/v1/template.docx
```

模板被已生成报告使用后不应直接替换文件。若确需替换，应先创建新的 `report_template` 记录或新模板编码，让历史报告仍可定位原模板路径和哈希。

### 4.4 产品与基因

```sql
CREATE TABLE `product_gene` (
  `product_id` INT UNSIGNED NOT NULL,
  `gene_id` BIGINT UNSIGNED NOT NULL,
  `panel_role` VARCHAR(30) NOT NULL DEFAULT 'TARGET',
  `is_required` TINYINT(1) NOT NULL DEFAULT 0,
  `sort_order` INT NOT NULL DEFAULT 0,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`, `gene_id`),
  KEY `idx_product_gene_gene` (`gene_id`, `enabled`),
  KEY `idx_product_gene_product` (`product_id`, `enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品与基因多对多';
```

### 4.5 产品与模板

```sql
CREATE TABLE `product_template` (
  `product_id` INT UNSIGNED NOT NULL,
  `template_id` BIGINT UNSIGNED NOT NULL,
  `is_default` TINYINT(1) NOT NULL DEFAULT 0,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`, `template_id`),
  KEY `idx_product_template_template` (`template_id`, `enabled`),
  KEY `idx_product_template_product` (`product_id`, `enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品与模板多对多';
```

MySQL 5.7 无法通过普通唯一索引表达“同一产品只能有一个启用的默认模板”。保存默认模板时，服务层锁定该产品的关系记录，先清除旧默认值，再写入新默认值。

### 4.6 分析数据增量字段

现有 `analysis_data.product` 继续保留为上传时的产品名称，同时增加逻辑产品 ID：

```sql
ALTER TABLE `analysis_data`
  ADD COLUMN `product_id` INT UNSIGNED DEFAULT NULL COMMENT '逻辑关联product.product_id' AFTER `product`,
  ADD KEY `idx_analysis_product_id` (`product_id`, `deleted`);
```

回填时用 `analysis_data.product = product.product_code` 匹配。未匹配数据先输出清单，不应自动创建未知产品。

### 4.7 报告增量字段

当前已落地字段：

```sql
ALTER TABLE `analysis_report`
  ADD COLUMN `template_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '逻辑关联report_template.template_id',
  ADD KEY `idx_report_template_id` (`template_id`, `status`);

ALTER TABLE `analysis_report`
  ADD COLUMN `report_json_path` VARCHAR(1000) DEFAULT NULL COMMENT '正式生成JSON文件路径' AFTER `template_id`,
  MODIFY COLUMN `report_file_raw_path` VARCHAR(1000) DEFAULT NULL COMMENT '原始DOCX报告路径';
```

沿用现有字段：

- `analysis_id`：报告所属分析。
- `product`：当前报告产品短名称；产品主数据通过 `analysis_data.product_id` 或产品名称兼容查询。
- `template_id`、`template`：本次生成使用的模板 ID 和编码。
- `report_json_path`：本次最新正式生成 JSON 的文件路径。
- `report_file_raw_path`：生成的 DOCX 路径。
- `report_file_path`：最终 PDF 路径。
- `sub_report_file_path`：可选子报告路径。
- `status`、产出/审核/发送人员和时间字段。
- `comment`：备注或最新驳回原因摘要；完整审核流水可沿用独立审核日志设计。

`source_report_id`、`version_no`、`report_detail`、`report_json_hash` 尚未在当前迁移中落地。若其他语言重写需要报告重出链或内容哈希，应作为新的迁移和状态机能力设计，不能假定现有数据库已有这些列。

### 4.8 体系与胚系匹配历史

实际建表迁移为 `V8__report_match_history.sql`；v2 键口径 + 复用留痕由
`script/sql/update/update_5.6.4-history-reuse-tracking.sql` 增量落地。两张表字段结构保持一致：

- **复用键（v2，当前口径）**：`project_code`（产品项目）、`disease_id/disease`（癌种）、`gender`、
  `gene`、`variant`、`ori_variant`、`parent_mutation_id`（人工改靶）。
  键 = 上列规范化后 `sha256("v2|产品项目|癌种|性别|基因|位点|原始位点|改靶父级")`。
  同产品项目下同一位点（同癌种、同性别、同改靶）复用同一条冻结结果。
- `customer`（医院）、`source_type`、`mutation_type`、`measurement`（合子）：**列保留、继续填充，但已不进复用键**。
  复用边界是「产品项目」，不按医院；来源类型/突变类型/合子是位点短名的派生或次要维度。
- `key_version`：复用键口径版本（1=旧键含 customer，2=当前键）。迁移把 v1 行 `del_flag='1'` 退役，不物理删除。
- `last_reused_at`、`reuse_count`：每次命中历史（复用）时更新，用于回答「何时被复用、复用了几次」。
- `history_reuse_log`（独立表）：append-only 留痕，记录每次复用的 `report_id / analysis_id / reused_by / reused_at`。
- `match_result`：冻结单个 `ReportDrugPreview.Item` JSON，包含药物分层、证据、说明和临床试验。
- `match_status`、`variation_class`、`knowledge_matched_at`：记录匹配结论及首次知识库匹配时间。
- `history_germline.clinical_significance`：人工确认的胚系五级临床意义，取值 1 到 5，默认 3；`history_somatic` 没有该字段。
- `source_analysis_id`、`source_report_id`、`source_variant_id`：仅用于审计追踪，不建立外键。
- `match_key`：全部匹配条件规范化后计算 SHA-256，并使用唯一索引保证首次结果不会被后续知识库更新覆盖。

上下文映射：`customer` 依次取最新 LIMS 的 `CUSTOMERNAME`、`CUSTOMEDESC`、`corpdesc`；全部为空时使用 `UNKNOWN_CUSTOMER@ANALYSIS:{analysisId}`，仅允许同一分析下的多报告复用，避免未知客户跨样本串用。`project_code = analysis_data.product`。项目编号缺失时不读写历史，直接使用最新知识库结果。

查找规则：

1. 完整键命中时：反序列化历史 `match_result` 并复用（**严格只读** —— NKB 更新不刷新），同时更新
   `last_reused_at` / `reuse_count`，并在 `history_reuse_log` 留一条痕（哪份报告 / 谁 / 何时）。
2. 未命中时按当前 NKB 匹配，使用 `INSERT IGNORE` 写入首条结果；并发请求由 `match_key` 唯一索引收敛。
3. 业务条件或人工父级不同则产生新键，按最新知识库匹配并新增历史（改靶**不覆盖**旧键那条，只新增一条新键）。
4. 胚系新键由改靶产生时，从同一产品、癌种、性别、位点的最近记录继承 `clinical_significance`；仅临床意义继承忽略人工父级。
5. 历史结果不随 NKB 定期更新而刷新，从而保证同一产品项目、同一匹配条件持续输出同一类药物与同一位点等级。
6. **没有任何路径覆盖已冻结的 `match_result` / `variation_class`**：旧版的 `refreshHistory`（旧结构时覆盖）
   与「保存临床意义时重跑匹配再覆盖」都已删除；保存临床意义只更新 `clinical_significance` 一列。

五级临床意义映射固定为：

| 值 | 中文标签 |
| -- | -------- |
| 1 | 致病 |
| 2 | 可能致病 |
| 3 | 未知临床意义 |
| 4 | 可能良性 |
| 5 | 良性 |

## 5. LIMS 动态读取

### 5.1 查询规则

不增加 LIMS 快照表。预览和生成时均执行：

```sql
SELECT *
FROM `myapp_webcrmsample`
WHERE `BARCODE` = ?
ORDER BY `id` DESC
LIMIT 1;
```

`?` 为 `analysis_report.subbarcode`。列表展示、预览和正式生成使用相同映射函数，避免 Java 页面与 Python 报告口径不同。

### 5.2 报告需要的字段

| 分类     | 目标字段            | LIMS 来源                    | 用途                           |
| -------- | ------------------- | ---------------------------- | ------------------------------ |
| 样本标识 | `subbarcode`        | `BARCODE`                    | 报告与分析数据关联             |
| 患者信息 | `patientId`         | `PCODE`                      | 患者标识                       |
| 患者信息 | `patientName`       | `PATIENTNAME`                | 报告封面与样本信息             |
| 患者信息 | `gender`            | `SEX`                        | 用药匹配；生殖系统癌种条件必填 |
| 患者信息 | `birthday`、`age`   | `BIRTHDAY`、`AGE`            | 报告基本信息                   |
| 疾病信息 | `cancerType`        | `CANCERTYPE`                 | NKB 疾病匹配                   |
| 疾病信息 | `pathologicalType`  | `PATHOLOGICALTYPE`           | 病理诊断                       |
| 疾病信息 | `clinicalStage`     | `CLINICALSTAGES`             | 分期展示和模块规则             |
| 疾病信息 | `clinicalRemark`    | `CLINICALREMARK`             | 临床备注                       |
| 送检信息 | `hospitalName`      | `CUSTOMERNAME`               | 医院/送检单位                  |
| 送检信息 | `doctorName`        | `DOCTORNAME`                 | 送检医生                       |
| 样本信息 | `specimenType`      | `SAMPLETYPE`                 | 样本分类                       |
| 样本信息 | `specimenQuantity`  | `SPECIMENNUM + UNIT`         | 样本量                         |
| 样本信息 | `sampleSource`      | `SAMPLESOURCE`               | 样本来源                       |
| 样本信息 | `fromOrgan`         | `FROMORGAN`                  | 来源器官                       |
| 日期信息 | `sampleCollectedAt` | `COLLECTDATE` / `SAMPLETIME` | 采样日期，映射前需统一业务口径 |
| 日期信息 | `sampleReceivedAt`  | `GETSPECDATE`                | 收样日期                       |
| 日期信息 | `commissionedAt`    | `ENTERDATE`                  | 委托日期                       |
| 产品信息 | `testingProgram`    | `ERPTESTNAME`                | 与产品做一致性检查             |
| 其他     | `sampleRemark`      | `SAMPLEREMARK`               | 样本备注和客户特例             |
| 其他     | `laboratoryName`    | `laboratoryname`             | 检测机构相关规则               |
| 邮件     | `reportReceiver`    | `REPORTRECEIVER`             | 收件人姓名候选                 |
| 邮件     | `emailAddress`      | `EMAILADDRESS`               | 邮箱候选                       |
| 邮件     | `patientInfoEmail`  | `patientinfoemail`           | 邮箱候选                       |
| 邮件     | `doctorEmail`       | `admissiondoctoremail`       | 邮箱候选                       |

关键校验：

- 没有 LIMS 记录时，样本信息模块失败并阻止生成。
- `patientName`、`cancerType`、`specimenType`、`testingProgram` 缺失时阻止生成。
- 性别先规范为 `MALE`、`FEMALE`、`UNKNOWN`；只有依赖性别的疾病或模块才阻止 `UNKNOWN`。
- 邮箱只作为候选，发送前由操作人确认并由后端校验格式。
- `COLLECTDATE` 与 `SAMPLETIME` 的最终口径由统一映射函数封装；原值可同时放进最终 JSON，避免特殊模板丢失数据。

LIMS 通常不变，因此不做版本化。若 LIMS 在报告正式生成前发生变化，本次生成使用生成时查询到的最新值；生成成功后以 `report_json_path` 指向的 JSON 文件为该次制品依据。

## 6. 共享位点模型

继续使用现有四类位点表：

- `file_Somatic_SNV_Indel`
- `file_CNV`
- `file_Fusion`
- `file_CR_ALL`

筛选接口直接更新对应记录的：

- `is_reported`
- `filtered_rationale`
- `reviewed_by`
- `reviewed_at`

更新前服务层必须校验：

1. `source_id` 存在且未删除。
2. 通过 `file_id -> data_file_status.analysis_id` 验证位点属于请求中的分析批次。
3. `is_reported` 只能是 `0` 或 `1`。
4. 设为不报出时，过滤理由是否必填由当前业务规则决定。

预览和生成只查询 `is_reported = 1` 的数据。当前所有报出的 `CR_ALL` 位点都进入胚系历史/NKB 匹配；`rp_cr` 不参与胚系临床意义或是否匹配的判定。

已接受的限制：同一 `analysis_id` 下多个未生成报告共享选择状态，不做并发锁或草稿隔离；最后一次修改会影响所有后续预览。各报告生成成功后有自己的冻结 JSON，因此历史成品不会互相覆盖。

## 7. Java 按 `module_code` 组装 JSON

### 7.1 最终决策

`report_template.module_code` 用来选择零个或多个有序的 Java 个性化模块。公共字段先无条件组装，再按分号顺序追加模板专属字段；数据库不保存模块实现细节或条件表达式。

```text
report_template.module_code
          |
          v
ReportModuleHandlerRegistry
          |
          v
CommonReportModuleAssembler
          +
有序个性化 Handler 流水线
          |
          v
ReportTemplateData（短路径 JSON）
```

采用“公共组装器 + 有序个性化模块列表”，约束如下：

- 模板配置只承担模块选择和顺序，不成为业务规则引擎。
- 癌种判断、必填校验、基因排序和质控标准都可以编译、测试和审查。
- 多个客户模板可以复用同一模块流水线。
- 新业务口径通过新增个性化模块隔离，不在公共组装器中堆积条件表达式。
- 空流水线仍能生成完整公共数据。

### 7.2 分层结构

```text
Controller
  -> ReportTemplateDataService
      -> ReportService / ReportPreviewService / ReportDiseaseContextResolver
      -> CommonReportModuleAssembler（公共字段）
      -> ReportModuleHandlerRegistry
          -> 按 report_template.module_code 分号顺序取得个性化 Handler
              -> ShengyuSomaticVariantsV1ModuleHandler
              -> ShengyuGermlineVariantsV1ModuleHandler
              -> ShengyuQcV1ModuleHandler
      -> ReportTemplateData
  -> ReportGenerationService（GENERATE）
      -> JSON 文件
      -> ReportDocxRenderer / Python
      -> DOCX 文件
      -> analysis_report 路径字段
```

### 7.3 公共上下文

```java
public record ReportContext(
    Long analysisId,
    Long reportId,
    AnalysisReportInfo report,
    ReportLimsInfo lims,
    ProductInfo product,
    TemplateInfo template,
    List<ReportFileInfo> files,
    ReportedVariants variants,
    MatchMode mode
) {}
```

`MatchMode` 只有 `PREVIEW` 和 `GENERATE`。上下文加载器一次性完成公共查询；Handler 不重复校验报告归属和 LIMS。Handler 需要访问 NKB、匹配历史或模块专属表时，可以调用现有只读 Service/Repository。

### 7.4 处理器接口

```java
public interface ReportModuleHandler {

    String moduleCode();

    default boolean requiresProductGenes() {
        return false;
    }

    void apply(ReportModuleContext context, ReportTemplateData target);
}
```

个性化模块示例：

```java
@Component
public class ShengyuSomaticVariantsV1ModuleHandler implements ReportModuleHandler {

    @Override
    public String moduleCode() {
        return "SHENGYU_SOMATIC_VARIANTS_V1";
    }

    @Override
    public boolean requiresProductGenes() {
        return true;
    }

    @Override
    public void apply(ReportModuleContext context, ReportTemplateData target) {
        // 真实实现通过 NKB 疾病层级判断癌种，并按 product_gene 组装个性化字段。
    }
}
```

约束：

- `moduleCode()` 在应用内唯一，并与数据库值做大小写统一。
- `apply()` 不接收前端回传的预览 JSON，只修改当前构建过程中的目标 DTO。
- 相同上下文必须产生相同 JSON；不得写入当前时间或随机值。
- 缺失业务值使用 JSON `null`，不能提前替换成 `/` 或 `-`。
- Handler 不直接调用 Python，也不更新报告状态。
- 必填数据缺失时返回明确错误或校验结果，不能静默编造。

### 7.5 处理器注册器

```java
@Component
public class ReportModuleHandlerRegistry {

    private final Map<String, ReportModuleHandler> handlers;

    public ReportModuleHandlerRegistry(List<ReportModuleHandler> handlerList) {
        this.handlers = handlerList.stream().collect(Collectors.toUnmodifiableMap(
            handler -> handler.moduleCode().toUpperCase(Locale.ROOT),
            Function.identity()
        ));
    }

    public ReportModuleHandler required(String moduleCode) {
        ReportModuleHandler handler = handlers.get(normalize(moduleCode));
        if (handler == null) {
            throw new IllegalArgumentException("未注册报告模块：" + moduleCode);
        }
        return handler;
    }

    public List<ReportModuleHandler> configuredPipeline(String moduleCodes) {
        // 空值返回空列表；按分号解析；重复或未注册编码报错。
    }
}
```

Spring 自动发现实现类。新增 Handler 时不修改注册器、Controller 或中心 `switch`。

### 7.6 当前 JSON 契约

当前圣域报告使用公共预览对象加顶层个性化字段。以下是删减后的结构示例，字段类型以 `ReportTemplateData` 及其嵌套 DTO 为准；`payload.example.json` 只是渲染样例，不是业务规则来源：

```json
{
  "schemaVersion": "1.2",
  "templateCode": "pharma-shengyu",
  "templateVersion": "v1",
  "analysisId": 123,
  "reportId": 456,
  "reportInfo": {
    "reportDate": "2026-09-14",
    "specimentType": "tissue"
  },
  "showTissueBloodSample": true,
  "showBloodOnlySample": false,
  "sampleInfo": {
    "researchCenterName": "示例研究中心",
    "participantNumber": "SUBJECT-001",
    "gender": "女",
    "birthYear": "1980",
    "disease": "乳腺癌",
    "visitCycle": null,
    "sampleCode": "SAMPLE-001",
    "sampleType": "组织+全血",
    "tissueCollectionDate": null,
    "sectionDate": null,
    "bloodCollectionDate": null,
    "receivedDate": "2026-08-19"
  },
  "somaticVariants": {
    "summary": {},
    "items": []
  },
  "germlineVariants": {
    "summary": {},
    "items": [
      {
        "sourceId": 12,
        "gene": "BRCA2",
        "variant": "A2825E",
        "zygosity": "杂合",
        "clinicalSignificance": 1,
        "clinicalSignificanceLabel": "致病",
        "matchStatus": "MATCHED",
        "drugMatch": {}
      }
    ]
  },
  "shengyuSomaticVariants": [],
  "shengyuGermlineVariants": [
    {
      "gene": "BRCA2",
      "mutationType": "SNV/Indel",
      "result": "A2825E",
      "zygosity": "杂合",
      "classification": "致病"
    }
  ],
  "qualityControl": {
    "specimenType": "组织",
    "status": "QUALIFIED",
    "assessment": "合格",
    "sampleQc": {
      "tumorCellContent": "30",
      "dnaTotal": "814",
      "dnaDegradation": "1",
      "preLibraryTotal": "420",
      "sequencingDataVolume": "18.6",
      "meanDepth": "5286",
      "coverageUniformity": "96.8",
      "targetRegionCoverage": "99.2",
      "genomeAlignmentRate": "99.7",
      "baseQualityQ30Rate": "91.5"
    },
    "controlQc": {
      "tumorCellContent": "20",
      "dnaTotal": "690",
      "dnaDegradation": "1",
      "preLibraryTotal": "380",
      "sequencingDataVolume": "17.9",
      "meanDepth": "5100",
      "coverageUniformity": "95.9",
      "targetRegionCoverage": "98.8",
      "genomeAlignmentRate": "99.5",
      "baseQualityQ30Rate": "90.8"
    }
  },
  "warnings": []
}
```

模板以后直接使用：

```text
sampleInfo.gender
sampleInfo.disease
shengyuSomaticVariants
shengyuGermlineVariants
qualityControl.sampleQc.meanDepth
qualityControl.controlQc.meanDepth
```

不使用以下结构：

```text
modules.sampleInfo.data.gender
modules.hrr.data.somatic.items
```

### 7.7 动态模块

个性化模块通过顶层动态字段输出数据。公共 `somaticVariants`/`germlineVariants` 保留完整预览对象；圣域模板读取专属列表：

```json
{
  "shengyuSomaticVariants": [],
  "shengyuGermlineVariants": []
}
```

圣域规则由 Java 模块实现：

- NKB 当前癌种或任一父级属于乳腺癌、卵巢癌、前列腺癌时，`shengyuSomaticVariants` 按产品启用基因输出。
- 其他实体瘤的 `shengyuSomaticVariants` 是空列表，但 `shengyuGermlineVariants` 仍按产品启用基因输出。
- 两个 1238 产品的目标基因均为 `BRCA1`、`BRCA2`、`PALB2`、`RAD51C`、`RAD51D`、`BRIP1`、`BARD1`、`FANCD2`。
- Python 模板只消费 Java 的最终字段，不重复实现癌种或基因规则。

### 7.8 同业务不同模板

| 差异类型 | 处理方式 |
| -------- | -------- |
| 仅 DOCX 排版不同 | 两个模板共用同一条模块流水线 |
| 空值显示 `/` 或 `-` 不同 | 对应个性化 Handler 直接写入最终展示值，Python 不判断客户规则 |
| 固定标题、平台、说明文字不同 | 直接保存在各自 DOCX 中 |
| 字段名称相同但业务来源不同 | 使用新的个性化模块，由新 Handler 明确实现 |
| 癌种条件、质控规则不同 | 使用新的个性化模块，不要写进模板配置 JSON |

### 7.9 当前实现状态

| 项目 | 当前行为 |
| ---- | -------- |
| 模板选择 | `ReportTemplateDataService` 查询启用模板并解析分号模块流水线 |
| 公共数据 | `CommonReportModuleAssembler` 无条件生成 |
| 个性化数据 | 三个圣域 Handler 分别生成体细胞、胚系和质控字段 |
| 业务配置 | 运行时不读取 `manifest.json`；癌种规则在 Java，基因来自 `product_gene` |
| 胚系解释 | 来源为 `history_germline.clinical_significance`，支持五级人工修改和改靶继承 |
| JSON DTO | `ReportTemplateData`，当前 `schemaVersion=1.2` |
| 示例 JSON | `payload.example.json` 仅用于说明和契约测试，不参与运行时决策 |
| 正式生成 | Java 已重新构建 JSON、写 JSON 文件并调用 Python 渲染 DOCX；数据库登记两个文件路径 |
| PDF | OnlyOffice PDF 转换尚未接入当前 `ReportGenerationService` |

`V9` 至 `V19` 已覆盖模板、模块流水线、产品基因、制品路径、质控、临床意义和 1238 产品配置。其他语言重写应复用相同数据库语义和 JSON 字段，不应恢复单 Handler、`rp_cr` 胚系判定或 `report_detail` 存储方案。

## 8. 预览接口

### 8.1 模板数据预览

```text
POST /admin/report/template/data
```

请求：

```json
{
  "analysisId": 123,
  "reportId": 456,
  "templateCode": "pharma-shengyu"
}
```

当前执行过程：

1. 根据 `analysisId + reportId` 查询报告上下文。
2. 根据 `templateCode` 查询并校验报告模板。
3. 读取模板记录的 `module_code`。
4. 先调用 `CommonReportModuleAssembler` 生成公共字段。
5. 从 `ReportModuleHandlerRegistry` 取得有序个性化流水线并逐个 `apply`。
6. 返回 `ReportTemplateData`，不写数据库和预览文件。

成功结果中的业务部分示例：

```json
{
  "schemaVersion": "1.2",
  "templateCode": "pharma-shengyu",
  "analysisId": 123,
  "reportId": 456,
  "sampleInfo": {},
  "somaticVariants": { "summary": {}, "items": [] },
  "germlineVariants": { "summary": {}, "items": [] },
  "shengyuSomaticVariants": [],
  "shengyuGermlineVariants": [],
  "warnings": []
}
```

预览只返回响应，不更新 `analysis_report.report_json_path`，也不创建预览文件。前端修改报出状态、临床意义或改靶后重新调用接口即可获得新 JSON。

解读相关接口：

```text
POST /admin/report/interpretation/preview
GET  /admin/report/interpretation/drug-preview
GET  /admin/report/interpretation/mutation-candidates
POST /admin/report/interpretation/target
POST /admin/report/interpretation/germline-clinical-significance
```

模板模块复用 `ReportDrugPreviewService`、`ReportGermlinePreviewService` 等现有查询和匹配能力，不能复制另一套用药算法。

### 8.2 错误语义

- HTTP 参数、权限或报告归属错误：返回请求失败。
- 必填业务数据缺失：当前服务抛出明确业务错误；后续若引入 `canGenerate`，需保持相同校验口径。
- 非关键数据缺失：使用 JSON `null` 并写入 `warnings`，不得在 Java 中替换成展示符号。
- `module_code` 未注册：模板配置错误，禁止预览和生成。
- 未知系统异常：统一错误处理记录内部日志，对前端返回稳定错误码，不返回 SQL、路径或堆栈。

## 9. 正式生成

### 9.1 接口

```text
POST /admin/report/template/generate
```

请求：

```json
{
  "analysisId": 123,
  "reportId": 456,
  "templateCode": "pharma-shengyu"
}
```

前端不提交预览 JSON。服务端执行：

1. 校验请求、模板和当前管理员身份；模板编码为空时使用配置的默认模板。
2. 调用 `ReportTemplateDataService.build` 重新加载全部当前数据并执行公共/个性化模块。
3. 在 `<artifact-root>/<reportId>/` 生成带时间和随机串的唯一文件名前缀。
4. 通过临时文件和原子移动写入格式化 JSON。
5. 调用 `ReportDocxRenderer`，由 Python 使用该 JSON 渲染 DOCX。
6. 成功后更新 `template_id`、`template`、`report_json_path`、`report_file_raw_path`、生成人和生成时间。
7. 返回 DOCX 文件资源；失败时删除本次尚未成功的 JSON/DOCX。

当前实现尚未包含基于状态条件的并发生成锁、JSON SHA-256、`GENERATING/PENDING_REVIEW` 状态流转和 PDF 转换。其他语言重写若要增加这些能力，应作为显式增强并补充数据库迁移与并发测试。

### 9.2 最终 JSON

正式 JSON 与模板数据预览使用同一份 `ReportTemplateData`，不增加 `modules` 包装层：

```json
{
  "schemaVersion": "1.2",
  "templateCode": "pharma-shengyu",
  "templateVersion": "v1",
  "analysisId": 123,
  "reportId": 456,
  "sampleInfo": {},
  "somaticVariants": { "summary": {}, "items": [] },
  "germlineVariants": { "summary": {}, "items": [] },
  "shengyuSomaticVariants": [],
  "shengyuGermlineVariants": [],
  "warnings": []
}
```

`schemaVersion` 用于 Python 识别 JSON 协议，不代表数据库快照版本。`module_code` 保存在模板记录中，用于 Java 选择处理器；一般不需要放入渲染 JSON。正式 JSON 不包含处理器执行堆栈、数据库连接信息或前端临时状态。

每次生成都创建新的 JSON/DOCX 文件并把数据库路径更新到最新制品，因此再次生成时内容可以变化。已经生成的旧 JSON 文件不会被原地修改，但当前代码也没有制品版本表；若审核和发送必须永久绑定某次生成，应在重写时增加不可变制品记录或严格状态锁。

## 10. Python 渲染边界

当前 Python 契约：

```text
python scripts/render_report_docx.py <template_path> <json_path> <output_docx_path>
```

Java 负责把最终 JSON 写到受控临时文件或通过标准输入传递。命令参数必须使用参数数组构造，不拼接 Shell 字符串。

Python 只负责：

1. 校验模板、UTF-8 JSON 对象和输出路径。
2. 使用严格变量模式的 docxtpl/Jinja2 渲染 DOCX。
3. 校验输出为有效 DOCX 且没有残留模板标记。
4. 通过同目录临时文件原子替换最终文件。
5. 在标准输出写入单行 JSON 供 Java 解析。

OnlyOffice 转 PDF 尚未接入，模板特定的数据适配和客户默认值均不进入 Python。

成功输出：

```json
{
  "status": "GENERATED",
  "outputPath": "<path>",
  "outputBytes": 856565,
  "sha256": "<sha256>"
}
```

失败输出：

```json
{
  "status": "ERROR",
  "errorType": "TemplateRenderError",
  "message": "<safe-message>"
}
```

Python 不再执行以下操作：

- 创建或更新报告业务记录。
- 查询 LIMS、产品、癌种、位点或 NKB。
- 决定报告状态流转。
- 接收或打印密码、Token 等敏感配置。

## 11. 审核、重出和发送

### 11.1 审核

- 只有 `PENDING_REVIEW` 可以审核。
- 产出人不能审核自己的报告。
- 审核通过更新为 `APPROVED`。
- 驳回更新为 `REJECTED`，驳回原因必填。
- 审核动作使用状态条件更新，避免重复审核。

### 11.2 重出

- 从 `REJECTED` 报告创建新 `analysis_report`。
- 复制分析、产品和模板 ID，但不复制旧 JSON/DOCX/PDF 路径。
- 设置 `source_report_id` 和递增的 `version_no`。
- 新报告重新读取当前 LIMS 和共享位点状态，再执行预览与生成。
- 旧报告永久保留，不覆盖、不逻辑删除。

### 11.3 邮件

- 收件地址默认来自实时 LIMS 候选，发送前由用户确认。
- 只允许发送 `APPROVED` 或 `SENT` 报告的已生成 PDF。
- 沿用 `analysis_report_email_log` 记录每次发送的 `trace_id`、收件人、状态和失败摘要。
- 邮件失败不改变审核结果；至少一次成功后报告状态可更新为 `SENT`。
- 应用日志中的邮箱必须脱敏。

## 12. 分阶段实施顺序

每一阶段都应保持改动小、可以独立测试和回退。上一阶段验收通过后再进入下一阶段，不同时修改数据库、JSON、DOCX 和 Python。

### 阶段 0（已完成）：冻结当前 JSON 原型

当前已有：

- `ReportTemplateData`：第一版短路径 DTO。
- `ReportTemplateDataService`：圣域 HRR 原型组装器。
- `POST /admin/report/template/data`：第一版预览入口。
- `payload.example.json`：结果示例。
- 原始 `template.docx`：尚未完成渲染占位符改造。

本阶段只做基线确认：

1. 保留现有单测。
2. 确认相同输入重复调用得到相同 JSON。
3. 确认缺失值为 `null`。
4. 确认非适用癌种输出空的 `shengyuSomaticVariants`，但仍生成 `shengyuGermlineVariants`。

完成标准：当前 `ReportTemplateDataServiceTest` 全部通过。该阶段已经具备基础实现。

### 阶段 1（迁移已创建）：给模板增加 `module_code`

涉及内容：

1. 先检查当前 `report_template` 的真实表结构和实体，不直接照抄草案。
2. 新增 Flyway 迁移，为 `report_template` 增加 `module_code VARCHAR(120)`。
3. 现有圣域模板配置当前三个圣域个性化模块流水线。
4. 模板新增、编辑和启用时校验 `module_code` 非空。
5. 暂时不删除当前 manifest，也不调整 JSON 组装逻辑。

测试：

- 迁移可以在 MySQL 5.7 执行。
- 原有模板正确配置三个圣域模块且顺序稳定。
- 未填写或未注册的编码不能启用。

完成标准：数据库可以稳定返回模板的 `module_code`，现有预览行为不变。

### 阶段 2（已完成）：建立 Handler 接口和注册器

新增：

```text
ReportModuleHandler.java
ReportModuleHandlerRegistry.java
```

实施步骤：

1. 定义 `moduleCode()` 和 `build(context)`。
2. 注册器通过 Spring 注入 `List<ReportModuleHandler>`。
3. 启动时检查重复编码，发现重复直接启动失败。
4. 查询未知编码时返回明确配置错误。
5. 先建立一个适配 Handler，内部仍调用现有 `ReportTemplateDataService`，不移动业务代码。

测试：

- 三个圣域模块都可以查到并按配置顺序执行。
- 未知编码失败。
- 两个 Handler 使用相同编码时失败。

完成标准：接口已经通过 registry 调用，但返回 JSON 与阶段 0 完全一致。

### 阶段 3（已完成）：把圣域逻辑拆分为个性化模块

实施步骤：

1. 新增 `ShengyuSomaticVariantsV1ModuleHandler`、`ShengyuGermlineVariantsV1ModuleHandler`、`ShengyuQcV1ModuleHandler`。
2. 把样本信息组装移动到 `buildSampleInfo`。
3. 把乳腺、卵巢、前列腺癌判断移动到 `isSomaticApplicable`。
4. 把体系和胚系基因固定顺序移动到 HRR V1 代码常量或专属规则类。
5. 把四套质控标准移动到 HRR V1 专属规则类。
6. 复用现有 `ReportDrugPreviewService`、`ReportGermlinePreviewService` 和 `ReportQcPreviewService`，不复制查询 SQL。
7. `ReportTemplateDataService` 收缩为：加载上下文、查询模板、按 `module_code` 找 Handler。

测试：

- 乳腺癌启用体系结果。
- 肺癌关闭体系结果并返回 `null`。
- 组织和血液样本选择正确的数据表开关及质控口径。
- 缺失变异生成固定基因空行。
- 当前结果 JSON 与 `payload.example.json` 契约一致。

完成标准：业务规则已不再从 manifest 读取，现有接口响应字段不变化。

### 阶段 4（已完成）：删除复杂 manifest 运行时依赖

实施步骤：

1. 将 `templateCode`、`templatePath`、`templateSha256`、`moduleCode` 统一从 `report_template` 读取。
2. 固定标题、检测平台和说明文字继续保留在 DOCX。
3. `payload.example.json` 继续保留，但只作为说明和契约测试数据。
4. 确认没有代码再读取 `manifest.json` 后删除该文件。
5. 空值仍保持 `null`；`/`、`-` 的区别留给 DOCX/Python 展示层。

完成标准：运行时只依赖数据库模板记录、DOCX 和 Java Handler，不依赖业务配置 JSON。

### 阶段 5（待实现）：完善上下文和生成前校验

实施步骤：

1. 提取 `ReportContextLoader`。
2. 一次加载报告、分析、模板、LIMS、文件和报出位点。
3. 明确样本采集日期、切片日期、访视周期等当前缺失字段的真实来源。
4. 区分必填错误与可选告警。
5. 预览接口返回 `canGenerate`、`warnings`、`errors`，业务 DTO 仍保持短路径。

完成标准：缺失核心数据时不能正式生成，普通缺失字段保持 `null` 且可预览。

### 阶段 6（部分完成）：改造 DOCX 模板

实施步骤：

1. 从原始 DOCX 复制一个新版本，保留旧文件和哈希。
2. 把静态示例数据逐项替换为 `sampleInfo.xxx` 等短路径占位符。
3. 使用空或非空的 `shengyuSomaticVariants` 控制体细胞章节内容。
4. 使用 `showTissueBloodSample` 和 `showBloodOnlySample` 控制两个样本表。
5. 使用循环渲染体系、胚系和质控列表。
6. Java Handler 按模板业务口径将空值转换为 `/` 或 `-`，模板只输出最终值。
7. 完成 DOCX 和 PDF 视觉回归检查。

完成标准：使用 `payload.example.json` 可以独立生成版式正确的 DOCX。

### 阶段 7（部分完成）：接入 Python 正式生成

实施步骤：

1. 生成接口重新加载上下文并执行 Handler，不采用前端预览 JSON。
2. 冻结最终 JSON 和 SHA-256。
3. Java 将模板路径和冻结 JSON 交给 Python。
4. Python 纯 JSON 到 DOCX 渲染器及 Java 调用已实现；PDF 转换待接入。
5. 成功后更新报告文件路径及状态，失败时保留安全错误摘要。

完成标准：同一份冻结 JSON 可以重复生成相同业务内容，Python 不查询业务数据库。

### 阶段 8（待实现）：增加第二个模板验证扩展方式

优先选择一个与圣域 HRR 业务相同、排版不同的模板：

1. 新增模板文件和 `report_template` 记录。
2. 两个模板填写相同的分号模块流水线。
3. 不修改 Java 业务代码即可生成两种版式。
4. 如果第二个模板确实存在不同业务口径，再新增 `HRR_V2` Handler。

完成标准：证明“新增同业务模板不改 Java；新增业务口径只增加 Handler”，而不是重新引入大型配置文件。

## 13. 验收用例

### 13.1 预览

- 报出位点变化后再次预览，返回 JSON 立即变化，数据库没有新增预览记录。
- LIMS 修改后再次预览使用最新值。
- 模板的 `module_code` 能按分号顺序选择唯一注册的 Handler；空配置只生成公共字段，未知或重复编码禁止预览。
- 核心业务数据缺失时返回明确业务错误；非关键字段缺失时返回 `null` 或告警。
- 非适用癌种返回空的 `shengyuSomaticVariants`，同时保留胚系个性化结果。
- 相同数据库状态下重复预览产生业务内容一致的 JSON；匹配时间等审计字段按现有 DTO 语义处理。
- 胚系五级临床意义保存后立即反映到预览和模板 JSON。
- 胚系改靶后药物历史键变化，但临床意义保持不变；癌种或性别变化时不得错误继承。

### 13.2 多报告

- 一个 `analysis_id` 可用两个允许的模板创建两个 `report_id`。
- 两份未生成报告共享位点状态，最后一次修改会反映到两份后续预览。
- 第一份生成后再修改位点并生成第二份，两次生成产生不同 JSON 文件，数据库路径指向各自报告的最新制品。
- 驳回重出链和不可变制品版本属于后续能力，当前验收不得假定已有 `version_no`。

### 13.3 生成

- 生成接口重新计算数据，不采用前端旧预览。
- 成功生成 JSON/DOCX 文件，并更新 `report_json_path`、`report_file_raw_path`、模板及生成人信息。
- 再次生成创建新文件并使用最新业务数据，数据库路径更新到最新制品。
- 模板或 Python 渲染失败时删除本次临时制品，不登记无效路径。
- JSON 哈希、PDF、生成状态锁和不可变制品版本需在后续增强中单独验收。

### 13.4 逻辑关联

- 不存在或停用的产品、模板、基因 ID 无法写入关系表或报告。
- 不属于当前分析批次的位点无法更新。
- 产品未关联的模板无法用于创建报告。
- `module_code` 为空是合法公共模板；未注册或重复编码禁止执行。
- 被报告引用的产品和模板只能停用，不能物理删除。
- 全部表结构不包含物理外键，但关联列均有查询索引。

## 14. 代码落点

后续实现从以下真实路径开始：

- 数据驱动：`docs/report-driver/scanner.py`、`docs/report-driver/core/file_scanner.py`
- Python 旧生成器：`docs/report-driver/report/gen_reports.py`
- Java 报告服务：`biotech-backend/src/main/java/com/cool/modules/report/`
- 当前短路径 DTO：`biotech-backend/src/main/java/com/cool/modules/report/dto/ReportTemplateData.java`
- 当前 JSON 原型服务：`biotech-backend/src/main/java/com/cool/modules/report/service/ReportTemplateDataService.java`
- 公共 JSON 组装：`biotech-backend/src/main/java/com/cool/modules/report/service/template/CommonReportModuleAssembler.java`
- 个性化模块注册：`biotech-backend/src/main/java/com/cool/modules/report/service/template/ReportModuleHandlerRegistry.java`
- 胚系预览与五级解释：`biotech-backend/src/main/java/com/cool/modules/report/service/ReportGermlinePreviewService.java`
- 体细胞/胚系历史：`biotech-backend/src/main/java/com/cool/modules/report/service/ReportMatchHistoryService.java`
- 正式 JSON/DOCX 生成：`biotech-backend/src/main/java/com/cool/modules/report/service/ReportGenerationService.java`
- 当前预览接口：`biotech-backend/src/main/java/com/cool/modules/report/controller/admin/AdminReportTemplateController.java`
- 圣域模板与示例：`biotech-backend/src/main/resources/report-templates/pharma-shengyu/v1/`
- 当前契约测试：`biotech-backend/src/test/java/com/cool/modules/report/service/ReportTemplateDataServiceTest.java`
- 数据库迁移：`biotech-backend/src/main/resources/db/migration/`
- 最新关键迁移：`V18__history_germline_clinical_significance.sql`、`V19__sync_1238_product_report_configuration.sql`
- 前端解读页：`biotech-frontend/src/views/report/interpretation/index.vue`

流程图见 [报告模块流程图](./report-module-flow.md)。其中 Java 模块化部分仍是旧的多模块聚合图，应在阶段 2 完成后按本文 `module_code -> Handler` 流程同步更新。
