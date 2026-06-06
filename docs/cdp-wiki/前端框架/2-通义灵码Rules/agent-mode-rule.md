---
trigger: always_on
knowledge_source:
  - docs/knowledge-base/7-项目级指令文件/lingma-custom-instructions.md
---

# CDP 前端 Agent 模式工作流

## 概述

本规则定义 AI Agent 在 CDP Web 前端项目中执行开发任务的六阶段闭环工作流。每个阶段有明确的输入、输出和检查点，确保代码变更可控、规范、可回溯。

## 技术上下文

- 技术栈：Vue 3.4 + TypeScript 5.4 + Element Plus 2.8 + Pinia 2.1 + Vite 5.x + UnoCSS + Axios
- 包管理器：仅限 pnpm
- 路径别名：`@/` 映射到 `src/`
- 开发服务器：`pnpm run dev`（端口 9527）
- 代码检查：`pnpm run lint:eslint`
- 生产构建：`pnpm run build:prod`
- 规范提交：`pnpm run commit`

---

## 阶段一：探索（Explore）

**目标**：充分理解需求，阅读相关源码和知识库，建立上下文。

**执行步骤**：
1. 分析用户需求，识别涉及的业务模块和技术领域
2. 阅读相关模块的现有代码：
   - 页面组件：`src/cdp-common-frame/{module}/views/`
   - API 接口：`src/cdp-common-frame/{module}/api/`
   - 路由配置：`src/_changeable/router.ts`
   - Store 状态：`src/cdp-admin/store/modules/`
3. 查阅知识库文档（`docs/knowledge-base/`）中的相关规范
4. 确认是否有可复用的 NUI 组件（`cdp-common-nui`）或公共控件（`cdp-common-ctrl`）

**输出**：需求理解摘要 + 相关代码/文件清单

---

## 阶段二：规划（Plan）

**目标**：制定实现方案，列出要创建或修改的文件清单。

**执行步骤**：
1. 确定需要变更的文件列表，按类型分类：
   - 新增文件：页面 Vue 组件、API 定义（index.ts + types.ts）、路由配置
   - 修改文件：聚合文件（`_changeable/api.ts`、`_changeable/router.ts`）、Store
2. 确认分层架构是否正确：
   - View 层只负责交互编排，不直接操作 HTTP
   - API 层只封装请求，不含业务逻辑
   - Component 层通过 Props/Emits 通信，不直接调用 API
3. 评估影响范围，标注风险点

**输出**：文件变更清单 + 实现方案概要

---

## 阶段三：确认（Confirm）

**目标**：与用户确认方案，明确变更范围，避免返工。

**执行步骤**：
1. 向用户展示实现方案：
   - 要创建/修改的文件列表
   - 核心实现思路
   - 需要注意的风险点
2. 等待用户确认或提出修改意见
3. 如有调整，更新方案后再次确认

**输出**：用户确认的最终方案

> 注意：对于简单明确的任务（如修复 typo、调整样式），可合并确认步骤，快速推进。

---

## 阶段四：执行（Execute）

**目标**：按方案编写代码，严格遵循 CDP 前端规范。

**编码规范检查清单**：
- [ ] 组件使用 `<script setup lang="ts">` 组合式 API
- [ ] Props 使用 `defineProps<T>()` 泛型定义
- [ ] Emits 使用 `defineEmits<{...}>()` 类型声明
- [ ] API 使用框架封装方法（`postJson` / `get` / `del`），不直接使用 axios
- [ ] 跨模块引用使用 `@/` 路径别名
- [ ] 类型定义放在对应模块的 `types.ts` 中，避免使用 `any`
- [ ] 权限控制使用 `v-hasPerm` 指令
- [ ] 样式使用 `<style scoped lang="scss">`，不手动 `@import` 全局变量
- [ ] Store 使用 Composition API 风格，组件外通过 `useXxxStoreHook()` 访问
- [ ] 密码字段使用 `rsa.encrypt()` 加密

**文件组织**：
```
新增业务模块参考结构：
src/cdp-common-frame/{module}/
├── api/
│   ├── index.ts          # API 接口方法
│   └── types.ts          # 请求/响应类型定义
├── views/
│   ├── index.vue         # 列表页
│   └── components/       # 页面级子组件
└── router/
    └── constantRoutes.ts # 模块路由（聚合到 _changeable/router.ts）
```

**输出**：符合规范的代码变更

---

## 阶段五：检查（Check）

**目标**：运行自动化检查，验证代码质量和功能正确性。

**执行步骤**：
1. 运行 ESLint 检查：
   ```bash
   pnpm run lint:eslint
   ```
2. 运行 TypeScript 类型检查（如需要）：
   ```bash
   npx vue-tsc --noEmit
   ```
3. 验证开发服务器能正常启动：
   ```bash
   pnpm run dev
   ```
4. 人工检查要点：
   - 模块间无循环依赖
   - 下层模块未引用上层模块
   - API 已聚合到 `_changeable/api.ts`
   - 路由已聚合到 `_changeable/router.ts`
   - 新增文件命名符合规范（组件 PascalCase，API kebab-case）

**输出**：检查结果（通过/失败 + 修复记录）

---

## 阶段六：反馈（Feedback）

**目标**：总结变更内容，告知用户后续注意事项。

**执行步骤**：
1. 列出所有变更文件及变更类型（新增/修改/删除）
2. 说明核心实现逻辑
3. 提示后续事项：
   - 是否需要配置后端接口
   - 是否需要在菜单管理中配置路由权限
   - 是否需要在 `_changeable/api.ts` 或 `_changeable/router.ts` 中注册
   - 是否影响其他模块，需要回归测试
4. 提供 Git 提交建议：
   ```bash
   pnpm run commit
   # 选择类型：feat / fix / refactor 等
   # scope 填写模块名，如 system / workflow / login
   ```

**输出**：变更总结 + 后续注意事项

---

## 工作流总览

```
探索 → 规划 → 确认 → 执行 → 检查 → 反馈
 ↑                              │
 └──────── 发现问题则回退 ───────┘
```

**关键原则**：
- 每个阶段完成后再进入下一阶段，不跳步
- 检查阶段发现问题时，回退到执行阶段修复，而非跳过
- 对于复杂任务，探索和规划阶段要充分，避免执行阶段频繁返工
- 始终遵循 CDP 前端分层架构，确保模块间依赖关系正确
