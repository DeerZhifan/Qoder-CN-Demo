# 如何使用 CDP 分布式锁组件

## 概述

锁组件（`leatop-cdp-base-lock`）提供统一的锁抽象，支持 JVM 本地锁（ReentrantLock）和 Redis 分布式锁（Redisson）两种实现，通过配置无感切换。支持注解式和编程式两种使用方式。

## 启用方式

**1. 添加 Maven 依赖：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-lock</artifactId>
</dependency>
```

**2. 在启动类添加注解：**

```java
@SpringBootApplication
@EnableCdpLock  // 开启 CDP 锁
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

## 核心 API

### CdpLockClient — 锁客户端

```java
public interface CdpLockClient {
    CdpLock getLock(String name);  // 获取指定名称的锁对象
}
```

### CdpLock — 锁接口（extends java.util.concurrent.locks.Lock）

```java
public interface CdpLock extends Lock {
    String getName();  // 获取锁名称
    // 继承 Lock 接口：lock(), tryLock(), tryLock(long, TimeUnit), unlock() 等
}
```

### @Lock — 注解式加锁

```java
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Lock {
    String key() default "";              // 锁 key，支持 SpEL 表达式
    int expire() default 60;             // 锁过期时间（秒）
    int waitTime() default 5;            // 等待获取锁的时间（秒）
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    int lockTime() default 60;           // 锁超时时间（秒）
}
```

## 配置项

### 本地锁（默认，单 JVM 场景）

```yaml
spring:
  lock:
    type: local  # 使用 JVM 本地 ReentrantLock
```

### Redis 分布式锁（集群/多实例场景）

```yaml
spring:
  data:
    redis:
      host: 172.17.1.96
      port: 6379
      password: a123456
      database: 10
  lock:
    type: redis  # 使用基于 Redisson 的分布式锁
```

> 注意：Redis 锁自动检测 Redis 部署模式（单机/哨兵/集群），无需额外配置。

## 使用示例

### 方式一：@Lock 注解 + SpEL 动态 key

```java
import com.leatop.cdp.lock.annotation.Lock;
import org.springframework.stereotype.Service;

@Service
public class TestLockService {

    private final Map<String, Integer> counter = new HashMap<>();

    // 根据方法参数 key 加锁，每个不同的 key 使用独立的锁
    @Lock(key = "#key", waitTime = 1)
    public Integer testLock(String key) {
        int num = counter.getOrDefault(key, 0);
        num = num + 1;
        counter.put(key, num);
        return num;
    }
}
```

### 方式二：@Lock 注解全局锁

```java
// 不指定 key 时，使用 "类名:方法名" 作为锁 key
@Lock
public Integer testLock2() {
    num = num + 1;
    return num;
}
```

### 方式三：编程式加锁

```java
import com.leatop.cdp.lock.CdpLock;
import com.leatop.cdp.lock.CdpLockClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    @Autowired
    private CdpLockClient cdpLockClient;

    public void processOrder(String orderId) throws InterruptedException {
        CdpLock cdpLock = cdpLockClient.getLock("order:" + orderId);
        boolean isLock = cdpLock.tryLock(1, TimeUnit.SECONDS);
        if (isLock) {
            try {
                // 需要加锁保护的业务逻辑
            } finally {
                cdpLock.unlock();  // 必须在 finally 中释放锁
            }
        }
    }
}
```

## 注意事项

> 注意：切换锁类型（local ↔ redis）只需修改 `spring.lock.type` 配置，业务代码无需改动。

> 注意：禁止直接使用 `RedissonClient` 操作锁，统一使用 `CdpLockClient` 或 `@Lock` 注解。

> 注意：编程式加锁时，必须在 `finally` 块中调用 `unlock()`，否则锁无法释放。

> 注意：`@Lock` 注解的 SpEL key 最终组合规则为 `类全限定名:方法名:SpEL表达式求值结果`。

> 注意：获取锁超时后会抛出 `LockFailException`（RuntimeException），可被全局异常处理器捕获。

> 注意：锁 key 命名建议使用"业务前缀:业务ID"格式，如 `order:create:123`，避免不同业务间的锁冲突。
