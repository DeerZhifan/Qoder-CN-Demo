# 组件体系设计手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: 组件, NUI, CdpTable, Pagination, Upload, SvgIcon, 自动注册, Element Plus 封装

---

## 一、三层组件架构

CDP Web 采用三层组件架构，从基础到业务逐层封装：

```
┌─────────────────────────────────────────────────┐
│ 第三层: Admin 组件 (cdp-common-admin/components/) │  管理端专用组件
│   UserSelect · HomeToDoList · Tinymce · SvgChartEdit │  15+ 组件
├─────────────────────────────────────────────────┤
│ 第二层: Ctrl 组件 (cdp-common-ctrl/components/)  │  业务通用控件
│   CdpTable · Pagination · Upload · OrgRoleUser · Cron │  20+ 组件
├─────────────────────────────────────────────────┤
│ 第一层: NUI 组件 (cdp-common-nui/packages/)      │  Element Plus 二次封装
│   NuiButton · NuiDialog · NuiForm · NuiTable ... │  52 个组件
├─────────────────────────────────────────────────┤
│ 基础: Element Plus 原生组件                      │  el-button · el-table ...
└─────────────────────────────────────────────────┘
```

| 层级 | 目录 | 组件数量 | 注册方式 | 职责 |
|------|------|---------|---------|------|
| NUI | `cdp-common-nui/packages/` | 52 | `import.meta.glob` 自动注册 | Element Plus 统一封装，扩展默认配置 |
| Ctrl | `cdp-common-ctrl/components/` | 20+ | `main.ts` 手动注册 | 业务通用控件（表格、分页、上传、树选择等） |
| Admin | `cdp-common-admin/components/` | 15+ | `unplugin-vue-components` 自动扫描 | 管理端专用组件 |

---

## 二、NUI 组件注册机制

源码位置：`src/main.ts:66-76`

### 注册流程

```typescript
// 1. 使用 import.meta.glob 动态导入所有 NUI 组件
const componentsContext = import.meta.glob("@/cdp-common-nui/packages/**/*.vue");

// 2. 遍历异步加载每个组件
Object.keys(componentsContext).forEach((key) => {
  const componentConfig = componentsContext[key]();
  componentConfig.then((component) => {
    // 3. 读取组件的 name 属性作为全局注册名
    const componentName = component.default.name;
    if (!componentName) return; // 无 name 属性的组件被跳过！
    // 4. 全局注册
    app.component(componentName, component.default || component);
  });
});
```

> 注意: NUI 组件**必须**在 `<script>` 中声明 `name` 属性，否则不会被全局注册。

### NUI 组件目录结构

```
cdp-common-nui/packages/
├── aside/index.vue        # NuiAside
├── button/index.vue       # NuiButton
├── dialog/index.vue       # NuiDialog
├── drawer/index.vue       # NuiDrawer
├── form/index.vue         # NuiForm
├── input/index.vue        # NuiInput
├── select/index.vue       # NuiSelect
├── table/index.vue        # NuiTable
├── tabs/index.vue         # NuiTabs
├── tree/index.vue         # NuiTree
├── pagination/index.vue   # NuiPagination
├── date-picker/index.vue  # NuiDatePicker
├── cascader/index.vue     # NuiCascader
├── upload/index.vue       # NuiUpload
├── ... (52 个组件)
└── tag/index.vue          # NuiTag
```

### NUI 组件模板

```vue
<template>
  <el-button v-bind="$attrs">
    <slot />
  </el-button>
</template>

<script>
export default {
  name: "NuiButton",  // ← 必须声明，用于全局注册
};
</script>

<script setup lang="ts">
// 组件逻辑
</script>
```

---

## 三、Ctrl 层核心组件 API

### CdpTable — 增强数据表格

源码位置：`src/cdp-common-ctrl/components/CdpTable/CdpTable.vue`

全局注册名：`CdpTable`、`CdpTableList`（列表变体）

```vue
<template>
  <CdpTable
    :data="tableData"
    :columns="columns"
    :loading="loading"
    @selection-change="handleSelectionChange"
  >
    <!-- 自定义列 -->
    <template #操作="{ row }">
      <el-button @click="handleEdit(row)">编辑</el-button>
    </template>
  </CdpTable>
</template>
```

### Pagination — 全局分页组件

源码位置：`src/cdp-common-ctrl/components/Pagination/index.vue`

全局注册名：`Pagination`

```vue
<template>
  <Pagination
    v-if="total > 0"
    v-model:total="total"
    v-model:page="queryParams.pageNum"
    v-model:limit="queryParams.pageSize"
    @pagination="handleQuery"
  />
</template>
```

配合标准分页类型使用：

```typescript
// src/typings/global.d.ts
interface PageQuery {
  pageNum: number;
  pageSize: number;
}
interface PageResult<T> {
  list: T[];
  total: number;
}
```

### OrgRoleUser / OrgunitTree — 组织架构选择器

全局注册名：`OrgRoleUser`、`OrgunitTree`

```vue
<!-- 组织/角色/用户选择器 -->
<OrgRoleUser @confirm="handleSelect" />

<!-- 组织树 -->
<OrgunitTree @node-click="handleNodeClick" />
```

### Upload / Uploader — 文件上传

```vue
<!-- 简单文件上传 -->
<Upload :action="uploadUrl" :headers="uploadHeaders" @success="handleSuccess" />
```

### SvgIcon — SVG 图标

全局注册名：`SvgIcon`

```vue
<!-- 使用本地 SVG 图标（src/assets/icons/ 目录下） -->
<SvgIcon icon-class="user" />

<!-- 带颜色和尺寸 -->
<SvgIcon icon-class="setting" color="#409EFF" size="24px" />
```

SVG 图标通过 `vite-plugin-svg-icons` 自动注册，symbolId 格式为 `icon-[dir]-[name]`。

---

## 四、auto-import 自动导入配置

源码位置：`vite.config.ts`

### unplugin-auto-import（函数自动导入）

自动导入以下库的函数，无需手动 `import`：

| 库 | 自动导入示例 |
|-----|-----------|
| Vue | `ref`, `reactive`, `computed`, `watch`, `onMounted` |
| Vue Router | `useRouter`, `useRoute` |
| Pinia | `defineStore`, `storeToRefs` |
| @vueuse/core | `useStorage`, `useDark`, `useToggle` |
| Element Plus | `ElMessage`, `ElMessageBox`, `ElNotification` |

### unplugin-vue-components（组件自动注册）

自动扫描并注册以下来源的组件：

- Element Plus 组件（通过 `ElementPlusResolver`）
- Element Plus 图标（通过 `IconsResolver`）
- `src/**/components/` 目录下的自定义组件

### 类型声明文件

- `src/typings/auto-imports.d.ts` — 函数自动导入的类型声明
- `src/typings/components.d.ts` — 组件自动注册的类型声明

> 注意: 当前项目中自动生成已关闭（`dts: false`），类型文件为预先生成的静态文件。

---

## 五、全局组件注册清单

源码位置：`src/main.ts`

| 组件名 | 来源 | 注册方式 |
|--------|------|---------|
| `CdpTable` | cdp-common-ctrl | 手动 `app.component()` |
| `CdpTableList` | cdp-common-ctrl | 手动 |
| `OrgRoleUser` | cdp-common-ctrl | 手动 |
| `OrgunitTree` | cdp-common-ctrl | 手动 |
| `SvgIcon` | cdp-common-ctrl | 手动 |
| `Pagination` | cdp-common-ctrl | 手动 |
| 51 个 NUI 组件 | cdp-common-nui | `import.meta.glob` 自动 |
| msdp-common-ctrl 组件 | msdp-common-ctrl | `import.meta.glob` 自动 |
| Element Plus 图标 | @element-plus/icons-vue | 循环 `app.component()` |
| Element Plus 组件 | element-plus | `unplugin-vue-components` 自动 |

---

## 六、组件开发规范

### 标准组件模板

```vue
<script lang="ts">
export default {
  name: "MyComponent", // NUI 组件必须声明 name
};
</script>

<script setup lang="ts">
// Props
const props = defineProps<{
  title: string;
  data?: any[];
}>();

// Emits
const emit = defineEmits<{
  (e: "change", value: string): void;
  (e: "submit"): void;
}>();

// 逻辑
const count = ref(0);
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

### 规范要点

1. 必须使用 `<script setup lang="ts">` 语法
2. Props 使用 `defineProps<T>()` 泛型写法
3. Emits 使用 `defineEmits<{...}>()` 声明
4. NUI 组件必须在单独的 `<script>` 块中声明 `name`
5. 样式使用 `<style scoped lang="scss">`，SCSS 全局变量自动可用
6. 全局 SCSS 变量和 mixin 已通过 Vite `additionalData` 注入，无需手动 import

---

## 七、常见陷阱

1. **NUI 组件不生效**: 检查组件文件中是否声明了 `name` 属性，无 `name` 的组件会被跳过
2. **组件名冲突**: NUI 组件和手动注册的组件如果 `name` 相同，后注册的会覆盖先注册的
3. **auto-import 类型缺失**: `auto-imports.d.ts` 是静态文件，新增 auto-import 配置后可能需要重新生成
4. **Element Plus 图标用法**: 全局注册后直接使用组件名 `<Edit />`，不需要额外 import
5. **SVG 图标路径**: 必须放在 `src/assets/icons/` 目录下才能被 `vite-plugin-svg-icons` 识别
