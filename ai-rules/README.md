# ai-rules — AI 协作规则（本仓库唯一事实源）

> 这套规则解决三件事：
> **① 控制 AI 写出的代码可维护** · **② Hermes 写 / Codex 审 怎么分工** · **③ 多人并行怎么保持一致**
>
> 规则只写在这里。项目里已有两套子规范（后端 `template-backend/docs/backend-code-standard.md`、
> 前端 `template-frontend/DESIGN.md` + `UX-CONTRACT.md`）**继续有效**，本目录是它们的跨栈补充与
> 三者之间的粘合层，不重复、不覆盖。

## 文件

| 文件 | 解决 | 谁执行 |
|---|---|---|
| `01-readability.md` | 需求①：可读性优先、禁语法糖、禁过度设计 | ai / 人 / 评审 |
| `02-review-contract.md` | 需求②：写与审分离，跨工具交接契约 | Hermes + Codex |
| `03-collaboration.md` | 需求③：多人并行的接口先行与一致性 | 团队 |
| `../tools/readability_check.py` | ①的**机械闸门**：一条命令 + 一份报告 | CI / pre-commit / 人 |
| `../ai-templates/` | ③的**代码模板**：新模块照抄 | 人 / ai |

## 三条命令（记住这三条就够用）

```bash
# 1. 我这次改的代码合不合规（提 PR 前必跑）
python3 tools/readability_check.py --changed-only origin/main

# 2. 看有哪些规则
python3 tools/readability_check.py --rules

# 3. 交给人/另一个 agent 评审时，产出 diff
git diff origin/main...HEAD > /tmp/change.diff
```

## 与已有的 VibeCoding Athena 什么关系

`template-frontend/.claude/`、`.codex/` 里已经有一套 Athena v9.6（PACE 路由 + 9 铁律 +
generator/evaluator/reviewer 子代理 + delivery-gate hook）。本目录**不替换它**，而是补上它没覆盖的三块：

| 缺口 | Athena 现状 | 本目录补什么 |
|---|---|---|
| 可读性的**判据** | 只有一句「SRP·OCP·LSP·ISP·DIP·DRY·KISS」（且鼓励抽象，与"不过度设计"相冲） | `01-readability.md` 给出三级判据 + 反例；并把口径改准 |
| 可读性的**机械闸门** | 无（Checkstyle 只查 SQL 注解等 8 条） | `tools/readability_check.py` |
| 跨工具**交接契约** | reviewer 子代理是 Codex 内部角色，没说 Hermes 产出的 diff 怎么交、审据哪份规则、几轮上限 | `02-review-contract.md` |
| **多人**并行 | `.ai_state/` 是单会话状态机，无 owner / 接口先行 / 分支纪律 | `03-collaboration.md` + `ai-templates/` |
| **后端** | Athena 只在 `template-frontend/` 下，后端什么都没有 | 本目录与工具对 Java / Vue 都生效 |

## 五条不可协商的原则

1. **规则只写一处。** 发现争议 → 补进本目录（能机械判定的一并加进闸门），下次自动生效。不许留成口头约定。
2. **写的人不审自己的代码。** 人和 agent 都适用。
3. **未运行 = 未完成。** 报告"完成"必须附真实终端输出，禁止编造。
4. **不编造事实。** 路径、字段、接口、配置必须来自实际读过的文件；本仓库设计文档与代码栈不一致（见根 `AGENTS.md` 第 1.2 节），尤其不能照抄。
5. **不为将来预留抽象。** 没有第二个实现就不要接口/工厂/策略。
