---
name: readability-guard
description: >
  写代码前与提交前必用。把「可读性优先、不过度语法糖、不过度设计」变成可执行的检查。
  触发场景：新增/修改 Java 或 Vue/TS 代码、准备提交或提 PR、怀疑代码写得过于"聪明"、
  要判断某个抽象是不是过度设计。
effort: high
---

# readability-guard — 可读性守门

## 什么时候触发

- 正在写或改 `template-backend/**/*.java`、`template-frontend/src/**` 里任何文件
- 准备 `git commit` / 提 PR
- 犹豫"这里要不要抽个接口 / 加个设计模式"

## 步骤

1. **读规则**：`ai-rules/01-readability.md`（唯一判据来源，不要凭自己的风格偏好）
2. **写完/改完立刻跑闸门**：

```bash
python3 tools/readability_check.py --changed-only origin/main
```

3. **error 必须清零**才能提交；warn 要在 PR 描述里逐条说明理由。
4. 判定不了的主观项（命名、是否"同一件事的第二种做法"），留给评审，列进 PR 的"评审者注意"。

## 三条最容易被违反的

1. **抽象只在"同一段逻辑重复 3 次以上"时引入。** 没有第二个实现就不要 interface / 工厂 / 策略。
   `OCP` / `DIP` / `ISP` **不作为本仓库目标** —— 口径见 `ai-rules/01-readability.md` §0。
2. **不为了少写几行而炫技**：禁嵌套三元、禁一行串 5 个以上方法调用、禁 Wrapper 链式堆条件。
   查询一律「声明 Wrapper → 基础条件 → 条件分支 → 排序 → 执行」。
3. **错误不许静默**：禁空 catch、禁无理由 `return null`（缺数据抛 `ServiceException`）。

## 尺寸红线

文件 500 行 · 方法 50 行 · 嵌套 3 层 · 参数 6 个 · 单行 120 字符 · 显式 `any` 0 个

## 豁免

确有必要的长行 / 格式常量 / i18n 文案，在该行加注释 `readability-check: allow` 并写明原因。

## 注意

- Java 侧的方法长度与嵌套是**花括号配对估算**（仓库不引第三方解析器），偶有误报，按报告的文件:行号人工确认。
- 闸门默认只查 `--changed-only`：模板自带代码的老债不用一次性清，**新改动清零**即可。
- 规则有缺口 → 补进 `ai-rules/01-readability.md`，能机械判定的同时加进 `tools/readability_check.py`。
