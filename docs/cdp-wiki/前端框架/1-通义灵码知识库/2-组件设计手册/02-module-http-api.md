# HTTP/API 层设计手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: Axios, 请求封装, 拦截器, Token, AES加密, API模块, request.ts, postJson

---

## 一、架构决策

CDP Web 采用**集中式 Axios 实例**管理所有 HTTP 请求，核心文件为 `src/cdp-common/utils/request.ts`。

### 双实例设计

| 实例 | 用途 | 超时 | Content-Type |
|------|------|------|-------------|
| `service` | 主业务请求 | 20000ms | application/json |
| `casService` | CAS 单点登录专用 | 20000ms | - |

两个实例共享 `baseURL = import.meta.env.VITE_APP_BASE_API`（开发 `/api/`，生产 `/`），但拦截器逻辑不同：`service` 含 AES 加解密逻辑，`casService` 仅做 Token 注入。

---

## 二、请求方法选择指南

`request.ts` 导出 16 种请求方法，根据场景选择：

| 场景 | 方法 | Content-Type | 备注 |
|------|------|-------------|------|
| JSON 数据提交 | `postJson(url, params, tout?)` | application/json | **最常用**，提交对象时必须用此方法 |
| 表单数据提交 | `post(url, params, tout?)` | x-www-form-urlencoded | 内部用 `qs.stringify` 序列化 |
| 表单提交（别名） | `postFormat(url, params, tout?)` | x-www-form-urlencoded | 与 `post()` 功能相同 |
| URL 参数 POST | `postJsonParams(url, params, tout?)` | application/json | 参数拼接到 URL，非 body |
| GET 请求 | `get(url, params?)` | - | 参数自动拼接为 query string |
| PUT 请求 | `put(url, params, tout?)` | application/json | JSON body |
| DELETE 请求 | `del(url, params, tout?)` | - | body 传参 |
| 文件上传 | `uploadFile(url, params, tout?)` | multipart/form-data | params 为 FormData |
| 上传并返回文件 | `uploadImportFile(url, params, tout?)` | multipart/form-data | responseType: blob |
| 文件下载（POST） | `getExport(url, params, tout?)` | x-www-form-urlencoded | responseType: blob，超时 999999ms |
| 文件下载（GET） | `getFile(url, headers?, tout?)` | - | responseType: blob |
| 文件下载（POST+body） | `postFile(url, params, tout?)` | multipart/form-data | responseType: blob |
| SSE 流式 | `getSSE(url, params?)` | - | responseType: stream |
| CAS GET | `casGet(url, params)` | - | 使用 casService 实例 |
| CAS POST | `casPost(url, params, tout?)` | x-www-form-urlencoded | 使用 casService 实例 |
| 工作流 PUT | `put_post(url, params, tout?)` | application/json | 实际发 POST 到 `${url}/edit` |

> 注意: `post()` 默认是 `x-www-form-urlencoded` 格式，不是 JSON！提交对象时必须用 `postJson()`，否则后端可能收不到参数。

> 注意: `tout` 参数可覆盖默认超时（20s）。大文件导出场景须传较大值，`getExport` 默认 999999ms。

---

## 三、请求拦截器详解

源码位置：`src/cdp-common/utils/request.ts:97-143`

### 处理流程

```
请求发出前 →
  ① Token 注入
     globalConfig.sessionCookie === true → Cookies.get("cdp-token")
     globalConfig.sessionCookie === false → localStorage.getItem("cdp-token")
     → 设置请求头 headers["cdp-token"] = token
     → 设置请求头 headers["Accept-Sign-Ignore"] = "BIGBIGGIRL"（忽略接口参数验签）

  ② Request ID 生成
     → Date.now().toString(36) + Math.floor(Math.random()*46656).toString(36).padStart(3,'0')
     → 17 字符唯一标识，设置到 headers["x-request-id"]

  ③ AES 加密（条件触发）
     → 跳过 FormData（文件上传场景）
     → checkEncryptApi(url): VITE_API_ENCRYPT=true 且 URL 不在白名单中
     → JSON 数据（typeof data == "string"）: 直接 aes.encrypt(data)
     → 表单数据: 包装为 { __encryptedData: aes.encrypt(JSON.stringify(data)) }
     → 设置 headers["alg"] = "AES"
```

### Ant 路径匹配器（加密白名单）

`VITE_IGNORE_ENCRYPT_API` 环境变量配置白名单，支持 Ant 风格通配符：

| 通配符 | 含义 | 示例 |
|--------|------|------|
| `?` | 匹配单个非斜杠字符 | `/api/user?` 匹配 `/api/user1` |
| `*` | 匹配任意非斜杠字符序列 | `/api/*/list` 匹配 `/api/user/list` |
| `**` | 匹配任意字符序列（含斜杠） | `/api/**` 匹配所有 `/api/` 开头的路径 |
| `{a,b}` | 匹配备选项 | `/api/{user,role}` |

---

## 四、响应拦截器详解

源码位置：`src/cdp-common/utils/request.ts:146-236`

### 处理流程

```
响应返回后 →
  ① AES 解密
     → 检查 response.headers["alg"] == "AES"
     → JSON.parse(aes.decrypt(response.data))

  ② 状态码分层处理
     code === 200    → 直接返回 respData（成功）
     ArrayBuffer     → 直接返回 response（Excel 导出）
     Blob            → 直接返回 response（文件下载）
     code === 401    → 登录过期处理
       ├── CAS 模式  → location.href = getCasLogoutUrl(true)
       └── 普通模式  → userStore.logout() → router.push("/login")
     code <= 1000    → ElMessage.error(msg) + Promise.reject（框架弹错误提示）
     code > 1000     → 返回 respData（不弹提示，由开发者自行处理）

  ③ 网络错误
     → error.response.data.code === 401 → 重新登录
     → 其他 → ElMessage.error(msg)
```

> 注意: `code > 1000` 时框架不弹错误提示也不 reject，会直接返回数据。开发者必须自行判断 code 并处理。

---

## 五、API 模块标准写法

### 文件结构

```
cdp-common-frame/模块名/
├── api/
│   ├── index.ts      # API 方法定义
│   └── types.ts      # TypeScript 类型定义
└── views/            # 页面组件
```

### types.ts 示例

源码参考：`src/cdp-common-frame/common/api/login/types.ts`

```typescript
// 登录请求参数
export interface LoginData {
  username: string;
  password: string;
  loginMode?: string;
}

// 用户信息
export interface UserInfo {
  userId?: string;
  username?: string;
  nickname?: string;
  avatar?: string;
  roles: string[];
  permission: string[];
}
```

### index.ts 示例

源码参考：`src/cdp-common-frame/common/api/login/index.ts`

```typescript
import { post, postJson, get } from "@/cdp-common/utils/request";
import { LoginData } from "./types";

// 登录
export function loginApi(data: LoginData) {
  return post("/auth/login", data);
}

// 获取用户信息
export function getUserInfoApi() {
  return get("/system/user/me");
}

// 获取动态路由
export function listRoutes() {
  return get("/system/resource/initMenu");
}

// 退出登录
export function logoutApi() {
  return post("/auth/logout");
}
```

### API 聚合链路

```
各模块 api/index.ts
  → 模块级 all.ts 或 index.ts 导出
  → _changeable/api.ts 合并
  → main.ts: app.config.globalProperties.$api = api
  → 组件中: this.$api.模块名.方法名() 或直接 import 使用
```

---

## 六、Token 管理机制

源码位置：`src/cdp-common/utils/auth.ts`

### 双模式存储

| 模式 | 触发条件 | 存储位置 | Key | 特点 |
|------|---------|---------|-----|------|
| Cookie | `globalConfig.sessionCookie === true` | Cookie | `cdp-token` | `sameSite: strict` 防 CSRF，关闭浏览器失效 |
| localStorage | `globalConfig.sessionCookie === false`（默认） | localStorage | `cdp-token` | 持久化，兼容 iframe 嵌入 |

### 核心函数

```typescript
// Cookie 模式
getToken()          // Cookies.get("cdp-token")
setToken(token)     // Cookies.set("cdp-token", token, { sameSite: "strict" })
removeToken()       // Cookies.remove("cdp-token")

// localStorage 模式
getLocalStorageToken()     // localStorage.getItem("cdp-token")
setLocalStorageToken(token) // localStorage.setItem("cdp-token", token)

// URL Token 注入（免登录场景）
getTokenFromUrl()   // 从 URL 参数中提取 cdp-token
removeUrlParam()    // 清理 URL 中的 cdp-token 参数（防止退出失败）
```

### URL Token 注入流程

```
访问 http://xxx/#/dashboard?cdp-token=abc123
  → permission.ts 检测到 cdp-token
  → 根据 globalConfig.sessionCookie 存储到 Cookie 或 localStorage
  → removeUrlParam() 清理 URL
  → history.replaceState 避免页面刷新
  → next({ path: to.path, query: 去除 cdp-token 后的 query })
```

---

## 七、辅助导出函数

```typescript
// 获取 baseURL（末尾带 /）
getBaseUrl(): string

// 拼接完整 API URL
getRealUrl(api: string): string

// 获取 Token 请求头对象（用于非 Axios 场景，如 WebSocket）
getTokenHeader(): { "Accept-Sign-Ignore": string; "cdp-token": string }

// 创建绑定 Router 的 Axios 实例（注入 X-Resource-Id 头）
createAxiosInstance(router: Router): AxiosInstance
```

---

## 八、配置参考

| 环境变量 | 开发环境 | 生产环境 | 说明 |
|---------|---------|---------|------|
| `VITE_APP_BASE_API` | `/api/` | `/` | Axios baseURL，开发环境配合 Vite Proxy |
| `VITE_API_ENCRYPT` | `false` | `false`/`true` | 接口加密总开关 |
| `VITE_IGNORE_ENCRYPT_API` | - | 逗号分隔 Ant 路径 | 加密白名单（匹配的 API 不加密） |

---

## 九、常见陷阱

1. **`post()` vs `postJson()`**: `post()` 是 `x-www-form-urlencoded`，提交 JSON 对象必须用 `postJson()`
2. **超时处理**: 默认 20s，大文件导出/上传须传 `tout` 参数（`getExport` 默认 999999ms）
3. **code > 1000**: 框架不弹错误、不 reject，开发者必须自行处理
4. **FormData 不加密**: 请求拦截器自动跳过 `FormData` 类型数据的 AES 加密
5. **401 双路径**: CAS 模式跳 `getCasLogoutUrl()`，普通模式跳 `/login`，不要混淆
6. **Token 双模式**: 取 Token 时必须根据 `globalConfig.sessionCookie` 选择 `getToken()` 或 `getLocalStorageToken()`
