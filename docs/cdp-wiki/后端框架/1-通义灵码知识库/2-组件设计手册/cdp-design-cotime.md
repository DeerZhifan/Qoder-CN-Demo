# CDP 调用耗时统计 设计手册

> 对应使用手册：[cdp-module-cotime.md](../3-组件使用手册/cdp-module-cotime.md)

## 一、设计目标与背景

调用耗时统计组件（`leatop-cdp-base-cotime`）基于 Ko-Time 框架实现方法级性能监控，为 CDP 应用提供调用链可视化、慢方法告警和异常追踪能力。

设计目标：

1. **零侵入接入** -- 通过 AOP 切点表达式自动拦截目标方法，无需修改业务代码。
2. **多存储后端** -- 监控数据支持内存、Redis、数据库三种存储方式，按部署场景灵活切换。
3. **调用链建模** -- 自动构建方法间的调用关系图（父子链路），而非仅记录单方法耗时。

> 设计决策：选择内嵌 Ko-Time 而非集成 SkyWalking 等 APM 系统，因为 CDP 需要的是轻量级方法耗时统计而非全链路追踪，Ko-Time 的嵌入式架构更适合单体/微服务混合部署。

## 二、整体架构

```
┌──────────────────────────────────────────────────┐
│                  业务方法调用                      │
└───────────┬──────────────────────────────────────┘
            │
     ┌──────┴──────┐
     │             │
     v             v
┌──────────┐  ┌──────────────┐
│RunTimeHandler│  │ComputeTimeHandler│
│(MethodInterceptor)│  │(@Around AOP)     │
│切点表达式拦截    │  │@ComputeTime注解  │
└────┬─────┘  └──────────────┘
     │
     v
┌──────────────────────┐
│   InvokedQueue       │  ← 异步队列
│   (生产者-消费者模型)  │
└────┬─────────────────┘
     │ 消费线程
     v
┌──────────────────────┐
│  GraphService (SPI)  │  ← 存储抽象
│  ├ MemoryBase        │
│  ├ RedisBase         │
│  └ DataBase          │
└──────────────────────┘
     │
     v
┌──────────────────────┐
│  InvokedHandler 链   │  ← 回调扩展
│  (邮件告警等)         │
└──────────────────────┘
```

## 三、核心设计模式

### 双拦截器模式

组件提供两种拦截方式：

- `RunTimeHandler`：实现 `MethodInterceptor` 接口，由 `LoadConfig.configurabledvisor()` 创建的 `AspectJExpressionPointcutAdvisor` 驱动，按配置的 `pointcut` 表达式拦截方法。这是主要的拦截方式，负责构建调用链关系。
- `ComputeTimeHandler`：标准 AspectJ `@Around` 切面，拦截标注 `@ComputeTime` 注解的方法，仅输出耗时日志，不参与调用链构建。

> 设计决策：保留 `@ComputeTime` 注解方式作为轻量级补充，适用于仅需日志输出而不需要调用链分析的场景。

### 异步队列处理

`RunTimeHandler` 在方法执行完毕后将 `InvokedInfo` 投入 `InvokedQueue`，由后台消费线程（数量由 `ko-time.thread-num` 控制，默认 2）异步处理。消费线程调用 `GraphService` 存储调用关系，并触发 `InvokedHandler` 回调链。这避免了监控逻辑阻塞业务线程。

### 策略模式 -- GraphService

`GraphService` 接口定义了方法节点、方法关系、异常节点的增删查操作。三个实现通过 `@Component("memory"/"redis"/"database")` 注册，`LoadConfig` 在初始化时根据 `ko-time.saver` 配置值匹配对应实现。

- `MemoryBase`：基于 HashMap 存储，重启丢失，适合开发环境。
- `RedisBase`：基于 StringRedisTemplate，支持集群共享，使用前缀隔离数据。
- `DataBase`：基于 JDBC 直连，数据持久化到 `ko_*` 表中。

### 监听器模式 -- InvokedHandler

`InvokedHandler` 接口定义 `onInvoked()` 和 `onException()` 回调。标注 `@KoListener` 注解的实现类被自动注册到 `Context` 的处理器链中。框架内置邮件告警处理器，仅在 `ko-time.mail-enable=true` 时激活。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| `LoadConfig` | 核心配置类，初始化引擎、配置切点、注册存储后端和监听器 |
| `DefaultConfig` | 配置属性类，绑定 `ko-time.*` 前缀的所有配置项 |
| `RunTimeHandler` | 方法拦截器，构建 MethodNode 调用链并投入异步队列 |
| `ComputeTimeHandler` | `@ComputeTime` 注解的 AOP 处理器，仅输出耗时日志 |
| `InvokedHandler` | 方法调用回调接口，支持自定义监听器扩展 |
| `InvokedQueue` | 生产者-消费者队列，解耦拦截与存储 |
| `MemoryBase` | 内存存储实现，基于 HashMap |
| `RedisBase` | Redis 存储实现，基于 StringRedisTemplate |
| `DataBase` | 数据库存储实现，基于 JDBC |
| `KoTimeController` / `HomeController` | 监控面板的 Web 接口 |
| `@Auth` | 监控面板访问认证注解 |
| `@KoListener` | 标记 InvokedHandler 实现类为自动注册的监听器 |

## 五、扩展机制

1. **自定义监听器**：实现 `InvokedHandler` 接口并标注 `@KoListener` + `@Component`，可在方法调用完成后执行自定义逻辑（如推送到监控平台）。
2. **自定义存储后端**：实现 `GraphService` 接口并注册为 `@Component("xxx")`，配置 `ko-time.saver=xxx` 即可启用。
3. **动态配置**：支持通过 classpath 下的 `dynamic.properties` 文件在运行时调整部分参数。

## 六、模块协作（简要）

- **Redis 模块**：当存储模式为 `redis` 时，依赖 Spring Boot 的 `StringRedisTemplate`，复用 CDP 缓存模块的 Redis 连接。
- **Web 面板**：`KoTimeController` 提供内嵌 Web 界面（`/koTime`），展示调用链图、慢方法排行和异常列表，可通过 `ko-time.auth-enable` 启用访问认证。

## 七、设计权衡与约束（简要）

- **切点范围与性能**：`pointcut` 配置范围过大会导致每个方法调用都经过拦截器，建议精确到业务包路径，并设置 `threshold` 过滤短耗时调用。
- **采样丢弃**：`discardRate`（默认 0.3）控制重复调用记录的概率丢弃率，在高并发场景下减少存储压力，但会导致统计数据不完全精确。
- **数据库存储模式的连接管理**：`DataBase` 使用单个 JDBC Connection 进行写入，通过 shutdown hook 关闭，不支持连接池，仅适用于低写入量场景。
