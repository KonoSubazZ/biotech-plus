---
name: crud-module
description: >
  新增一个 CRUD 模块（后端 Java + 前端 Vue）时使用。触发场景：要加一张表的增删改查、
  要加一个后台管理菜单对应的接口与页面、"帮我加个产品配置/质控记录这类管理功能"、
  已经有设计文档要落地成代码。一条命令完成 建目录→复制模板→改名→注册 Maven 模块→自检，
  并给出正确的字段骨架、权限点位与前后端联调清单。
effort: high
---

# crud-module — 按模板生成 CRUD 模块

## Overview

后台功能绝大多数是「一张表 + 一套增删改查 + 一个管理页」。本 skill 用
`tools/new_crud.py` 把这类模块的脚手架一次生成，替代手工 cp/sed ——
手工最容易漏的两步是**改文件名**（class 名与文件名不一致 → 编译不过）和
**在父 pom 注册**（编译不过 / 接口 404）。

权威来源：`ai-templates/backend-module/` 与 `ai-templates/frontend-module/`（模板）、
`ai-rules/01-readability.md`（代码规范）。本 skill 只负责"怎么用它们"。

## When to Use

- 需求是「一张表的增删改查」或「一个后台管理页面」
- 设计文档里已有该表的字段表（如 `docs/context/设计文档/*.md`）
- 要给已有模块再加一个实体（同模块第二个实体）

**Don't use for:**
- 单表之外的复杂查询/报表 → 仍需本 skill 起骨架，但复杂 SQL 走 Mapper XML
- 只有前端页面、没有后端实体 → 直接复制 `ai-templates/frontend-module/`
- 改已有模块的字段 → 直接改 `domain`/`Bo`/`Vo` 三处，不要重跑脚本
- 表结构还没定 → 先按 `ai-rules/03-collaboration.md` §2 冻结接口，别先写代码

## 步骤

### 1. 先确认三件事（不确认就动手 = 白写）

| 要确认 | 为什么 |
|---|---|
| 表名、字段清单与类型 | 仓库里**没有业务表 DDL**，字段只能来自设计文档或用户口述；不要凭字段名猜类型 |
| 模块名与实体名 | 决定包名 `org.dromara.<模块>`、Maven 模块 `ruoyi-<模块>`、权限点位 `<模块>:<实体驼峰>:*` |
| 权限点位 | 前端 `hasAuth()` 与后端 `@SaCheckPermission()` 必须**逐字一致** |

完成判据：能写出 `--module / --entity / --table / --title / --fields` 五个参数。

### 2. 先 dry-run 看计划

```bash
cd <仓库根>
python3 tools/new_crud.py --root . --module biotech --entity QcRecord --table qc_record \
  --title 质控记录 --fields "subbarcode:string:样本条码:req:80,status:string:人工状态:req:20" \
  --dry-run
```

完成判据：输出的 12 个目标路径都对（后端 9 + 前端 3），权限前缀与包名符合预期。

### 3. 正式生成

去掉 `--dry-run` 即可。脚本会：

1. 复制 9 个后端模板 + 3 个前端模板
2. 替换三种大小写形态（类名 / 驼峰 / 短横线），并**同时改文件名**
3. 注册到 `ruoyi-modules/pom.xml` 的 `<modules>` 与 `ruoyi-admin/pom.xml` 的 `<dependencies>`（幂等）
4. 按 `--fields` 重写 `domain` / `Bo` / `Vo` 的字段块
5. 自检：模板名残留 + 类名是否等于文件名

完成判据：自检两行都是 `✓`。出现 `⚠` 必须处理，不要往下走。

### 4. 字段语义（脚本只懂类型，不懂业务）

脚本按 `列名:类型:中文标签[:req][:最大长度]` 生成字段，类型 `string|text|int|long|decimal|date|datetime|bool`
→ String/String/Integer/Long/BigDecimal/Date/Date/Boolean，`req` 加 `@NotBlank`/`@NotNull`，`:N` 加 `@Size(max=N)`。

脚本**生成的字段肯定不完整**，之后必须人工补：
- 唯一约束字段（如 `code`）除了 `@NotBlank` 还要在 Service 里查重（模板已给 `validateCodeUnique`）
- 外键字段（如 `productId`）要补引用校验，删除时禁止被引用（模板已给 `assertNotReferenced` 占位）
- 枚举字段（如 `status`）的取值范围要写进 javadoc 注释

### 5. 前端与路由

脚本已生成 `views/<实体短横线>/index.vue`、`service/api/<模块>/<实体短横线>.ts`、`typings/api/<模块>.api.d.ts`。

- 同模块第二个实体时，typings 文件已存在 → 脚本**不会覆盖**，会提示你把新实体的
  `namespace <实体> { ... }` 手工合并进去
- 路由用 `pnpm gen-route` 生成，不手写
- 菜单与权限点位加进 `sys_menu`（`<模块>:<实体驼峰>:list/add/edit/remove/query`）

### 6. 验证（三件都要做，缺一不算完成）

```bash
cd template-backend && mvn -B -ntp -pl ruoyi-modules/ruoyi-<模块> -am validate   # 期望 BUILD SUCCESS
cd .. && python3 tools/readability_check.py --changed-only origin/main            # error 必须为 0
git diff origin/main...HEAD > /tmp/change.diff                                    # 交给评审
```

完成判据：mvn 出 `BUILD SUCCESS`（**核实实际执行的测试数量，零测试不算通过**）、
闸门 error 0、diff 已产出。**未运行 = 未完成**，贴真实输出。

## One-Shot Recipes

**落地设计文档里的一张表**（以质控记录为例）

```bash
python3 tools/new_crud.py --root . --module biotech --entity QcRecord --table qc_record \
  --title 质控记录 --author 张三 \
  --fields "subbarcode:string:样本条码:req:80,qc_item:string:质控项目:req:100,\
qc_result:string:质控结果:50,status:string:人工状态:req:20,tested_at:datetime:检测时间,remark:string:备注:500"
```

**只加前端页面**：`cp -r ai-templates/frontend-module/* src/` 后按 README 改名。

**同模块第二个实体**：直接跑第 3 步，脚本会跳过 pom 与 typings，只提示合并 namespace。

## Common Pitfalls

1. **只改内容不改文件名** → `class QcRecord` 留在 `ProductConfigController.java` 里，编译不过，
   Checkstyle 的 `OuterTypeFilename` 也拦。脚本已内置改名，手工做时务必 `mv`。
2. **漏注册父 pom** → 文件都在但 `mvn` 报模块不存在 / 接口 404。
   `ruoyi-modules/pom.xml` 的 `<modules>` + `ruoyi-admin/pom.xml` 的 `<dependencies>` 两处都要。
3. **包名不在 `org.dromara.*` 下** → 启动类只有 `@SpringBootApplication`，组件扫描根就是该包，放别处不会被扫描。
4. **权限点位前后端不一致** → 前端 `hasAuth('a:b:c')` 与后端 `@SaCheckPermission("a:b:c")` 差一个字母就整页按钮消失。
5. **Maven XML 位置靠猜** → 无需配置：`application.yml` 已是 `mapperPackage: org.dromara.**.mapper`，
   XML 放 `src/main/resources/mapper/<模块>/` 即可。
6. **拿设计文档当实现规格** → 设计文档描述的是 NestJS/React/PostgreSQL 栈（见根 `AGENTS.md` 第 1.2 节），
   字段名可参考，**代码结构必须按本仓库的 Java/Vue 模板**。
7. **表结构自己编** → 仓库里没有业务表 DDL。字段清单只能来自设计文档或用户确认，不许猜。
8. **脚本生成后直接提交** → 生成的是骨架（`assertNotReferenced` 是空占位、示例注释未改），
   必须补业务逻辑再提交。

## 生成后必做的 6 项校验（骨架 ≠ 成品，逐条过）

`new_crud.py` 只保证「结构、命名、注册」正确，**字段语义与框架接线仍要人工过一遍**。
下面 6 条都是实测踩过的（模板已按此修好；换模板或手写时仍会撞）：

1. **pom 要注册三处，缺一处就编译不过**
   - `ruoyi-modules/pom.xml` 加 `<module>ruoyi-<模块></module>`
   - `ruoyi-admin/pom.xml` 加 `<dependency>`（**不写 version**，交给 dependencyManagement）
   - **根 `pom.xml` 的 `<dependencyManagement>` 加 `<version>${revision}</version>`**
     ← 漏这处报：`'dependencies.dependency.version' for org.dromara:ruoyi-xxx:jar is missing`
2. **`@AutoMapper` 不能漏**：`XxxBo` 标 `@AutoMapper(target = Xxx.class, reverseConvertGenerate = false)`，
   `XxxVo` 标 `@AutoMapper(target = Xxx.class)`。漏了**编译不报、调接口才炸**：
   `ConvertException: cannot find converter from XxxBo to Xxx`。
3. **前端 `index.vue` 要显式 `import { ref } from 'vue'`**（本项目没有 auto-imports 声明文件，
   不写就 `Cannot find name 'ref'`）。
4. **列表页要 `transform: response => defaultTransform(response)`**：RuoYi 分页响应是顶层 `rows/total`，
   不 transform 会 `UseNaivePaginatedTableOptions<unknown, unknown>` 泛型推断失败 + 一片 `'row' is of type 'unknown'`。
5. **前端 `api.ts` 的 import 用 `@/service/request`**（不是相对路径 `../request`）；
   批量删除参数类型用 `CommonType.IdType[]`（不是 `number[]`）。
6. **新页面要加 i18n 路由标题**：`src/locales/langs/{zh-cn,en-us}.ts` 的 `route` 段加
   `'<实体短横线>': '中文名'`（路由名由文件推导，漏了会报 `Record<I18nRouteKey, string>` 缺 key）。

接线后的验收顺序（缺一不算完成）：
`mvn -pl ruoyi-modules/ruoyi-<模块> -am -DskipTests compile`
→ `bash ~/docker/biotech-plus/deploy.sh build` 重打 jar → 重启后端
→ `node_modules/.bin/vue-tsc --noEmit --skipLibCheck` 0 错误（pnpm typecheck 会被依赖检查卡住，直接跑 vue-tsc）
→ 真机调一次 CRUD（新增 / 列表 / 删除，确认主键自增 + tenant_id 自动补成 000000）。

7. **页面目录层级必须与菜单层级一致** —— 否则路由会被静默丢弃（实测排查了很久）：
   页面放 `src/views/<模块目录>/<实体短横线>/index.vue`，菜单的 `component` 写 `<模块目录>/<实体短横线>/index`。
   **不要**放在一层 `src/views/<实体短横线>/`：
   - 前端会按 component 算出路由名（`project/qc-standard/index` → `project_qc-standard`；一层则是 `qc-standard`）
   - `elegant/transform.ts` 用 `isFirstLevelRoute(name) = !name.includes('_')` 判断层级
   - 名字里**没有下划线**就被当成「一级路由」，再走 `getSingleLevelRouteComponent(component)`
     （要求 `layout.base$view.xxx` 这种带 `$` 的格式），动态菜单给的是 `view.qc-standard` →
     `throw "Layout component not found"` → 被 `catch` 吞掉 → **该路由根本没注册**
   - 症状：菜单能正常显示、一点击就报 `Uncaught Error: No match for {"name":"qc-standard","params":{}}`
   - 一句话：**目录层级 = 菜单层级，路由名才有下划线，才不会被误判**。

## Verification Checklist

- [ ] dry-run 的 12 个路径与权限前缀已确认
- [ ] 生成后自检两行均为 `✓`（无模板名残留、类名=文件名）
- [ ] 三处字段已按真实表结构改完（`domain` / `Bo` / `Vo`），`Bo` 校验注解与业务一致
- [ ] 唯一约束、外键引用、枚举取值范围已补
- [ ] 权限点位前后端逐字一致，且已进 `sys_menu`
- [ ] 三处 pom 已注册（`ruoyi-modules` / `ruoyi-admin` / **根 pom 的 `dependencyManagement`**），`mvn compile` 能过
- [ ] `Bo`/`Vo` 都标了 `@AutoMapper`（漏了编译不报、调接口才炸）
- [ ] 前端 `index.vue` 显式 import `ref` + 用 `defaultTransform`；`api.ts` 用 `@/service/request` 与 `CommonType.IdType[]`
- [ ] 新页面加了 i18n 路由标题（`zh-cn` + `en-us` 的 `route` 段）
- [ ] 后端重打 jar 并重启；`vue-tsc --noEmit` 0 错误
- [ ] 真机调过一次 CRUD（新增/列表/删除，主键自增 + `tenant_id` 自动补成 `000000`）
- [ ] `readability_check.py --changed-only` error = 0，warn 已逐条说明
- [ ] 真实终端输出已贴（mvn 结果 + 闸门报告），未运行或受阻已明确说明
