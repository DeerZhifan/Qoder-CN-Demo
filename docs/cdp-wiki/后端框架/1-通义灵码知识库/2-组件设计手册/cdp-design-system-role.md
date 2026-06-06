# CDP 角色与权限 设计手册

> 对应使用手册：[cdp-module-system-role.md](../3-组件使用手册/cdp-module-system-role.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

角色模块是 CDP RBAC 权限体系的核心，负责角色的定义、功能资源分配、数据权限管理和用户关联。设计目标包括：

1. 支持多种角色类型，覆盖超级管理员、租户管理员、公司管理员、普通角色、预设角色等场景
2. 实现功能权限（菜单/按钮）和数据权限（数据范围）的双维度控制
3. 角色与组织绑定，支持按公司维度管理角色
4. 预设角色跨租户共享，普通角色租户隔离

> 设计决策：角色类型采用枚举值 `roleType` 区分（0-普通, 1-租户管理员, 2-预设, 3-超管, 4-公司管理员, 5-全体用户, 6-审计管理员），而非继承体系，保持数据模型扁平简洁。

## 二、整体架构

角色模块涉及多张关联表，形成以角色为中心的权限关系网络：

```
frame_orgstruc_role          (角色主表)
    |--- frame_orgstruc_role_operator  (角色-资源关系)
    |--- frame_orgstruc_role_data      (角色-数据资源关系)
    |--- frame_orgstruc_roleorg        (角色-组织关系，数据权限范围)
    |--- frame_orgstruc_user_role      (用户-角色关系)
```

`RoleBusiness` 作为 Feign 接口暴露角色管理能力，`RoleService` 承载核心业务逻辑，`RoleOperatorService`、`RoleDataService`、`RoleOrgService` 分别管理三种关联关系。

## 三、核心设计模式

### 3.1 角色类型体系

`RolePo.roleType` 定义了 7 种角色类型，各类型的创建方式和作用范围不同：

| roleType | 名称 | 创建方式 | 作用范围 |
|----------|------|---------|---------|
| 0 | 普通角色 | 管理员手动创建 | 所属公司内 |
| 1 | 租户管理员 | 创建租户时自动生成 | 整个租户 |
| 2 | 预设角色 | 超管创建，`@TenantUnaware` 查询 | 跨租户共享 |
| 3 | 超级管理员 | 系统内置 | 全局 |
| 4 | 公司管理员 | 创建公司时自动生成 | 所属公司 |
| 5 | 全体用户 | 创建公司时自动生成 | 所属公司 |
| 6 | 审计管理员 | 管理员手动创建 | 所属公司内 |

> 设计决策：公司管理员(4)和全体用户(5)角色在 `OrgUnitService.createCompanyRole()` 中随公司创建自动生成，确保每个公司都有基础角色结构，减少人工配置。

### 3.2 功能权限分配（RBAC）

功能权限通过 `RoleOperatorService` 管理角色与资源的绑定关系：

1. `updateRoleOperator(roleId, operatorIds)` -- 批量重置角色关联的资源 ID
2. `selectByRoleIds(roleIds)` -- 根据角色 ID 集合查询已分配的资源
3. `getOperatorIdByRoleIds(roleIds)` -- 获取角色关联的资源 ID 列表

`RoleOperatorDto` 中 `operatorId` 与 `ResourcePo.resourceId` 一致，建立角色到资源的映射。用户登录后，系统聚合其所有角色的资源权限，生成可访问的菜单树和 URL 权限列表。

### 3.3 数据权限范围（DataScope）

数据权限通过 `dsType` 字段控制角色可访问的数据边界，`RoleBusiness` 提供两个关键接口：

- `loadDatasByRoleId(roleId)` -- 返回 `DataScopeDto`，包含当前数据权限类型和组织机构树（含选中状态标记 `isSelected`）
- `updateDatasByRoleId(roleId, dsType, orgIds)` -- 更新角色的数据权限类型和自定义组织范围

`RoleOrgPo`（对应 `frame_orgstruc_roleorg`）记录角色与组织的关联，当 `dsType` 为自定义时，通过此表存储角色可访问的具体组织 ID 集合。

### 3.4 预设角色与租户无关查询

预设角色（roleType=2）通过 `@TenantUnaware` 注解标记 `presetListPage()` 方法，绕过租户过滤器实现跨租户查询。这使得超级管理员可以定义全局角色模板，各租户直接引用。

## 四、关键类说明

| 类名 | 所属模块 | 职责 |
|------|---------|------|
| `RoleBusiness` | api | 角色 Feign 接口，含角色 CRUD、资源分配、数据权限管理 |
| `RoleService` | service | 角色核心业务，继承 `IService<RolePo>` |
| `RoleOperatorService` | service | 角色-资源（操作权限）关系维护 |
| `RoleDataService` | service | 角色-数据资源关系维护 |
| `RoleOrgService` | service | 角色-组织关系维护（数据权限范围） |
| `UserRoleService` | service | 用户-角色关系维护 |
| `RolePo` | service | 角色实体，对应 `frame_orgstruc_role` 表 |
| `RoleOrgPo` | service | 角色-组织关系实体，对应 `frame_orgstruc_roleorg` 表 |
| `RoleDataPo` | service | 角色-数据资源关系实体，对应 `frame_orgstruc_role_data` 表 |
| `RoleDto` | api | 角色 DTO，含 `AddGroup`/`UpdateGroup` 分组校验 |
| `RoleOperatorDto` | api | 角色操作权限 DTO，`operatorId` 对应 `resourceId` |
| `DataScopeDto` | api | 数据权限范围 DTO，含嵌套组织树结构 `DataScopeOrg` |

## 五、扩展机制

1. **角色类型扩展**：`roleType` 字段为整数枚举，新增角色类型只需在业务层添加对应逻辑，无需修改表结构
2. **数据权限类型扩展**：`dsType` 为字符串类型，允许业务系统自定义数据权限维度，结合 `leatop-cdp-business-datascope` 模块实现 SQL 级别的数据过滤
3. **角色-组织关联**：通过 `RoleDto.orgId` 将角色绑定到组织层级，`getRoleListByOrgId()` 可查询指定组织下的角色列表，支持按组织维度展示角色

## 六、模块协作（简要）

- **用户模块**：`UserRoleService` 管理用户-角色关系，`RoleBusiness.getRoleListByUserId()` 查询用户角色
- **资源模块**：通过 `RoleOperatorService` 关联资源，`ResourceBusiness.selectResourceByRoleId()` 查询角色资源树
- **组织模块**：`RolePo.orgId` 指定角色所属组织，`createCompanyRole()` 在创建公司时自动生成角色
- **租户模块**：`RolePo` 继承 `BasePo` 包含 `tenantId`，查询时自动注入租户过滤；`getTenantAdminRole()` 获取租户管理员角色
- **数据权限模块**：`DataResourcePermBusiness` 管理数据资源的行级权限，与角色的 `dsType` 配合实现完整的数据访问控制

## 七、设计权衡与约束（简要）

1. **扁平角色模型**：当前不支持角色继承或角色层级嵌套，所有权限通过直接的角色-资源关系表达。这简化了权限计算逻辑，但需要在角色分配时手动维护权限的完整性
2. **资源分配为全量替换**：`updateRoleOperator()` 采用先删后插策略，每次更新角色资源时全量重置关联关系，避免增量操作的并发一致性问题
3. **预设角色的安全边界**：预设角色使用 `@TenantUnaware` 跨租户查询，但资源分配仍受租户约束，需确保预设角色不包含租户专属资源
