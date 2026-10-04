# 报告模块流程图

> 适用范围：`scanner.py` 数据入库、报告解读、位点筛选、内存预览、报告生成、审核和邮件发送。
>
> 本文描述简化后的目标架构。所有表关系均为逻辑关联，不建立数据库物理外键。详细表结构和模块接口见 [报告模块设计书](./report-module-design.md)。

## 1. 端到端业务流程

```mermaid
flowchart TD
    A[外部系统调用 scanner.py] --> B[解析目录和文件名元数据]
    B --> C{analysis_data 是否存在}
    C -->|否| D[创建分析批次<br/>样本 + 产品 + 分析日期]
    C -->|是| E[复用 analysis_id 并进入重驱]
    D --> F[创建或复用 data_file_status]
    E --> F
    F --> G[按文件类型调用 Processor]
    G --> H[(file_Somatic_SNV_Indel<br/>file_CNV / file_Fusion / file_CR_ALL<br/>以及其他 file_* 明细表)]
    G --> I[更新文件状态<br/>Loaded / Error]
    I --> J[更新 analysis_data<br/>LOADED / PARTIAL]

    J --> K[报告管理列表]
    K --> L{用户操作}
    L -->|继续已有报告| M[按 report_id 进入解读]
    L -->|不同模板新报告| N[选择产品允许的模板]
    L -->|驳回后重出| O[基于旧报告创建新 report_id]
    N --> P[创建 analysis_report<br/>version_no = 1]
    O --> Q[记录 source_report_id<br/>version_no + 1]
    P --> M
    Q --> M

    M --> R[LIMS 信息<br/>按 subbarcode = BARCODE 动态读取]
    M --> S[集群对接<br/>读取 data_file_status]
    M --> T[筛选位点<br/>更新共享 file_* .is_reported]
    R --> U[报告预览]
    T --> U
    U --> V[ReportContextLoader<br/>一次加载报告上下文]
    V --> VH{位点完整业务键<br/>是否命中匹配历史}
    VH -->|命中| VI[复用 history_somatic / history_germline<br/>冻结的位点匹配 JSON]
    VH -->|未命中| W[ReportMatchOrchestrator<br/>执行适用的 Java 模块]
    W --> VJ[首次写入位点匹配历史]
    VI --> X
    VJ --> X
    X[聚合内存预览 JSON<br/>不保存整份预览]
    X --> Y{必需模块是否成功}
    Y -->|否| Z[返回错误并禁止生成]
    Y -->|是| AA[返回预览和可选模块告警]

    AA --> AB[点击生成报告]
    AB --> AC[服务端重新加载实时数据<br/>重新执行同一模块流水线]
    AC --> AD{生成前校验}
    AD -->|失败| Z
    AD -->|通过| AE[冻结最终 JSON 到 report_detail<br/>保存 report_json_hash]
    AE --> AF[Python + docxtpl/Jinja2<br/>渲染 template.docx]
    AF --> AG[OnlyOffice 转 PDF]
    AG --> AH{文件生成成功}
    AH -->|否| AI[记录失败原因<br/>保留为可重新生成状态]
    AH -->|是| AJ[保存 DOCX/PDF 路径<br/>状态 PENDING_REVIEW]
    AJ --> AK{其他解读员审核}
    AK -->|驳回| AL[本报告 REJECTED]
    AL --> O
    AK -->|通过| AM[状态 APPROVED]
    AM --> AN[确认收件人并发送 PDF]
    AN --> AO[追加邮件发送审计]
    AO --> AP{发送结果}
    AP -->|成功| AQ[状态 SENT]
    AP -->|失败| AR[保留 APPROVED<br/>允许重新发送]
```

## 2. 逻辑数据关系

```mermaid
erDiagram
    ANALYSIS_DATA ||--o{ DATA_FILE_STATUS : contains
    DATA_FILE_STATUS ||--o{ FILE_DETAIL : parses_to
    ANALYSIS_DATA ||--o{ ANALYSIS_REPORT : produces
    FILE_DETAIL ||--o{ HISTORY_SOMATIC : somatic_match
    FILE_DETAIL ||--o{ HISTORY_GERMLINE : germline_match

    PRODUCT ||--o{ PRODUCT_GENE : contains
    GENE ||--o{ PRODUCT_GENE : selected_by
    PRODUCT ||--o{ PRODUCT_TEMPLATE : allows
    REPORT_TEMPLATE ||--o{ PRODUCT_TEMPLATE : assigned_to

    ANALYSIS_REPORT ||--o{ REPORT_EMAIL_LOG : sends
    ANALYSIS_REPORT o|--o{ ANALYSIS_REPORT : reissues

    ANALYSIS_DATA {
        bigint analysis_id PK
        varchar subbarcode
        int product_id
        varchar product
        varchar analysis_date
        varchar drive_status
    }
    DATA_FILE_STATUS {
        int file_id PK
        bigint analysis_id
        varchar file_type
        varchar status
    }
    FILE_DETAIL {
        bigint id PK
        int file_id
        boolean is_reported
    }
    HISTORY_SOMATIC {
        bigint id PK
        char match_key UK
        tinyint key_version
        varchar gene
        varchar variant
        varchar disease
        varchar gender
        varchar customer
        varchar project_code
        json match_result
        datetime last_reused_at
        int reuse_count
    }
    HISTORY_GERMLINE {
        bigint id PK
        char match_key UK
        tinyint key_version
        varchar gene
        varchar variant
        varchar disease
        varchar gender
        varchar customer
        varchar project_code
        json match_result
        datetime last_reused_at
        int reuse_count
    }
    ANALYSIS_REPORT {
        int report_id PK
        bigint analysis_id
        int product_id
        bigint template_id
        int source_report_id
        int version_no
        json report_detail
        char report_json_hash
        varchar status
    }
    PRODUCT_GENE {
        int product_id
        bigint gene_id
    }
    PRODUCT_TEMPLATE {
        int product_id
        bigint template_id
    }
```

说明：

- 图中的连线只表达业务关系，数据库不创建物理外键。
- `FILE_DETAIL` 是逻辑名称，实际对应 `file_Somatic_SNV_Indel`、`file_CNV`、`file_Fusion`、`file_CR_ALL` 等表。
- `file_* .is_reported` 是分析级共享状态。不同报告在正式生成前可能互相影响；正式生成后以各自的 `report_detail` 为准。
- `history_somatic/history_germline` 保存单个位点**首次**匹配结果（复用键 v2 = 产品项目 + 癌种 + 性别 + 位点 + 人工改靶父级）：
  命中即**严格只读复用**（NKB 更新不刷新），并更新 `last_reused_at` / `reuse_count` + 写一条 `history_reuse_log` 留痕；
  完整预览仍只在内存中聚合，不落库。

## 3. Java 模块化预览流程

```mermaid
flowchart LR
    A[Preview/Generate API] --> B[ReportContextLoader]
    B --> B1[analysis_report]
    B --> B2[实时 LIMS]
    B --> B3[data_file_status]
    B --> B4[file_* 中 is_reported = 1 的数据]
    B --> B5[product / gene / template]

    B --> C[ReportMatchOrchestrator]
    C --> D{遍历 Spring 注入的 ReportModule}
    D --> E[supports context]
    E -->|否| F[跳过模块]
    E -->|是| G[检查体系/胚系匹配历史]
    G -->|命中| I[写入冻结的 modules.code.data]
    G -->|未命中| H{执行 build 结果}
    H -->|成功| HS[首次保存位点匹配历史]
    HS --> I
    H -->|必需模块失败| J[canGenerate = false]
    H -->|可选模块失败| K[追加 warning]
    I --> L[聚合 ReportMatchResult]
    J --> L
    K --> L
    F --> L
    L --> M{调用模式}
    M -->|PREVIEW| N[返回内存 JSON]
    M -->|GENERATE| O[冻结最终 JSON 后交给 Python]
```

模块由代码自行定义 `code`、顺序、适用条件和必需性，不建设模块配置表。样本信息、位点、靶向用药、化疗、免疫、质控只是首批模块示例，不限制以后新增其他模块。

## 4. 报告状态机

```mermaid
stateDiagram-v2
    [*] --> INTERPRETING: 创建 report_id
    INTERPRETING --> GENERATING: 重新匹配且校验通过
    GENERATING --> INTERPRETING: 生成失败
    GENERATING --> PENDING_REVIEW: DOCX/PDF 生成成功
    PENDING_REVIEW --> APPROVED: 审核通过
    PENDING_REVIEW --> REJECTED: 审核驳回
    REJECTED --> INTERPRETING: 创建新 report_id 重出
    APPROVED --> SENT: 邮件发送成功
    SENT --> [*]
```

状态约束：

- `INTERPRETING` 阶段允许修改共享位点状态并重复预览。
- 生成开始前必须重新执行全部适用模块，不能直接采用前端缓存的预览 JSON。
- `PENDING_REVIEW` 后不得覆盖 `report_detail`、JSON 哈希、模板或报告文件。
- 驳回后创建新报告，不覆盖旧报告的 JSON、DOCX、PDF 和审核记录。
- 邮件失败不回退审核状态，发送结果由邮件日志记录。

## 5. 预览和生成时序

```mermaid
sequenceDiagram
    actor U as 解读员
    participant FE as 前端
    participant API as Java 报告服务
    participant DB as omics DB
    participant NKB as NKB
    participant PY as Python 渲染器
    participant OO as OnlyOffice

    U->>FE: 修改位点报出状态
    FE->>API: 更新 file_* .is_reported
    API->>DB: 校验 analysisId 和位点归属后更新
    API-->>FE: 更新成功

    U->>FE: 打开/刷新预览
    FE->>API: POST /interpretation/preview
    API->>DB: 查询报告、实时 LIMS、文件和报出位点
    API->>NKB: 模块按需查询癌种和用药证据
    API->>API: 内存执行模块并聚合 JSON
    API-->>FE: canGenerate + modules + warnings/errors

    U->>FE: 点击生成
    FE->>API: POST /interpretation/generate
    API->>DB: 重新查询全部输入
    API->>NKB: 重新执行匹配
    API->>API: 校验必需模块并生成最终 JSON
    API->>DB: 保存 report_detail 和 report_json_hash
    API->>PY: reportId + templatePath + finalJson
    PY->>PY: docxtpl/Jinja2 渲染 DOCX
    PY->>OO: DOCX 转 PDF
    OO-->>PY: PDF
    PY-->>API: 状态 + DOCX/PDF 路径或错误码
    API->>DB: 更新路径和报告状态
    API-->>FE: 最终生成结果
```

## 6. 多报告及边界行为

- 不同模板：同一个 `analysis_id` 创建不同 `report_id`，通过 `product_template` 校验产品是否允许该模板。
- 驳回重出：新报告保存旧 `report_id` 到 `source_report_id`，`version_no` 在旧版本基础上加一。
- 多草稿：不加锁、不限制并行解读，接受共享 `file_* .is_reported` 导致最后一次修改影响全部未生成预览。
- 已生成报告：最终 JSON 已冻结，后续 LIMS、知识库或位点状态变化不会修改旧 `report_detail`。
- LIMS 多条：当前按 `BARCODE` 查询并取 `id` 最大的一条；没有记录或关键字段缺失时由对应必需模块阻止生成。
- 模块失败：必需模块失败阻止生成；可选模块失败保留预览并返回告警。
- Python/OnlyOffice 失败：不进入待审核，可修复后重新生成；成功后禁止覆盖原文件。
