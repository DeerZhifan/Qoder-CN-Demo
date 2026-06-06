# CDP Web 前端框架架构总览

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: 架构总览, 模块依赖, 目录结构, 技术栈, 请求链路, 启动流程

---

## 一、技术栈清单

| 分类 | 技术 | 版本 |
|------|------|------|
| 运行时框架 | Vue 3 + TypeScript | 3.4 + 5.4 |
| 路由 | Vue Router（Hash 模式） | 4.2 |
| 状态管理 | Pinia（Composition API 风格） | 2.1 |
| 构建工具 | Vite + Terser + Rollup | 5.x |
| UI 组件库 | Element Plus | 2.8 |
| 原子化 CSS | UnoCSS | 0.58 |
| 预处理器 | SCSS | - |
| HTTP 请求 | Axios | 1.12 |
| 加密 | crypto-js（AES）+ jsencrypt（RSA） | 4.2 / 3.3 |
| 工作流引擎 | otworkflow + sockjs-client + stompjs | - |
| 富文本编辑器 | @wangeditor/editor | - |
| 代码编辑器 | codemirror + codemirror-editor-vue3 | - |
| 图表 | ECharts | 5.4 |
| 文件处理 | xlsx + file-saver + @vue-office/* | - |
| 包管理器 | pnpm（强制，preinstall 钩子限制） | - |

---

## 二、模块依赖关系

```
_changeable（聚合层 — 路由/API/常量的汇聚点）
  │
  ├── cdp-common              基础工具层
  │   └── utils/              request · auth · aes · rsa · cas · validate · date · permission
  │
  ├── cdp-common-nui          UI 基础组件层（52 个 Element Plus 二次封装组件）
  │   └── packages/           button · dialog · drawer · form · input · select · table · tabs ...
  │
  ├── cdp-common-ctrl         业务控件层（20+ 组件 + 权限指令）
  │   ├── components/         CdpTable · Pagination · Upload · OrgRoleUser · OrgunitTree · Cron ...
  │   └── directive/          v-hasPerm · v-hasRole
  │
  ├── cdp-common-admin        管理端层（API + 15+ 组件）
  │   ├── api/                upload
  │   └── components/         UserSelect · HomeToDoList · Tinymce · SvgChartEdit ...
  │
  ├── cdp-common-frame        业务功能层（15 个业务模块）
  │   ├── common/             登录 · 仪表盘 · 错误页
  │   ├── system/             用户 · 角色 · 组织 · 字典 · 资源 · 数据权限
  │   ├── form/               自定义表单设计器
  │   ├── job/                定时任务调度
  │   ├── export/             数据导出
  │   ├── report/             报表（ureport）
  │   ├── gen/                代码生成器
  │   ├── fulltext/           全文检索
  │   ├── api-manage/         API 管理
  │   └── ...                 org-user-sync · sms-platform · system-logs · demo
  │
  ├── cdp-admin               应用壳层
  │   ├── store/modules/      user · permission · settings · app · tagsView（5 个 Pinia Store）
  │   └── layout/             NavBar · Sidebar · TagsView · AppMain · Settings
  │
  └── workflow                工作流集成层
      ├── api/                30+ 工作流 API
      ├── views/              50+ 工作流视图
      ├── router/             独立路由定义
      └── common-lib/         msdp-common · otworkflow · otchart
```

> 注意: 模块之间禁止循环依赖。下层模块不可引用上层模块。`_changeable` 是唯一允许跨层聚合的目录。

---

## 三、核心目录结构

```
src/
├── _changeable/              部署级定制点
│   ├── router.ts             聚合各模块路由（constantRoutes + asyncRoutes）
│   ├── api.ts                聚合各模块 API → 挂载为全局 $api
│   └── GlobalConst.ts        聚合全局常量
│
├── cdp-admin/
│   ├── store/                Pinia Store（Composition API + useStorage 持久化）
│   └── layout/               主布局（侧边栏/导航栏/标签页/内容区）
│
├── cdp-common/utils/         核心工具集
│   ├── request.ts            Axios 双实例 + 请求/响应拦截器
│   ├── auth.ts               Token 管理（Cookie/localStorage 双模式）
│   ├── aes.ts                AES ECB 加解密
│   ├── rsa.ts                RSA 1024/2048 公钥加密
│   ├── cas.ts                CAS 单点登录 URL 生成
│   ├── validate.ts           自定义校验器
│   ├── permission.ts         权限检查工具函数
│   └── index.ts              通用工具（parseTime/debounce/deepClone 等）
│
├── cdp-common-nui/packages/  52 个 Element Plus 封装组件（import.meta.glob 自动注册）
├── cdp-common-ctrl/          业务控件 + 权限指令
├── cdp-common-admin/         管理端 API + 组件
├── cdp-common-frame/         15 个业务模块（各含 api/ + views/）
├── workflow/                 工作流引擎集成
│
├── styles/                   全局样式
│   ├── variables.scss        SCSS 变量 + CSS 自定义属性 + :export 导出到 JS
│   ├── mixin.scss            12+ 通用 mixin
│   ├── theme.scss            多主题定义（light/dark/blue）
│   └── index.scss            入口聚合
│
├── typings/                  TypeScript 全局类型
│   ├── global.d.ts           PageQuery · PageResult<T> · TagView · AppSettings
│   ├── router.d.ts           RouteMeta 扩展
│   └── env.d.ts              Vite 环境变量类型
│
├── assets/icons/             100+ SVG 图标（vite-plugin-svg-icons 自动注册）
├── App.vue                   根组件（el-config-provider 包装）
├── main.ts                   应用入口（插件/组件/Store/指令注册）
├── permission.ts             路由守卫（Token 检查 + 动态路由加载）
└── settings.ts               UI 默认设置（主题/布局/语言/水印）
```

---

## 四、请求链路全流程

```
用户操作
  → Vue 组件调用 API 函数
  → cdp-common-frame/xxx/api/index.ts
  → request.ts 请求拦截器
      ├── Token 注入（cdp-token 请求头）
      ├── Request ID 生成（x-request-id: 13位时间戳base36 + 3位随机数）
      └── AES 加密（VITE_API_ENCRYPT=true 时，Ant 路径匹配白名单）
  → Vite Proxy（开发 /api/ → 172.17.1.28:28080）/ Nginx（生产）
  → 后端服务处理
  → request.ts 响应拦截器
      ├── AES 解密（响应头 alg=AES 时）
      ├── 状态码处理
      │   ├── code=200  → 返回 data
      │   ├── code=401  → CAS 跳退出 / 普通跳登录页
      │   ├── code<=1000 → ElMessage 弹错误提示
      │   └── code>1000 → 透传给开发者自行处理
      └── 二进制响应（ArrayBuffer/Blob）→ 直接透传
  → 组件接收数据更新视图
```

---

## 五、应用启动流程

```
① index.html
   └── <script src="config/config.js"> 加载运行时配置 → window.globalConfig

② main.ts 初始化（按顺序执行）
   ├── createApp(App)
   ├── Element Plus 图标全局注册（所有 @element-plus/icons-vue）
   ├── 全局属性挂载: $request · $http · $api · $message · $confirm · GLOBAL_CONST
   ├── NUI 组件注册: import.meta.glob("@/cdp-common-nui/packages/**/*.vue")
   ├── Ctrl 组件注册: CdpTable · CdpTableList · OrgRoleUser · OrgunitTree · SvgIcon · Pagination
   ├── Uploader 插件注册
   ├── setupDirective(app)      自定义指令（v-hasPerm · v-hasRole）
   ├── setupStore(app)          Pinia Store 初始化
   ├── otworkflow 工作流插件注册
   ├── form 自定义表单插件注册
   ├── msdp-common-ctrl 组件注册
   └── app.use(router).mount("#app")

③ permission.ts 路由守卫（每次导航触发）
   ├── URL cdp-token 检测 → 存储 Token + 清理 URL
   ├── CAS ticket 检测 → 放行到 caslogin
   ├── Token 存在?
   │   ├── 已有路由 → 直接放行（未匹配跳 404）
   │   └── 无路由 → getUserInfo() + generateRoutes() + addRoute()
   └── Token 不存在?
       ├── CAS 模式 → 跳转 getCasLoginUrl()
       └── 普通模式 → 白名单放行 / 跳转 /login
```

---

## 六、运行时配置机制

### 三层配置体系

| 层级 | 文件 | 特点 | 关键配置项 |
|------|------|------|-----------|
| 部署级 | `public/config/config.js` | 不经 Vite 打包，部署时可直接修改 | `casStatus`（CAS 开关）、`sessionCookie`（Token 存储模式）、`sysName`（系统名称）、`defaultRouterPath` |
| 构建级 | `.env.development` / `.env.production` | Vite 构建时注入，需重新构建生效 | `VITE_APP_BASE_API`（API 前缀）、`VITE_APP_PORT`（端口）、`VITE_API_ENCRYPT`（加密开关） |
| 应用级 | `src/settings.ts` | UI 默认值，可被用户偏好覆盖 | `title`、`layout`（left/top/mix）、`theme`（light/dark/blue）、`language`、`watermark` |

### globalConfig 关键配置速查

```javascript
// public/config/config.js
window.globalConfig = {
  casStatus: false,        // CAS 单点登录开关（true=CAS, false=普通登录）
  casServerType: 1,        // CAS 类型: 1=框架CAS, 2=新版集团CAS, 3=旧版, 4=CDP代理
  sessionCookie: false,    // Token 存储: true=Cookie(关浏览器失效), false=localStorage
  sysName: "通用技术研发平台",
  defaultRouterName: "",   // 默认路由名
  defaultRouterPath: "",   // 默认路由路径
};
```

---

## 七、关键设计决策摘要

| 决策 | 选择 | 理由 |
|------|------|------|
| 路由模式 | Hash (`createWebHashHistory`) | 兼容性强，无需服务器配置 |
| Store 风格 | Composition API `defineStore` | 与 Vue 3 组合式 API 统一，TypeScript 友好 |
| 持久化 | `@vueuse/core` 的 `useStorage` | 轻量，直接在 Store 内声明，无需额外插件 |
| 组件注册 | `import.meta.glob` 动态注册 | 无需手动维护注册列表，新增组件自动生效 |
| 加密 | AES ECB + 可选开关 | 开发环境关闭便于调试，生产环境按需开启 |
| Token 双模式 | Cookie / localStorage | Cookie 模式更安全（sameSite=strict），localStorage 兼容 iframe 嵌入场景 |
| 组件外 Store 访问 | `useXxxStoreHook()` | 传入预创建的 store 实例，避免在 Pinia 初始化前调用 |
