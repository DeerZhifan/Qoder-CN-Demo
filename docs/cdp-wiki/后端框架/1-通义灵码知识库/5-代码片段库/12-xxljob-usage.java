package com.leatop.example.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXL-Job 定时任务使用示例
 *
 * 前提：主类加 @EnableXxlJobExecutor，application.yaml 配置 xxl-job 地址
 *
 * 开发步骤：
 * 1. 在 Spring Bean 中编写 @XxlJob 方法
 * 2. 在 XXL-Job 调度中心新建任务，JobHandler 填写 @XxlJob 的 value 值
 * 3. 日志用 XxlJobHelper.log()，不要用 System.out 或 log.info
 * 4. 失败时调用 XxlJobHelper.handleFail()
 */
@Component
@Slf4j
public class XxlJobUsageExample {

    /**
     * 简单定时任务（最常用）
     * JobHandler 名称：sampleJobHandler
     */
    @XxlJob("sampleJobHandler")
    public void sampleJob() {
        XxlJobHelper.log("任务开始执行");
        try {
            // 业务逻辑
            XxlJobHelper.log("任务执行成功");
            // 不需要显式调用 handleSuccess，默认就是成功
        } catch (Exception e) {
            XxlJobHelper.log("任务执行失败：" + e.getMessage());
            XxlJobHelper.handleFail("任务失败：" + e.getMessage());
        }
    }

    /**
     * 分片广播任务（适合大数据量并行处理）
     * JobHandler 名称：shardingJobHandler
     *
     * 场景：多个执行器实例同时运行，每个实例处理数据的一个分片
     */
    @XxlJob("shardingJobHandler")
    public void shardingJob() {
        int shardIndex = XxlJobHelper.getShardIndex(); // 当前分片索引（从0开始）
        int shardTotal = XxlJobHelper.getShardTotal(); // 总分片数

        XxlJobHelper.log("分片参数：当前={}, 总数={}", shardIndex, shardTotal);

        // 示例：按 id % shardTotal == shardIndex 筛选当前分片数据
        // List<Long> ids = xxxDao.selectIdsBySharding(shardIndex, shardTotal);
        // ids.forEach(id -> process(id));
    }

    /**
     * 带初始化和销毁回调的任务
     */
    @XxlJob(value = "lifecycleJobHandler", init = "initJob", destroy = "destroyJob")
    public void lifecycleJob() {
        XxlJobHelper.log("执行任务");
    }

    public void initJob() {
        log.info("任务初始化");
    }

    public void destroyJob() {
        log.info("任务销毁");
    }
}
