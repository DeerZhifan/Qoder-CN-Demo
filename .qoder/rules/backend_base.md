---
trigger: always_on
alwaysApply: true
---
# 知识库后台 · 后端通用规范

适用范围：backend/ 下所有 Spring Boot + MyBatis-Plus 代码。

## 接口返回
- 统一返回 Result<T>：成功用 Result.success(data)，失败用 Result.error(code, msg)
- 禁止裸返回实体、Map 或 String
- Do：return Result.success(page);

## 异常处理
- 不在 Controller/Service 里 try-catch 吞异常
- 参数非法抛 IllegalArgumentException；状态非法抛 IllegalStateException；资源不存在抛 NoSuchElementException
- 统一由 @RestControllerAdvice（GlobalExceptionHandler）兜底转成 Result.error

## 事务
- 增、删、改、发布/下线、版本回滚等写操作必须加 @Transactional

## 数据访问
- 统一用 MyBatis-Plus 的 LambdaQueryWrapper 或内置方法，禁止手写字符串拼接 SQL（防注入）
- 逻辑删除用 @TableLogic 字段，禁止物理删除（deleteById 走逻辑删除）

## 日志
- 用 SLF4J：类上加 @Slf4j，调用 log.info/warn/error，禁止 System.out.println
- 不打印 content 正文等大字段；不打印密码、token 等敏感信息