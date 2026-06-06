# 配置层设计手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: _changeable, 配置, globalConfig, 路由聚合, API聚合, 环境变量, config.js

---

## 一、_changeable 目录职责

源码位置：`src/_changeable/`

`_changeable` 是整个前端项目的**聚合层**，将各模块的路由、API、常量汇聚到一处，作为应用的配置入口点。

| 文件 | 职责 | 引用位置 |
|------|------|---------|
| `router.ts` | 聚合所有模块路由，创建 Router 实例 | `main.ts`、`permission.ts` |
| `api.ts` | 聚合所有模块 API，挂载为全局 `$api` | `main.ts` |
| `GlobalConst.ts` | 聚合全局常量 | `main.ts`（挂载为 `GLOBAL_CONST`） |

### router.ts

```typescript
import { createRouter, createWebHashHistory } from "vue-router";

// 聚合各模块的静态路由
import { constantRoutes as cdpRoutes, asyncRoutes as cdpAsyncRoutes }
  from "@/cdp-common-frame/router";
import { constantRoutes as demoRoutes } from "@/cdp-demo/router";
import workflowRouter from "@/workflow/router";
import otworkflow from "otworkflow";

export const constantRoutes = [
  ...cdpRoutes,
  ...demoRoutes,
  ...workflowRouter.constantRoutes,
  ...otworkflow.workflowApiRoutes,
];

export const asyncRoutes = [...cdpAsyncRoutes];

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
});

export function resetRouter() { /* 重置路由 */ }
export default router;
```

### api.ts

```typescript
import { cdpFrameApi } from "@/cdp-common-frame";       // → system/api/all
import { cdpAdminApi } from "@/cdp-common-admin";        // → api/all

export default {
  ...cdpFrameApi,
  ...cdpAdminApi,
};
```

### GlobalConst.ts

```typescript
import commonConst from "@/cdp-common/models/const";
import flowConst from "@/cdp-common/models/const_workflow";

export default {
  ...commonConst,
  ...flowConst,
};
```

---

## 二、三层配置体系

### 第一层：运行时配置（config.js）

文件位置：`public/config/config.js`

```javascript
window.globalConfig = {
  // 认证配置
  casStatus: false,           // CAS 总开关
  casServerType: 1,           // CAS 类型 (1-4)
  sessionCookie: false,       // Token 存储: true=Cookie, false=localStorage

  // 应用配置
  sysName: "通用技术研发平台",  // 系统名称
  defaultRouterName: "",      // 默认路由名
  defaultRouterPath: "",      // 默认路由路径
};
```

**特点:**
- 通过 `<script>` 标签在 `index.html` 中加载
- **不经过 Vite 打包**，部署后可直接修改
- 适用于部署级差异配置（CAS 地址、系统名称等）

**使用方式:**

```typescript
const globalConfig = window.globalConfig;
if (globalConfig.casStatus) {
  // CAS 登录逻辑
}
```

### 第二层：构建级配置（.env 文件）

| 文件 | 环境 | 说明 |
|------|------|------|
| `.env.development` | 开发 | `pnpm dev` 时加载 |
| `.env.production` | 生产 | `pnpm build:prod` 时加载 |

**开发环境 (.env.development):**

```bash
NODE_ENV='development'
VITE_APP_PORT=9527                     # 开发服务器端口
VITE_APP_BASE_API='/api/'              # API 前缀（配合 Vite Proxy）
VITE_API_ENCRYPT='false'              # 关闭接口加密
VITE_MICRO_MANAGER_IP_NACOS='http://172.17.1.83:8848/'
VITE_MICRO_MANAGER_IP_GATEWAY='http://172.17.1.28:28080'
```

**生产环境 (.env.production):**

```bash
NODE_ENV='production'
VITE_APP_BASE_API='/'                  # 生产环境直接请求
VITE_API_ENCRYPT='false'              # 根据需要开启
```

**使用方式:**

```typescript
// 通过 import.meta.env 访问
const baseApi = import.meta.env.VITE_APP_BASE_API;
```

> 注意: 自定义环境变量必须以 `VITE_` 开头才能在前端代码中访问。

### 第三层：应用级配置（settings.ts）

源码位置：`src/settings.ts`

```typescript
const defaultSettings: AppSettings = {
  title: "通用技术研发平台",
  preTitle: "二次登录验证",
  version: "v1.0.0",
  showSettings: true,       // 显示设置面板
  menuIcon: true,           // 菜单图标
  tagsView: true,           // 标签页栏
  breadcrumbs: false,        // 面包屑
  fixedHeader: false,        // 固定顶栏
  sidebarLogo: true,         // 侧边栏 Logo
  layout: "left",            // 布局: left / top / mix
  theme: "light",            // 主题: light / dark / blue
  size: "default",           // UI 尺寸
  language: "zh-cn",         // 语言
  themeColor: "#409EFF",     // 主题色
  watermark: false,          // 水印
};
```

**特点:**
- 参与 Vite 构建，修改需重新打包
- 可被用户偏好覆盖（通过 settings store + useStorage 持久化到 localStorage）

---

## 三、Vite Proxy 配置

源码位置：`vite.config.ts`

```typescript
server: {
  host: "0.0.0.0",
  port: Number(env.VITE_APP_PORT),
  proxy: {
    "/dev-api/": {
      target: "http://172.17.1.28:28080",  // 后端服务地址
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/dev-api/, ""),
    },
    "/ureport": {
      target: "http://localhost:28080",     // 报表服务
      changeOrigin: true,
    },
  },
}
```

> 注意: 开发环境请求通过 Vite Proxy 转发到后端，修改后端地址需修改 proxy target。

---

## 四、新项目定制指南

### 基础定制

1. **修改系统名称**: `public/config/config.js` → `sysName`
2. **修改默认首页**: `config.js` → `defaultRouterPath`
3. **切换登录模式**: `config.js` → `casStatus: true/false`
4. **修改 UI 默认**: `src/settings.ts` → layout/theme/language

### 模块增减

1. **增加模块路由**: `_changeable/router.ts` 中 import 并合并到 `constantRoutes`
2. **增加模块 API**: `_changeable/api.ts` 中 import 并合并到 default export
3. **移除工作流**: 注释 `main.ts` 中 otworkflow 相关代码 + `_changeable/router.ts` 中工作流路由

### 后端地址切换

- **开发环境**: `vite.config.ts` → `proxy.target`
- **生产环境**: Nginx 反向代理配置（或 `config.js` 中配置 API 地址）

---

## 五、常见陷阱

1. **config.js 不打包**: 修改 `public/config/config.js` 不需要重新构建，直接部署生效
2. **VITE_ 前缀**: 环境变量必须以 `VITE_` 开头才能在前端代码中通过 `import.meta.env` 访问
3. **TypeScript 类型**: 环境变量类型定义在 `src/typings/env.d.ts`，新增变量需同步更新
4. **globalConfig 类型**: 定义在 `src/typings/global.d.ts` 的 `Window` 接口扩展中
5. **优先级**: 用户 localStorage 偏好 > settings.ts 默认值 > config.js 全局配置
