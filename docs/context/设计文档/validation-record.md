# 3Q 验证记录模块设计文档

> 路由：/compliance/validation-records | 角色：operation_admin（管理） / 全部 4 角色（查看）

---

## 1. 数据模型

### validation_record（验证记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | uuid | PK, defaultRandom | 主键 |
| validation_type | varchar(20) | NOT NULL | IQ（安装确认）/ OQ（运行确认）/ PQ（性能确认） |
| title | varchar(200) | NOT NULL | 验证标题 |
| version | varchar(50) | - | 版本号 |
| executed_by | varchar(80) | - | 执行人 |
| executed_date | date | - | 执行日期 |
| result | varchar(20) | NOT NULL | passed（通过）/ failed（未通过）/ na（不适用） |
| summary | text | - | 验证摘要 |
| _created_at | timestamptz | NOT NULL | 创建时间 |
| _updated_at | timestamptz | NOT NULL | 更新时间 |
| _updated_by | user_profile | - | 更新者 |

**索引**：`idx_validation_record_type`(validation_type)、`idx_validation_record_date`(executedDate)

---

## 2. API 接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /api/validation-record | read:ComplianceRecord | 全量列表（按 executedDate DESC, createdAt DESC 排序） |
| POST | /api/validation-record/save | manage:ComplianceRecord | 新增/更新 |
| DELETE | /api/validation-record/:id | manage:ComplianceRecord | 删除 |

---

## 3. 功能流程

```mermaid
flowchart TD
    A[页面：3Q验证记录\n/compliance/validation-records] --> B[GET /api/validation-record\n全量加载]
    B --> C[渲染表格：类型可筛选\n(IQ/OQ/PQ) 标题/版本/执行人/结果/日期]
    C --> D{manage:ComplianceRecord?}
    D -->|是| E[显示操作按钮]
    D -->|否| F[只读浏览]
    
    E --> G[点击「新增」或行内「编辑」]
    G --> H[ValidationRecordFormDialog\n类型*(Select)/标题*/版本/执行人/日期/结果*(Select)/摘要]
    H --> I[POST /api/validation-record/save]
    I --> J[刷新列表]
    
    E --> K[行内「删除」]
    K --> L[AlertDialog 确认]
    L --> M[DELETE /api/validation-record/:id]
    M --> J
    
    classDef unimplemented stroke-dasharray: 5 5
```

---

## 4. 角色与权限

| 角色 | read:ComplianceRecord | manage:ComplianceRecord |
|------|:---:|:---:|
| wet_lab | ✅ | ❌ |
| bioinformatician | ✅ | ❌ |
| interpreter | ✅ | ❌ |
| operation_admin | ✅ | ✅ |