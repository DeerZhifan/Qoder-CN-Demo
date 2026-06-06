package com.leatop.example.service.impl;

import com.leatop.cdp.data.exception.BusException;
import com.leatop.cdp.data.exception.UncheckedException;
import org.springframework.stereotype.Service;

/**
 * 异常处理使用示例
 *
 * CDP 框架异常体系：
 * - BusException：业务异常，映射为对应 HTTP 状态码返回给前端，用于业务校验失败场景
 * - UncheckedException：运行时异常，通过响应体 code 字段返回，用于系统级错误
 *
 * 全局异常处理器已在 leatop-cdp-common-core 中注册，无需业务代码额外配置。
 *
 * 注意：
 * - 不要直接抛 RuntimeException 或 IllegalArgumentException
 * - 不要在 Controller/Service 中 try-catch 后吞掉异常
 * - 不要用 try-catch 捕获后再打印日志，全局处理器会统一处理
 */
@Service
public class ExceptionUsageExample {

    public void validateAndProcess(String id, String type) {
        // 场景一：参数/数据校验失败 → BusException
        if (id == null || id.isBlank()) {
            throw new BusException("ID 不能为空");
        }

        // 场景二：业务数据不满足条件 → BusException
        if (!"VALID_TYPE".equals(type)) {
            throw new BusException("不支持的类型：" + type);
        }

        // 场景三：调用外部系统失败 → UncheckedException（包装原始异常）
        try {
            callExternalService();
        } catch (Exception e) {
            throw new UncheckedException("调用外部服务失败", e);
        }
    }

    private void callExternalService() throws Exception {
        // 模拟外部调用
    }
}
