# 如何使用 CDP 调用耗时统计组件

## 概述

调用耗时统计组件（`leatop-cdp-base-cotime`）基于 AOP 拦截方法调用，自动统计 Controller、Service、DAO 各层方法的执行耗时，支持调用链可视化、慢方法告警和异常追踪。底层基于 Ko-Time 框架。

## 启用方式

**1. 添加 Maven 依赖：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-cotime</artifactId>
</dependency>
```

**2. 添加配置：**

```yaml
ko-time:
  enable: true
  # AOP 切点表达式，指定监控的包路径
  pointcut: "execution(* com.leatop.example..*.*(..))"
```

## 核心注解

### @ComputeTime — 方法耗时统计

标记在方法或类上，记录方法执行耗时并输出日志。

```java
import com.leatop.cdp.cotime.annotation.ComputeTime;

@Service
public class UserService {

    @ComputeTime
    public UserDTO getUser(Long id) {
        // 方法执行耗时会被自动记录
        return userMapper.selectById(id);
    }
}
```

### @Auth — 接口认证

标记在方法上，要求请求携带 `kotoken` 参数进行认证（用于监控面板访问控制）。

## 配置项

```yaml
ko-time:
  enable: true                        # 是否启用
  language: "chinese"                 # 界面语言：chinese / english
  log-enable: false                   # 是否打印耗时日志到控制台
  exception-enable: false             # 是否追踪异常
  param-analyse: false                # 是否分析方法参数
  pointcut: "execution(* com.example..*.*(..))"  # AOP 切点
  threshold: 0.0                      # 最小记录阈值（毫秒），低于此值不记录
  saver: "memory"                     # 数据存储方式：memory / redis / database
  auth-enable: false                  # 监控面板是否需要认证
  # 邮件告警配置
  mail-enable: false                  # 是否启用慢方法邮件告警
  mail-threshold: 5000                # 告警阈值（毫秒）
  mail-host: ""                       # SMTP 服务器
  mail-user: ""                       # SMTP 用户名
  mail-code: ""                       # SMTP 密码
  mail-receivers: ""                  # 收件人（逗号分隔）
```

### 数据存储方式

| 方式 | 配置值 | 说明 |
|------|--------|------|
| 内存 | `memory` | 默认，重启丢失 |
| Redis | `redis` | 需配置 `redis-template` |
| 数据库 | `database` | 需配置 `data-source` |

## 使用示例

### 基础配置

```yaml
ko-time:
  enable: true
  pointcut: "execution(* com.leatop.example.controller..*.*(..)) || execution(* com.leatop.example.service..*.*(..))"
  log-enable: true
  threshold: 100          # 只记录耗时超过 100ms 的方法
```

### 自定义监听器

实现 `InvokedHandler` 接口可自定义方法调用的回调处理：

```java
import com.leatop.cdp.cotime.handler.InvokedHandler;
import com.leatop.cdp.cotime.model.InvokedInfo;

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

## 注意事项

> 注意：`pointcut` 配置决定了监控范围，范围过大会影响性能，建议精确到业务包路径。

> 注意：生产环境建议设置 `threshold` 过滤短耗时方法，减少数据量。

> 注意：使用 `memory` 存储时数据重启丢失，需要持久化请选择 `redis` 或 `database`。

> 注意：邮件告警功能需配置完整的 SMTP 信息，仅对超过 `mail-threshold` 的方法发送告警。
