# 启用 CDP 分布式锁

## 描述

在已有 CDP 项目中启用分布式锁组件（`leatop-cdp-base-lock`），支持 JVM 本地锁（ReentrantLock）和 Redis 分布式锁（Redisson）两种实现，通过配置无感切换。提供 `@Lock` 注解式和 `CdpLockClient` 编程式两种使用方式。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **锁类型**（`local` 或 `redis`，默认 `local`；多实例部署必须使用 `redis`）
3. **使用方式**（`annotation` 注解式 或 `programmatic` 编程式，默认 `annotation`）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-base-lock` 提供统一锁抽象，自动配置 CdpLockClient 和 LockAspect。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-lock</artifactId>
</dependency>
```

## 步骤 2：添加启用注解

> `@EnableCdpLock` 是 CDP 自定义的功能开关注解，内部通过 `@Import(CdpLockAutoConfig.class)` 激活锁基础设施。必须显式声明，未声明时不会加载任何锁相关 Bean。

在主启动类（或配置类）上添加：

```java
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@MapperScan("com.leatop.cdp.**.dao")
@EnableCdpLock  // 开启 CDP 分布式锁
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 步骤 3：添加配置

> 本地锁为默认模式，无需额外配置即可使用。Redis 分布式锁需要配置 Redis 连接信息。

在 `application-dev.yaml` 中添加：

**本地锁方式（默认，可省略）：**

```yaml
spring:
  lock:
    type: local
```

**Redis 分布式锁方式：**

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:172.17.1.28}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:!QAZ2wsx3edc}
      database: ${REDIS_DB:10}
  lock:
    type: redis
```

> 切换锁类型只需修改 `spring.lock.type`，业务代码无需改动。Redis 部署模式（单机/哨兵/集群）自动检测。

## 步骤 4：编写示例代码

> 提供两种使用方式的示例。注解式适合简单的方法级互斥，编程式适合需要自定义超时处理的场景。

**方式一：@Lock 注解 + SpEL 动态 key**

```java
import com.leatop.cdp.lock.annotation.Lock;
import org.springframework.stereotype.Service;

@Service
public class DemoLockService {

    /**
     * 按参数 key 加锁，waitTime=1 表示等待 1 秒获取不到锁则放弃
     * key 支持 SpEL：#userId 取参数值
     */
    @Lock(key = "#userId", waitTime = 1)
    public String submitOrder(String userId) {
        // 同一 userId 的请求串行执行
        return "下单成功";
    }

    /**
     * 不指定 key 时使用全局锁（类名:方法名 作为锁 key）
     */
    @Lock
    public void globalSerialTask() {
        // 全局只有一个线程在执行此方法
    }
}
```

**方式二：CdpLockClient 编程式加锁**

```java
import com.leatop.cdp.lock.CdpLock;
import com.leatop.cdp.lock.CdpLockClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class DemoProgrammaticLockService {

    @Autowired
    private CdpLockClient cdpLockClient;

    public String submitOrderWithFallback(String userId) throws InterruptedException {
        CdpLock lock = cdpLockClient.getLock("order:submit:" + userId);
        boolean acquired = lock.tryLock(1, TimeUnit.SECONDS);
        if (!acquired) {
            // 获取锁失败，返回友好提示
            return "操作频繁，请稍后重试";
        }
        try {
            // 执行业务逻辑
            return "下单成功";
        } finally {
            // 必须在 finally 中释放锁
            lock.unlock();
        }
    }
}
```

## 步骤 5：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 日志中出现 `CdpLockAutoConfig` 初始化信息
3. 调用加锁方法，并发请求时确认锁生效（同一 key 的请求串行执行）
4. 锁超时时抛出 `LockFailException`，可被全局异常处理器捕获

---

## 完成后提醒

1. 切换锁类型（local 与 redis）只需修改 `spring.lock.type`，业务代码无需改动
2. 禁止直接使用 `RedissonClient` 操作锁，统一通过 `CdpLockClient` 或 `@Lock` 注解
3. 编程式加锁时，`unlock()` 必须在 `finally` 块中调用，否则锁无法释放
4. 锁 key 命名建议使用 `业务前缀:业务ID` 格式，如 `order:submit:userId:123`，避免不同业务间的锁冲突
5. `@Lock` 注解仅对 Spring Bean 生效，且不支持同类内部方法调用（AOP 代理限制）
