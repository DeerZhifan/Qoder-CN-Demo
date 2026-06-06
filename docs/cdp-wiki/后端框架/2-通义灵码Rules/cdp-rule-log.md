---
trigger: when_referenced
knowledge_source:
  - cdp-design-log
  - cdp-module-log
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-business-log-boot-starter` 或 `leatop-cdp-business-log-cloud-starter` 依赖
- 使用 `@LogParam`、`@EfficiencyLog`、`@LogModule` 注解
- 使用 `LogClientUtils` 静态工具类记录日志
- 操作 `frame_log_login`、`frame_log_operate`、`frame_log_authorize`、`frame_log_efficiency` 表

---

## 前置依赖

1. Maven 依赖（单体模式）：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-log-boot-starter</artifactId>
</dependency>
```

微服务模式使用 `leatop-cdp-business-log-cloud-starter`。

2. 配置启用日志采集：

```yaml
cdp:
  syslog:
    enable: true
```

3. 数据库中需包含四张日志表：`frame_log_login`、`frame_log_operate`、`frame_log_authorize`、`frame_log_efficiency`。

---

## 配置要点

```yaml
cdp:
  syslog:
    enable: true                    # 是否启用日志采集（AOP 切面注册开关）
    excludePatterns:                # 排除的接口路径（减少效率日志量）
      - /xxl-job-admin/api/**
      - /system/holiday/**
    # includePatterns:              # 指定监控的接口（默认全部）
    #   - /system/**
```

- `enable: true` 控制 `LogAspect` 的注册，未启用时 `@LogParam` 和 `@EfficiencyLog` 注解不生效。
- `includePatterns` 配置后才激活 `LogInterceptor` 的 URL 级别效率日志拦截。
- `excludePatterns` 排除静态资源和健康检查等高频低价值接口。

---

## 代码模式

### 推荐写法

**操作日志 -- @LogParam 注解**

```java
import com.leatop.cdp.log.annotation.LogModule;
import com.leatop.cdp.log.annotation.LogParam;

@LogModule("执行器管理")  // 类级别声明模块名称
@Service
public class JobGroupService {

    @LogParam(logDes = "'添加执行器组:' + #dto.title")
    public Message<String> save(XxlJobGroupDTO dto) {
        // 操作日志自动记录
    }

    @LogParam(logDes = "'查询执行器: appName=' + #appName + ';title=' + #title")
    public Message<List<XxlJobGroupDTO>> query(String appName, String title) {
        // ...
    }
}
```

**效率日志 -- @EfficiencyLog 注解**

```java
import com.leatop.cdp.log.annotation.LogModule;
import com.leatop.cdp.log.annotation.EfficiencyLog;

@LogModule("报表管理")
@EfficiencyLog(ignore = "result")  // 类级别：不记录返回结果
@Service
public class ReportService {

    @EfficiencyLog(module = "面板信息")  // 方法级别覆盖模块名
    public Map<String, Object> dashboardInfo() {
        // 自动记录执行耗时
    }
}
```

**静态工具类记录日志（非 AOP 场景）**

```java
// 登录日志（框架自动记录，通常无需手动调用）
LogClientUtils.logLogin(loginLogDTO);

// 操作日志
LogClientUtils.logOperate(operateLogDTO);

// 授权日志
LogClientUtils.logAuthorize(authorizeLogDTO);

// 效率日志
LogClientUtils.logEfficiency(efficiencyLogDTO);
```

**操作类型自动推导**：当 `@LogParam` 未显式指定 `operateType` 时，框架根据方法名前缀自动推导：
- `add/save/insert` -> CREATE
- `update/edit` -> UPDATE
- `delete/remove` -> DELETE
- `query/search/get` -> RETRIEVE
- `import` -> IMPORT
- `export` -> EXPORT

### 禁止事项

- **禁止在未配置 `cdp.syslog.enable: true` 的情况下依赖 `@LogParam` 生效** -- AOP 切面不会注册
- **禁止在非 Spring Bean 类上使用 `@LogParam` 或 `@EfficiencyLog`** -- AOP 代理仅对容器管理的 Bean 生效
- **禁止在 `@EfficiencyLog` 方法中记录大量返回数据** -- 应设置 `ignore = "result"` 减少存储压力
- **禁止在极高频方法的 `logDes` 中使用复杂 SpEL 表达式** -- 每次调用都会解析，建议使用简单字符串
- **禁止手动管理日志线程池** -- 框架已配置专用 `logCollectTaskExecutor`（核心 10、最大 30、队列 2000、CallerRunsPolicy）
- **禁止忽略 `excludePatterns` 配置** -- 高并发下不排除低价值接口会产生大量效率日志记录
