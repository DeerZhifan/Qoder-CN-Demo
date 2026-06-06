---
trigger: when_referenced
knowledge_source:
  - cdp-design-stream
  - cdp-module-stream
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-stream-kafka`、`leatop-cdp-base-stream-rabbit` 或 `leatop-cdp-base-stream-rocket` 依赖
- 使用 `AbstractMessageProducer`、`AbstractMessageConsumer` 抽象类
- 使用 `MessageOperations` 接口
- 配置 `spring.cloud.stream` 或 `spring.cloud.function.definition`

---

## 前置依赖

1. Maven 依赖（三选一，按中间件选型）：

```xml
<!-- Kafka（推荐） -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-stream-kafka</artifactId>
</dependency>

<!-- RabbitMQ -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-stream-rabbit</artifactId>
</dependency>

<!-- RocketMQ -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-stream-rocket</artifactId>
</dependency>
```

2. 在 `application.yaml` 或对应 profile 中配置 `spring.cloud.stream` 绑定信息和中间件连接地址。

3. 消费者 Bean 名称必须在 `spring.cloud.function.definition` 中声明。

---

## 配置要点

### Kafka 配置示例

```yaml
spring:
  cloud:
    function:
      definition: personInput;receiveMessage    # 消费者函数名，分号分隔
    stream:
      bindings:
        personInput-in-0:                       # 消费者绑定：<函数名>-in-<索引>
          binder: kafka
          destination: test-topic
          group: test-topic-group
          consumer:
            concurrency: 3
        sendMessage-out-0:                      # 生产者绑定：<函数名>-out-<索引>
          binder: kafka
          destination: test-topic
          content-type: application/json
      kafka:
        binder:
          brokers:
            - 172.17.1.115:9092
```

### 绑定命名规范

- 消费者：`<functionName>-in-<index>`，`functionName` 与 `@Component` Bean 名称一致
- 生产者：`<functionName>-out-<index>`，`functionName` 与 `getBindingName()` 返回值前缀一致

---

## 代码模式

### 推荐写法

**消费者：继承 AbstractMessageConsumer（推荐）**

```java
import com.leatop.cdp.base.stream.common.AbstractMessageConsumer;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component(PersonMessageConsumer.FUNCTION_NAME)
public class PersonMessageConsumer extends AbstractMessageConsumer<String> {

    public static final String FUNCTION_NAME = "personInput";

    @Override
    protected boolean doProcess(String message, Map<String, Object> headers) {
        // 业务逻辑
        return true;  // 返回 false 触发重试
    }
}
```

**生产者：继承 AbstractMessageProducer（推荐）**

```java
import com.leatop.cdp.base.stream.common.AbstractMessageProducer;
import org.springframework.stereotype.Component;

@Component
public class OrderProducer extends AbstractMessageProducer<OrderDTO> {

    @Override
    public String getBindingName() {
        return "sendMessage-out-0";
    }

    public void sendOrder(OrderDTO order) {
        this.send(order, Map.of("traceId", UUID.randomUUID().toString()));
    }
}
```

**函数式消费者（简单场景可用）**

```java
@Configuration
public class MessageConsumerDemo {

    @Bean
    public Consumer<String> receiveMessage() {
        return message -> System.out.println("接收到：" + message);
    }
}
```

### 禁止事项

- **禁止直接依赖具体中间件 SDK**（如 `kafka-clients`、`amqp-client`） -- 必须通过 CDP Stream 统一抽象层编程，保持中间件无关性
- **禁止通过 Spring Profile 切换中间件** -- 切换中间件是通过替换 Maven 依赖实现，不是通过 Profile；运行时同时加载多个 Binder 会导致类路径冲突
- **禁止同时引入多个 Binder 适配模块** -- 如同时引入 `stream-kafka` 和 `stream-rabbit`，会引起 Binder 冲突
- **禁止消费者 Bean 名称与 `spring.cloud.function.definition` 不一致** -- `@Component` 的 value 必须与配置中声明的函数名完全匹配，否则无法绑定
- **禁止手动构造 `MessageWrapper`** -- 框架自动包装消息信封（含 messageId、timestamp），无需手动创建
- **禁止在消费者 `doProcess()` 中吞掉异常** -- 返回 `false` 或抛出异常才能触发重试机制，静默捕获会丢失失败信号
- **禁止绕过 `MessageOperations` 直接使用 `StreamBridge`** -- 统一通过 `AbstractMessageProducer` 或 `MessageOperations` 发送，确保消息信封格式一致
