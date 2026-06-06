# 如何使用 CDP 消息通知组件

## 概述

消息通知组件（`leatop-cdp-business-message`）提供统一的消息发送接口，支持短信、邮件、企业微信、钉钉等多种通道。支持百度、阿里、华为、腾讯等主流云短信厂商，提供发送日志记录和失败重试机制。底层基于 sms4j 框架。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-message-boot-starter</artifactId>
</dependency>
```

> 注意：该组件依赖系统管理功能，需先引入 `leatop-cdp-business-system-boot-starter`。

## 统一发送接口

```java
import com.leatop.cdp.message.business.MessageBusiness;
import com.leatop.cdp.message.dto.SendMsgDTO;
import com.leatop.cdp.message.dto.SendResultDTO;

@Autowired
private MessageBusiness messageBusiness;

// 发送消息
SendMsgDTO msg = new SendMsgDTO();
// 设置消息参数...
Message<List<SendResultDTO>> result = messageBusiness.sendMsg(msg);
```

REST 接口：

```
POST /system/smsplatform/sendMsg
Body: SendMsgDTO
Response: Message<List<SendResultDTO>>
```

## 相关数据库表

| 表名 | 说明 |
|------|------|
| `frame_smsplatform` | 消息平台配置表 |
| `frame_smsplatform_log` | 消息发送日志表 |

## 管理功能

- **短信平台配置**：在管理界面配置各厂商的 AccessKey、密钥、签名等参数
- **发送日志**：查看消息发送状态、时间、接收方和结果
- **失败重试**：对发送失败的消息可手动触发重试

## 注意事项

> 注意：`MessageBusiness` 接口使用 `@FeignClient` 标注，单体部署走本地调用，微服务部署走远程调用。

> 注意：消息平台配置通过管理界面维护，存储在 `frame_smsplatform` 表中。

> 注意：发送日志自动记录，包括发送状态、错误信息，便于排查问题。
