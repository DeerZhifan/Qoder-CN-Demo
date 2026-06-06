---
trigger: when_referenced
knowledge_source:
  - cdp-design-lock
  - cdp-module-lock
  - 10-lock-usage.java
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-lock` 依赖
- 使用 `@EnableCdpLock` 注解
- 使用 `CdpLockClient`、`CdpLock` 接口
- 使用 `@Lock` 注解进行声明式加锁

---

## 前置依赖

1. Maven 依赖：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-lock</artifactId>
</dependency>
```

2. 启动类添加注解：

```java
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@EnableCdpLock  // 必须显式声明，不会自动启用
public class Application { ... }
```

3. 使用 Redis 分布式锁时需配置 `spring.data.redis` 连接信息，本地锁无需额外配置。

---

## 配置要点

### 本地锁（默认，单 JVM 场景）

```yaml
spring:
  lock:
    type: local  # 使用 JVM 本地 ReentrantLock，matchIfMissing=true 可省略
```

### Redis 分布式锁（集群 / 多实例场景）

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:172.17.1.28}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DB:10}
  lock:
    type: redis  # 使用基于 Redisson 的分布式锁
```

- `spring.lock.type` 取值 `local`（默认）或 `redis`，两者互斥。
- 锁模块支持独立 Redis 连接：配置 `spring.lock.redis.*` 时使用专用 Redis 实例，否则回退到 `spring.data.redis` 全局配置。
- Redis 部署模式（单机/哨兵/集群）自动检测，无需额外配置。

---

## 代码模式

### 推荐写法

**方式一：@Lock 注解 + SpEL 动态 key（推荐简单场景）**

```java
@Service
public class OrderService {

    // 按参数加锁，waitTime=1 表示等待 1 秒获取不到锁则放弃
    @Lock(key = "#userId", waitTime = 1)
    public String submitOrder(String userId) {
        // 同一 userId 的请求串行执行
        return "下单成功";
    }

    // 不指定 key 时使用全局锁（类名:方法名）
    @Lock
    public void globalSerialTask() {
        // 全局只有一个线程在执行此方法
    }
}
```

**方式二：CdpLockClient 编程式加锁（需要精细控制超时逻辑）**

```java
@Service
public class OrderService {

    @Autowired
    private CdpLockClient cdpLockClient;

    public String submitOrderWithFallback(String userId) throws InterruptedException {
        CdpLock lock = cdpLockClient.getLock("order:submit:" + userId);
        boolean acquired = lock.tryLock(1, TimeUnit.SECONDS);
        if (!acquired) {
            return "操作频繁，请稍后重试";
        }
        try {
            // 执行业务逻辑
            return "下单成功";
        } finally {
            lock.unlock();  // 必须在 finally 中释放锁
        }
    }
}
```

**锁 key 命名规范**：使用 `业务前缀:业务ID` 格式，如 `order:submit:userId:123`，避免不同业务间的锁冲突。

**@Lock 注解 key 组合规则**：最终 key 格式为 `类全限定名:方法名:SpEL表达式求值结果`，框架自动添加类名和方法名前缀。

### 禁止事项

- **禁止直接使用 `RedissonClient` 操作锁** -- 必须通过 `CdpLockClient` 或 `@Lock` 注解，统一走框架封装
- **禁止在未添加 `@EnableCdpLock` 的情况下注入 `CdpLockClient`** -- 会导致 Bean 找不到
- **禁止编程式加锁时忽略 `finally` 块** -- `unlock()` 必须在 `finally` 中调用，否则锁无法释放
- **禁止在非 Spring Bean 类上使用 `@Lock` 注解** -- AOP 代理仅对 Spring 容器管理的 Bean 生效
- **禁止同类内部方法调用时依赖 `@Lock`** -- Spring AOP 代理不拦截自调用，锁不会生效
- **禁止在编程式锁 key 中使用无业务前缀的简单名称** -- 会导致不同业务间的锁冲突
