# 生成 XXL-Job Handler

## 描述

根据用户提供的任务信息，生成 XXL-Job Handler 类文件，包含 `@XxlJob` 注解、标准异常处理骨架和调度中心日志输出。生成后需在 XXL-Job 调度中心注册对应任务。

## 输入

请向用户确认以下信息：

1. **Handler 名称**（`@XxlJob` 注解的 value 值，如 `orderSyncJobHandler`）
2. **Cron 表达式**（调度周期，如 `0 0/5 * * * ?` 每 5 分钟执行）
3. **任务描述**（中文说明，如 `订单数据同步`）
4. **所在包名**（如 `com.leatop.example.jobhandler`）
5. **类名**（如 `OrderSyncXxlJob`，默认根据 Handler 名称自动推导）
6. **是否需要分片**（默认 `否`）

---

## 步骤 1：生成 Handler 类文件

文件路径：`src/main/java/{包路径}/{类名}.java`

**普通任务模板：**

```java
package {包名};

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {任务描述} - XXL-Job Handler
 *
 * <p>JobHandler 名称：{Handler名称}</p>
 * <p>调度周期：{Cron表达式}</p>
 */
@Component
@Slf4j
public class {类名} {

    /**
     * {任务描述}
     * <p>JobHandler：{Handler名称}</p>
     * <p>Cron：{Cron表达式}</p>
     */
    @XxlJob("{Handler名称}")
    public void execute() {
        XxlJobHelper.log("{任务描述} - 开始执行");
        try {
            String param = XxlJobHelper.getJobParam();
            XxlJobHelper.log("任务参数：{}", param);

            // TODO 业务逻辑

            XxlJobHelper.log("{任务描述} - 执行完成");
        } catch (Exception e) {
            XxlJobHelper.log("{任务描述} - 执行失败：{}", e.getMessage());
            XxlJobHelper.handleFail("{任务描述}失败：" + e.getMessage());
        }
    }
}
```

**分片任务模板（当用户选择需要分片时使用）：**

```java
package {包名};

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {任务描述} - XXL-Job 分片广播 Handler
 *
 * <p>JobHandler 名称：{Handler名称}</p>
 * <p>调度周期：{Cron表达式}</p>
 * <p>路由策略：分片广播</p>
 */
@Component
@Slf4j
public class {类名} {

    /**
     * {任务描述}（分片广播模式）
     * <p>JobHandler：{Handler名称}</p>
     * <p>Cron：{Cron表达式}</p>
     */
    @XxlJob("{Handler名称}")
    public void execute() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("{任务描述} - 开始执行，分片：{}/{}", shardIndex, shardTotal);
        try {
            String param = XxlJobHelper.getJobParam();
            XxlJobHelper.log("任务参数：{}", param);

            // TODO 按分片处理业务数据
            // 示例：List<Long> ids = xxxDao.selectIdsBySharding(shardIndex, shardTotal);
            // ids.forEach(id -> process(id));

            XxlJobHelper.log("{任务描述} - 分片 {} 执行完成", shardIndex);
        } catch (Exception e) {
            XxlJobHelper.log("{任务描述} - 分片 {} 执行失败：{}", shardIndex, e.getMessage());
            XxlJobHelper.handleFail("{任务描述}分片" + shardIndex + "失败：" + e.getMessage());
        }
    }
}
```

---

## 步骤 2：在调度中心注册任务

> 代码编写完成后，需在 XXL-Job 调度中心管理界面创建对应任务，将 JobHandler 名称与 `@XxlJob` 注解的 value 值关联。

提醒用户完成以下操作：

1. 访问调度中心管理界面
2. 进入「任务管理」，点击「新增」
3. 填写任务信息：
   - **执行器**：选择当前应用注册的执行器
   - **任务描述**：`{任务描述}`
   - **JobHandler**：`{Handler名称}`
   - **Cron**：`{Cron表达式}`
   - **路由策略**：普通任务选「轮询」或「故障转移」，分片任务选「分片广播」
   - **运行模式**：BEAN
4. 保存并启动任务

---

## 完成后提醒

1. 确认主启动类已添加 `@EnableXxlJobExecutor` 注解，且 `xxl.job.executor.*` 已正确配置
2. `@XxlJob` 的 value 值必须与调度中心的 JobHandler 名称完全一致
3. Handler 内部日志必须使用 `XxlJobHelper.log()`，不要用 `System.out` 或 `log.info`
4. 异常情况必须调用 `XxlJobHelper.handleFail()` 标记失败，否则调度中心默认任务成功
5. 分片任务需在调度中心选择「分片广播」路由策略才能生效
