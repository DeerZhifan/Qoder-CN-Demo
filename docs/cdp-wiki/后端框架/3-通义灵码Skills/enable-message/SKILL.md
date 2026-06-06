# 启用 CDP 消息通知

## 描述

在已有 CDP 项目中启用消息通知组件（`leatop-cdp-business-message`），提供统一的消息发送接口，支持短信（SMS）、邮件（EMAIL）、OA 机器人 Webhook（钉钉、企业微信、飞书）等多种通道。消息平台配置通过管理后台维护，业务代码只需调用统一接口即可完成多通道消息发送。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`boot` 单体模式 或 `cloud` 微服务模式，默认 `boot`）
3. **需要的通道类型**（短信、邮件、OA 机器人 Webhook，或全部，默认全部由管理后台配置）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-business-message-boot-starter` 提供消息发送、日志记录的完整功能。微服务模式使用 `cloud-starter`。版本号由父 POM 的 BOM 管理，不需要手动指定。该组件依赖系统管理功能，需同时引入 `leatop-cdp-business-system-boot-starter`。

在 `pom.xml` 的 `<dependencies>` 中添加：

**单体模式：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-message-boot-starter</artifactId>
</dependency>
```

**微服务模式：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-message-cloud-starter</artifactId>
</dependency>
```

确认 `leatop-cdp-business-system-boot-starter`（或 `cloud-starter`）已引入。

## 步骤 2：确认启动类配置

> 消息组件不需要额外的 `@Enable` 注解，但需确保主启动类的 `scanBasePackages` 包含 `com.leatop.cdp`，以便扫描到消息模块的自动配置。

确认主启动类配置：

```java
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@MapperScan("com.leatop.cdp.**.dao")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 步骤 3：确认数据库表

> 消息组件依赖两张数据库表，通过 Flyway 自动创建或手动执行 SQL。

确认以下表存在：

| 表名 | 说明 |
|------|------|
| `frame_smsplatform` | 消息平台配置表（存储各通道的 AccessKey、密钥、Webhook 地址等） |
| `frame_smsplatform_log` | 消息发送日志表（自动记录每次发送的状态和结果） |

## 步骤 4：配置消息平台

> 消息平台配置通过管理后台界面维护，存储在数据库中，无需在 YAML 文件中配置。运营人员可动态增删通道、修改密钥，无需重启服务。

在管理界面【短信平台配置】中添加消息通道：

- **短信通道**：配置厂商（阿里、腾讯、华为等）的 AccessKey、密钥、签名参数
- **邮件通道**：配置 SMTP 服务器地址、用户名、密码
- **OA 机器人**：配置钉钉/企业微信/飞书的 Webhook 地址和密钥

## 步骤 5：编写发送示例代码

> 业务代码只需注入 `MessageBusiness` 接口，调用 `sendMsg()` 方法即可，框架根据平台编码自动路由到对应通道。

```java
import com.leatop.cdp.message.business.MessageBusiness;
import com.leatop.cdp.message.dto.SendMsgDTO;
import com.leatop.cdp.message.dto.SendResultDTO;
import com.leatop.cdp.common.data.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private MessageBusiness messageBusiness;

    /**
     * 发送消息通知
     * @param platformCode 平台编码，对应 frame_smsplatform 表中的 code 字段
     * @param receiver 接收方（手机号/邮箱/群组标识）
     * @param content 消息内容
     */
    public void sendNotification(String platformCode, String receiver, String content) {
        SendMsgDTO msg = new SendMsgDTO();
        msg.setCode(platformCode);
        msg.setReceiver(receiver);
        msg.setMsg(content);
        msg.setSerialNumber("BIZ-" + System.currentTimeMillis());
        Message<List<SendResultDTO>> result = messageBusiness.sendMsg(msg);
        // 检查发送结果
        if (result != null && result.getData() != null) {
            for (SendResultDTO sendResult : result.getData()) {
                // 处理每个通道的发送结果
            }
        }
    }
}
```

## 步骤 6：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 确认 `frame_smsplatform` 表中已配置至少一个通道
3. 调用发送方法后，查询 `frame_smsplatform_log` 表确认发送日志已写入
4. 检查发送结果的状态码和错误信息

---

## 完成后提醒

1. `MessageBusiness` 接口使用 `@FeignClient` 标注，单体部署走本地调用，微服务部署走远程调用，业务代码无需修改
2. 消息平台配置存储在数据库中，管理员修改配置后下一次发送自动加载新配置，无需手动清缓存
3. 不要绕过 `MessageBusiness` 接口直接调用底层 SDK，否则发送日志不会记录
4. 新增短信厂商需实现 `BaseProviderFactory` 接口并在 `SmsMessageSender` 的 static 块中注册
5. 新增通道类型需在 `SmsTypeEnum` 新增枚举值、实现 `MessageSender` 接口、在 `MessageSenderHolder` 添加映射
6. `MessageSenderHolder` 按 `{code}_{tenantId}` 缓存发送器实例，支持多租户场景
