package com.leatop.example;

import com.leatop.cdp.cache.annotation.EnableCdpCaching;
import com.leatop.cdp.lock.annotation.EnableCdpLock;
import com.xxl.job.core.config.EnableXxlJobExecutor;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CDP 框架主启动类示例
 *
 * 功能开关注解说明：
 * - @EnableCdpCaching    启用两级缓存（Caffeine L1 + Redis L2）
 * - @EnableCdpLock       启用基于 Redisson 的分布式锁
 * - @EnableXxlJobExecutor 启用 XXL-Job 定时任务执行器
 * - @EnableAdminServer   启用 Spring Boot Admin 监控
 *
 * 按需引入以上注解，不需要的功能不要加，避免引入不必要的依赖。
 */
@SpringBootApplication(scanBasePackages = {"com.leatop.example"})
@MapperScan(basePackages = {"com.leatop.example.dao"})
@EnableCdpCaching
@EnableCdpLock
@EnableXxlJobExecutor
@EnableAdminServer
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
