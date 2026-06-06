# CDP 分布式事务 设计手册

> 对应使用手册：无对应使用手册，从源码直接分析

## 一、设计目标与背景

分布式事务模块（`leatop-cdp-base-transaction`）是 CDP 框架中基于 Seata 的分布式事务示例工程，演示了在微服务架构下如何通过 Seata AT 模式实现跨服务的事务一致性。

该模块并非一个可复用的框架组件，而是一个完整的**参考实现**，包含三个独立微服务（订单、账户、库存）和一个公共模块，展示了典型的电商下单场景中分布式事务的完整流程。

> 设计决策：选择 Seata AT（自动补偿）模式而非 TCC 或 Saga，因为 AT 模式对业务代码侵入最小，仅需 `@GlobalTransactional` 注解即可开启全局事务，适合 CDP 目标用户群体。

## 二、整体架构

```
                    ┌─────────────────┐
                    │  OrderTmApp     │  ← TM（事务管理者）
                    │  @GlobalTransactional
                    └───────┬─────────┘
                            │ Feign 调用
              ┌─────────────┼─────────────┐
              v             v             v
    ┌─────────────┐  ┌───────────┐  ┌──────────────┐
    │ StorageRmApp│  │AccountRmApp│  │  本地订单     │
    │ RM（资源管理）│  │ RM（资源管理）│  │  insert      │
    └─────────────┘  └───────────┘  └──────────────┘
              │             │
              v             v
        ┌──────────┐  ┌──────────┐
        │ storage  │  │ account  │
        │ 库存表    │  │ 账户表    │
        └──────────┘  └──────────┘

    公共模块：leatop-cdp-base-transaction-common
    （CommonResult、StorageDTO 等共享类型）
```

### 子模块划分

| 子模块 | 角色 | 说明 |
|--------|------|------|
| `transaction-order` | TM | 事务发起方，编排下单流程 |
| `transaction-account` | RM | 账户服务，扣减余额 |
| `transaction-storage` | RM | 库存服务，扣减库存 |
| `transaction-common` | 公共 | 共享 DTO 和响应封装 |

## 三、关键类说明

| 类名 | 职责 |
|------|------|
| `OrderTmApp` | 订单服务启动类，启用 `@EnableDiscoveryClient` 和 `@EnableFeignClients` |
| `OrderBusinessServiceImpl` | 下单业务核心类，标注 `@GlobalTransactional` 开启全局事务，依次调用库存扣减、余额扣减、订单创建 |
| `AccountRmApp` | 账户服务启动类，注册为 Nacos 服务 |
| `AccountBusinessServiceImpl` | 实现 `AccountFeignService` 接口，执行余额扣减 |
| `StorageRmApp` | 库存服务启动类 |
| `StorageBusinessServiceImpl` | 实现 `StorageFeignService` 接口，执行库存扣减 |
| `AccountFeignService` | 账户服务 Feign 客户端接口 |
| `StorageFeignService` | 库存服务 Feign 客户端接口 |
| `CommonResult` | 统一响应封装 |
| `Order` / `Account` / `Storage` | MyBatis-Plus ActiveRecord 实体 |

## 四、扩展机制

1. **替换事务模式**：将 `@GlobalTransactional` 替换为 Seata TCC 注解（`@TwoPhaseBusinessAction`），可切换到 TCC 模式，需要手动实现 prepare/commit/rollback 三个阶段。
2. **接入实际业务**：将示例中的 Order/Account/Storage 替换为真实业务实体，保持 Feign 接口定义和 `@GlobalTransactional` 注解模式即可。
3. **Seata 配置**：通过 Nacos 管理 Seata Server 地址和事务组映射，各 RM 服务在 `application.yml` 中配置 `seata.tx-service-group`。

> 设计决策：该模块作为示例工程未集成到 CDP BOM 中，避免对不需要分布式事务的项目引入 Seata 依赖。业务项目需要时参照此示例自行集成。
