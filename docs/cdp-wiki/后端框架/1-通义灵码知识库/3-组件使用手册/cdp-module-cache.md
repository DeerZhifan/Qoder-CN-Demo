# 如何使用 CDP 缓存组件

## 概述

缓存组件（`leatop-cdp-base-cache`）提供统一的缓存 API，支持 Redis 分布式缓存和 Caffeine 本地缓存两种实现，可通过配置无感切换。同时与 Spring Cache 注解体系集成。

## 启用方式

**1. 添加 Maven 依赖：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-cache</artifactId>
</dependency>
```

**2. 在启动类添加注解：**

```java
@SpringBootApplication
@EnableCdpCaching  // 开启 CDP 缓存
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

## 核心 API

### CdpCacheClient — 缓存客户端

```java
public interface CdpCacheClient {
    CdpCache getCache(String name);                              // 获取缓存实例
    CdpCache getCache(String name, int time, TimeUnit timeUnit); // 获取带自定义 TTL 的缓存实例
    Collection<String> getCacheNames();                          // 获取所有缓存名称
}
```

### CdpCache — 缓存操作接口

```java
public interface CdpCache {
    Object put(String key, Object value);                            // 写入缓存，返回旧值
    Object get(String key);                                          // 读取缓存
    <T> T get(String key, Class<T> type);                            // 读取并转型
    <T> T get(String key, Function<String, T> loader, Class<T> type); // 读取，不存在则通过 loader 加载并缓存
    Object remove(String key);                                       // 删除缓存
    void clear();                                                    // 清空缓存
    boolean containsKey(String key);                                 // 判断 key 是否存在
    int size();                                                      // 缓存条目数量
}
```

## 配置项

### Redis 缓存配置（推荐生产环境使用）

```yaml
spring:
  data:
    redis:
      host: 172.17.1.96
      port: 6380
      password: a123456
      database: 10
  cache:
    cache-names: demo1-cache,demo2-cache   # 预初始化的缓存名称
    type: redis                            # 缓存类型
    redis:
      use-key-prefix: true                 # 是否启用 key 前缀
      key-prefix: "demo1-"                 # key 前缀
      time-to-live: 100000                 # 过期时间（毫秒）
      enable-statistics: true              # 是否开启统计
      cache-null-values: false             # 是否缓存 null 值
```

### Caffeine 本地缓存配置

```yaml
spring:
  cache:
    cache-names: demo1-cache,demo2-cache
    type: caffeine
    caffeine:
      spec: initialCapacity=100,maximumSize=8,expireAfterWrite=100000s
```

Caffeine spec 参数说明：`initialCapacity`（初始容量）、`maximumSize`（最大条数）、`expireAfterWrite`（写入后过期时间）、`expireAfterAccess`（访问后过期时间）、`recordStats`（开启命中率统计）。

## 使用示例

### 方式一：通过 CdpCache API 操作

```java
import com.leatop.cdp.cache.CdpCache;
import com.leatop.cdp.cache.CdpCacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    @Autowired
    private CdpCacheClient cdpCacheClient;

    public String testCache(String op, String key, Object value) {
        CdpCache cdpCache = cdpCacheClient.getCache("DemoCacheName");
        Object result = switch (op) {
            case "put" -> cdpCache.put(key, value);
            case "get" -> cdpCache.get(key);
            case "size" -> cdpCache.size();
            case "remove" -> cdpCache.remove(key);
            default -> null;
        };
        return result == null ? null : result.toString();
    }
}
```

### 方式二：使用自定义 TTL 缓存

```java
import com.leatop.cdp.cache.CdpCache;
import com.leatop.cdp.cache.CdpCacheClient;
import java.util.concurrent.TimeUnit;

// 创建 24 小时过期的缓存
CdpCache appCache = cdpCacheClient.getCache("appToken", 24, TimeUnit.HOURS);
appCache.put(appId, tokenValue);
String token = appCache.get(appId, String.class);
```

### 方式三：Spring Cache 注解

```java
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "userCache")
public class UserService {

    @Cacheable(key = "#id", unless = "#result==null")
    public UserDTO getUser(Long id) {
        return userMapper.selectById(id);
    }

    @CachePut(key = "#dto.id", unless = "#result==null")
    public UserDTO updateUser(UserDTO dto) {
        userMapper.updateById(dto);
        return dto;
    }

    @CacheEvict(key = "#id")
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
```

## 注意事项

> 注意：切换缓存类型（Redis ↔ Caffeine）只需修改 `spring.cache.type` 配置，业务代码无需改动。

> 注意：不要直接操作 `RedisTemplate` 绕过缓存抽象层，应统一使用 `CdpCacheClient` 或 Spring Cache 注解。

> 注意：使用 `@Cacheable` 注解时，修改数据后必须配合 `@CacheEvict` 或 `@CachePut` 清除/更新缓存，否则会读到旧数据。

> 注意：`getCache(name, time, timeUnit)` 可在运行时动态创建不同 TTL 的缓存实例，适用于需要差异化过期策略的场景。
