# CDP 分库分表 设计手册

> 对应使用手册：[cdp-module-sharding.md](../3-组件使用手册/cdp-module-sharding.md)

## 一、设计目标与背景

CDP 分库分表组件（`leatop-cdp-base-sharding`）在 ShardingSphere-JDBC 5.5.2 基础上解决两个核心问题：

1. **简化配置管理** -- ShardingSphere 原生 YAML 要求逐表声明分片规则，当业务模块众多时配置冗长且易出错。CDP 引入"简化配置层"，通过 `cdp.sharding-extra-config` 批量声明分片表和单表，框架在 YAML 加载阶段自动合并。
2. **多来源配置加载** -- 企业部署场景需要从 classpath、绝对路径、Nacos 配置中心三种途径加载 ShardingSphere YAML，且加载过程中注入 CDP 简化配置。

> 设计决策：选择在 ShardingSphere 的 SPI 扩展点（`ShardingSphereURLLoader`）注入 CDP 逻辑，而非 Spring Bean 后置处理，因为 ShardingSphere 数据源初始化早于 Spring AutoConfiguration 时机。

## 二、整体架构

```
┌──────────────────────────────────────────────────┐
│            spring.datasource.url                 │
│  jdbc:shardingsphere:{classpath|absolutepath|nacos}:...  │
└───────────┬──────────────────────────────────────┘
            │ ShardingSphere SPI
            v
┌───────────────────────────────┐
│  ClassPathURLLoader           │
│  AbsolutePathURLLoader        │  ← CDP 重写的 URLLoader
│  NacosURLLoader               │
└───────────┬───────────────────┘
            │ 调用 CdpShardingUtil.cdpShardingConfig()
            v
┌───────────────────────────────┐
│  CdpShardingUtil              │  ← 简化配置合并引擎
│  ├ addShardingTables()        │
│  └ addSingleTables()          │
└───────────┬───────────────────┘
            │ 读取
            v
┌───────────────────────────────┐
│  CdpShardingProperties        │  ← cdp.sharding-extra-config.*
│  CdpShardingTables            │  ← classpath:cdp-default-tables/*.yml
└───────────────────────────────┘
```

配置合并发生在 YAML 文本层面：先将原始 YAML 按行解析为 `YamlJDBCConfiguration`，再在 `!SHARDING` 或 `!SINGLE` 规则节点下追加表声明，最后做一次反序列化校验确保结果合法。

## 三、核心设计模式

### SPI 替换模式

CDP 在 `org.apache.shardingsphere.infra.url` 包下放置了三个 URLLoader 实现，通过 Java SPI 覆盖 ShardingSphere 原生实现。每个 Loader 的 `load()` 方法在读取 YAML 内容后统一调用 `CdpShardingUtil.cdpShardingConfig(List<String> yamlLines)`，将简化配置合并到 YAML 中。

> 设计决策：使用同包名覆盖而非继承，因为 ShardingSphere URLLoader SPI 按 `getType()` 返回值匹配，CDP 需要完全替代原有实现来注入合并逻辑。

### 表清单外置模式

各业务模块在自身 resources 下提供 `cdp-default-tables/*.yml` 文件，文件格式为 `CdpShardingTables`（仅含 `tables` 列表）。`CdpShardingUtil.listTablesByUrl()` 利用 ShardingSphere 自身的 `ShardingSphereURLLoadEngine` 加载这些文件，支持 classpath 和绝对路径两种前缀。

这使得每个业务模块（系统管理、日志、工作流等）可以自行声明所管辖的表，分库分表配置与业务模块解耦。

### 映射分片算法

`MapShardingAlgorithm` 实现 `StandardShardingAlgorithm<String>` 接口，适用于按租户 ID 等离散值路由的场景。算法通过 `sharding-map` 属性接收 JSON 数组，将分片列值映射为数据源/表的索引后缀。当分片值为空时，回退到 `empty-target` 指定的默认目标。

> 设计决策：不采用 HASH 或 RANGE 算法，因为租户与数据源的映射关系由运维决定，需要显式声明而非计算。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| `CdpShardingAutoConfiguration` | Spring 配置类，启用 `CdpShardingProperties` 属性绑定 |
| `CdpShardingProperties` | 绑定 `cdp.sharding-extra-config`，包含分片表和单表的数据源表达式、表后缀、默认策略表列表及排除列表 |
| `CdpShardingTables` | 实现 `YamlConfiguration`，解析外部表清单 YAML 文件 |
| `CdpShardingUtil` | 核心工具类，负责 YAML 行级合并、表清单加载、已配置表去重 |
| `MapShardingAlgorithm` | 自定义分片算法，基于 JSON 映射表实现精确分片路由 |
| `ClassPathURLLoader` | 覆盖 ShardingSphere classpath 加载器，注入 CDP 合并逻辑 |
| `AbsolutePathURLLoader` | 覆盖 ShardingSphere 绝对路径加载器 |
| `NacosURLLoader` | 新增 Nacos 配置中心加载器，通过 `NacosConfigManager` 获取 YAML |

## 五、扩展机制

1. **自定义表清单**：新建 `classpath:cdp-default-tables/xxx-tables.yml`，在 `cdp.sharding-extra-config.sharding-tables.default-strategy-tables` 中引用即可。
2. **自定义分片算法**：实现 `StandardShardingAlgorithm` 接口，在 ShardingSphere YAML 的 `shardingAlgorithms` 中以 `CLASS_BASED` 类型声明，无需修改 CDP 框架代码。
3. **新增配置来源**：实现 `ShardingSphereURLLoader` SPI 接口并在 `load()` 中调用 `CdpShardingUtil.cdpShardingConfig()`，即可支持新的配置来源（如 Apollo、Consul）。

## 六、模块协作（简要）

- **Flyway 集成**：CDP 在 ShardingSphere YAML 数据源上扩展了 `flyway: true/false` 属性，控制该数据源是否执行 Flyway 迁移，主从部署时仅主库开启。
- **各业务模块**：系统管理、日志、工作流等模块通过 resources 下的表清单文件与分库分表模块协作，无需直接依赖。

## 七、设计权衡与约束（简要）

- **启动类需手动扫描包**：由于 ShardingSphere 数据源初始化先于 Spring AutoConfiguration，`CdpShardingProperties` 必须通过 `scanBasePackages` 提前加载，这增加了接入成本但保证了配置合并时机正确。
- **YAML 文本操作而非对象操作**：早期版本直接操作 YAML 字符串（已标记 `@Deprecated`），当前版本改为先反序列化再按行追加，兼顾了准确性与兼容性。
- **MapShardingAlgorithm 不支持范围查询**：`doSharding(RangeShardingValue)` 直接抛出异常，因为映射式分片在语义上不支持范围路由，使用时需避免对分片列做 BETWEEN 查询。
