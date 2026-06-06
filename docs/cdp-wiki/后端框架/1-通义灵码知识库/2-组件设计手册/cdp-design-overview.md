# CDP 框架设计总览

> 本文档是所有模块设计手册的索引与汇总，提供跨模块视角的设计全景。阅读本文后，开发者可以快速理解 CDP 框架的设计理念、模块关系、核心模式与扩展机制，并通过索引定位到各模块的详细设计手册。

---

## 一、设计理念

CDP（通用开发平台）基于 Java 17、Spring Boot 3.5.12 和 Spring Cloud 2025.0.0 构建，其核心设计理念贯穿于每一个模块：

### 1.1 约定优于配置

框架通过合理的默认值减少开发者的配置负担。`CdpDefaultConfigFillListener` 在 `ApplicationEnvironmentPreparedEvent` 阶段自动注入 Jasypt 加密器、PageHelper 方言等默认配置；各 Starter 模块的自动配置类通过 `@ConditionalOnMissingBean` 允许业务方覆盖任何默认行为。引入依赖即获得合理默认行为，需要定制时显式覆盖。

### 1.2 双部署模式（同源异构）

同一套业务代码可在单体和微服务两种模式下运行。每个业务模块提供 `*-boot-starter`（单体）和 `*-cloud-starter`（微服务）两个启动器。单体模式下 Business 接口绑定到本地 BusinessImpl 实例；微服务模式下 Business 接口被 OpenFeign 代理填充为远程调用。切换部署模式只需替换 Starter 依赖和调整配置文件，业务代码（Controller / Service / Mapper）无需任何改动。

### 1.3 面向接口编程与 SPI 扩展

每个需要可替换实现的关注点都定义了明确的接口或 SPI。认证通过 `IUserHelper` 抽象用户体系，缓存通过 `CdpCache` / `CdpCacheClient` 抽象后端差异，锁通过 `CdpLock` / `CdpLockClient` 屏蔽本地锁与分布式锁差异，消息队列通过 `MessageOperations` 屏蔽中间件差异。业务代码面向接口编程，框架通过条件装配注入合适的实现。

### 1.4 显式功能开关

缓存（`@EnableCdpCaching`）、分布式锁（`@EnableCdpLock`）、任务调度（`@EnableXxlJobExecutor`）等基础设施能力通过注解显式启用，未启用时不加载任何相关 Bean 和连接，避免资源浪费和启动失败。

### 1.5 分层依赖、禁止循环

模块依赖严格自上而下，禁止循环依赖。版本通过 BOM 模式（`leatop-cdp-dependencies` + `leatop-cdp-bom`）统一管理，子模块声明依赖时不填写版本号。

---

## 二、模块依赖全景图

```
┌─────────────────────────────────────────────────────────────────┐
│                    leatop-cdp-dependencies                       │
│              （第三方库版本管理，全框架顶层 BOM）                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      leatop-cdp-bom                              │
│              （内部模块版本管理，全框架统一引用）                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     leatop-cdp-common                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ ┌────────┐  │
│  │common-util│ │common-data│ │common-core│ │  auth  │ │ jasypt │  │
│  │(工具类)   │ │(PO/DTO/QO│ │(过滤器链、 │ │(SA-Token│ │(SM4配置│  │
│  │          │ │ 异常、响应)│ │ 异常处理、 │ │ 认证鉴权)│ │ 加密)  │  │
│  │          │ │          │ │ SPI接口)  │ │        │ │        │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘ └────────┘  │
│  ┌──────────────┐                                               │
│  │common-starter│  （Spring Boot 自动配置、Flyway字段填充）        │
│  └──────────────┘                                               │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      leatop-cdp-base                             │
│  ┌──────┐ ┌─────┐ ┌───────┐ ┌─────┐ ┌──────┐ ┌────────┐       │
│  │cache │ │lock │ │stream │ │ job │ │export│ │ flyway │       │
│  │(多级 │ │(分布│ │(MQ统一│ │(XXL │ │(导入 │ │(DB版本 │       │
│  │缓存) │ │式锁)│ │抽象)  │ │ JOB)│ │导出) │ │ 管理)  │       │
│  └──────┘ └─────┘ └───────┘ └─────┘ └──────┘ └────────┘       │
│  ┌──────┐ ┌─────┐ ┌───────┐ ┌─────────┐ ┌──────┐ ┌──────┐    │
│  │ echo │ │ gen │ │apikey │ │sharding │ │cotime│ │trans │    │
│  │(字典 │ │(代码│ │(APIKey│ │(分库分表)│ │(耗时 │ │action│    │
│  │回显) │ │生成)│ │ 认证) │ │         │ │统计) │ │(分布式│    │
│  └──────┘ └─────┘ └───────┘ └─────────┘ └──────┘ │事务) │    │
│                                                    └──────┘    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    leatop-cdp-business                            │
│  ┌────────┐ ┌─────┐ ┌──────┐ ┌─────────┐ ┌────────┐            │
│  │ system │ │ log │ │ file │ │ message │ │ report │            │
│  │(用户角色│ │(审计│ │(附件 │ │(消息通知)│ │(报表)  │            │
│  │组织租户)│ │日志)│ │管理) │ │         │ │        │            │
│  └────────┘ └─────┘ └──────┘ └─────────┘ └────────┘            │
│  ┌──────────┐ ┌─────────┐ ┌──────┐ ┌──────┐ ┌──────────┐       │
│  │ workflow │ │fulltext │ │ form │ │ data │ │ syncdata │       │
│  │(工作流)  │ │(全文检索)│ │(表单 │ │scope │ │(数据同步)│       │
│  │          │ │         │ │引擎) │ │(数据 │ │          │       │
│  │          │ │         │ │      │ │权限) │ │          │       │
│  └──────────┘ └─────────┘ └──────┘ └──────┘ └──────────┘       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      leatop-cdp-micro                            │
│  ┌──────────┐ ┌──────────┐ ┌─────────┐ ┌───────┐ ┌───────┐    │
│  │micro-core│ │ gateway  │ │discovery│ │ limit │ │ track │    │
│  │(微服务   │ │(API网关) │ │(服务注册│ │(限流  │ │(链路  │    │
│  │ 核心)    │ │          │ │ 与发现) │ │ 熔断) │ │ 追踪) │    │
│  └──────────┘ └──────────┘ └─────────┘ └───────┘ └───────┘    │
│  ┌──────────┐ ┌──────────┐                                      │
│  │  config  │ │ monitor  │                                      │
│  │(配置中心)│ │(监控管理)│                                      │
│  └──────────┘ └──────────┘                                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     leatop-cdp-example                            │
│  demo1 | micro | vacation | sharding | fulltext | ai | lite     │
└─────────────────────────────────────────────────────────────────┘
```

**依赖规则：** 依赖方向只能从上到下，下层模块不能反向依赖上层模块。common 层是所有模块的基石；base 层依赖 common 层，提供跨业务域的基础设施；business 层依赖 common 和 base，实现具体业务域；micro 层依赖 common，提供微服务支撑。

---

## 三、设计模式目录

以下汇总 CDP 框架中反复使用的设计模式，每个模式列出其应用模块和核心参与类。

### 3.1 Strategy（策略模式）

框架中使用最广泛的模式，用于实现"可插拔实现"。

| 应用模块 | 策略接口 | 具体策略 | 用途 |
|----------|---------|---------|------|
| cache | CdpCacheClient | DefaultCdpCacheClient | 缓存后端切换（Caffeine / Redis） |
| lock | CdpLockClient | LocalCdpLockClient / RedissonCdpLockClient | 本地锁与分布式锁切换 |
| auth | AuthHandlerExecutor | ApiKeyAuthHandlerExecutor / SaSignAuthHandlerExecutor | 可插拔认证方式 |
| echo | LoadService | 各业务模块的 LoadService 实现 | 字典回显数据源 |
| stream | MessageOperations | DefaultMessageOperations | 消息发送抽象 |
| export | DataExportService / DataInsertService | DefaultDataExportServiceImpl / DefaultDataInsertServiceImpl | 导入导出数据处理 |
| message | MessageSender | SmsMessageSender / EmailMessageSender / WebHookMessageSender | 消息通道切换 |
| datascope | DataPermissionHandler | DefaultDataPermissionHandler | 数据权限条件生成 |
| common-core | DataMaskingStrategy | 各脱敏策略实现 | 数据脱敏算法 |
| common-data | ErrorCode | ErrorCodeEnum / 业务自定义实现 | 错误码体系 |
| file | FileStorage | LocalPlus / MinIO / AliyunOSS / Database 等 | 文件存储平台切换 |
| syncdata | UpdateService | NewUpdateServiceImpl / GzwUpdateServiceImpl | 外部系统数据适配 |
| job | ExecutorRouter | Round / Random / ConsistentHash / Failover 等 | 任务路由策略 |

### 3.2 Adapter（适配器模式）

用于桥接 CDP 接口与第三方库接口之间的语义差异。

| 应用模块 | Adapter 类 | 适配目标 | 用途 |
|----------|-----------|---------|------|
| cache | DefaultCdpCache | Spring Cache -> CdpCache | 扩展 Spring Cache 的 API 能力 |
| lock | RedissonCdpLock | Redisson RLock -> CdpLock | 统一锁契约 |
| auth | CdpStpInterface | SA-Token StpInterface | 反向权限模型适配 |
| jasypt | CustomEncryptor | Jasypt StringEncryptor -> SM4 | 国密算法桥接 |

### 3.3 Template Method（模板方法模式）

用于定义处理骨架，子类只需实现关键步骤。

| 应用模块 | 模板类 | 钩子方法 | 用途 |
|----------|-------|---------|------|
| common-core | BaseRequestFilter | doFilterInternal() | 过滤器公共逻辑（白名单、OPTIONS 跳过） |
| common-data | BasePo / CommonEntity / CommonInfoEntity | 字段填充由 BaseColumnsHandler 执行 | PO 基类层次 |
| stream | AbstractMessageConsumer | doProcess() / handleProcessFailure() | 消费者处理骨架 |
| workflow | BaseFlowBusiness<T> | 继承接口扩展特有方法 | CRUD 型 Business 通用操作骨架 |

### 3.4 Chain of Responsibility（责任链模式）

| 应用模块 | 链条组成 | 用途 |
|----------|---------|------|
| common-core | CheckCSRFFilter -> MDCRequestFilter -> RepeatedlyRequestFilter | Servlet 过滤器链 |
| micro-gateway | traceIdFilter -> SentinelGatewayFilter -> SaReactorFilter | 网关过滤器链 |
| auth | CdpAuthHandler 遍历 AuthHandlerExecutor 列表 | 认证策略链 |

### 3.5 Observer（观察者模式）

| 应用模块 | 事件/主题 | 监听者 | 用途 |
|----------|---------|-------|------|
| system-login | LoginEvent（Spring ApplicationEvent） | 日志记录等下游监听器 | 登录事件发布 |
| workflow | EventPo / EventvarPo 事件配置 | 业务系统事件处理器 | 流程事件驱动业务解耦 |
| micro-limit | Sentinel 规则变更 | SentinelGatewayRuleConfig 定时检测 | 规则本地持久化 |
| micro-gateway | LocalSaveApiDefinitionChangeObserver | API 分组变更时本地持久化 | 网关规则持久化 |

### 3.6 Bridge（桥接模式）

| 应用模块 | 抽象维度 | 实现维度 | 用途 |
|----------|---------|---------|------|
| stream | MessageProducer / MessageConsumer / MessageOperations | Spring Cloud Stream Binder（Kafka / RabbitMQ / RocketMQ） | 消息中间件抽象与实现分离 |

### 3.7 Factory（工厂模式）

| 应用模块 | 工厂类 | 产品 | 用途 |
|----------|-------|------|------|
| cache | DefaultCdpCacheClient | CdpCache 实例 | 缓存实例创建与注册 |
| lock | CdpLockClient | CdpLock 实例 | 锁实例创建 |
| message | MessageSenderHolder | MessageSender 实例 | 消息发送器缓存与生命周期管理 |

### 3.8 SPI 插件模式

| 应用模块 | SPI 接口 | 实现 | 用途 |
|----------|---------|------|------|
| flyway | org.flywaydb.core.extensibility.Plugin | DmSQLDatabaseType / OpenGaussSQLDatabaseType / KingbaseDatabaseType | 信创数据库适配 |
| sharding | ShardingSphereURLLoader | ClassPathURLLoader / AbsolutePathURLLoader / NacosURLLoader | 分库分表配置加载 |

### 3.9 其他模式

| 模式 | 应用模块 | 参与类 | 用途 |
|------|---------|-------|------|
| AOP + 注解驱动 | lock / echo / log / cotime | @Lock + LockAspect / @EchoResult + EchoResultAspect / @LogParam + LogAspect / @ComputeTime + ComputeTimeHandler | 声明式横切关注点 |
| Registry（注册表） | echo / message | EchoService.strategyMap / MessageSenderHolder.senders | 实例缓存与查找 |
| Delegation（委托） | stream / common-util | AbstractMessageProducer -> MessageOperations / BeanUtil -> Hutool BeanUtil | 间接引用便于测试和替换 |
| Marker Annotation（标记注解） | common-data | @BusinessService / @ServiceScope / @ChangeEvent / @EncryptField | 元数据声明式标记 |
| Composite Document | fulltext | budata(父) / workflow + attach + labelperm(子) | ES Parent-Child 文档关联 |

---

## 四、跨模块交互图

### 4.1 完整请求处理链路（单体模式）

```
外部 HTTP 请求
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│  Servlet Filter Chain                                        │
│  CheckCSRFFilter(-1) → MDCRequestFilter(0)                  │
│    → RepeatedlyRequestFilter(1)                              │
│  （设置 TRACE_ID、校验 CSRF、包装请求体可重复读）               │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  Spring MVC Interceptor                                      │
│  CdpSaInterceptor.preHandle()                                │
│    1. 清除 CdpTokenHolder / TenantContentHolder              │
│    2. 遍历 AuthHandlerExecutor 链（ApiKey / SaSign 等）       │
│    3. SA-Token 登录校验 + 反向权限检查                         │
│    4. Token 续期                                             │
│    5. 解析用户信息 → CdpTokenHolder / TenantContentHolder     │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  DataPermissionHandlerExecutors（全局数据权限拦截器）           │
│    → 写入 DataPermissionCondition 到 DataPermissionHolder     │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  Controller → BusinessImpl → Service → Mapper                │
│  （@DataPermission 注解触发 DataPermissionAdvisor）            │
│  （@LogParam 注解触发 LogAspect 异步记录日志）                  │
│  （@EchoResult 注解触发 EchoResultAspect 自动回显）            │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  MyBatis-Plus Interceptor 层                                  │
│  DataPermissionInterceptor — 读取 ThreadLocal 改写 SQL WHERE  │
│  CustomSelectFieldsInterceptor — 列权限，无权字段置 null       │
│  TenantLineInnerInterceptor — 租户隔离条件注入                 │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  数据库（MySQL / DM / GaussDB / KingBase / GBase / PolarDB）  │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  @RestControllerAdvice 层                                     │
│  GlobalResponseBodyHandler — @DataMasking 数据脱敏            │
│  GlobalExceptionHandler — 异常 → HTTP 状态码 + Message 映射    │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
                     HTTP 响应返回
```

### 4.2 微服务模式请求链路

```
外部请求
    │
    ▼
┌─────────────────────────────────────────────┐
│  Spring Cloud Gateway (WebFlux)             │
│  traceIdFilter → SentinelGatewayFilter      │
│    → SaReactorFilter (CdpFilterAuthStrategy)│
│    → 路由断言（Path / AntPath）              │
│    → 负载均衡转发 (lb://service-name)        │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│  Feign 调用链                                │
│  FeignClient 方法 → RequestInterceptor       │
│    → 注入 Token + MICRO_TOKEN + REQUEST_ID   │
│    → LoadBalancer 选择实例                    │
│    → HTTP 请求到目标服务                      │
└──────────────────┬──────────────────────────┘
                   ▼
            目标微服务的 Filter Chain
            （同单体模式的请求处理链路）
```

### 4.3 消息队列异步数据流

```
业务代码
    │
    ▼
AbstractMessageProducer.send()
    → MessageOperations.send(destination, payload)
      → DefaultMessageOperations 包装 MessageWrapper
        → StreamBridge.send() → Spring Cloud Stream Binder
          → Kafka / RabbitMQ / RocketMQ
                │
                ▼
AbstractMessageConsumer.accept(Message<T>)
    → 解包 payload + headers
      → doProcess()（业务子类实现）
        → handleProcessFailure()（失败处理）
```

---

## 五、核心扩展点汇总

以下表格汇总所有模块的主要扩展点，便于开发者快速找到自定义注入的方式。

### 5.1 公共层扩展点

| 接口/注解 | 所属模块 | 注册方式 | 扩展场景 |
|-----------|---------|---------|---------|
| `IUserHelper` | common-core | 实现接口，注册为 Spring Bean | 对接自定义用户体系（获取当前用户、登出等） |
| `PermissionManage` | common-core | 实现接口，注册为 Spring Bean | 自定义权限管理逻辑 |
| `ErrorCode` | common-data | 实现接口，定义业务领域错误码 | 扩展错误码体系，通过 Message.fail(ErrorCode) 使用 |
| `AuthHandlerExecutor` | auth | 实现接口，注册为 Spring Bean | 新增认证方式（如 CAS、OAuth2） |
| `DataMaskingStrategy` | common-core | 实现接口，注册为 Spring Bean | 自定义数据脱敏算法 |

### 5.2 基础设施层扩展点

| 接口/注解 | 所属模块 | 注册方式 | 扩展场景 |
|-----------|---------|---------|---------|
| `CdpLockClient` | lock | 实现接口，注册为 Spring Bean（替换默认） | 自定义锁实现（如 ZooKeeper 锁） |
| `CdpCacheClient` | cache | 实现接口，注册为 Spring Bean | 自定义缓存后端 |
| `MessageOperations` | stream | 实现接口，注册为 Spring Bean（@ConditionalOnMissingBean 让位） | 添加链路追踪 header、消息加密等 |
| `AbstractMessageConsumer.doProcess()` | stream | 继承抽象类 | 实现消息消费逻辑 |
| `AbstractMessageConsumer.handleProcessFailure()` | stream | 覆盖 protected 方法 | 自定义消费失败处理（写失败表、发告警） |
| `LoadService` | echo | 实现接口，注册为 Spring Bean | 新增字典回显数据源（用户表、组织表、远程调用等） |
| `DataExportService` | export | 实现接口，调用时传入 | 导出数据加工（脱敏、格式转换、字典翻译） |
| `DataInsertService` | export | 实现接口，调用时传入 | 自定义导入数据入库逻辑 |
| `PartitionKeyExtractorStrategy` | stream | 替换 messageIdExtractor Bean | 按业务主键分区 |
| `GraphService` | cotime | Ko-Time SPI 选择存储 | 切换耗时统计的存储后端（Memory / Redis / Database） |

### 5.3 业务层扩展点

| 接口/注解 | 所属模块 | 注册方式 | 扩展场景 |
|-----------|---------|---------|---------|
| `UserExtraDataService` | system-login | 实现接口，注册为 Spring Bean | 登录后注入用户扩展数据 |
| `AccessTokenService` | system-login | 实现接口，注册为 Spring Bean | 第三方 Token 校验（CAS、SSO 等） |
| `DataPermissionHandler` | datascope | 实现接口，注册为 Spring Bean 或通过 @DataPermission 引用 | 自定义数据权限条件生成 |
| `MessageSender` | message | 实现接口，注册为 Spring Bean | 新增消息通道（如推送、WebSocket） |
| `UpdateService` | syncdata | 实现接口，注册为 Spring Bean | 适配新的外部数据源 |
| `FileStorage` | file | 通过 x-file-storage 扩展注册 | 新增文件存储后端 |
| `@LogRecord` / `@LogParam` | log | 标注在方法上 | 声明式操作日志采集 |
| `@EchoResult` + `@Echo` | echo | 标注在方法/字段上 | 声明式字典回显 |
| `@DataPermission` | datascope | 标注在 Mapper 或 Service 方法上 | 方法级数据权限控制 |
| `@Lock` | lock | 标注在方法上 | 声明式分布式锁 |

### 5.4 微服务层扩展点

| 接口/注解 | 所属模块 | 注册方式 | 扩展场景 |
|-----------|---------|---------|---------|
| `SaFilterAuthStrategy` | micro-gateway | 实现接口，注册为 Spring Bean | 完全替换网关认证逻辑 |
| `CdpBlockExceptionHandler` | micro-limit | 替换 Bean | 自定义限流异常响应 |
| `AntPathRoutePredicateFactory` | micro-gateway | Spring Cloud Gateway 路由配置 | Ant 风格路径匹配 |

---

## 六、设计手册索引

### 公共层（leatop-cdp-common）

| 文件名 | 模块 | 简要描述 |
|--------|------|---------|
| [cdp-design-common-util.md](cdp-design-common-util.md) | common-util | 工具类库设计：加密安全、JSON/Bean、ID 生成、SpEL 等 |
| [cdp-design-common-data.md](cdp-design-common-data.md) | common-data | PO/DTO/QO 基类层次、统一响应 Message、异常分层、错误码体系 |
| [cdp-design-common-core.md](cdp-design-common-core.md) | common-core | 过滤器链、全局异常处理、数据脱敏、SPI 接口、ThreadLocal 上下文 |
| [cdp-design-auth.md](cdp-design-auth.md) | common-auth | SA-Token 集成、AuthHandlerExecutor 策略链、反向权限适配、ThreadLocal 上下文 |
| [cdp-design-jasypt.md](cdp-design-jasypt.md) | common-jasypt | 配置文件敏感信息 SM4 加密，Jasypt 桥接 |

### 基础设施层（leatop-cdp-base）

| 文件名 | 模块 | 简要描述 |
|--------|------|---------|
| [cdp-design-cache.md](cdp-design-cache.md) | base-cache | 多级缓存（Caffeine L1 + Redis L2）、Adapter/Factory 模式、功能开关 |
| [cdp-design-lock.md](cdp-design-lock.md) | base-lock | 分布式锁（Redisson/ReentrantLock）、Strategy/Adapter/AOP+SpEL |
| [cdp-design-stream.md](cdp-design-stream.md) | base-stream | 消息队列统一抽象、Bridge 模式、Template Method 消费者骨架 |
| [cdp-design-echo.md](cdp-design-echo.md) | base-echo | 字典回显组件、LoadService SPI、AOP 注解驱动、批量加载 |
| [cdp-design-job.md](cdp-design-job.md) | base-job | XXL-JOB 嵌入式调度中心、执行器注解驱动、路由策略 |
| [cdp-design-export.md](cdp-design-export.md) | base-export | 通用导入导出、Strategy 数据处理、EasyExcel + Jxls-POI 双引擎 |
| [cdp-design-flyway.md](cdp-design-flyway.md) | base-flyway | 数据库版本管理、信创数据库 SPI 插件、ShardingSphere 兼容 |
| [cdp-design-gen.md](cdp-design-gen.md) | base-gen | 代码生成器、Velocity 模板引擎、多数据源表结构读取 |
| [cdp-design-sharding.md](cdp-design-sharding.md) | base-sharding | 分库分表简化配置、ShardingSphere SPI URLLoader 替换 |
| [cdp-design-apikey.md](cdp-design-apikey.md) | base-apikey | API Key 认证、双认证策略、与 auth 模块统一集成 |
| [cdp-design-cotime.md](cdp-design-cotime.md) | base-cotime | 方法级耗时统计、Ko-Time 集成、多存储后端 |
| [cdp-design-transaction.md](cdp-design-transaction.md) | base-transaction | Seata AT 模式分布式事务参考实现 |

### 业务层（leatop-cdp-business）

| 文件名 | 模块 | 简要描述 |
|--------|------|---------|
| [cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md) | -- | 业务模块通用 5 子模块架构模式（api/service/controller/boot-starter/cloud-starter） |
| [cdp-design-system-login.md](cdp-design-system-login.md) | system-login | 多模式登录（密码/短信/邮箱）、Token 管理、安全策略、CAS 集成 |
| [cdp-design-system-user.md](cdp-design-system-user.md) | system-user | 用户全生命周期管理、用户-组织多对多模型、密码安全策略 |
| [cdp-design-system-role.md](cdp-design-system-role.md) | system-role | 角色管理与 RBAC 权限分配 |
| [cdp-design-system-tenant.md](cdp-design-system-tenant.md) | system-tenant | 多租户管理与隔离 |
| [cdp-design-system-org.md](cdp-design-system-org.md) | system-org | 组织机构树管理 |
| [cdp-design-system-resource.md](cdp-design-system-resource.md) | system-resource | 菜单与资源管理 |
| [cdp-design-system-dict.md](cdp-design-system-dict.md) | system-dict | 数据字典管理 |
| [cdp-design-system-setting.md](cdp-design-system-setting.md) | system-setting | 系统参数与安全设置 |
| [cdp-design-datascope.md](cdp-design-datascope.md) | datascope | 数据权限（行列双控）、SQL 改写、ThreadLocal 传递 |
| [cdp-design-log.md](cdp-design-log.md) | log | 四类审计日志、AOP 声明式采集、异步写入 |
| [cdp-design-file.md](cdp-design-file.md) | file | 附件管理、x-file-storage 多平台抽象、分片上传 |
| [cdp-design-message.md](cdp-design-message.md) | message | 消息通知、MessageSender 策略、多通道可插拔（SMS/Email/Webhook） |
| [cdp-design-report.md](cdp-design-report.md) | report | 报表引擎（Ureport2 + 永洪 BI）集成 |
| [cdp-design-form.md](cdp-design-form.md) | form | 自定义表单引擎、元数据驱动、设计态与运行态分离 |
| [cdp-design-fulltext.md](cdp-design-fulltext.md) | fulltext | 全文检索、ES Parent-Child 文档模型、权限内嵌检索 |
| [cdp-design-syncdata.md](cdp-design-syncdata.md) | syncdata | 数据同步、UpdateService 多源适配、异步两阶段同步 |

### 工作流（leatop-cdp-business-workflow）

| 文件名 | 模块 | 简要描述 |
|--------|------|---------|
| [cdp-design-workflow-process.md](cdp-design-workflow-process.md) | workflow-process | 流程定义与管理、模板域与实例域分离、事件驱动架构 |
| [cdp-design-workflow-task.md](cdp-design-workflow-task.md) | workflow-task | 任务处理（签收/办理/退回/转办/会签/催办） |
| [cdp-design-workflow-bill.md](cdp-design-workflow-bill.md) | workflow-bill | 流程发起与撤回（草稿/提交/结束） |
| [cdp-design-workflow-monitor.md](cdp-design-workflow-monitor.md) | workflow-monitor | 流程监控（挂起/恢复/强制结束/催办） |
| [cdp-design-workflow-config.md](cdp-design-workflow-config.md) | workflow-config | 工作流配置管理（分类/系统/模板配置） |

### 微服务层（leatop-cdp-micro）

| 文件名 | 模块 | 简要描述 |
|--------|------|---------|
| [cdp-design-micro-gateway.md](cdp-design-micro-gateway.md) | micro-gateway | API 网关（Spring Cloud Gateway）、分层过滤器链、Sentinel 限流集成 |
| [cdp-design-micro-config.md](cdp-design-micro-config.md) | micro-config | 配置中心（Nacos）、零代码封装、依赖聚合 |
| [cdp-design-micro-discovery.md](cdp-design-micro-discovery.md) | micro-discovery | 服务注册与发现（Nacos）、Feign Token 透传、MICRO_TOKEN 签名 |
| [cdp-design-micro-limit.md](cdp-design-micro-limit.md) | micro-limit | 限流熔断（Sentinel）、规则本地持久化、空文件容错 |
| [cdp-design-micro-track.md](cdp-design-micro-track.md) | micro-track | 链路追踪（Skywalking Agent）、零侵入、MDC Trace ID 协同 |
