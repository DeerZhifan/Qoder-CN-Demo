# CDP 代码生成器 设计手册

> 对应使用手册：[cdp-module-gen.md](../3-组件使用手册/cdp-module-gen.md)

## 一、设计目标与背景

代码生成器组件（`leatop-cdp-base-gen`）旨在根据数据库表结构自动生成符合 CDP 分层规范（PO/DTO/QO/DAO/Service/Controller/Vue）的前后端代码，降低重复编码工作量。

核心设计目标：

1. **模板可管理** -- 模板以"模板组"为单位组织，支持数据库存储（`DB:` 前缀）和 classpath 文件两种来源，用户可通过管理界面上传、预览、替换模板。
2. **多数据源感知** -- 生成器可连接本地数据源或外部配置数据源读取表结构，支持跨库生成。
3. **类型自动推断** -- 数据库列类型通过 `SqlTypeEnum` 自动映射为 Java 类型，并推断前端控件类型。

> 设计决策：选择 Apache Velocity 作为模板引擎，因为其语法简单、社区成熟，且 `.vm` 文件可直接由非开发人员编辑。

## 二、整体架构

```
┌─────────────────────────────────────────────────┐
│                 Controller 层                    │
│  CodeCreateConfigController                     │
│  TemplateController / TemplateGroupController   │
│  CodeDataSourceController / CodeModuleInfoController │
│  EnumController                                 │
└───────────┬─────────────────────────────────────┘
            │
            v
┌─────────────────────────────────────────────────┐
│              Business Service 层                 │
│  CodeCreateConfigBusinessServiceImpl            │
│  TemplateBusinessServiceImpl                    │
│  TemplateGroupBusinessServiceImpl               │
│  CodeDataSourceBusinessServiceImpl              │
│  CodeModuleInfoBusinessServiceImpl              │
└───────────┬─────────────────────────────────────┘
            │
            v
┌─────────────────────────────────────────────────┐
│              模板渲染引擎                         │
│  VelocityUtils (String/ClassPath Engine)        │
└───────────┬─────────────────────────────────────┘
            │
            v
┌─────────────────────────────────────────────────┐
│              数据层                               │
│  CodeTemplateParamDAO / TemplateDao             │
│  TemplateGroupDao / CodeDataSourceDao           │
│  CodeModuleInfoDao                              │
└─────────────────────────────────────────────────┘
```

## 三、核心设计模式

### 模板组织模式

模板采用二级结构：**模板组 -> 模板**。一个模板组代表一套完整的代码生成方案（如"标准 CRUD 模板组"），包含 PO、DTO、Controller 等多个模板文件。`TemplateTypeEnum` 枚举定义了所有支持的模板类型（PO、DTO、QO、MAPPER、SERVICE、CONTROLLER、FRONT_END_VUE 等），每种类型对应确定的输出文件路径和后缀。

模板内容存储在数据库 `frame_code_template` 表的 `template_content` 字段中（路径以 `DB:` 前缀标识），也支持从 classpath 加载（路径以 `classpath:` 前缀标识）。

### 双引擎渲染

`VelocityUtils` 封装了两个独立的 VelocityEngine 实例：

- **stringEngine** -- 使用 `StringResourceLoader`，用于渲染数据库中存储的模板字符串。
- **classPathEngine** -- 使用 `ClasspathResourceLoader`，用于渲染 classpath 下的 `.vm` 文件。

`CodeCreateConfigBusinessServiceImpl.createCodeFile()` 根据模板路径前缀自动选择引擎。

> 设计决策：将两个引擎分开初始化而非共用一个多 loader 引擎，避免 StringResourceLoader 的全局仓库在并发场景下互相干扰。

### 类型推断链

数据库列类型 -> `SqlTypeEnum`（SQL 类型枚举） -> `JavaTypeEnum`（Java 类型枚举） -> `FormTypeEnum`（前端控件类型枚举） / `FrontWebTypeEnum`（前端展示类型枚举）。`convertColumnInfo()` 方法在读取表结构后自动完成驼峰命名转换和类型推断。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| `GenAutoConfiguration` | 自动配置类，扫描 `com.leatop.cdp.base.gen` 包并注册 Mapper |
| `CodeCreateConfigBusinessServiceImpl` | 代码生成配置的核心业务类，负责配置 CRUD、表结构读取、代码渲染和 ZIP 打包下载 |
| `TemplateBusinessServiceImpl` | 模板管理业务类，支持模板的上传、预览（Base64）、导入和分页查询 |
| `TemplateGroupBusinessServiceImpl` | 模板组管理业务类 |
| `VelocityUtils` | Velocity 引擎封装，提供字符串模板和 classpath 文件模板两种渲染方式 |
| `SqlTypeEnum` | SQL 列类型到 Java 类型的映射枚举 |
| `TemplateTypeEnum` | 模板类型枚举，定义生成文件的分层归属 |
| `RegexConstant` | 正则常量，用于模板变量提取 |
| `CodeDataSourceBusinessServiceImpl` | 数据源管理，支持连接测试和多数据源切换 |

## 五、扩展机制

1. **自定义模板**：上传 `.vm` 文件到模板组，使用 Velocity 标准语法和框架提供的模板变量（`${params.className}`、`${params.columns}` 等）即可生成任意格式的代码。
2. **新增模板类型**：在 `TemplateTypeEnum` 中新增枚举值，并在 `CodeCreateConfigBusinessServiceImpl.getFileName()` 中添加对应的路径生成规则。
3. **扩展外部数据源**：通过 `CodeDataSourceController` 管理界面添加外部数据库连接，生成器通过 JDBC 直连读取表结构，不依赖 CDP 主数据源。

## 六、模块协作（简要）

- **Flyway**：代码生成器依赖的数据库表（`frame_code_*`）通过 Flyway 自动迁移创建。
- **分库分表**：`cdp-default-tables/cdp-gen-tables.yml` 声明了代码生成器自身的表，在分库分表场景下可被自动注册为单表。
- **认证模块**：生成器接口受 CDP 统一认证拦截，`@TenantUnaware` 注解标记其业务服务跳过租户过滤。

## 七、设计权衡与约束（简要）

- **模板内容存数据库**：相比文件系统存储增加了管理便利性，但大量模板内容存储在 `template_content` TEXT 字段中，对数据库有一定压力。
- **临时文件清理**：生成代码后通过 `ScheduledExecutorService` 延迟 5-7 秒删除临时文件，在高并发下存在竞态风险，适用于低频操作场景。
- **不支持增量生成**：每次生成全量输出，不检测已有代码的手动修改，适合初始化阶段使用。
