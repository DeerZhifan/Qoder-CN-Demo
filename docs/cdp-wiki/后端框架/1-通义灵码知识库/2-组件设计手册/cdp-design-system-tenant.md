# CDP 租户管理 设计手册

> 对应使用手册：[cdp-module-system-tenant.md](../3-组件使用手册/cdp-module-system-tenant.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

租户管理是 CDP 多租户体系的基础模块，负责租户的全生命周期管理。设计目标包括：

1. 提供基于 `tenant_id` 的逻辑数据隔离方案，同一数据库内多租户共存
2. 租户创建时自动初始化完整的基础数据（管理员账号、根组织、管理员角色），实现开箱即用
3. 支持租户的启用/禁用、权限分配和缓存管理
4. 租户上下文在请求链路中透明传递，业务代码无需感知多租户逻辑

> 设计决策：采用共享数据库、共享 Schema、按 `tenant_id` 字段隔离的方式，而非独立数据库方案。这降低了运维成本，但需要框架层面确保所有查询自动注入租户过滤条件。

## 二、整体架构

租户模块的数据模型相对简洁，但其影响贯穿整个系统：

```
frame_tenant (租户主表)
    |--- 创建时自动生成:
    |    ├── frame_orgstruc_user (租户管理员)
    |    ├── frame_orgstruc_orgunit (根组织)
    |    └── frame_orgstruc_role (租户管理员角色, roleType=1)
    |
    |--- 所有业务表通过 tenant_id 字段关联
```

`TenantBusiness` 定义 Feign 接口，`TenantService` 继承 `IService<TenantPo>` 实现核心逻辑，`TenantDao` 继承 `BaseMapper<TenantPo>` 提供持久层操作。

## 三、核心设计模式

### 3.1 租户数据隔离

CDP 的多租户隔离在框架层面通过 `BasePo` 中的 `tenant_id` 字段和 MyBatis-Plus 的租户拦截器实现。所有继承 `BasePo` 的实体类在 SQL 执行时自动注入 `WHERE tenant_id = ?` 条件，业务代码无需手动添加租户过滤。

对于需要跨租户操作的场景（如超管查询所有租户、预设角色管理），使用 `@TenantUnaware` 注解标记方法，框架跳过该方法的租户过滤。

> 设计决策：租户过滤在 MyBatis 拦截器层面全局生效，而非在 Service 层逐个添加。这确保了隔离的完整性，消除了开发者遗漏租户条件的风险。

### 3.2 租户创建与初始化链

创建租户时触发一系列级联操作：

1. 插入 `TenantPo` 到 `frame_tenant` 表
2. 创建租户根组织（`OrgUnitPo`），`orgType=1`（公司类型），`orgName` 取自 `TenantDto.openorg`
3. 创建租户管理员用户（`UserPo`），`userType=1`（租户管理员），默认密码 `88888888`
4. 创建租户管理员角色（`RolePo`），`roleType=1`
5. 建立用户-角色、用户-组织的关联关系

`TenantDto` 中的 `userId` 和 `roleId` 字段为 `@JsonProperty(access = READ_ONLY)`，仅在返回时携带管理员和角色信息，输入时忽略。

### 3.3 租户状态管理

`TenantService.changeStatue()` 实现批量启用/禁用：

- `TenantPo.enableUse` 字段（映射 `isuse` 列）控制租户状态
- 禁用租户后，该租户下所有用户的登录验证将被拦截（`TenantService.checkTenant()` 返回 false）

### 3.4 租户缓存管理

`TenantBusiness.clearCache()` 提供手动清除租户相关缓存的能力。修改租户权限配置后调用此接口，确保权限变更即时生效。缓存键以 `tenantId` 为维度组织。

### 3.5 租户与组织名称同步

`TenantService.syncOrgNameToTenantOrg()` 在租户管理员修改根组织名称时，同步更新 `frame_tenant` 表中的 `openorg` 字段，保持租户信息与组织信息的一致性。

## 四、关键类说明

| 类名 | 所属模块 | 职责 |
|------|---------|------|
| `TenantBusiness` | api | 租户 Feign 接口，定义租户 CRUD 和状态管理操作 |
| `TenantService` | service | 租户核心业务逻辑，继承 `IService<TenantPo>` |
| `TenantDao` | service | 租户持久层，继承 `BaseMapper<TenantPo>` |
| `TenantPo` | service | 租户实体，对应 `frame_tenant` 表，继承 `BasePo` |
| `TenantDto` | api | 租户 DTO，包含管理员和角色的只读输出字段 |
| `TenantPageQo` | api | 租户分页查询参数 |
| `TenantController` | controller | REST 控制器，委托 `TenantBusiness` 处理请求 |

## 五、扩展机制

1. **租户校验扩展**：`TenantService.checkTenant()` 提供租户合法性校验入口，可在登录流程中调用以判断租户状态。业务系统可结合 `SecuritySettingBusiness` 实现更细粒度的租户级安全策略
2. **租户编码唯一性**：`TenantService.validateCode()` 在创建和编辑时校验编码唯一性，支持排除当前租户 ID 的去重校验
3. **选项列表**：`TenantBusiness.listOptions()` 返回 `BaseOptionItem<String>` 列表，供前端下拉选择器使用，遵循 CDP 统一的选项数据结构

## 六、模块协作（简要）

- **用户模块**：租户创建时自动创建管理员用户，`UserBusiness` 参与管理员账号的初始化
- **组织模块**：租户创建时自动创建根组织，`OrgUnitBusiness` 参与根组织的初始化；根组织改名同步触发 `syncOrgNameToTenantOrg()`
- **角色模块**：租户创建时自动创建管理员角色（roleType=1），`RoleBusiness.getTenantAdminRole()` 查询租户管理角色
- **认证模块**：登录流程中通过 `checkTenant()` 验证租户是否启用，禁用租户下的用户无法通过认证

## 七、设计权衡与约束（简要）

1. **共享数据库方案**：所有租户数据在同一数据库中，通过 `tenant_id` 逻辑隔离。优势是部署和运维简单；约束是不支持租户级别的物理数据库隔离，大规模多租户场景下需评估性能
2. **级联初始化的原子性**：租户创建涉及多表插入（租户、用户、组织、角色），需在事务内完成。当前实现依赖 Spring 事务管理，跨服务场景下需关注分布式事务一致性
3. **超管专属操作**：租户管理、资源管理等功能仅超级管理员可操作。框架通过 `userType` 判断而非角色判断来控制，确保租户管理员无法越权访问
