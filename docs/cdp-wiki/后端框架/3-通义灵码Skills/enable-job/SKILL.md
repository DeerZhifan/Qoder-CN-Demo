# 启用 CDP 任务调度组件

## 描述

在已有 CDP 项目中启用任务调度组件（`leatop-cdp-base-job`），基于 XXL-JOB 2.4.0 提供分布式定时任务调度能力。包含嵌入式调度中心和执行器，通过 `@EnableXxlJobExecutor` 注解激活，支持 `@XxlJob` 注解快速定义任务处理器。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`standalone` 含调度中心 或 `executor-only` 仅执行器，默认 `standalone`）
3. **执行器名称**（在调度中心注册的 appname，如 `my-executor`）
4. **调度中心地址**（如 `http://172.17.1.28:28080/xxl-job-admin`，standalone 模式指向自身）
5. **执行器端口**（默认 `9998`）

---

## 步骤 1：添加 Maven 依赖

> 根据部署模式选择依赖。`boot-starter` 包含完整调度中心管理界面和执行器（需要数据库表支持），`job-core` 仅包含执行器。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

**含调度中心（standalone 模式）：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-job-boot-starter</artifactId>
</dependency>
```

**仅执行器（executor-only 模式）：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-job-core</artifactId>
</dependency>
```

## 步骤 2：添加启用注解

> `@EnableXxlJobExecutor` 通过 `@Import` 导入 `XxlJobExecutorConfig` 配置类，绑定 `xxl.job.executor.*` 配置属性并创建 `XxlJobSpringExecutor` Bean。默认启用，可通过 `xxl.job.executor.enabled=false` 禁用。

在主启动类上添加：

```java
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@MapperScan("com.leatop.cdp.**.dao")
@EnableXxlJobExecutor  // 开启 XXL-Job 执行器
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 步骤 3：添加 YAML 配置

> 配置执行器连接调度中心的必要参数。`accessToken` 必须与调度中心配置一致，否则执行器无法注册。

在 `application-dev.yaml` 中添加：

```yaml
xxl:
  job:
    executor:
      adminAddresses: {调度中心地址}
      accessToken: faa60c091fb047c993c5fee20385c1c5
      appname: {执行器名称}
      port: {执行器端口}
      logPath: /data/applogs/xxl-job/jobhandler
      logRetentionDays: 30
```

## 步骤 4：编写 Handler 示例

> 使用 `@XxlJob` 注解定义任务处理器。注解 value 值对应调度中心任务管理中配置的 JobHandler 名称，必须完全匹配。日志输出使用 `XxlJobHelper.log()`，失败时调用 `XxlJobHelper.handleFail()`。

```java
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SampleXxlJob {

    /**
     * 简单定时任务
     * JobHandler 名称：sampleJobHandler
     */
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

    /**
     * 分片广播任务（适合大数据量并行处理）
     * JobHandler 名称：shardingJobHandler
     */
    @XxlJob("shardingJobHandler")
    public void shardingJob() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("分片参数：当前={}, 总数={}", shardIndex, shardTotal);
        // 按 id % shardTotal == shardIndex 筛选当前分片数据
    }
}
```

## 步骤 5：在调度中心注册任务

> 编写完 Handler 代码后，需要在 XXL-Job 调度中心管理界面中创建任务，将 JobHandler 名称与代码中 `@XxlJob` 的 value 值对应。

1. 访问调度中心管理界面（如 `http://172.17.1.28:28080/xxl-job-admin`）
2. 进入「任务管理」，点击「新增」
3. 填写任务信息：
   - **执行器**：选择已注册的执行器（appname）
   - **JobHandler**：填写 `@XxlJob` 注解的 value 值（如 `sampleJobHandler`）
   - **Cron**：配置调度表达式（如 `0 0/5 * * * ?` 每 5 分钟执行）
   - **路由策略**：选择合适的策略（轮询、随机、故障转移等）
4. 保存并启动任务

## 步骤 6：验证

启动应用，检查以下内容：

1. 控制台日志中出现 `XxlJobSpringExecutor` 初始化信息
2. 调度中心「执行器管理」中能看到已注册的执行器实例
3. 手动触发任务，确认 Handler 正常执行
4. 在调度中心「调度日志」中查看执行日志

---

## 完成后提醒

1. `@XxlJob` 的 value 值必须与调度中心任务管理中配置的 JobHandler 名称完全一致
2. 任务内日志输出必须使用 `XxlJobHelper.log()`，不要使用 `System.out` 或 `log.info`
3. 任务默认结果为成功，失败时必须主动调用 `XxlJobHelper.handleFail()` 标记
4. 使用 `boot-starter` 会引入调度中心管理界面和 `xxl_job_*` 数据库表依赖；仅需执行器功能请使用 `job-core`
5. 不需要执行器时，设置 `xxl.job.executor.enabled: false` 即可禁用，无需移除依赖
