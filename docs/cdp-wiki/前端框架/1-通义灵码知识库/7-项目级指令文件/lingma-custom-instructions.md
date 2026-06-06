# 通义灵码项目级自定义指令（前端）

> 将以下内容复制到通义灵码 → 设置 → 项目级自定义指令（Custom Instructions）中。
> 配置后，每次 AI 对话将自动注入这些约束，无需开发者手动说明框架规范。

---

## 指令内容（直接复制以下文本）

```
你正在辅助开发基于 CDP Web 前端框架的项目，技术栈为：
Vue 3.4 + TypeScript 5.4 + Element Plus 2.8 + Pinia 2.1 + Vite 5.x + UnoCSS + Axios

【组件规范】
- 使用 <script setup lang="ts"> 组合式 API 编写组件，不使用 Options API
- 组件使用 NUI 封装组件（cdp-common-nui）和公共控件（cdp-common-ctrl），不直接使用 el-* 原始组件
- 全局组件（CdpTable、Pagination、OrgRoleUser、OrgunitTree、SvgIcon）已注册，直接在模板中使用
- Element Plus 组件通过 unplugin-vue-components 自动按需导入，无需手动 import

【API 调用规范】
- 在模块目录下创建 api/ 子目录，定义类型（types.ts）和接口方法（index.ts）
- 使用框架封装的请求方法：postJson() 提交 JSON，post() 提交表单，get() 查询，getExport() 导出文件
- 不要自行创建 Axios 实例，统一使用 src/cdp-common/utils/request.ts 中的方法
- API 聚合到 src/_changeable/api.ts，通过 $api 全局访问

【状态管理规范】
- 使用 Pinia Composition API 风格定义 Store：defineStore("id", () => {...})
- 持久化使用 @vueuse/core 的 useStorage，key 统一加 cdp- 前缀
- 在非 setup 上下文中（路由守卫、拦截器），通过 useXxxStoreHook() 访问 Store
- 解构 Store 中的响应式属性必须使用 storeToRefs()

【路由规范】
- 使用 Hash 路由模式（createWebHashHistory）
- 静态路由在模块 router/constantRoutes.ts 中定义，聚合到 src/_changeable/router.ts
- 动态路由由后端菜单管理，前端通过 import.meta.glob 自动加载对应 Vue 文件
- 路由配置在 _changeable/router.ts 中统一管理，不在组件内硬编码路径

【权限规范】
- 按钮权限使用 v-hasPerm 指令：v-hasPerm="['sys:user:add']"
- 不要直接读取 permission store 判断权限

【样式规范】
- 样式使用 SCSS + UnoCSS，布局用 UnoCSS 原子类，复杂样式用 SCSS
- 全局 SCSS 变量（variables.scss）和 mixin（mixin.scss）由 Vite 自动注入，不要手动 @import
- 覆盖 Element Plus 样式使用 :deep() 穿透选择器
- 暗黑模式通过 Settings Store 的 changeSetting({ key: "theme", value: "dark" }) 切换

【类型规范】
- 类型定义放在对应模块的 types.ts 中
- 不使用 any，优先定义明确的 interface 或 type
- 全局类型（PageQuery、PageResult<T>）在 src/typings/global.d.ts 中声明

【加密规范】
- 密码字段使用 RSA 加密：import rsa from "@/cdp-common/utils/rsa"; rsa.encrypt(password)
- 接口加密通过环境变量 VITE_API_ENCRYPT 控制，业务代码无需处理
- 不要自行实现加密逻辑，使用框架提供的 aes.ts 和 rsa.ts

【文件处理规范】
- 文件上传使用框架上传组件（Upload/Uploader），不直接使用 input[type=file]
- 文件下载使用 getExport() 或 getFile() 方法 + file-saver 的 saveAs()

【Git 提交规范】
- 使用 pnpm run commit 交互式提交，遵守 commitlint 规范
- 提交类型：feat/fix/docs/style/refactor/test/chore
```

---

## 配置说明

### 在 VS Code / JetBrains 中配置

1. 打开通义灵码插件设置
2. 找到 **自定义指令** 或 **Custom Instructions** 选项
3. 将上方代码块中的内容粘贴进去
4. 保存，重启 IDE 生效

### 在 Claude Code 中配置

将指令内容写入项目根目录的 `CLAUDE.md` 文件即可自动生效。

### 验证是否生效

配置后，在对话框输入以下问题验证：

| 测试问题 | 期望回答包含 |
|----------|-------------|
| 帮我写一个用户列表页面 | `<script setup lang="ts">`、`CdpTable`、`postJson` |
| 如何新增一个 API 接口 | `request.postJson()`、`types.ts`、`_changeable/api.ts` |
| 帮我写一个 Pinia Store | `defineStore("id", () => {...})`、`useStorage`、`useXxxStoreHook` |
| 如何判断用户是否有权限 | `v-hasPerm="['xxx']"` |
| 密码字段如何加密 | `rsa.encrypt(password)` |
