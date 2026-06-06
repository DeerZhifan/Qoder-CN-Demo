# CDP 缓存模块设计手册

> 对应使用手册：[cdp-module-cache.md](../3-组件使用手册/cdp-module-cache.md)

## 一、设计目标与背景

企业应用中，缓存是提升系统吞吐量和降低数据库压力的关键手段。然而直接使用 Spring Cache 或 Redis/Caffeine 原生 API 存在以下问题：

1. **切换成本高** — 从 Redis 切换到 Caffeine（或反向）需要修改大量业务代码。
2. **序列化陷阱** — Spring Boot 默认的 JDK 序列化产生不可读的二进制数据，且 `java.time` 类型在 JSON 序列化时常出现格式不兼容。
3. **TTL 管理僵硬** — Spring Cache 的 `@Cacheable` 注解不支持在运行时按缓存实例指定不同的过期时间。
4. **API 粒度不足** — Spring `Cache` 接口缺少 `size()`、`containsKey()`、带类型转换的 `get()` 等常用操作。

CDP 缓存模块的设计原则：

- **一次编码，两种后端** — 通过统一抽象屏蔽 Redis 与 Caffeine 差异，配置切换即可。
- **显式优于隐式** — 提供 `@EnableCdpCaching` 功能开关注解，不自动侵入未使用缓存的应用。
- **渐进增强** — 既支持编程式 API（CdpCache），也兼容声明式注解（Spring `@Cacheable`），两者可混合使用。
- **安全的序列化默认值** — 内置 Jackson JSON 序列化，自动处理 `LocalDateTime` 等 Java 8+ 时间类型。

## 二、整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    业务代码                               │
│  ┌──────────────────┐   ┌────────────────────────────┐  │
│  │ CdpCacheClient   │   │ @Cacheable / @CacheEvict   │  │
│  │   .getCache()    │   │   (Spring Cache 注解)       │  │
│  └────────┬─────────┘   └──────────┬─────────────────┘  │
├───────────┼────────────────────────┼────────────────────┤
│           ▼                        ▼                     │
│  ┌─────────────────┐     ┌─────────────────────┐        │
│  │ DefaultCdpCache  │     │   CacheManager       │       │
│  │ (Adapter)        │◄────│ (Spring 管理)         │       │
│  └────────┬─────────┘     └────────┬────────────┘        │
│           │                        │                     │
│  ┌────────┴────────────────────────┴────────────┐        │
│  │          DefaultCdpCacheClient (Factory)       │       │
│  │  ┌─────────────────────────────────────────┐  │       │
│  │  │ ConcurrentMap<String, CdpCache> cacheMap │  │       │
│  │  └─────────────────────────────────────────┘  │       │
│  └───────────────────────────────────────────────┘       │
├──────────────────────────────────────────────────────────┤
│                  Spring Cache SPI                         │
│  ┌──────────────────────┐  ┌──────────────────────────┐  │
│  │  CaffeineCacheManager │  │  CustomRedisCacheManager  │ │
│  │  (本地缓存)            │  │  (分布式缓存)              │ │
│  └──────────────────────┘  └──────────────────────────┘  │
├──────────────────────────────────────────────────────────┤
│  Caffeine (JVM 内存)          Redis (网络)                │
└──────────────────────────────────────────────────────────┘
```

**层次划分：**

- **接口层**：CdpCache、CdpCacheClient — 面向业务的统一缓存契约。
- **实现层**：DefaultCdpCacheClient、DefaultCdpCache — 桥接 Spring CacheManager 与 CDP 接口。
- **基础设施层**：CacheAutoConfig — 自动配置 Redis 序列化、RedisTemplate、CacheManager 和 CdpCacheClient Bean。

## 三、核心设计模式

### 3.1 Adapter 模式 — DefaultCdpCache

DefaultCdpCache 是一个经典的对象适配器，将 Spring `Cache` 接口适配为 CDP 自定义的 `CdpCache` 接口。

**参与类：**
- **Target**：`CdpCache`（目标接口，提供 `put`/`get`/`remove`/`size`/`containsKey` 等方法）
- **Adaptee**：`org.springframework.cache.Cache`（Spring 原生缓存接口，方法签名不同）
- **Adapter**：`DefaultCdpCacheClient.DefaultCdpCache`（内部静态类，持有 `Cache` 实例并转发调用）

> 设计决策：选择 Adapter 而非继承 Spring Cache，是因为 CdpCache 需要提供 `size()`、`containsKey()`、带 loader 的 `get()` 等 Spring Cache 不具备的语义。适配器在转发基础操作的同时，通过类型判断（`instanceof RedisCache` / `CaffeineCache`）实现了 `size()` 等扩展方法。

### 3.2 Factory 模式 — DefaultCdpCacheClient

DefaultCdpCacheClient 充当缓存实例的工厂和注册中心。

**参与类：**
- **Factory**：`DefaultCdpCacheClient`
- **Product**：`CdpCache`（由 `getCache()` 方法创建并缓存）

缓存实例通过 `ConcurrentHashMap` 做本地注册表，保证同名缓存只创建一次。`getCache(name, time, timeUnit)` 重载方法支持在运行时动态创建具有自定义 TTL 的缓存实例，内部根据 CacheManager 类型（Caffeine 或 Redis）采用不同的创建策略：

- **Caffeine 路径**：通过 `Caffeine.newBuilder()` 构建原生缓存，再调用 `CaffeineCacheManager.registerCustomCache()` 注册。
- **Redis 路径**：通过反射调用 `RedisCacheManager` 的 `getDefaultCacheConfiguration()` 获取默认配置，覆盖 TTL 后调用 `createRedisCache()` 创建。

> 设计决策：使用反射调用 RedisCacheManager 的 protected 方法（`getDefaultCacheConfiguration`、`createRedisCache`），是因为 Spring 未暴露运行时动态创建带自定义 TTL 缓存的公开 API。CustomRedisCacheManager 通过将这两个 protected 方法提升为 public 可见性来简化反射调用，同时保持与 Spring 原生 RedisCacheManager 的兼容。

### 3.3 功能开关模式 — @EnableCdpCaching

通过 `@Import(CacheAutoConfig.class)` 实现按需激活，不使用缓存的应用不会加载任何缓存基础设施。

**参与类：**
- `EnableCdpCaching`：组合注解，触发 `CacheAutoConfig` 导入
- `CacheAutoConfig`：配置类，同时声明 `@EnableCaching` 激活 Spring Cache 注解支持

> 设计决策：没有使用 Spring Boot 的 `spring.factories` 自动装配（虽然文件存在），而是要求显式声明 `@EnableCdpCaching`。这样做的原因是缓存依赖 Redis 连接或 Caffeine 库，在没有配置的情况下自动加载会导致启动失败。显式开关让开发者明确知道缓存被激活。

## 四、关键类说明

| 类名 | 包 | 职责 | 设计角色 |
|------|-----|------|---------|
| `CdpCache` | `com.leatop.cdp.cache` | 缓存操作统一契约，定义 put/get/remove/clear/size 等方法 | 目标接口 (Target) |
| `CdpCacheClient` | `com.leatop.cdp.cache` | 缓存客户端契约，负责获取和管理 CdpCache 实例 | 抽象工厂接口 |
| `DefaultCdpCacheClient` | `com.leatop.cdp.cache.client` | CdpCacheClient 的默认实现，内含 ConcurrentMap 注册表和按类型分派的创建逻辑 | 具体工厂 + 注册中心 |
| `DefaultCdpCache` | `com.leatop.cdp.cache.client` (内部类) | 包装 Spring Cache 为 CdpCache，补充 size/containsKey/带 loader 的 get | 适配器 (Adapter) |
| `CustomRedisCacheManager` | `com.leatop.cdp.cache.client` | 继承 RedisCacheManager，将 protected 方法提升为 public，供动态创建缓存使用 | 可见性提升包装器 |
| `CacheAutoConfig` | `com.leatop.cdp.cache.config` | 核心自动配置：注册 RedisCacheConfiguration、RedisTemplate、CdpCacheClient | 自动配置入口 |
| `EnableCdpCaching` | `com.leatop.cdp.cache.annotation` | 功能开关注解，通过 @Import 导入 CacheAutoConfig | 功能开关 |
| `CacheProperties` / `CdpCacheProperties` | `com.leatop.cdp.cache.config` | 绑定 `spring.cache` 前缀的配置属性 | 配置承载 |
| `CacheNameTime` | `com.leatop.cdp.cache.constant` | record 类型，封装缓存名称 + 过期时间 + 时间单位的三元组 | 值对象 |
| `CacheNameTimeConstant` | `com.leatop.cdp.cache.constant` | 预定义常用过期时间的缓存名称常量 | 常量定义 |

## 五、扩展机制

### 5.1 替换 CdpCacheClient 实现

CacheAutoConfig 中 `CdpCacheClient` Bean 标注了 `@ConditionalOnMissingBean`，业务方可提供自定义实现：

```java
@Bean
public CdpCacheClient customCacheClient(CacheManager cacheManager) {
    return new MyMultiLevelCacheClient(cacheManager);
}
```

典型场景：实现真正的 L1 + L2 两级联动缓存（Caffeine 做 L1，Redis 做 L2，通过 Redis Pub/Sub 同步失效）。

### 5.2 替换 RedisTemplate 序列化

`redisTemplate` Bean 同样标注了 `@ConditionalOnMissingBean(name = "redisTemplate")`，可通过自定义同名 Bean 覆盖序列化策略。

### 5.3 动态 TTL 缓存

通过 `getCache(name, time, timeUnit)` 在运行时创建不同过期策略的缓存实例，无需预先在 YAML 中声明。创建后的实例会被缓存在 `ConcurrentMap` 中复用。

### 5.4 扩展 CacheManager 类型支持

当前 `createCdpCache()` 方法通过 `instanceof` 判断支持 `CaffeineCacheManager` 和 `RedisCacheManager`。如需支持其他 CacheManager（如 EhCache），可继承 `DefaultCdpCacheClient` 并覆盖创建逻辑。

## 六、模块协作

```
leatop-cdp-base-cache
    │
    ├── 依赖 ──► Spring Cache Abstraction (spring-context)
    ├── 依赖 ──► Spring Data Redis (spring-data-redis)
    ├── 依赖 ──► Caffeine (com.github.benmanes.caffeine)
    ├── 依赖 ──► Jackson (fasterxml.jackson) — 序列化
    ├── 依赖 ──► leatop-cdp-common-util — BeanUtil 反射工具
    │
    ├── 被依赖 ◄── leatop-cdp-business-system — 用户/角色/资源缓存
    ├── 被依赖 ◄── leatop-cdp-common-auth — 权限列表缓存（通过 PermissionManage）
    └── 被依赖 ◄── leatop-cdp-base-lock — 分布式锁依赖同一 Redis 连接
```

**数据流向：**

1. 应用启动时，`@EnableCdpCaching` 触发 `CacheAutoConfig` 加载。
2. CacheAutoConfig 创建 `RedisCacheConfiguration`（含 JSON 序列化器和 TTL 配置）、`RedisTemplate`、`CdpCacheClient`。
3. 业务代码通过 `CdpCacheClient.getCache(name)` 获取 `CdpCache` 实例，底层委托给 Spring `CacheManager`。
4. Spring `@Cacheable` 注解走标准 Spring Cache 代理路径，由同一个 `CacheManager` 管理，与编程式 API 共享同一批缓存实例。

## 七、设计权衡与约束

### 7.1 显式权衡

**单 CacheManager vs 多 CacheManager**：当前设计只注入一个 CacheManager，`spring.cache.type` 决定全局使用 Redis 还是 Caffeine。如果需要同一应用内同时使用两种缓存（如热数据用 Caffeine，共享数据用 Redis），需要自行注册多个 CacheManager 并通过 `@Qualifier` 区分。

> 设计决策：保持单 CacheManager 简化了配置和心智模型。大多数业务场景下，开发环境用 Caffeine、生产环境用 Redis 的切换模式已经足够。真正的两级缓存联动（L1 失效同步）属于高级场景，留给业务方扩展 CdpCacheClient 实现。

**反射调用 vs 公开 API**：`DefaultCdpCacheClient.createCdpCache()` 中通过 `BeanUtil.executeMethod()` 反射调用 RedisCacheManager 的 protected 方法。虽然 `CustomRedisCacheManager` 已经将这些方法提升为 public，但当前代码仍走反射路径。

> 设计决策：这是一个已知的技术债务。反射调用在 Spring 版本升级时可能断裂。后续应统一使用 CustomRedisCacheManager 的 public 方法替代反射。

### 7.2 已知限制

- **size() 精度问题**：Redis 缓存的 `size()` 通过 `puts - deletes` 统计值估算，不反映 TTL 过期后的真实条目数。Caffeine 的 `estimatedSize()` 同样是近似值。
- **缓存穿透防护缺失**：`get(key, loader, type)` 的 loader 不具备防击穿能力（无互斥锁），高并发场景下同一 key 的 loader 可能被多线程同时调用。
- **CacheProperties 重复定义**：`CacheProperties` 和 `CdpCacheProperties` 两个类绑定相同的 `spring.cache` 前缀，存在配置绑定冲突的风险，后续应合并为一个。

### 7.3 演进方向

- 引入 L1（Caffeine）+ L2（Redis）真正的两级缓存实现，通过 Redis Pub/Sub 实现 L1 失效广播。
- 为 `get(key, loader, type)` 增加分布式锁保护，防止缓存击穿。
- 支持缓存预热机制（应用启动时自动加载热点数据）。
- 将 `CustomRedisCacheManager` 的 public 方法直接调用替代反射路径。
