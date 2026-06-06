# CDP 框架常见问题 Q&A

> 本文档为通义灵码知识库专用，覆盖 CDP 框架开发中的高频问题。

---

## 一、模块架构与项目结构

### Q1: CDP 框架的模块依赖顺序是什么？

CDP 采用分层多模块 Maven 架构，各模块遵循严格的依赖顺序，禁止循环依赖：

```
leatop-cdp-dependencies  （第三方库版本管理）
  └─ leatop-cdp-bom           （内部模块版本管理）
       └─ leatop-cdp-common        （横切关注点：过滤器、认证、异常、启动器）
            └─ leatop-cdp-base          （基础设施：缓存、锁、消息流、任务、导出、分库分表）
                 └─ leatop-cdp-business      （业务域：系统、日志、文件、消息、工作流等）
                      └─ leatop-cdp-micro         （微服务：配置中心、服务发现、网关、限流、链路追踪）
                           └─ leatop-cdp-example       （参考示例应用）
```

> 注意：上层模块可依赖下层模块，反向依赖或跨层依赖是不允许的。

---

### Q2: 新建一个业务模块需要创建哪些子模块？

每个业务模块需要创建以下子模块：

```
leatop-cdp-business-xxx/
├── leatop-cdp-business-xxx-api          # 接口定义、DTO、QO、枚举、Business 接口
├── leatop-cdp-business-xxx-service      # 业务实现、Mapper（DAO）、PO
├── leatop-cdp-business-xxx-controller   # REST 接口（Controller）
├── leatop-cdp-business-xxx-boot-starter # 单体部署自动配置
└── leatop-cdp-business-xxx-cloud-starter # 微服务部署自动配置（含 Feign 客户端）
```

- `api` 模块放接口定义和数据模型，不包含实现逻辑
- `service` 模块放业务实现，DAO 继承 MyBatis-Plus 的 `BaseMapper`
- `controller` 模块放 REST 接口，只做参数校验和 Service 调用
- `boot-starter` 通过 `@ComponentScan` + `@MapperScan` 注册所有组件
- `cloud-starter` 通过 `@EnableFeignClients` 提供远程调用能力

---

### Q3: 如何切换单体和微服务部署模式？

CDP 同一套业务代码支持单体和微服务两种部署模式，切换步骤：

**单体部署** — 引入 `*-boot-starter`：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-system-boot-starter</artifactId>
</dependency>
```

**微服务部署** — 替换为 `*-cloud-starter`：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-system-cloud-starter</artifactId>
</dependency>
```

两者区别：`boot-starter` 直接引入 Service 实现和 Mapper，所有逻辑在同一进程运行；`cloud-starter` 引入 Feign 客户端，通过 HTTP 调用远程服务。

> 注意：业务代码（Controller/Service/Mapper）无需修改，仅改 pom 依赖和 application.yaml 配置。

---

### Q4: PO、DTO、QO 分别在什么场景使用？

| 类型 | 全称 | 使用场景 | 典型特征 |
|------|------|----------|----------|
| PO | Persistent Object | Mapper/DAO 层输入输出 | 带 `@TableName`、`@TableId`、`@TableField` 注解，继承 `BasePo` |
| DTO | Data Transfer Object | Controller 入参/出参、Service 层传递 | 无数据库注解，可包含计算字段和关联数据 |
| QO | Query Object | 列表查询条件封装 | 继承 `PageQo`（含分页参数），只包含筛选字段 |

示例 — PO 定义：

```java
package com.leatop.example.model.po;

import com.baomidou.mybatisplus.annotation.*;
import com.leatop.cdp.data.po.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("dsunit_application")
public class ApplicationPO extends BasePo<ApplicationPO> {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("app_name")
    private String appName;

    @TableField("app_type")
    private String appType;
}
```

> 注意：PO、DTO、QO 不要混用。Controller 层不应直接返回 PO 对象，需转换为 DTO。

---

### Q5: 如何添加新的第三方 Maven 依赖？

添加新依赖必须遵循 BOM 版本管理规范：

1. 在 `leatop-cdp-dependencies/pom.xml` 的 `<dependencyManagement>` 中声明版本
2. 在需要使用的子模块 `pom.xml` 中引用依赖，**不写版本号**

```xml
<!-- 第一步：在 leatop-cdp-dependencies/pom.xml 声明版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>example-lib</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 第二步：在子模块 pom.xml 引用（不写版本号） -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>example-lib</artifactId>
</dependency>
```

> 注意：禁止在子模块 pom.xml 中直接指定版本号，所有版本由 `leatop-cdp-dependencies` 统一管理。内部模块版本由 `leatop-cdp-bom` 管理。

---

### Q6: Controller、Service、Mapper 各层的职责划分是什么？

```
Controller 层：参数校验（@Validated）+ 调用 Service，不写业务逻辑
Service 层：  业务逻辑实现，调用 Mapper 或其他 Service
Mapper 层：   数据库操作，使用 MyBatis-Plus，复杂 SQL 写 XML
```

Controller 示例：

```java
package com.leatop.example.controller;

import com.leatop.cdp.data.vo.Message;
import com.leatop.example.model.dto.NewsItemDto;
import com.leatop.example.service.NewsItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/news_research/news_item")
public class NewsItemController {

    @Autowired
    private NewsItemService newsItemService;

    @PostMapping("/add")
    public Message<Boolean> add(@RequestBody @Validated(AddGroup.class) NewsItemDto dto) {
        return newsItemService.add(dto);
    }

    @GetMapping("/queryById")
    public Message<NewsItemDto> queryById(@RequestParam String id) {
        return newsItemService.queryById(id);
    }
}
```

> 注意：Controller 中不要出现数据库操作、业务判断等逻辑，这些统一放在 Service 层。

---

## 二、缓存模块

### Q7: 如何在 CDP 框架中启用缓存？

1. 在主应用类上添加 `@EnableCdpCaching` 注解
2. 在 `application.yaml` 中配置缓存参数

```java
package com.leatop.example;

import com.leatop.cdp.cache.annotation.EnableCdpCaching;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.leatop.example"})
@EnableCdpCaching
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

配置项（`application-dev.yaml`）：

```yaml
spring:
  cache:
    cache-names: demo1-cache,demo2-cache
    type: redis          # 可选：redis / caffeine
    redis:
      use-key-prefix: true
      key-prefix: "demo1-"
      time-to-live: 100000   # 毫秒
      enable-statistics: true
      cache-null-values: false
```

---

### Q8: 如何使用 Spring Cache 注解为查询方法添加缓存？

在 Service 方法上使用标准 Spring Cache 注解：

```java
package com.leatop.example.service.impl;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "user")
public class UserServiceImpl implements UserService {

    // 查询时缓存结果，key 为方法参数 id
    @Cacheable(key = "#id", unless = "#result==null")
    public UserDTO getUser(String id) {
        return userDAO.selectById(id);
    }

    // 更新时同步更新缓存
    @CachePut(key = "#dto.id", unless = "#result==null")
    public UserDTO updateUser(UserDTO dto) {
        // 更新逻辑
        return dto;
    }

    // 删除时清除缓存
    @CacheEvict(key = "#id")
    public void deleteUser(String id) {
        userDAO.deleteById(id);
    }
}
```

> 注意：修改或删除数据后必须通过 `@CacheEvict` 清除缓存，否则集群环境下各节点本地缓存不会自动失效。

---

### Q9: 如何使用 CdpCacheClient 编程式操作缓存？

除了注解方式，框架还提供 `CdpCacheClient` 和 `CdpCache` 接口用于编程式缓存操作：

```java
package com.leatop.example.service.impl;

import com.leatop.cdp.cache.CdpCache;
import com.leatop.cdp.cache.CdpCacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl {

    private static final String DEMO_CACHE_NAME = "demo1-cache";

    @Autowired
    private CdpCacheClient cdpCacheClient;

    public void cacheDemo(String key, String value) {
        // 获取缓存实例
        CdpCache cdpCache = cdpCacheClient.getCache(DEMO_CACHE_NAME);

        // 写入缓存
        cdpCache.put(key, value);

        // 读取缓存
        Object result = cdpCache.get(key);

        // 带类型读取
        String typed = cdpCache.get(key, String.class);

        // 删除缓存
        cdpCache.remove(key);

        // 获取缓存大小
        int size = cdpCache.size();
    }
}
```

`CdpCacheClient` 还支持指定 TTL 获取缓存实例：

```java
CdpCache cache = cdpCacheClient.getCache("myCache", 60, TimeUnit.SECONDS);
```

> 注意：不要直接操作 `RedisTemplate` 绕过缓存抽象层，统一使用 `CdpCacheClient`。

---

### Q10: CDP 缓存模块提供了哪些预设的 TTL 常量？

框架通过 `CacheNameTimeEnum` 提供预设的缓存 TTL 常量：

```java
import com.leatop.cdp.cache.constant.CacheNameTimeEnum;

// 可用常量：
CacheNameTimeEnum.CACHE_1SECS   // 1 秒
CacheNameTimeEnum.CACHE_5SECS   // 5 秒
CacheNameTimeEnum.CACHE_10SECS  // 10 秒
CacheNameTimeEnum.CACHE_30SECS  // 30 秒
```

适用于对时效性要求高的短期缓存场景（如验证码、临时令牌）。

---

## 三、分布式锁模块

### Q11: 如何在 CDP 框架中启用分布式锁？

1. 在主应用类上添加 `@EnableCdpLock` 注解
2. 配置锁类型（默认本地锁，生产环境建议 Redis）

```java
package com.leatop.example;

import com.leatop.cdp.lock.annotation.EnableCdpLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCdpLock
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

配置项（`application-dev.yaml`）：

```yaml
spring:
  lock:
    type: redis    # redis（分布式）或 local（单机，默认）
```

> 注意：单机开发可用 `local` 类型（基于 `ReentrantLock`），集群部署必须配置 `redis` 类型（基于 Redisson）。

---

### Q12: 如何使用 @Lock 注解为方法加分布式锁？

在 Service 方法上使用 `@Lock` 注解，框架通过 AOP 自动处理加锁/解锁：

```java
package com.leatop.example.service.impl;

import com.leatop.cdp.lock.annotation.Lock;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl {

    // 使用 Spring EL 表达式作为锁 key
    @Lock(key = "#orderId", waitTime = 3, expire = 60)
    public void processOrder(String orderId) {
        // 此方法同一时刻只有一个线程可执行（相同 orderId）
        // 业务逻辑...
    }

    // 默认锁：key 自动生成为 className:methodName
    @Lock
    public void globalTask() {
        // 此方法全局只有一个线程可执行
    }
}
```

`@Lock` 注解参数说明：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `key` | `""` | 锁 key，支持 Spring EL 表达式（如 `#param`），为空则自动生成 |
| `waitTime` | 5 | 等待获取锁的超时时间 |
| `expire` | 60 | 锁自动过期时间 |
| `lockTime` | 60 | 锁持有时间 |
| `timeUnit` | `SECONDS` | 时间单位 |

> 注意：获取锁失败时会抛出 `LockFailException`，由全局异常处理器处理。锁 key 建议使用"业务前缀:业务ID"格式，如 `#orderId` 对应 `processOrder:orderId:123`。

---

### Q13: 如何使用 CdpLockClient 编程式操作分布式锁？

```java
package com.leatop.example.service.impl;

import com.leatop.cdp.lock.CdpLock;
import com.leatop.cdp.lock.CdpLockClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TestLockService {

    @Autowired
    private CdpLockClient cdpLockClient;

    public void doWithLock(String bizId) {
        CdpLock lock = cdpLockClient.getLock("order:process:" + bizId);
        try {
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
                try {
                    // 获取锁成功，执行业务逻辑
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败
                throw new RuntimeException("获取锁失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

> 注意：禁止直接使用 `RedissonClient` 操作锁，统一通过 `CdpLockClient` 或 `@Lock` 注解。编程式用法需自行在 `finally` 中释放锁。

---

### Q14: 分布式锁的 local 模式和 redis 模式有什么区别？

| 对比项 | local 模式 | redis 模式 |
|--------|-----------|------------|
| 底层实现 | `ReentrantLock`（JVM 内） | Redisson `RLock`（Redis） |
| 适用场景 | 本地开发、单节点部署 | 集群部署、多节点环境 |
| 配置 | `spring.lock.type=local` | `spring.lock.type=redis` |
| 跨进程互斥 | 不支持 | 支持 |
| Redis 依赖 | 无 | 需要 Redis 连接 |

Redis 锁模式支持单节点、Sentinel 和 Cluster 三种连接方式，由 `RedisLockConfiguration` 自动配置。

---

## 四、异常处理

### Q15: BusException、UncheckedException、UnauthorizedException 分别在什么场景使用？

CDP 框架提供四种异常类型，位于 `com.leatop.cdp.data.exception` 包下：

| 异常类 | HTTP 状态码 | 使用场景 |
|--------|------------|----------|
| `BusException` | 400 | 业务校验失败（参数不合法、数据不存在、业务规则不满足） |
| `UncheckedException` | 自定义 code / 400 | 系统级运行时错误，支持自定义错误码 |
| `UnauthorizedException` | 401 | 未认证或认证过期 |
| `ServiceUncheckedException` | 500 | 服务调用异常，支持 i18n 消息模板 |

使用示例：

```java
import com.leatop.cdp.data.exception.BusException;
import com.leatop.cdp.data.exception.UncheckedException;

// 业务校验失败 → BusException
if (user == null) {
    throw new BusException("用户不存在");
}

// 带自定义状态码的业务异常
throw new BusException("无效的应用id：" + appId, 404);

// 系统级错误 → UncheckedException
throw UncheckedException.of(5001, "调用外部服务失败");

// 带原始异常的系统错误
throw new UncheckedException("数据解析异常", e);
```

> 注意：不要直接抛 `RuntimeException` 或 `IllegalArgumentException`，使用框架异常类型以确保全局异常处理器能正确映射 HTTP 状态码。

---

### Q16: 全局异常处理器是如何工作的？

框架在 `leatop-cdp-common-core` 中注册了 `GlobalExceptionHandler`（`@RestControllerAdvice`），自动捕获所有异常并转换为统一响应格式 `Message<T>`：

```json
{
    "code": 400,
    "msg": "用户不存在",
    "data": null
}
```

异常到 HTTP 状态码的映射规则：

| 异常类型 | HTTP 状态码 |
|----------|------------|
| `BusException` | 400 BAD_REQUEST |
| `UnauthorizedException` | 401 UNAUTHORIZED |
| `UncheckedException` | 异常自带 code 或 400 |
| `ServiceUncheckedException` | 500 INTERNAL_SERVER_ERROR |
| `NullPointerException` | 500 INTERNAL_SERVER_ERROR |
| `MissingServletRequestParameterException` | 400 BAD_REQUEST |
| `BindException`（参数绑定错误） | 400 BAD_REQUEST |
| `NoResourceFoundException` | 404 NOT_FOUND |
| 其他 `Exception` | 500 INTERNAL_SERVER_ERROR |

> 注意：不要在 Controller/Service 中 `try-catch` 后吞掉异常，让全局异常处理器统一处理。

---

### Q17: 异常处理的最佳实践有哪些？

1. **业务校验失败用 `BusException`**：如参数不合法、数据不存在
2. **系统级错误用 `UncheckedException`**：如第三方接口调用失败、文件读写异常
3. **认证失败用 `UnauthorizedException`**：如 token 过期、未登录
4. **不要吞掉异常**：不要 `catch` 后仅打日志而不抛出
5. **不要用 `RuntimeException`**：使用框架异常类型
6. **不要在 Controller 中处理异常**：让全局处理器统一处理
7. **Service 中外部调用建议包装异常**：

```java
try {
    externalApi.call();
} catch (Exception e) {
    throw new UncheckedException("调用外部服务失败", e);
}
```

---

## 五、消息队列

### Q18: 如何在 CDP 中发送消息？

框架通过 `MessageOperations` 接口提供统一的消息发送能力：

```java
package com.leatop.example.service;

import com.leatop.cdp.base.stream.common.MessageOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private MessageOperations messageOperations;

    public void sendNotification(String topic, Object message) {
        // 简单发送
        messageOperations.send(topic, message);

        // 带自定义头发送
        messageOperations.send(topic, message, Map.of("priority", "high"));
    }
}
```

也可以继承 `AbstractMessageProducer` 创建专用的消息生产者：

```java
package com.leatop.cdp.base.stream.demo.producer;

import com.leatop.cdp.base.stream.common.AbstractMessageProducer;
import org.springframework.stereotype.Component;

@Component
public class PersonMessageProducer extends AbstractMessageProducer<PersonDTO> {

    @Override
    public String getBindingName() {
        return "sendMessage-out-0_spec";
    }
}
```

> 注意：不要直接使用 `KafkaTemplate`、`RabbitTemplate` 等具体实现，统一使用框架抽象。

---

### Q19: 如何在 CDP 中接收消息？

继承 `AbstractMessageConsumer` 并实现 `doProcess` 方法：

```java
package com.leatop.cdp.base.stream.demo.consumer;

import com.leatop.cdp.base.stream.common.AbstractMessageConsumer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("personInput")
public class PersonMessageConsumer extends AbstractMessageConsumer<String> {

    @Override
    public boolean doProcess(String message, Map<String, Object> headers) {
        // 处理消息
        System.out.println("收到消息：" + message);
        return true;  // 返回 true 表示处理成功
    }

    @Override
    public String getDestination() {
        return "personInput-in-0";
    }
}
```

框架内置的消费者能力：
- 自动重试（默认 3 次，由 `spring.cloud.stream.message.consumer.max-attempts` 控制）
- 指数退避（初始 1s，倍率 2.0，最大 10s）
- 死信队列（默认启用，后缀 `.DLQ`）

---

### Q20: 消息队列如何从 Kafka 切换到 RabbitMQ？

通过 Spring Profile 切换，业务代码无需修改：

```yaml
# application.yaml
spring:
  profiles:
    active:
      - dev
      - kafka      # 改为 rabbitmq 即可切换
```

框架基于 Spring Cloud Stream 的 Binder 抽象，`MessageOperations` 和 `AbstractMessageConsumer` 屏蔽了底层 MQ 差异。

消息队列相关配置项（`spring.cloud.stream.message` 前缀）：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enabled` | `true` | 是否启用消息功能 |
| `producer.timeout` | `3000` | 发送超时（ms） |
| `producer.retry-times` | `3` | 发送重试次数 |
| `consumer.concurrency` | `1` | 消费者并发数 |
| `consumer.max-attempts` | `3` | 最大消费重试次数 |
| `consumer.dlq-enabled` | `true` | 是否启用死信队列 |
| `consumer.dlq-suffix` | `.DLQ` | 死信队列后缀 |

---

## 六、认证与授权

### Q21: 如何获取当前登录用户信息？

通过 `IUserHelper` 接口获取，位于 `com.leatop.cdp.core.api` 包：

```java
package com.leatop.example.service.impl;

import com.leatop.cdp.core.api.IUserHelper;
import com.leatop.cdp.core.dto.CurrentUserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyService {

    @Autowired
    private IUserHelper userHelper;

    public void doSomething() {
        // 获取当前用户完整信息（含组织、资源权限）
        CurrentUserDto user = userHelper.getCurrentUserInfo();

        // 获取当前用户 ID
        String userId = userHelper.getCurrentUserId();

        // 获取租户 ID
        String tenantId = userHelper.getTenantId();

        // 检查是否已登录
        boolean loggedIn = userHelper.isLogin();

        // 检查是否为租户管理员
        boolean isAdmin = userHelper.isTenantAdmin();

        // 获取用户角色类型列表
        List<Integer> roleTypes = userHelper.getRoleTypes();
    }
}
```

> 注意：不要直接操作 SA-Token API 获取用户信息，统一通过 `IUserHelper` 接口。

---

### Q22: 如何配置接口白名单（免认证访问）？

在 `application.yaml` 中配置 `cdp.security.auth-white-list`：

```yaml
cdp:
  security:
    auth-white-list:
      - /test/**          # 测试接口
      - /chat/**          # 聊天接口
      - /cas/**           # CAS 单点登录回调
      - /attachment/uploadFile  # 文件上传
      - /demo/api/apiKey  # API Key 接口
```

白名单中的接口不需要携带 `cdp-token` 请求头即可访问。

---

### Q23: CDP 的数据权限是如何工作的？

CDP 通过 `DataPermissionInterceptor`（MyBatis-Plus 拦截器）实现行级和列级数据权限控制：

- **行级权限**：通过 `DataRuleCondition` 在 SQL 中自动追加 WHERE 条件
- **列级权限**：通过 `DataColumnRule` 控制可查询的字段

数据权限变量：

| 变量 | 含义 |
|------|------|
| `${tenant_id}` | 当前租户 ID |
| `${user_id}` | 当前用户 ID |
| `${org_id}` | 当前组织 ID |
| `${org_ids}` | 用户所属所有组织 ID |
| `*` | 所有数据（不限制） |

配置项：

```yaml
cdp:
  extension:
    data-permission:
      include-apis:       # 需要数据权限控制的接口路径
        - /logs/**
      exclude-apis:       # 排除的接口路径
        - /xxl-job-admin/api/**
  permission:
    enable: true          # 总开关（默认 true）
```

---

## 七、数据库与 ORM

### Q24: MyBatis-Plus 在 CDP 中是如何配置的？

核心配置在 `MybatisPlusConfig`（`com.leatop.cdp.autoconfig.config`）中，自动注册了以下拦截器：

- `OptimisticLockerInnerInterceptor` — 乐观锁
- `DataPermissionInterceptor` — 数据权限
- `EncryptInterceptor` — 字段加密

`application.yaml` 配置项：

```yaml
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*DAO.xml
  global-config:
    db-config:
      table-underline: true     # 驼峰转下划线
      id-type: ASSIGN_ID        # 默认 ID 策略：雪花算法
  type-handlers-package: com.leatop.system.handler
```

DAO 定义（命名为 `*DAO` 而非 `*Mapper`）：

```java
package com.leatop.example.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leatop.example.model.po.ApplicationPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApplicationDAO extends BaseMapper<ApplicationPO> {
}
```

> 注意：CDP 中 Mapper 接口统一命名为 `*DAO`，XML 文件放在 `resources/mapper/` 目录下。

---

### Q25: Flyway 数据库迁移脚本怎么写？

迁移脚本放在各模块 `resources/db/migration/{vendor}/` 目录下（`{vendor}` 为数据库类型，如 `mysql`、`dm`、`oracle` 等）。

**命名规则：**
- 版本迁移：`V{版本号}__{描述}.sql`，如 `V1.1.5__news.sql`
- 可重复执行：`R__{描述}.sql`，如 `R__data_init.sql`（文件内容变化时重新执行）

示例脚本 `V1.1.5__news.sql`：

```sql
CREATE TABLE IF NOT EXISTS `news_item` (
    `id` varchar(50) NOT NULL,
    `title` varchar(100) NOT NULL COMMENT '文章标题',
    `content` varchar(2000) DEFAULT '' COMMENT '动态内容',
    `is_deleted` int DEFAULT 0 COMMENT '是否删除: 1-是 0-否',
    `create_gmt` datetime DEFAULT NULL COMMENT '创建时间',
    `update_gmt` datetime DEFAULT NULL COMMENT '更新时间',
    `tenant_id` varchar(36) DEFAULT NULL COMMENT '租户ID',
    PRIMARY KEY (`id`)
) COMMENT='行业新闻动态';
```

配置项：

```yaml
spring:
  flyway:
    enabled: ${FLYWAY_ENABLED:false}  # 开发环境建议开启
    clean-disabled: true               # 禁止清库（安全措施）
    baseline-on-migrate: true
    baseline-version: 1.0.0
    locations:
      - classpath:db/migration/{vendor}
```

> 注意：版本号中用双下划线 `__` 分隔版本号和描述。`clean-disabled` 必须设为 `true`，禁止在生产环境执行清库操作。

---

### Q26: CDP 框架支持哪些数据库，如何切换？

CDP 支持以下数据库：

| 数据库 | Profile | DatabaseId |
|--------|---------|------------|
| MySQL | `mysql` | `MySQL` |
| Oracle | `oracle` | `Oracle` |
| 达梦（DM） | `dm` | `DM DBMS` |
| 高斯数据库（GaussDB） | `gauss` | `GaussDB` |
| 人大金仓（KingbaseES） | `kingbase` | `KingbaseES` |
| 南大通用（GBase） | `gbase` | `GBase` |
| PolarDB | `polardb` | `PolarDB` |

切换方式：

```yaml
spring:
  profiles:
    active:
      - dev
      - mysql   # 改为 dm / oracle / gauss / kingbase / gbase / polardb
```

数据库 ID 映射配置在 `cdp.extension.database-ids` 中，用于 MyBatis 的 `databaseIdProvider`，支持在 XML 中编写数据库特定的 SQL。

---

## 八、定时任务

### Q27: 如何启用 XXL-Job 定时任务？

1. 在主类添加 `@EnableXxlJobExecutor` 注解
2. 配置 XXL-Job 相关参数

```java
package com.leatop.cdp.xxljob.sample;

import com.xxl.job.core.config.EnableXxlJobExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableXxlJobExecutor
public class XxljobExecutorApplication {
    public static void main(String[] args) {
        SpringApplication.run(XxljobExecutorApplication.class, args);
    }
}
```

配置 `XxlJobSpringExecutor` Bean：

```java
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.executor.adminAddresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setAppname(appname);
        executor.setPort(port);
        return executor;
    }
}
```

配置项（`application.yaml`）：

```yaml
xxl:
  job:
    executor:
      adminAddresses: http://172.17.1.28:28080/xxl-job-admin
      accessToken: faa60c091fb047c993c5fee20385c1c5
      appname: my-executor
      port: 9998
      logPath: /data/applogs/xxl-job/jobhandler
      logRetentionDays: 30
```

---

### Q28: 如何编写一个 XXL-Job 任务处理器？

使用 `@XxlJob` 注解标记任务方法：

```java
package com.leatop.cdp.xxljob.sample.jobhandler;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class SampleXxlJob {

    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        // 任务逻辑
        System.out.println("XXL-Job 任务执行：" + System.currentTimeMillis());
    }
}
```

任务名称（如 `demoJobHandler`）需要在 XXL-Job 管理后台注册。

---

## 九、配置与安全

### Q29: 如何对配置文件中的敏感信息加密？

CDP 使用 Jasypt + SM4（国密算法）对配置文件中的敏感信息加密。加密由 `CustomEncryptor`（`com.leatop.cdp.core.jasypt`）实现。

使用步骤：

1. 配置加密密钥：

```yaml
jasypt:
  encryptor:
    key: your-16-byte-key    # SM4 密钥，至少 16 字节
    bean: customEncryptor
    property:
      prefix: "ENC("
      suffix: ")"
```

2. 在配置中使用 `ENC()` 包裹加密后的值：

```yaml
spring:
  datasource:
    druid:
      password: ENC(c91905c48bfb839822eba56e209b7725)
```

框架启动时会自动解密 `ENC(...)` 包裹的配置值。

> 注意：加密密钥本身不应明文写在配置文件中，建议通过环境变量传入。

---

### Q30: CDP 的请求过滤器链执行顺序是什么？

框架在 `leatop-cdp-common-core` 中注册了以下过滤器，按 `@Order` 顺序执行：

```
CheckCSRFFilter（@Order(-1)）— CORS/CSRF 安全防护
  → MDCRequestFilter（@Order(0)）— 请求追踪 ID、清理数据权限上下文
    → RepeatedlyRequestFilter（@Order(1)）— 请求体/响应体可重复读
```

各过滤器职责：

| 过滤器 | Order | 功能 |
|--------|-------|------|
| `CheckCSRFFilter` | -1 | 校验请求来源（`allowedOrigins` 白名单），防止 CSRF 攻击 |
| `MDCRequestFilter` | 0 | 设置 MDC traceId（`HeaderConstant.REQUEST_ID`），清理 `DataPermissionHolder` 和 `TenantContentHolder` |
| `RepeatedlyRequestFilter` | 1 | 包装请求为 `RepeatedlyRequestWrapper`，支持多次读取请求体（如日志记录、签名验证） |

所有过滤器继承自 `BaseRequestFilter`（`OncePerRequestFilter`），支持静态资源和 OPTIONS 请求的白名单跳过。

---

### Q31: 如何配置 CORS 和 CSRF 防护？

通过 `cdp.extension` 配置项控制：

```yaml
cdp:
  extension:
    allowed-origins:         # CORS 白名单（为空则不限制）
      - http://localhost:3000
      - https://app.example.com
    check-headers:           # CSRF 校验的请求头（默认 Referer）
      - Referer
```

`CheckCSRFFilter` 会校验请求的 `Referer` 头是否在 `allowedOrigins` 白名单中，拒绝来源不明的请求。

---

## 十、API 文档与构建

### Q32: 如何生成 API 文档？

CDP 使用 Smart-doc 生成接口文档，配置文件为 `smart-doc.json`：

```json
{
    "serverUrl": "/",
    "isStrict": false,
    "allInOne": true,
    "outPath": "src/main/resources/static/api-docs",
    "createDebugPage": false,
    "projectName": "CDP框架API接口文档",
    "requestHeaders": [
        {
            "name": "cdp-token",
            "type": "string",
            "desc": "接口认证token",
            "required": true
        }
    ]
}
```

生成命令：

```bash
mvn com.ly.smart-doc:smart-doc-maven-plugin:3.0.3:html
```

文档输出到 `src/main/resources/static/api-docs/index.html`。

> 注意：使用 Smart-doc 的 JavaDoc 注释规范编写接口文档，不要使用 Swagger `@Api` 系列注解。

---

### Q33: 常用的 Maven 构建命令有哪些？

```bash
# 构建全部模块（跳过测试）
mvn clean install -DskipTests=true

# 构建全部模块（含测试）
mvn clean install

# 构建指定模块
mvn clean install -f leatop-cdp-example/leatop-cdp-example-demo1 -DskipTests=true

# 运行示例应用
cd leatop-cdp-example/leatop-cdp-example-demo1
mvn spring-boot:run

# 打包可执行 JAR
mvn package -DskipTests=true
java -jar target/leatop-cdp-example-demo1-1.0.3-SNAPSHOT-exec.jar

# 生成 API 文档
mvn com.ly.smart-doc:smart-doc-maven-plugin:3.0.3:html
```

> 注意：Maven 私服需在 `settings.xml` 中配置 CDP Nexus 地址，并在 `/etc/hosts` 中添加 `172.19.0.62 cdp.nexus.com`。

---

### Q34: 异步任务和日志追踪如何配合使用？

在异步方法中需要手动传递 MDC 和租户上下文：

```java
package com.leatop.cdp.log.service;

import com.leatop.cdp.core.constant.CoreConstants;
import com.leatop.cdp.core.holder.TenantContentHolder;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LogCollectService {

    @Async(LogThreadPoolConfig.LOG_COLLECT_TASK_EXECUTOR)
    public void doCollectLoginLog(LoginLogDTO logInfo) {
        // 1. 恢复追踪上下文
        MDC.put(CoreConstants.TRACE_ID, logInfo.getTraceId());
        TenantContentHolder.setTenantId(logInfo.getTenantId());

        try {
            // 2. 执行业务逻辑
            // ...
        } finally {
            // 3. 清理上下文（防止线程池复用导致数据泄漏）
            TenantContentHolder.clear();
            MDC.clear();
        }
    }
}
```

> 注意：异步方法中 MDC 和 `TenantContentHolder` 不会自动传递，必须手动设置和清理。

---

### Q35: 如何在 CDP 中使用多租户功能？

配置项（`application.yaml`）：

```yaml
cdp:
  tenant:
    enable: true             # 启用多租户
    ignore-null: true        # 忽略租户 ID 为空的情况
    exclusion-tables:        # 排除多租户过滤的表
      - frame_code_template_group
```

框架通过 `TenantContentHolder` 管理当前请求的租户上下文，`MDCRequestFilter` 在请求结束时自动清理。数据权限拦截器会自动为 SQL 追加 `tenant_id` 条件。

---

### Q36: 如何使用日志注解记录操作日志？

框架在 `leatop-cdp-business-log-api` 中提供日志注解：

- `@LogRecord` — 记录操作日志
- `@LogModule` — 标记日志模块
- `@LogParam` — 标记日志参数
- `@EfficiencyLog` — 记录效率日志

日志类型包括：登录日志、操作日志、效率日志、授权日志，通过 `LogCollectBusiness` 接口异步采集。

配置排除路径（不记录操作日志的接口）：

```yaml
cdp:
  syslog:
    exclude-patterns:
      - /xxl-job-admin/jobgroup/pageList
      - /system/holiday/**
```

---

### Q37: 如何使用参数校验分组？

CDP 使用 Spring Validation 的分组校验，在 Controller 中通过 `@Validated` 指定分组：

```java
// 新增时校验
@PostMapping("/add")
public Message<Boolean> add(@RequestBody @Validated(AddGroup.class) NewsItemDto dto) {
    return service.add(dto);
}

// 更新时校验（需要 id 字段）
@PostMapping("/update")
public Message<Boolean> update(@RequestBody @Validated(UpdateGroup.class) NewsItemDto dto) {
    return service.update(dto);
}
```

在 DTO 中按分组标注校验规则：

```java
public class NewsItemDto {

    @NotBlank(groups = UpdateGroup.class, message = "ID不能为空")
    private String id;

    @NotBlank(groups = {AddGroup.class, UpdateGroup.class}, message = "标题不能为空")
    private String title;
}
```

校验失败会抛出 `BindException`，由全局异常处理器自动返回 400 状态码。

---

### Q38: 统一响应格式 Message 是什么结构？

CDP 所有接口统一使用 `Message<T>` 作为响应包装类：

```json
{
    "code": 200,
    "msg": "success",
    "data": { ... }
}
```

在 Controller 中使用：

```java
@GetMapping("/queryById")
public Message<ApplicationDTO> queryById(@RequestParam String id) {
    ApplicationDTO dto = applicationService.getById(id);
    return Message.success(dto);
}

@PostMapping("/add")
public Message<Boolean> add(@RequestBody ApplicationDTO dto) {
    boolean result = applicationService.save(dto);
    return Message.success(result);
}
```

分页查询返回 `Page<T>` 类型，由 PageHelper 插件自动处理。PageHelper 配置：

```yaml
pagehelper:
  reasonable: false
  support-methods-arguments: true
  max-page-size: 500
```

---

### Q39: 如何配置 P6SPY 进行 SQL 监控？

P6SPY 用于在开发环境输出实际执行的 SQL 语句。配置文件 `spy.properties`：

```properties
modulelist=com.baomidou.mybatisplus.extension.p6spy.MybatisPlusLogFactory,com.p6spy.engine.outage.P6OutageFactory
logMessageFormat=com.baomidou.mybatisplus.extension.p6spy.P6SpyLogger
appender=com.baomidou.mybatisplus.extension.p6spy.StdoutLogger
deregisterdrivers=true
useprefix=true
excludecategories=info,debug,result,commit,resultset
outagedetection=true
outagedetectioninterval=2
```

> 注意：P6SPY 仅在开发环境使用，生产环境应移除或禁用以避免性能影响。

---

### Q40: Git 提交信息应遵循什么规范？

CDP 项目遵循以下 Git 提交规范：

```
feat:     新功能
fix:      Bug 修复
docs:     文档更新
refactor: 代码重构
test:     测试相关
```

示例：

```
feat: 新增消息队列批量发送功能
fix: 修复缓存 TTL 配置不生效的问题
docs: 补全 Controller JavaDoc 注释
refactor: 重构数据权限拦截器逻辑
```
