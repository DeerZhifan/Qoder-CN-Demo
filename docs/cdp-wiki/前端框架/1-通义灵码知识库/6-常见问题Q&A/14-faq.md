# CDP Web 前端常见问题 Q&A

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: FAQ, 常见问题, 报错, 解决方案

---

## 开发环境

### Q1: 如何切换后端地址？

修改 `vite.config.ts` 中 `server.proxy` 的 `target`：

```typescript
proxy: {
  "/dev-api/": {
    target: "http://新的后端地址:端口",
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/dev-api/, ""),
  },
}
```

修改后需重启开发服务器。

### Q2: 为什么只能用 pnpm？

`package.json` 中配置了 `preinstall` 钩子：`"preinstall": "npx only-allow pnpm"`，使用 npm 或 yarn 会被拦截。这是为了确保团队统一使用 pnpm 管理依赖。

### Q3: 如何启用 Mock 数据？

取消注释 `vite.config.ts` 中的 `mockDevServerPlugin()` 插件，同时修改 proxy target 为 localhost。Mock 数据定义在项目 `mock/` 目录下。

### Q4: 新增环境变量如何生效？

1. 变量名必须以 `VITE_` 开头
2. 在 `.env.development` 或 `.env.production` 中声明
3. 通过 `import.meta.env.VITE_XXX` 访问
4. TypeScript 类型定义在 `src/typings/env.d.ts` 中更新

### Q5: TypeScript 报错 `globalConfig` 不存在？

检查 `src/typings/global.d.ts` 中是否有 Window 接口扩展：

```typescript
interface Window {
  globalConfig: {
    casStatus: boolean;
    sessionCookie: boolean;
    // ...
  };
}
```

### Q6: 热更新（HMR）失效？

- 检查组件是否有 `name` 属性冲突（两个组件同名会导致 HMR 异常）
- 确认文件保存后控制台无报错
- 尝试重启开发服务器

---

## 路由与权限

### Q7: 新页面不显示在菜单中？

动态路由由后端 `/system/resource/initMenu` 接口返回。需在后端资源管理中配置菜单项，前端不需要手动添加。`pagePath` 填写相对于 `src/` 的路径（不含 `.vue`），如 `cdp-common-frame/system/views/user/index`。

### Q8: 路由守卫出现死循环怎么办？

检查 `permission.ts` 中 `next({ ...to, replace: true })` 是否有 `replace: true`。缺少此参数会导致每次导航都重新触发守卫，造成无限循环。

### Q9: 如何添加不需要登录的页面？

在 `src/permission.ts` 中将路径加入 `whiteList` 数组：

```typescript
const whiteList = ["/login", "/public-page"];
```

### Q10: v-hasPerm 指令不生效？

1. 检查后端 `getUserInfoApi` 返回的 `permissions` 数组是否包含对应权限标识
2. ROOT 角色会跳过所有权限检查
3. v-hasPerm 是直接 removeChild 移除 DOM，不是隐藏，F12 检查元素确认节点是否存在

### Q11: CAS 登录后跳不回来？

检查 `public/config/config.js` 中 `casServerType` 是否正确，以及对应的 CAS 服务器地址配置。不同 `casServerType`（1-4）对应不同的登录/退出 URL 拼接逻辑。

---

## API / 网络

### Q12: POST 请求参数后端收不到？

`post()` 方法默认使用 `application/x-www-form-urlencoded` 格式，内部用 `qs.stringify` 序列化。如果需要提交 JSON 对象，必须使用 `postJson()`：

```typescript
// ❌ post() 提交对象，后端可能收不到
post("/api/user", { name: "张三", age: 20 });

// ✅ postJson() 提交 JSON
postJson("/api/user", { name: "张三", age: 20 });
```

### Q13: 接口加密后调试困难？

开发环境关闭加密：`.env.development` 中设置 `VITE_API_ENCRYPT='false'`。生产环境可通过 `VITE_IGNORE_ENCRYPT_API` 配置加密白名单。

### Q14: 文件下载返回乱码？

文件下载必须设置 `responseType: "blob"`。使用框架提供的方法：

```typescript
// 表单参数下载
getExport("/api/export", params);

// GET 方式下载
getFile("/api/download/file-id");

// POST body 下载
postFile("/api/export", params);
```

### Q15: 请求超时怎么处理？

Axios 默认超时 20 秒。大文件操作需要传 `tout` 参数：

```typescript
postJson("/api/big-data", params, 120000); // 120 秒
getExport("/api/export", params);           // 默认 999999ms
```

### Q16: 跨域报错？

- 开发环境：由 Vite Proxy 处理，检查 `vite.config.ts` 中 proxy 配置
- 生产环境：由 Nginx 处理，检查 Nginx 反向代理配置

---

## 组件

### Q17: 自定义 NUI 组件不生效？

NUI 组件通过 `import.meta.glob` 动态注册，**必须在 `<script>` 中声明 `name` 属性**：

```vue
<script>
export default { name: "NuiMyComponent" };  // ← 必须有
</script>
<script setup lang="ts">
// ...
</script>
```

没有 `name` 的组件会被跳过，不会注册到全局。

### Q18: CdpTable 分页数据不刷新？

检查 `queryParams` 的 `pageNum` 和 `pageSize` 是否正确传递给 API，以及 Pagination 组件的 `@pagination` 事件是否绑定了查询函数。

### Q19: Element Plus 组件样式丢失？

检查 `vite.config.ts` 中 `optimizeDeps.include` 是否包含该组件的样式模块：

```typescript
include: [
  "element-plus/es/components/新组件/style/css",
]
```

### Q20: SVG 图标不显示？

1. 确认图标文件在 `src/assets/icons/` 目录下
2. symbolId 格式为 `icon-[dir]-[name]`
3. 使用方式：`<SvgIcon icon-class="图标名" />`（不含 .svg 后缀）

### Q21: Upload 组件上传失败？

检查请求 Content-Type 是否为 `multipart/form-data`。使用框架提供的 `uploadFile` 方法，会自动设置正确的 Content-Type。

---

## 状态管理

### Q22: 组件外使用 Store 报错？

组件外（路由守卫、拦截器等）必须使用 Hook 模式：

```typescript
// ❌ 错误
import { useUserStore } from "@/cdp-admin/store/modules/user";
const store = useUserStore(); // Pinia 未初始化，报错

// ✅ 正确
import { useUserStoreHook } from "@/cdp-admin/store/modules/user";
const store = useUserStoreHook(); // 传入预创建的实例
```

### Q23: 刷新后状态丢失？

需要持久化的数据使用 `@vueuse/core` 的 `useStorage` 包装：

```typescript
const myData = useStorage("storage-key", defaultValue);
```

### Q24: Token 存储在哪里？

取决于 `window.globalConfig.sessionCookie`：
- `true` → Cookie（`Cookies.get/set("cdp-token")`），关闭浏览器失效
- `false`（默认）→ localStorage（`localStorage.getItem("cdp-token")`），持久化

### Q25: 主题切换后刷新恢复默认？

settings store 使用 `useStorage` 持久化主题设置到 localStorage。如果恢复默认，检查 localStorage 中是否有对应的 key（`theme`、`layout`、`themeColor`）。

---

## 样式

### Q26: 全局 SCSS 变量在组件中不可用？

确认 `vite.config.ts` 中的 `css.preprocessorOptions.scss.additionalData` 配置正确：

```typescript
additionalData: `
  @use "@/styles/variables.scss" as *;
  @use "@/styles/mixin.scss" as *;
`,
```

### Q27: 暗黑模式下某些组件样式异常？

需要在 `src/styles/theme.scss` 或 `dark.scss` 中补充该组件的暗黑模式样式覆盖。

### Q28: UnoCSS 类名不生效？

1. 确认 `main.ts` 中已导入 `"uno.css"`
2. 确认类名符合 UnoCSS 语法
3. UnoCSS 必须在其他样式之后导入

---

## 构建部署

### Q29: 构建后白屏？

检查 `vite.config.ts` 中 `base` 配置是否与部署路径匹配。当前配置为 `"./"` 相对路径。如果部署在子目录下，需要修改为对应路径。

### Q30: 构建后接口 404？

1. 检查 `.env.production` 中 `VITE_APP_BASE_API` 配置
2. 检查 Nginx 反向代理 rewrite 规则
3. 确认后端服务正常启动

### Q31: 构建包过大？

1. 使用 `rollup-plugin-visualizer` 分析包组成
2. 检查是否误引入大型库（如 lodash 全量引入，应改为按需引入）
3. 确认路由懒加载正确配置

### Q32: Docker 部署注意事项？

参考项目 `docker/cdp-dev/` 目录配置。主要关注：
- Nginx 配置（代理转发、gzip 压缩、缓存策略）
- `config.js` 运行时配置（可在容器启动时挂载覆盖）
- 环境变量通过 `.env.production` 构建时注入
