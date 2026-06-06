# CDP Web 前端框架知识库

> 通用技术研发平台 (CDP Web) 前端框架知识文档，用于 AI RAG 检索和开发者参考。

---

## 文档索引

### 架构与规范

| 编号 | 文档 | 说明 |
|------|------|------|
| D1 | [框架架构总览](1-架构总览/01-architecture-overview.md) | 技术栈、模块依赖、请求链路、启动流程、配置体系 |
| D12 | [编码规范](4-代码规范文档/12-coding-standards.md) | 命名、组件编写、分层、TypeScript、Git 提交 |

### 核心模块设计手册

| 编号 | 文档 | 说明 |
|------|------|------|
| D2 | [HTTP/API 层](2-组件设计手册/02-module-http-api.md) | Axios 封装、拦截器、Token 管理、API 模块标准写法 |
| D3 | [状态管理](2-组件设计手册/03-module-state-management.md) | Pinia Store、5 个核心 Store、持久化策略、Hook 模式 |
| D4 | [路由与权限](2-组件设计手册/04-module-routing-permission.md) | 路由守卫、动态路由、CAS SSO、v-hasPerm 权限指令 |
| D5 | [组件体系](2-组件设计手册/05-module-component-architecture.md) | 三层组件架构、NUI 注册机制、核心组件 API |
| D6 | [样式系统](2-组件设计手册/06-module-style-system.md) | SCSS 变量/mixin、主题切换、UnoCSS |
| D7 | [加密模块](2-组件设计手册/07-module-encryption.md) | AES/RSA 加密、接口加密链路 |
| D8 | [工作流集成](2-组件设计手册/08-module-workflow.md) | OT 工作流引擎、API、路由集成 |
| D9 | [业务模块开发](2-组件设计手册/09-module-business-frame.md) | 15 个业务模块、新增模块步骤 |
| D10 | [配置层](2-组件设计手册/10-module-changeable-config.md) | _changeable 聚合层、运行时配置、环境变量 |
| D11 | [构建与工具链](2-组件设计手册/11-module-build-tooling.md) | Vite 插件、ESLint/Prettier/Stylelint/Commitlint |

### 组件使用手册

| 编号 | 文档 | 说明 |
|------|------|------|
| U1 | [HTTP/API 层](3-组件使用手册/cdp-usage-http-api.md) | 新增 API 模块、请求方法选择、文件上传下载 |
| U2 | [状态管理](3-组件使用手册/cdp-usage-state-management.md) | 新增 Pinia Store、useStorage 持久化、Hook 模式 |
| U3 | [路由与权限](3-组件使用手册/cdp-usage-routing-permission.md) | 添加路由、v-hasPerm 指令、CAS/白名单配置 |
| U4 | [组件使用](3-组件使用手册/cdp-usage-component.md) | NUI/公共控件使用、CdpTable、创建自定义组件 |
| U5 | [样式系统](3-组件使用手册/cdp-usage-style.md) | SCSS 变量/mixin、UnoCSS、主题切换 |
| U6 | [加密功能](3-组件使用手册/cdp-usage-encryption.md) | RSA 密码加密、AES 接口加密配置 |
| U7 | [工作流集成](3-组件使用手册/cdp-usage-workflow.md) | 工作流路由、API 调用、WebSocket 通知 |
| U8 | [业务模块开发](3-组件使用手册/cdp-usage-business-module.md) | 新增模块完整步骤（目录→类型→API→页面→路由→聚合） |
| U9 | [配置管理](3-组件使用手册/cdp-usage-config.md) | _changeable 聚合、环境变量、globalConfig、代理 |
| U10 | [构建与工具链](3-组件使用手册/cdp-usage-build.md) | Vite 配置、pnpm 命令、ESLint/Prettier/Commitlint |

### 实用参考

| 编号 | 文档 | 说明 |
|------|------|------|
| D13 | [代码片段库](5-代码片段库/) | 19 个场景的标准写法（API、CRUD、权限、上传下载等） |
| D14 | [常见问题 Q&A](6-常见问题Q&A/14-faq.md) | 32 条高频问题（开发环境/路由/API/组件/状态/样式/部署） |
| D15 | [AI 项目级指令](7-项目级指令文件/lingma-custom-instructions.md) | 通义灵码 / Claude Code 的框架约束配置 |

### 代码片段目录

| 文件 | 场景 |
|------|------|
| [api-module-standard.ts](5-代码片段库/api-module-standard.ts) | 新增 API 接口 |
| [crud-page-standard.vue](5-代码片段库/crud-page-standard.vue) | CRUD 页面 |
| [pinia-store-standard.ts](5-代码片段库/pinia-store-standard.ts) | 新增 Pinia Store |
| [static-route-add.ts](5-代码片段库/static-route-add.ts) | 添加静态路由 |
| [permission-directive.vue](5-代码片段库/permission-directive.vue) | 权限指令 |
| [file-upload.vue](5-代码片段库/file-upload.vue) | 文件上传 |
| [file-download.ts](5-代码片段库/file-download.ts) | 文件下载/导出 |
| [form-validation.vue](5-代码片段库/form-validation.vue) | 表单校验 |
| [nui-component-template.vue](5-代码片段库/nui-component-template.vue) | NUI 封装组件 |
| [scss-variables-usage.vue](5-代码片段库/scss-variables-usage.vue) | SCSS 全局变量 |
| [mixin-usage.vue](5-代码片段库/mixin-usage.vue) | SCSS mixin |
| [echarts-integration.vue](5-代码片段库/echarts-integration.vue) | ECharts 图表 |
| [websocket-connection.ts](5-代码片段库/websocket-connection.ts) | WebSocket 消息 |
| [rsa-password-encrypt.ts](5-代码片段库/rsa-password-encrypt.ts) | RSA 密码加密 |
| [theme-switch.vue](5-代码片段库/theme-switch.vue) | 主题切换 |
| [table-with-pagination.vue](5-代码片段库/table-with-pagination.vue) | 分页表格 |
| [dictionary-usage.vue](5-代码片段库/dictionary-usage.vue) | 字典数据 |
| [org-tree-usage.vue](5-代码片段库/org-tree-usage.vue) | 组织树选择 |
| [mock-data-config.ts](5-代码片段库/mock-data-config.ts) | Mock 数据配置 |
