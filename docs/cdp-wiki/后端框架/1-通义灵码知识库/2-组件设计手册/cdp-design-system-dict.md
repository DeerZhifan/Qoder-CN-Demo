# CDP 字典管理 设计手册

> 对应使用手册：[cdp-module-system-dict.md](../3-组件使用手册/cdp-module-system-dict.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

字典管理是几乎所有企业应用的基础设施：下拉框选项、状态码映射、类型枚举等均依赖字典数据。`leatop-cdp-business-system` 中的字典子模块围绕以下目标设计：

1. **树形字典结构**：通过 `parentId` 构建父子层级关系，支持多级字典嵌套，满足复杂分类场景（如行政区划、多级业务类型）。
2. **多租户隔离**：字典数据按 `oldTenantId` 进行租户级隔离，缓存键也包含租户标识，确保不同租户的字典数据互不干扰。
3. **缓存加速查询**：高频访问的字典通过 CDP 两级缓存（Caffeine L1 + Redis L2）提升读取性能，减少数据库压力。
4. **与 Echo 模块无缝集成**：通过实现 `LoadService` 接口，字典值可被 `@Echo` 注解自动回显，将字典编码透明转为显示文本。

> 设计决策：字典采用单表 `DictionaryPO` 统一存储，通过 `parentId` 字段构建树形关系，而非旧版中 `DictionaryKind`（字典类型）和 `DictionaryValue`（字典值）双表设计。单表方案简化了查询逻辑并天然支持无限层级嵌套。旧版双 Controller（`DictionaryKindController`、`DictionaryValueController`）已标记 `@Deprecated`。

## 二、整体架构

```
DictionaryController (REST 接口)
  |
  v
DictionaryBusiness / DictionaryBusinessImpl (业务逻辑)
  |
  +---> DictionaryDao (MyBatis-Plus Mapper，操作 DictionaryPO)
  |
  +---> CdpCacheClient / CdpCache (两级缓存读写)
  |
  +---> IUserHelper (获取当前租户信息)

DictionaryEchoServiceImpl (implements LoadService)
  |
  +---> DictionaryBusiness (查询可用字典)
  +---> EchoProperties (获取字典分隔符配置)
```

**缓存流程**：读取字典时，`DictionaryBusinessImpl.getAvailableListByParentId()` 先检查 `cdp.dict.enableCache` 开关，若开启则从 `CdpCache`（缓存名 `dict_cache`）中按 `{tenantId}_{parentId}` 键读取。缓存未命中时回源数据库查询，但不自动回填缓存。缓存需通过 `/system/dictionary/cacheChildren` 接口显式预热写入。

> 设计决策：采用"显式预热"而非"读穿透自动回填"的缓存策略，是因为字典数据的变更频率极低，由管理员手动触发预热可以精确控制哪些字典进入缓存，避免冷数据占用缓存空间。

## 三、关键类说明

### DictionaryBusinessImpl

字典模块的核心业务实现，标注 `@BusinessService`。主要职责：

- **缓存管理**：通过 `@PostConstruct` 在 Bean 初始化时获取名为 `dict_cache` 的 `CdpCache` 实例。`cacheChildren()` 方法将指定 `parentId` 下的子字典写入缓存，缓存键格式为 `{tenantId}_{parentId}` 或 `{tenantId}_{parentId}_{dictType}`。
- **树形查询**：`childrenByParentId()` 支持两种模式 -- 仅查询直接子节点（默认）或递归展开整棵子树（`includeSubtree=true`）。子树查询通过 `likeRight` 前缀匹配 `parentId` 实现单次 SQL 递归加载。
- **parentId 编码规则**：顶层字典的 `parentId` 为 `-1`，其 parentId 键为 `valueCode` 本身；非顶层字典的 parentId 键为 `parentId/valueCode` 拼接格式，形成路径式层级标识。

### DictionaryEchoServiceImpl

实现 `com.leatop.cdp.base.echo.core.LoadService` 接口，是字典与 Echo 模块的桥梁。当业务 DTO 中的字段标注了 `@Echo(api = "dictionaryEchoServiceImpl", parentId = "xxx")` 时，Echo 框架会调用其 `findByIds()` 方法批量加载字典显示文本。

该方法的返回值 Map 的键格式为 `{parentId}{separator}{valueCode}`，其中 `separator` 来自 `EchoProperties.dictSeparator` 配置。Echo 框架根据这一组合键将字典名称回填到 DTO 的 `ref` 指定字段。

### DictionaryPO

持久化实体，映射数据库表字段。关键字段包括：`valueCode`（字典值编码）、`valueName`（字典值名称）、`parentId`（父级标识）、`status`（状态，1=启用）、`serial`（排序号）、`oldTenantId`（租户标识）。

## 四、扩展机制

1. **自定义缓存策略**：通过 `cdp.dict.enableCache` 配置项可全局关闭字典缓存。如需更细粒度控制（如按字典类型设置不同 TTL），可扩展 `DictionaryBusinessImpl` 中的缓存键策略，配合 `CdpCache` 的分区能力实现。
2. **Echo 回显扩展**：`DictionaryEchoServiceImpl` 注册为名为 `dictionaryEchoServiceImpl` 的 Spring Bean。业务模块可在任意 DTO 字段上通过 `@Echo` 注解引用此服务，实现字典码到文本的自动翻译，无需手动调用查询接口。
3. **字典数据初始化**：字典数据通过 Flyway 迁移脚本或管理界面维护。框架不内置种子数据，各项目按需在 `db/migration/` 下提供初始字典 SQL。
