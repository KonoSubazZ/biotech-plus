---
name: module-dev-test-docs
description: 将 biotech-plus 项目的口述或聊天需求整理为按模块维护的开发测试文档，输出 Markdown 和 Word。用于编写模块说明、开发方案、验收用例或补齐已有模块文档；支持先出样例再迭代，不自动开发业务功能或执行数据库测试。
---

# 模块开发测试文档

把用户口述的业务意图整理成开发者能实现、测试者能执行、需求提出者能核对的模块文档。默认一个模块一份文档，同时提供 `.md` 和 `.docx`；用户只要一种格式时按其要求输出。

## 从口述建立可核对的需求

1. 确认模块、目标角色和本次范围。从当前对话提取目标、业务规则、例外及后续修正；不要求用户先补完整需求书。
2. 区分三类依据：用户明确确认、当前代码实现、建议或待确认。代码已有某行为不等于用户已批准该需求。示例话术要标明是示例，不能写成用户原话。
3. 缺少决定权限、数据归属或删除行为的信息时，简短询问；不依赖答案的部分继续整理。其余空缺写入待确认项，先交付可评审版本，不自行扩展套餐、审批、计费等功能。
4. 按模块定位相关前后端、接口、实体、SQL、测试和现有文档。只读取与本次模块有关的内容。实际路径先查证；业务说明与实现冲突时并列记录，不静默选择一方。

本项目常用入口：前端目录 `template-frontend` 下的 `src/views`、`src/service/api`；后端目录 `template-backend` 下的 `ruoyi-modules`、`ruoyi-admin/src/test`、`script/sql/update`；背景资料在 `docs/context`。这些是定位起点，不是对每个模块结构的保证。读取适用的 `AGENTS.md` 与后端代码规范。

## 写作与追踪

读取 [文档结构与证据要求](references/document-structure.md)，按模块规模选取必要章节。用 `REQ-模块代号-序号` 标识需求、`TC-模块代号-序号` 标识测试用例，更新时保留原编号，新增内容追加编号。

- 开发前文档写“计划实现”；已有模块样例写“实现基线 待业务确认”，不能写成已验收。
- 每项需求说明角色、触发条件、规则、可观察的验收结果及依据。
- 每个用例给出前置条件、数据、操作步骤、预期结果和关联需求，覆盖相关的正常、边界、异常及权限路径。
- 测试计划与执行记录分开。没有本次实际执行证据，状态就是“待执行”；历史测试记录须标明来源和日期，不能填入本次通过栏。
- 当前实现、建议改造和已知差异分开写。SQL 简单查询用 MyBatis-Plus，复杂查询按项目规范放 Mapper XML；代码可读性沿用显式变量和分支的约定。
- 不虚构接口、字段、错误码、执行结果、确认人或上线结论；不复制真实账号凭据或业务敏感数据。

## 双格式交付

默认输出到 `docs/modules/<module-slug>/<模块名>开发测试文档.md` 和同名 `.docx`。已有对应文档时更新原文件；不未经请求另建一串日期版本。多模块分别输出。

Markdown 是唯一内容源，先完成内容，再转换 Word，不分别维护两套文字。Word 使用黑色标题、可读的中文字体、重复表头和自然分页；测试步骤使用段落，避免宽表挤压长文字。

脚本支持标题、段落、加粗、行内代码、链接、列表、代码块和 Markdown 表格；`<!-- pagebreak -->` 可用于审阅版分页。遇到图片、嵌入 HTML 等不支持结构会报错，需调整源文档或采用合适的文档工具，不能丢内容继续生成。

```powershell
python .agents/skills/module-dev-test-docs/scripts/build_docx.py --input 'docs/modules/tenant-management/租户管理开发测试文档.md' --output 'docs/modules/tenant-management/租户管理开发测试文档.docx'
python .agents/skills/module-dev-test-docs/scripts/test_build_docx.py
```

运行时需要 `python-docx` 与 `markdown-it-py`。在 Codex 中使用 workspace dependency loader 返回的 Python；缺少依赖时放入任务临时目录并通过 `PYTHONPATH` 引用，不改业务项目依赖，也不在技能中固化个人安装路径。

若 documents 技能可用，依照其文档生成与视觉检查流程。优先使用它的 `render_docx.py`。Windows 环境缺少受支持的 LibreOffice 时，可以使用 [Word 渲染脚本](scripts/render_word.ps1)，参数中的 Poppler 路径从运行环境查证：

```powershell
powershell -NoProfile -File .agents/skills/module-dev-test-docs/scripts/render_word.ps1 -InputDocx 'docs/modules/tenant-management/租户管理开发测试文档.docx' -OutputDirectory 'target/module-docs-preview' -PopplerPath '<已查证的 pdftoppm.exe 路径>'
```

逐页查看最终渲染图片，检查中文字符、表格、标题孤行和分页。不能渲染时说明限制，不声称排版已通过。预览与测试临时产物放 `target`，交付仅给所需的 MD/DOCX。

交付时简要说明文档范围、待确认事项及实际验证结果，给出文件入口。产出文档不自动授权修改业务代码、运行会写入数据库的测试、提交或推送 Git。
