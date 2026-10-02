# 数据周期管理模块设计文档

> 路由：/system/data-retention | 角色：operation_admin

---

## 1. 数据模型

### 1.1 data_retention_policy（数据周期策略表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | uuid | PK, defaultRandom | 主键 |
| name | varchar(120) | NOT NULL | 策略名称 |
| product_id | uuid | FK→product_config.id, 可为 null | 关联产品（null=全局策略） |
| retention_months | integer | NOT NULL | 保留月数 |
| start_basis | varchar(30) | NOT NULL | 起算基准：report_sent（报告发送日）/ analysis_date（分析日期） |
| storage_target | varchar(120) | - | 归档存储目标 |
| status | varchar(20) | NOT NULL, default 'active' | active / inactive |
| remark | varchar(500) | - | 备注 |
| _created_at / _created_by | - | 系统字段 | - |
| _updated_at / _updated_by | - | 系统字段 | - |

### 1.2 data_archive_record（数据归档记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | uuid | PK, defaultRandom | 主键 |
| report_id | uuid | UNIQUE, FK→analysis_report.reportId | 关联报告（唯一） |
| policy_id | uuid | FK→data_retention_policy.id | 关联策略 |
| due_date | timestamptz | - | 到期日期 |
| status | varchar(20) | NOT NULL, default 'pending' | pending（待归档）/ archived（已归档） |
| storage_target | varchar(120) | - | 归档存储目标 |
| archived_by | user_profile | - | 归档执行人 |
| archived_at | timestamptz | - | 归档时间 |
| remark | varchar(500) | - | 备注 |
| _created_at | timestamptz | NOT NULL | 创建时间 |

---

## 2. API 接口

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /api/data-retention/policies | manage:DataRetention | 全量策略列表 |
| POST | /api/data-retention/policies/save | manage:DataRetention | 新增/更新策略 |
| DELETE | /api/data-retention/policies/:id | manage:DataRetention | 删除策略 |
| POST | /api/data-retention/archives/page | manage:DataRetention | 待归档记录分页查询 |
| POST | /api/data-retention/archives/:id/mark-archived | manage:DataRetention | 标记为已归档 |

---

## 3. 自动化任务

**触发器**：`data_archive_daily_scan`（cron：每日 03:00，需用户在触发器面板手动激活）

**执行逻辑**：
1. 扫描所有 `status='active'` 的策略
2. 按策略 `productId` 读取对应报告
   - 产品策略（productId 非空）读取对应产品的报告
   - 全局策略（productId 为空）读取未被产品策略覆盖的报告
3. 取报告的 `startBasis` 日期字段（reportSentAt 或 analysisDate）
4. 到期日 = startBasis + retentionMonths
5. 到期日 < 今天的报告 → 插入 `data_archive_record`
6. `reportId` 唯一约束 + `onConflictDoNothing` 保证幂等

---

## 4. 功能流程

```mermaid
flowchart TD
    subgraph 策略配置
        A[页面：数据周期管理\n/system/data-retention] --> B[Tab「周期策略」]
        B --> C[GET /api/data-retention/policies\n全量策略列表]
        C --> D[「新增」/行内「编辑」]
        D --> E[PolicyFormDialog\n名称/产品(可选)/保留月数/起算基准/状态]
        E --> F[POST /api/data-retention/policies/save]
        F --> G[刷新策略列表]
        
        C --> H[行内「删除」]
        H --> I[AlertDialog 确认]
        I --> J[DELETE /api/data-retention/policies/:id]
        J --> G
    end

    subgraph 归档管理
        B --> K[Tab「待归档清单」]
        K --> L[POST /api/data-retention/archives/page\n分页查询 + status/产品筛选]
        L --> M[展示表格：报告ID/策略/到期日/状态/操作]
        M --> N[「标记已归档」行内操作]
        N --> O[POST /api/data-retention/archives/:id/mark-archived]
        O --> P[UPDATE status=archived\n记录归档人/时间]
        P --> L
    end

    subgraph 自动化扫描
        Q[每日 03:00 cron] --> R[扫描 active 策略]
        R --> S[按 productId 匹配合适报告]
        S --> T[到期日 = startBasis + retentionMonths]
        T --> U{到期日 < 今天?}
        U -->|是| V[INSERT data_archive_record\nreportId 唯一防重复]
        U -->|否| W[跳过]
    end
```

---

## 5. 角色与权限

| 角色 | manage:DataRetention |
|------|:---:|
| wet_lab | ❌ |
| bioinformatician | ❌ |
| interpreter | ❌ |
| operation_admin | ✅ |