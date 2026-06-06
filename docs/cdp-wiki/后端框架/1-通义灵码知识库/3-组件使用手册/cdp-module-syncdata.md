# 如何使用 CDP 数据同步组件

## 概述

数据同步组件（`leatop-cdp-business-syncdata`）提供跨系统的组织机构和用户数据同步能力，支持从外部系统（如统一用户平台、CAS）导入组织和用户数据到 CDP 系统中。

## 启用方式

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-syncdata-boot-starter</artifactId>
</dependency>
```

## 核心功能

### 同步配置

- **SyncSetting**：同步任务配置（数据源地址、同步规则、调度时间等）
- **SyncSettingIgnore**：同步忽略规则（排除特定组织或用户）

### 数据同步

- **SyncOrg**：组织机构数据同步（从外部系统拉取组织树）
- **SyncUser**：用户数据同步（从外部系统拉取用户信息）

### 同步日志

- **SyncLog**：同步执行日志，记录每次同步的执行结果和异常信息

## 同步流程

```
1. 配置同步数据源（外部系统接口地址、认证信息等）
2. 配置同步规则（全量/增量、字段映射、忽略规则）
3. 执行同步任务（手动触发或定时调度）
4. 数据匹配：外部组织/用户与本地数据自动匹配（同名同级组织、同账号用户）
5. 写入本地数据库，记录同步日志
```

## CAS 集成

数据同步组件也用于 CAS 单点登录场景，同步 CAS 中的用户数据到 CDP：

```yaml
cdp:
  cas:
    enabled: true
    servers:
      - id: 1
        server-url: http://cas-server:8080/cas
    client-prefix-url: http://localhost:28080
```

## 注意事项

> 注意：同步操作支持数据匹配（同名同级组织自动关联），避免重复创建。

> 注意：`SyncSettingIgnore` 可排除不需要同步的组织或用户，减少数据冲突。

> 注意：CAS 登录场景必须引入此组件，用于将 CAS 认证的用户信息同步到本地系统。

> 注意：同步日志记录每次执行的详细结果，便于排查同步失败原因。
