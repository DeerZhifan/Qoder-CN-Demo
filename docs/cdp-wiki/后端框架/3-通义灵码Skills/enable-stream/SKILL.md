# 启用 CDP 消息队列组件

## 描述

在已有 CDP 项目中启用消息队列组件（`leatop-cdp-base-stream`），基于 Spring Cloud Stream 提供统一的消息发布与订阅抽象，支持 Kafka、RabbitMQ、RocketMQ 三种中间件，切换中间件只需更换 Maven 依赖，业务代码无需修改。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **消息中间件类型**（`kafka`、`rabbitmq` 或 `rocketmq`，默认 `kafka`）
3. **中间件连接地址**（如 Kafka broker 地址 `172.17.1.115:9092`）
4. **主题名称**（如 `order-topic`）
5. **消费组名称**（如 `order-topic-group`）

---

## 步骤 1：添加 Maven 依赖

> 根据选择的中间件引入对应的 Binder 适配模块。该模块会传递依赖 `stream-common` 抽象层和对应的 Spring Cloud Stream Binder Starter。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加（三选一）：

**Kafka（推荐）：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-stream-kafka</artifactId>
</dependency>
```

**RabbitMQ：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-stream-rabbit</artifactId>
</dependency>
```

**RocketMQ：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-stream-rocket</artifactId>
</dependency>
```

## 步骤 2：添加 YAML 配置

> 配置消费者/生产者绑定和中间件连接信息。绑定名称遵循 `<functionName>-in-<index>`（消费者）和 `<functionName>-out-<index>`（生产者）的命名规范。

在 `application-dev.yaml`（或对应的 profile 文件）中添加：

**Kafka 配置：**

```yaml
spring:
  cloud:
    function:
      definition: {消费者函数名}              # 消费者函数名，多个用分号分隔
    stream:
      bindings:
        {消费者函数名}-in-0:
          binder: kafka
          destination: {主题名称}
          group: {消费组名称}
          consumer:
            concurrency: 3
        {生产者绑定名}-out-0:
          binder: kafka
          destination: {主题名称}
          content-type: application/json
          producer:
            auto-startup: true
      kafka:
        binder:
          brokers:
            - {Kafka地址}
```

**RabbitMQ 配置：**

```yaml
spring:
  rabbitmq:
    host: {RabbitMQ地址}
    port: 5672
    username: guest
    password: guest
  cloud:
    function:
      definition: {消费者函数名}
    stream:
      bindings:
        {消费者函数名}-in-0:
          binder: rabbit
          destination: {主题名称}
          group: {消费组名称}
        {生产者绑定名}-out-0:
          binder: rabbit
          destination: {主题名称}
          content-type: application/json
```

## 步骤 3：编写消费者

> 继承 `AbstractMessageConsumer` 实现消费者。`@Component` 的 Bean 名称必须与 `spring.cloud.function.definition` 中声明的函数名完全一致。`doProcess()` 返回 `false` 会触发消息重试。

```java
import com.leatop.cdp.base.stream.common.AbstractMessageConsumer;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component({实体名}MessageConsumer.FUNCTION_NAME)
public class {实体名}MessageConsumer extends AbstractMessageConsumer<{消息类型}> {

    /**
     * Bean 名称，必须与配置文件中 spring.cloud.function.definition 的函数名一致
     */
    public static final String FUNCTION_NAME = "{消费者函数名}";

    @Override
    protected boolean doProcess({消息类型} message, Map<String, Object> headers) {
        // 业务处理逻辑
        System.out.println("接收到消息：" + message);
        return true;  // 返回 true 表示处理成功，返回 false 触发重试
    }
}
```

## 步骤 4：编写生产者

> 继承 `AbstractMessageProducer` 实现生产者。`getBindingName()` 返回的名称必须与 YAML 中配置的生产者绑定名称一致。消息会自动包装为 `MessageWrapper`（含 messageId、timestamp）。

```java
import com.leatop.cdp.base.stream.common.AbstractMessageProducer;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
public class {实体名}Producer extends AbstractMessageProducer<{消息类型}> {

    @Override
    public String getBindingName() {
        return "{生产者绑定名}-out-0";  // 对应 YAML 中的 binding 名称
    }

    /**
     * 发送消息示例
     */
    public void sendMessage({消息类型} data) {
        this.send(data, Map.of("traceId", UUID.randomUUID().toString()));
    }
}
```

## 步骤 5：验证

启动应用，检查以下内容：

1. 控制台无 Binder 初始化异常
2. 日志中出现 `CdpStreamAutoConfiguration` 初始化信息和中间件连接成功日志
3. 调用生产者 `send()` 方法发送消息，消费者 `doProcess()` 能正确接收并处理
4. 检查消息信封中包含 `messageId` 和 `timestamp`

---

## 完成后提醒

1. 切换中间件只需更换 Maven 依赖（如从 `stream-kafka` 换成 `stream-rabbit`），业务代码无需修改
2. 不要同时引入多个 Binder 适配模块（如同时引入 `stream-kafka` 和 `stream-rabbit`），会导致类路径冲突
3. 消费者 `@Component` 的 Bean 名称必须与 `spring.cloud.function.definition` 中的函数名完全一致
4. `doProcess()` 返回 `false` 或抛出异常会触发重试，超过最大重试次数后进入死信队列
5. 不要直接使用 `StreamBridge` 或具体中间件 SDK，统一通过 `AbstractMessageProducer` 或 `MessageOperations` 发送消息
