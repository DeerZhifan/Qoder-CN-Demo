# 如何使用 CDP 组织机构管理功能

## 概述

组织机构管理提供树形组织结构的维护能力，支持公司、部门、群组三级组织类型，支持组织排序、转移、复制等操作。

## 核心接口

**Controller 路径前缀：** `/system/orgunit`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/orgunit/add` | POST | 添加组织（公司/部门/群组） |
| `/system/orgunit/update` | POST | 更新组织信息 |
| `/system/orgunit/get/{orgId}` | GET | 获取组织详情 |
| `/system/orgunit/delete/{orgIds}` | POST | 删除组织 |
| `/system/orgunit/list/all/{parentOrgId}` | POST | 获取完整子树 |
| `/system/orgunit/list/children/{parentOrgId}` | POST | 获取直属子组织 |
| `/system/orgunit/listPage` | POST | 分页查询组织 |
| `/system/orgunit/sort/{orgId}/{orderType}` | POST | 组织排序（上移/下移） |
| `/system/orgunit/transfer/{orgId}/{transferToOrgId}` | POST | 转移组织到新上级 |
| `/system/orgunit/copyDepartment/{orgId}/{copyOrgIds}` | POST | 复制部门到指定公司 |
| `/system/orgunit/change/{companyId}` | POST | 切换当前公司 |

## 组织层级规则

```
租户公司（根节点）
├── 子公司          ← 公司下可创建子公司、部门、群组
│   ├── 部门        ← 部门下可创建子部门、群组（不能创建公司）
│   │   └── 群组    ← 群组下不能创建任何组织
│   └── 群组
└── 部门
```

## 注意事项

> 注意：组织机构树的根节点是租户公司，每个租户有独立的组织树。

> 注意：删除组织前需确保该组织下无子组织和用户，否则删除失败。

> 注意：`transfer` 操作会将组织及其所有子组织整体移动到新的上级节点下。

> 注意：部分接口（如 `matchLocalOrgFromTemp`、`listBySyncIds`）用于组织数据同步场景，通过 `@ServiceScope("micro")` 限制为微服务内部调用。
