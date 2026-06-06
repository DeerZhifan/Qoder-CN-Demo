# CDP 后端技能索引

> 本目录包含 CDP 后端框架的所有操作技能。用户通过指令或关键词调用对应技能。

## 技能列表

### 项目初始化

| 技能目录 | 类型 | 关联规则 | 简述 |
|---------|------|---------|------|
| `init-project/` | init | cdp-backend-rule | 初始化单体应用项目骨架（pom、启动类、配置文件） |
| `init-multimodule-project/` | init | cdp-backend-rule | 初始化多模块应用项目骨架（父工程 + main + 业务模块） |
| `init-business-module/` | init | cdp-backend-rule | 初始化业务模块（api/service/controller/boot-starter/cloud-starter） |

### 基础设施

| 技能目录 | 类型 | 关联规则 | 简述 |
|---------|------|---------|------|
| `enable-cache/` | enable | cdp-rule-cache | 启用缓存组件（依赖 + @EnableCdpCaching + 配置 + 示例） |
| `enable-lock/` | enable | cdp-rule-lock | 启用分布式锁（依赖 + @EnableCdpLock + 配置 + 示例） |
| `enable-log/` | enable | cdp-rule-log | 启用操作日志（依赖 + 配置 + 注解使用） |
| `gen-flyway-migration/` | gen | cdp-rule-flyway | 生成 Flyway 迁移脚本（命名 + 模板 + 目录） |
| `gen-crud/` | gen | cdp-rule-crud-gen | 生成标准 CRUD 代码（PO/DTO/QO/DAO/Service/Controller） |
| `gen-smartdoc/` | gen | cdp-rule-smartdoc | 生成 Smart-doc API 文档配置和执行 |

### 安全与认证

| 技能目录 | 类型 | 关联规则 | 简述 |
|---------|------|---------|------|
| `enable-auth/` | enable | cdp-rule-auth | 启用认证授权（SA-Token 配置 + IUserHelper 实现） |
| `enable-jasypt/` | enable | cdp-rule-jasypt | 启用配置加密（Jasypt 依赖 + 加密示例） |
| `enable-apikey/` | enable | cdp-rule-apikey | 启用 API Key 认证（依赖 + 配置 + 验证流程） |
| `enable-datascope/` | enable | cdp-rule-datascope | 启用数据权限（依赖 + 注解 + SQL 拦截配置） |

### 业务功能

| 技能目录 | 类型 | 关联规则 | 简述 |
|---------|------|---------|------|
| `enable-stream/` | enable | cdp-rule-stream | 启用消息队列（依赖 + profile 切换 + 生产者/消费者示例） |
| `enable-job/` | enable | cdp-rule-job | 启用任务调度（依赖 + @EnableXxlJobExecutor + 配置） |
| `gen-xxljob-handler/` | gen | cdp-rule-job | 生成 XXL-Job Handler 模板 |
| `enable-file/` | enable | cdp-rule-file | 启用文件管理（依赖 + 存储配置 + 上传下载示例） |
| `enable-export/` | enable | cdp-rule-export | 启用数据导出（依赖 + 模板配置 + 导出示例） |
| `enable-echo/` | enable | cdp-rule-echo | 启用字段回显（依赖 + @Echo 注解使用） |
| `enable-message/` | enable | cdp-rule-message | 启用消息通知（依赖 + 模板 + 发送渠道配置） |
| `enable-gen/` | enable | cdp-rule-gen | 启用代码生成器（依赖 + 配置 + 模板定制） |
| `enable-syncdata/` | enable | cdp-rule-syncdata | 启用数据同步（依赖 + 同步策略配置） |
| `enable-cotime/` | enable | cdp-rule-cotime | 启用协同时间（依赖 + 时间同步配置） |

### 复杂业务组件

| 技能目录 | 类型 | 关联规则 | 简述 |
|---------|------|---------|------|
| `enable-workflow/` | enable | cdp-rule-workflow | 启用工作流引擎（依赖 + 配置 + 流程定义示例） |
| `enable-form/` | enable | cdp-rule-form | 启用自定义表单（依赖 + 配置 + 表单定义示例） |
| `enable-report/` | enable | cdp-rule-report | 启用报表系统（帆软集成 + 数据源配置） |
| `enable-fulltext/` | enable | cdp-rule-fulltext | 启用全文检索（Elasticsearch 依赖 + 索引配置） |
| `enable-sharding/` | enable | cdp-rule-sharding | 启用分库分表（ShardingSphere 配置 + 分片策略） |
| `enable-micro/` | enable | cdp-rule-micro | 启用微服务治理（Nacos + Gateway + Sentinel 全套配置） |
