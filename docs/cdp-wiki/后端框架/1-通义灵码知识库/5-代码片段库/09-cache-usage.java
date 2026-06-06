package com.leatop.example.service.impl;

import com.leatop.cdp.cache.CdpCache;
import com.leatop.cdp.cache.CdpCacheClient;
import com.leatop.cdp.data.exception.BusException;
import com.leatop.example.model.po.ApplicationPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 缓存使用示例（两种方式）
 *
 * 方式一：Spring Cache 注解（推荐，简洁）
 * 方式二：CdpCacheClient 编程式操作（需要精细控制时使用）
 *
 * 前提：主类加 @EnableCdpCaching
 */
@Service
// 方式一：@CacheConfig 设置该类所有方法的默认缓存名称，也可在各方法单独指定
@CacheConfig(cacheNames = "myCache")
public class CacheUsageExample {

    // 方式二所需：注入编程式缓存客户端
    @Autowired(required = false)
    private CdpCacheClient cdpCacheClient;

    // ========== 方式一：Spring Cache 注解 ==========

    /**
     * 查询时缓存结果。
     * - key 支持 SpEL 表达式，#id 取参数值
     * - unless = "#result==null" 表示结果为 null 时不缓存
     */
    @Cacheable(key = "#id", unless = "#result==null")
    public ApplicationPO getById(String id) {
        // 实际业务：从数据库查询
        return null;
    }

    /**
     * 更新时同步刷新缓存（先执行方法，再用返回值更新缓存）
     */
    @CachePut(key = "#po.id")
    public ApplicationPO updateAndCache(ApplicationPO po) {
        // 实际业务：更新数据库
        return po;
    }

    /**
     * 删除时清除对应缓存
     */
    @CacheEvict(key = "#id")
    public void deleteAndEvict(String id) {
        // 实际业务：删除数据库记录
    }

    // ========== 方式二：CdpCacheClient 编程式操作 ==========

    private static final String APP_CACHE_NAME = "appCache";

    /**
     * 先查缓存，缓存未命中再查数据库，并写入缓存（Cache-Aside 模式）
     */
    public ApplicationPO getWithCacheAside(String appId) {
        CdpCache cache = cdpCacheClient.getCache(APP_CACHE_NAME);

        // 尝试从缓存获取
        ApplicationPO result = cache.get(appId, ApplicationPO.class);
        if (Objects.nonNull(result)) {
            return result;
        }

        // 缓存未命中，从数据库查询
        result = queryFromDb(appId);
        if (Objects.isNull(result)) {
            throw new BusException("应用不存在，id=" + appId);
        }

        // 写入缓存
        cache.put(appId, result);
        return result;
    }

    private ApplicationPO queryFromDb(String appId) {
        // 实际业务：从数据库查询
        return null;
    }
}
