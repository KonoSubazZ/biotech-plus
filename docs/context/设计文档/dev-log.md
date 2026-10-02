# 开发记录模块设计文档

> 路由：/compliance/dev-log | 角色：operation_admin（管理） / 全部 4 角色（查看）

---

## 1. 数据模型

### dev_log（开发记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | uuid | PK, defaultRandom | 主键 |
| title | varchar(200) | NOT NULL | 记录标题 |
| category | varchar(20) | NOT NULL | 分类：feature（功能新增）/ fix（缺陷修复）/ change（变更调整） |
| content | text | - | 详细内容 |
| developer | varchar(80) | - | 开发人员 |
| log_date | date | - | 记录日期 |
| _created_at | timestamptz | NOT NULL | 创建时间 |
| _updated_at | timestamptz | NOT NULL | 更新时间 |
| _updated_by | user_profile | - | 更新者 |

**索引**：`idx_dev_log_date`(log_date)、`idx_dev_log_category`(category)

---

## 2. API 接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /api/dev-log | read:ComplianceRecord | 全量列表（按 logDate DESC, createdAt DESC 排序） |
| POST | /api/dev-log/save | manage:ComplianceRecord | 新增/更新 |
| DELETE | /api/dev-log/:id | manage:ComplianceRecord | 删除 |

---

## 3. 功能流程

```mermaid
flowchart TD
    A[页面：开发记录\n/compliance/dev-log] --> B[GET /api/dev-log\n全量加载]
    B --> C[渲染表格：标题/分类/开发者/日期/操作]
    C --> D{manage:ComplianceRecord?}
    D -->|是| E[显示「新增」和行内操作按钮]
    D -->|否| F[只读浏览]
    
    E --> G[点击「新增」或行内「编辑」]
    G --> H[DevLogFormDialog\n标题*/分类*(Select)/内容/开发者/日期]
    H --> I[POST /api/dev-log/save]
    I --> J[刷新列表]
    
    E --> K[行内「删除」]
    K --> L[AlertDialog 确认]
    L --> M[DELETE /api/dev-log/:id]
    M --> J
```

---

## 4. 角色与权限

| 角色 | read:ComplianceRecord | manage:ComplianceRecord |
|------|:---:|:---:|
| wet_lab | ✅ | ❌ |
| bioinformatician | ✅ | ❌ |
| interpreter | ✅ | ❌ |
| operation_admin | ✅ | ✅ |