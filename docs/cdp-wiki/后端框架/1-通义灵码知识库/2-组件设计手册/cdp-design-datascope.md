# CDP 数据权限 设计手册

> 对应使用手册：[cdp-module-datascope.md](../3-组件使用手册/cdp-module-datascope.md)

## 一、设计目标与背景

在多租户企业应用中，不同角色的用户对同一张数据表的可见范围存在差异。传统方案要求业务代码手动拼接 SQL 过滤条件，导致权限逻辑与业务逻辑深度耦合，难以统一维护。

CDP 数据权限模块的设计目标：

1. **声明式接入** -- 业务代码通过注解或配置即可启用，无需修改 SQL。
2. **行列双控** -- 行权限通过 SQL WHERE 条件实现过滤，列权限通过结果集拦截实现字段脱敏/隐藏。
3. **多租户融合** -- 数据实体的租户范围控制与行权限统一管道处理，避免两套拦截器各自执行。
4. **可扩展** -- 通过 DataPermissionHandler SPI 允许业务自定义权限逻辑，无需修改框架代码。

> 设计决策：选择 MyBatis-Plus DataPermissionInterceptor 作为 SQL 改写引擎，而非自研 SQL 解析器，以复用 JSqlParser 的成熟能力并与 MyBatis-Plus 生态保持一致。

## 二、整体架构

数据权限的执行链路跨越三个层次，自上而下依次触发：

```
HTTP 请求
  |
  v
[DataPermissionHandlerExecutors]  -- Spring MVC 拦截器，处理全局生效的 Handler
  |  将权限条件写入 DataPermissionHolder (ThreadLocal)
  v
[DataPermissionAdvisor]           -- AOP MethodInterceptor，处理 @DataPermission 注解
  |  追加注解指定 Handler 产生的权限条件到 ThreadLocal
  v
[DataPermissionInterceptor]       -- MyBatis-Plus InnerInterceptor (SQL 改写层)
  |  CustomDataPermissionHandler 读取 ThreadLocal，生成 JSqlParser Expression
  v
[CustomSelectFieldsInterceptor]   -- MyBatis ResultSetHandler 拦截器 (列权限)
  |  读取 ThreadLocal 中的 noPermissionFieldNames，将无权字段置 null
  v
响应返回
```

权限条件的产生与消费完全解耦：Handler 只负责产生 DataPermissionCondition 列表，SQL 改写层只负责消费。两者通过 DataPermissionHolder 这一 ThreadLocal 桥梁连接。

## 三、核心设计模式

### 3.1 策略模式 -- DataPermissionHandler

DataPermissionHandler 是权限条件的生产者接口。框架提供 DefaultDataPermissionHandler 作为默认实现（基于 RBAC），业务可注册多个 Handler 实例，在 `@DataPermission(handlers = {...})` 中按名称引用。

每个 Handler 声明 `isGlobal()` 方法：返回 `true` 表示在配置的 API 路径范围内自动生效（由 DataPermissionHandlerExecutors 驱动）；返回 `false` 表示仅在标注了 `@DataPermission` 注解的方法中触发（由 DataPermissionAdvisor 驱动）。

> 设计决策：采用双驱动机制（拦截器 + AOP）而非单一入口，是因为全局 Handler 需要在请求级别尽早执行（避免嵌套调用时重复查询），而注解 Handler 需要获取方法参数做精细化控制。

### 3.2 ThreadLocal 传递 -- DataPermissionHolder

DataPermissionHolder 维护两个 ThreadLocal 变量：

- `HOLDER` -- 当前线程累积的 DataPermissionCondition 列表。
- `FINISHED_HANDLERS` -- 已执行过的 Handler 类名集合，用于防止同一请求内重复执行同一个 Handler。

特殊标记 `"*"` 表示跳过所有数据权限检查，由 `@UncheckDataPermission` 注解触发写入。DataPermissionAdvisor 在方法调用前后保存/恢复旧的 ThreadLocal 状态，确保嵌套调用场景下外层权限不被内层覆盖。

### 3.3 SQL 改写 -- CustomDataPermissionHandler

CustomDataPermissionHandler 实现了 MyBatis-Plus 的 MultiDataPermissionHandler 接口，核心方法 `getSqlSegment(Table, Expression, String)` 按以下流程工作：

1. 从 DataPermissionHolder 获取当前权限条件列表。
2. 根据 TableInfoHelper 建立字段属性名到数据库列名的映射。
3. 遍历每个 DataPermissionCondition，匹配表名后将 DataRuleCondition 递归转换为 JSqlParser Expression（支持嵌套子条件组）。
4. 多个条件之间根据 `operate` 字段用 AND/OR 连接。

> 设计决策：规则条件支持 `children` 嵌套结构，使用 ParenthesedExpressionList 包装子表达式组，确保生成的 SQL 括号语义正确。这让前端可以构建任意深度的条件树。

### 3.4 列权限拦截 -- CustomSelectFieldsInterceptor

列权限不在 SQL 层面处理（避免改写 SELECT 字段列表的复杂性），而是在 MyBatis ResultSetHandler 层面拦截查询结果，对无权字段调用 setter 置 null。字段名集合从 DataPermissionHolder.getNoPermissionFieldNames() 获取，由 DataScopeBusinessImpl 在查询数据规则时根据 colRules 配置计算得出。

## 四、关键类说明

| 类名 | 所属模块 | 职责 |
|------|---------|------|
| DataPermissionHandler | common-core | 权限条件生产者 SPI 接口 |
| DefaultDataPermissionHandler | datascope-api | 默认 RBAC 实现，查询角色数据规则并替换变量 |
| DataPermissionAdvisor | common-core | AOP 拦截器，处理 @DataPermission 注解方法 |
| DataPermissionHandlerExecutors | common-core | MVC 拦截器，驱动 isGlobal()=true 的 Handler |
| DataPermissionHolder | common-core | ThreadLocal 容器，传递权限条件和已执行 Handler |
| DataPermissionCondition | common-core | 权限条件模型，包含表名、列名、操作符、值列表及 tableRules |
| CustomDataPermissionHandler | common-core | MyBatis-Plus MultiDataPermissionHandler 实现，将条件转为 SQL Expression |
| CustomSelectFieldsInterceptor | common-starter | MyBatis 结果集拦截器，实现列级字段隐藏 |
| DataScopeBusinessImpl | datascope-service | 业务门面，查询用户数据规则并替换变量（`${user_id}` 等） |
| DataScopeServiceImpl | datascope-service | 数据访问层，带 `@Cacheable` 全表缓存 |
| RoleExtraDataService | datascope-service | 登录时将角色 ID 写入用户会话扩展数据 |
| DataScopePO | datascope-service | 数据规则持久化对象，映射 `frame_data_scope` 表 |
| RuleConditionHandler | datascope-service | MyBatis-Plus JSON 类型处理器，序列化/反序列化行权限规则 |

## 五、扩展机制

### 5.1 自定义 DataPermissionHandler

实现 DataPermissionHandler 接口并注册为 Spring Bean，即可被 `@DataPermission(handlers = "beanName")` 引用。自定义 Handler 可以：

- 根据方法参数动态生成权限条件（通过 `methodParams` 参数获取）。
- 指定 `tableName`、`columnName`、`type` 等注解属性做精细过滤。
- 通过 `isGlobal()` 返回 `true` 实现全局自动生效。

### 5.2 配置文件控制拦截范围

通过 CdpExtensionProperties.DataPermissionPath 的 `includeApis` 和 `excludeApis` 配置项，控制 DataPermissionHandlerExecutors 拦截器的生效路径，无需修改代码。

### 5.3 替换 SQL 改写引擎

CustomDataPermissionHandler 以 `@ConditionalOnMissingBean` 方式注册。业务项目可提供自定义的 MyBatis-Plus DataPermissionHandler Bean 替换默认实现。

## 六、模块协作

### 与 auth 模块

- 数据权限依赖 IUserHelper 获取当前用户信息（userId、orgId、tenantId）。
- RoleExtraDataService 实现 UserExtraDataService 接口，在用户登录时将角色 ID 列表写入会话扩展数据，避免每次权限查询都访问数据库。

### 与 system 模块

- 数据实体（受控表）通过 ResourceService 管理，存储为 `type=5` 的资源记录。
- 角色到数据规则的映射通过 RoleService 和 RoleOperatorService 查询。
- 租户范围通过 TenantService 获取租户列表并映射显示名称。
- 组织层级通过 OrgUnitService.getAllOrgListByParentId() 展开 `${org_ids}` 变量。

### 与 cache 模块

- DataScopeServiceImpl 使用 `@Cacheable(cacheNames = "cdp:dataScopes")` 全表缓存数据规则，`@CacheEvict` 在批量保存时清除。底层走 CDP 两级缓存体系（Caffeine L1 + Redis L2）。

### 与 common-core 中的租户机制

- DefaultDataPermissionHandler 生成的租户过滤条件会与 TenantContentHolder 联动：当数据权限已显式控制某张表的 tenant_id 时，通过 `TenantContentHolder.setIgnoreTable()` 避免框架级租户拦截器重复追加条件。

## 七、设计权衡与约束

1. **全表缓存 vs 精确查询** -- 数据规则采用全表缓存（limit 1000）加内存过滤方式，适用于规则数量可控的场景。若规则量级超过千级，需调整缓存策略。

2. **列权限在结果集层面实现** -- 未在 SQL SELECT 层面裁剪字段，而是查出后置 null。优点是实现简单、不影响 MyBatis 映射逻辑；缺点是敏感数据仍会从数据库传输到应用层，安全级别较低。

3. **变量替换在 Java 层完成** -- `${user_id}`、`${org_ids}` 等变量在 DataScopeBusinessImpl 中替换为具体值后再传入 SQL 改写层。这意味着变量种类的扩展需要修改 Java 代码，目前不支持通过配置动态添加新变量类型。

4. **Handler 去重机制** -- 通过 FINISHED_HANDLERS 集合避免同一 Handler 在一次请求中被重复执行。这在嵌套调用（Controller A 调用 Service B，B 内部又触发了标注 @DataPermission 的方法）场景下尤为重要。

5. **注解优先级** -- `@UncheckDataPermission` 标注在方法上时，会跳过当前方法的所有数据权限检查，但不影响该方法内部调用的其他方法的权限控制（因为 DataPermissionAdvisor 在 finally 块中恢复旧状态）。
