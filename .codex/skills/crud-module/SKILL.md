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

20. **从生产库 dump 整表搬字段（100+ 列那种）**：别手抄，写脚本从 `CREATE TABLE` 生成
    DDL 列 / 实体字段 / 前端列清单。三个实测坑：
    - **拆词要过一遍**：源表名是 `BIRTHDAY`、`firsttreatmentdrugregimen` 这种密排写法，转小写下划线
      靠词表匹配；词表里要有 `day / stages / spec / mailing / recorder / contact / attachments /
      gene / result / contracts` 这些词，并且**把 `a / p / s / er` 这类单字母词删掉**，
      否则会拆出 `birth_d_a_y`、`sample_cont_a_ct_desc`、`record_er` 这种垃圾列名（实测踩到）。
      拆完打印「源列名 → 新列名」对照表人工过一遍，再生成代码。
    - **源表没有 COMMENT 时不要编中文名**：列注释写 `COMMENT '源表列 XXX'`（可逐列对照），
      UI 标签先用源列名 + 只补用户明确点过名的几个。编错业务含义比留英文更难收拾。
    - 类型映射：`longtext→text`、`varchar(n)` 原样（04-db-schema §7）；补 `id` 自增、公共字段、
      `del_flag`、`tenant_id`、`uk_<表>_<业务键>(tenant_id, 业务键)`、`idx_tenant_id`。
21. **MyBatis-Plus 的驼峰转下划线：数字前不插下划线**。`cancerType1` → `cancer_type1`；
    如果按规范建成 `cancer_type_1`，一切查询都报 `Unknown column 'cancer_type1' in 'field list'`。
    给实体字段加 `@TableField("cancer_type_1")` 显式指定列名即可。
    **搬完表一定要跑一遍比对**：实体字段名 → MP 推导列名 vs 库里真实列名，逐个核对（实测就漏过这一列，
    列表和导入一起炸）。
22. **列数一多，文件长度红线会顶穿**（122 列实测）：
    - Java：每个字段 3 行 × 122 + 头 ≈ 400 行还行；**Bo 别再写 javadoc**（用 @Size 的 message 带中文名），
      否则必超 500；`ExcelRow` 只给有中文标签的字段写 javadoc。
    - 前端默认值：122 行的对象字面量写在抽屉里 → `createDefaultModel()` 超 50 行被判 `web-func-too-long`。
      抽成 `modules/<实体>-form-model.ts` 里的**常量**（数据与行为分文件），抽屉里
      `jsonClone(EMPTY_XXX_FORM)` —— 必须 clone，否则多个抽屉共用同一对象会被改脏。
    - 表单按语义拆成 4 组子组件（患者与临床 / 样本与收样 / 录单与商务 / 治疗史与家族史），
      每个 < 200 行，父抽屉用 `v-model:model` 传同一个 model（子组件里 `defineModel<T>('model')`，
      直接改属性即可，NFormItem 在子组件里也能吃到父 NForm 的校验上下文）。
    - 列表 columns 把全部列都放进去（靠右上角「列设置」关掉），列顺序保持源表顺序。
23. **导入模板的表头直接用源表列名**（`@ExcelProperty(value = "BARCODE", index = 1)`）：
    这样从源系统导出的表格可以原样导入，不用先改名；读的时候仍按 `index`，表头被改也能读对。
24. **搜索栏按钮要贴行最右**（一行放不下就另起一行仍贴右）—— 完整做法与两个实测坑见「搜索栏布局契约」章节，别用 NGrid 最后一格放按钮。
25. **[特坑] 路由页面的 `<template>` 必须是单根元素 —— 多根(fragment)会让整个布局卡死**（2026-10 实测，用户报「点菜单没数据、刷新才有、再点还是没有」）：
    - 症状：点某个菜单进来**内容区整块空白**（连卡片都没有），**之后所有菜单都空白**，F5 刷新后又能显示一次；
      console 里**没有任何报错**（`<Transition>` 对非单元素根只在 dev 打 warning，很难注意）。
    - 原因：`<template>` 里「注释 + 组件」/两个同级元素 → 编译成 fragment；全局布局是
      `<Transition mode="out-in"><KeepAlive><component :is="Component"/></KeepAlive></Transition>`，
      out-in 过渡要求单一元素根，fragment 会让过渡状态机卡在「已 leave、未 enter」，此后每次导航都不渲染。
    - 复现特征：两个共用同一页面组件的 wrapper，**第一个进去正常，切到第二个就空白**；
      单根页面（如 views/report/sample-info/index.vue）来回切都正常 —— 对比就能定位。
    - 修法：说明文字写进 `<script>` 注释，模板只留一个根元素。判据：
      生成的编译产物里不该出现 `STABLE_FRAGMENT / DEV_ROOT_FRAGMENT`（`curl 127.0.0.1:9527/src/views/xxx/index.vue` 能看到）。
    - 验收脚本：连续点两个菜单来回切，看内容区元素数 `document.querySelectorAll('.flex-grow.bg-layout').length` 是否始终 ≥ 1
      （只看接口有没有数据抓不到这个 bug —— 请求根本没发出去）。
26. **菜单/配置迁移脚本要「按当前位置再收敛一次」**：只用「旧位置」定位的 UPDATE 只能生效一次
    （旧行搬走后旧位置就查不到了），第二次执行不会把 name/path 等字段刷回权威值 —— 脚本看着幂等，其实不收敛
    （实测：菜单名被外部改掉后，重跑脚本也改不回来）。做法：INSERT/搬迁之后，再按**当前位置**
    （`parent_id + path`）UPDATE 一次权威字段。
27. **状态标签配色**：绿=正常 / 黄=警告 / 红=失败或错误，非状态类标签不许借用这三色 —— 完整表格与四条硬要求见「状态标签配色契约」章节。


## 状态标签配色契约（2026-10 用户定稿）

**规则：绿色 = 正常，黄色 = 警告，红色 = 失败 / 错误。非状态类标签不许借用这三个颜色。**

| 语义 | `NTag` type | 颜色 | 本项目取值举例 |
|---|---|---|---|
| 正常 / 通过 / 启用 / 成功 / 有效 | `success` | 绿 | `passed`、`active`、`normal`、`enabled` |
| 警告 / 待确认 / 待处理 / 进行中 | `warning` | 黄 | `pending`、`warning`、`running` |
| 失败 / 错误 / 未通过 / 停用 / 异常 | `error` | 红 | `failed`、`error`、`inactive`、`disabled` |
| 未知值 / 空值（兜底专用） | `default` | 灰 | 其它任何值 |

写法：状态 → 颜色用一张表 + 兜底，别把颜色散在各个三元表达式里：

```tsx
/** 配色契约：绿=正常 黄=警告 红=失败/错误；灰只留给「未知值兜底」 */
const STATUS_META: Record<string, { label: string; type: 'success' | 'warning' | 'error' }> = {
  active: { label: '启用', type: 'success' },
  inactive: { label: '停用', type: 'error' }
};

render: row => {
  // 未知值兜底成灰色并原样显示，别静默按正常渲染（否则脏数据看不出来）
  const meta = STATUS_META[row.status] ?? { label: row.status, type: 'default' as const };
  return <NTag type={meta.type}>{meta.label}</NTag>;
}
```

四条硬要求（都是实测踩过的）：

1. **停用 / 禁用用红 `error`，不要用灰 `default`** —— 灰是「未知值兜底」的专用色，用灰会让「停用」和脏数据长得一样。
2. **未知值兜底必须 `default` + 原样显示原值**，不要 `?? 正常`、不要空字符串。
3. **类别 / 数量 / 说明类标签不要用 `success|warning|error`**：用 `info` 或 `default`。
   实测踩到：质控类别「湿实验 / 生信」原本一个 `success` 一个 `info`，绿色的「湿实验」看着像「通过」；
   同一维度内要保持同一种中性色。
4. **字典驱动的状态优先用 `DictTag`**（颜色由 `sys_dict_data.list_class` 决定），同一状态在不同页面
   不许出现两种颜色；改字典时检查 `list_class` 是否落在这套配色里（`success/warning/error/default`）。

## 搜索栏布局契约（search 组件，2026-10 用户定稿）

**规则：查询 / 重置等操作按钮放在「一行的最右边」；一行放不下时就另起一行，仍然贴最右边。**

照抄这段（两种情形都实测过：宽屏按钮与搜索项同一行靠右；搜索项占满整行时按钮另起一行靠右）：

```vue
<NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
  <!-- 搜索项用 flex-wrap + 每项固定宽度；按钮组 ml-auto 自动贴行尾，换行后仍贴右 -->
  <div class="flex flex-wrap items-start">
    <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本编号" path="barcode">
      <NInput v-model:value="model.barcode" placeholder="请输入样本编号" clearable />
    </NFormItem>
    <!-- …其余搜索项同样一行一个… -->
    <NFormItem class="ml-auto" :show-feedback="false">
      <NSpace :size="16">
        <NButton @click="reset">重置</NButton>
        <NButton type="primary" ghost @click="search">搜索</NButton>
      </NSpace>
    </NFormItem>
  </div>
</NForm>
```

要点 / 都是实测踩过的：

- **不要用 `NGrid` + 最后一个 `NFormItemGi` 放按钮**：那一格的 `justify="end"` 只在**自己的格子内**靠右，
  字段数不是列数整数倍时按钮会悬在行中间（实测：6 个搜索项按 `span="24 s:12 m:8"` 排，按钮落在第三行中段）。
- **搜索项宽度用百分比时配 `pr-24px`，不要用 `gap-x-24px`**：宽度是容器的百分比，再加 gap 就超过 100%，
  每行会少放一个（实测 1680px 下 `w-1/3` + `gap-x-24px` 变成每行 2 个；改成 `pr-24px` 后正常 3 个）。
- 按钮组也包在 `NFormItem` 里（`ml-auto` + `:show-feedback="false"`），跟搜索项同高、同一套间距；
  用裸 `div` 会和输入框错位。
- **验收（用户视角，两条都要过）**：量「搜索按钮右边缘 → 卡片右边缘」的距离，应等于卡片内边距（本项目 16px）；
  再把窗口缩到搜索项换行（如 1040px）重复量一次。只看接口/只看 DOM 不算过。


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
