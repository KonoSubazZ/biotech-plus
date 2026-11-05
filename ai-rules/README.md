# 开发、评审与提交规则

从仓库根目录执行检查。日常开发从 `dev` 建分支，提 PR 前两道检查都以 `origin/master` 为基线。

| 工作 | 规则 | 工具或模板 |
|---|---|---|
| 写 Java、Vue 或 TypeScript | [可读性](01-readability.md) | [可读性检查](../tools/readability_check.py) |
| 交接变更给另一位评审者 | [评审契约](02-review-contract.md) | 提供变更意图、差异与实际验证结果 |
| 多人改同一模块、新增页面或模块 | [接口与协作](03-collaboration.md) | [代码模板](../ai-templates/) · [脚手架](../tools/new_crud.py) · [页面结构检查](../tools/structure_check.py) |
| 建表、加列或调整索引 | [数据库规范](04-db-schema.md) | [DDL 模板](../ai-templates/db/table-template.sql) · [表结构检查](../tools/check_db_schema.py) |
| 分组暂存并提交 | [提交规范](05-git-commit.md) | [钩子安装脚本](../tools/install-git-hooks.sh) · [提交信息校验](../tools/check_commit.py) |

## 提交前检查

以下命令在仓库根目录运行。PowerShell 中使用 `python` 调用 Python 3；使用 Git Bash 或 Linux 时可将解释器换成 `python3`。

```powershell
python tools/readability_check.py --changed-only origin/master
python tools/structure_check.py --changed-only origin/master
```

改表时另运行 `python tools/check_db_schema.py`；该检查需要已配置的数据库。每个 clone 按[提交规范](05-git-commit.md)安装 Git 钩子。交接评审前用 `git diff origin/master...HEAD` 查看已提交的差异，并单独核对待提交文件。

## 规则维护

跨端规则在本目录维护，语言和页面约束分别见[后端代码规范](../template-backend/docs/backend-code-standard.md)、[前端设计](../template-frontend/DESIGN.md)与[交互契约](../template-frontend/UX-CONTRACT.md)。规则可机械检查时一并维护对应工具。前端 Athena 的评审衔接见[评审契约 §7](02-review-contract.md)。

同一分支同一时刻只由一人写入，评审交给另一位执行者。事实和验证结论须来自实际代码及命令输出，只为已有需求引入抽象。
