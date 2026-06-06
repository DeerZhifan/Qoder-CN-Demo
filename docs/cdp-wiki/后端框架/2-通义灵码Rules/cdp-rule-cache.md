---
trigger: when_referenced
knowledge_source:
  - cdp-design-cache
  - cdp-module-cache
  - 09-cache-usage
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-cache` 依赖
- 使用 `@EnableCdpCaching` 注解
- 使用 `CdpCacheClient`、`CdpCache` 接口
- 使用 Spring Cache 注解（`@Cacheable`、`@CacheEvict`、`@CachePut`）

---

## 前置依赖

1. Maven 依赖：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-cache</artifactId>
</dependency>
```

2. 启动类添加注解：

```java
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@EnableCdpCaching  // 必须显式声明，不会自动启用
public class Application { ... }
```

3. Redis 连接配置（生产环境必须）或 Caffeine 依赖（本地开发可选）。

---

## 配置要点

### Redis 缓存（推荐生产环境）

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:172.17.1.28}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DB:10}
  cache:
    type: redis                           # 缓存后端类型
    cache-names: app-cache                # 预初始化的缓存名称（逗号分隔）
    redis:
      use-key-prefix: true                # 启用 key 前缀避免冲突
      key-prefix: "${spring.application.name}-"
      time-to-live: 100000                # 默认 TTL（毫秒）
      cache-null-values: false            # 不缓存 null 值
```

### Caffeine 本地缓存（开发环境）

```yaml
spring:
  cache:
    type: caffeine
    cache-names: app-cache
    caffeine:
      spec: initialCapacity=100,maximumSize=500,expireAfterWrite=300s
```

切换缓存后端只需修改 `spring.cache.type`，业务代码无需改动。

---

## 代码模式

### 推荐写法

**方式一：CdpCacheClient 编程式 API（需要精细控制时使用）**

```java
@Service
public class OrderService {

    @Autowired
    private CdpCacheClient cdpCacheClient;

    /**
     * Cache-Aside 模式：先查缓存，未命中再查库并写入缓存
     */
    public OrderDto getOrder(String id) {
        CdpCache cache = cdpCacheClient.getCache("orderCache");
        OrderDto result = cache.get(id, OrderDto.class);
        if (result != null) {
            return result;
        }
        result = queryFromDb(id);
        if (result == null) {
            throw new BusException("订单不存在");
        }
        cache.put(id, result);
        return result;
    }

    /**
     * 带自定义 TTL 的缓存（运行时动态创建）
     */
    public String getToken(String appId) {
        CdpCache tokenCache = cdpCacheClient.getCache("tokenCache", 24, TimeUnit.HOURS);
        return tokenCache.get(appId, String.class);
    }
}
```

**方式二：Spring Cache 注解（推荐，简洁）**

```java
@Service
@CacheConfig(cacheNames = "userCache")
public class UserService {

    @Cacheable(key = "#id", unless = "#result==null")
    public UserDto getById(String id) { ... }

    @CachePut(key = "#dto.id", unless = "#result==null")
    public UserDto update(UserDto dto) { ... }

    @CacheEvict(key = "#id")
    public void delete(String id) { ... }
}
```

两种方式可混合使用，共享同一个 CacheManager。

### 禁止事项

- **禁止直接注入 `RedisTemplate`** -- 必须通过 `CdpCacheClient` 或 Spring Cache 注解操作缓存
- **禁止在未添加 `@EnableCdpCaching` 的情况下注入 `CdpCacheClient`** -- 会导致 Bean 找不到
- **禁止在 `@Cacheable` 方法内部调用同类的其他 `@Cacheable` 方法** -- Spring AOP 代理不生效，缓存不会命中
- **禁止忽略缓存一致性** -- 修改数据后必须配合 `@CacheEvict` 或 `cache.remove()` 清除旧缓存
- **禁止在循环中逐条调用 `cache.get()`** -- 应批量查询后一次性写入缓存
- **禁止硬编码缓存名称字符串散落各处** -- 应定义为常量或使用 `@CacheConfig` 统一管理
