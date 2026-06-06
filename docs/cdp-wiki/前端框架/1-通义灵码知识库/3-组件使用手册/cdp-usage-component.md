# 如何使用和创建组件

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: 组件, NUI, CdpTable, CdpSelect, Pagination, OrgRoleUser, 组件注册, 全局组件, cdp-common-ctrl, cdp-common-nui

---

## 概述

CDP Web 采用三层组件架构：NUI 基础组件（`cdp-common-nui`，Element Plus 二次封装）→ 公共控件（`cdp-common-ctrl`，业务级复用组件）→ 业务组件（各模块内部）。组件通过 Vite 的 `import.meta.glob` 自动注册或在 `main.ts` 中手动注册。

## NUI 组件（cdp-common-nui）

NUI 是对 Element Plus 的统一封装，共 51 个组件（50 个目录，其中 checkbox-group 包含 2 个独立组件），位于 `src/cdp-common-nui/packages/`。

### 自动注册机制

NUI 组件在 `main.ts` 中通过 glob 动态导入并自动注册：

```typescript
// src/main.ts
const componentsContext = import.meta.glob("@/cdp-common-nui/packages/**/*.vue");
Object.keys(componentsContext).forEach((key) => {
  const componentConfig = componentsContext[key]();
  componentConfig.then((component: any) => {
    const componentName = component.default.name;
    if (!componentName) return;
    app.component(componentName, component.default || component);
  });
});
```

### 使用方式

NUI 组件已全局注册，在模板中直接使用，无需 import：

```vue
<template>
  <!-- NUI 组件以 n- 为前缀 -->
  <n-button type="primary">按钮</n-button>
  <n-dialog v-model="visible" title="弹窗">内容</n-dialog>
  <n-form :model="form" :rules="rules">
    <n-form-item label="名称" prop="name">
      <n-input v-model="form.name" />
    </n-form-item>
  </n-form>
</template>
```

## 公共控件（cdp-common-ctrl）

位于 `src/cdp-common-ctrl/components/`，共 20 个业务级复用组件：

| 组件 | 用途 |
|------|------|
| `CdpTable` | 表格组件，自动计算高度，支持全屏模式 |
| `CdpSelect` | 下拉选择，支持远程搜索和字典数据 |
| `Pagination` | 分页栏 |
| `OrgRoleUser` | 组织/角色/用户选择器 |
| `OrgunitTree` | 组织树 |
| `Upload` / `Uploader` | 文件上传 |
| `WangEditor` | 富文本编辑器 |
| `Dictionary` | 字典数据展示 |
| `SvgIcon` | SVG 图标 |
| `CodeMirror` | 代码编辑器 |
| `Cron` / `Schedule` | Cron 表达式编辑 |
| `IconSelect` | 图标选择器 |
| `Breadcrumb` | 面包屑 |

### CdpTable 使用示例

```vue
<template>
  <CdpTable ref="cdpTable" @getTableData="getTableData">
    <!-- 查询栏 -->
    <template #queryBar>
      <el-form-item label="名称">
        <el-input v-model="queryParams.name" />
      </el-form-item>
    </template>

    <!-- 操作栏 -->
    <template #operaBar>
      <el-button v-hasPerm="['sys:user:add']" type="primary" @click="handleAdd">
        新增
      </el-button>
    </template>

    <!-- 表格列 -->
    <template #default>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </template>
  </CdpTable>
</template>
```

### Pagination 使用示例

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

## 创建自定义 NUI 组件

在 `src/cdp-common-nui/packages/` 下新建组件目录：

```
src/cdp-common-nui/packages/my-component/
└── src/
    └── my-component.vue
```

```vue
<!-- src/cdp-common-nui/packages/my-component/src/my-component.vue -->
<script lang="ts">
export default {
  name: "NMyComponent",  // 必须定义 name，自动注册依赖此属性
};
</script>

<script setup lang="ts">
defineProps<{
  title: string;
}>();
</script>

<template>
  <div class="n-my-component">{{ title }}</div>
</template>
```

> 注意：NUI 组件**必须**在 `<script>` 中通过 `export default { name: "..." }` 定义组件名，自动注册逻辑依赖 `component.default.name` 属性。没有 name 的组件不会被注册。

## 注意事项

> 注意：`CdpTable`、`CdpTableList`、`OrgRoleUser`、`OrgunitTree`、`SvgIcon`、`Pagination` 已在 `main.ts` 中全局注册，直接在模板中使用即可。

> 注意：Element Plus 组件通过 `unplugin-vue-components` 自动按需导入，无需手动 import。

> 注意：Element Plus 图标组件（如 `<Edit />`、`<Delete />`）已在 `main.ts` 中全局注册。

> 注意：新增 NUI 组件后，需要重启开发服务器（`pnpm run dev`），因为 `import.meta.glob` 在开发模式下仅在启动时扫描。
