# CDP 框架代码片段库

本目录为通义灵码知识库代码示例素材，所有代码均从 `leatop-cdp-example-demo1` 真实代码提取并整理。

## 文件说明

| 文件 | 内容 |
|------|------|
| `01-main-application.java` | 主启动类 — 功能开关注解的标准用法 |
| `02-standard-po.java` | PO 实体类标准写法 |
| `03-standard-dto.java` | DTO 传输对象标准写法 |
| `04-standard-qo.java` | QO 查询参数对象标准写法 |
| `05-standard-dao.java` | DAO 层标准写法（继承 BaseMapper） |
| `06-standard-service-interface.java` | Service 接口标准写法 |
| `07-standard-service-impl.java` | Service 实现层标准写法（含分页、删除、异常） |
| `08-standard-controller.java` | Controller 层标准写法（含分组校验） |
| `09-cache-usage.java` | 缓存使用示例（注解 + 编程式两种方式） |
| `10-lock-usage.java` | 分布式锁使用示例（注解 + 编程式两种方式） |
| `11-exception-usage.java` | 异常处理示例（BusException / UncheckedException） |
| `12-xxljob-usage.java` | XXL-Job 定时任务示例（简单任务 + 分片广播） |
| `13-mybatisplus-query.java` | MyBatis-Plus 常用查询写法（LambdaQueryWrapper） |

## 使用方式

将整个 `snippets/` 目录导入通义灵码企业知识库，AI 在生成代码时会参考这些示例，
从而生成符合 CDP 框架规范的代码。

## 核心规范摘要（供 AI 快速检索）

```
分层：Controller（接收/校验/转发）→ Service（业务逻辑）→ Dao（数据库）
模型：PO（数据库）/ DTO（传输）/ QO（查询），严格区分，不混用
异常：业务失败用 BusException，系统错误用 UncheckedException
缓存：@EnableCdpCaching + @Cacheable，禁止直接用 RedisTemplate
锁：@EnableCdpLock + @Lock 注解 或 CdpLockClient，禁止直接用 RedissonClient
返回值：统一用 Message<T> 包装，分页用 Message<Page<T>>
工具类：优先用 BeanUtil / StrUtil / CollUtil（框架或 Hutool），不要重复造轮子
```
