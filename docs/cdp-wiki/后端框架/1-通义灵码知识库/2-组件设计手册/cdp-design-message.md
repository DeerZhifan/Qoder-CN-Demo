# CDP 消息通知 设计手册

> 对应使用手册：[cdp-module-message.md](../3-组件使用手册/cdp-module-message.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

企业信息化系统中，消息通知是不可或缺的基础能力。不同业务场景对通知渠道的要求各不相同——审批提醒可能走短信或企业微信，运维告警适合通过钉钉/飞书机器人推送，正式通知则需要邮件。传统做法是每个业务模块自行对接各通道 SDK，导致重复编码和维护碎片化。

`leatop-cdp-business-message` 模块的设计目标是：

1. **统一发送入口**：业务代码只需调用一个接口（`MessageBusiness.sendMsg()`），由框架根据平台配置路由到对应通道。
2. **多通道可插拔**：支持短信（SMS）、邮件（EMAIL）、OA 机器人 Webhook（钉钉、企业微信、飞书）等通道，新增通道只需实现 `MessageSender` 接口。
3. **配置驱动**：消息平台的 AccessKey、密钥、Webhook 地址等参数存储在数据库（`frame_smsplatform` 表），通过管理后台维护，无需重启服务。
4. **日志可追溯**：每次发送自动记录完整日志（含流水号、状态、错误信息），便于排查和审计。
5. **单体/微服务通用**：通过 `@FeignClient` 声明式接口，单体模式走本地调用，微服务模式走远程调用，业务代码零修改。

## 二、整体架构

模块遵循 CDP 标准业务模块五层结构：

```
leatop-cdp-business-message/
  message-api            # 接口契约：Business 接口（@FeignClient）、DTO、QO、枚举
  message-service        # 核心实现：MessageSender 体系、通道适配、日志记录
  message-controller     # REST 端点
  message-boot-starter   # 单体模式自动配置
  message-cloud-starter  # 微服务模式自动配置
```

核心数据流为：

```
业务调用方 → MessageBusiness.sendMsg(SendMsgDTO)
           → MessageBusinessImpl 查询平台配置（SmsPlatformPO）
           → MessageSenderHolder 获取或创建 MessageSender 实例
           → 具体通道发送（SmsMessageSender / EmailMessageSender / WebHookMessageSender）
           → 返回 MsgResponse → 记录 SmsPlatformLogPO → 返回 SendResultDTO
```

> 设计决策：平台配置存数据库而非 YAML 文件。这样运营人员可以在管理界面动态增删通道、修改密钥，无需开发介入和服务重启。代价是每次发送需查库，但通过 `MessageSenderHolder` 的缓存机制有效缓解了这一开销。

## 三、核心设计模式

### 3.1 Strategy 模式：通道发送策略

`MessageSender` 是通道发送的统一策略接口，只定义一个方法 `sendMessage(String receiver, String msg)`，返回 `MsgResponse`。三个实现类分别封装了不同通道的发送逻辑：

- `SmsMessageSender` —— 短信通道，底层委托 sms4j 框架的 `SmsBlend` 接口。通过 `ProviderFactoryHolder` 注册了阿里、腾讯、华为、百度、网易、云片、亿息通等 15 家短信厂商的工厂实例，根据平台配置中的 `supplier` 字段动态选择厂商。
- `EmailMessageSender` —— 邮件通道，基于 dromara email-jakarta 库构建 `MailClient`，支持 HTML 邮件发送。
- `WebHookMessageSender` —— OA 机器人通道，支持钉钉（`DING_TALK`）、企业微信（`WE_TALK`）、飞书（`BYTE_TALK`）三种 Webhook 协议。

`MessageBusinessImpl` 在调用时通过 `SmsTypeEnum` 枚举决定使用哪个策略，完全屏蔽了通道差异。

### 3.2 Registry 模式：发送器缓存与生命周期

`MessageSenderHolder` 采用静态注册表模式管理 `MessageSender` 实例。内部维护两个 `ConcurrentHashMap`：`senders` 存放实例，`lastTime` 记录上次更新时间。Key 的格式为 `{code}_{tenantId}`，支持多租户场景下同一通道编码对应不同配置。

核心方法 `createIfAbsent()` 的策略是：如果缓存中的实例比数据库记录更新（通过 `updateGmt` 比较），直接返回缓存实例；否则重新创建。这样管理员在后台修改平台配置后，下一次发送即会自动加载新配置，无需手动清缓存。

> 设计决策：选择时间戳比较而非版本号。数据库中 `updateGmt` 字段由 MyBatis-Plus 自动维护，无需额外字段。缺点是在极端并发下存在微小的时间窗口竞态，但对消息发送场景影响可忽略。

### 3.3 Factory Method 模式：Webhook 消息体构建

`BaseOaSender.createMessage()` 根据 `OaType` 枚举委托给不同的 Builder 类构建 JSON 消息体：`WeTalkBuilder` 构建企业微信格式，`DingTalkBuilder` 构建钉钉格式（含签名），`ByteTalkBuilder` 构建飞书格式（含时间戳签名）。各 Builder 是纯静态工具类，职责单一且易于测试。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| `MessageBusiness` | 消息发送顶层接口，`@FeignClient` 标注，定义 `sendMsg()` 方法 |
| `MessageBusinessImpl` | 核心业务实现，查询平台配置、路由到对应发送器、记录日志 |
| `MessageSender` | 通道发送策略接口，定义 `sendMessage(receiver, msg)` |
| `SmsMessageSender` | 短信发送实现，集成 sms4j，静态注册 15 家厂商工厂 |
| `EmailMessageSender` | 邮件发送实现，基于 dromara email-jakarta 的 `MailClient` |
| `WebHookMessageSender` | Webhook 发送实现，委托 `OaSender` 处理协议差异 |
| `MessageSenderHolder` | 发送器注册表，按 `{code}_{tenantId}` 缓存实例，支持配置变更自动刷新 |
| `BaseOaSender` | OA Webhook 统一发送器，封装签名计算、HTTP POST、异步发送与优先级队列 |
| `OaType` | OA 平台类型枚举（钉钉、企业微信、飞书），含默认 Webhook URL 前缀 |
| `SmsTypeEnum` | 消息类型枚举：SMS(1)、QiYeWeiXin(2)、OARobot(3)、EMAIL(4) |
| `SmsPlatformPO` | 消息平台配置持久化实体，映射 `frame_smsplatform` 表 |
| `SmsPlatformLogPO` | 消息发送日志持久化实体，映射 `frame_smsplatform_log` 表 |
| `MsgResponse` | 发送结果内部模型，含 success、errorMsg、configId、data |
| `SendMsgDTO` | 发送请求 DTO，含 code、receiver、msg、serialNumber、tenantId |
| `SendResultDTO` | 发送结果 DTO，含平台 ID、状态码、错误信息 |
| `OaBeanFactory` | Webhook 基础设施工厂，管理线程池和优先级队列的单例初始化 |

## 五、扩展机制

### 5.1 新增短信厂商

sms4j 框架基于 SPI 机制，新增短信厂商只需：
1. 实现 `BaseProviderFactory<SmsBlend, SupplierConfig>` 接口。
2. 在 `SmsMessageSender` 的 `static` 块中调用 `ProviderFactoryHolder.registerFactory()` 注册。
3. 在数据库平台配置的 `settingParam` JSON 中填入对应的 `supplier` 标识和厂商参数。

框架已内置百度（`BaiduFactory`）和亿息通（`YiXiTongFactory`）两个自定义厂商扩展作为参考。

### 5.2 新增通道类型

若需新增如"站内信"通道：
1. 在 `SmsTypeEnum` 中新增枚举值。
2. 创建新的 `MessageSender` 实现类。
3. 在 `MessageSenderHolder.createMessageSender()` 的 switch 分支中添加映射。

### 5.3 异步发送与优先级

`OaSender` 接口预留了三种发送模式：同步 `sender()`、异步 `senderAsync()`（支持回调 `OaCallBack`）、按优先级异步 `senderAsyncByPriority()`（利用 `PriorityBlockingQueue`）。当前 `WebHookMessageSender` 使用同步模式，业务方可直接调用 `OaSender` 实例切换为异步模式。

### 5.4 多租户隔离

`MessageSenderHolder` 的缓存 Key 包含 `tenantId`，同一通道编码在不同租户下可配置不同的密钥和参数。`MessageBusinessImpl` 通过 `IUserHelper` 接口获取当前租户 ID，当显式传入 `tenantId` 时优先使用传入值。

## 六、模块协作

- **leatop-cdp-business-system**：消息模块依赖系统模块，共享用户体系（`IUserHelper`）和租户上下文。
- **leatop-cdp-common-data**：使用 `BasePo` 基类、`@BusinessService` 注解和 `Message<T>` 统一响应包装。
- **leatop-cdp-business-log**：发送日志通过 SLF4J 输出，可被日志模块采集；同时 `SmsPlatformLogPO` 提供结构化的发送记录查询。

## 七、设计权衡与约束

> 设计决策：选择 sms4j 作为短信底层框架而非自行封装各厂商 SDK。sms4j 已对接主流云厂商且社区活跃，避免重复造轮子。代价是引入了外部依赖，但通过 `MessageSender` 接口隔离，未来替换底层实现的影响范围可控。

> 设计决策：`MessageSenderHolder` 使用静态 `ConcurrentHashMap` 而非 Spring Bean。这是因为发送器实例的生命周期由平台配置驱动（数据库记录增删改），与 Spring Bean 的生命周期不匹配。静态注册表更灵活，但牺牲了 Spring 容器的依赖注入便利性。

> 设计决策：每次发送都查数据库获取平台配置（`smsPlatformDAO.list()`），而非启动时一次性加载。这保证了配置变更的即时性，但增加了数据库访问。`MessageSenderHolder` 的缓存机制通过 `updateGmt` 比较避免了不必要的实例重建，是对性能的折中优化。

> 设计决策：`OaSenderFactory` 类已被注释掉，团队将 Webhook 发送器的创建逻辑合并到 `WebHookMessageSender` 构造函数中，由 `BaseOaSender` 统一处理三种 OA 平台的协议差异。这简化了类层次，但降低了对新 OA 平台的开放性——新增平台需要修改 `BaseOaSender.createMessage()` 中的 switch 分支。
