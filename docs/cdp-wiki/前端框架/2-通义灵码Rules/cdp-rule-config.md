---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/10-module-changeable-config.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-config.md
---

# CDP 配置管理规则

## 适用场景

- 新增或修改环境变量（`.env.development` / `.env.production`）
- 配置运行时参数（`window.globalConfig`）
- 在 `_changeable` 聚合层注册新模块的路由、API、常量
- 调整应用默认设置（`src/settings.ts`）
- 切换后端地址、CAS 登录模式、Token 存储策略

## 前置依赖

- **环境变量文件**: `.env.development`（开发）、`.env.production`（生产）
- **运行时配置**: `public/config/config.js`（不经过 Vite 打包）
- **聚合层**: `src/_changeable/`（router.ts、api.ts、GlobalConst.ts）
- **应用设置**: `src/settings.ts`（UI 默认配置）
- **类型声明**: `src/typings/env.d.ts`（环境变量类型）、`src/typings/global.d.ts`（Window 扩展）

## 配置要点

### 1. 三层配置体系

| 层级 | 文件 | 修改后是否需要重新构建 | 适用场景 |
|------|------|----------------------|----------|
| 运行时配置 | `public/config/config.js` | 否，刷新即生效 | CAS 开关、系统名称、Token 策略 |
| 构建级配置 | `.env.development` / `.env.production` | 需重启 dev server | 端口、API 前缀、加密开关 |
| 应用级配置 | `src/settings.ts` | 需重新构建 | 布局、主题、语言、水印 |

### 2. 环境变量（VITE_ 前缀）

自定义环境变量**必须**以 `VITE_` 开头才能在前端代码中访问：

```bash
# .env.development
VITE_APP_PORT = 9527
VITE_APP_BASE_API = '/api/'
VITE_API_ENCRYPT = 'false'
VITE_IGNORE_ENCRYPT_API = '/login/**,/test/**'
VITE_MICRO_MANAGER_IP_GATEWAY = 'http://172.17.1.28:28080'
```

在代码中通过 `import.meta.env` 访问：

```typescript
const baseApi = import.meta.env.VITE_APP_BASE_API;
const port = import.meta.env.VITE_APP_PORT;
```

新增环境变量时，需同步更新类型声明文件 `src/typings/env.d.ts`：

```typescript
interface ImportMetaEnv {
  VITE_APP_PORT: string;
  VITE_APP_BASE_API: string;
  VITE_MY_NEW_VAR: string;  // 新增变量的类型声明
}
```

### 3. 运行时配置（globalConfig）

`public/config/config.js` 通过 `<script>` 标签加载，部署后可直接修改：

```javascript
window.globalConfig = {
  casStatus: false,            // CAS 单点登录开关
  casServerType: 1,            // CAS 服务类型 (1-4)
  sessionCookie: false,        // Token 存储: true=Cookie, false=localStorage
  sysName: "通用技术研发平台",  // 系统显示名称
  defaultRouterName: "",       // 默认路由名
  defaultRouterPath: "",       // 默认路由路径
};
```

在代码中使用：

```typescript
const globalConfig = window.globalConfig;
if (globalConfig.casStatus) {
  // CAS 登录逻辑
}
```

### 4. _changeable 聚合层

#### 注册新模块路由

```typescript
// src/_changeable/router.ts
import * as newModule from "@/my-new-module/router";

export const constantRoutes = [
  ...cdpRoutes,
  ...newModule.constantRoutes,   // 新增
];

export const asyncRoutes = [
  ...cdpAsyncRoutes,
  ...newModule.asyncRoutes,      // 新增
];
```

#### 注册新模块 API

```typescript
// src/_changeable/api.ts
import { newModuleApi } from "@/my-new-module";

export default {
  ...cdpFrameApi,
  ...cdpAdminApi,
  ...newModuleApi,  // 新增
};
```

#### 注册全局常量

```typescript
// src/_changeable/GlobalConst.ts
import newConst from "@/my-new-module/const";

export default {
  ...commonConst,
  ...flowConst,
  ...newConst,  // 新增
};
```

### 5. 应用设置（settings.ts）

```typescript
// src/settings.ts
const defaultSettings: AppSettings = {
  title: "通用技术研发平台",
  layout: "left",            // 布局: left / top / mix
  theme: "light",            // 主题: light / dark / blue
  size: "default",           // UI 尺寸
  language: "zh-cn",         // 语言
  themeColor: "#409EFF",     // 主题色
  tagsView: true,            // 标签页栏
  watermark: false,          // 水印
};
```

用户偏好通过 Pinia settings store 持久化到 localStorage，优先级高于默认值。

## 代码模式

### 推荐写法

- 环境变量命名统一使用 `VITE_` 前缀 + 大写蛇形命名（如 `VITE_APP_BASE_API`）
- 部署级差异配置放在 `public/config/config.js`（无需重新构建）
- 开发/生产环境差异放在 `.env.*` 文件中
- 新增模块时在 `_changeable/` 中统一注册路由、API、常量
- `import.meta.env` 中的值始终当作字符串处理，布尔判断用 `=== 'true'`
- 新增环境变量时同步更新 `src/typings/env.d.ts` 类型声明

### 禁止事项

- **禁止** 定义不以 `VITE_` 开头的前端可访问环境变量（Vite 不会暴露）
- **禁止** 在 `public/config/config.js` 中放置敏感信息（该文件不打包，可直接访问）
- **禁止** 直接修改 `node_modules` 中的配置来覆盖框架行为
- **禁止** 对 `import.meta.env` 的值用 truthy 判断代替严格字符串比较
- **禁止** 在 `_changeable/` 之外的位置创建 Router 实例或全局挂载 API
- **禁止** 修改 `.env.*` 文件后不重启开发服务器（变更不会热更新）
- **禁止** 跳过 `_changeable/` 聚合层直接在 `main.ts` 中注册模块路由或 API
