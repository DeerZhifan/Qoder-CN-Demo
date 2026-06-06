# CDP 组织机构管理 设计手册

> 对应使用手册：[cdp-module-system-org.md](../3-组件使用手册/cdp-module-system-org.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

组织机构模块管理企业的树形组织结构，是用户归属、角色分配、数据权限的基础维度。设计目标包括：

1. 支持公司-部门-群组三级组织类型，满足集团化企业的多层级管理需求
2. 通过 `queryCode`（层级码）实现高效的树形查询，避免递归 SQL
3. 提供组织转移、复制、排序等结构调整操作，保证层级码的一致性
4. 支持多维组织结构（`OrgstrucMultiPo`），满足矩阵型组织架构

> 设计决策：采用 `queryCode` 层级码方案而非闭包表或嵌套集，以编码前缀匹配实现子树查询。层级码在组织移动时需要级联更新，但查询效率优于递归方案。

## 二、整体架构

组织模块的核心数据结构围绕 `OrgUnitPo` 和层级码展开：

```
frame_orgstruc_orgunit (组织主表)
    |--- queryCode: "001.002.003" (层级码，点分编码)
    |--- orgLevelCode (层级路径码)
    |--- parentOrgId (父节点 ID)
    |--- companyId (所属公司 ID)
    |--- orgType: 1-公司, 0-部门
    |
frame_orgstruc_multi (多维组织)
    |--- multi (维度标识)
    |--- orgId, parentOrgId, levelCode
```

`OrgUnitBusiness` 提供组织 CRUD 和树形查询的 Feign 接口，`OrgUnitService` 实现核心业务逻辑，`IOrgListener` 提供组织变更的监听扩展点。

## 三、核心设计模式

### 3.1 层级码（queryCode）设计

`OrgUnitPo.queryCode` 是组织树形查询的核心字段，采用点分层级编码格式（如 `001.002.003`）：

- 查询某节点的所有子孙节点：`WHERE queryCode LIKE 'prefix%'`
- 查询某节点的直接子节点：`WHERE parentOrgId = ?`
- 判断层级深度：按 `.` 分割 queryCode 计算段数

`OrgUnitService.getAllOrgUitByParentId()` 利用 queryCode 前缀匹配实现子树查询，返回 `OrgAllListQo` 对象。`getOrgListByParentId()` 则查询直接子节点，返回 `OrgSubTreeListDto` 列表。

> 设计决策：queryCode 的每一段使用固定位数编号，新增组织时通过 `getAddOrgSort()` 获取父节点下的最大排序号加一，确保同级组织的 queryCode 有序且唯一。

### 3.2 组织类型与层级规则

`OrgUnitPo.orgType` 区分公司(1)和部门(0)，结合 `orgTypeExt` 扩展类型和 `hasVirtual` 虚拟部门标记：

- **公司节点**：可创建子公司、部门、群组；拥有独立的角色体系
- **部门节点**：可创建子部门、群组；不能创建子公司
- **群组节点**（通过 orgTypeExt 区分）：叶子节点，不能创建子组织

每个租户有唯一的根节点（`OrgUnitPo.ROOTID`），根节点 ID 为全零字符串 `00000000000000000000000000000000`。

### 3.3 组织转移与层级码级联更新

`OrgUnitService.transferOrgUnit()` 实现组织的跨层级转移：

1. 验证目标节点的合法性（不能转移到自身子节点下）
2. 修改被转移组织的 `parentOrgId` 和 `companyId`
3. 级联更新被转移组织及所有子孙节点的 `queryCode`
4. 重新计算排序号

重载方法 `transferOrgUnit(OrgUnitPo, OrgUnitPo, Date)` 支持指定更新时间戳，供批量同步场景使用。

### 3.4 组织复制

`OrgUnitBusiness.copyOrg()` 支持将部门结构从一个公司复制到另一个公司。`OrgUnitService.queryCopyOrgs()` 先查询可复制的部门列表，再批量创建，新组织生成新的 ID 和 queryCode。

### 3.5 公司角色自动创建

`OrgUnitService.createCompanyRole()` 在创建公司时自动生成两个系统角色：

- **公司管理员**（roleType=4）：管理该公司下的用户和角色
- **全体用户**（roleType=5）：该公司下所有用户默认拥有的角色

### 3.6 多维组织

`OrgstrucMultiPo`（对应 `frame_orgstruc_multi`）支持多维组织架构，同一个组织节点可以在不同维度（`multi` 字段）下有不同的层级关系。`OrgstrucMultiService` 管理多维组织的 CRUD。

## 四、关键类说明

| 类名 | 所属模块 | 职责 |
|------|---------|------|
| `OrgUnitBusiness` | api | 组织 Feign 接口，定义组织 CRUD、树查询、转移、复制等操作 |
| `OrgUnitService` | service | 组织核心业务逻辑，继承 `IService<OrgUnitPo>` |
| `OrgstrucMultiService` | service | 多维组织关系维护 |
| `IOrgListener` | service | 组织变更监听接口，修改组织时触发 |
| `OrgUnitPo` | service | 组织实体，对应 `frame_orgstruc_orgunit` 表，含 queryCode 层级码 |
| `OrgstrucMultiPo` | service | 多维组织实体，对应 `frame_orgstruc_multi` 表 |
| `OrgUnitDto` | api | 组织 DTO，包含完整的组织属性和扩展字段 |
| `OrgSubTreeListDto` | api | 子树查询结果 DTO，含子节点列表 |
| `OrgTreeDto` | api | 组织树形展示 DTO |
| `OrgAllListQo` | api | 完整子树查询的返回对象 |
| `TransferOrgUnitDto` | api | 组织转移操作的参数 DTO |

## 五、扩展机制

1. **IOrgListener**：组织变更监听接口，实现 `onListener(OrgUnitPo newOrgUnit, OrgUnitPo oldOrgUnit)` 方法可在组织修改时执行自定义逻辑（如同步外部系统、更新缓存等）。将实现类注入 Spring 容器即可生效
2. **组织范围**：`OrgUnitPo.orgRange` 区分内部机构(0)和外部机构(1)，为供应商、合作方等外部组织提供管理能力
3. **数据同步**：`matchLocalOrgFromTemp()` 和 `matchLocalUserFromTemp()` 方法支持从外部系统同步组织和用户数据，通过 `syncId` 字段标识外部系统的唯一记录
4. **微服务内部接口**：`@ServiceScope("micro")` 标记的方法（如 `matchLocalOrgFromTemp`、`listBySyncIds`）仅供微服务间调用

## 六、模块协作（简要）

- **用户模块**：通过 `UserOrgUnitPo` 关联用户与组织，`queryUserListByDepartment()` 查询部门下的用户
- **角色模块**：`createCompanyRole()` 在创建公司时自动创建角色；`RoleBusiness.getRoleListByOrgId()` 查询组织下的角色
- **租户模块**：租户创建时创建根组织，组织树的根节点与租户一一对应
- **数据权限模块**：`DataScopeDto.DataScopeOrg` 以组织树结构展示数据权限范围

## 七、设计权衡与约束（简要）

1. **queryCode 维护成本**：层级码在组织移动时需要级联更新所有子孙节点，大规模组织树的转移操作开销较大。但查询时的前缀匹配效率远优于递归查询，属于写少读多场景下的合理取舍
2. **companyId 冗余存储**：每个组织节点都存储了 `companyId`，标识其所属公司。这是一个冗余字段（可通过 queryCode 推导），但极大简化了按公司维度的查询逻辑
3. **删除前置校验**：删除组织前需确保无子组织和关联用户，当前通过业务层校验实现。未使用数据库外键约束，以保持 ORM 层面的灵活性
