---
trigger: when_referenced
knowledge_source:
  - cdp-design-sharding
  - cdp-module-sharding
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-sharding` 依赖
- 使用 `ShardingSphereDriver` 数据源驱动
- 配置 `cdp.sharding-extra-config` 简化分库分表规则
- 使用 `MapShardingAlgorithm` 自定义分片算法
- 编写 ShardingSphere YAML 配置文件

---

## 前置依赖

1. Maven 依赖：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-sharding</artifactId>
</dependency>
```

2. 启动类必须添加包扫描：

```java
@SpringBootApplication(scanBasePackages = {"com.leatop.cdp.sharding.config"})
```

3. 数据源驱动配置为 ShardingSphere：

```yaml
spring:
  datasource:
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    url: jdbc:shardingsphere:classpath:mysql-readwrite-splitting.yml
```

---

## 配置要点

### ShardingSphere YAML 加载方式

| 方式 | URL 格式 |
|------|---------|
| classpath | `jdbc:shardingsphere:classpath:config.yml` |
| 绝对路径 | `jdbc:shardingsphere:absolutepath:/data/config.yml` |
| Nacos | `jdbc:shardingsphere:nacos:config?dataId=config.yml&group=DEFAULT_GROUP` |

### 简化配置（cdp.sharding-extra-config）

```yaml
cdp:
  sharding-extra-config:
    enabled: true
    sharding-tables:
      actualDataSources: logic_ds_${0..1}
      actualTableSuffix: _${0..2}
      default-strategy-tables:
        - classpath:cdp-default-tables/cdp-log-tables.yml
        - classpath:cdp-default-tables/cdp-system-tables.yml
        - my_custom_table
      exclusions:
        - some_excluded_table
    single-tables:
      actualDataSources: logic_ds_0
      defaultStrategyTables:
        - classpath:cdp-default-tables/cdp-gen-tables.yml
```

### Flyway 集成

ShardingSphere YAML 中数据源的 `flyway: true` 属性为 CDP 扩展，控制该数据源是否执行 Flyway 迁移。主从部署时仅主库设为 `true`：

```yaml
dataSources:
  ds_master:
    flyway: true
  ds_slave_0:
    flyway: false
```

### MapShardingAlgorithm（租户映射分片）

```yaml
shardingAlgorithms:
  database_map:
    type: CLASS_BASED
    props:
      strategy: STANDARD
      algorithmClassName: com.leatop.cdp.sharding.algorithm.MapShardingAlgorithm
      empty-target: logic_ds_0
      sharding-map:
        - '{"value": "tenant_001", "name": "租户A", "index": 0}'
        - '{"value": "tenant_002", "name": "租户B", "index": 1}'
```

### 调试方式

设置 `logging.level.com.leatop.cdp.sharding: debug` 可查看自动配置合并后的完整 YAML。

---

## 代码模式

### 推荐写法

**自定义表清单文件（cdp-default-tables/*.yml）：**

在业务模块 resources 下创建 `cdp-default-tables/xxx-tables.yml`，声明该模块管辖的表：

<!-- TODO: 补充代码示例 -->

**自定义分片算法（扩展 StandardShardingAlgorithm）：**

实现 `StandardShardingAlgorithm` 接口，在 ShardingSphere YAML 的 `shardingAlgorithms` 中以 `CLASS_BASED` 类型声明，无需修改 CDP 框架代码。

<!-- TODO: 补充代码示例 -->

**读写分离配置：**

```yaml
rules:
  - !READWRITE_SPLITTING
    dataSourceGroups:
      logic_ds:
        writeDataSourceName: ds_master
        readDataSourceNames:
          - ds_slave_0
        loadBalancerName: round_robin
    loadBalancers:
      round_robin:
        type: ROUND_ROBIN

  - !SINGLE
    tables:
      - logic_ds.*
    defaultDataSource: logic_ds
```

### 禁止事项

- **禁止省略启动类 scanBasePackages** -- 必须添加 `scanBasePackages = {"com.leatop.cdp.sharding.config"}`，否则 `CdpShardingProperties` 无法在 ShardingSphere 数据源初始化前加载，简化配置不生效
- **禁止对 MapShardingAlgorithm 的分片列执行 BETWEEN 范围查询** -- 映射式分片在语义上不支持范围路由，`doSharding(RangeShardingValue)` 会直接抛出异常
- **禁止手动修改框架生成的合并 YAML** -- 简化配置在 YAML 加载阶段自动合并，手动修改 `!SHARDING` 或 `!SINGLE` 规则节点会与自动合并冲突
- **禁止从库启用 Flyway 迁移** -- `flyway: true` 仅设置在主库数据源上，从库设为 `false`，否则主从同时执行 DDL 会导致冲突
- **禁止绕过 ShardingSphere 直接创建数据源** -- 所有数据库操作必须走 ShardingSphere 代理的数据源，直接创建独立连接会绕过分片路由
- **禁止在 `cdp.sharding-extra-config` 中声明已在 ShardingSphere YAML 中手动配置的表** -- `CdpShardingUtil` 会自动去重，但重复声明可能引起配置歧义
