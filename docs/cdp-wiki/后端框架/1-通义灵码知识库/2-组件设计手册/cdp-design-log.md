# CDP 日志管理 设计手册

> 对应使用手册：[cdp-module-log.md](../3-组件使用手册/cdp-module-log.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

企业应用的审计合规要求对用户行为进行完整记录和可追溯查询。`leatop-cdp-business-log` 模块围绕以下目标设计：

1. **四类日志全覆盖**：登录日志、操作日志、授权日志、效率日志分别对应不同审计场景，满足等保合规和性能分析需求。
2. **声明式采集**：通过 `@LogRecord` 元注解及其衍生注解 `@LogParam`、`@EfficiencyLog`，以 AOP 方式透明采集日志，业务代码零侵入。
3. **异步写入**：日志采集在专用线程池中异步执行，避免日志写入阻塞业务请求。
4. **双采集通道**：操作日志和效率日志通过 AOP 注解采集，HTTP 接口访问日志通过 `HandlerInterceptor` 采集，两条通道互补覆盖。

> 设计决策：日志写入采用 Spring `@Async` + 专用线程池方案，而非消息队列异步方案。这是因为日志模块作为基础设施需要尽量减少外部依赖，`@Async` 方案在单体部署模式下无需引入 MQ 中间件。在微服务模式下，日志采集通过 `LogCollectBusiness` 接口的远程调用实现解耦。

## 二、整体架构

```
业务代码（标注 @LogParam / @EfficiencyLog）
  |
  v
LogAspect (MethodInterceptor + PointcutAdvisor)  <--- AOP 切面
  |
  v                                          HTTP 请求
LogClientUtils (静态工具) <----- LogInterceptor (HandlerInterceptor)
  |
  v
LogClient (组装 DTO，填充用户/IP 等上下文)
  |
  v
LogCollectBusiness / LogCollectBusinessImpl (业务入口)
  |
  v
LogCollectService (@Async 异步写入)
  |
  +---> LoginLogDAO    ---> frame_log_login
  +---> OperateLogDAO  ---> frame_log_operate
  +---> EfficiencyLogDAO ---> frame_log_efficiency
  +---> AuthorizeLogDAO ---> frame_log_authorize
```

**配置驱动**：`LogHandlerConfig` 通过 `@ConditionalOnProperty(name = "cdp.syslog.enable")` 控制 `LogAspect` 的注册。`LogInterceptor` 的注册由 `LogProperties.includePatterns` 决定 -- 仅当配置了 `includePatterns` 时才激活 URL 级别的效率日志拦截。

## 三、核心设计模式

### 静态代理 + InitializingBean 初始化

`LogClientUtils` 是一个纯静态工具类，提供 `logLogin()`、`logOperate()`、`logEfficiency()`、`logAuthorize()` 四个静态方法，供框架内任何位置调用。它持有一个静态 `LogClient` 引用，由 `LogClient` 在 Spring 容器初始化完成后（`afterPropertiesSet()`）通过 `LogClientUtils.setLogClient(this)` 注入。

> 设计决策：使用静态方法而非注入 Bean 的方式提供日志 API，是因为日志记录可能发生在非 Spring 管理的上下文中（如过滤器、静态工具方法），静态访问消除了对 Spring 容器的依赖。`LogClient` 在未初始化时优雅降级为日志打印而非抛出异常。

### 元注解组合

`@LogRecord` 是底层元注解，定义了 `type()` 属性来区分日志类型（`OPERATE` / `EFFICIENCY`）。`@LogParam` 和 `@EfficiencyLog` 通过 `@LogRecord` 元标注实现功能继承，各自设定默认的日志类型，简化使用方的注解参数。`LogAspect` 通过 `AnnotatedElementUtils.getMergedAnnotation()` 解析合并后的注解属性，支持方法级覆盖类级配置。

### 操作类型自动推导

`LogAspect.deduceFromMethodName()` 在 `@LogRecord.operateType` 未显式指定时，根据方法名前缀自动推导操作类型：`add/save/insert` -> `CREATE`，`update/edit` -> `UPDATE`，`delete/remove` -> `DELETE`，`query/search/get` -> `RETRIEVE`，`import` -> `IMPORT`，`export` -> `EXPORT`。这一约定优于配置的设计减少了注解参数的冗余。

### 异步线程池隔离

`LogThreadPoolConfig` 定义了专用的 `logCollectTaskExecutor` 线程池，核心线程 10、最大线程 30、队列容量 2000、拒绝策略 `CallerRunsPolicy`。`LogCollectService` 的四个采集方法均标注 `@Async("logCollectTaskExecutor")`，确保日志写入在独立线程中执行。

> 设计决策：拒绝策略选择 `CallerRunsPolicy` 而非丢弃策略，是因为审计日志不可丢失。当线程池和队列都满时，由调用者线程同步执行写入，虽然会短暂影响业务响应时间，但保证了日志完整性。

## 四、关键类说明

### LogAspect

实现 `MethodInterceptor` 和 `PointcutAdvisor` 双接口的 Spring AOP Advisor。切入点通过 `ComposablePointcut` 组合了方法级和类级的 `@LogRecord` 注解匹配。`invoke()` 方法在目标方法执行前后采集信息：记录起始时间、解析 SpEL 表达式生成日志描述、区分操作日志和效率日志分别调用 `LogClientUtils` 的不同方法。

### LogClient

Spring 组件，承担日志 DTO 组装职责。为每条日志填充通用上下文：通过 `IUserHelper` 获取当前用户 ID、用户名、组织信息、租户 ID；通过 `ServletUtils.getRemoteIP()` 获取客户端 IP；通过 `EnvUtils.getAppName()` 获取应用名称；通过 `IdUtils.nextIdStr()` 生成日志主键。组装完成后调用 `LogCollectBusiness` 的对应采集方法。

### LogInterceptor

`HandlerInterceptor` 实现，在 `preHandle()` 中记录请求起始时间（存入 `ThreadLocal`），在 `afterCompletion()` 中计算耗时并通过 `LogClientUtils.logEfficiency()` 记录 URL 级别效率日志（`type=0` 表示 URL 类型，区别于 `LogAspect` 的 `type=1` 方法类型）。

### LogCollectService

四个 `@Async` 方法对应四类日志的持久化逻辑。每个方法的流程一致：设置 MDC 链路 ID 和租户上下文 -> 检查日志记录是否已存在（支持 idempotent 更新） -> 执行 insert 或 update -> 清理 ThreadLocal 上下文。

### 日志表结构

四张表共享 `BaseLogEntity` 基类字段（`logId`、`applicationName`、`tenant_id`、`create_gmt`、`update_gmt`）：

- **frame_log_login**：`LoginLogPO`，记录用户 ID、账号、操作 IP、登录状态、操作类型
- **frame_log_operate**：`OperateLogPO`，记录操作模块、操作类型、日志描述、变更前后值（`oldValue`/`newValue`）、业务主键
- **frame_log_efficiency**：`EfficiencyLogPO`，记录访问模块、请求参数、返回结果、耗时（`duration`）、类型（URL/方法）
- **frame_log_authorize**：`AuthorizeLogPO`，记录授权角色、授权类型、变更前后值、授权内容

## 五、扩展机制

1. **自定义日志类型**：如需新增日志类型（如数据导出日志），在 `LogRecord.LogType` 枚举中增加类型，在 `LogAspect.invoke()` 中补充对应分支，新建 DAO 和 PO 实现持久化。
2. **日志存储替换**：当前日志直接写入关系型数据库。如需切换到 Elasticsearch 或日志服务，只需替换 `LogCollectService` 中的 DAO 调用为对应客户端的写入逻辑，上层 `LogClient` 和注解体系无需变更。
3. **微服务日志汇聚**：在微服务模式下，`LogCollectBusiness` 接口可通过 Feign Client 远程调用日志中心服务，`LogCollectBusinessImpl` 中通过 `HeaderConstant.RESOURCE_ID` 标识日志来源的微服务实例。`DefaultLogCollectBusiness` 作为 fallback 实现，在日志服务不可用时优雅降级。

## 六、模块协作（简要）

- **leatop-cdp-common-core**：`MDCRequestFilter` 在请求入口生成 `traceId`，日志模块在异步线程中通过 `MDC.put()` 传递该 ID，实现日志与请求链路的关联。
- **leatop-cdp-common-auth**：登录/登出事件由认证模块触发，通过 `LogClientUtils.logLogin()` 记录到 `frame_log_login` 表。
- **leatop-cdp-business-system**：角色授权变更通过 `LogClientUtils.logAuthorize()` 记录授权日志。

## 七、设计权衡与约束（简要）

- **异步写入的一致性**：`@Async` 方案下，若应用突然宕机，队列中未持久化的日志会丢失。对于审计要求极高的场景，可考虑引入消息队列实现写入前持久化。
- **效率日志数据量**：URL 级别效率日志在高并发下会产生大量记录。建议通过 `excludePatterns` 排除静态资源和健康检查等低价值接口，并定期归档历史日志。
- **SpEL 解析开销**：`@LogParam` 的 `logDes` 支持 SpEL 表达式，每次方法调用都会进行表达式解析。对于极高频方法，建议使用简单字符串描述而非复杂表达式。
