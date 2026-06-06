# 通义灵码项目级自定义指令

> 将以下内容复制到通义灵码 → 设置 → 项目级自定义指令（Custom Instructions）中。
> 配置后，每次 AI 对话将自动注入这些约束，无需开发者手动说明框架规范。

---

## 指令内容（直接复制以下文本）

```
你正在辅助开发基于 CDP 框架的 Java 项目，技术栈为：
Java 17 + Spring Boot 3.5 + Spring Cloud 2025 + MyBatis-Plus 3.5 + SA-Token 1.44

【分层规范】
- Controller 层只做：参数接收、@Validated 校验、调用 Service 方法、返回 Message<T>
- Service 层负责所有业务逻辑，不在 Controller 中写业务代码
- Dao 层继承 MyBatis-Plus BaseMapper，简单查询用 LambdaQueryWrapper，复杂 SQL 写 XML

【模型命名规范】
- PO：数据库实体，加 @TableName/@TableId/@TableField，实现 Serializable
- DTO：传输对象，用于 Controller 入参/出参和 Service 层数据传递，不加数据库注解
- QO：查询参数对象，分页查询继承 PageQo，不做其他用途
- 三类模型严格区分，禁止混用

【返回值规范】
- 所有接口返回值统一用 Message<T> 包装
- 分页接口返回 Message<Page<T>>
- 不要返回裸对象或 Map

【异常处理规范】
- 业务校验失败（参数非法、数据不存在、状态不对）→ 抛 BusException
- 系统级错误（调用失败、IO异常）→ 抛 UncheckedException，包装原始异常
- 禁止抛 RuntimeException 或 IllegalArgumentException
- 禁止 try-catch 后吞掉异常或仅打印日志

【缓存规范】
- 启用：主类加 @EnableCdpCaching（L1 Caffeine + L2 Redis 两级缓存）
- 使用：@Cacheable/@CachePut/@CacheEvict 注解，或注入 CdpCacheClient 编程操作
- 禁止：直接使用 RedisTemplate 或 StringRedisTemplate 操作缓存

【分布式锁规范】
- 启用：主类加 @EnableCdpLock（底层 Redisson）
- 使用：@Lock(key="#xxx") 注解，或注入 CdpLockClient 编程操作
- 禁止：直接使用 RedissonClient
- 锁 key 命名：业务前缀:业务ID，如 "order:submit:userId:123"

【定时任务规范】
- 启用：主类加 @EnableXxlJobExecutor
- 使用：@XxlJob("handlerName") 注解方法，日志用 XxlJobHelper.log()
- 失败时调用 XxlJobHelper.handleFail()

【依赖管理规范】
- 新增第三方库：先在 leatop-cdp-dependencies/pom.xml 的 dependencyManagement 中声明版本
- 子模块 pom.xml 引用时不写版本号
- 内部模块版本由 leatop-cdp-bom 统一管理

【接口文档规范】
- 使用 Smart-doc 注解规范写 JavaDoc
- 禁止引入 Swagger/SpringDoc 相关注解（@Api、@ApiOperation 等）
- 每个 Controller 方法必须有 JavaDoc 注释（供 Smart-doc 生成文档）

【工具类规范】
- 对象复制：BeanUtil.copyProperties / BeanUtil.copyToList（框架工具）
- 字符串处理：StrUtil（框架工具，基于 Hutool）
- 集合判空：CollUtil.isEmpty / CollUtil.isNotEmpty
- 禁止引入 Apache Commons、Guava 等重复功能的工具库
```

---

## 配置说明

### 在 VS Code / JetBrains 中配置

1. 打开通义灵码插件设置
2. 找到 **自定义指令** 或 **Custom Instructions** 选项
3. 将上方代码块中的内容粘贴进去
4. 保存，重启 IDE 生效

### 验证是否生效

配置后，在对话框输入以下问题验证：

| 测试问题 | 期望回答包含 |
|----------|-------------|
| 帮我写一个查询用户的 Service 方法 | `Message<UserDTO>` 作为返回值 |
| 如何在 CDP 框架中使用缓存？ | `@EnableCdpCaching` + `@Cacheable` |
| 数据不存在时应该怎么抛异常？ | `BusException` |
| 帮我写一个分页查询接口 | `PageQo`、`LambdaQueryWrapper`、`Page.of` |
