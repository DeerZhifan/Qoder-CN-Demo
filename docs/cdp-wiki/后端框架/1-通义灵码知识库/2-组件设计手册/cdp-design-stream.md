# CDP 消息队列统一抽象层 设计手册

> 对应使用手册：[cdp-module-stream.md](../3-组件使用手册/cdp-module-stream.md)

## 一、设计目标与背景

企业级系统通常需要消息中间件来实现异步解耦、事件驱动和流量削峰。然而不同项目组、不同客户环境对中间件的选型各有偏好——有的用 Kafka，有的用 RabbitMQ，还有的用 RocketMQ。如果业务代码直接依赖某一种中间件的 SDK，切换成本极高，同时也造成框架层的碎片化。

`leatop-cdp-base-stream` 模块的设计目标是：

1. **一次编写，多处运行**：业务代码面向统一抽象接口编程，不感知底层中间件差异。
2. **零代码切换**：切换中间件只需替换 Maven 依赖（如从 `stream-kafka` 换成 `stream-rabbit`），无需修改任何 Java 代码。
3. **降低学习成本**：对 Spring Cloud Stream 的函数式编程模型做二次封装，提供更贴近业务直觉的生产者/消费者抽象。
4. **保留扩展口**：高级用户仍可直接使用 Spring Cloud Stream 原生的 `Supplier`/`Consumer` 函数式 Bean。

## 二、整体架构

模块采用"公共抽象 + Binder 适配"的分层结构，Maven 工程划分为五个子模块：

```
leatop-cdp-base-stream/
  leatop-cdp-base-stream-common    # 统一抽象层：接口、抽象类、自动配置、消息包装
  leatop-cdp-base-stream-kafka     # Kafka Binder 适配（传递依赖 spring-cloud-starter-stream-kafka）
  leatop-cdp-base-stream-rabbit    # RabbitMQ Binder 适配（传递依赖 spring-cloud-starter-stream-rabbit）
  leatop-cdp-base-stream-rocket    # RocketMQ Binder 适配（传递依赖 spring-cloud-starter-stream-rocketmq）
  leatop-cdp-base-stream-example   # 参考示例
```

> 设计决策：common 模块仅依赖 `spring-cloud-stream` 核心包，不引入任何具体 Binder。三个适配模块各自引入对应的 Spring Cloud Stream Binder Starter 并传递依赖 common。这样业务工程只需声明一个适配模块依赖，即可同时获得抽象层和具体 Binder 实现。

依赖关系如下：

```
业务工程 ──depends──> stream-kafka ──depends──> stream-common
                                   ──depends──> spring-cloud-starter-stream-kafka
```

## 三、核心设计模式

### 3.1 Bridge 模式：隔离抽象与实现

模块的核心思想是 Bridge（桥接）模式。抽象维度是 CDP 定义的 `MessageProducer`/`MessageConsumer`/`MessageOperations` 接口体系；实现维度是 Spring Cloud Stream 的 Binder SPI（Kafka Binder、Rabbit Binder、RocketMQ Binder）。两个维度独立变化：

- 新增业务生产者/消费者 —— 只需继承抽象类，不触碰 Binder 层。
- 新增中间件支持 —— 只需新建一个适配子模块引入对应 Binder Starter，不触碰 common 层。

> 设计决策：切换中间件的方式选择了"替换 Maven 依赖"而非"Spring Profile 切换"。这是因为不同 Binder 的传递依赖差异很大（Kafka Client vs. AMQP Client vs. RocketMQ Client），运行时同时加载多个 Binder 会增加类路径冲突风险和启动时间。编译期隔离更干净。

### 3.2 Template Method 模式：消费者骨架

`AbstractMessageConsumer` 实现了经典的模板方法模式。它同时实现 `MessageConsumer<T>`（CDP 内部接口）和 `java.util.function.Consumer<Message<T>>`（Spring Cloud Stream 函数式契约），在 `accept()` 方法中完成消息拆包（提取 payload 和 headers），然后调用 `process()`。`process()` 内部包含 try-catch 骨架，成功时返回 `doProcess()` 的结果，失败时委托给 `handleProcessFailure()` 做重试计数判断。子类只需实现 `doProcess()` 即可。

### 3.3 Delegation 模式：生产者与 MessageOperations

`AbstractMessageProducer` 不直接持有 `StreamBridge`，而是委托给 `MessageOperations` 接口。`DefaultMessageOperations` 是该接口的默认实现，负责：

1. 将业务载荷包装为 `MessageWrapper`（附加 messageId、timestamp、headers）。
2. 通过 `MessageBuilder` 构造 Spring Messaging 的 `Message` 对象。
3. 调用 `StreamBridge.send()` 发送到指定的 binding。

这一层间接引用使得生产者逻辑可以在不依赖 `StreamBridge` 的环境下进行单元测试（Mock `MessageOperations` 即可）。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| `MessageProducer<T>` | 生产者顶层接口，定义 `send()`、`sendTransactional()`、`getBindingName()` |
| `AbstractMessageProducer<T>` | 生产者抽象实现，注入 `MessageOperations`，子类只需提供 binding 名称 |
| `MessageConsumer<T>` | 消费者顶层接口，定义 `process()` 和可选的 `onBatchMessage()` |
| `AbstractMessageConsumer<T>` | 消费者抽象实现，桥接 `Consumer<Message<T>>` 与 CDP 消费接口，含异常处理骨架 |
| `MessageOperations` | 低级发送 API，按 destination 发送任意类型消息 |
| `DefaultMessageOperations` | `MessageOperations` 默认实现，封装 `StreamBridge` + `MessageWrapper` 构建逻辑 |
| `MessageWrapper<T>` | 消息信封，携带 messageId（UUID）、timestamp、payload、headers、retryCount |
| `MessageQueueProperties` | 配置属性类，前缀 `spring.cloud.stream.message`，含 Producer/Consumer/Backoff 子配置 |
| `CdpStreamAutoConfiguration` | Spring Boot 自动配置入口，注册 `MessageOperations` Bean 和分区键提取策略 |

## 五、扩展机制

### 5.1 自定义 MessageOperations

`CdpStreamAutoConfiguration` 中 `MessageOperations` Bean 标注了 `@ConditionalOnMissingBean`。业务方可以提供自定义实现（如添加链路追踪 header、消息加密等），框架会自动让位。

### 5.2 分区键策略

自动配置注册了名为 `messageIdExtractor` 的 `PartitionKeyExtractorStrategy`，默认以消息头中的 `messageId` 作为分区键。业务方可替换该 Bean 以实现按业务主键分区。

### 5.3 消费者失败处理

`AbstractMessageConsumer.handleProcessFailure()` 是 protected 方法，子类可覆盖以实现自定义的失败策略（如写入本地失败表、发送告警等）。默认实现会检查 `deliveryAttempt` 和 `maxAttempts` 头，超过最大重试次数时记录警告日志。

### 5.4 批量消费

`MessageConsumer` 接口预留了 `onBatchMessage()` 默认方法，目前抛出 `UnsupportedOperationException`。当 Binder 支持批量投递时（如 Kafka Batch Listener），子类可覆盖该方法启用批量消费路径。

## 六、模块协作

### 与 Spring Cloud Stream 的关系

CDP Stream 模块是 Spring Cloud Stream 之上的薄封装层，并非替代品。底层消息路由、Binder 生命周期、消费组管理、重试与死信队列等核心能力全部由 Spring Cloud Stream 提供。CDP 的价值在于：

- 提供面向对象的抽象类（vs. 纯函数式 Bean），更适合复杂业务场景。
- 统一消息信封格式（`MessageWrapper`），保证 messageId 和 timestamp 的一致性。
- 通过 Maven 子模块隔离不同 Binder 的依赖，避免类路径污染。

### 与 CDP 其他模块的协作

- **leatop-cdp-common-filter**：`MDCRequestFilter` 设置的 traceId 可通过生产者 headers 传递到消息消费端，实现跨消息链路追踪。
- **leatop-cdp-base-lock**：在消费者的 `doProcess()` 中可配合分布式锁实现幂等消费。
- **leatop-cdp-business-log**：消息发送/消费的关键节点通过 SLF4J 输出日志，可被日志模块统一采集。

## 七、设计权衡与约束

> 设计决策：选择 Maven 依赖替换而非运行时 Profile 切换。优点是类路径干净、无多余依赖；缺点是无法在同一个 JVM 中同时连接两种中间件（极少数场景需要）。如有双 MQ 需求，可通过 Spring Cloud Stream 的多 Binder 配置原生支持，但需自行管理依赖。

> 设计决策：`AbstractMessageConsumer` 同时实现 `Consumer<Message<T>>` 接口，使其可直接注册为 Spring Cloud Stream 的函数式消费者 Bean。这避免了额外的适配器类，但要求子类的 `@Component` 名称必须与 `spring.cloud.function.definition` 中的函数名一致。

> 设计决策：`MessageWrapper` 在发送侧自动生成 UUID 作为 messageId，而非依赖中间件提供的消息 ID。这保证了跨中间件的 ID 格式一致性，但在 Kafka 场景下会与 Kafka 自身的 offset/key 机制形成冗余。考虑到框架需要中间件无关的统一标识，接受了这一冗余。

> 设计决策：`MessageConsumerRegister` 类中的动态 Bean 注册逻辑目前被注释掉。这表明团队曾探索过"消费者自动注册到 Spring Cloud Function"的方案，但由于 `BindableFunctionProxyFactory` 的内部 API 稳定性不足而暂时搁置，转为要求开发者显式声明 `@Component` + `spring.cloud.function.definition`。这是一个务实的取舍——牺牲少量配置便利性，换取对 Spring Cloud Stream 内部 API 变更的免疫力。

> 设计决策：`MessageQueueProperties` 中的 `Consumer.backoff` 配置（initialInterval、multiplier、maxInterval）提供了指数退避重试的参数化能力，但实际的重试执行依赖 Spring Cloud Stream 的 RetryTemplate 配置。CDP 属性类起到"集中声明默认值"的作用，最终需要在 YAML 中桥接到 Spring Cloud Stream 的对应配置项。
