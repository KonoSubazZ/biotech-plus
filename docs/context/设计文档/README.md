# 系统设计文档索引

> 生成时间：2026-09-17
> 应用范围：Biotech 管理系统（不含报告管理）

---

## 模块清单

| 序号 | 模块名称 | 路由 | 文档 | 流程图 |
|------|---------|------|------|--------|
| 1 | 湿实验质控 | /qc/quality-control | [spec.md](spec.md) | [wetlab-qc-flow.md](../../docs/flowchart/wetlab-qc-flow.md) |
| 2 | 生信质控 | /qc/bio-qc | [bioinfo-qc.md](bioinfo-qc.md) | 参考湿实验质控（qc_category 不同） |
| 3 | 公司管理 | /company/management | [company-management.md](company-management.md) | 嵌入文档 |
| 4 | 权限申请 | /company-access | [company-access.md](company-access.md) | 嵌入文档 |
| 5 | 权限审批 | /company-access/approval | [company-access.md](company-access.md) | 嵌入文档 |
| 6 | 审计日志 | /audit/logs | [audit-log.md](audit-log.md) | 嵌入文档 |
| 7 | 3Q 文档管理 | /compliance/documents | [compliance-docs.md](compliance-docs.md) | 嵌入文档 |
| 8 | 开发记录 | /compliance/dev-log | [dev-log.md](dev-log.md) | 嵌入文档 |
| 9 | 3Q 验证记录 | /compliance/validation-records | [validation-record.md](validation-record.md) | 嵌入文档 |
| 10 | 产品配置 | /system/product-config | [product-config.md](product-config.md) | 嵌入文档 |
| 11 | 数据周期管理 | /system/data-retention | [data-retention.md](data-retention.md) | 嵌入文档 |
| 12 | 系统管理 | /system/* | [system-management.md](system-management.md) | 嵌入文档 |

---

## 模块间数据关系总览

```mermaid
flowchart TD
    Company[公司 company] -->|公司权限申请| App[权限申请 company_access_application]
    App -->|审批通过后数据隔离| Report[报告 analysis_report]
    
    Product[产品 product_config] -->|FK| QcStd[质控标准 qc_standard]
    Product -->|FK| DPolicy[数据周期策略 data_retention_policy]
    Product -->|FK| Product
    Product -->|product_name 桥接| Sample[样本 sample_file]
    Sample -->|subbarcode| QCRec[质控记录 qc_record]
    QcStd -->|product_id+qc_item+qc_category| QCRec
    
    DPolicy -->|扫描到期| Archive[归档记录 data_archive_record]
    
    Compliance[合规文档 compliance_document] -.->|同 3Q 体系| VRecord[验证记录 validation_record]
    VRecord -.->|同 3Q 体系| DLog[开发记录 dev_log]
    
    Audit[审计日志 audit_log] -.->|统一埋点| 各业务模块

    subgraph 权限
        Perm[权限点位 authz_permissions]
        RolePerm[角色映射 authz_role_permissions]
    end
```

---

## 角色权限矩阵（汇总）

| 权限点位 | wet_lab | bioinformatician | interpreter | operation_admin |
|---------|:---:|:---:|:---:|:---:|
| read:QcControl | ✅ | ✅(bioinfo) | ❌ | ✅ |
| manage:QcStandard | ✅ | ❌ | ❌ | ✅ |
| read:ProductConfig | ✅ | ✅ | ✅ | ✅ |
| manage:ProductConfig | ❌ | ❌ | ❌ | ✅ |
| manage:DataRetention | ❌ | ❌ | ❌ | ✅ |
| manage:ComplianceDoc | ❌ | ❌ | ❌ | ✅ |
| read:ComplianceRecord | ✅ | ✅ | ✅ | ✅ |
| manage:ComplianceRecord | ❌ | ❌ | ❌ | ✅ |
| read:AuditLog | ✅ | ✅ | ✅ | ✅ |
| apply:CompanyAccess | ✅ | ✅ | ✅ | ✅ |
| approve:CompanyAccess | ❌ | ❌ | ❌ | ✅ |
| manage:Company | ❌ | ❌ | ❌ | ✅ |
| manage:Permission | ❌ | ❌ | ❌ | ✅ |