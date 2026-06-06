# 如何使用 CDP 用户管理功能

## 概述

用户管理是系统管理模块（`leatop-cdp-business-system`）的核心功能，提供用户 CRUD、密码管理、用户与组织机构关联等能力。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-system-boot-starter</artifactId>
</dependency>
```

## 核心接口

**Controller 路径前缀：** `/system/user`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/user/listPage` | POST | 分页查询用户列表 |
| `/system/user/add` | POST | 添加用户 |
| `/system/user/update` | POST | 更新用户信息 |
| `/system/user/get/{id}` | GET | 获取用户详情 |
| `/system/user/delete/{ids}/{orgId}` | POST | 删除用户 |
| `/system/user/curUser` | POST | 获取当前登录用户信息 |
| `/system/user/changePassword` | POST | 修改密码 |
| `/system/user/resetPassword/{userId}` | POST | 管理员重置密码 |
| `/system/user/validateAccount/{account}` | POST | 校验账号是否可用 |
| `/system/user/users/{roleId}` | POST | 查询角色下的用户 |
| `/system/user/list/depart/{companyId}/{departmentId}` | POST | 查询部门下的用户 |

### 用户-组织关系接口

**Controller 路径前缀：** `/system/userOrgUnit`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/userOrgUnit/listByUserIds` | POST | 查询用户的组织关系 |
| `/system/userOrgUnit/insert` | POST | 添加用户组织关系 |
| `/system/userOrgUnit/deleteByUserIdAndIgnoreOrgId` | POST | 删除用户组织绑定 |

## 业务接口（微服务调用）

```java
import com.leatop.cdp.system.business.UserBusiness;

@Autowired
private UserBusiness userBusiness;

// 分页查询用户
Page<UserDTO> page = userBusiness.listPage(userQO);

// 获取当前用户信息（含角色）
CurrentUserDto curUser = userBusiness.getCurUser(true);

// 获取用户无权限资源列表
List<String> forbidden = userBusiness.getUserForbiddenPermissions(userId, tenantId);
```

> 注意：`UserBusiness` 使用 `@FeignClient` 标注，单体部署走本地调用，微服务部署走 Feign 远程调用。

## 注意事项

> 注意：用户添加/更新操作使用 `@Validated` 分组校验（`AddGroup` / `UpdateGroup`），需在 DTO 上配置对应分组。

> 注意：带 `@ServiceScope("micro")` 标记的接口仅用于微服务内部调用，不应暴露给前端。

> 注意：用户密码使用 BCrypt 编码（`CdpPasswordEncoder`），重置密码默认为 `88888888`。
