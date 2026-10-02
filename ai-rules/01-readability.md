# 01 · 可读性契约

> 需求①。适用于 Java（`template-backend/`）与 Vue/TS（`template-frontend/`）。
> 分级：**【禁】**任何情况不做 · **【硬】**必改（机械闸门会拦）· **【软】**需在 PR 里说明理由。
> 带 ⚙ 的由 `python3 tools/readability_check.py` 自动检查。

## 0. 唯一的总原则

**可读性 > 简洁 > 性能。** 写给三个月后接手的人看，不是写给编译器看。
判据一句话：**这段代码能不能用「一行一行的 if / for / 赋值」直接读出来？** 不能就重写。

关于"允许的抽象"，只有一条：

> 同一段逻辑**重复出现 3 次以上**，才抽成一个有业务名字的函数。
> 除此之外的任何抽象（接口、工厂、策略、注册表、DI 容器、基类）都算过度设计。

### 与已有"设计原则"说法的口径修正（重要）

`template-frontend/.claude/CLAUDE.md` 写的是「设计原则: SRP · OCP · LSP · ISP · DIP · DRY · KISS · 第一性原理」。
这批原则**不是等价的**，本仓库按下面的口径执行，避免 AI 主动往抽象上跑：

| 原则 | 本仓库口径 |
|---|---|
| KISS、DRY、第一性原理 | ✅ 采纳 |
| SRP（单一职责） | ✅ 采纳，但表现为"函数短、文件按职责拆"，不是"为每个职责建一个类" |
| OCP（开闭） | ⚠️ **不作为目标**。没有第二个实现就不留扩展点 |
| LSP / ISP | ⚠️ 只在确实使用继承/接口时适用；本仓库默认用组合，不用继承 |
| DIP（依赖倒置） | ⚠️ **不作为目标**。不为了"可替换"而抽接口 |

## 1. 【禁】语法糖与炫技

**Java**
- 【禁】`@Select` / `@Insert` / `@Update` / `@Delete` / `*Provider` / `@SelectKey` 在 Java 里写 SQL —— 手写 SQL 放 Mapper XML ⚙
- 【禁】把复杂 SQL 整段塞进 Wrapper 的 `apply` / `inSql` / `exists` / `last`，或在 Service 里拼 SQL
- 【禁】空 catch 或只留一句注释的 catch ⚙
- 【禁】为少写几行引入的自定义注解、AOP 切面、动态代理
- 【软】`import x.*` —— 历史代码保留（见 `backend-code-standard.md` 第 100 行），**新代码显式列出** ⚙
- 【软】嵌套三元 `a ? b : x ? y : z` ⚙
- 【软】一行串 5 个以上方法调用 ⚙

**Vue / TS**
- 【禁】`var`、`==` / `!=`、`eval` / `new Function`、`debugger` ⚙
- 【禁】新建第二套请求封装 / 第二套表格 hook / 第二个 UI 库。既有 `useNaivePaginatedTable`、`useTableOperate`、`useNaiveForm`、`NSelect`、`window.$message` 一律复用
- 【禁】在组件里写 `any` 绕过类型；类型放 `src/typings/` ⚙
- 【软】`console.log` 残留 ⚙
- 【软】嵌套三元、可选链超过 2 段

**通用**
- 【禁】硬编码密钥 / 口令 / Token / IP ⚙（确属格式常量或 i18n 文案，在该行加注释 `readability-check: allow` 说明）
- 【软】遗留 `TODO` / `FIXME` ⚙

## 2. 【禁】过度设计与投机抽象

- 【禁】为「将来可能的需求」预留接口、扩展点、配置开关
- 【禁】只有一个实现的 interface / 抽象类 / 工厂 / 策略 / 注册表
- 【禁】元类、自定义 `@Component` 之外的 AOP、`BeanPostProcessor` 级别的魔法（除非仓库已有先例）
- 【禁】引入新的 DI 容器、ORM、状态管理库、UI 库
- 【软】多继承 / 深层继承树 → 改组合
- 允许的自然抽象，仅此两类：**重复 3 次以上抽函数**、**按既有分层落位**（Java 的 Controller/Service/Mapper，前端的 view/component/hook）

## 3. 【硬】尺寸红线 ⚙

| 对象 | 上限 | 超了怎么办 |
|---|---|---|
| 单文件 | 500 行 | 按职责拆文件 |
| 单方法 / 单函数 | 50 行 | 拆成有业务名字的小方法 |
| 控制流嵌套 | 3 层 | 提前 `return` / `continue` 拉平 |
| 方法参数 | 6 个 | 传一个显式的 BO / DTO / 入参对象 |
| 单行 | 120 字符 | 换行 |
| 显式 `any` | 0 | 写具体类型，或 `unknown` + 收窄 ⚙ 仅前端 |

## 4. 【硬】错误处理

- 【禁】空 catch、`catch (Exception e) {}`、`except: pass` ⚙
- 【硬】catch 里至少做一件事：记日志（含异常对象，保留堆栈）、转成项目业务异常、或重新抛出 ⚙
- 【硬】缺数据要**明确报错**或返回空集合；`return null` 必须在该行注释说明理由 ⚙
- 【硬】前端失败要保持表单与输入、pending 时禁止重复提交与关闭抽屉（见 `UX-CONTRACT.md`）

## 5. 反例 → 正例

**Java · Wrapper 与权限判断**（对齐 `backend-code-standard.md` 第 25 行）

```java
// ✗ 反例：条件、权限、排序挤在一行，读的人要在脑子里拆括号
List<SysTenant> list = mapper.selectList(new LambdaQueryWrapper<SysTenant>()
    .eq(SysTenant::getStatus, "0")
    .eq(!LoginHelper.isSuperAdmin(), SysTenant::getTenantId, TenantContext.getTenantId())
    .orderByAsc(SysTenant::getId));

// ✓ 正例：声明 → 基础条件 → 条件分支 → 排序 → 执行；同一权限只算一次
boolean isSuperAdmin = LoginHelper.isSuperAdmin();
LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
wrapper.select(SysTenant::getId, SysTenant::getTenantId, SysTenant::getTenantName, SysTenant::getStatus)
    .eq(SysTenant::getStatus, "0");

if (!isSuperAdmin) {
    wrapper.eq(SysTenant::getTenantId, TenantContext.getTenantId());
}

wrapper.orderByAsc(SysTenant::getId);
return mapper.selectList(wrapper);
```

**Java · 嵌套拉平**

```java
// ✗ 4 层嵌套
if (order != null) { if (order.isPaid()) { if (order.hasStock()) { ship(order); } } }

// ✓ 提前 return，一层到底
if (order == null)      { throw new ServiceException("订单不存在"); }
if (!order.isPaid())    { throw new ServiceException("订单未支付"); }
if (!order.hasStock())  { throw new ServiceException("库存不足"); }
ship(order);
```

**Vue · 复用既有能力，不另起一套**

```vue
<!-- ✗ 反例：自己写分页表格 + 自己写的 message + any 绕过类型 -->
<template><n-data-table :data="list" /><button @click="save">保存</button></template>
<script setup lang="ts">
const list = ref<any[]>([]);
async function save(x: any) { if (x == null) { alert('空'); return; } }
</script>

<!-- ✓ 正例：复用 useNaivePaginatedTable / useTableOperate，类型来自 typings -->
<script setup lang="ts">
import { useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
const { data: rows, loading, getDataByPage } = useNaivePaginatedTable({ apiFn: fetchProductConfigList });
const { handleAdd, handleEdit } = useTableOperate(rows, getDataByPage);
</script>
```

## 6. 机械闸门

```bash
python3 tools/readability_check.py --changed-only origin/main   # 只查本次改动（推荐）
python3 tools/readability_check.py template-backend             # 全量，看老债
python3 tools/readability_check.py --fail-on warn               # 严格模式（warn 也算失败）
python3 tools/readability_check.py --json report.json           # 给 CI 归档
```

- **error 必须清零才能提 PR**；warn 需在 PR 描述里逐条说明理由。
- 豁免：在该行加 `readability-check: allow` 并写明原因（用于格式常量、i18n 文案、确有必要的长行）。
- 实现说明：Java 没有内置 AST，方法长度/嵌套用花括号配对**估算**（已剥离字符串与注释），
  偶有误报 —— 报告给了文件:行号，人工确认即可。规则清单见 `--rules`。

### 当前基线（首次实测，供对比）

```
template-backend    437 文件   error 24   warn 425   ← error: 深层嵌套 12、空 catch 12
template-frontend   268 文件   error 13   warn 110   ← error: 深层嵌套 12、疑似凭据 1（待人工确认）
```

这是**模板自带代码的老债**，不用一次性清。闸门默认只查 `--changed-only`：**新改动清零，老债慢慢还。**

## 7. 机器查不到、只能靠评审的（写进评审清单）

1. 命名是否表达了业务含义（`data` / `temp` / `d1` / `handleClick2` 一律算不合格）
2. 抽象是否属于"为将来预留"
3. 是否属于"同一件事的第二种做法"（仓库里已经有类似实现）
4. 注释是否在解释"为什么"而不是复述代码
5. 新代码是否和同目录里最像的那个文件保持同一风格
