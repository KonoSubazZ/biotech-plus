# 系统管理 业务流程

> 生成时间：2026-09-17
> 分析范围：system 模块（角色管理 + 权限配置 + 角色成员管理）

## 流程概述

系统管理模块提供运行态 RBAC 角色-权限管理。operation_admin 可通过角色管理页创建/编辑/删除角色、为角色绑定权限点位、管理角色成员（添加/移除人员/部门/群组）。

## 流程图

```mermaid
flowchart TD
    A[页面：角色管理\n/system/role] --> B[并行加载]
    B --> C[GET /api/permissions\n全部权限点位]
    B --> D[GET /api/role_manager/list\n角色列表]
    C --> E
    D --> E[渲染角色表格]

    subgraph 角色 CRUD
        E --> F[「添加角色」]
        F --> G[输入角色名称/标识/描述]
        G --> H[POST /api/role_manager/create]
        H --> D
        
        E --> I[行内「角色详情」]
        I --> J[GET /api/role_manager/get?roleKey=xxx]
        J --> K[展示角色基本信息]
        
        E --> L[行内「删除角色」]
        L --> M{allEmployees/public?}
        M -->|是| N[禁止删除（平台角色）]
        M -->|否| O[POST /api/role_manager/delete]
        O --> D
    end

    subgraph 权限配置
        E --> P[行内「配置权限」]
        P --> Q[ConfigPermissionsDialog\nAccordion 按 subject 分组]
        Q --> R[Checkbox 勾选/取消权限点位]
        R --> S[POST /api/permissions/role-mappings/batch\n{roleKey, add[], remove[]}]
        S --> T[事务内原子增减角色-权限映射]
        T --> E
    end

    subgraph 成员管理
        E --> U[行内「编辑成员」]
        U --> V[EditMembersDialog\nTab: 人员/部门/群组]
        V --> W[UserSelect 选具体人员]
        V --> X[DepartmentSelect 选部门]
        V --> Y[ChatSelect 选群组]
        W --> Z[POST /api/role_manager/addMembers\nPOST /api/role_manager/removeMembers]
        X --> Z
        Y --> Z
        Z --> E
    end

    classDef unimplemented stroke-dasharray: 5 5
```

## 关键接口（角色管理）

| 步骤 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 角色列表 | GET | /api/role_manager/list | 所有角色 |
| 角色详情 | GET | /api/role_manager/get?roleKey=xxx | 单个角色 |
| 创建角色 | POST | /api/role_manager/create | 创建新角色 |
| 删除角色 | POST | /api/role_manager/delete | 删除（受保护角色不可删） |
| 添加成员 | POST | /api/role_manager/addMembers | 添加人员/部门/群组 |
| 移除成员 | POST | /api/role_manager/removeMembers | 移除指定成员 |
| 成员列表 | POST | /api/role_manager/listMembers | 角色现有成员 |
| 人员搜索 | POST | /api/role_manager/search | 搜索可添加的用户 |
| 权限列表 | GET | /api/permissions | 全部权限点位 |
| 权限映射 | GET | /api/permissions/role-mappings | 角色-权限映射 |
| 批量更新 | POST | /api/permissions/role-mappings/batch | 原子增减权限 |