# 启用 CDP 数据同步

## 描述

在已有 CDP 项目中启用数据同步组件（`leatop-cdp-business-syncdata`），提供跨系统的组织机构和用户数据同步能力，支持从外部系统（如统一用户平台、新门户、国资委、CAS）导入组织和用户数据到 CDP 系统中。支持全量同步、增量同步和清空重建三种模式，采用两阶段数据写入保护正式数据安全。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`boot` 单体模式 或 `cloud` 微服务模式，默认 `boot`）
3. **外部数据源类型**（统一用户、新门户、国资委、CAS，或自定义）
4. **是否需要 CAS 单点登录集成**（是/否，默认否）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-business-syncdata-boot-starter` 提供数据同步的完整功能。微服务模式使用 `cloud-starter`。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

**单体模式：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-syncdata-boot-starter</artifactId>
</dependency>
```

**微服务模式：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-syncdata-cloud-starter</artifactId>
</dependency>
```

## 步骤 2：确认启动类配置

> 数据同步组件不需要额外的 `@Enable` 注解，但需确保主启动类的 `scanBasePackages` 包含 `com.leatop.cdp`。

确认主启动类配置：

```java
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@MapperScan("com.leatop.cdp.**.dao")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 步骤 3：添加配置

> 如需 CAS 单点登录集成，添加 CAS 配置。同步配置本身通过管理界面维护。

**CAS 集成配置（可选）：**

在 `application-dev.yaml` 中添加：

```yaml
cdp:
  cas:
    enabled: true
    servers:
      - id: 1
        server-url: http://cas-server:8080/cas
    client-prefix-url: http://localhost:28080
```

## 步骤 4：配置同步数据源

> 同步配置存储在数据库中，通过管理界面维护。

在管理界面中配置同步设置：

1. **外部系统地址**：填写数据源的接口 URL
2. **认证信息**：配置 AppID、Token 等认证参数
3. **根组织映射**：将外部系统根组织映射到 CDP 本地组织
4. **忽略规则**：通过 SyncSettingIgnore 排除不需要同步的组织或用户

## 步骤 5：执行同步

> 同步支持三种模式，`SyncDataHandle` 使用 `@Async` 异步执行，不阻塞主线程。

同步模式说明：

| 模式 | 方法 | 说明 |
|------|------|------|
| 全量同步 | `reRun` | 重建所有数据 |
| 增量同步 | `incRun` | 仅拉取指定时间戳之后的变更 |
| 清空重建 | `cleanRun` | 清空临时表后重新全量同步 |

同步流程：

```
1. 配置同步数据源（外部系统接口地址、认证信息等）
2. 配置同步规则（全量/增量、忽略规则）
3. 执行同步任务（手动触发或定时调度）
4. 数据匹配：外部组织/用户与本地数据自动匹配（同名同级组织、同账号用户）
5. 写入本地数据库，记录同步日志
```

## 步骤 6：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 同步配置管理界面可正常访问
3. 执行一次全量同步，检查 `SyncLog` 日志记录
4. 确认组织和用户数据已正确写入本地系统

---

## 完成后提醒

1. 同步采用"先拉取到临时表、再匹配写入正式表"的两阶段模式，避免同步失败时污染正式数据
2. 匹配策略为同名同级组织自动关联、同账号用户自动关联，无需手动映射
3. `SyncSettingIgnore` 可排除不需要同步的组织或用户，减少数据冲突
4. 增量同步时间戳由 `SyncSettingPo` 自动维护，无需手动管理
5. 结合 XXL-Job 可将 `incRun()` 注册为定时任务实现自动增量同步
6. 适配新的外部系统需实现 `UpdateService` 接口，并在 `DataSourceTypeEnum` 中新增枚举值
7. CAS 登录场景必须引入此组件，用于将 CAS 认证的用户信息同步到本地系统
