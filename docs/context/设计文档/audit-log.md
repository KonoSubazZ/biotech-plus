# 审计日志模块设计文档

> 路由：/audit/logs | 角色：wet_lab / bioinformatician / interpreter / operation_admin

---

## 1. 数据模型

### audit_log（审计日志表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | uuid | PK, defaultRandom | 主键 |
| user_id | varchar(64) | NOT NULL | 操作人 ID |
| user_name | varchar(120) | - | 操作人姓名 |
| action | varchar(50) | NOT NULL | 操作动作（如 create_company、view、approve 等） |
| resource_type | varchar(50) | NOT NULL | 操作对象类型 |
| resource_id | varchar(100) | - | 对象 ID（可选） |
| detail | text | - | JSON 详情描述 |
| _created_at | timestamptz | NOT NULL | 操作时间 |

**索引**：`idx_audit_log_user`(user_id)、`idx_audit_log_resource`(resource_type)、`idx_audit_log_time`(createdAt)

---

## 2. API 接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | /api/audit/logs/page | read:AuditLog | 审计日志分页查询 |

**请求体**：
```json
{
  "page": 1,
  "size": 10,
  "userId": "可选-操作人ID精确",
  "action": "可选-动作关键词",
  "resourceType": "可选-资源类型",
  "startTime": "可选-查询起始",
  "endTime": "可选-查询截止"
}
```

**响应体**：
```json
{
  "list": [
    {
      "id": "uuid",
      "userId": "user_123",
      "userName": "张三",
      "action": "create_company",
      "resourceType": "company",
      "resourceId": "comp_uuid",
      "detail": "{\"name\":\"测试公司\",\"code\":\"TEST\"}",
      "createdAt": "2026-09-17T10:00:00.000Z"
    }
  ],
  "pagination": { "page": 1, "size": 10, "total": 50 }
}
```

---

## 3. 业务逻辑

- **AuditService** 被所有业务模块注入调用，提供统一的 `record(entry)` 方法
- `AuditEntry` 接口：userId + userName? + action + resourceType + resourceId? + detail?
- 审计记录无删除/修改接口（只追加、不回溯），保证审计完整性
- 查询支持多字段筛选：操作人、动作关键词、资源类型、时间范围

---

## 4. 功能流程

```mermaid
flowchart TD
    A[任意业务模块] --> B[调用 AuditService.record\n{userId, userName, action, resourceType, ...}]
    B --> C[INSERT INTO audit_log]
    
    D[页面：审计日志\n/audit/logs] --> E[输入搜索条件\n操作人/动作/资源类型/日期范围]
    E --> F[POST /api/audit/logs/page]
    F --> G[DB 分页查询 + 筛选]
    G --> H[渲染表格\n时间/操作人/动作/资源/ID/详情]

    subgraph 审计埋点列表
        I["company: create/update/delete_company"]
        J["company_access: apply/approve/reject_company_access"]
        K["report: 6 类操作"]
        L["qc: view"]
        M["compliance/doc: create/update/delete_compliance_document"]
        N["dev_log: create/update/delete_dev_log"]
        O["validation_record: create/update/delete_validation_record"]
        P["qc_standard: create/update/delete_qc_standard"]
        Q["product_config: create/update/delete_product_config"]
    end
```

---

## 5. 角色与权限

| 角色 | read:AuditLog |
|------|:---:|
| 全部 4 角色 | ✅ |