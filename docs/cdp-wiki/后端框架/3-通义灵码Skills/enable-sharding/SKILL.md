# 启用 CDP 分库分表组件

## 描述

在已有 CDP 项目中启用分库分表组件（`leatop-cdp-base-sharding`），基于 ShardingSphere-JDBC 5.5.2 实现数据库读写分离和分库分表功能。提供简化配置工具，支持从 classpath、绝对路径或 Nacos 加载 ShardingSphere YAML 配置。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **分库分表场景**（`读写分离`、`分库`、`分表`、`分库分表` 或 `租户隔离`）
3. **数据源信息**（主库/从库的 JDBC 地址、用户名、密码）
4. **配置加载方式**（`classpath`、`absolutepath` 或 `nacos`，默认 `classpath`）
5. **是否启用简化配置**（`cdp.sharding-extra-config`，默认 `true`）
6. **是否启用 Flyway 迁移**（默认 `true`，仅主库）

---

## 步骤 1：添加 Maven 依赖

> 版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-sharding</artifactId>
</dependency>
```

## 步骤 2：配置启动类扫描包

> ShardingSphere 数据源初始化早于 Spring AutoConfiguration，必须通过 `scanBasePackages` 提前加载 `CdpShardingProperties`，否则简化配置不生效。

在启动类的 `@SpringBootApplication` 注解中添加扫描包：

```java
@SpringBootApplication(scanBasePackages = {"com.leatop.cdp.sharding.config"})
public class {应用名}Application {
    public static void main(String[] args) {
        SpringApplication.run({应用名}Application.class, args);
    }
}
```

> 如果启动类已有 `scanBasePackages`，将 `"com.leatop.cdp.sharding.config"` 追加到数组中。

## 步骤 3：配置数据源驱动

> 将数据源驱动切换为 ShardingSphere，并指定配置文件加载路径。

在 `application.yaml` 中配置：

```yaml
spring:
  datasource:
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    url: jdbc:shardingsphere:classpath:{ShardingSphere配置文件名}.yml
```

支持三种加载方式：

| 方式 | URL 格式 |
|------|---------|
| classpath | `jdbc:shardingsphere:classpath:config.yml` |
| 绝对路径 | `jdbc:shardingsphere:absolutepath:/data/config.yml` |
| Nacos | `jdbc:shardingsphere:nacos:config?dataId=config.yml&group=DEFAULT_GROUP` |

## 步骤 4：编写 ShardingSphere YAML 配置文件

> 在 `src/main/resources/` 下创建 ShardingSphere 配置文件，文件名与步骤 3 中 URL 指定的名称一致。

**读写分离配置示例：**

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
    url: jdbc:mysql://{主库地址}:{端口}/{数据库名}?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: {用户名}
    password: "{密码}"
    initialSize: 5
    minIdle: 5
    maxActive: 50
  ds_slave_0:
    flyway: false   # 从库不启用 Flyway
    dataSourceClassName: com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://{从库地址}:{端口}/{数据库名}?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: {用户名}
    password: "{密码}"

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

## 步骤 5：配置简化分库分表（可选）

> 启用 CDP 简化配置后，框架在 YAML 加载阶段自动将表清单合并到 ShardingSphere 规则中，无需逐表声明分片规则。

在 `application.yaml` 中添加：

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
        - {自定义表名}
      exclusions:
        - {排除的表名}
    single-tables:
      actualDataSources: logic_ds_0
      defaultStrategyTables:
        - classpath:cdp-default-tables/cdp-gen-tables.yml
```

框架已内置以下表清单文件：

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

## 步骤 6：配置租户映射分片（可选）

> 使用框架提供的 `MapShardingAlgorithm` 实现按租户 ID 路由到不同数据源。分片值为空时回退到 `empty-target` 指定的默认目标。

在 ShardingSphere YAML 的 `shardingAlgorithms` 中声明：

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

## 步骤 7：验证

启动应用，检查以下内容：

1. 控制台无 ShardingSphere 初始化异常
2. 设置 `logging.level.com.leatop.cdp.sharding: debug`，检查合并后的完整 YAML 是否正确
3. 执行数据库读写操作，确认读写分离或分片路由生效
4. 如果启用 Flyway，确认仅主库执行了迁移脚本
5. 检查 `props.sql-show: true` 输出的 SQL 路由日志

---

## 完成后提醒

1. 启动类必须添加 `scanBasePackages = {"com.leatop.cdp.sharding.config"}`，这是框架硬性要求
2. `flyway: true` 仅设置在主库数据源上，从库必须设为 `false`
3. `MapShardingAlgorithm` 不支持对分片列做 BETWEEN 范围查询，会直接抛出异常
4. 使用 OpenGauss 等 PostgreSQL 协议数据库时，需设置 `props.proxy-frontend-database-protocol-type: openGauss`
5. 不要在 `cdp.sharding-extra-config` 中重复声明已在 ShardingSphere YAML 中手动配置的表
6. 自定义表清单文件放在 `classpath:cdp-default-tables/` 目录下，格式为 `CdpShardingTables` YAML
7. 调试时设置 `logging.level.com.leatop.cdp.sharding: debug` 查看合并后的完整配置
