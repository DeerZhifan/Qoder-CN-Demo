# CDP 系统配置 设计手册

> 对应使用手册：[cdp-module-system-setting.md](../3-组件使用手册/cdp-module-system-setting.md)
> 通用业务模块架构模式参见：[cdp-design-business-module-pattern.md](cdp-design-business-module-pattern.md)

## 一、设计目标与背景

系统配置模块管理应用运行时的可变参数，包括界面设置（系统名称、Logo、主题色）、安全策略（密码复杂度、锁定规则）等。其设计目标：

1. **多级配置继承**：支持系统级、租户级、公司级三级配置体系，下级未配置时自动继承上级，减少重复配置工作量。
2. **动态生效**：配置修改后通过清除缓存立即生效，无需重启应用。
3. **公开接口安全**：登录页所需的系统名称、Logo 等信息通过认证白名单接口 `/public` 暴露，避免未登录用户无法渲染登录页。
4. **安全策略自初始化**：安全配置在首次访问时自动初始化为三个等级（自定义、二级、三级），确保系统开箱即用。

> 设计决策：系统设置与安全设置拆分为两个独立的 Controller 和 Business 层（`SysSettingController` / `SecuritySettingController`），是因为两者的访问权限、变更频率和使用场景差异较大。系统设置主要由管理员在设置页面操作，安全设置影响全局登录策略，需要更严格的权限控制。

## 二、整体架构

```
SysSettingController          SecuritySettingController
  |                              |
  v                              v
SysSettingBusiness            SecuritySettingBusiness
(SysSettingBusinessImpl)      (SecuritySettingBusinessImpl)
  |                              |
  +-> SysSettingService          +-> SecuritySettingService
  |   (SysSettingServiceImpl)    |   (SecuritySettingServiceImpl)
  |   +-> SysSettingDao          |   +-> SecuritySettingDao
  |   +-> CdpCacheClient         |
  +-> IUserHelper                +-> SysSettingService (清除缓存)
```

**配置加载流程**：`SysSettingBusinessImpl.getSetting()` 按 `settingType`（设备类型）查询全部配置记录，然后按优先级依次匹配：公司级 > 租户级 > 系统级。命中后返回对应配置，并通过 `parentConfig` 标记是否为继承而来的上级配置。

**缓存机制**：`SysSettingServiceImpl` 使用 `CdpCacheClient` 获取名为 `cdp::publicSetting` 的缓存实例。公开设置接口 `getPublicSetting()` 优先从缓存读取，未命中时查询数据库并回填缓存。任何配置保存操作（`save` / `saveOrUpdate`）后均调用 `clearCache()` 清除该缓存，保证下次读取获得最新值。

> 设计决策：公开设置接口使用读穿透+写后失效的缓存策略，而非字典模块的显式预热模式。这是因为公开设置在登录页每次加载时都会被访问，属于高频热点数据，自动缓存管理更为合理。

## 三、关键类说明

### SysSettingBusinessImpl

系统设置业务实现，标注 `@BusinessService`。核心方法 `getSetting()` 实现多级配置继承逻辑：接收 `visitType`（设备类型，0=PC、1=移动端）和 `companyId` 参数，查询对应设备类型的全部配置记录后按公司 > 租户 > 系统的优先级匹配。`save()` 方法在新增时通过 `IUserHelper` 获取当前组织 ID 作为 `tenantId`，实现配置的层级归属。

### SysSettingServiceImpl

持久层服务，继承 `ServiceImpl<SysSettingDao, SysSettingPo>`。关键方法 `getPublicSetting()` 负责组装登录页所需的公开配置信息，它查询系统设置后还会通过 `SecuritySettingService` 获取当前安全策略中的验证码开关（`hasVerificationCode`），合并返回给前端。

### SecuritySettingBusinessImpl

安全配置业务实现。`saveOrUpdate()` 在保存成功后调用 `SysSettingService.clearCache()` 清除公开设置缓存，因为安全策略中的验证码开关会影响公开设置的返回值。`getAllSetting()` 在检测到安全配置记录数不足时自动调用 `initSetting()` 初始化三个安全等级。

### SecuritySettingPo

安全配置持久化实体。`name` 字段存储安全等级标识（"0"=自定义、"2"=二级、"3"=三级），`toEnable` 标记是否启用（1=启用），`objectValue` 以 JSON 字符串存储完整策略配置（`SecuritySettingValuesDto` 序列化结果），包含密码复杂度、锁定策略等所有安全参数。

> 设计决策：安全策略的具体参数以 JSON 存储在单个字段中而非拆分为多列，是为了应对安全策略项的频繁扩展需求。新增策略项时只需修改 `SecuritySettingValuesDto`，无需变更数据库表结构。

## 四、扩展机制

1. **新增配置维度**：当前支持按设备类型和组织层级区分配置。如需按角色或用户组区分，可扩展 `SysSettingPo` 增加维度字段，并在 `getSetting()` 中增加匹配逻辑。
2. **安全等级扩展**：`SecuritySettingBusinessImpl.initSetting()` 中的 `safetylevelList` 定义了预设安全等级。如需新增等级（如"一级"），只需在列表中添加标识并补充对应的默认参数。
3. **缓存策略调整**：公开设置的缓存 TTL 由 `CdpCache` 的全局配置决定。如需针对性调整，可通过 `spring.cache.cache-names` 为 `cdp::publicSetting` 指定独立的过期时间。
