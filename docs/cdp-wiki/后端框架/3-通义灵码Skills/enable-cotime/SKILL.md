# 启用 CDP 调用耗时统计

## 描述

在已有 CDP 项目中启用调用耗时统计组件（`leatop-cdp-base-cotime`），基于 AOP 拦截方法调用，自动统计 Controller、Service、DAO 各层方法的执行耗时，支持调用链可视化、慢方法告警和异常追踪。底层基于 Ko-Time 框架，零侵入接入，提供内嵌 Web 监控面板。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **监控包路径**（需要监控的业务包路径，如 `com.leatop.example`）
3. **数据存储方式**（`memory`、`redis` 或 `database`，默认 `memory`）
4. **是否启用邮件告警**（是/否，默认否）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-base-cotime` 提供方法耗时统计、调用链分析和监控面板的完整功能。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-cotime</artifactId>
</dependency>
```

## 步骤 2：添加配置

> `ko-time.pointcut` 决定了监控范围，建议精确到业务包路径。`threshold` 设置最小记录阈值，过滤短耗时方法。

在 `application-dev.yaml` 中添加：

**基础配置：**

```yaml
ko-time:
  enable: true
  pointcut: "execution(* {监控包路径}..*.*(..))"
  log-enable: true
  threshold: 100                      # 只记录耗时超过 100ms 的方法
  saver: "{数据存储方式}"               # memory / redis / database
  language: "chinese"
  exception-enable: true              # 启用异常追踪
```

**多层监控配置示例：**

```yaml
ko-time:
  enable: true
  pointcut: "execution(* com.leatop.example.controller..*.*(..)) || execution(* com.leatop.example.service..*.*(..))"
  log-enable: true
  threshold: 100
  saver: "memory"
```

**邮件告警配置（可选）：**

```yaml
ko-time:
  mail-enable: true
  mail-threshold: 5000                # 超过 5 秒告警
  mail-host: smtp.example.com
  mail-user: alert@example.com
  mail-code: password
  mail-receivers: admin@example.com   # 多个收件人逗号分隔
```

## 步骤 3：编写示例代码（可选）

> `@ComputeTime` 注解为轻量级补充方式，仅输出耗时日志，不参与调用链构建。主要监控由 `pointcut` 配置自动完成。

**@ComputeTime 注解方式：**

```java
import com.leatop.cdp.cotime.annotation.ComputeTime;
import org.springframework.stereotype.Service;

@Service
public class DemoService {

    @ComputeTime
    public String slowMethod() {
        // 方法执行耗时会被自动记录到日志
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "done";
    }
}
```

**自定义监听器（可选）：**

```java
import com.leatop.cdp.cotime.handler.InvokedHandler;
import com.leatop.cdp.cotime.model.InvokedInfo;
import org.springframework.stereotype.Component;

@Component
public class CustomInvokedHandler implements InvokedHandler {

    @Override
    public void onInvoked(InvokedInfo info) {
        // 方法调用完成回调，可推送到外部监控平台
        System.out.println("方法：" + info.getCurrent().getMethodName()
            + " 耗时：" + info.getCurrent().getValue() + "ms");
    }

    @Override
    public void onException(InvokedInfo info) {
        // 方法异常回调
    }
}
```

## 步骤 4：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 日志中出现 Ko-Time 初始化信息
3. 访问 `/koTime` 路径查看内嵌 Web 监控面板
4. 调用业务方法后，在监控面板中确认调用链和耗时数据已记录
5. 如启用了 `@ComputeTime` 注解，检查控制台是否输出耗时日志

---

## 完成后提醒

1. `pointcut` 配置范围过大会影响性能，建议精确到业务包路径（如 `controller` 和 `service` 层）
2. 生产环境建议设置 `threshold` 过滤短耗时方法，减少数据量
3. `memory` 存储方式数据重启丢失，需要持久化请选择 `redis` 或 `database`
4. `database` 存储模式使用单个 JDBC Connection，仅适用于低写入量场景
5. `@ComputeTime` 注解仅输出耗时日志，不参与调用链构建；完整调用链分析依赖 `pointcut` 配置
6. 自定义监听器实现 `InvokedHandler` 接口并注册为 `@Component`，可扩展方法调用后的回调处理
7. 监控面板通过 `ko-time.auth-enable` 启用访问认证，生产环境建议开启
8. 邮件告警仅对超过 `mail-threshold` 的方法发送，需配置完整的 SMTP 信息
