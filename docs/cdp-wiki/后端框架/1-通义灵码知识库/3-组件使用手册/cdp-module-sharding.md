# 如何使用 CDP 分库分表组件

## 概述

分库分表组件（`leatop-cdp-base-sharding`）基于 ShardingSphere-JDBC 5.5.2 实现数据库读写分离和分库分表功能。提供简化配置工具，支持从 classpath、绝对路径或 Nacos 加载 ShardingSphere YAML 配置。

## 启用方式

**1. 添加 Maven 依赖：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-sharding</artifactId>
</dependency>
```

**2. 启动类配置扫描包：**

```java
@SpringBootApplication(scanBasePackages = {"com.leatop.cdp.sharding.config"})
public class ShardingDemoMain {
    public static void main(String[] args) {
        SpringApplication.run(ShardingDemoMain.class, args);
    }
}
```

**3. 配置数据源驱动和 ShardingSphere 配置文件来源：**

```yaml
spring:
  datasource:
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    url: jdbc:shardingsphere:classpath:mysql-readwrite-splitting.yml
```

`spring.datasource.url` 支持三种配置文件加载方式：

| 方式 | 格式 |
|------|------|
| classpath | `jdbc:shardingsphere:classpath:config.yml` |
| 绝对路径 | `jdbc:shardingsphere:absolutepath:/data/config.yml` |
| Nacos | `jdbc:shardingsphere:nacos:config?dataId=config.yml&group=DEFAULT_GROUP` |

## 配置项

### 简化分库分表配置（可选）

```yaml
cdp:
  sharding-extra-config:
    enabled: true
    # 分片表配置
    sharding-tables:
      actualDataSources: logic_ds_${0..1}      # 数据源表达式
      actualTableSuffix: _${0..2}              # 实际表后缀
      default-strategy-tables:                 # 使用默认策略的表
        - classpath:cdp-default-tables/cdp-log-tables.yml
        - classpath:cdp-default-tables/cdp-system-tables.yml
        - my_custom_table
      exclusions:                              # 排除的表
        - some_excluded_table
    # 单表配置（不分片的表）
    single-tables:
      actualDataSources: logic_ds_0
      defaultStrategyTables:
        - classpath:cdp-default-tables/cdp-gen-tables.yml
```

### 框架自带表配置文件

| 模块 | 配置文件路径 |
|------|-------------|
| 系统管理 | `classpath:cdp-default-tables/cdp-system-tables.yml` |
| 日志管理 | `classpath:cdp-default-tables/cdp-log-tables.yml` |
| 任务调度 | `classpath:cdp-default-tables/cdp-xxljob-tables.yml` |
| 导入导出 | `classpath:cdp-default-tables/cdp-export-tables.yml` |
| 代码生成 | `classpath:cdp-default-tables/cdp-gen-tables.yml` |
| 附件管理 | `classpath:cdp-default-tables/cdp-attachment-tables.yml` |
| 工作流 | `classpath:cdp-default-tables/cdp-workflow-tables.yml` |
| 全文检索 | `classpath:cdp-default-tables/cdp-fulltext-tables.yml` |
| 消息管理 | `classpath:cdp-default-tables/cdp-message-tables.yml` |
| 报表 | `classpath:cdp-default-tables/cdp-report-tables.yml` |

## 使用示例

### 读写分离配置（mysql-readwrite-splitting.yml）

```yaml
mode:
  type: Standalone
  repository:
    type: JDBC

dataSources:
  ds_master:
    flyway: true    # 主库启用 Flyway
    dataSourceClassName: com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://172.17.1.80:13306/mydb?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: root
    password: "******"
    initialSize: 5
    minIdle: 5
    maxActive: 50
  ds_slave_0:
    flyway: false   # 从库不启用 Flyway
    dataSourceClassName: com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://172.17.1.80:23306/mydb?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: root
    password: "******"

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

props:
  sql-show: true
```

### 租户 ID 映射分库

框架提供 `MapShardingAlgorithm` 自定义分片算法，根据 `tenant_id` 将数据路由到不同数据源：

```yaml
shardingAlgorithms:
  database_map:
    type: CLASS_BASED
    props:
      strategy: STANDARD
      algorithmClassName: com.leatop.cdp.sharding.algorithm.MapShardingAlgorithm
      empty-target: logic_ds_0    # tenant_id 为空时的默认数据源
      sharding-map:               # 租户与数据源的映射关系
        - '{"value": "tenant_001", "name": "租户A", "index": 0}'
        - '{"value": "tenant_002", "name": "租户B", "index": 1}'
```

## 注意事项

> 注意：必须在启动类添加 `scanBasePackages = {"com.leatop.cdp.sharding.config"}`，否则简化配置不生效。

> 注意：开启 `cdp.sharding-extra-config.enabled: true` 后，框架会自动将简化配置合并到 ShardingSphere YAML 中。

> 注意：设置 `logging.level.com.leatop.cdp.sharding: debug` 可查看自动配置后的完整 YAML，便于调试。

> 注意：使用 OpenGauss 等 PostgreSQL 协议数据库时，需设置 `props.proxy-frontend-database-protocol-type: openGauss`。

> 注意：ShardingSphere YAML 中数据源的 `flyway: true` 属性为 CDP 扩展，控制该数据源是否执行 Flyway 迁移，主从部署时仅主库设为 `true`。
