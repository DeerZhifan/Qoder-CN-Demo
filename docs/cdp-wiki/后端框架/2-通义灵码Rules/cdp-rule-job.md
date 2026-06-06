---
trigger: when_referenced
knowledge_source:
  - cdp-design-job
  - cdp-module-job
  - 12-xxljob-usage.java
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-job-boot-starter` 或 `leatop-cdp-base-job-core` 依赖
- 使用 `@EnableXxlJobExecutor` 注解
- 使用 `@XxlJob` 注解定义任务处理器
- 使用 `XxlJobHelper` 工具类
- 配置 `xxl.job.executor.*` 属性

---

## 前置依赖

1. Maven 依赖（二选一）：

```xml
<!-- 含调度中心管理界面 + 执行器（单体应用） -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-job-boot-starter</artifactId>
</dependency>

<!-- 仅执行器（不含管理界面） -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-job-core</artifactId>
</dependency>
```

2. 启动类添加注解：

```java
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@EnableXxlJobExecutor  // 开启 XXL-Job 执行器
public class Application { ... }
```

3. 使用 `boot-starter` 时需要数据库中存在 `xxl_job_*` 系列表（随 Flyway 迁移脚本管理）。

---

## 配置要点

```yaml
xxl:
  job:
    executor:
      adminAddresses: http://172.17.1.28:28080/xxl-job-admin  # 调度中心地址（必填）
      accessToken: faa60c091fb047c993c5fee20385c1c5            # 访问令牌，需与调度中心一致
      appname: my-executor                                     # 执行器名称
      port: 9998                                               # 执行器端口
      logPath: /data/applogs/xxl-job/jobhandler                # 日志路径
      logRetentionDays: 30                                     # 日志保留天数
```

- `xxl.job.executor.enabled` 默认 `true`，设为 `false` 可在不移除依赖的情况下禁用执行器
- `accessToken` 必须与调度中心配置一致，否则执行器无法注册
- 使用 `boot-starter` 时调度中心嵌入应用内部，`adminAddresses` 指向自身地址即可

---

## 代码模式

### 推荐写法

**@XxlJob 注解方式（推荐）**

```java
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class SampleXxlJob {

    @XxlJob("sampleJobHandler")
    public void sampleJob() {
        XxlJobHelper.log("任务开始执行");
        try {
            // 业务逻辑
            XxlJobHelper.log("任务执行成功");
        } catch (Exception e) {
            XxlJobHelper.log("任务执行失败：" + e.getMessage());
            XxlJobHelper.handleFail("任务失败：" + e.getMessage());
        }
    }

    @XxlJob("shardingJobHandler")
    public void shardingJob() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("分片参数：当前={}, 总数={}", shardIndex, shardTotal);
    }
}
```

**带生命周期回调的任务**

```java
@XxlJob(value = "lifecycleJobHandler", init = "initJob", destroy = "destroyJob")
public void lifecycleJob() {
    XxlJobHelper.log("执行任务");
}

public void initJob() { /* 任务初始化 */ }
public void destroyJob() { /* 任务销毁 */ }
```

**XxlJobHelper 常用 API**

| 方法 | 说明 |
|------|------|
| `XxlJobHelper.log(msg)` | 向调度中心输出日志 |
| `XxlJobHelper.getJobParam()` | 获取任务传递参数 |
| `XxlJobHelper.handleFail(msg)` | 标记任务失败 |
| `XxlJobHelper.handleSuccess(msg)` | 标记任务成功 |
| `XxlJobHelper.getShardIndex()` | 获取当前分片序号 |
| `XxlJobHelper.getShardTotal()` | 获取总分片数 |

### 禁止事项

- **禁止在 Handler 方法中使用 `System.out` 或 `log.info` 替代 `XxlJobHelper.log()`** -- 调度中心只能采集 `XxlJobHelper.log()` 的输出，使用其他方式输出的日志在调度中心不可见
- **禁止 `@XxlJob` 的 value 值与调度中心注册的 JobHandler 名称不一致** -- 名称必须完全匹配，否则任务无法触发
- **禁止在不需要执行器的模块中引入 `boot-starter` 而不禁用** -- `boot-starter` 包含调度中心，会扫描 `xxl_job_*` 数据库表；仅需执行器功能应使用 `job-core`
- **禁止忽略异常处理** -- 任务默认结果为成功，失败时必须主动调用 `XxlJobHelper.handleFail()` 标记，否则调度中心无法感知失败
- **禁止在非 Spring Bean 类中使用 `@XxlJob` 注解** -- 该注解依赖 Spring 容器扫描，非 Bean 类中的 Handler 不会被注册
- **禁止手动注册 Handler 而不实现 `InitializingBean`** -- 使用 `IJobHandler` 接口方式时，必须在 `afterPropertiesSet()` 中调用 `XxlJobExecutor.registJobHandler()` 完成注册
