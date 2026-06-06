# CDP 任务调度 设计手册

> 对应使用手册：[cdp-module-job.md](../3-组件使用手册/cdp-module-job.md)

## 一、设计目标与背景

企业应用中存在大量定时任务需求（报表生成、数据同步、缓存刷新等）。CDP 需要提供一套开箱即用的分布式任务调度能力，同时支持单体和微服务两种部署形态。

设计目标：

1. **嵌入式调度中心** -- 将 XXL-JOB Admin 嵌入到 CDP 应用中，无需独立部署调度中心服务。
2. **最小化接入成本** -- 通过 `@EnableXxlJobExecutor` 一个注解即可激活执行器，零配置启动。
3. **双模式部署** -- boot-starter 包含完整调度中心 + 执行器，cloud-starter 通过 Feign 代理支持微服务拆分。
4. **条件化装配** -- 通过 `xxl.job.executor.enabled` 属性可在不移除依赖的情况下禁用执行器。

> 设计决策：选择将 XXL-JOB 2.4.0 源码内置而非 JAR 依赖，以便对调度中心进行 CDP 框架级定制（安全集成、数据库适配、管理界面嵌入）。

## 二、整体架构

模块内部分为四个子模块，职责清晰：

```
leatop-cdp-base-job/
  |- leatop-cdp-base-job-core          # 执行器核心：XxlJobSpringExecutor、注解配置
  |- leatop-cdp-base-job-admin         # 调度中心：调度引擎、路由策略、任务管理
  |- leatop-cdp-base-job-api           # 对外接口：Business 接口、DTO 定义
  |- leatop-cdp-base-job-boot-starter  # 单体自动配置：引入 admin + core
  |- leatop-cdp-base-job-cloud-starter # 微服务自动配置：Feign 代理 + controller 扫描
```

运行时架构：

```
+----------------------------+          +----------------------------+
|   CDP 应用 (boot-starter)   |          |   CDP 应用 (cloud-starter)  |
|                            |          |                            |
|  [XxlJobScheduler]  调度引擎 |  <---->  |  [Feign Client] 远程调用     |
|  [XxlJobTrigger]   触发执行  |          |  [Controller]  API 暴露     |
|  [XxlJobSpringExecutor] 执行器|          |  [XxlJobSpringExecutor]    |
+----------------------------+          +----------------------------+
```

## 三、核心设计模式

### 3.1 注解驱动装配 -- EnableXxlJobExecutor

EnableXxlJobExecutor 是一个 `@Import` 注解，导入 XxlJobExecutorConfig 配置类。该配置类：

- 通过 `@EnableConfigurationProperties(XxlJobExecutorProperties.class)` 绑定 `xxl.job.executor.*` 配置。
- 通过 `@ConditionalOnProperty(value = "enabled", matchIfMissing = true)` 实现默认启用、可配置禁用。
- 通过 `@ConditionalOnMissingBean` 确保业务项目可替换默认的 XxlJobSpringExecutor 实例。

### 3.2 调度中心嵌入 -- JobAutoConfiguration

JobAutoConfiguration 通过 `@ComponentScan({"com.leatop.cdp.job", "com.xxl.job.admin"})` 将 XXL-JOB Admin 的所有 Controller、Service、DAO 扫描进 Spring 容器，配合 `@MapperScan` 注册 MyBatis Mapper。这种方式将调度中心完整嵌入到宿主应用中，共享同一个数据库连接池和端口。

已废弃的 `@EnableXxlJobAdmin` 注解通过 `@Deprecated` 标记，引导用户直接使用 boot-starter 依赖。

### 3.3 微服务模式 -- CdpJobCloudAutoConfiguration

cloud-starter 不引入 admin 模块，而是扫描 `com.leatop.cdp.job.controller` 包暴露 REST API，同时通过 `@EnableFeignClients(basePackages = "com.leatop.cdp.job.business")` 注册 Feign 客户端。api 模块中的 Business 接口（JobApiBusiness、JobInfoBusiness 等）既是 Feign 客户端接口，也是本地调用接口，实现了同一套接口在单体和微服务下的透明切换。

### 3.4 路由策略模式

调度中心通过 ExecutorRouteStrategyEnum 枚举定义多种路由策略，每种策略对应一个 ExecutorRouter 实现类：

- ExecutorRouteFirst / ExecutorRouteLast -- 固定首/末节点
- ExecutorRouteRound -- 轮询
- ExecutorRouteRandom -- 随机
- ExecutorRouteLFU / ExecutorRouteLRU -- 最不常用/最近最少使用
- ExecutorRouteConsistentHash -- 一致性哈希
- ExecutorRouteBusyover / ExecutorRouteFailover -- 忙碌转移/故障转移

## 四、关键类说明

| 类名 | 所属模块 | 职责 |
|------|---------|------|
| EnableXxlJobExecutor | job-core | 启用执行器的注解，导入 XxlJobExecutorConfig |
| XxlJobExecutorConfig | job-core | 执行器自动配置，创建 XxlJobSpringExecutor Bean |
| XxlJobExecutorProperties | job-core | 执行器配置属性绑定（adminAddresses、appname、port 等） |
| XxlJobSpringExecutor | job-core | Spring 环境下的执行器实现，管理任务线程池和注册逻辑 |
| JobAutoConfiguration | job-boot-starter | 单体模式自动配置，扫描 admin 和 job 包 |
| EnableXxlJobAdmin | job-boot-starter | 已废弃，原用于显式启用调度中心 |
| CdpJobCloudAutoConfiguration | job-cloud-starter | 微服务模式自动配置，启用 Feign 客户端 |
| XxlJobScheduler | job-admin | 调度引擎，管理调度线程和触发池 |
| XxlJobTrigger | job-admin | 任务触发器，选择路由策略并下发执行请求 |
| ExecutorRouteStrategyEnum | job-admin | 路由策略枚举，映射到具体 ExecutorRouter 实现 |
| XxlJobAdminConfig | job-admin | 调度中心配置，持有 DAO 和线程池引用 |
| JobApiBusiness / JobInfoBusiness | job-api | 对外业务接口，支持 Feign 和本地双模式 |

## 五、扩展机制

1. **自定义执行器** -- 业务项目可声明自己的 XxlJobSpringExecutor Bean（`@ConditionalOnMissingBean` 保证不冲突），自定义线程池大小或注册逻辑。

2. **任务告警扩展** -- 调度中心通过 JobAlarm 接口实现告警通知。框架内置 EmailJobAlarm 和 WeiXinJobAlarm，可通过实现 JobAlarm 接口并注册 Bean 扩展新的告警渠道。

3. **路由策略扩展** -- 新增 ExecutorRouter 实现类并在 ExecutorRouteStrategyEnum 中注册，即可支持自定义路由策略。

## 六、模块协作（简要）

- **与 auth 模块**：调度中心管理界面的权限控制通过 PermissionInterceptor 实现，使用 `@PermissionLimit` 注解标记需要登录的接口。在 CDP 集成环境中，可与 SA-Token 认证体系对接。
- **与 flyway 模块**：调度中心依赖 `xxl_job_*` 系列数据库表，表结构随 CDP Flyway 迁移脚本一起管理。
- **与 system 模块**：JobConfigService 提供调度中心参数的动态配置能力。

## 七、设计权衡与约束（简要）

1. **嵌入式 vs 独立部署** -- 将 XXL-JOB Admin 嵌入应用简化了运维，但意味着调度中心的高可用依赖宿主应用的高可用。集群部署时需注意数据库锁竞争。

2. **源码内置** -- 内置 XXL-JOB 源码便于定制，但后续升级需要手动合并上游变更。当前固定在 2.4.0 版本。

3. **执行器默认启用** -- `matchIfMissing = true` 设计使得引入依赖即激活执行器，降低接入门槛，但在不需要执行器的模块中需要显式设置 `enabled: false`。
