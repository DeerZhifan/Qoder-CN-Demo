package com.leatop.example.service;

import com.leatop.cdp.lock.CdpLock;
import com.leatop.cdp.lock.CdpLockClient;
import com.leatop.cdp.lock.annotation.Lock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁使用示例（三种方式）
 *
 * 前提：主类加 @EnableCdpLock（底层基于 Redisson）
 *
 * 注意：
 * - 禁止直接使用 RedissonClient 操作锁，统一走框架封装
 * - 锁 key 命名规范：业务前缀:业务ID，如 "order:submit:userId:123"
 */
@Service
@Slf4j
public class LockUsageExample {

    @Autowired
    private CdpLockClient cdpLockClient;

    // ========== 方式一：@Lock 注解 + SpEL key（推荐） ==========

    /**
     * 按参数 key 加锁，waitTime=1 表示等待 1 秒获取不到锁则放弃
     * key 支持 SpEL：#userId 取参数值，可拼接字符串 "'order:'+#userId"
     */
    @Lock(key = "#userId", waitTime = 1)
    public String submitOrder(String userId) {
        // 同一 userId 的请求串行执行
        return "下单成功";
    }

    /**
     * 不指定 key 时使用全局锁（方法级别互斥）
     */
    @Lock
    public void globalSerialTask() {
        // 全局只有一个线程在执行此方法
    }

    // ========== 方式二：编程式加锁（需要精细控制超时时的业务逻辑） ==========

    public String submitOrderWithFallback(String userId) throws InterruptedException {
        CdpLock lock = cdpLockClient.getLock("order:submit:" + userId);
        boolean acquired = lock.tryLock(1, TimeUnit.SECONDS);
        if (!acquired) {
            // 获取锁失败，返回友好提示
            return "操作频繁，请稍后重试";
        }
        try {
            // 执行业务逻辑
            return "下单成功";
        } finally {
            // 必须在 finally 中释放锁
            lock.unlock();
        }
    }
}
