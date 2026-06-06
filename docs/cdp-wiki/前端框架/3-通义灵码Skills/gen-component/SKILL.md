---
type: gen
description: 基于 CDP 前端框架规范，生成 Vue 组件骨架
---

# 生成 Vue 组件

## 输入参数

| 参数 | 必填 | 说明 | 示例 |
|------|------|------|------|
| 组件名称 | 是 | PascalCase 格式的组件名 | `UserProfile` |
| 组件类型 | 是 | `页面` / `业务` / `通用`（NUI 封装） | `业务` |
| Props列表 | 否 | 属性名:类型 格式，多个用逗号分隔 | `title:string, data:any[], loading:boolean` |
| Emits列表 | 否 | 事件名:参数类型 格式，多个用逗号分隔 | `change:string, submit, delete:number` |

## 组件类型说明

| 类型 | 存放目录 | 注册方式 | 特点 |
|------|---------|---------|------|
| 页面 | `src/{模块}/views/{页面名}/index.vue` | 路由自动加载 | 包含 `app-container` 容器，可使用 CdpTable |
| 业务 | `src/{模块}/components/{组件名}.vue` | 局部 import 或 unplugin 自动扫描 | 可复用的业务逻辑组件 |
| 通用 | `src/cdp-common-nui/packages/{组件名}/index.vue` | `import.meta.glob` 自动全局注册 | 必须声明 `name`，Element Plus 二次封装 |

## 执行步骤

### 第 1 步：创建组件文件

根据组件类型选择对应模板生成 `.vue` 文件。

#### 页面组件模板

创建文件 `src/{模块名}/views/{页面名}/index.vue`：

```vue
<script setup lang="ts">
/**
 * {组件名称} 页面
 */

// Props（页面组件通常无需 Props）

// 响应式数据
const loading = ref(false);
const queryParams = reactive<PageQuery>({
  pageNum: 1,
  pageSize: 10,
});
const total = ref(0);

// 方法
function handleQuery() {
  loading.value = true;
  // TODO: 调用 API 获取数据
  loading.value = false;
}

// 生命周期
onMounted(() => {
  handleQuery();
});
</script>

<template>
  <div class="app-container">
    <!-- TODO: 页面内容 -->
  </div>
</template>

<style scoped lang="scss">
// 全局 SCSS 变量已自动注入
</style>
```

#### 业务组件模板

创建文件 `src/{模块名}/components/{组件名称}.vue`：

```vue
<script setup lang="ts">
/**
 * {组件名称} 组件
 */

// Props
const props = defineProps<{
  {propName}: {propType};
}>();

// Emits
const emit = defineEmits<{
  (e: "{emitName}", value: {emitType}): void;
}>();

// 组件逻辑
</script>

<template>
  <div class="{组件kebab-case名}">
    <slot />
  </div>
</template>

<style scoped lang="scss">
.{组件kebab-case名} {
  // 样式
}
</style>
```

#### 通用组件模板（NUI 封装）

创建文件 `src/cdp-common-nui/packages/{组件kebab-case名}/index.vue`：

```vue
<script lang="ts">
export default {
  name: "Nui{组件名称}", // 必须声明 name，全局注册依赖此属性
};
</script>

<script setup lang="ts">
/**
 * Nui{组件名称} — Element Plus 二次封装组件
 */

// Props（使用 withDefaults 设置默认值）
const props = withDefaults(
  defineProps<{
    {propName}?: {propType};
  }>(),
  {
    {propName}: {defaultValue},
  }
);

// Emits
const emit = defineEmits<{
  (e: "{emitName}", value: {emitType}): void;
}>();

// 组件逻辑
</script>

<template>
  <div class="nui-{组件kebab-case名}">
    <div class="nui-{组件kebab-case名}__body">
      <slot />
    </div>
    <div v-if="$slots.footer" class="nui-{组件kebab-case名}__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.nui-{组件kebab-case名} {
  border-radius: 4px;

  &__body {
    padding: 16px;
  }

  &__footer {
    padding: 12px 16px;
    border-top: 1px solid #ebeef5;
  }
}
</style>
```

### 第 2 步：定义 Props/Emits

根据输入的 Props 列表和 Emits 列表，填充组件模板中的类型定义。

Props 定义规范：
- 必填属性不加 `?`，可选属性加 `?`
- 通用组件使用 `withDefaults` 提供默认值
- 复杂类型单独定义 interface

```typescript
// Props 泛型定义
const props = defineProps<{
  title: string;           // 必填
  data?: any[];            // 可选
  loading?: boolean;       // 可选
}>();

// Emits 类型声明
const emit = defineEmits<{
  (e: "change", value: string): void;
  (e: "submit"): void;
  (e: "delete", id: number): void;
}>();
```

### 第 3 步：添加基础模板和样式

根据组件类型补充模板结构：
- 页面组件：添加 `app-container` 容器，按需引入 CdpTable、Pagination
- 业务组件：添加默认插槽和具名插槽
- 通用组件：使用 BEM 命名（`nui-组件名__元素--修饰`），添加 `v-bind="$attrs"` 透传属性

样式统一使用 `<style scoped lang="scss">`，全局 SCSS 变量（如 `$c-m`）和 mixin 可直接使用。

## 完成后提醒

- [ ] 组件文件已创建在正确的目录位置
- [ ] Props 和 Emits 使用 TypeScript 泛型声明
- [ ] 通用组件（NUI）已在 `<script>` 块中声明唯一的 `name` 属性
- [ ] 样式使用 `scoped` 避免全局污染，CSS 类名为 kebab-case
- [ ] 通用组件需重启开发服务器（`pnpm run dev`）以触发 `import.meta.glob` 重新扫描
- [ ] 页面组件需配置对应路由才能访问（参考 gen-route-config 技能）
