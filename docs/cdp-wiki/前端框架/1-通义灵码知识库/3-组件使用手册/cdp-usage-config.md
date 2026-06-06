# 如何管理配置

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: 配置, _changeable, 环境变量, globalConfig, .env, VITE_, 代理, 运行时配置

---

## 概述

CDP Web 配置分为三层：构建时环境变量（`.env.*` 文件）、运行时配置（`window.globalConfig`）、聚合层（`src/_changeable/`）。`_changeable` 是项目的集中配置入口，聚合了路由、API、全局常量。

## _changeable 聚合层

位于 `src/_changeable/`，每个文件职责明确：

| 文件 | 职责 | 修改场景 |
|------|------|----------|
| `router.ts` | 聚合所有模块的静态/动态路由 | 新增业务模块时 |
| `api.ts` | 聚合所有模块的 API 导出 | 新增 API 模块时 |
| `GlobalConst.ts` | 聚合全局常量 | 新增全局常量时 |

### 添加新模块路由

```typescript
// src/_changeable/router.ts
import * as newModule from "@/my-new-module/router";

export const constantRoutes = [
  ...cdpCommonFrame.constantRoutes,
  ...newModule.constantRoutes,   // 在此添加
];

export const asyncRoutes = [
  ...cdpCommonFrame.asyncRoutes,
  ...newModule.asyncRoutes,      // 在此添加
];
```

### 添加新模块 API

```typescript
// src/_changeable/api.ts
import { cdpFrameApi } from "@/cdp-common-frame";
import { cdpAdminApi } from "@/cdp-common-admin";

export default {
  ...cdpFrameApi,
  ...cdpAdminApi,
  // 新增模块 API 在各自的 all.ts 中聚合后自动包含
};
```

## 环境变量

环境变量文件位于项目根目录，Vite 根据运行模式自动加载：

| 文件 | 加载时机 |
|------|----------|
| `.env.development` | `pnpm run dev` |
| `.env.production` | `pnpm run build:prod` |

### 已有环境变量

```bash
# 应用端口
VITE_APP_PORT = 9527

# API 代理前缀（开发环境）
VITE_APP_BASE_API = '/api/'

# 接口加密开关
VITE_API_ENCRYPT = 'false'

# 加密排除接口
VITE_IGNORE_ENCRYPT_API = '/login/**,/test/**'

# 微服务管理地址
VITE_MICRO_MANAGER_IP_NACOS = 'http://172.17.1.83:8848/'
VITE_MICRO_MANAGER_IP_SKYWALKING = 'http://172.17.1.83:18080/'
VITE_MICRO_MANAGER_IP_GATEWAY = 'http://172.17.1.28:28080'
VITE_MICRO_MANAGER_IP_SENTINEL = 'http://172.17.1.83:8858/'
```

### 添加新环境变量

1. 在 `.env.development` 和 `.env.production` 中添加：

```bash
VITE_MY_CONFIG = 'value'
```

2. 在代码中使用：

```typescript
const myConfig = import.meta.env.VITE_MY_CONFIG;
```

> 注意：只有以 `VITE_` 开头的变量才会暴露给前端代码。不以 `VITE_` 开头的变量只在 `vite.config.ts` 中可用。

## 运行时配置（globalConfig）

`window.globalConfig` 是部署后可修改的运行时配置，无需重新构建。配置文件在 `public/` 目录下。

### 常用运行时配置

```typescript
const globalConfig = window.globalConfig;

// CAS 单点登录开关
globalConfig.casStatus         // true: 启用 CAS | false: 传统登录

// Token 存储方式
globalConfig.sessionCookie     // true: Cookie | false: localStorage

// 默认首页路由
globalConfig.defaultRouterName // 如 "/dashboard"
```

### 在组件中使用

```typescript
const globalConfig = window.globalConfig;

if (globalConfig.casStatus) {
  // CAS 登录逻辑
} else {
  // 传统登录逻辑
}
```

## 开发代理配置

在 `vite.config.ts` 的 `server.proxy` 中配置后端代理：

```typescript
proxy: {
  [env.VITE_APP_BASE_API]: {
    changeOrigin: true,
    target: "http://172.17.1.28:28080",  // 后端地址
    rewrite: (path) =>
      path.replace(new RegExp("^" + env.VITE_APP_BASE_API), ""),
  },
}
```

切换后端地址时，修改 `target` 并重启开发服务器。

## 注意事项

> 注意：环境变量必须以 `VITE_` 为前缀才能在前端代码中通过 `import.meta.env.VITE_xxx` 访问。

> 注意：修改 `.env.*` 文件后需要重启开发服务器才能生效。

> 注意：`import.meta.env` 中的值始终是字符串类型。判断布尔值时需用 `=== 'true'`，不能直接用 truthy 判断。

> 注意：`window.globalConfig` 是运行时配置，修改后无需重新构建，刷新页面即可生效。适合部署后需要调整的配置项（如 CAS 地址、Token 策略）。

> 注意：切换后端代理地址后，必须重启 `pnpm run dev`，Vite 代理配置不支持热更新。
