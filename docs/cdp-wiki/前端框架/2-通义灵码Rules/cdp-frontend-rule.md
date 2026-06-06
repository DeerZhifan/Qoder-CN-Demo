---
trigger: always_on
knowledge_source:
  - docs/knowledge-base/1-架构总览/01-architecture-overview.md
  - docs/knowledge-base/4-代码规范文档/12-coding-standards.md
---

# CDP 前端开发规范

## 适用场景

本规则适用于 CDP Web 前端框架下的所有开发任务，包括：新增页面、编写组件、定义 API 接口、创建 Pinia Store、修改路由与权限配置。技术栈为 Vue 3.4 + TypeScript 5.4 + Element Plus 2.8 + Pinia 2.1 + Vite 5.x + UnoCSS + Axios。

## 前置依赖

- 包管理器仅限 **pnpm**（preinstall 钩子强制）
- 路径别名 `@/` 映射到 `src/`，跨模块引用必须使用别名
- SCSS 全局变量（`variables.scss`）和 mixin（`mixin.scss`）由 Vite 自动注入，无需手动 `@import`
- Element Plus 组件通过 unplugin-vue-components 自动按需导入，无需手动 import
- 全局组件（CdpTable、Pagination、OrgRoleUser、OrgunitTree、SvgIcon）已在 main.ts 注册

## 配置要点

### 目录结构

```
src/
├── _changeable/          聚合层（路由 router.ts / API api.ts / 常量 GlobalConst.ts）
├── cdp-admin/            应用壳层（Pinia Store + Layout 布局）
├── cdp-common/utils/     核心工具（request / auth / aes / rsa / validate）
├── cdp-common-nui/       52 个 Element Plus 二次封装组件
├── cdp-common-ctrl/      业务控件 + 权限指令（v-hasPerm / v-hasRole）
├── cdp-common-admin/     管理端 API + 组件
├── cdp-common-frame/     15 个业务模块（各含 api/ + views/）
├── workflow/             工作流引擎集成
├── styles/               全局 SCSS（variables / mixin / theme）
└── typings/              全局 TypeScript 类型（PageQuery / PageResult<T>）
```

### 命名约定

| 类型 | 规范 | 示例 |
|------|------|------|
| Vue 组件文件 | PascalCase 或 index.vue | `CdpTable.vue`、`index.vue` |
| API 文件/目录 | kebab-case | `dictionary-kind/`、`api-manage/` |
| 工具文件 | camelCase 或 kebab-case | `request.ts`、`scroll-to.ts` |
| Pinia Store | `useXxxStore` + `useXxxStoreHook` | `useUserStore()` |
| API 函数 | 动词 + 名词 + Api | `loginApi`、`getUserInfoApi` |
| 类型/接口 | PascalCase，不加 I 前缀 | `LoginData`、`UserQuery` |
| CSS 类名 | kebab-case | `app-container`、`sidebar-logo` |
| 常量 | UPPER_SNAKE_CASE | `TOKEN_KEY` |

### 分层规则

1. **View 层** — 组合 API 调用和 Store，处理页面交互，不直接操作 HTTP
2. **API 层** — 仅封装 HTTP 请求，类型定义放 `types.ts`，不含业务逻辑
3. **Store 层** — 管理跨组件共享状态，不直接操作 DOM
4. **Component 层** — 通过 Props/Emits 通信，不直接调用 API

## 代码模式

### 推荐写法

#### 标准 Vue 组件

```vue
<script setup lang="ts">
// Props 使用泛型定义
const props = defineProps<{
  title: string;
  list?: any[];
}>();

// Emits 使用类型声明
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

<template>
  <div class="my-component">
    <slot />
  </div>
</template>

<style scoped lang="scss">
.my-component {
  padding: 16px;
}
</style>
```

#### API 接口定义

```typescript
import { postJson, get, del } from "@/cdp-common/utils/request";
import { UserQuery, UserForm } from "./types";

export function listUsers(params: UserQuery) {
  return postJson("/system/user/page", params);
}
```

#### 分页查询类型

```typescript
// 查询参数继承 PageQuery
interface UserQuery extends PageQuery {
  username?: string;
  status?: number;
}

// 分页结果使用 PageResult<T>
type UserPageResult = PageResult<UserInfo>;
```

#### Pinia Store

```typescript
export const useUserStore = defineStore("user", () => {
  const token = useStorage("cdp-token", "");
  // ...
  return { token };
});

// 组件外访问（路由守卫、拦截器）
export function useUserStoreHook() {
  return useUserStore(store);
}
```

#### 权限控制

```vue
<template>
  <el-button v-hasPerm="['sys:user:add']">新增</el-button>
</template>
```

#### 样式穿透

```vue
<style scoped lang="scss">
:deep(.el-table__header) {
  background: $bg-color;
}
</style>
```

### 禁止事项

1. **禁止使用 Options API** — 必须使用 `<script setup lang="ts">`
2. **禁止直接使用 axios** — 必须使用 `@/cdp-common/utils/request` 封装方法
3. **禁止跨模块相对路径引用** — 必须使用 `@/` 路径别名
4. **禁止使用 any** — 使用 `Record<string, any>` 或 `unknown` 替代
5. **禁止手动 @import SCSS 全局变量** — 已由 Vite additionalData 自动注入
6. **禁止组件内硬编码路由路径** — 路由配置统一在 `_changeable/router.ts` 管理
7. **禁止直接读取 permission store 判断权限** — 使用 `v-hasPerm` 指令
8. **禁止自行实现加密逻辑** — 使用框架提供的 `aes.ts` 和 `rsa.ts`
9. **禁止模块间循环依赖** — 下层模块不可引用上层模块
10. **禁止在 Component 层直接调用 API** — 通过 Props/Emits 与 View 层通信
