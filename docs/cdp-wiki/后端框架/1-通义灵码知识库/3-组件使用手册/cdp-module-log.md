# 如何使用 CDP 日志管理组件

## 概述

日志管理组件（`leatop-cdp-business-log`）提供四类日志的自动记录和查询分析：登录登出日志、操作日志、授权日志和访问效率日志。通过注解方式声明需要记录的方法，框架自动采集。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-log-boot-starter</artifactId>
</dependency>
```

## 配置项

```yaml
cdp:
  syslog:
    enable: true                    # 是否启用效率日志
    excludePatterns:                # 排除的接口路径
      - /xxl-job-admin/api/**
      - /system/holiday/**
    # includePatterns:              # 指定监控的接口（默认全部）
    #   - /system/**
```

## 核心注解

### @LogParam — 操作日志

记录业务操作行为，支持 SpEL 表达式获取方法参数。

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

`@LogParam` 参数：

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `operateType` | 操作类型枚举（`OperateType`） | — |
| `module` | 模块名称（覆盖 `@LogModule`） | — |
| `logDes` | 日志描述，支持 SpEL 表达式 | — |
| `isLogEx` | 是否记录异常信息 | `true` |

### @EfficiencyLog — 访问效率日志

记录方法执行耗时和访问详情。

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

`@EfficiencyLog` 参数：

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `module` | 模块名称 | — |
| `ignore` | 忽略返回结果（`"result"`） | — |
| `isLogEx` | 是否记录异常信息 | `true` |

### @LogModule — 模块声明

标注在类上，声明该类所属的日志模块名称，被 `@LogParam` 和 `@EfficiencyLog` 继承。

## 日志查询接口

- `LogCollectController` — 日志采集接口
- `LogAnalysisController` — 日志查询和分析接口

## 相关数据库表

| 表名 | 说明 |
|------|------|
| `frame_log_login` | 登录登出日志 |
| `frame_log_operate` | 操作日志 |
| `frame_log_authorize` | 授权日志 |
| `frame_log_efficiency` | 访问效率日志 |

## 注意事项

> 注意：`@LogParam` 的 `logDes` 支持 SpEL 表达式，通过 `#参数名` 引用方法参数值。

> 注意：登录登出日志由框架自动记录，无需手动添加注解。

> 注意：效率日志可通过 `excludePatterns` 排除高频低价值的接口，减少日志量。

> 注意：`@EfficiencyLog` 设置 `ignore = "result"` 可避免记录大量返回数据，降低存储压力。
