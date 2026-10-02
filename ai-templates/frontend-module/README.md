# 前端模块模板（照抄即可）

> 用途：需求③「code-template」。**新页面一律从这里复制**，改名后只填业务逻辑。
> 依据：现有 `src/views/system/*` 真实写法 + `template-frontend/DESIGN.md` + `UX-CONTRACT.md`。

## 怎么用

推荐用 `tools/new_crud.py` 一条命令生成（同时产后端 + 前端 5 个文件），见 skill `crud-module`。
只有前端、没有后端实体时，手工复制**本目录**的文件：

```
src/views/<实体短横线>/index.vue                             列表页        ← index.vue
src/views/<实体短横线>/modules/<实体>-search.vue              搜索区        ← search.vue
src/views/<实体短横线>/modules/<实体>-operate-drawer.vue      新增/编辑抽屉  ← operate-drawer.vue
src/service/api/<模块>/<实体短横线>.ts                        接口层        ← api.ts
src/typings/api/<模块>.api.d.ts                              类型定义      ← types.d.ts
```

> 目录落位现状（待项目拍板）：`new_crud.py` 生成的是 `src/views/<实体短横线>/`（一层），
> 而仓库既有页面（`system/notice`、`system/tenant` …）是 `src/views/<模块>/<实体>/`（两层）。
> 两者只能选一种；定了以后对齐 `new_crud.py` 的 `fe_jobs` 一处即可。

注意最后一行：**类型是"一模块一文件"**（`src/typings/api/<模块>.api.d.ts`，
里面是 `declare namespace Api { namespace <模块> { interface ... } }`）。
同模块加第二个实体时不要覆盖该文件，要把新实体的 `namespace` 手工合并进去。

1. 全局替换实体名（示例 `ProductConfig` / `productConfig` / `product-config` 三种形态）
   与路由、权限点位
2. **只填业务**，不动既定结构
3. 路由用 `pnpm gen-route` 生成，不手写 router 文件

## 必须遵守（模板已体现）

- `<script setup lang="tsx">` + `defineOptions({ name: 'XxxList' })` —— 少这个 keep-alive 会失效
- 列表统一用 `useNaivePaginatedTable`；增删改用 `useTableOperate`；表单用 `useNaiveForm`
- 权限用 `useAuth().hasAuth('模块:实体:动作')`，与后端 `@SaCheckPermission` 字符串**逐字一致**
- 表格操作列、搜索卡片、抽屉都用项目既有组件（`ButtonIcon`、`TableHeaderOperation`、`DictTag`），**不新引 UI 库**
- 单选/下拉用 `NSelect`，消息用应用级 `window.$message`，**不写 `alert`/`console.log`**
- 类型放 `src/typings/`，命名空间 `Api.<模块>.<实体>`；**不写 `any`**
- 搜索参数命名 `xxxSearchParams`，分页字段 `pageNum` / `pageSize`
- 失败时保留抽屉与输入；pending 时禁止重复提交与关闭

## 提交前

```bash
cd template-frontend
pnpm typecheck && pnpm lint
cd .. && python3 tools/readability_check.py --changed-only origin/main
python3 tools/structure_check.py --changed-only origin/main   # 页面结构契约（缺组件/hooks/落位）
```
