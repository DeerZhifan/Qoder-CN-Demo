# 如何使用 CDP 任务调度组件

## 概述

任务调度组件（`leatop-cdp-base-job`）基于 XXL-JOB 2.4.0 改造适配，包含调度中心和执行器两部分。调度中心负责任务管理和调度，执行器负责接收调度并执行具体任务逻辑。

## 启用方式

### 调度中心（含管理界面）

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-job-boot-starter</artifactId>
</dependency>
```

### 仅执行器（不含管理界面）

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-job-core</artifactId>
</dependency>
```

在启动类添加注解：

```java
@SpringBootApplication
@EnableXxlJobExecutor  // 开启 XXL-Job 执行器
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

## 配置项

```yaml
xxl:
  job:
    executor:
      # 调度中心地址（必填）
      adminAddresses: http://172.17.1.28:28080/xxl-job-admin
      # 访问令牌，需与调度中心配置一致
      accessToken: faa60c091fb047c993c5fee20385c1c5
      # 执行器名称，在调度中心注册使用
      appname: my-executor
      # 执行器端口号
      port: 9998
      # 日志存放路径
      logPath: /data/applogs/xxl-job/jobhandler
      # 日志保留天数
      logRetentionDays: 30
```

> 注意：`xxl.job.executor.enabled` 默认为 `true`，设置为 `false` 可禁用执行器。

## 使用示例

### 方式一：@XxlJob 注解（推荐）

```java
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.context.XxlJobHelper;
import org.springframework.stereotype.Component;

@Component
public class SampleXxlJob {

    /**
     * 简单任务示例
     * 注解 value 值对应调度中心配置的 JobHandler 名称
     */
    @XxlJob("demoSpringJobHandler")
    public void demoJobHandler() throws Exception {
        XxlJobHelper.log("Spring, Hello World.");
    }

    /**
     * 分片广播任务示例
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("当前分片：{}, 总分片数：{}", shardIndex, shardTotal);
    }
}
```

### 方式二：实现 IJobHandler 接口

```java
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.context.XxlJobHelper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class CustomJobHandler extends IJobHandler implements InitializingBean {

    private static final String JOB_HANDLER_NAME = "customJobHandler";

    @Override
    public void execute() throws Exception {
        XxlJobHelper.log("执行自定义任务");
        XxlJobHelper.handleSuccess("执行成功");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        XxlJobExecutor.registJobHandler(JOB_HANDLER_NAME, this);
    }
}
```

### XxlJobHelper 常用方法

| 方法 | 说明 |
|------|------|
| `XxlJobHelper.log(msg)` | 向调度中心输出日志 |
| `XxlJobHelper.getJobParam()` | 获取任务传递参数 |
| `XxlJobHelper.getJobId()` | 获取当前任务 ID |
| `XxlJobHelper.handleFail(msg)` | 标记任务失败 |
| `XxlJobHelper.handleSuccess(msg)` | 标记任务成功 |
| `XxlJobHelper.getShardIndex()` | 获取当前分片序号 |
| `XxlJobHelper.getShardTotal()` | 获取总分片数 |

## 注意事项

> 注意：`@XxlJob` 注解的 `value` 值必须与调度中心任务管理中配置的 JobHandler 名称一致。

> 注意：执行器和调度中心的 `accessToken` 必须保持一致，否则执行器无法注册。

> 注意：任务默认结果为"成功"状态，如需标记失败需主动调用 `XxlJobHelper.handleFail()`。

> 注意：使用 `leatop-cdp-base-job-boot-starter` 会同时引入调度中心管理界面，需配置对应数据库表。仅需执行器功能请使用 `leatop-cdp-base-job-core`。
