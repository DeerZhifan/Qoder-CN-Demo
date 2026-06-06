# 启用 CDP 日志管理

## 描述

在已有 CDP 项目中启用日志管理组件（`leatop-cdp-business-log`），提供四类日志的自动记录和查询分析：登录登出日志、操作日志、授权日志和访问效率日志。通过 `@LogParam` 和 `@EfficiencyLog` 注解声明式采集，框架异步写入数据库，业务代码零侵入。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`boot` 单体模式 或 `cloud` 微服务模式，默认 `boot`）
3. **需要的日志类型**（操作日志、效率日志、或两者都需要，默认两者都启用）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-business-log-boot-starter` 提供日志采集、异步写入和查询分析的完整功能。微服务模式使用 `cloud-starter`。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

**单体模式：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-log-boot-starter</artifactId>
</dependency>
```

**微服务模式：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-log-cloud-starter</artifactId>
</dependency>
```

## 步骤 2：确认启用注解

> 日志组件不需要额外的 `@Enable` 注解，通过配置项 `cdp.syslog.enable` 控制开关。但需确保主启动类的 `scanBasePackages` 包含 `com.leatop.cdp`，以便扫描到日志模块的自动配置。

确认主启动类配置：

```java
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@MapperScan("com.leatop.cdp.**.dao")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 步骤 3：添加配置

> 通过 `cdp.syslog.enable` 启用日志采集的 AOP 切面。`excludePatterns` 排除不需要记录效率日志的接口路径。

在 `application-dev.yaml` 中添加：

```yaml
cdp:
  syslog:
    enable: true                    # 启用日志采集
    excludePatterns:                # 排除高频低价值的接口路径
      - /actuator/**
      - /favicon.ico
    # includePatterns:              # 指定监控的接口（配置后激活 URL 级别效率日志拦截）
    #   - /api/**
```

## 步骤 4：编写示例代码

> 提供操作日志和效率日志两种注解的使用示例。登录日志由框架自动记录，无需手动添加注解。

**操作日志 -- @LogParam + @LogModule**

```java
import com.leatop.cdp.log.annotation.LogModule;
import com.leatop.cdp.log.annotation.LogParam;
import org.springframework.stereotype.Service;

@LogModule("订单管理")  // 类级别声明模块名称
@Service
public class DemoOrderService {

    /**
     * logDes 支持 SpEL 表达式，通过 #参数名 引用方法参数
     * operateType 未指定时，根据方法名前缀自动推导（save -> CREATE）
     */
    @LogParam(logDes = "'创建订单:' + #dto.orderName")
    public void saveOrder(OrderDTO dto) {
        // 操作日志自动记录：模块=订单管理，操作类型=CREATE
    }

    @LogParam(logDes = "'删除订单:' + #orderId")
    public void deleteOrder(String orderId) {
        // 操作类型自动推导为 DELETE
    }
}
```

**效率日志 -- @EfficiencyLog**

```java
import com.leatop.cdp.log.annotation.LogModule;
import com.leatop.cdp.log.annotation.EfficiencyLog;
import org.springframework.stereotype.Service;

@LogModule("报表管理")
@EfficiencyLog(ignore = "result")  // 类级别：不记录返回结果，降低存储压力
@Service
public class DemoReportService {

    @EfficiencyLog(module = "面板信息")  // 方法级别覆盖模块名
    public Map<String, Object> dashboardInfo() {
        // 自动记录执行耗时
        return new HashMap<>();
    }
}
```

## 步骤 5：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 日志中出现 `LogHandlerConfig` 初始化信息
3. 调用标注了 `@LogParam` 的方法后，查询 `frame_log_operate` 表确认操作日志已写入
4. 调用标注了 `@EfficiencyLog` 的方法后，查询 `frame_log_efficiency` 表确认效率日志已写入

---

## 完成后提醒

1. 登录登出日志由框架自动记录，无需手动添加注解
2. `@LogParam` 的 `operateType` 未指定时，框架根据方法名前缀自动推导操作类型（add->CREATE, update->UPDATE, delete->DELETE, query->RETRIEVE）
3. `@EfficiencyLog` 设置 `ignore = "result"` 可避免记录大量返回数据，建议在返回大对象的方法上启用
4. 效率日志通过 `excludePatterns` 排除静态资源和健康检查等低价值接口，减少数据量
5. 日志异步写入采用专用线程池（核心 10、最大 30、队列 2000），拒绝策略为 CallerRunsPolicy，保证审计日志不丢失
6. 微服务模式下日志采集通过 `LogCollectBusiness` 接口远程调用日志中心服务
