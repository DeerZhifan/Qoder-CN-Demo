# CDP 用户管理 设计手册

> 对应使用手册：[cdp-module-system-user.md](../3-组件使用手册/cdp-module-system-user.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

用户管理是 CDP 框架的核心身份模块，承担用户全生命周期管理职责。设计目标包括：

1. 支持多租户环境下的用户隔离与统一管理
2. 提供灵活的用户-组织多对多关联模型，满足企业用户在不同组织间的隶属、兼职、借调等复杂关系
3. 实现可配置的密码安全策略，满足企业信息安全合规要求
4. 通过 `@FeignClient` 实现单体与微服务两种部署模式下的透明调用

> 设计决策：用户与组织的关系独立建表（`frame_orgstruc_user_orgunit`），而非在用户表中存储组织 ID，以支持多组织归属和关系类型区分。

## 二、整体架构

用户模块位于 `leatop-cdp-business-system` 内，遵循标准的 5 子模块结构。核心数据流如下：

```
UserController --> UserBusiness (Feign 接口)
                       |
               UserBusinessImpl --> UserService --> UserDao (MyBatis-Plus)
                       |                               |
               UserOrgUnitService              frame_orgstruc_user (PO)
                       |
               frame_orgstruc_user_orgunit (关系表)
```

UserBusiness 作为 Feign 接口定义在 api 模块中，单体部署时由 `UserBusinessImpl` 直接实现本地调用，微服务部署时通过 Feign 代理进行远程调用。

## 三、核心设计模式

### 3.1 用户-组织多对多模型

用户与组织通过中间表 `UserOrgUnitPo`（对应 `frame_orgstruc_user_orgunit`）关联，支持四种关系类型：

- `relationship = 0` -- 隶属关系（主部门）
- `relationship = 1` -- 兼职关系
- `relationship = 2` -- 借调关系
- `relationship = 3` -- 代管关系

每条关系记录包含 `userId`、`orgId`、`companyId`，确保用户在每个公司下有明确的归属。`UserOrgUnitBusiness` 接口提供关系的增删查操作。

> 设计决策：将组织关系类型作为 `relationship` 字段存储在关系表中，而非拆分为多张表，降低了查询复杂度，同时通过枚举值约束保证数据一致性。

### 3.2 密码安全策略

密码管理涉及三个层次：

1. **编码存储**：使用 `CdpPasswordEncoder`（BCrypt）对密码进行单向哈希，数据库中不存储明文
2. **安全策略校验**：`SecurityPasswordService.checkPasswordSecuritySetting()` 根据 `SecuritySettingValuesDto` 中的配置项（密码长度、复杂度、历史密码重复次数等）校验新密码
3. **密码历史记录**：`SecurityPasswordService.listLastNumberData()` 查询用户最近 N 次修改密码记录，防止密码重复使用

密码变更的三种来源通过 `operateSource` 参数区分：修改密码(1)、忘记密码(2)、重置密码(3)。管理员重置密码默认为 `88888888`。

### 3.3 账号锁定机制

`UserDto` 中的 `loginerrorcount` 和 `locktime` 字段协同实现登录失败锁定：连续登录失败达到阈值后锁定账号，管理员可通过 `unlockAccount()` 手动解锁。`checkUpdatePwd()` 在登录流程中检查用户是否需要强制修改密码（首次登录或密码过期场景）。

### 3.4 登录扩展点

`UserExtraDataService` 接口提供登录后扩展数据的注入点，业务方实现 `needSaveExtraDataAfterLogin()` 方法可在登录时附加自定义数据到会话中。

## 四、关键类说明

| 类名 | 所属模块 | 职责 |
|------|---------|------|
| `UserBusiness` | api | Feign 业务接口，定义用户管理全部操作契约 |
| `UserService` | service | 用户核心业务逻辑接口，继承 `IService<UserPo>` |
| `UserOrgUnitBusiness` | api | 用户-组织关系 Feign 接口 |
| `UserOrgUnitService` | service | 用户-组织关系维护 |
| `SecurityPasswordService` | service | 密码历史记录与安全策略校验 |
| `UserExtraDataService` | api | 登录扩展数据 SPI 接口 |
| `UserPo` | service | 用户实体，对应 `frame_orgstruc_user` 表 |
| `UserOrgUnitPo` | service | 用户-组织关系实体，对应 `frame_orgstruc_user_orgunit` 表 |
| `UserDto` | api | 用户数据传输对象，包含组织、角色等关联冗余字段 |
| `UserInfoDto` | api | 用户新增/修改时的入参 DTO，配合 `AddGroup`/`UpdateGroup` 分组校验 |
| `UserPageQo` | api | 用户分页查询参数对象 |

## 五、扩展机制

1. **UserExtraDataService**：SPI 扩展接口，业务系统实现该接口后注入 Spring 容器，登录流程自动调用 `needSaveExtraDataAfterLogin()` 获取扩展数据
2. **分组校验**：通过 `@Validated(AddGroup.class)` 和 `@Validated(UpdateGroup.class)` 在同一个 DTO 上实现新增和修改的差异化校验规则
3. **微服务内部接口**：标记 `@ServiceScope("micro")` 的方法（如 `getUserByAccountAndTenantIdForLogin`）仅暴露给微服务间调用，前端不可直接访问

## 六、模块协作（简要）

- **角色模块**：`UserDto.userRoleIds` 关联角色 ID，`RoleBusiness.getRoleListByUserId()` 查询用户角色列表
- **组织模块**：通过 `UserOrgUnitPo` 关联组织，`OrgUnitBusiness` 提供组织信息查询
- **资源模块**：`UserBusiness.getUserForbiddenPermissions()` 获取用户无权限资源列表，与 `ResourceBusiness` 协作实现菜单过滤
- **租户模块**：`UserDto.tenantId` 标识用户所属租户，查询时自动注入租户过滤条件
- **安全配置模块**：`SecuritySettingBusiness` 提供密码策略配置，`SecurityPasswordService` 执行校验

## 七、设计权衡与约束（简要）

1. **UserDto 字段冗余**：`UserDto` 包含 `userOrgNames`、`companyIds`、`departmentNames` 等冗余聚合字段，牺牲了 DTO 的简洁性以换取前端展示的便利，减少二次查询
2. **密码传输安全**：前端加密后传输，后端再用 BCrypt 编码存储；`modifyPwdByOldPwd` 方法中区分已登录/未登录场景，未登录时统一返回模糊错误信息防止账号探测
3. **删除为组织级别**：`delete/{ids}/{orgId}` 实际是将用户从指定组织中移除（`removeUserFromOrgUnit`），而非物理删除用户记录，保证用户在其他组织下的数据完整性
