# 如何使用 CDP 消息发布与订阅组件

## 概述

消息组件（`leatop-cdp-base-stream`）基于 Spring Cloud Stream 4.1 框架，提供统一的消息发布和订阅接口，屏蔽底层消息中间件（Kafka、RabbitMQ、RocketMQ）的差异，实现业务代码与具体中间件的解耦。

## 启用方式

根据使用的消息中间件选择对应的 Maven 依赖（三选一）：

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

> 注意：切换中间件只需更换 Maven 依赖，业务代码无需修改。

## 核心 API

### AbstractMessageProducer\<T\> — 消息生产者抽象类

```java
public abstract class AbstractMessageProducer<T> implements MessageProducer<T> {
    public boolean send(T message);                              // 发送消息
    public boolean send(T message, Map<String, Object> headers); // 发送消息（带自定义头）
    public boolean sendTransactional(T message, String txId);    // 事务消息
    public abstract String getBindingName();                     // 返回输出绑定名称
}
```

### AbstractMessageConsumer\<T\> — 消息消费者抽象类

```java
public abstract class AbstractMessageConsumer<T> extends Consumer<Message<T>> {
    protected abstract boolean doProcess(T message, Map<String, Object> headers); // 业务处理逻辑
    public String getDestination();  // 返回输入绑定名称
}
```

### MessageOperations — 低级发送 API

```java
public interface MessageOperations {
    <T> boolean send(String destination, T message);
    <T> boolean send(String destination, T message, Map<String, Object> headers);
}
```

## 配置项

### Kafka 配置示例

```yaml
spring:
  cloud:
    function:
      # 声明消费者函数名，多个用分号分隔
      definition: personInput;receiveMessage
    stream:
      bindings:
        # 消费者绑定：<函数名>-in-<索引>
        personInput-in-0:
          binder: kafka
          destination: test-topic       # 主题名
          group: test-topic-group       # 消费组
          consumer:
            concurrency: 3              # 并发消费者数量
        # 生产者绑定：<函数名>-out-<索引>
        sendMessage-out-0:
          binder: kafka
          destination: test-topic
          content-type: application/json
          producer:
            auto-startup: true
            poller:
              fixed-delay: 2000         # Supplier 轮询间隔（毫秒）
      kafka:
        binder:
          brokers:
            - 172.17.1.115:9092
```

**绑定命名规范：** `<functionName>-in-<index>`（消费者）/ `<functionName>-out-<index>`（生产者），其中 `functionName` 对应 Java 代码中的 Bean 方法名或组件名。

## 使用示例

### 方式一：继承抽象类（推荐）

**消费者：**

```java
import com.leatop.cdp.base.stream.common.AbstractMessageConsumer;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component(PersonMessageConsumer.FUNCTION_NAME)
public class PersonMessageConsumer extends AbstractMessageConsumer<String> {

    // Bean 名称必须与配置文件中 binding 的函数名一致
    public static final String FUNCTION_NAME = "personInput";

    @Override
    protected boolean doProcess(String message, Map<String, Object> headers) {
        System.out.println("业务逻辑处理：" + message);
        return true;  // 返回 false 触发重试
    }
}
```

**生产者：**

```java
import com.leatop.cdp.base.stream.common.AbstractMessageProducer;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OrderProducer extends AbstractMessageProducer<OrderDTO> {

    @Override
    public String getBindingName() {
        return "sendMessage-out-0";  // 对应配置中的 binding 名称
    }

    public void sendOrder(OrderDTO order) {
        this.send(order, Map.of("traceId", UUID.randomUUID().toString()));
    }
}
```

### 方式二：函数式编程

**消费者：**

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Consumer;

@Configuration
public class MessageConsumerDemo {

    @Bean
    public Consumer<String> receiveMessage() {
        return message -> {
            System.out.println("接收到消息：" + message);
        };
    }
}
```

**生产者：**

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Supplier;

@Configuration
public class MessageProducerDemo {

    @Bean
    public Supplier<String> sendMessage() {
        return () -> "hello >>> " + System.currentTimeMillis();
    }
}
```

> 注意：Supplier 方式会周期性调用函数并将返回值发送到绑定的主题，默认间隔 1 秒，可通过 `poller.fixed-delay` 配置。

## 注意事项

> 注意：消费者 `@Component` 的 Bean 名称必须与 `spring.cloud.function.definition` 中声明的函数名一致，否则无法绑定。

> 注意：`doProcess()` 返回 `false` 会触发消息重试。超过最大重试次数后消息进入死信队列（DLQ）。

> 注意：消息自动包装为 `MessageWrapper`（含 messageId、timestamp、payload、headers），无需手动构造。

> 注意：切换中间件是通过更换 Maven 依赖实现，不是通过 Spring Profile 切换。
