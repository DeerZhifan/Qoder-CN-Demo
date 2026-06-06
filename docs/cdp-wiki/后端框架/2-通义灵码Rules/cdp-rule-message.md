---
trigger: when_referenced
knowledge_source:
  - cdp-design-message
  - cdp-module-message
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-business-message-boot-starter` 或 `leatop-cdp-business-message-cloud-starter` 依赖
- 使用 `MessageBusiness` 接口发送消息
- 使用 `SendMsgDTO`、`SendResultDTO` 等消息 DTO
- 操作 `frame_smsplatform`、`frame_smsplatform_log` 表
- 涉及短信（SMS）、邮件（EMAIL）、OA 机器人 Webhook 通道

---

## 前置依赖

1. Maven 依赖（单体模式）：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-message-boot-starter</artifactId>
</dependency>
```

微服务模式使用 `leatop-cdp-business-message-cloud-starter`。

2. 必须同时引入系统管理组件：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-system-boot-starter</artifactId>
</dependency>
```

3. 数据库中需包含 `frame_smsplatform`（消息平台配置表）和 `frame_smsplatform_log`（发送日志表）。

---

## 配置要点

- 消息平台配置（AccessKey、密钥、Webhook 地址等）存储在 `frame_smsplatform` 表中，通过管理后台维护，无需在 YAML 中配置。
- 通道类型由 `SmsTypeEnum` 枚举控制：`SMS(1)`、`QiYeWeiXin(2)`、`OARobot(3)`、`EMAIL(4)`。
- `MessageSenderHolder` 按 `{code}_{tenantId}` 缓存发送器实例，配置变更后自动通过 `updateGmt` 时间戳比较刷新实例。
- OA 机器人支持钉钉（`DING_TALK`）、企业微信（`WE_TALK`）、飞书（`BYTE_TALK`）三种 Webhook 协议。
- 短信通道基于 sms4j 框架，已内置阿里、腾讯、华为、百度、网易、云片、亿息通等 15 家厂商。

---

## 代码模式

### 推荐写法

**统一发送接口**

```java
import com.leatop.cdp.message.business.MessageBusiness;
import com.leatop.cdp.message.dto.SendMsgDTO;
import com.leatop.cdp.message.dto.SendResultDTO;

@Service
public class NotificationService {

    @Autowired
    private MessageBusiness messageBusiness;

    public void sendNotification(String receiver, String content, String platformCode) {
        SendMsgDTO msg = new SendMsgDTO();
        msg.setCode(platformCode);       // 平台编码，对应 frame_smsplatform 表配置
        msg.setReceiver(receiver);        // 接收方
        msg.setMsg(content);              // 消息内容
        msg.setSerialNumber("BIZ-001");   // 业务流水号，用于日志追踪
        Message<List<SendResultDTO>> result = messageBusiness.sendMsg(msg);
    }
}
```

**REST 接口调用**

```
POST /system/smsplatform/sendMsg
Body: SendMsgDTO
Response: Message<List<SendResultDTO>>
```

**新增短信厂商扩展**

1. 实现 `BaseProviderFactory<SmsBlend, SupplierConfig>` 接口
2. 在 `SmsMessageSender` 的 `static` 块中调用 `ProviderFactoryHolder.registerFactory()` 注册
3. 在数据库平台配置的 `settingParam` JSON 中填入对应 `supplier` 标识和厂商参数

**新增通道类型扩展**

1. 在 `SmsTypeEnum` 中新增枚举值
2. 创建新的 `MessageSender` 实现类
3. 在 `MessageSenderHolder.createMessageSender()` 的 switch 分支中添加映射

### 禁止事项

- **禁止绕过 `MessageBusiness` 接口直接调用底层 SDK** -- 必须通过统一入口 `messageBusiness.sendMsg()` 发送，确保日志记录和通道路由
- **禁止在 YAML 中硬编码消息平台密钥** -- 平台配置存储在 `frame_smsplatform` 表中，通过管理后台维护
- **禁止手动管理 `MessageSender` 实例** -- 由 `MessageSenderHolder` 统一管理生命周期和缓存
- **禁止忽略发送结果检查** -- `SendResultDTO` 包含状态码和错误信息，必须处理发送失败场景
- **禁止在高并发场景下同步批量发送** -- OA Webhook 支持异步模式（`senderAsync()`），大量发送时应使用异步方式
- **禁止直接操作 `frame_smsplatform_log` 表写入日志** -- 发送日志由框架自动记录
