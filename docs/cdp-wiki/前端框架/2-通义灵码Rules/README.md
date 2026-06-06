# CDP 前端规则索引

> 本目录包含 CDP 前端框架的 12 个规则文件，AI 编程助手在生成代码时自动遵循。

## 核心规范

| 规则文件 | 触发方式 | 关联技能 | 简述 |
|---------|---------|---------|------|
| cdp-frontend-rule.md | always_on | — | 前端核心编码规范：目录结构、命名约定、TypeScript/Vue 代码风格 |
| agent-mode-rule.md | always_on | — | 智能体六阶段闭环工作流：探索→规划→确认→执行→检查→反馈 |

## 核心模块

| 规则文件 | 触发方式 | 关联技能 | 简述 |
|---------|---------|---------|------|
| cdp-rule-build.md | when_referenced | gen-build-config | Vite 构建配置、环境变量、打包优化规范 |
| cdp-rule-config.md | when_referenced | gen-env-config | 多环境配置管理、运行时配置切换 |
| cdp-rule-http-api.md | when_referenced | gen-api-module | Axios 封装、请求/响应拦截、API 模块组织 |
| cdp-rule-state-management.md | when_referenced | gen-pinia-store | Pinia Store 定义、持久化、模块化状态管理 |
| cdp-rule-routing-permission.md | when_referenced | gen-route-config | 路由配置、动态路由、权限指令、菜单生成 |
| cdp-rule-component.md | when_referenced | gen-component | NUI 组件使用、自定义组件封装、组件通信 |
| cdp-rule-style.md | when_referenced | gen-theme-config | SCSS 变量体系、主题切换、BEM 命名 |

## 业务功能

| 规则文件 | 触发方式 | 关联技能 | 简述 |
|---------|---------|---------|------|
| cdp-rule-encryption.md | when_referenced | enable-encryption | RSA 密码加密、敏感数据传输 |
| cdp-rule-workflow-fe.md | when_referenced | enable-workflow-fe | 工作流前端集成、流程表单、审批操作 |
| cdp-rule-business-module.md | when_referenced | init-frontend-module | 业务模块目录结构、页面组织、CRUD 模板 |
