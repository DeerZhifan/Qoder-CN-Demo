# CDP 后端规则索引

> 本目录包含 CDP 后端框架的所有编码规则文件。AI 助手根据触发方式自动加载对应规则。

## 规则列表

### 核心规范

| 规则文件 | 触发方式 | 关联技能 | 简述 |
|---------|---------|---------|------|
| `cdp-backend-rule.md` | always_on | init-project, init-multimodule-project, init-business-module | 四层架构、模型约定、CRUD 路径、异常处理、禁止事项 |
| `agent-mode-rule.md` | always_on | （全部技能） | 六阶段闭环流程：探索-规划-确认-执行-检查-反馈 |

### 基础设施

| 规则文件 | 触发方式 | 关联技能 | 简述 |
|---------|---------|---------|------|
| `cdp-rule-cache.md` | when_referenced | enable-cache | 缓存组件约束：@EnableCdpCaching、CdpCacheClient API、TTL 配置、禁止直接操作 RedisTemplate |
| `cdp-rule-lock.md` | when_referenced | enable-lock | 分布式锁约束：@EnableCdpLock、CdpLockClient API、锁粒度、超时配置 |
| `cdp-rule-log.md` | when_referenced | enable-log | 日志组件约束：操作日志注解、日志级别规范 |
| `cdp-rule-flyway.md` | when_referenced | gen-flyway-migration | Flyway 迁移约束：脚本命名、版本号、禁止修改已执行脚本 |
| `cdp-rule-crud-gen.md` | when_referenced | gen-crud | CRUD 代码生成约束：生成文件规范、模板一致性 |
| `cdp-rule-testing.md` | when_referenced | -- | 测试编码规范：单元测试、集成测试约定 |
| `cdp-rule-smartdoc.md` | when_referenced | gen-smartdoc | Smart-doc API 文档约束：注释规范、文档生成配置 |

### 安全与认证

| 规则文件 | 触发方式 | 关联技能 | 简述 |
|---------|---------|---------|------|
| `cdp-rule-auth.md` | when_referenced | enable-auth | 认证授权约束：SA-Token 配置、IUserHelper 实现、权限注解 |
| `cdp-rule-jasypt.md` | when_referenced | enable-jasypt | 配置加密约束：Jasypt 加密格式、密钥管理 |
| `cdp-rule-apikey.md` | when_referenced | enable-apikey | API Key 约束：密钥生成、验证流程 |
| `cdp-rule-datascope.md` | when_referenced | enable-datascope | 数据权限约束：范围注解、SQL 拦截规范 |

### 业务功能

| 规则文件 | 触发方式 | 关联技能 | 简述 |
|---------|---------|---------|------|
| `cdp-rule-stream.md` | when_referenced | enable-stream | 消息队列约束：统一抽象 API、Kafka/RabbitMQ/RocketMQ 切换 |
| `cdp-rule-job.md` | when_referenced | enable-job, gen-xxljob-handler | 任务调度约束：@EnableXxlJobExecutor、Handler 编写规范 |
| `cdp-rule-file.md` | when_referenced | enable-file | 文件管理约束：上传下载 API、存储策略 |
| `cdp-rule-export.md` | when_referenced | enable-export | 数据导出约束：导出模板、异步导出 |
| `cdp-rule-echo.md` | when_referenced | enable-echo | 字段回显约束：@Echo 注解、回显策略 |
| `cdp-rule-message.md` | when_referenced | enable-message | 消息通知约束：消息模板、发送渠道 |
| `cdp-rule-gen.md` | when_referenced | enable-gen | 代码生成器约束：模板配置、生成策略 |
| `cdp-rule-syncdata.md` | when_referenced | enable-syncdata | 数据同步约束：同步策略、冲突处理 |
| `cdp-rule-cotime.md` | when_referenced | enable-cotime | 协同时间约束：时间同步、时区处理 |

### 复杂业务组件

| 规则文件 | 触发方式 | 关联技能 | 简述 |
|---------|---------|---------|------|
| `cdp-rule-workflow.md` | when_referenced | enable-workflow | 工作流约束：流程定义、任务处理、单据集成 |
| `cdp-rule-form.md` | when_referenced | enable-form | 自定义表单约束：表单定义、数据绑定 |
| `cdp-rule-report.md` | when_referenced | enable-report | 报表约束：帆软集成、数据源配置 |
| `cdp-rule-fulltext.md` | when_referenced | enable-fulltext | 全文检索约束：Elasticsearch 配置、索引管理 |
| `cdp-rule-sharding.md` | when_referenced | enable-sharding | 分库分表约束：ShardingSphere 配置、分片策略 |
| `cdp-rule-micro.md` | when_referenced | enable-micro | 微服务约束：Nacos/Gateway/Sentinel 配置、服务间调用 |
