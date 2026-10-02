# 03 · 多人协作（保持一致）

> 需求③。多人（含多个 agent 会话）并行开发，不一致只有三个来源，逐个掐掉。
> 前提：读 `ai-rules/01-readability.md`（写码规则）与 `02-review-contract.md`（评审规则）。

## 1. 不一致的三个来源与对策

| 来源 | 对策 | 章节 |
|---|---|---|
| 并行改同一个接口 | **接口先行**：先合并骨架，再并行实现 | §2 |
| 两个人改同一个文件 | **模块 owner** + 热点文件串行 | §3 §4 |
| 同一件事两种写法 | **统一模板** + 机械闸门 + 评审拦"第二种做法" | §5 §6 |

## 2. 接口先行（最重要的一条，能消灭 90% 的后续冲突）

**不要并行实现同一个接口。** 所有跨人/跨 agent 的接口改动，分两步：

1. **第一步只提交骨架**（一个独立 PR，先合进 main）：
   - Java：Controller 方法签名 + `IXxxService` 接口 + `XxxBo` / `XxxVo` 字段 + Mapper 方法声明（空实现或 `throw new UnsupportedOperationException()`）
   - 前端：`src/service/api/xxx.ts` 的类型与函数签名 + `src/typings/` 类型定义
   - 如果有 DB 变更：先提交建表/迁移脚本
2. 骨架合并之后，各人**只填函数体**，不再动签名。

规则：
- **骨架合并后，改签名 = 破坏契约**，必须单独开 PR 并通知所有 owner。
- 骨架 PR 用 `ai-templates/` 里的模板生成，保证结构一致。

## 3. 模块 owner 制

- **一人一目录**，按 `template-backend/ruoyi-modules/` 与前端 `src/views/` 的模块边界划分。
- 目录内文件随便改；**跨目录改动要走 PR + 通知对应 owner**。
- 新增文件必须落到既有目录（后端 `ruoyi-modules/*/src/main/java/.../{controller,service,mapper,domain}`，前端 `src/views/<模块>/`），**不许新开并列根目录**。

## 4. 热点文件：串行改

以下几类**天然是冲突热点**，约定为「单点负责人串行修改」：

| 热点 | 说明 |
|---|---|
| 路由/菜单/权限注册 | `sys_menu`、权限点位表、前端路由 |
| 全局类型与常量 | `src/typings/`、`src/constants/`、Java 的 `domain` 公共类 |
| DB 结构 | 建表/迁移脚本（`script/sql/`） |
| 公共组件 / hooks | `src/hooks/`、`ruoyi-common/` |
| 规则本身 | `ai-rules/`、`AGENTS.md`、`pom.xml`、`package.json` |

要改 → 提 issue 或口头同步，由 owner 改。**不要两个人同时动同一个热点文件。**

## 5. 一致性靠模板，不靠记性

`ai-templates/` 是可直接复制的新模块骨架：

```
ai-templates/backend-module/    Controller / I*Service / ServiceImpl / Mapper / Bo / Vo / Mapper.xml
ai-templates/frontend-module/   index.vue / api.ts / types.ts
```

**新模块一律从模板复制，改名后只填业务逻辑。** 模板本身遵守 `01-readability.md`，
所以复制出来的代码天然合规、且风格统一 —— 这是"多人开发的模块整体一致"最省力的实现方式。

## 6. 分支与合并纪律

```bash
git checkout main && git pull
git checkout -b feat/<模块>-<简述>

# 每天开工先 rebase，别攒三天
git fetch origin && git rebase origin/main

# 提 PR 前：机械闸门 + 自测
python3 tools/readability_check.py --changed-only origin/main

git push -u origin feat/<模块>-<简述>
```

- **PR 要小**：目标 < 400 行改动。大 PR 没人真审，也最容易冲突。
- **每天同步 main**，不要攒。
- **合并方式统一 squash**（一个 PR 一个 commit），main 历史线性、好回溯。
- 一个 PR 只做一件事，不混入"顺手重构"。

## 7. 把闸门装进自动化

### 7.1 本地 pre-commit（改动文件即查）

```bash
cat > .git/hooks/pre-commit <<'EOF'
#!/bin/sh
python3 tools/readability_check.py --changed-only HEAD --fail-on error || {
  echo "提交被拦截：请修复上面报告里的 error（紧急绕过：git commit --no-verify）"
  exit 1
}
python3 tools/structure_check.py --changed-only HEAD || {
  echo "提交被拦截：页面结构契约不通过（缺 modules 组件 / 未用统一 hooks / 类型写错位置 等）"
  exit 1
}
EOF
chmod +x .git/hooks/pre-commit
```

两个闸门分工不同，都要跑：`readability_check.py` 管「代码写得好不好读」（行长/嵌套/any/console），
`structure_check.py` 管「页面结构对不对」（文件落位、import 目标存在、统一 hooks、类型位置、跨端权限点位）。

### 7.2 CI（合并阻断条件）

GitLab CI（仓库根 `.gitlab-ci.yml`）：

```yaml
readability-gate:
  stage: test
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
  script:
    - python3 tools/readability_check.py --changed-only origin/main --json readability.json
  artifacts:
    when: always
    paths: [readability.json]
```

GitHub Actions（仓库根 `.github/workflows/readability.yml`）：

```yaml
name: readability-gate
on: [pull_request]
jobs:
  gate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - run: python3 tools/readability_check.py --changed-only origin/${{ github.base_ref }} --json readability.json
      - if: always()
        uses: actions/upload-artifact@v4
        with: { name: readability, path: readability.json }
```

**设成必过检查。** 核心一句：**规则不接 CI，就只是建议。**

## 8. 多 agent 并行的额外约束

- 一个分支同时只有一个写者（见 `02-review-contract.md` 第 1 节）。
- 需要多 agent 同时写 → 用 git worktree 隔离，各自分支，最后走 PR：

```bash
git worktree add -b feat/module-a /tmp/wt-a main
git worktree add -b feat/module-b /tmp/wt-b main
```

- 用 `.ai_state/`（前端 Athena 机制）时，**每个 worktree 一份状态**，不要共享 —— 状态文件本身也会冲突。

## 9. 每人每次交付的固定格式

1. **改了什么** —— 文件清单
2. **验证了什么** —— 实际执行的命令
3. **真实终端输出** —— 原样粘贴（含 `readability_check.py` 的报告），未运行或受阻必须明说
4. **结论** —— 完成 / 未完成 / 遗留问题

四项缺一不可。这是"多人协作时谁改了什么、验没验过"的唯一凭据。
