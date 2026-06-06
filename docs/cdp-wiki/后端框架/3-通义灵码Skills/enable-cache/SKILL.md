# 启用 CDP 缓存组件

## 描述

在已有 CDP 项目中启用缓存组件（`leatop-cdp-base-cache`），支持 Redis 分布式缓存和 Caffeine 本地缓存两种实现，可通过配置无感切换。同时与 Spring Cache 注解体系集成。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **缓存类型**（`redis` 或 `caffeine`，默认 `redis`）
3. **缓存名称**（预初始化的缓存名称，如 `app-cache`）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-base-cache` 提供统一缓存抽象，自动配置 CacheManager 和 CdpCacheClient。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-cache</artifactId>
</dependency>
```

## 步骤 2：添加启用注解

> `@EnableCdpCaching` 是 CDP 自定义的功能开关注解，内部通过 `@Import(CacheAutoConfig.class)` 激活缓存基础设施。必须显式声明，未声明时不会加载任何缓存 Bean。

在主启动类（或配置类）上添加：

```java
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@MapperScan("com.leatop.cdp.**.dao")
@EnableCdpCaching  // 开启 CDP 缓存
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 步骤 3：添加配置

> 以下为 Redis 缓存配置（推荐生产环境）。如选择 Caffeine，将 `type` 改为 `caffeine` 并添加 `caffeine.spec`。

在 `application-dev.yaml` 中添加：

**Redis 方式：**

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:172.17.1.28}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:!QAZ2wsx3edc}
      database: ${REDIS_DB:10}
  cache:
    type: redis
    cache-names: {缓存名称}
    redis:
      use-key-prefix: true
      key-prefix: "${spring.application.name}-"
      time-to-live: 100000
      cache-null-values: false
```

**Caffeine 方式：**

```yaml
spring:
  cache:
    type: caffeine
    cache-names: {缓存名称}
    caffeine:
      spec: initialCapacity=100,maximumSize=500,expireAfterWrite=300s
```

## 步骤 4：编写示例代码

> 提供两种使用方式的示例。方式一适合需要精细控制的场景，方式二适合标准 CRUD 缓存。

**方式一：CdpCacheClient 编程式操作**

```java
import com.leatop.cdp.cache.CdpCache;
import com.leatop.cdp.cache.CdpCacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DemoCacheService {

    @Autowired
    private CdpCacheClient cdpCacheClient;

    /**
     * Cache-Aside 模式示例
     */
    public String getValue(String key) {
        CdpCache cache = cdpCacheClient.getCache("{缓存名称}");
        String result = cache.get(key, String.class);
        if (result != null) {
            return result;
        }
        // 缓存未命中，从数据源获取
        result = loadFromSource(key);
        cache.put(key, result);
        return result;
    }

    /**
     * 带自定义 TTL 的缓存
     */
    public void cacheWithTtl(String key, Object value) {
        CdpCache cache = cdpCacheClient.getCache("shortLivedCache", 5, java.util.concurrent.TimeUnit.MINUTES);
        cache.put(key, value);
    }

    private String loadFromSource(String key) {
        // 实际业务逻辑
        return "value-" + key;
    }
}
```

**方式二：Spring Cache 注解**

```java
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "{缓存名称}")
public class DemoAnnotationService {

    @Cacheable(key = "#id", unless = "#result==null")
    public OrderDto getById(String id) {
        // 首次调用查库，后续直接走缓存
        return orderDao.selectById(id);
    }

    @CachePut(key = "#dto.id")
    public OrderDto update(OrderDto dto) {
        orderDao.updateById(dto);
        return dto;
    }

    @CacheEvict(key = "#id")
    public void delete(String id) {
        orderDao.deleteById(id);
    }
}
```

## 步骤 5：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 日志中出现 `CacheAutoConfig` 初始化信息
3. 调用示例方法，第二次调用确认走缓存（无数据库 SQL 日志）

---

## 完成后提醒

1. 切换缓存类型（Redis 与 Caffeine）只需修改 `spring.cache.type`，业务代码无需改动
2. 不要直接注入 `RedisTemplate` 绕过缓存抽象层，应统一使用 `CdpCacheClient` 或 Spring Cache 注解
3. 使用 `@Cacheable` 注解时，修改数据后必须配合 `@CacheEvict` 或 `@CachePut` 清除/更新缓存
4. `getCache(name, time, timeUnit)` 可在运行时动态创建不同 TTL 的缓存实例，适用于差异化过期策略
