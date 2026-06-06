---
trigger: when_referenced
knowledge_source:
  - cdp-design-syncdata
  - cdp-module-syncdata
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-business-syncdata-boot-starter` 或 `leatop-cdp-business-syncdata-cloud-starter` 依赖
- 使用 `SyncSettingBusinessServiceImpl`、`SyncDataHandle` 等同步核心类
- 实现 `UpdateService` 接口对接外部系统
- 操作 `SyncSettingPo`、`SyncOrgPo`、`SyncUserPo`、`SyncLogPo` 等实体
- 涉及 CAS 单点登录用户同步场景

---

## 前置依赖

1. Maven 依赖（单体模式）：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-syncdata-boot-starter</artifactId>
</dependency>
```

微服务模式使用 `leatop-cdp-business-syncdata-cloud-starter`。

2. 需要引入系统管理组件（组织和用户写入依赖 `OrgUnitBusiness` / `UserBusiness`）。

3. 数据库中需包含同步相关的临时表和配置表（通过 Flyway 自动创建）。

---

## 配置要点

- 同步配置存储在 `SyncSettingPo` 实体对应的表中，包含：外部系统地址、AppID、Token、根组织映射、增量时间戳。
- 同步类型由 `DataSourceTypeEnum` 枚举控制，当前支持：统一用户、新门户、国资委。
- 同步模式：
  - **全量同步**（`reRun`）：重建所有数据
  - **增量同步**（`incRun`）：仅拉取指定时间戳之后的变更
  - **清空重建**（`cleanRun`）：清空临时表后重新全量同步
- `SyncDataHandle` 使用 `@Async` 异步执行，同步任务不阻塞主线程。
- 数据同步采用两阶段模式：先拉取到临时表（`SyncOrgPo`/`SyncUserPo`），再匹配写入正式表。
- 匹配策略：同名同级组织自动关联，同账号用户自动关联。
- 可通过 `SyncSettingIgnoreOrgBusinessService` 管理不参与同步的组织列表。

### CAS 集成配置

```yaml
cdp:
  cas:
    enabled: true
    servers:
      - id: 1
        server-url: http://cas-server:8080/cas
    client-prefix-url: http://localhost:28080
```

---

## 代码模式

### 推荐写法

**适配新的外部系统**

```java
// 1. 实现 UpdateService 接口
public class CustomUpdateServiceImpl implements UpdateService {

    @Override
    public List<SyncOrgPo> fetchOrgs(SyncSettingPo setting) {
        // 从外部系统拉取组织数据
    }

    @Override
    public List<SyncUserPo> fetchUsers(SyncSettingPo setting) {
        // 从外部系统拉取用户数据
    }
}

// 2. 在 DataSourceTypeEnum 中新增枚举值
// 3. 在 SyncDataHandle 中根据枚举选择对应实现
```

**结合 XXL-Job 实现定时增量同步**

<!-- TODO: 补充代码示例 -->

将 `SyncSettingBusinessServiceImpl.incRun()` 注册为 XXL-Job 定时任务，实现自动增量同步。

### 禁止事项

- **禁止直接写入正式组织/用户表绕过同步流程** -- 必须通过 `SyncDataHandle` 的两阶段模式（临时表 -> 正式表），避免同步失败时污染正式数据
- **禁止在同步过程中手动操作临时表** -- 临时表数据由 `SyncDataHandle` 统一管理
- **禁止忽略同步日志** -- `SyncLogPo` 记录执行类型、起止时间、结果，排查问题必须查看日志
- **禁止在同步任务中使用同步调用** -- `SyncDataHandle.syncData()` 必须通过 `@Async` 异步执行
- **禁止忽略忽略规则配置** -- 使用 `SyncSettingIgnoreOrgBusinessService` 排除不需要同步的组织，减少数据冲突
- **禁止在增量同步中手动管理时间戳** -- 增量时间戳由 `SyncSettingPo` 自动维护
