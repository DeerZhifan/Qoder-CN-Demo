# 启用 CDP 报表组件

## 描述

在已有 CDP 项目中启用报表组件（`leatop-cdp-business-report`），提供两种报表方案：内置报表（基于 Ureport2，适合简单报表）和永洪报表集成（对接永洪 BI 平台，适合复杂分析场景）。支持多种数据源、模板在线编辑和多格式导出。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`boot` 单体部署 或 `cloud` 微服务部署，默认 `boot`）
3. **报表方案**（`ureport` 内置报表 或 `yonghong` 永洪报表，默认 `ureport`）
4. **永洪 BI 地址**（仅永洪方案需要，如 `http://bi.example.com`）

---

## 步骤 1：添加 Maven 依赖

> 根据部署模式和报表方案引入对应依赖。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

**报表模块 Starter（必选，二选一）：**

```xml
<!-- 单体部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-report-boot-starter</artifactId>
</dependency>

<!-- 微服务部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-report-cloud-starter</artifactId>
</dependency>
```

**内置报表引擎（仅 Ureport2 方案需要）：**

```xml
<dependency>
    <groupId>com.bstek.ureport</groupId>
    <artifactId>ureport2-springboot-starter</artifactId>
</dependency>
```

## 步骤 2：配置启动类注解（仅 Ureport2 方案）

> 在启动类添加 `@EnableUreport` 注解开启内置报表引擎。不使用 Ureport2 时不要添加此注解。

```java
@SpringBootApplication
@EnableUreport  // 开启报表
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

## 步骤 3：初始化数据库

> 报表模块需要数据源管理表和报表模板表。通过 Flyway 迁移脚本或手动执行 SQL 创建。

确保以下数据库表已创建：

| 表名 | 说明 |
|------|------|
| `frame_report_ds` | 报表数据源管理表 |
| `frame_report_tp` | 报表模板表 |

## 步骤 4：配置 YAML

> 根据选择的报表方案添加对应配置。

**Ureport2 反向代理配置（独立部署报表服务时使用）：**

```yaml
cdp:
  report:
    urlPatterns: /ureport/*                    # 代理路径匹配
    targetUrl: http://report-server:8080       # 报表服务地址
```

> 注意：引入 `ureport2-springboot-starter` 后使用内嵌引擎，`ReportProxyFilter` 自动禁用，无需配置反向代理。

**永洪报表配置：**

```yaml
cdp:
  report:
    yonghong:
      url: http://bi.example.com               # 永洪 BI 平台地址
      username: admin                           # 登录账号
      password: ENC(加密后的密码)                # SM4 加密密码
      sessionTimeout: 1800                      # Session 超时时间（秒）
```

## 步骤 5：验证

启动应用，检查以下内容：

**Ureport2 方案：**

1. 控制台无报表模块初始化异常
2. 访问 `/ureport/designer` 可打开报表设计器页面
3. 通过 `DataReportBusiness` 可管理数据源和报表模板
4. 报表支持 PDF、Excel、CSV 格式导出

**永洪报表方案：**

1. 控制台无报表模块初始化异常
2. `YongHongReportBusiness` 可成功登录获取 Token
3. Token 已缓存到 Redis 并自动续期（28 分钟轮询）
4. 前端可通过 Token 访问永洪 BI 报表页面

---

## 完成后提醒

1. `@EnableUreport` 注解仅在使用内置报表时添加，不使用时不要添加
2. 永洪报表密码生产环境必须使用 SM4 加密存储，不要使用明文
3. 通过 `DataReportBusiness` 管理数据源和模板，不要直接操作 `frame_report_ds` 和 `frame_report_tp` 表
4. 永洪 Token 由 `YongHongReportBusinessImpl` 自动管理（Redis 缓存 + 定时续期），不要手动管理
5. 新增数据源类型需在 `DsTypeEnum` 枚举中添加，并在 `DataReportBusinessImpl` 中增加处理逻辑
6. 不要同时引入 `report-boot-starter` 和 `report-cloud-starter`
