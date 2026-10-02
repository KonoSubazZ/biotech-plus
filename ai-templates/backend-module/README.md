# 后端模块模板（照抄即可）

> 用途：需求③「code-template」。**新模块一律从这里复制**，改名后只填业务逻辑。
> 模板本身遵守 `ai-rules/01-readability.md`，复制出来的代码天然合规、风格统一。
> 依据：`template-backend/docs/backend-code-standard.md` + 现有 `ruoyi-modules/ruoyi-system` 的真实写法。

## 推荐：一条命令生成（`tools/new_crud.py`）

**新模块一律走脚本**，别手工 cp/sed。手工最容易漏「改文件名」和「注册父 pom」这两步。

```bash
cd <仓库根>
# 先看计划
python3 tools/new_crud.py --root . --module biotech --entity QcRecord --table qc_record \
  --title 质控记录 --dry-run
# 正式生成（去掉 --dry-run）
python3 tools/new_crud.py --root . --module biotech --entity QcRecord --table qc_record \
  --title 质控记录 --author 张三 \
  --fields "subbarcode:string:样本条码:req:80,qc_item:string:质控项目:req:100,\
qc_result:string:质控结果:50,status:string:人工状态:req:20,tested_at:datetime:检测时间,remark:string:备注:500"
```

字段语法：`列名:类型:中文标签[:req][:最大长度]`，类型 `string|text|int|long|decimal|date|datetime|bool`。
脚本会一并生成前端页面/接口/类型，并自动注册两个 pom。用法与坑见 skill `crud-module`
（`.claude/skills/crud-module/SKILL.md`）。

---

## 手工方式（了解原理 / 脚本不适用时）

**四步：建目录 → 复制改名 → 注册模块 → 只填业务逻辑。**

```bash
# ① 建目录（示例模块名 biotech、实体名 ProductConfig、包名 org.dromara.biotech）
cd template-backend/ruoyi-modules
mkdir -p ruoyi-biotech/src/main/java/org/dromara/biotech/{controller,service/impl,mapper,domain/bo,domain/vo}
mkdir -p ruoyi-biotech/src/main/resources/mapper/biotech

# ② 复制（在仓库根目录执行）
cd ../..
cp ai-templates/backend-module/pom.xml                   template-backend/ruoyi-modules/ruoyi-biotech/pom.xml
B=template-backend/ruoyi-modules/ruoyi-biotech/src/main/java/org/dromara/biotech
cp ai-templates/backend-module/ProductConfig.java        $B/domain/
cp ai-templates/backend-module/ProductConfigBo.java      $B/domain/bo/
cp ai-templates/backend-module/ProductConfigVo.java      $B/domain/vo/
cp ai-templates/backend-module/ProductConfigController.java $B/controller/
cp ai-templates/backend-module/IProductConfigService.java   $B/service/
cp ai-templates/backend-module/ProductConfigServiceImpl.java $B/service/impl/
cp ai-templates/backend-module/ProductConfigMapper.java      $B/mapper/
cp ai-templates/backend-module/ProductConfigMapper.xml   template-backend/ruoyi-modules/ruoyi-biotech/src/main/resources/mapper/biotech/

# ③ 改名：先改内容，再改文件名（两步都要，别只做第一步）
cd template-backend/ruoyi-modules/ruoyi-biotech
grep -rl 'ProductConfig' . | xargs sed -i 's/ProductConfig/你的实体名/g'
grep -rl 'product_config' . | xargs sed -i 's/product_config/你的表名/g'
grep -rl 'org\.dromara\.biotech' . | xargs sed -i 's/org\.dromara\.biotech/org.dromara.你的模块/g'

# 文件名也要改：Java 的 public class 必须与文件名一致，
# 否则编译不过、Checkstyle 的 OuterTypeFilename 也会拦
for f in $(find . -name '*ProductConfig*'); do
  mv "$f" "$(echo "$f" | sed 's/ProductConfig/你的实体名/')"
done

# 自检：内容与文件名都应无残留
grep -rn 'ProductConfig\|product_config\|org\.dromara\.biotech' . && echo '⚠ 还有残留' || echo '✓ 改名干净'
for f in $(find . -name '*.java'); do
  cls=$(grep -m1 -oE '(class|interface) [A-Za-z0-9_]+' "$f" | awk '{print $2}')
  [ "${f%.java}" = "$(dirname "$f")/$cls" ] || echo "⚠ 类名与文件名不一致: $f (声明 $cls)"
done
```

**④ 注册模块（漏了这步编译不过 / 接口 404）**

| 文件 | 改什么 |
|---|---|
| `template-backend/ruoyi-modules/pom.xml` | `<modules>` 里加一行 `<module>ruoyi-biotech</module>` |
| `template-backend/ruoyi-admin/pom.xml` | `<dependencies>` 里加对 `ruoyi-biotech` 的依赖（版本由父 POM 管，不写 `<version>`） |
| 包名 | **必须在 `org.dromara.*` 下** —— 启动类只有 `@SpringBootApplication`，组件扫描根就是这个包 |
| Mapper / XML 位置 | 无需配置：`application.yml` 已是 `mapperPackage: org.dromara.**.mapper`、`mapperLocations: classpath*:mapper/**/*Mapper.xml`、`typeAliasesPackage: org.dromara.**.domain` |

验证：

```bash
cd template-backend
mvn -B -ntp -pl ruoyi-modules/ruoyi-biotech -am validate     # 期望 BUILD SUCCESS
cd .. && python3 tools/readability_check.py --changed-only origin/main
```

**⑤ 前端配套**：接口层与页面从 `ai-templates/frontend-module/` 复制；
路由用 `pnpm gen-route` 生成；菜单与权限点位（`biotech:productConfig:add` 这一串）
前后端必须**逐字一致**，加进 `sys_menu` 与权限点位表。


## 分层与命名（不得偏离）

| 层 | 类名 | 职责 | 禁止 |
|---|---|---|---|
| Controller | `XxxController extends BaseController` | HTTP 参数、校验、权限、包装返回 | 写业务逻辑、拼 SQL |
| Service | `IXxxService` + `XxxServiceImpl` | 业务规则、事务边界、跨 Mapper 操作 | 拼 SQL、碰 HttpServletRequest |
| Mapper | `XxxMapper extends BaseMapperPlus<Xxx, XxxVo>` | 持久化 | 业务判断 |
| 实体 | `domain/Xxx` | 表映射，`extends BaseEntity` | 直接当接口返回（防敏感字段泄漏） |
| 请求对象 | `domain/bo/XxxBo` | 入参校验 | 承载响应 |
| 响应对象 | `domain/vo/XxxVo` | 出参 | 混入入参校验注解 |

## 硬性约定（模板已体现）

- 普通接口返回 `R<T>`，分页返回 `TableDataInfo<T>`，分页入参用 `PageQuery`
- 构造器注入：`@RequiredArgsConstructor` + `private final`
- 管理接口加 `@SaCheckPermission("模块:实体:动作")`；写接口加 `@Log(title=..., businessType=...)`；防重复提交加 `@RepeatSubmit`
- 单表查询用 `BaseMapperPlus` + `LambdaQueryWrapper`；**查询按「声明 Wrapper → 基础条件 → 条件分支 → 排序 → 执行」展开**，权限判断只算一次
- 复杂联表 / 子查询 / 聚合**写 Mapper XML**；禁止 `@Select` / `@Insert` / `@Update` / `@Delete` / `*Provider` / `@SelectKey`
- SQL 值参数用 `#{param}`；动态表名/列名/排序字段必须服务端白名单校验
- 缺数据抛 `ServiceException`（或项目既有异常），**不返回裸 null**
- 类与方法上方保留 Javadoc（写清 @param / @return），注释解释"为什么"而不是复述代码

## 提交前

```bash
cd template-backend
mvn -B -ntp -pl ruoyi-modules/ruoyi-biotech -am validate
cd .. && python3 tools/readability_check.py --changed-only origin/main
```
