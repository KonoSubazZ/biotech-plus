# 系统管理模块设计文档

> 路由：/system/role（角色管理） /system/user（用户管理） /system/menu（菜单管理）
> 角色：operation_admin

---

## 1. 模块概述

系统管理包含三个子页面，共用 `manage:Permission` 权限点位。

| 子页面 | 路由 | 后端支撑 | 说明 |
|--------|------|----------|------|
| 角色管理 | /system/role | 真实 API（`api/role_manager` + `api/permissions`） | 运行态角色 CRUD + 成员管理 + 权限点位绑定 |
| 用户管理 | /system/user | 本地模拟数据（`initialUsers`） | 纯前端 demo |
| 菜单管理 | /system/menu | 本地模拟数据（`initialMenus`） | 纯前端 demo |

---

## 2. 数据模型（权限相关表）

### authz_permissions（权限点位表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | uuid | PK |
| action | varchar(50) | 动作（read / manage / apply / approve） |
| subject | varchar(50) | 主体（Report / Company / QcControl / QcStandard / AuditLog 等） |
| description | varchar(200) | 描述 |

现有 20 个权限点位，覆盖报告、公司、权限、质控、审计、合规文档、产品配置、数据周期等全部功能模块。

### authz_role_permissions（角色-权限映射表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | uuid | PK |
| role_key | varchar(50) | 角色标识（wet_lab / bioinformatician / interpreter / operation_admin） |
| permission_id | uuid | FK→authz_permissions.id |

---

## 3. API 接口

### 3.1 权限点位管理（/api/permissions）

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /api/permissions | manage:Permission | 列出所有权限点位 |
| GET | /api/permissions/role-mappings | manage:Permission | 查询角色的权限映射 |
| POST | /api/permissions/role-mappings/batch | manage:Permission | 批量更新角色权限（事务内原子增减） |

### 3.2 角色管理（/api/role_manager）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/role_manager/list | 角色列表 |
| GET | /api/role_manager/get?roleKey=xxx | 角色详情 |
| POST | /api/role_manager/create | 创建角色 |
| POST | /api/role_manager/update | 更新角色 |
| POST | /api/role_manager/delete | 删除角色（受保护角色不可删） |
| POST | /api/role_manager/addMembers | 添加角色成员 |
| POST | /api/role_manager/removeMembers | 移除角色成员 |
| POST | /api/role_manager/listMembers | 角色成员列表 |
| POST | /api/role_manager/search | 人员搜索 |

---

## 4. 功能流程

### 4.1 角色管理

```mermaid
flowchart TD
    A[页面：角色管理\n/system/role] --> B[GET /api/permissions\n加载全部权限点位]
    A --> C[GET /api/role_manager/list\n加载角色列表]
    B --> D
    C --> D[渲染表格：角色名称/标识/成员摘要/权限摘要]
    
    D --> E[「添加角色」]
    E --> F[输入名称/标识/描述]
    F --> G[POST /api/role_manager/create]
    G --> D
    
    D --> H[行内「编辑成员」]
    H --> I[EditMembersDialog\nUserSelect/DepartmentSelect/ChatSelect]
    I --> J[POST /api/role_manager/addMembers/removeMembers]
    J --> D
    
    D --> K[行内「配置权限」]
    K --> L[ConfigPermissionsDialog\nAcorddion 按 subject 分组 + Checkbox]
    L --> M[POST /api/permissions/role-mappings/batch\n{roleKey, add[], remove[]}]
    M --> D
    
    D --> N[行内「删除角色」]
    N --> O{allEmployees/public 角色?}
    O -->|是| P[禁止删除]
    O -->|否| Q[POST /api/role_manager/delete]
    Q --> D
```

### 4.2 用户管理 / 菜单管理

```mermaid
flowchart TD
    subgraph 用户管理（本地 demo）
        A[/system/user] --> B[读取 initialUsers 数据]
        B --> C[表格：姓名/角色/手机/邮箱/状态]
        C --> D[搜索筛选]
        C --> E[新增/编辑/删除/批量删除]
    end
    
    subgraph 菜单管理（本地 demo）
        F[/system/menu] --> G[读取 initialMenus 数据]
        G --> H[树形 Table 展示菜单层级]
        H --> I[新增/编辑/删除（级联删除子菜单）]
    end
    
    classDef unimplemented stroke-dasharray: 5 5
```

---

## 5. 角色与权限

| 角色 | manage:Permission |
|------|:---:|
| wet_lab | ❌ |
| bioinformatician | ❌ |
| interpreter | ❌ |
| operation_admin | ✅ |

---

## 6. 已知限制

- **用户管理**和**菜单管理**页面当前为纯前端模拟数据，无后端 API 对接，修改不会被持久化
- 平台内置角色（如 `allEmployees`、`public`）受保护不可删除
- 用户管理页面中的角色分配不会影响真实的 RBAC 角色映射，角色成员管理需通过角色管理页面操作