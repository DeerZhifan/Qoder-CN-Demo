# CDP 数据同步 设计手册

> 对应使用手册：[cdp-module-syncdata.md](../3-组件使用手册/cdp-module-syncdata.md)

## 一、设计目标与背景

数据同步组件（`leatop-cdp-business-syncdata`）解决企业常见的跨系统组织机构和用户数据同步问题，支持从外部统一用户平台、新门户、国资委等系统拉取数据并写入 CDP 本地系统。

设计目标：

1. **多数据源适配** -- 通过 `DataSourceTypeEnum` 和 `UpdateService` 接口适配不同的外部系统协议。
2. **全量/增量两种同步模式** -- 全量同步重建所有数据，增量同步仅拉取指定时间戳之后的变更。
3. **异步执行与日志追踪** -- 同步任务异步执行，全程记录日志，支持断点分析。

> 设计决策：同步逻辑采用"先拉取到临时表、再匹配写入正式表"的两阶段模式，避免同步失败时污染正式数据。

## 二、整体架构

```
┌──────────────────────────────────────────────────┐
│                  Controller 层                    │
│  SyncSettingController  SyncOrgController        │
│  SyncUserController     SyncLogController        │
│  SyncSettingIgnoreController                     │
└───────────┬──────────────────────────────────────┘
            │
            v
┌──────────────────────────────────────────────────┐
│              Business Service 层                  │
│  SyncSettingBusinessServiceImpl  ← 同步配置与执行  │
│  SyncOrgBusinessServiceImpl     ← 组织数据查询    │
│  SyncUserBusinessServiceImpl    ← 用户数据查询    │
│  SyncLogBusinessServiceImpl     ← 日志查询        │
│  SyncSettingIgnoreOrgBusinessServiceImpl          │
└───────────┬──────────────────────────────────────┘
            │
            v
┌──────────────────────────────────────────────────┐
│              SyncDataHandle （异步核心）            │
│  @Async syncData()                               │
│  ├ 拉取外部组织/用户 → 写入临时表                   │
│  ├ 匹配本地数据（同名同级组织、同账号用户）           │
│  └ 写入正式表（OrgUnitBusiness / UserBusiness）    │
└───────────┬──────────────────────────────────────┘
            │
            v
┌──────────────────────────────────────────────────┐
│           UpdateService 接口                      │
│  ├ NewUpdateServiceImpl   ← 新门户适配            │
│  └ GzwUpdateServiceImpl   ← 国资委适配            │
└──────────────────────────────────────────────────┘
```

### 数据模型

| 表/实体 | 职责 |
|---------|------|
| `SyncSettingPo` | 同步配置（外部地址、AppID、Token、根组织映射、增量时间戳） |
| `SyncOrgPo` | 组织临时表，存储从外部拉取的组织数据及匹配状态 |
| `SyncUserPo` | 用户临时表，存储从外部拉取的用户数据及匹配状态 |
| `SyncLogPo` | 同步日志，记录执行类型、起止时间、结果 |
| `SyncSettingIgnoreOrgPo` | 忽略规则，排除特定组织不参与同步 |

## 三、关键类说明

| 类名 | 职责 |
|------|------|
| `SyncSettingBusinessServiceImpl` | 同步配置管理和执行入口，支持全量同步（`reRun`）、增量同步（`incRun`）、清空重建（`cleanRun`） |
| `SyncDataHandle` | 异步同步核心处理器，通过 `@Async` 在独立线程执行，协调拉取、匹配、写入全流程 |
| `UpdateService` | 外部数据拉取接口，不同的外部系统有不同实现 |
| `NewUpdateServiceImpl` | 新门户系统的数据拉取实现 |
| `GzwUpdateServiceImpl` | 国资委系统的数据拉取实现 |
| `DataSourceTypeEnum` | 数据来源类型枚举（统一用户、新门户、国资委） |
| `SyncLogType` | 同步日志类型枚举（FULL_SYNC、INCREMENT、CLEAN_TEMP） |
| `SpringAsyncConfig` | 异步线程池配置 |
| `SyncTypeJsonSerializer` | 同步类型 JSON 序列化器 |

## 四、扩展机制

1. **适配新的外部系统**：实现 `UpdateService` 接口，在 `SyncDataHandle` 中根据 `DataSourceTypeEnum` 选择对应实现，即可接入新的数据源。在 `DataSourceTypeEnum` 中添加新枚举值。
2. **自定义匹配规则**：当前匹配策略为"同名同级组织自动关联、同账号用户自动关联"，可在 `SyncDataHandle` 中修改匹配逻辑以适应不同的业务规则。
3. **定时调度**：结合 CDP 的 XXL-Job 任务调度模块，可将 `SyncSettingBusinessServiceImpl.incRun()` 注册为定时任务实现自动增量同步。
4. **忽略规则**：通过 `SyncSettingIgnoreOrgBusinessService` 管理不参与同步的组织列表，支持按远程组织和本地组织两个维度排除。
