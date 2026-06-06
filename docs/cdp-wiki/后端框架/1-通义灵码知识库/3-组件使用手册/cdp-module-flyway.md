# 如何使用 CDP 数据库版本管理组件

## 概述

数据库版本管理组件（`leatop-cdp-base-flyway`）基于 Flyway 实现数据库迁移管理，支持 MySQL、达梦、OpenGauss、GaussDB、金仓、PolarDB、GBase 等多种数据库，通过扩展插件方式适配信创数据库。

## 启用方式

**1. 添加 Maven 依赖：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-flyway</artifactId>
</dependency>
<!-- 对应数据库驱动，如 MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**2. 添加配置：**

```yaml
spring:
  flyway:
    enabled: true                    # 是否启用
    clean-disabled: true             # 禁止清理数据库表（生产必须为 true）
    baseline-on-migrate: true        # 数据库非空时需设为 true
    validate-on-migrate: false       # 迁移时是否校验
    baseline-version: 1.0.0          # 基准版本号
    locations:
      - classpath:db/migration/mysql # 迁移脚本目录（必填）
```

## 迁移脚本规范

### 目录结构

```
src/main/resources/
└── db/migration/
    └── {数据库类型}/          # mysql / dm / gauss / gaussdb / gbase
        ├── V1.0.1__Create_user_table.sql
        └── V1.0.2__Add_email_to_user.sql
```

### 命名规则

```
V<版本号>__<描述>.sql        # 版本迁移（只执行一次）
R__<描述>.sql                # 可重复迁移（内容变化时重新执行）
```

示例：

```
V1.0.1__Create_user_table.sql
V1.0.2__Add_email_to_user.sql
R__Init_default_data.sql
```

### 数据库类型对照

| 数据库 | 目录名 |
|--------|--------|
| MySQL | `mysql` |
| 达梦 | `dm` |
| OpenGauss | `gauss` |
| GaussDB | `gaussdb` |
| 金仓（兼容 MySQL 版） | `mysql` |
| PolarDB | `gauss` |

## 使用示例

### 创建迁移脚本

```sql
-- V1.0.1__Create_user_table.sql
CREATE TABLE IF NOT EXISTS `user` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

```sql
-- V1.0.2__Add_email_to_user.sql
ALTER TABLE `user` ADD COLUMN `email` VARCHAR(100);
```

### 启动自动执行

Spring Boot 启动时自动检测并执行未应用的迁移脚本，控制台输出：

```
Flyway Community Edition 9.22.3 by Redgate
Current version of schema: << Empty Schema >>
Migrating schema to version "1.0.1 - Create user table"
Migrating schema to version "1.0.2 - Add email to user"
Successfully applied 2 migrations
```

## 注意事项

> 注意：启动时会自动创建 `flyway_schema_history` 表，用于记录已执行的迁移版本，不要手动修改该表。

> 注意：已执行的迁移脚本不要修改内容，否则校验会失败。如需修改，应创建新版本的迁移脚本。

> 注意：每个脚本只完成一个明确的变更，复杂变更应拆分为多个小脚本。

> 注意：Flyway 社区版不支持回滚，如需修复错误应编写新的迁移脚本。

> 注意：生产环境必须设置 `spring.flyway.clean-disabled: true`，防止误清数据库。

> 注意：如脚本不在约定目录下，可在 `spring.flyway.locations` 中添加额外路径。
