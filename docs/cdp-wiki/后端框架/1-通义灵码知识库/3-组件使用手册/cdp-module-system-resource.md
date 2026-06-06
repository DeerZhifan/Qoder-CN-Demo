# 如何使用 CDP 资源管理功能

## 概述

资源管理维护系统的菜单、按钮、数据实体等资源定义。资源通过角色分配给用户，控制用户可访问的功能和数据。

## 核心接口

### 功能资源（菜单/按钮）

**Controller 路径前缀：** `/system/resource`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/resource/add` | POST | 添加资源（菜单/按钮） |
| `/system/resource/update` | POST | 更新资源 |
| `/system/resource/get/{id}` | GET | 获取资源详情 |
| `/system/resource/delete/{ids}` | POST | 删除资源 |
| `/system/resource/list` | POST | 获取子资源列表 |
| `/system/resource/listPage` | POST | 分页查询资源 |
| `/system/resource/initMenu` | POST | 初始化当前用户菜单树 |
| `/system/resource/list/{roleId}/all` | POST | 获取角色的资源树 |
| `/system/resource/list/user/all/{userId}/{orgId}` | POST | 获取用户在组织下的资源树 |

### 数据资源（数据实体）

**Controller 路径前缀：** `/system/dataResource`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/dataResource/add` | POST | 添加数据资源 |
| `/system/dataResource/update` | POST | 更新数据资源 |
| `/system/dataResource/get/{id}` | GET | 获取数据资源详情 |
| `/system/dataResource/delete/{ids}` | POST | 删除数据资源 |
| `/system/dataResource/listPage` | POST | 分页查询 |
| `/system/dataResource/list` | POST | 查询所有数据资源 |

## 菜单初始化流程

前端启动时调用 `/system/resource/initMenu` 获取当前用户可见的菜单树，渲染左侧导航栏。

## 注意事项

> 注意：`initMenu` 返回的菜单树已根据用户角色过滤，只包含有权限的资源。

> 注意：数据资源用于数据权限控制（`business-datascope`），定义哪些数据表需要做行/列级权限管理。

> 注意：资源管理功能仅超级管理员可操作，不应分配给租户管理员。
