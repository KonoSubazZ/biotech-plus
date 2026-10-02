# 02 · 评审契约（Hermes 写 / Codex 审）

> 需求②。目标是让「一个 agent 写、另一个 agent 审」**不靠口味，靠规则**——否则两个 agent 会互相不认，
> 来回扯皮，最后还是要人来裁决。

## 1. 三条角色铁律

1. **写的人和审的人必须是不同执行体。** 同一个 agent 审自己的代码基本审不出东西（它认为自己就是这么想的）。
2. **同一时刻，一个分支只允许一个 agent 在写。** 两个 agent 同时改同一分支必然冲突/互相覆盖。
3. **评审者只读。** 只输出意见，**不改代码**。修改由作者完成——责任链清晰，也避免评审者顺手引入新问题。

方向随意：Hermes 写 + Codex 审，或 Codex 写 + Hermes 审，规则一样。

## 2. 评审判据只有一个来源

评审者**只准**引用以下三处，不许引入自己的风格偏好：

| 判据 | 文件 |
|---|---|
| 可读性（语法糖 / 过度设计 / 尺寸 / 错误处理） | `ai-rules/01-readability.md` |
| 后端工程规范 | `template-backend/docs/backend-code-standard.md`（+ `backend/AGENTS.md`） |
| 前端设计与交互契约 | `template-frontend/DESIGN.md`、`UX-CONTRACT.md`（+ `.claude/CLAUDE.md`） |
| 机械结论 | `tools/readability_check.py` 的**真实输出** |

> 这条是整套机制的关键。评审者一旦"按自己的口味"提意见，就会和作者互相拉扯。
> 出现争议 → 说明规则有缺口 → **补进 `01-readability.md`（能机械判定的一并加进闸门）**，下次自动生效。
> 不许留成口头约定。

## 3. 交接：给评审者什么

评审者需要的是**意图 + 差异**，不是整个仓库。缺了意图，它会瞎提意见；给了整仓，它会跑偏。

```bash
# ① 作者侧：产出三样东西
git diff origin/main...HEAD > /tmp/change.diff                      # 改动全貌
python3 tools/readability_check.py --changed-only origin/main \
        > /tmp/readability-report.txt                               # 机械结论（真实输出）
# ② 计划/意图：写在 PR 描述里（见 ai-templates/ 的 PR 模板），或 .ai_state/details/design.md 对应段
```

然后交给评审者：**diff + 计划 + 三份规则 + 机械报告**。

## 4. 评审顺序（逐条过完再往下）

1. **正确性**：需求真的实现了吗？有没有漏掉的调用点？（要求评审者搜一遍被改函数/字段的全部引用）
2. **契约**：对照 `01-readability.md` 逐条核对，error 项一律视为必须修复，不需要讨论
3. **边界与错误**：空输入、非法输入、并发、失败重试、事务回滚、跨租户数据归属
4. **一致性**：和仓库里同类既有实现写法是否一致？是不是"同一件事的第二种做法"？
5. **测试证据**：有没有真实运行输出？边界用例覆盖了吗？是不是编造的？
6. **安全**：硬编码凭据、注入、越权、日志泄露敏感信息、`${}` 拼接 SQL

## 5. 输出格式（强制，便于机器汇总）

```
## 结论
<BLOCK 需修改 | 通过>

## 阻塞问题（必须改）
1. [规则/类别] app/.../XxxServiceImpl.java:120 —— 一句话问题
   证据：<引用 diff 片段或代码>
   建议：<最小改法>

## 建议（不阻塞）
1. ...

## 需要作者说明
1. Xxx.java:45 用了 @Select —— 契约【禁】，请说明为什么不可避免

## 已核查项
- [x] 正确性：已检查 3 处调用点
- [x] 契约：机械报告 error 0 项（已核对原文）
- [x] 测试证据：输出真实，边界用例已覆盖
```

- 每条问题必须带 **文件:行号 + 证据 + 最小改法**。只回 `LGTM` 视为**无效评审**。
- 没有阻塞问题时明确写「通过」，不要含糊措辞。

## 6. 轮次与裁决

- 同一 PR **最多 2 轮**评审。第 3 轮仍有分歧 → **人工裁决**，不许两个 agent 无限来回。
- 评审者与作者对同一条规则的解读不一致 → 回到第 2 节的"补规则"流程，当场把口径写进文件。

## 7. 与已有 Athena 机制的衔接（别另起一套）

`template-frontend/` 下已有 Athena v9.6 的评审链路，本契约与它**是同一件事**，按下面接上即可：

| Athena 已有 | 本契约怎么用 |
|---|---|
| `reviewer` 子代理（read-only，输出 `file:line` findings） | 直接用它；把第 2 节的规则文件喂进它的 context，替换它原来的判据（原来只说"违反 design.md/SOLID/KISS"） |
| `evaluator` 子代理（给 VERDICT） | 保留。它给结论，reviewer 给证据，两者不要混 |
| `.ai_state/details/reviews/sprint-N.md` | 第 5 节的输出格式写进这个文件的 "## Step 2/3" 段 |
| `delivery-gate` hook（Stop 时阻断） | 保留。它检查"有没有外部审查记录/测试记录/VERDICT"，正好兜住本契约的第 4、5 节 |
| `.claude/skills/athena-review` | 已有，指向 pace 的 review 阶段；本契约是它的**判据补充** |

后端没有 Athena（没有 `.ai_state/`）。后端走轻量版：**PR 描述里贴第 5 节格式的评审结论 + 机械报告**，
用 PR 而不是状态文件承载。评审结论落在 PR 里同样可追溯。

## 8. 可选：双向独立评审（更稳）

同一份 diff 平行跑两次独立评审，比对结论：

```bash
codex exec "按 ai-rules/02-review-contract.md 审查 /tmp/change.diff。判据只用 \
ai-rules/01-readability.md 与 template-backend/docs/backend-code-standard.md，\
不许引入你自己的风格偏好。只输出意见，不要改代码。" > /tmp/review-A.txt 2>&1 &

codex exec "同上，独立审一遍" > /tmp/review-B.txt 2>&1 &
```

两次都漏掉的问题，通常就是规则缺的地方 —— **反过来补规则，而不是每次都靠人盯**。

## 9. 一条命令的评审入口

```bash
# 在仓库根目录执行
git diff origin/main...HEAD > /tmp/change.diff
python3 tools/readability_check.py --changed-only origin/main | tee /tmp/readability-report.txt
codex exec "按 ai-rules/02-review-contract.md 审查 /tmp/change.diff；\
机械结论见 /tmp/readability-report.txt；判据只用 ai-rules/01-readability.md 与 \
template-backend/docs/backend-code-standard.md；只输出意见，不要改代码。"
```
