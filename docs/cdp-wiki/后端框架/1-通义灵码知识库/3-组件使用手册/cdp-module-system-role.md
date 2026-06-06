# 如何使用 CDP 角色与权限管理功能

## 概述

角色与权限管理提供 RBAC（基于角色的访问控制）能力，包括角色 CRUD、资源分配、数据权限配置和用户关联。

## 核心接口

**Controller 路径前缀：** `/system/role`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/role/add` | POST | 创建角色 |
| `/system/role/update` | POST | 更新角色 |
| `/system/role/get/{id}` | GET | 获取角色详情 |
| `/system/role/delete/{ids}` | POST | 删除角色 |
| `/system/role/listPage` | POST | 分页查询角色 |
| `/system/role/list` | POST | 查询所有角色（可按 roleType 过滤） |
| `/system/role/operators/{roleId}` | POST | 给角色分配资源/操作人员 |
| `/system/role/{roleIds}/operator` | POST | 查询角色已分配的资源 |
| `/system/role/roleListByUserId/{userId}` | POST | 查询用户拥有的角色 |
| `/system/role/orgunit/{orgUnitId}` | POST | 查询组织下的角色 |
| `/system/role/loadDatas/{roleId}` | POST | 加载角色的数据权限范围 |
| `/system/role/updateDatas/{roleId}/{dsType}` | POST | 更新角色的数据权限范围 |
| `/system/role/getUsers` | POST | 分页查询角色下的用户 |

### 数据资源权限接口

**Controller 路径前缀：** `/system/dataResourcePerm`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/dataResourcePerm/pageByRoleId` | POST | 按角色查询数据权限 |
| `/system/dataResourcePerm/pageByUserId` | POST | 按用户查询数据权限 |
| `/system/dataResourcePerm/getSelectedDataPerms` | GET | 获取角色已选的数据权限 |
| `/system/dataResourcePerm/savePermission` | POST | 保存数据权限配置 |

## 权限分配流程

```
1. 创建角色 → POST /system/role/add
2. 给角色分配菜单/功能资源 → POST /system/role/operators/{roleId}
3. 配置角色的数据权限范围 → POST /system/role/updateDatas/{roleId}/{dsType}
4. 将角色分配给用户 → 在用户管理中设置
```

## 注意事项

> 注意：角色分为普通角色和预设角色（preset），预设角色通过 `/presetListPage` 查询，作用于整个租户范围。

> 注意：`@TenantUnaware` 标记的接口可跨租户操作，使用时需注意数据隔离。

> 注意：数据权限范围（dsType）控制角色可访问的数据边界（如：本人数据、本部门数据、全部数据等）。
