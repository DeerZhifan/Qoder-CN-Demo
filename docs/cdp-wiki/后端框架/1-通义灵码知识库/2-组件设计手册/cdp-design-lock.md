# CDP 分布式锁模块设计手册

> 对应使用手册：[cdp-module-lock.md](../3-组件使用手册/cdp-module-lock.md)

## 一、设计目标与背景

CDP 框架支持单体与微服务两种部署模式，锁组件必须在这两种模式下都能正常工作。单体部署时，JVM 内的 ReentrantLock 已经足够；微服务或多实例部署时，则需要跨进程的分布式锁。

设计目标如下：

1. **统一抽象** -- 业务代码面向接口编程，不感知底层锁实现，切换部署模式时零代码修改。
2. **声明式与编程式并存** -- 简单场景通过 `@Lock` 注解一行搞定，复杂场景可编程式获取锁对象，灵活控制锁的生命周期。
3. **按需激活** -- 通过 `@EnableCdpLock` 注解显式开启，未启用时不引入任何 AOP 代理和 Redis 连接，避免资源浪费。
4. **动态锁粒度** -- 利用 SpEL 表达式根据方法参数动态生成锁 key，支持细粒度的并发控制。

## 二、整体架构

模块包结构如下：

```
com.leatop.cdp.lock
├── CdpLock                          # 锁抽象接口（extends java.util.concurrent.locks.Lock）
├── CdpLockClient                    # 锁客户端接口（工厂角色）
├── annotation/
│   ├── EnableCdpLock                # 功能开关注解
│   └── Lock                         # 方法级锁注解
├── aop/
│   └── LockAspect                   # AOP 切面，拦截 @Lock 注解
├── client/
│   ├── LocalCdpLockClient           # 本地锁实现（ReentrantLock）
│   └── RedissonCdpLockClient        # 分布式锁实现（Redisson）
├── config/
│   ├── CdpLockAutoConfig            # 自动配置入口
│   ├── CdpLockProperties            # 配置属性绑定
│   └── RedisLockConfiguration       # Redis/Redisson 连接配置
└── exception/
    └── LockFailException            # 获取锁失败异常
```

整体调用链路为：`@Lock 注解 -> LockAspect -> CdpLockClient -> CdpLock 实现`。编程式使用时直接注入 CdpLockClient，跳过 AOP 层。

## 三、核心设计模式

### 3.1 Strategy 模式：可插拔的锁实现

`CdpLockClient` 是策略接口，定义了唯一的工厂方法 `getLock(String name)`。两个实现类分别封装不同的锁机制：

- **LocalCdpLockClient** -- 内部维护一个 `ConcurrentHashMap<String, CdpLock>` 作为锁注册表，每个 key 对应一个 `LocalCdpLock`（继承自 ReentrantLock）。锁释放时自动从 Map 中移除，避免内存泄漏。创建锁时通过 `synchronized` 块保证同名锁只创建一次。
- **RedissonCdpLockClient** -- 持有 RedissonClient 引用，每次 `getLock` 调用委托给 `redisson.getLock(name)` 获取 RLock，再包装为 `RedissonCdpLock` 适配器。

> 设计决策：选择 Strategy 模式而非继承体系，是因为锁的创建逻辑（工厂）与锁本身的行为（Lock 接口）是两个独立的变化维度。CdpLockClient 负责"如何创建锁"，CdpLock 负责"锁怎么用"，职责分离清晰。

### 3.2 Adapter 模式：统一锁契约

`CdpLock` 接口继承 `java.util.concurrent.locks.Lock`，仅额外增加了 `getName()` 方法。两个实现类分别适配不同的底层锁：

- `LocalCdpLock` 直接继承 `ReentrantLock`，天然实现了 Lock 接口的全部方法。
- `RedissonCdpLock` 持有 Redisson 的 `RLock` 对象，将 Lock 接口的每个方法委托给 RLock 的对应方法。

> 设计决策：CdpLock 继承标准 Lock 接口而非自定义新接口，使得熟悉 JUC 的开发者可以零学习成本使用，同时保留了与 JUC 生态（如 Condition）的兼容性。

### 3.3 AOP + SpEL：声明式锁获取

`LockAspect` 是一个 Java Record 类型的 AOP 切面，围绕 `@Lock` 注解进行环绕通知。其核心流程：

1. **提取方法元信息** -- 通过 `MethodSignature` 获取 Method 对象、参数名和参数值。
2. **构建 SpEL 变量表** -- 将方法参数以 `参数名 -> 参数值` 的形式放入 Map。
3. **解析锁 key** -- 调用 `SpringELUtils.convertToTemplate()` 将原始 key 转换为 SpEL 模板格式（`#{...}`），再通过 `SpringELUtils.parseEL()` 求值。最终 key 格式为 `类全限定名:方法名:SpEL求值结果`。
4. **获取锁并执行** -- 通过 CdpLockClient 获取 CdpLock，调用 `tryLock(waitTime, timeUnit)` 尝试加锁。成功则执行目标方法并在 finally 中释放锁；失败则抛出 `LockFailException`。

> 设计决策：LockAspect 使用 record 类型声明，CdpLockClient 通过构造器注入，确保不可变性。锁 key 采用"类名:方法名:SpEL 值"三段式组合，既避免不同类的同名方法冲突，又支持参数级别的细粒度锁。

### 3.4 条件装配：@EnableCdpLock 与 @ConditionalOnProperty

模块的激活分为两层：

- **第一层：`@EnableCdpLock`** -- 标注在启动类上，通过 `@Import(CdpLockAutoConfig.class)` 触发整个锁模块的 Bean 注册。不加此注解，锁模块完全不加载。
- **第二层：`spring.lock.type` 属性** -- 在 `CdpLockAutoConfig` 内部，`LocalLockConfiguration` 和 `RedisLockConfiguration` 分别通过 `@ConditionalOnProperty` 条件装配。`local` 为默认值（`matchIfMissing = true`），`redis` 需要显式配置。两者互斥，同一时刻只有一个 CdpLockClient Bean 生效。

`CdpLockAutoConfig` 同时注册 `LockAspect` Bean，接收唯一的 CdpLockClient 实例，这样无论底层是本地锁还是 Redis 锁，切面的行为完全一致。

## 四、关键类说明

| 类名 | 角色 | 核心职责 |
|------|------|---------|
| `CdpLock` | 锁抽象接口 | 继承 JUC Lock，增加 getName()，统一上层调用契约 |
| `CdpLockClient` | 锁工厂接口 | 根据 name 创建或获取锁实例，Strategy 模式的策略接口 |
| `LocalCdpLockClient` | 本地锁工厂 | 基于 ConcurrentHashMap + ReentrantLock，单 JVM 场景 |
| `LocalCdpLock` | 本地锁实现 | 继承 ReentrantLock，unlock 时自动从 Map 清除 |
| `RedissonCdpLockClient` | 分布式锁工厂 | 委托 RedissonClient 创建 RLock 并包装为 CdpLock |
| `RedissonCdpLock` | 分布式锁适配器 | 将 CdpLock 接口方法逐一委托给 Redisson RLock |
| `LockAspect` | AOP 切面 | 拦截 @Lock 注解，解析 SpEL key，自动加锁/解锁 |
| `CdpLockAutoConfig` | 自动配置 | 条件装配 LocalLockConfiguration 或 RedisLockConfiguration |
| `RedisLockConfiguration` | Redis 配置 | 根据 Spring RedisProperties 自动创建 RedissonClient，支持单机/哨兵/集群 |
| `CdpLockProperties` | 配置属性 | 绑定 `spring.lock.*` 前缀，包含 type 和可选的 redis 子属性 |
| `LockFailException` | 异常 | 获取锁超时时抛出，继承 RuntimeException |

## 五、扩展机制

### 5.1 自定义锁实现

实现 `CdpLockClient` 接口即可接入新的锁后端（如 ZooKeeper、etcd）。需要做的是：

1. 编写一个新的 CdpLockClient 实现类，在 `getLock` 方法中返回适配了 CdpLock 接口的锁对象。
2. 编写对应的 `@Configuration` 类，使用 `@ConditionalOnProperty(name = "spring.lock.type", havingValue = "xxx")` 进行条件装配。
3. 将新的 Configuration 类加入 `CdpLockAutoConfig` 的 `@Import` 列表。

### 5.2 Redis 部署模式自适应

`RedisLockConfiguration` 内的 `getRedissonConfig()` 方法依次检测 RedisProperties 中的配置：优先单机模式（host/url 非空），其次哨兵模式（sentinel 配置存在），最后集群模式（cluster 配置存在）。这种链式判断使得切换 Redis 部署拓扑只需修改 YAML 配置。

### 5.3 独立 Redis 连接

`CdpLockProperties` 支持嵌套的 `redis` 子属性。当 `spring.lock.redis` 存在时，锁模块使用独立的 Redis 连接配置；否则回退到 Spring Boot 全局的 `spring.data.redis` 配置。这允许锁服务使用专用的 Redis 实例，避免与缓存等组件争抢连接。

## 六、模块协作

- **leatop-cdp-common-util** -- LockAspect 依赖 `SpringELUtils` 解析 SpEL 表达式。这是锁模块唯一的内部模块依赖，体现了模块间最小依赖原则。
- **leatop-cdp-common-core** -- 全局异常处理器可捕获 `LockFailException`，将其转换为标准 HTTP 错误响应。
- **Spring AOP** -- LockAspect 基于 spring-boot-starter-aop 提供的代理机制工作，要求被 `@Lock` 标注的方法所在类必须是 Spring Bean。
- **Redisson** -- 作为 Redis 锁的底层实现，由 `RedisLockConfiguration` 管理其生命周期。Redisson 的看门狗机制（watchdog）为持有锁的线程自动续期，防止业务执行超时导致锁提前释放。

## 七、设计权衡与约束

1. **本地锁的内存管理** -- `LocalCdpLockClient` 在 `unlock()` 时从 Map 中移除锁对象。这意味着同一个 key 在并发场景下可能被多次创建和销毁。如果 key 的基数非常大（如每个用户一把锁），Map 的 GC 压力可控；但如果出现只加锁不解锁的 bug，Map 会持续增长。

2. **LockAspect 不支持 lockTime/expire 参数** -- `@Lock` 注解定义了 `expire` 和 `lockTime` 属性，但当前 LockAspect 实现中仅使用了 `waitTime` 和 `timeUnit`，调用的是 `tryLock(waitTime, timeUnit)` 两参数版本。锁的自动过期依赖 Redisson 的 watchdog 默认行为（30秒续期），本地锁则无过期概念。这是一个有意为之的简化：避免在本地锁场景引入定时器的复杂度。

3. **注解式锁的异常处理** -- LockAspect 将 `tryLock` 的 InterruptedException 和目标方法的异常统一包装为 `RuntimeException` 抛出。这简化了调用方的异常处理，但丢失了检查型异常的语义。需要精细异常控制的场景建议使用编程式锁。

4. **锁 key 的命名空间** -- 注解式锁的 key 自动包含类名和方法名前缀，保证了跨类的唯一性。但编程式使用 `CdpLockClient.getLock(name)` 时，key 完全由调用方控制，需要开发者自行规范命名以避免冲突。

5. **单一锁类型** -- 当前只支持 Redisson 的普通可重入锁（RLock）。如需读写锁（RReadWriteLock）、公平锁（RFairLock）或红锁（RedLock），需扩展 CdpLockClient 接口增加对应的工厂方法。
