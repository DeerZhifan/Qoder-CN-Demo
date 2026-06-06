# CDP 资源管理 设计手册

> 对应使用手册：[cdp-module-system-resource.md](../3-组件使用手册/cdp-module-system-resource.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

资源管理模块统一抽象系统中的菜单、按钮、接口、数据实体等可控访问对象，是 RBAC 权限分配的目标端。设计目标包括：

1. 用统一的 `ResourcePo` 模型表达目录、菜单、操作按钮、接口四种资源类型，避免多表分散管理
2. 资源以树形结构组织，支持无限层级的目录嵌套
3. 提供功能资源（`ResourceBusiness`）和数据资源（`DataResourceBusiness`）两套独立的管理体系
4. 数据资源与数据权限模块联动，实现行/列级别的数据访问控制

> 设计决策：功能资源和数据资源分为两张表（`frame_resource` 和 `frame_data_resource`），因为它们的属性差异较大。功能资源关注 URL、图标、页面路径等展示属性；数据资源关注数据源、编码、授权展示方式等数据治理属性。

## 二、整体架构

资源模块包含两条并行的管理线：

```
功能资源线:
  ResourceController --> ResourceBusiness --> ResourceService --> ResourceDao
                                                                    |
                                                            frame_resource (树形)
                                                                    |
                                          RoleOperatorService <-- frame_orgstruc_role_operator
                                          (角色-资源绑定)

数据资源线:
  DataResourceController --> DataResourceBusiness --> DataResourceService --> DataResourceDao
                                                                                |
                                                                   frame_data_resource
                                                                                |
                             DataResourcePermBusiness --> DataResourcePermService
                             (数据权限分配)                frame_data_resource_perm
```

## 三、核心设计模式

### 3.1 统一资源类型体系

`ResourcePo.type` 定义了五种资源类型：

| type | 名称 | 说明 |
|------|------|------|
| 1 | 目录 | 菜单的分组容器，不可直接访问 |
| 2 | 菜单 | 对应一个前端页面路由 |
| 3 | 操作资源 | 页面内的按钮或操作项 |
| 4 | 接口 | 后端 API 端点 |
| 5 | 实体资源 | 数据实体（关联数据资源） |

所有类型共用 `frame_resource` 表和 `ResourcePo` 实体，通过 `type` 字段区分。树形结构通过 `parentResourceId` 字段建立父子关系。

### 3.2 资源树与菜单初始化

`ResourceBusiness.initMenu()` 是前端启动时的核心调用，返回当前用户可见的菜单树（`List<MenuQo>`）。其内部逻辑：

1. 获取当前用户的所有角色 ID
2. 通过 `RoleOperatorService` 聚合角色关联的所有资源 ID
3. 从 `frame_resource` 中查询这些资源，组装为树形结构
4. 按 `seq` 字段排序，过滤 `hidden=1` 的隐藏资源

`selectResourceByRoleId(roleId)` 和 `selectResourceByUserId(userId, orgId)` 返回 `ResourceTreeDto` 树形结构，其中 `isSelected` 字段标记资源是否已分配给指定角色或用户。

### 3.3 资源的多端归属

`ResourcePo.belongTo` 字段区分资源的目标终端：

- `0` -- 桌面端
- `1` -- 移动端
- `2` -- 接口

这使得同一套资源管理体系可以服务于 Web 端、移动端和 API 网关三种场景。

### 3.4 资源 URL 与路由

`ResourcePo` 中有三个路径相关字段：

- `url`：资源的后端 API 路径，用于 URL 级权限校验
- `pagePath`：前端 Vue 页面组件路径，用于路由渲染
- `urlType`：路径类型（1-路由路径, 2-非弹出外链, 3-弹出外链），控制前端的导航行为

### 3.5 数据资源与数据权限

`DataResourceBusiness` 管理数据资源（`DataResourceDto`），每个数据资源定义了一个可控的数据集合：

- `dataCode`：数据资源唯一编码，业务系统通过此编码引用
- `sourceFrom`：数据来源地址
- `showType`：数据授权的展示方式

`DataResourcePermBusiness` 管理数据资源的权限分配，核心接口：

- `save(dataResourcePermList)` -- 批量保存数据权限配置
- `getDataPermIds2(userId, dataCode)` -- 获取用户对指定数据资源可访问的主键 ID 集合
- `getDataPermIds3(userId, dataCode, moduleId)` -- 带模块维度的数据权限查询

> 设计决策：`getDataPermIds2` 和 `getDataPermIds3` 标记为"供业务系统使用，不可改动"，作为稳定的 SPI 契约，业务系统在查询数据时调用这些接口获取可访问的数据 ID 集合，再作为过滤条件注入查询。

## 四、关键类说明

| 类名 | 所属模块 | 职责 |
|------|---------|------|
| `ResourceBusiness` | api | 功能资源 Feign 接口，含资源 CRUD、菜单初始化、角色/用户资源树查询 |
| `DataResourceBusiness` | api | 数据资源 Feign 接口，含数据资源 CRUD 和预览 |
| `DataResourcePermBusiness` | api | 数据权限 Feign 接口，含权限分配和权限 ID 查询 |
| `ResourcePo` | service | 功能资源实体，对应 `frame_resource` 表，支持五种资源类型 |
| `DataResourcePo` | service | 数据资源实体，对应 `frame_data_resource` 表 |
| `DataResourcePermPo` | service | 数据权限配置实体，对应 `frame_data_resource_perm` 表 |
| `ResourceDao` | service | 功能资源持久层 |
| `ResourceDto` | api | 功能资源 DTO，包含前端渲染所需的路由、图标等字段 |
| `ResourceTreeDto` | api | 资源树形结构 DTO，含 `isSelected` 选中标记和 `children` 子节点列表 |
| `MenuQo` | api | 菜单查询结果对象，`initMenu` 接口的返回类型 |
| `DataResourceDto` | api | 数据资源 DTO，含 `AddGroup`/`UpdateGroup` 分组校验 |
| `DataResourcePermDto` | api | 数据权限配置 DTO |
| `RoleOperatorService` | service | 角色-资源关系服务，桥接角色模块与资源模块 |

## 五、扩展机制

1. **资源类型扩展**：`type` 为整数字段，新增资源类型（如报表、仪表盘）只需扩展枚举值和对应的前端渲染逻辑
2. **数据权限 SPI**：`DataResourcePermBusiness.getDataPermIds2()` 和 `getDataPermIds3()` 作为稳定接口供业务系统调用，业务系统将返回的 ID 集合注入 SQL 条件实现行级过滤
3. **数据资源预览**：`DataResourceBusiness.preview()` 支持预览数据资源的内容，便于管理员在配置权限前确认数据范围
4. **多端资源管理**：通过 `belongTo` 字段实现桌面端、移动端、接口三套资源的统一管理和独立授权

## 六、模块协作（简要）

- **角色模块**：`RoleOperatorService` 管理角色-资源的绑定关系，`RoleBusiness.updateRoleOperator()` 触发资源分配
- **用户模块**：`UserBusiness.getUserForbiddenPermissions()` 查询用户无权限的资源列表，前端据此控制 UI 元素的显隐
- **租户模块**：`ResourcePo.tenantId` 标识资源所属租户；资源管理功能仅超级管理员可操作，租户管理员通过角色间接获取资源权限
- **数据权限模块（datascope）**：数据资源定义在 `frame_data_resource` 中，`leatop-cdp-business-datascope` 在 SQL 层面拦截查询并注入数据权限过滤条件

## 七、设计权衡与约束（简要）

1. **统一资源表**：五种资源类型共用一张表，简化了权限分配逻辑（一次角色-资源关联覆盖所有类型），但单表数据量可能较大，需关注查询性能。`parentResourceId` 索引和 `type` 过滤是主要的查询优化手段
2. **功能资源与数据资源分离**：两者分别在 `frame_resource` 和 `frame_data_resource` 中管理。功能资源控制"能做什么"，数据资源控制"能看到什么"。这种分离使得两种权限的生命周期和管理流程可以独立演进
3. **菜单树的运行时组装**：`initMenu` 每次请求都从数据库查询并组装菜单树，未做服务端缓存。在权限频繁变更的场景下这保证了实时性，但高并发场景下可考虑结合 CDP 两级缓存机制进行优化
