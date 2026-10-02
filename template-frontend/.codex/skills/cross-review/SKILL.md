---
name: cross-review
description: >
  跨工具交叉评审（Hermes 写 / Codex 审，或反之）。触发场景：一个 agent 或一个人写完了
  一个 PR 需要另一个执行体审查、要给评审者准备 diff 与判据、评审意见来回拉扯需要裁决、
  想知道评审该按什么规则说什么格式。
effort: xhigh
---

# cross-review — 跨工具评审入口

## 什么时候触发

- 我（或另一个 agent）写完了一个分支，要交给 Codex / Hermes / 人 做 code review
- 收到了评审意见，要决定改还是反驳
- 评审来回了两次以上还没收敛

## 三条铁律

1. **写的人和审的人必须是不同执行体。** 自己审自己等于没审。
2. **同一时刻，一个分支只允许一个 agent 在写。**
3. **评审者只读**：只输出意见，不改代码。改由作者做。

## 交接什么（缺一项评审就会跑偏）

```bash
git diff origin/main...HEAD > /tmp/change.diff
python3 tools/readability_check.py --changed-only origin/main | tee /tmp/readability-report.txt
```

交给评审者：**diff + 计划/意图 + 三份判据 + 机械报告**
判据 = `ai-rules/01-readability.md`、`template-backend/docs/backend-code-standard.md`、
`template-frontend/DESIGN.md` + `UX-CONTRACT.md`。
**不许评审者引入自己的风格偏好** —— 这是避免两个 agent 互相扯皮的关键。

## 发起评审

```bash
codex exec "按 ai-rules/02-review-contract.md 审查 /tmp/change.diff；\
机械结论见 /tmp/readability-report.txt；判据只用 ai-rules/01-readability.md 与 \
template-backend/docs/backend-code-standard.md；只输出意见，不要改代码。"
```

## 评审输出必须长这样

结论（需修改 / 通过）+ 阻塞问题（每条带 **文件:行号 + 证据 + 最小改法**）+
建议 + 需要作者说明 + 已核查项。**只回 LGTM 视为无效评审。**

## 收敛与裁决

- 同一 PR **最多 2 轮**，第 3 轮人工裁决。
- 双方对同一条规则理解不一致 → 说明**规则有缺口** → 补进 `ai-rules/01-readability.md`
  （能机械判定的一并加进 `tools/readability_check.py`）。不许留成口头约定。
- 完整契约见 `ai-rules/02-review-contract.md`；与已有 Athena reviewer/evaluator/delivery-gate
  的衔接方式也在那一节，**不要另起一套机制**。
