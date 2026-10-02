# 生信质控模块设计文档

> 路由：/qc/bio-qc | 角色：bioinformatician / operation_admin

---

## 1. 模块说明

生信质控与湿实验质控共享同一套代码架构，仅 `qcCategory` 参数不同（bioinfo vs wet_lab）。

- **后端**：同一 `QcService` / `QcController`，参数 `qcCategory='bioinfo'` 过滤
- **前端**：同一 `QcQualityControl` 组件，传入 `qcCategory="bioinfo"` prop
- **数据表**：同一张 `qc_record` 表，通过 `qc_category` 字段区分
- **自动判定**：同一套判定算法，按 `qcCategory='bioinfo'` 过滤质控标准

---

## 2. 差异点

| 维度 | 湿实验质控 | 生信质控 |
|------|-----------|---------|
| qcCategory | wet_lab | bioinfo |
| 默认角色 | wet_lab | bioinformatician |
| 质控标准 | 独立配置 wet_lab 的标准 | 独立配置 bioinfo 的标准 |
| 菜单路由 | /qc/quality-control | /qc/bio-qc |
| 页面标题 | 湿实验质控记录搜索 | 生信质控记录搜索 |

---

## 3. 完整流程参考

完整的表结构、API 定义、自动判定算法、功能流程图请参考 **湿实验质控设计文档**（`spec.md`），差异仅在于 `qc_category = 'bioinfo'`。

```mermaid
flowchart TD
    A[页面：生信质控\n/qc/bio-qc] --> B[POST /api/qc/records/page\n{qcCategory: 'bioinfo', ...}]
    B --> C[同湿实验质控判定链路\n仅标准按 bioinfo 类别过滤]
    C --> D[渲染表格：条码/项目/结果/状态/判定/操作员/时间]
    
    subgraph 数据流
        E[qc_record.qc_category='bioinfo'] --> B
        F[qc_standard.qc_category='bioinfo'] --> C
    end
```