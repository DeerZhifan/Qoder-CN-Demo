---
trigger: when_referenced
knowledge_source:
  - cdp-design-cotime
  - cdp-module-cotime
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-cotime` 依赖
- 使用 `@ComputeTime` 注解
- 配置 `ko-time.*` 相关属性
- 实现 `InvokedHandler` 接口或使用 `@KoListener` 注解
- 访问 `/koTime` 监控面板

---

## 前置依赖

1. Maven 依赖：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-cotime</artifactId>
</dependency>
```

2. 基础配置：

```yaml
ko-time:
  enable: true
  pointcut: "execution(* com.leatop.example..*.*(..))"
```

3. 如使用 `redis` 存储模式，需配置 Redis 连接；如使用 `database` 存储模式，需数据库中存在 `ko_*` 表。

---

## 配置要点

```yaml
ko-time:
  enable: true                        # 是否启用
  language: "chinese"                 # 界面语言：chinese / english
  log-enable: false                   # 是否打印耗时日志到控制台
  exception-enable: false             # 是否追踪异常
  param-analyse: false                # 是否分析方法参数
  pointcut: "execution(* com.example..*.*(..))"  # AOP 切点表达式
  threshold: 0.0                      # 最小记录阈值（毫秒），低于此值不记录
  saver: "memory"                     # 数据存储方式：memory / redis / database
  auth-enable: false                  # 监控面板是否需要认证
  mail-enable: false                  # 是否启用慢方法邮件告警
  mail-threshold: 5000                # 告警阈值（毫秒）
  mail-host: ""                       # SMTP 服务器
  mail-user: ""                       # SMTP 用户名
  mail-code: ""                       # SMTP 密码
  mail-receivers: ""                  # 收件人（逗号分隔）
```

- `pointcut` 决定监控范围，建议精确到业务包路径，范围过大会影响性能。
- `threshold` 生产环境建议设置合理值（如 100ms），过滤短耗时方法，减少数据量。
- `saver` 三种模式：`memory`（默认，重启丢失）、`redis`（支持集群共享）、`database`（持久化到 `ko_*` 表）。
- `discardRate`（默认 0.3）控制重复调用记录的概率丢弃率，高并发下减少存储压力。

### 数据存储模式对比

| 方式 | 配置值 | 适用场景 | 注意事项 |
|------|--------|---------|---------|
| 内存 | `memory` | 开发环境 | 重启丢失 |
| Redis | `redis` | 生产环境/集群 | 复用 CDP 的 Redis 连接 |
| 数据库 | `database` | 持久化需求 | 单 JDBC 连接，仅适用于低写入量 |

---

## 代码模式

### 推荐写法

**方式一：AOP 切点拦截（主要方式，构建调用链关系）**

```yaml
ko-time:
  enable: true
  pointcut: "execution(* com.leatop.example.controller..*.*(..)) || execution(* com.leatop.example.service..*.*(..))"
  log-enable: true
  threshold: 100
```

配置后自动拦截匹配的方法，构建方法间调用关系图，无需修改业务代码。

**方式二：@ComputeTime 注解（轻量级补充，仅输出耗时日志）**

```java
import com.leatop.cdp.cotime.annotation.ComputeTime;

@Service
public class UserService {

    @ComputeTime
    public UserDTO getUser(Long id) {
        // 方法执行耗时会被自动记录到日志
        return userMapper.selectById(id);
    }
}
```

**自定义监听器**

```java
import com.leatop.cdp.cotime.handler.InvokedHandler;
import com.leatop.cdp.cotime.model.InvokedInfo;
import org.springframework.stereotype.Component;

@Component
public class CustomInvokedHandler implements InvokedHandler {

    @Override
    public void onInvoked(InvokedInfo info) {
        // 方法调用完成回调
        System.out.println("方法：" + info.getCurrent().getMethodName()
            + " 耗时：" + info.getCurrent().getValue() + "ms");
    }

    @Override
    public void onException(InvokedInfo info) {
        // 方法异常回调
    }
}
```

### 禁止事项

- **禁止将 `pointcut` 配置范围过大** -- 如 `execution(* com..*.*(..))`，会导致每个方法调用都经过拦截器，严重影响性能
- **禁止在生产环境使用 `memory` 存储且不设置 `threshold`** -- 会导致大量无价值数据占用内存
- **禁止在非 Spring Bean 上使用 `@ComputeTime` 注解** -- AOP 代理仅对容器管理的 Bean 生效
- **禁止依赖 `database` 存储模式的高并发写入** -- 底层使用单个 JDBC Connection，不支持连接池
- **禁止手动管理消费线程** -- 异步队列的消费线程数由 `ko-time.thread-num` 配置（默认 2），框架自动管理
- **禁止忽略 `discardRate` 设置** -- 高并发下不设置丢弃率会导致存储压力过大，统计数据不完全精确但可接受
