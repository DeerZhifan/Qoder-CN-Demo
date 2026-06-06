---
trigger: when_referenced
knowledge_source:
  - cdp-design-flyway
  - cdp-module-flyway
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-flyway` 依赖
- 创建或修改 `db/migration/` 目录下的 SQL 脚本
- 配置 `spring.flyway.*` 相关属性
- 讨论数据库版本管理、表结构变更、数据初始化

---

## 前置依赖

1. Maven 依赖：

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

2. `<properties>` 中须声明 `<flyway.version>9.22.3</flyway.version>`。

3. 数据库连接配置（`spring.datasource.*`）已就绪。

---

## 配置要点

```yaml
spring:
  flyway:
    enabled: true                    # 是否启用（开发环境 true，生产按需）
    clean-disabled: true             # 禁止清理数据库表（生产必须为 true）
    baseline-on-migrate: true        # 数据库非空时需设为 true
    validate-on-migrate: false       # 迁移时是否校验
    baseline-version: 1.0.0          # 基准版本号
    locations:
      - classpath:db/migration/mysql # 迁移脚本目录（必填，按数据库类型调整）
```

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

### 目录结构

```
src/main/resources/
└── db/migration/
    └── {数据库类型}/
        ├── V1.0.1__Create_user_table.sql
        ├── V1.0.2__Add_email_to_user.sql
        └── R__Init_default_data.sql
```

---

## 代码模式

### 推荐写法

**脚本命名规则：**

```
V<版本号>__<描述>.sql        # 版本迁移（只执行一次）
R__<描述>.sql                # 可重复迁移（内容变化时重新执行）
```

- 版本号与描述之间是**两个下划线**（`__`）
- 版本号格式 `x.y.z`（如 `1.0.1`），必须递增，不可与已有脚本重复
- 描述使用英文，单词间用下划线分隔（如 `Create_order_table`）

**DDL 与 DML 脚本分离：**

- 建表脚本（DDL）：严格模式执行，失败即停止
- 数据初始化脚本（DML）：文件名匹配 `cdp_*_init.sql` 前缀，宽容模式执行（失败继续）
- 每个脚本只完成一个明确的变更，复杂变更应拆分为多个小脚本

**建表模板（MySQL）：**

```sql
-- V1.0.1__Create_order_table.sql
CREATE TABLE IF NOT EXISTS `order` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) DEFAULT NULL COMMENT '租户ID',
    `create_gmt` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_gmt` DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
```

**加字段模板：**

```sql
-- V1.0.2__Add_email_to_user.sql
ALTER TABLE `user` ADD COLUMN `email` VARCHAR(100) COMMENT '邮箱';
```

**ShardingSphere 环境：** 框架通过 `CdpFlywayMigrationStrategy` 自动检测 ShardingSphere 数据源，解析标记 `flyway: true` 的底层真实数据源独立执行迁移，无需额外配置。

**迁移失败自动修复：** 迁移失败时框架自动调用 `flyway.repair()` 清理失败记录，降低人工干预需求。

### 禁止事项

- **禁止修改已执行的迁移脚本** -- 否则 Flyway 校验失败。如需修改，应创建新版本的迁移脚本
- **禁止手动修改 `flyway_schema_history` 表** -- 该表由 Flyway 自动创建和维护
- **禁止版本号重复或倒退** -- V 脚本版本号必须严格递增
- **禁止在 V 脚本中放置可重复执行的数据初始化** -- 应使用 R 脚本
- **禁止生产环境设置 `clean-disabled: false`** -- 会导致 `flyway.clean()` 清空所有表
- **禁止在单个脚本中混合多个不相关的变更** -- 每个脚本只做一件事
- **禁止 Flyway 社区版中期望回滚** -- 不支持回滚，修复错误应编写新的迁移脚本
- **禁止将 SQL 脚本放在约定目录外而不配置 `locations`** -- 否则 Flyway 扫描不到
