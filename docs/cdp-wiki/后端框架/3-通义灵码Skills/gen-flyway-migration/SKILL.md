# 生成 Flyway 迁移脚本

## 描述

生成符合 CDP 框架约定的 Flyway 数据库迁移 SQL 脚本，支持 MySQL、达梦、OpenGauss、GaussDB、金仓、GBase、PolarDB 等多种数据库。

## 输入

请向用户确认以下信息：

1. **版本号**（如 `1.0.1`，须大于当前最大版本号）
2. **描述**（英文，用下划线分隔，如 `Create_order_table`）
3. **数据库类型**（`mysql` / `dm` / `gauss` / `gaussdb` / `gbase`，默认 `mysql`）
4. **脚本类型**（`V` 版本迁移 或 `R` 可重复迁移，默认 `V`）
5. **SQL 内容概述**（建表 / 加字段 / 初始化数据等）

---

## 步骤 1：确定文件路径和名称

### 命名规则

```
V<版本号>__<描述>.sql        # 版本迁移（只执行一次）
R__<描述>.sql                # 可重复迁移（内容变化时重新执行）
```

注意：版本号与描述之间是**两个下划线**（`__`）。

### 文件路径

```
src/main/resources/db/migration/{数据库类型}/
```

示例：

- `src/main/resources/db/migration/mysql/V1.0.1__Create_order_table.sql`
- `src/main/resources/db/migration/mysql/R__Init_default_data.sql`

### 数据库目录对照

| 数据库 | 目录名 |
|--------|--------|
| MySQL | `mysql` |
| 达梦 | `dm` |
| OpenGauss | `gauss` |
| GaussDB | `gaussdb` |
| 金仓（兼容 MySQL 版） | `mysql` |
| PolarDB | `gauss` |
| GBase | `gbase` |

## 步骤 2：生成 SQL 内容

> 根据用户描述的变更内容生成 SQL。以下为建表模板示例。

**建表模板（MySQL）：**

```sql
-- V{版本号}__{描述}.sql
-- {变更说明}

CREATE TABLE IF NOT EXISTS `{表名}` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) DEFAULT NULL COMMENT '租户ID',
    `create_gmt` DATETIME DEFAULT NULL COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_gmt` DATETIME DEFAULT NULL COMMENT '更新时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{表中文名}';
```

**加字段模板（MySQL）：**

```sql
-- V{版本号}__{描述}.sql
ALTER TABLE `{表名}` ADD COLUMN `{字段名}` {字段类型} COMMENT '{字段说明}';
```

**初始化数据模板：**

```sql
-- R__{描述}.sql
-- 可重复执行的数据初始化脚本
INSERT INTO `{表名}` (`id`, `{字段}`) VALUES ('{值}', '{值}')
ON DUPLICATE KEY UPDATE `{字段}` = VALUES(`{字段}`);
```

## 步骤 3：验证前置条件

检查以下配置是否已就绪：

1. `pom.xml` 中已引入 `leatop-cdp-base-flyway` 依赖
2. `application.yaml` 中 Flyway 配置的 `locations` 包含目标目录
3. `application-dev.yaml` 中 `spring.flyway.enabled: true`

如缺少以上配置，提示用户先执行 `enable-flyway` 或手动添加。

---

## 完成后提醒

1. 已执行的迁移脚本**禁止修改**内容，否则 Flyway 校验失败
2. 版本号必须递增，不可与已有脚本重复
3. 每个脚本只完成一个明确的变更，复杂变更应拆分为多个小脚本
4. 生产环境必须设置 `spring.flyway.clean-disabled: true`
5. Flyway 社区版不支持回滚，如需修复错误应编写新的迁移脚本
6. 如需多数据库支持，应在对应的数据库目录下分别创建脚本（SQL 方言可能不同）
