# 05 · Git 提交规范（提交信息 + 提交前动作）

> 适用：根仓库所有提交。
> 读者：写代码的人、让 AI 改代码的人、Review 的人。
> 机械闸门：`python3 tools/check_commit.py`（校验提交信息）。
> 一条命令装好本地钩子：`bash tools/install-git-hooks.sh`（pre-commit 跑三道闸门 + commit-msg 校验信息）。

---

## 0. 为什么要有这个规范

1. **squash 合并后，提交信息就是唯一的历史记录**（见 `ai-rules/03-collaboration.md` §6）。
   半年后回看「这行为什么这么写」，只有 commit message 能回答。写得潦草 = 把这段历史删了。
2. **能机械校验，就不该只靠自觉** —— 本仓库用 `commit-msg` 钩子强制拦。
3. **提交粒度决定 review 质量**：一个 commit 一件事，reviewer 才看得懂；
   把「修 bug + 顺手重构 + 格式化」混在一起，就只能整体放行。

---

## 1. 格式

```
<type>(<scope>): <中文简述>          ← 首行（必填）

<body：为什么这么改、有什么取舍>      ← 可选，每行 ≤ 72 字

<footer：关联的设计文档 / 闸门结果>   ← 可选
```

- 首行 **≤ 50 字**（含 type/scope 最多 72），不加句号
- `type` / `scope` 用小写英文；`subject` 用**中文**（与仓库注释、文档一致）
- body 讲「**为什么**」，「改了什么」看 `git diff` 就够
- 破坏性改动：`type(scope)!: ...`，并在 body 里写 `BREAKING CHANGE: ...`

---

## 2. type（必须取其一）

| type | 用在 |
|---|---|
| `feat` | 新增功能（表、接口、页面、菜单） |
| `fix` | 修 bug |
| `refactor` | 重构（不改变行为） |
| `perf` | 性能 |
| `style` | 纯格式（本仓库尽量少用，避免污染历史） |
| `docs` | 文档（含 `ai-rules/`、`AGENTS.md`、README） |
| `test` | 测试 |
| `build` | 构建（pom / vite / 依赖版本） |
| `ci` | CI 配置 |
| `chore` | 杂活（脚本、配置、生成物） |
| `revert` | 回滚 |

---

## 3. scope（模块名，与代码归属一致）

| scope | 覆盖范围 |
|---|---|
| `backend` | template-backend 通用（pom、yml、公共模块） |
| `frontend` | template-frontend 通用（vite、locales、elegant 生成物） |
| `db` | 建表 / 迁移脚本 / 表结构 |
| `system` | 系统管理（菜单、权限、租户） |
| `qc` | 质控标准（及将来的 qc_record） |
| `project` | 项目管理（产品配置等） |
| `report` | 报告管理（样本信息等） |
| `compliance` | 合规管理（开发记录 / 3Q 验证记录 / 3Q 文档管理） |
| `tools` | `tools/` 下的闸门与脚手架 |
| `rules` | `ai-rules/`、`AGENTS.md`、`ai-templates/` |
| `deps` | 依赖升级 |

**一个 commit 只用一个 scope。** 跨模块的改动拆开提交（典型三段式：
`db 建表` → `backend 接口` → `frontend 页面 + 菜单`）。

---

## 4. subject 好与坏

```
✗ update          ✗ fix bug        ✗ 修改           ✗ WIP
✗ 提交            ✗ 按照设计文档实现了产品配置模块的增删改查以及相关的前端页面和菜单

✓ feat(project): 新增产品配置模块（表/接口/页面/菜单）
✓ fix(frontend): 修正质控标准路由被 elegant-router 丢弃
✓ docs(rules): 补 05 提交规范并接进 commit-msg 钩子
✓ refactor(qc): 唯一性校验改按 产品+项目+类别 组合
```

---

## 5. 提交前必做（四步，缺一不算完成）

1. **跑闸门**（按改动范围，见 `AGENTS.md` 常用命令）
   - 动过代码 → `python3 tools/readability_check.py --changed-only HEAD --fail-on error`
   - 动过前端页面 → `python3 tools/structure_check.py --changed-only HEAD`
   - 动过表 → `python3 tools/check_db_schema.py`
2. **看 diff**：`git diff --cached` —— 确认没夹带（临时埋点、调试日志、`.env`、密钥、生成物）
3. **分组暂存**：`git add <具体文件>`（**不要 `git add .`**），一个逻辑改动一组
4. **先自检 message 再提交**：`python3 tools/check_commit.py --message "<你的提交信息>"`

---

## 6. 钩子（一条命令装好）

```bash
bash tools/install-git-hooks.sh
```

装两个钩子：

| 钩子 | 做什么 |
|---|---|
| `pre-commit` | 跑 readability / structure / db 三个闸门（只查本次改动的文件），有 error 就拦截 |
| `commit-msg` | 跑 `check_commit.py` 校验本次提交信息格式，不合规就拦截 |

紧急情况可 `git commit --no-verify` 绕过，**但必须在 PR 描述里写明原因**，否则等于没有规范。

---

## 7. 禁止

- ✗ `git add .` / `git add -A`（会把临时文件、生成物、密钥一起提交）
- ✗ 一个 commit 塞多个不相关的改动
- ✗ message 写 `update` / `fix` / `修改` / `提交` 这类无信息量的话
- ✗ 提交调试残留（`console.log`、临时埋点、注释掉的代码、`TODO(临时)`）
- ✗ 提交 `.env`、密钥、`target/`、`node_modules/`、`dist/`
- ✗ 不跑闸门就提交

---

## 8. 与 PR 的关系

- 分支名：`<type>/<scope>-<简述>`，如 `feat/project-product-config`
- 合并方式统一 **squash**（一个 PR 一个 commit）→ 所以 **PR 标题就是最终的 commit message**，
  按本规范写（`ai-rules/03-collaboration.md` §6）
