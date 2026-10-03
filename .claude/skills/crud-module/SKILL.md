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

**最后一步：规范提交**（`ai-rules/05-git-commit.md`）——别攒着，一个逻辑改动一个 commit：

```bash
bash tools/install-git-hooks.sh        # 每个 clone 装一次：pre-commit 跑闸门 + commit-msg 校验提交信息
git add <具体文件>                      # 不要 git add .（会带上临时文件/密钥/生成物）
git commit -m "feat(<模块>): 新增 <中文实体名> 模块（表/接口/页面/菜单）"
```

典型拆三段：`feat(db): 建表脚本` → `feat(<模块>): 后端 + 前端页面` → `chore(frontend): 生成物与 i18n`。
提交前自检：`python3 tools/check_commit.py --message "..."`（钩子也会拦）。
**注意**：`ai-templates/`、`src/router/elegant/`（插件生成物）已被闸门排除 —— 它们是骨架/生成物，
不套业务契约；如果哪天闸门又报这两个目录，说明排除规则被改坏了。

7. **页面目录层级必须与菜单层级一致** —— 否则路由会被静默丢弃（实测排查了很久）：
   页面放 `src/views/<模块目录>/<实体短横线>/index.vue`，菜单的 `component` 写 `<模块目录>/<实体短横线>/index`。
   - 前端会按 component 算出路由名（`project/qc-standard/index` → `project_qc-standard`；一层则是 `qc-standard`）
   - `elegant/transform.ts` 用 `isFirstLevelRoute(name) = !name.includes('_')` 判断层级
   - 名字里**没有下划线**就被当成「一级路由」，再走 `getSingleLevelRouteComponent(component)`
     （要求 `layout.base$view.xxx` 这种带 `$` 的格式），动态菜单给的是 `view.qc-standard` →
     `throw "Layout component not found"` → 被 `catch` 吞掉 → **该路由根本没注册**
   - 症状：菜单能正常显示、一点击就报 `Uncaught Error: No match for {"name":"qc-standard","params":{}}`
   - 一句话：**目录层级 = 菜单层级，路由名才有下划线，才不会被误判**。

8. **逻辑删除 + 唯一键：删了再加会撞唯一键**（实测踩到：`Duplicate entry '000000-2-675' for key 'uk_product_gene'`）
   唯一键（如 `uk_product_gene(tenant_id, product_id, gene_id)`）**不含 del_flag**，被软删的行仍占着键位。
   所以「曾经加过又删掉」的记录不能重新 insert，必须把旧行恢复：
   - 查该维度下**含软删**的全部记录 —— **不能**用 MyBatis-Plus 的查询（`@TableLogic` 会自动补
     `del_flag='0'` 导致查不到），必须写手写 SQL（XML 里加一条 `selectAllGeneIdsByProduct` 这种）
   - 分流处理：未删的 → 跳过；软删的 → `UPDATE ... SET del_flag='0'`（恢复）；全新的 → insert
   - 除非业务上「重复添加」本就该报错，否则新增/导入逻辑都要按这三类走
   - `check_db_schema.py` 只查表结构，发现不了这个，只有在真机调「删了再加」时才暴露

9. **其它几个必踩的小坑**（都是本仓库实测）
   - Excel 用的是 **FastExcel**（`cn.idev.excel`），**不是** `com.alibaba.excel`（EasyExcel）；读一列用
     `@ExcelProperty(index = 0)` 的行对象 + `ExcelUtil.importExcel(is, clazz)`，别按表头名匹配
   - `TableDataInfo.build(mapper.selectVoPage(...))` 直接套会报 `reference to build is ambiguous` ——
     `selectVoPage` 的泛型返回让重载解析不出来，要先赋给 `Page<Vo>` 变量再 build
   - 列表页 `NDataTable` **必须带 `remote`**（闸门 `list-no-remote`），分页器用 `:pagination` 交给表格，
     不要另放 `<NPagination>`
   - 组件里写 JSX 时 `<script setup lang="ts">` 要改成 **`lang="tsx"`**，否则 vue-tsc 报一串 `TS1005`
   - 跨库读没有 `tenant_id` 的表（如基因库 `nkb.ncbi_gene`），要加进
     `TenantLineHandlerImpl.SHARED_TABLES`，否则租户拦截器自动加条件 → `Unknown column` 报错

10. **菜单是多级目录（模块 > 目录 > 页面）时，脚本的前端部分不能直接用**
    `new_crud.py` 只会把页面放到 `views/<模块>/<实体短横线>/`（两层）。若菜单树是三层
    （如「项目管理 > 质量管理 > 湿实验质控」），页面要放三层
    `views/<一级>/<目录>/<页面>/index.vue`，菜单 `component` 同步写三层。
    做法：`--no-frontend` 只生成后端，前端手写（照 `views/project/qc-standard/` 抄），
    再 **必须** 重跑 `pnpm gen-route` —— 重新生成 `router/elegant/{imports,routes,transform}.ts`
    与 `typings/elegant-router.d.ts`；不重跑，`views` 映射里没有新 key → 路由被静默丢弃
    （页面上点菜单报 `No match for`）。
    - 中间目录菜单：`menu_type='M'`、`component` 留空即可 —— 后端 `SysMenu.getComponentInfo()`
      会返回 `ParentView`，前端默认支持，不用手写布局。
    - `pnpm gen-route` 可能先报 `ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY`（它内部跑了
      一次 `pnpm install` 依赖检查）—— 生成物通常已经写好，用 `git diff src/router/elegant/` 确认；
      确实没写就 `CI=true pnpm gen-route`。
    - 两个页面共用同一组件时：页面实现放 `views/<一级>/<目录>/modules/xxx-page.vue`（props 传
      分类），两个菜单各自的 `index.vue` 只做一层薄包装传 prop，别复制两份业务代码。

11. **实体带日期字段时，脚本生成的 Java 少一行 import**
    `new_crud.py` 只替换 `// @fields:start .. // @fields:end` 之间的内容，**类顶部 import 不动**。
    模板里没有 `java.util.Date`，所以 `--fields` 一出现 `date`/`datetime`，
    `domain/Xxx.java` 与 `domain/bo/XxxBo.java` 就编译不过（`cannot find symbol: class Date`）。
    生成后必查这两处补 `import java.util.Date;`（`Vo` 模板自带）。

12. **模板 XML 里有一段抄「产品配置」的样板 SQL，必须清掉**
    生成的 `XxxMapper.xml` 带着 `selectPageWithStandardCount` 与 `p.name / p.code / p.test_type`
    这类**别的表**的列。单表 CRUD 用不到 XML —— 只留 `namespace` 的空 mapper，别把样板留给下一个人。

13. **要「导入 + 下载导入模板」时，先照抄 `views/system/user/modules/user-import-modal.vue`**
    仓库里已经有成套先例，别自己造第二套：
    - 前端：`NUpload`（`:action="\`${baseURL}/report/xxx/importData\`"` + `:headers` 带 token/clientid +
      `:default-upload="false"` + `:is-error-state` 判 `code !== 200`），footer 放「下载模板 / 导入」两个按钮；
      模板下载用 `useDownload().download('/<模块>/<实体>/importTemplate', {}, '<文件名>.xlsx')`。
    - 后端：`POST /importTemplate`（**POST，不是 GET**）+ `ExcelUtil.exportExcel(new ArrayList<>(),
      "导入模板", XxxExcelRow.class, response)` —— 空 list 就只写表头。
    - 想看导入结果计数（新增/更新/失败）就别用 `R<String>` + `v-html` 的老写法，学
      `ruoyi-project` 的 `ProductGeneImportResultVo` / `SampleInfoImportResultVo` 返回结构化 VO。
14. **Excel 行对象可以同时当「导入模板表头」**：`@ExcelProperty(value = "中文列名", index = n)`。
    `value` 用于生成模板表头，**读的时候按 `index`**（实测把上传文件表头文案改掉仍能正确导入）。
    `date`/`datetime` 之外，上游本来就是字符串的日期（如 `yyyy-MM-dd`）继续用 `varchar(20)` 存，
    别为了「像日期」改成 `date` —— 脏值会让整批导入失败。
15. **「宽容导入」不能加 `@Transactional`**：单行 `catch` 住的异常仍会把事务标成 rollback-only，
    最后整体提交失败 —— 越是「逐行收集错误」越不能包一个事务。每行走自动提交，
    失败原因逐行收集返回（`errors` 列表）。
16. **软删 + 唯一键的恢复，落地的两条手写 SQL**（第 8 条讲的是坑，这里给可复制的做法）：
    ```xml
    <select id="selectIdByXxxIncludeDeleted" resultType="java.lang.Long">
        SELECT id FROM <表> WHERE <业务键> = #{xxx} ORDER BY id ASC LIMIT 1
    </select>
    <update id="restoreById">
        UPDATE <表> SET del_flag = '0', update_time = NOW() WHERE id = #{id}
    </update>
    ```
    服务里按「活动行 → 命中则 update；否则查含删行 → 有则 restore+update；都没有则 insert」分流。
    手工新增路径也要单独查一次含删行，给出可读提示（否则用户看到的是数据库唯一键冲突）。
    删除保护同理：在被引用方加一条只读 count SQL（跨模块不建依赖，见 `countQcStandardByProductId`）。
17. **日期范围搜索的固定接线**：前端 `NDatePicker type="daterange" value-format="yyyy-MM-dd"`
    → 写进 `model.params.beginTime / params.endTime`（axios 的 `paramsSerializer` 是 `qs.stringify`，
    后端能绑成 `Map<String,Object> params`）；后端在 `buildQueryWrapper` 里
    `if (params.get("beginTime") != null && params.get("endTime") != null) wrapper.between(...)`。
18. **列表页要挂额外按钮（导入/自定义）**：`TableHeaderOperation` 有 `#prefix` 插槽，
    放在 `NCard` 的 `#header-extra` 里包住它即可，不用改这个公共组件。
19. **新业务域要记得加 commit scope**：`ai-rules/05-git-commit.md` 的 scope 表与
    `tools/check_commit.py` 的 `SCOPES` 白名单是两处，必须同时加（否则提交被钩子拦下，
    只能硬套 backend/frontend，scope 与代码归属就不一致了）。已有：
    backend/frontend/db/system/qc/project/report/tools/rules/deps。

## 真机联调与 UI 验收（本地栈，2026-10 实测）

- **登录接口开了接口加密**（`application.yml` 的 `api-decrypt.enabled: true`）：
  `POST /auth/login` 必须带 `encrypt-key` 头（= RSA 加密 `base64(AES密钥)`，body 用该 AES-ECB
  加密 JSON），否则直接 403「没有访问权限」。公私钥看 `application.yml` 的 `api-decrypt` 段——
  **以实际值为准**，注释里「请求解密私钥」标错位过：`privateKey` 才是请求解密私钥。
- **验证码**：`GET /auth/code` 拿 `uuid`，答案可从 redis 只读取
  `global:captcha_codes:<uuid>`（`docker exec biotech-plus-redis redis-cli -a ruoyi123 GET ...`，
  值是带引号的 JSON 字符串，记得 strip 引号）。
- **自己写的业务接口没挂 `@ApiEncrypt`**：拿到 token 后直接明文 JSON 调，不用加密。
- **UI 验收要看到真渲染**（别只验接口）：用系统自带 chrome
  `~/.agent-browser/browsers/chrome-*/chrome` + python playwright 打开 `http://localhost:9527`，
  真登录（走上面的验证码）后 goto 目标路由，断言面包屑/卡片标题/列头/侧边菜单都在，并截图。
  判断路由通不通就看控制台有没有 `Error transforming route "xxx": View component "xxx" not found`。

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
