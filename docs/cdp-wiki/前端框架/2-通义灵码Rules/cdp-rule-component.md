---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/05-module-component-architecture.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-component.md
  - docs/knowledge-base/5-代码片段库/nui-component-template.vue
---

# CDP 组件开发规范

## 适用场景

本规则适用于以下开发任务：
- 使用 NUI 组件库（`cdp-common-nui`，Element Plus 二次封装）
- 使用公共控件（`cdp-common-ctrl`，CdpTable、Pagination、OrgRoleUser 等）
- 自定义 NUI 组件封装
- 业务组件开发（页面组件、业务组件、通用组件）
- Props/Emits 类型定义与 Slot 设计
- 组件间通信模式选型

## 前置依赖

- 组件语法：必须使用 `<script setup lang="ts">`，禁止 Options API
- Element Plus 组件通过 `unplugin-vue-components` 自动按需导入，无需手动 import
- NUI 组件（52 个）通过 `import.meta.glob` 在 `main.ts` 中自动全局注册
- 全局手动注册组件：CdpTable、CdpTableList、OrgRoleUser、OrgunitTree、SvgIcon、Pagination
- Element Plus 图标组件已全局注册（如 `<Edit />`、`<Delete />`），无需额外 import
- SCSS 全局变量和 mixin 由 Vite `additionalData` 自动注入

## 配置要点

### 三层组件架构

| 层级 | 目录 | 数量 | 注册方式 | 职责 |
|------|------|------|---------|------|
| NUI | `cdp-common-nui/packages/` | 52 | `import.meta.glob` 自动 | Element Plus 统一封装，扩展默认配置 |
| Ctrl | `cdp-common-ctrl/components/` | 20+ | `main.ts` 手动注册 | 业务通用控件（表格、分页、上传、树选择等） |
| Admin | `cdp-common-admin/components/` | 15+ | `unplugin-vue-components` 自动 | 管理端专用组件 |

### NUI 组件注册机制

NUI 组件必须在 `<script lang="ts">` 中声明 `name` 属性，`main.ts` 读取 `component.default.name` 进行全局注册。没有 `name` 的组件会被跳过。

### 常用 Ctrl 组件

| 组件 | 用途 |
|------|------|
| `CdpTable` | 增强表格，自动计算高度，支持全屏，含 queryBar/operaBar 插槽 |
| `Pagination` | 分页栏，配合 `PageQuery` / `PageResult<T>` 类型 |
| `OrgRoleUser` | 组织/角色/用户选择器 |
| `SvgIcon` | SVG 图标，图标文件放 `src/assets/icons/` |

## 代码模式

### 推荐写法

#### 标准业务组件

```vue
<script setup lang="ts">
const props = defineProps<{
  title: string;
  data?: any[];
}>();

const emit = defineEmits<{
  (e: "change", value: string): void;
  (e: "submit"): void;
}>();

const loading = ref(false);
</script>

<template>
  <div class="my-component">
    <h3>{{ title }}</h3>
    <slot />
  </div>
</template>

<style scoped lang="scss">
.my-component {
  padding: 16px;
}
</style>
```

#### NUI 组件模板（必须声明 name）

```vue
<script lang="ts">
export default {
  name: "NuiMyComponent", // 必须声明，全局注册依赖此属性
};
</script>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    title?: string;
    size?: "small" | "default" | "large";
  }>(),
  { title: "", size: "default" }
);

const emit = defineEmits<{
  (e: "click"): void;
  (e: "change", value: string): void;
}>();
</script>

<template>
  <div class="nui-my-component" :class="[`nui-my-component--${size}`]">
    <div v-if="title" class="nui-my-component__header">{{ title }}</div>
    <slot />
    <slot name="footer" />
  </div>
</template>

<style scoped lang="scss">
.nui-my-component {
  border-radius: 4px;
  &__header { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
}
</style>
```

#### CdpTable 使用示例

```vue
<template>
  <CdpTable ref="cdpTable" @getTableData="getTableData">
    <template #queryBar>
      <el-form-item label="名称">
        <el-input v-model="queryParams.name" />
      </el-form-item>
    </template>
    <template #operaBar>
      <el-button v-hasPerm="['sys:user:add']" type="primary" @click="handleAdd">
        新增
      </el-button>
    </template>
    <template #default>
      <el-table-column prop="name" label="名称" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </template>
  </CdpTable>
</template>
```

#### Pagination 配合分页类型

```vue
<template>
  <Pagination
    v-if="total > 0"
    :total="total"
    v-model:page="queryParams.pageNum"
    v-model:limit="queryParams.pageSize"
    @pagination="handleQuery"
  />
</template>
```

#### Slot 与组件通信

```vue
<!-- Slot 类型声明 -->
<script setup lang="ts">
defineSlots<{
  default(): any;
  header(props: { title: string }): any;
}>();
</script>

<!-- 父子通信：Props 向下，Emits 向上 -->
<ChildComponent :data="listData" @update="handleUpdate" />

<!-- 跨层级通信：provide/inject -->
<script setup lang="ts">
provide("sharedKey", sharedValue);
// 子组件中: const value = inject("sharedKey");
</script>
```

### 禁止事项

1. **禁止使用 Options API** — 必须使用 `<script setup lang="ts">`
2. **禁止 NUI 组件省略 `name` 属性** — 没有 `name` 的组件不会被全局注册
3. **禁止手动 import Element Plus 组件** — 已通过 `unplugin-vue-components` 自动导入
4. **禁止手动 `@import` SCSS 全局变量** — 已由 Vite `additionalData` 自动注入
5. **禁止在 Component 层直接调用 API** — 通过 Props/Emits 与 View 层通信
6. **禁止使用 `any` 类型定义 Props** — 使用具体类型或 `Record<string, unknown>`
7. **禁止 NUI 组件名与已有全局组件冲突** — 后注册的会覆盖先注册的
8. **禁止在非 `src/assets/icons/` 目录放置 SVG 图标** — `vite-plugin-svg-icons` 仅扫描该目录
9. **禁止新增 NUI 组件后不重启开发服务器** — `import.meta.glob` 仅在启动时扫描
