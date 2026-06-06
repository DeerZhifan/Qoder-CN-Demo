# CDP 框架架构总览

> 本文档面向使用 CDP 框架进行业务开发的工程师，帮助快速理解框架整体架构、模块职责和核心机制。

---

## 一、项目概述

CDP（通用开发平台）是基于以下技术栈构建的企业级 Java 框架：

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.5.12 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| MyBatis-Plus | 3.5.15 |
| SA-Token | 1.44.0 |
| Redisson | 3.29.0 |
| ShardingSphere | 5.5.2 |

当前版本：`1.0.3-SNAPSHOT`

采用分层多模块 Maven 架构，同一套业务代码可支持**单体部署**和**微服务部署**两种模式，通过切换 Starter 依赖即可完成。

---

## 二、模块依赖顺序

各模块遵循严格的依赖顺序，**禁止循环依赖**：

```
leatop-cdp-dependencies   →  第三方库版本管理
       ↓
leatop-cdp-bom            →  内部模块版本管理
       ↓
leatop-cdp-common         →  横切关注点：工具类、过滤器、认证、异常、启动器
       ↓
leatop-cdp-base           →  基础设施：缓存、锁、消息流、任务调度、导出、分库分表
       ↓
leatop-cdp-business       →  业务域：系统管理、日志、文件、消息、工作流、全文检索等
       ↓
leatop-cdp-micro          →  微服务支撑：配置中心、服务发现、网关、限流、链路追踪
       ↓
leatop-cdp-example        →  参考示例应用
```

> 注意：依赖方向只能从上到下，下层模块不能反向依赖上层模块。

---

### 2.1 leatop-cdp-common（公共模块）

提供全框架共享的基础能力，共 6 个子模块：

| 子模块 | 职责 |
|--------|------|
| `leatop-cdp-common-util` | 通用工具类 |
| `leatop-cdp-common-core` | 核心抽象（过滤器、全局异常处理器、请求处理） |
| `leatop-cdp-common-data` | 数据模型（Message 响应体、异常类、枚举） |
| `leatop-cdp-common-auth` | 认证授权（SA-Token 集成、用户体系实现） |
| `leatop-cdp-common-starter` | Spring Boot 自动配置启动器 |
| `leatop-cdp-common-jasypt` | 配置文件敏感信息加密（Jasypt） |

### 2.2 leatop-cdp-base（基础设施模块）

提供跨业务域的基础设施能力，共 12 个子模块：

| 子模块 | 职责 |
|--------|------|
| `leatop-cdp-base-cache` | 多级缓存（Caffeine L1 + Redis L2） |
| `leatop-cdp-base-lock` | 分布式锁（Redisson） |
| `leatop-cdp-base-stream` | 消息队列统一抽象（Kafka / RabbitMQ / RocketMQ） |
| `leatop-cdp-base-job` | 分布式任务调度（XXL-Job） |
| `leatop-cdp-base-export` | 数据导出 |
| `leatop-cdp-base-flyway` | 数据库迁移（Flyway） |
| `leatop-cdp-base-gen` | 代码生成 |
| `leatop-cdp-base-sharding` | 分库分表（ShardingSphere） |
| `leatop-cdp-base-transaction` | 事务管理 |
| `leatop-cdp-base-apikey` | API Key 认证 |
| `leatop-cdp-base-cotime` | 协同时间工具 |
| `leatop-cdp-base-echo` | 健康检查 |

**leatop-cdp-base-stream 内部结构：**

```
leatop-cdp-base-stream/
├── leatop-cdp-base-stream-common    # 统一抽象层（MessageOperations 接口）
├── leatop-cdp-base-stream-kafka     # Kafka 实现
├── leatop-cdp-base-stream-rabbit    # RabbitMQ 实现
├── leatop-cdp-base-stream-rocket    # RocketMQ 实现
└── leatop-cdp-base-stream-example   # 示例
```

**leatop-cdp-base-job 内部结构：**

```
leatop-cdp-base-job/
├── leatop-cdp-base-job-api            # 任务接口定义
├── leatop-cdp-base-job-core           # 任务引擎核心
├── leatop-cdp-base-job-admin          # 任务管理后台
├── leatop-cdp-base-job-controller     # REST 接口
├── leatop-cdp-base-job-boot-starter   # 单体部署启动器
└── leatop-cdp-base-job-cloud-starter  # 微服务部署启动器
```

### 2.3 leatop-cdp-business（业务域模块）

包含 10 个业务域，每个业务模块内部结构一致（见第四节）：

| 业务模块 | 职责 |
|----------|------|
| `leatop-cdp-business-system` | 系统管理（用户、角色、菜单、组织机构、登录） |
| `leatop-cdp-business-log` | 操作审计日志 |
| `leatop-cdp-business-file` | 附件/文件管理 |
| `leatop-cdp-business-message` | 消息通知管理 |
| `leatop-cdp-business-report` | 报表管理 |
| `leatop-cdp-business-form` | 自定义表单 |
| `leatop-cdp-business-workflow` | 工作流引擎 |
| `leatop-cdp-business-fulltext` | 全文检索（Elasticsearch） |
| `leatop-cdp-business-datascope` | 数据权限控制 |
| `leatop-cdp-business-syncdata` | 数据同步 |

### 2.4 leatop-cdp-micro（微服务支撑模块）

| 子模块 | 职责 |
|--------|------|
| `leatop-cdp-micro-core` | 微服务核心基础设施 |
| `leatop-cdp-micro-config` | 分布式配置管理（Nacos） |
| `leatop-cdp-micro-discovery` | 服务注册与发现（Nacos） |
| `leatop-cdp-micro-gateway` | API 网关（Spring Cloud Gateway） |
| `leatop-cdp-micro-limit` | 限流熔断（Sentinel） |
| `leatop-cdp-micro-monitor` | 监控管理 |
| `leatop-cdp-micro-track` | 分布式链路追踪 |

---

## 三、双部署模式

CDP 框架支持同一套业务代码在两种部署模式下运行：

| 部署模式 | 引入的 Starter | 特点 |
|----------|---------------|------|
| **单体部署** | `*-boot-starter` | 所有模块打成一个 JAR，直接运行 |
| **微服务部署** | `*-cloud-starter` | 各服务独立部署，集成 Nacos 注册中心和 Spring Cloud Gateway |

**切换方式：**

1. 修改 `pom.xml` 中的 Starter 依赖（`boot-starter` ↔ `cloud-starter`）
2. 调整 `application.yaml` 配置（如添加 Nacos 地址）
3. 业务代码（Controller / Service / Mapper）**无需任何改动**

> 注意：boot-starter 和 cloud-starter 不能同时引入，同一个部署只选其一。

---

## 四、业务模块内部结构

每个业务模块统一遵循以下五层结构：

```
leatop-cdp-business-xxx/
├── leatop-cdp-business-xxx-api            # 接口定义、DTO、QO、枚举、Feign 客户端
├── leatop-cdp-business-xxx-service        # 业务实现、Mapper 接口、PO 实体、DAO XML
├── leatop-cdp-business-xxx-controller     # REST 接口（Controller 层）
├── leatop-cdp-business-xxx-boot-starter   # 单体部署自动配置
└── leatop-cdp-business-xxx-cloud-starter  # 微服务部署自动配置
```

**各层职责：**

- **api** — 对外暴露的接口契约，其他模块通过依赖 api 模块进行调用。包含 DTO（数据传输对象）、QO（查询参数对象）、枚举定义。
- **service** — 核心业务逻辑实现。包含 PO（数据库实体）、MyBatis Mapper 接口和 XML 映射文件。
- **controller** — REST API 端点，只做参数校验和 Service 调用，不包含业务逻辑。
- **boot-starter** — 单体部署的 Spring Boot 自动配置类。
- **cloud-starter** — 微服务部署的自动配置类，额外集成服务注册与发现。

> 注意：部分模块有额外子模块，如 `business-system` 多了 `system-login`（登录逻辑），`business-fulltext` 多了 `fulltext-datatool`（数据索引工具）。

---

## 五、请求处理链

HTTP 请求进入应用后，依次经过以下过滤器（按 `@Order` 值从小到大执行）：

```
HTTP Request
    ↓
CheckCSRFFilter (@Order(-1))     ← 安全防护：校验 Origin/Referer 防止 CSRF 攻击
    ↓
MDCRequestFilter (@Order(0))     ← 链路追踪：设置 MDC Trace ID，用于日志关联
    ↓
RepeatedlyRequestFilter (@Order(1)) ← 请求体可重复读：包装 Request 使 Body 可多次读取
    ↓
Spring DispatcherServlet → Controller → Service → Mapper
    ↓
GlobalResponseBodyHandler       ← 响应后处理：数据脱敏（@DataMasking 注解驱动）
    ↓
GlobalExceptionHandler          ← 全局异常处理（如有异常）
    ↓
HTTP Response
```

所有过滤器继承自 `BaseRequestFilter`（`com.leatop.cdp.core.filter`），自动跳过静态资源和 OPTIONS 预检请求。

**全局异常处理器**（`com.leatop.cdp.core.handler.GlobalExceptionHandler`）处理的异常类型：

| 异常类型 | HTTP 状态码 | 说明 |
|----------|------------|------|
| `BusException` | 400 | 业务异常（参数校验失败、数据不存在等） |
| `UncheckedException` | 400 | 运行时异常，通过 code 字段返回错误码 |
| `UnauthorizedException` | 401 | 认证/授权失败 |
| `NoResourceFoundException` | 404 | 资源不存在 |
| `ServiceUncheckedException` | 500 | 服务级异常 |
| `Exception`（兜底） | 500 | 未预期异常 |

**统一响应格式**（`com.leatop.cdp.data.message.Message<T>`）：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... }
}
```

---

## 六、核心功能开关注解

在应用主类上声明以下注解以启用对应功能：

```java
@SpringBootApplication(scanBasePackages = {"com.leatop.example"})
@MapperScan(basePackages = {"com.leatop.example.dao"})
@EnableCdpCaching       // 启用多级缓存（Caffeine L1 + Redis L2）
@EnableCdpLock          // 启用分布式锁（Redisson）
@EnableXxlJobExecutor   // 启用 XXL-Job 任务调度执行器
@EnableAdminServer      // 启用 Spring Boot Admin 监控
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

| 注解 | 所在包 | 导入的配置类 |
|------|--------|-------------|
| `@EnableCdpCaching` | `com.leatop.cdp.cache.annotation` | `CacheAutoConfig` |
| `@EnableCdpLock` | `com.leatop.cdp.lock.annotation` | `CdpLockAutoConfig` |
| `@EnableXxlJobExecutor` | `com.xxl.job.core.config` | `XxlJobExecutorConfig` |
| `@EnableAdminServer` | `de.codecentric.boot.admin.server.config` | Spring Boot Admin Server |

---

## 七、核心基础设施抽象

### 7.1 缓存（leatop-cdp-base-cache）

- **启用方式：** 主类加 `@EnableCdpCaching`
- **架构：** L1 Caffeine（本地极速） + L2 Redis（分布式共享）
- **编程接口：** 与 Spring Cache 注解一致（`@Cacheable`、`@CacheEvict`、`@CachePut`）
- **编程式访问：** 注入 `CdpCacheClient`（`com.leatop.cdp.cache.CdpCacheClient`），调用 `getCache(name)` 获取 `CdpCache` 实例

> 注意：不要直接操作 `RedisTemplate` 绕过缓存抽象层。集群部署时 L1 各节点独立，修改数据后必须 `@CacheEvict`。

### 7.2 分布式锁（leatop-cdp-base-lock）

- **启用方式：** 主类加 `@EnableCdpLock`
- **注解式：** 在方法上加 `@Lock(key = "order:create:#userId", expire = 60)`
- **编程式：** 注入 `CdpLockClient`（`com.leatop.cdp.lock.CdpLockClient`），调用 `getLock(name)` 获取 `CdpLock` 实例
- **实现切换：** 配置 `spring.lock.type=local`（本地锁）或不配置（默认 Redisson 分布式锁）

> 注意：禁止直接使用 `RedissonClient` 操作锁，统一走框架封装。

### 7.3 消息队列（leatop-cdp-base-stream）

- **统一接口：** `MessageOperations`（`com.leatop.cdp.base.stream.common.MessageOperations`）
- **发送消息：** 注入 `MessageOperations`，调用 `send(destination, message)`
- **底层实现：** 基于 Spring Cloud Stream 的 `StreamBridge`，通过 Profile 切换 Kafka / RabbitMQ / RocketMQ

> 注意：业务代码使用框架统一抽象，不直接依赖 `KafkaTemplate` 等具体实现类。

### 7.4 异常体系

| 异常类 | 包路径 | 使用场景 |
|--------|--------|----------|
| `BusException` | `com.leatop.cdp.data.exception` | 业务校验失败（用户不存在、参数不合法） |
| `UncheckedException` | `com.leatop.cdp.data.exception` | 运行时错误（外部服务调用失败） |
| `ServiceUncheckedException` | `com.leatop.cdp.data.exception` | 服务级系统错误 |

> 注意：不要在 Controller/Service 中 try-catch 后吞掉异常，不要直接抛 `RuntimeException`，使用框架异常类型。

### 7.5 用户体系（认证与授权）

- **认证框架：** SA-Token 1.44.0（轻量级 JWT 方案）
- **用户接口：** `IUserHelper`（`com.leatop.cdp.core.api.IUserHelper`）
- **默认实现：** `SysUserHelper`（`com.leatop.cdp.auth.api.SysUserHelper`）
- **权限模型：** RBAC + 数据权限范围控制（`leatop-cdp-business-datascope`）
- **敏感配置：** 通过 Jasypt 加密（`leatop-cdp-common-jasypt`）

**IUserHelper 常用方法：**

```java
CurrentUserDto getCurrentUserInfo();   // 获取当前登录用户信息
String getCurrentUserId();             // 获取当前用户 ID
String getTenantId();                  // 获取租户 ID
boolean isLogin();                     // 判断是否已登录
boolean logout();                      // 登出
```

> 注意：获取用户信息统一通过 `IUserHelper` 接口，不要直接操作 SA-Token API。

---

## 八、配置管理

应用采用分层 YAML Profile 结构，按需组合激活：

```yaml
spring:
  profiles:
    active:
      - dev          # 环境：dev / local / test
      - kafka        # 消息队列
      - sba          # Spring Boot Admin
      - apikey       # API Key 认证
```

**配置文件清单：**

| 配置文件 | 用途 |
|----------|------|
| `application.yaml` | 基础配置（端口 28080、Flyway、MyBatis-Plus、日志） |
| `application-dev.yaml` | 开发环境（DB/Redis 地址 172.17.1.28） |
| `application-local.yaml` | 本地开发（localhost） |
| `application-mysql.yaml` | MySQL 数据库适配 |
| `application-dm.yaml` | 达梦数据库适配 |
| `application-gauss.yaml` | GaussDB 适配 |
| `application-kingbase.yaml` | KingBase 适配 |
| `application-gbase.yaml` | GBase 适配 |
| `application-polardb.yaml` | PolarDB 适配 |
| `application-kafka.yaml` | Kafka 消息队列配置 |
| `application-fulltext.yaml` | Elasticsearch 全文检索 + MinIO 存储 |
| `application-sba.yaml` | Spring Boot Admin 监控 |
| `application-apikey.yaml` | API Key 认证 |
| `application-sharding.yaml` | ShardingSphere 分库分表 |

所有配置项支持环境变量覆盖：`${VAR_NAME:默认值}`。

---

## 九、数据库支持与迁移

### 支持的数据库

MySQL、Oracle、GaussDB、达梦（DM）、KingBase、GBase、PolarDB

### ORM 框架

- MyBatis-Plus 3.5.15，含自定义类型处理器
- 连接池：Druid（开发环境默认 initial:1, min:1, max:20）
- SQL 监控：P6SPY

### Flyway 数据库迁移

**迁移脚本目录：** 各模块 `resources/db/migration/{vendor}/`

```
db/migration/
├── mysql/           # MySQL 脚本
├── dm/              # 达梦脚本
├── gauss/           # GaussDB 脚本
├── gbase/           # GBase 脚本
└── kingbase/        # KingBase 脚本
```

**脚本命名规则：**

| 类型 | 命名格式 | 示例 |
|------|----------|------|
| 版本化迁移 | `V{版本}__{描述}.sql` | `V1.1.1__report_create.sql` |
| 可重复迁移 | `R__{描述}.sql` | `R__data_init.sql` |

框架还提供 Java 版迁移基类 `BaseInitCdpData`（`db.migration.BaseInitCdpData`），支持 SQL 文件自动发现和 CRC32 校验。

> 注意：Flyway 开发环境默认启用（`baseline-on-migrate: true`），可通过 `FLYWAY_ENABLED` 环境变量控制。

---

## 十、CI/CD 流水线

`.gitlab-ci.yml` 定义三个阶段：

```
┌─────────┐     ┌────────────────┐     ┌──────────┐
│  Build  │ ──→ │ SonarQube Scan │ ──→ │  Deploy  │
└─────────┘     └────────────────┘     └──────────┘
```

| 阶段 | 动作 |
|------|------|
| **Build** | `mvn clean install`（4 线程并行），执行单元测试，生成 Smart-doc 文档，打包可执行 JAR |
| **SonarQube** | 代码质量扫描（可通过 `ignore_scan=yes` 跳过） |
| **Deploy** | 将 JAR 复制至 Docker 目录，通过 SSH 部署到 172.17.1.28 |

**分支对应关系：**

| 分支 | 部署目标 |
|------|----------|
| `test` | demo1 单体应用 |
| `develop-nacos` | 微服务架构 |

---

## 十一、示例应用一览

| 示例模块 | 用途 | 端口 |
|----------|------|------|
| `leatop-cdp-example-demo1` | 全功能单体示例（推荐入门参考） | 28080 |
| `leatop-cdp-example-micro` | 微服务架构示例（gateway + provider + consumer + usercenter） | 28081/28085 |
| `leatop-cdp-example-vacation` | 工作流引擎示例（请假审批流程） | - |
| `leatop-cdp-example-sharding` | 分库分表示例 | - |
| `leatop-cdp-example-fulltext` | 全文检索示例（Elasticsearch） | 28080 |
| `leatop-cdp-example-ai` | AI 平台集成示例（DeepSeek） | - |
| `leatop-cdp-example-lite` | 轻量级示例（最小依赖） | - |
| `leatop-cdp-example-adapter` | 数据适配器示例 | - |
| `leatop-cdp-example-doc` | API 文档生成示例 | - |
| `leatop-job-executor-sample` | XXL-Job 执行器示例 | - |

> 注意：`leatop-cdp-example-demo1` 是最完整的参考实现，新项目建议以此为模板。

---

## 十二、依赖版本管理

通过 BOM 模式统一管理版本，子模块声明依赖时**不填写版本号**：

- `leatop-cdp-dependencies` — 管理所有第三方库版本
- `leatop-cdp-bom` — 管理所有内部模块版本

**新增第三方依赖的流程：**

1. 在 `leatop-cdp-dependencies/pom.xml` 的 `<dependencyManagement>` 中声明版本
2. 在需要的子模块 `pom.xml` 中引用依赖（不写 `<version>`）

> 注意：禁止在子模块中直接指定第三方库版本号，否则会导致版本冲突。

---

## 十三、自动配置机制

框架使用 Spring Boot 3.x 标准的自动配置方式：

- **配置注册文件：** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **默认配置填充：** `CdpDefaultConfigFillListener` 监听 `ApplicationEnvironmentPreparedEvent`，自动设置 Jasypt 加密器、PageHelper 方言等默认值
- **部分模块保留 `spring.factories`** 用于注册 `ApplicationListener`（如 Gateway、Sentinel 模块）

每个 Starter 模块（boot-starter / cloud-starter）都包含自动配置类，引入依赖即自动生效，无需手动配置。
