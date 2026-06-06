# 编码规范

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: 编码规范, 命名, TypeScript, Vue组件, 目录结构, Git提交

---

## 一、命名规范

### 文件与目录

| 类型 | 规范 | 示例 |
|------|------|------|
| Vue 组件目录 | PascalCase | `CdpTable/`、`OrgRoleUser/` |
| Vue 组件文件 | PascalCase 或 index.vue | `CdpTable.vue`、`index.vue` |
| API 文件 | 小写 kebab-case | `dictionary-kind/`、`api-manage/` |
| 工具文件 | 小写 camelCase 或 kebab-case | `request.ts`、`scroll-to.ts` |
| 类型文件 | `types.ts` | 固定命名 |
| 样式文件 | 小写 kebab-case | `variables.scss`、`mixin.scss` |
| 常量文件 | 小写 camelCase | `const.ts`、`const_workflow.ts` |

### 代码命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 组件 name 属性 | PascalCase | `name: "CdpTable"` |
| Pinia Store | `useXxxStore` + `useXxxStoreHook` | `useUserStore`、`useUserStoreHook` |
| API 函数 | 动词 + 名词 + `Api` | `loginApi`、`getUserInfoApi`、`listRoutes` |
| 类型/接口 | PascalCase | `LoginData`、`UserInfo`、`PageQuery` |
| ref 变量 | camelCase | `tableData`、`loading`、`total` |
| 常量 | UPPER_SNAKE_CASE（语义常量）或 camelCase | `TOKEN_KEY`、`whiteList` |
| CSS 类名 | kebab-case | `app-container`、`sidebar-logo` |

---

## 二、Vue 组件编写规范

### 必须使用 Composition API

```vue
<!-- ✅ 正确 -->
<script setup lang="ts">
const count = ref(0);
</script>

<!-- ❌ 禁止使用 Options API -->
<script>
export default {
  data() { return { count: 0 } }
}
</script>
```

### 标准组件结构

```vue
<!-- 1. NUI 组件需要单独声明 name -->
<script lang="ts">
export default { name: "MyComponent" };
</script>

<!-- 2. 组合式 setup -->
<script setup lang="ts">
// Props
const props = defineProps<{
  title: string;
  list?: any[];
}>();

// Emits
const emit = defineEmits<{
  (e: "change", value: string): void;
}>();

// 响应式数据
const loading = ref(false);
const data = ref([]);

// 计算属性
const isEmpty = computed(() => data.value.length === 0);

// 方法
function handleClick() { /* ... */ }

// 生命周期
onMounted(() => { /* ... */ });
</script>

<!-- 3. 模板 -->
<template>
  <div class="my-component">
    <slot />
  </div>
</template>

<!-- 4. 样式 -->
<style scoped lang="scss">
.my-component {
  padding: 16px;
}
</style>
```

### 要点

- `<script setup lang="ts">` 必须带 `lang="ts"`
- Props 使用 `defineProps<T>()` 泛型，不使用运行时声明
- Emits 使用 `defineEmits<{...}>()` 类型声明
- 样式使用 `<style scoped lang="scss">`
- SCSS 全局变量和 mixin 已自动注入，不需要 `@import`

---

## 三、分层规则

```
┌──────────────────────────────────────────┐
│ View 层 (views/*.vue)                    │  页面级组件
│   · 组合 API 调用和 Store 操作            │  · 处理页面级交互逻辑
│   · 使用全局组件（CdpTable/Pagination）   │  · 不直接操作 HTTP
├──────────────────────────────────────────┤
│ API 层 (api/index.ts + types.ts)         │  接口定义
│   · 仅负责 HTTP 调用                     │  · 不含业务逻辑
│   · 类型定义独立放 types.ts              │  · 使用 request.ts 导出的方法
├──────────────────────────────────────────┤
│ Store 层 (store/modules/*.ts)            │  全局状态
│   · 管理跨组件共享状态                    │  · 不直接操作 DOM
│   · 持久化数据使用 useStorage            │  · Hook 模式支持组件外调用
├──────────────────────────────────────────┤
│ Component 层 (components/*.vue)          │  可复用组件
│   · 通过 Props/Emits 通信                │  · 不直接调用 API
│   · 不依赖特定业务逻辑                   │  · 可在多个 View 中复用
└──────────────────────────────────────────┘
```

### 规则

1. **View** 可以调用 API、Store、Component
2. **API** 仅封装 HTTP 请求，不含业务逻辑
3. **Store** 管理全局状态，可调用 API
4. **Component** 通过 Props 接收数据、Emits 发出事件，不直接调用 API

---

## 四、TypeScript 规范

### 类型文件位置

| 类型 | 位置 | 示例 |
|------|------|------|
| 全局类型 | `src/typings/` | `PageQuery`、`PageResult<T>`、`TagView` |
| API 类型 | 对应模块 `api/types.ts` | `LoginData`、`UserQuery`、`UserForm` |
| 组件 Props 类型 | 组件内 `defineProps<T>()` | 内联定义 |
| Store 类型 | Store 文件内 | 内联定义 |

### 标准分页类型

```typescript
// src/typings/global.d.ts

// 分页查询参数
interface PageQuery {
  pageNum: number;
  pageSize: number;
}

// 分页结果
interface PageResult<T> {
  list: T[];
  total: number;
}
```

### 类型规范

- 避免 `any`，必要时使用 `Record<string, any>` 或 `unknown`
- API 响应类型放在对应模块的 `types.ts` 中
- 查询参数类型继承 `PageQuery`：`interface UserQuery extends PageQuery { ... }`
- 接口名使用 PascalCase，不加 `I` 前缀

---

## 五、API 编写规范

```typescript
// ✅ 正确
import { postJson, get, del } from "@/cdp-common/utils/request";
import { UserQuery, UserForm } from "./types";

export function listUsers(params: UserQuery) {
  return postJson("/system/user/page", params);
}

// ❌ 错误：直接使用 axios
import axios from "axios";
export function listUsers(params) {
  return axios.post("/system/user/page", params);
}
```

### 方法选择

- JSON 对象 → `postJson()`
- 表单提交 → `post()`
- 文件上传 → `uploadFile()`
- 文件下载 → `getExport()` / `getFile()`

---

## 六、Git 提交规范

### 格式

```
type(scope): subject
```

### 工具

```bash
pnpm run commit  # 交互式提交（推荐）
```

### 类型

| 类型 | 说明 | 类型 | 说明 |
|------|------|------|------|
| `feat` | 新功能 | `fix` | Bug 修复 |
| `docs` | 文档更新 | `style` | 代码格式（不影响逻辑） |
| `refactor` | 重构 | `perf` | 性能优化 |
| `test` | 测试 | `build` | 构建相关 |
| `ci` | CI 配置 | `revert` | 回退 |
| `chore` | 杂项 | | |

### 示例

```
feat(system): 新增用户批量导入功能
fix(login): 修复 CAS 登录回调地址拼接错误
refactor(request): 重构请求拦截器加密逻辑
docs: 更新知识库文档
```

---

## 七、路径别名

```typescript
// 统一使用 @/ 别名
import { useUserStore } from "@/cdp-admin/store/modules/user";

// ❌ 不使用相对路径（跨模块时）
import { useUserStore } from "../../cdp-admin/store/modules/user";
```

`@/` 映射到 `src/`，在 `vite.config.ts` 和 `tsconfig.json` 中配置。
