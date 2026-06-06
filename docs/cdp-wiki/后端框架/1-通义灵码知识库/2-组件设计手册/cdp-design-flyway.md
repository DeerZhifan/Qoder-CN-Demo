# CDP 数据库版本管理 设计手册

> 对应使用手册：[cdp-module-flyway.md](../3-组件使用手册/cdp-module-flyway.md)

## 一、设计目标与背景

CDP 框架需要支持 MySQL、达梦（DM）、OpenGauss、GaussDB、金仓（Kingbase）、GBase、PolarDB 等多种数据库。Flyway 社区版仅原生支持 MySQL 和 PostgreSQL，对国产信创数据库缺少适配。

设计目标：

1. **信创数据库适配** -- 通过 Flyway Plugin SPI 扩展，使 Flyway 能够识别和处理达梦、OpenGauss、金仓等国产数据库。
2. **框架级初始化** -- CDP 框架自身的表结构和基础数据需要在应用首次启动时自动创建，且支持增量更新。
3. **ShardingSphere 兼容** -- 当数据源为 ShardingSphere 分库分表时，Flyway 需要绕过 ShardingSphere 代理，直接对底层真实数据源执行迁移。
4. **脚本执行容错** -- 区分 DDL 建表脚本和 DML 数据初始化脚本，对后者降低失败敏感度。

> 设计决策：选择通过 Flyway SPI Plugin 机制扩展而非 fork Flyway 源码，保持对 Flyway 版本升级的兼容性。同时通过 BaseInitCdpData 抽象类实现基于 Java 的迁移，获得比纯 SQL 脚本更灵活的执行控制。

## 二、整体架构

模块文件结构分为三个层次：

```
leatop-cdp-base-flyway/
  src/main/java/
    com/leatop/cdp/flyway/
      |- CdpFlywayAutoConfiguration         # Spring Boot 自动配置
      |- strategy/CdpFlywayMigrationStrategy # 自定义迁移策略（ShardingSphere 兼容）
      |- common/CdpScriptRunner              # 增强的 SQL 脚本执行器
      |- exception/DDLException              # DDL 执行异常
    db/migration/
      |- BaseInitCdpData                     # Java 迁移基类
      |- mysql/R__InitCdpData                # MySQL 可重复迁移
      |- mysql/V1_0_0__InitCdpData           # MySQL 版本迁移
      |- dm/R__InitCdpData                   # 达梦可重复迁移
      |- dm/V1_0_0__InitCdpData              # 达梦版本迁移
      |- gauss/R__InitCdpData                # OpenGauss 可重复迁移
      |- gaussdb/R__InitCdpData              # GaussDB 可重复迁移
      |- gbase/R__InitCdpData                # GBase 可重复迁移
    org/flywaydb/database/
      |- dm/DmSQL*                           # 达梦数据库 Flyway 插件
      |- gaussdb/OpenGaussSQL*               # OpenGauss Flyway 插件
      |- kingbase/Kingbase*                  # 金仓 Flyway 插件
      |- mysql/MySQL*                        # MySQL Flyway 插件（覆盖原生）
  src/main/resources/
    META-INF/services/
      org.flywaydb.core.extensibility.Plugin # SPI 注册文件
```

## 三、核心设计模式

### 3.1 SPI 插件模式 -- 厂商检测机制

Flyway 通过 Java SPI 加载 `org.flywaydb.core.extensibility.Plugin` 接口的实现类。CDP 在 `META-INF/services/` 中注册了以下 DatabaseType 插件：

- DmSQLDatabaseType -- 匹配 `jdbc:dm:` URL，产品名以 "DM DBMS" 开头
- OpenGaussSQLDatabaseType -- 匹配 `jdbc:opengauss:` 或 `jdbc:postgresql:` URL，产品名为 "PostgreSQL" 且版本 >= 9.2
- KingbaseDatabaseType -- 匹配 `jdbc:kingbase8:` URL，产品名含 "KingbaseES"
- MySQLDatabaseType / MariaDBDatabaseType -- 覆盖原生 MySQL 实现

每个 DatabaseType 实现 `handlesJDBCUrl()` 和 `handlesDatabaseProductNameAndVersion()` 两个检测方法，Flyway 启动时按 `getPriority()` 返回值从高到低匹配。CDP 为各插件分配了不同的优先级（OpenGauss=10, Kingbase=11, DM=12），确保匹配顺序正确。

> 设计决策：OpenGaussSQLDatabaseType 同时匹配 `jdbc:postgresql:` URL 以兼容 PolarDB 等使用 PostgreSQL 驱动的数据库。通过优先级机制确保在同时注册 PostgreSQL 和 OpenGauss 插件时，OpenGauss 优先匹配。

每个 DatabaseType 还提供对应的 Database、Connection、Schema、Table 实现类，处理各厂商 SQL 方言差异（如 schema 查询、表存在性检查、锁机制等）。

### 3.2 模板方法模式 -- BaseInitCdpData

BaseInitCdpData 继承 Flyway 的 BaseJavaMigration，是 CDP 框架数据初始化的核心抽象类。它定义了以下模板流程：

1. **getChecksum()** -- 扫描 classpath 中匹配 `getLocationPattern()` 的所有 SQL 文件，计算 CRC32 校验和总和。Flyway 通过校验和变化检测是否需要重新执行可重复迁移。

2. **migrate()** -- 执行迁移的主流程：
   - 查询 `flyway_schema_history` 表获取已执行文件记录（按文件名 + 校验和二元组去重）。
   - 先执行非 `cdp_*_init.sql` 的建表脚本（stopOnError=true）。
   - 再执行 `cdp_*_init.sql` 数据初始化脚本（stopOnError=false，允许部分失败）。
   - 每个脚本执行后记录到 `flyway_schema_history` 表。

子类只需实现两个抽象方法：
- `getLocationPattern()` -- 返回 SQL 文件的 classpath 匹配模式（如 `classpath*:/db/mysql/**/cdp_*.sql`）。
- `getLogger()` -- 返回日志实例。

V1_0_0__InitCdpData 继承对应数据库的 R__InitCdpData，复用同一套扫描逻辑，区别仅在于 Flyway 将 V 开头的类视为版本迁移（只执行一次），R 开头的类视为可重复迁移（校验和变化时重新执行）。

> 设计决策：达梦的 R__InitCdpData 重写了 `saveFinishedFiles()` 方法，因为达梦数据库的 INSERT 语法对列名反引号和布尔类型的处理与 MySQL 不同（使用 `true` 替代 `1`，去除反引号）。

### 3.3 策略模式 -- CdpFlywayMigrationStrategy

CdpFlywayMigrationStrategy 实现 Spring Boot 的 FlywayMigrationStrategy 接口，在标准 Flyway 迁移前增加 ShardingSphere 检测逻辑：

1. 检查 `spring.datasource.url` 是否以 `jdbc:shardingsphere:` 开头。
2. 如果是，解析 ShardingSphere YAML 配置，提取标记了 `flyway: true` 的底层数据源。
3. 为每个真实数据源创建独立的 Flyway 实例执行迁移。
4. 如果不是 ShardingSphere 数据源，直接执行标准迁移。
5. 迁移失败时自动调用 `flyway.repair()` 尝试修复。

### 3.4 CdpScriptRunner -- 增强脚本执行器

CdpScriptRunner 基于 MyBatis 的 ScriptRunner 改造，主要增强：

- **注释中分隔符处理** -- `commandReadyToExecute()` 方法在检测到分隔符时，会排除 SQL COMMENT 字符串中的分隔符（如 `COMMENT '备注;说明'`），避免误截断。
- **可配置错误处理** -- 通过 `stopOnError` 和 `errorLogWriter` 控制脚本执行失败时的行为，DDL 脚本严格模式、DML 脚本宽容模式。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| CdpFlywayAutoConfiguration | 自动配置入口，`@AutoConfigureBefore(FlywayAutoConfiguration.class)` 确保在 Spring Boot 原生 Flyway 配置前加载 |
| CdpFlywayMigrationStrategy | 自定义迁移策略，处理 ShardingSphere 数据源和迁移失败自动修复 |
| BaseInitCdpData | Java 迁移基类，模板方法模式实现 SQL 文件扫描、校验和计算、分阶段执行 |
| R__InitCdpData (各数据库目录) | 各厂商的可重复迁移实现，定义 SQL 文件扫描路径 |
| V1_0_0__InitCdpData (各数据库目录) | 版本迁移，继承 R__ 复用逻辑 |
| CdpScriptRunner | 增强 SQL 脚本执行器，处理注释中分隔符问题 |
| DDLException | DDL 执行异常包装 |
| DmSQLDatabaseType | 达梦数据库 Flyway 插件，JDBC URL 和产品名检测 |
| OpenGaussSQLDatabaseType | OpenGauss/PolarDB 数据库插件 |
| KingbaseDatabaseType | 金仓数据库插件 |

## 五、扩展机制

1. **新增数据库支持** -- 实现 BaseDatabaseType 子类及配套的 Database、Connection、Schema、Table 类，在 SPI 文件中注册，然后在 `db/migration/{vendor}/` 目录下创建对应的 R__InitCdpData 子类。

2. **自定义迁移脚本** -- 业务项目在自己的 `resources/db/migration/{vendor}/` 目录下放置 SQL 文件，Flyway 会自动扫描执行。框架级和业务级脚本通过目录隔离互不干扰。

3. **替换迁移策略** -- CdpFlywayMigrationStrategy 以 `@Component` 注册，业务项目可声明自己的 FlywayMigrationStrategy Bean 替换默认行为。

## 六、模块协作（简要）

- **与 ShardingSphere 模块**：CdpFlywayMigrationStrategy 解析 ShardingSphere 配置文件获取真实数据源，对每个标记 `flyway: true` 的数据源独立执行迁移。
- **与各业务模块**：每个业务模块可在自己的 `resources/db/migration/` 下放置迁移脚本，框架通过 Flyway 的 `locations` 配置统一管理。
- **与 CdpExtensionProperties**：`databaseIds` 映射关系用于 MyBatis databaseId 判断，与 Flyway 的厂商检测逻辑保持对应。

## 七、设计权衡与约束（简要）

1. **覆盖原生 MySQL 插件** -- CDP 注册了自己的 MySQLDatabaseType 覆盖 Flyway 原生实现，以统一处理 P6SPY 代理 URL。这要求升级 Flyway 时检查兼容性。

2. **R__ 可重复迁移的校验和策略** -- BaseInitCdpData.getChecksum() 返回所有 SQL 文件校验和的总和。新增或修改任何一个 SQL 文件都会触发整体重新执行，可能导致已执行脚本再次运行。通过 `flyway_schema_history` 表中的细粒度文件级记录来避免重复执行。

3. **DDL 与 DML 分离执行** -- 建表脚本严格模式（失败即停止），数据初始化脚本宽容模式（失败继续）。通过文件名前缀 `cdp_*_init.sql` 区分，命名约定是强制性的。

4. **自动修复** -- 迁移失败时自动调用 `flyway.repair()` 清理失败记录，降低人工干预需求，但也可能掩盖需要关注的迁移问题。
