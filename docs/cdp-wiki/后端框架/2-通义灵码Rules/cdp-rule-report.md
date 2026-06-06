---
trigger: when_referenced
knowledge_source:
  - cdp-design-report
  - cdp-module-report
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-business-report-boot-starter` 或 `leatop-cdp-business-report-cloud-starter` 依赖
- 使用 `@EnableUreport` 注解启用内置报表引擎
- 使用 `DataReportBusiness`、`YongHongReportBusiness` 等报表接口
- 配置 `cdp.report.*` 相关属性
- 操作 `frame_report_ds`、`frame_report_tp` 数据库表

---

## 前置依赖

1. Maven 依赖（内置报表需额外引入 Ureport2）：

```xml
<!-- 报表模块 Starter（单体部署） -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-report-boot-starter</artifactId>
</dependency>

<!-- 内置报表引擎（按需引入） -->
<dependency>
    <groupId>com.bstek.ureport</groupId>
    <artifactId>ureport2-springboot-starter</artifactId>
</dependency>
```

2. 启动类需添加 `@EnableUreport` 注解（仅内置报表需要）。

3. 数据库表 `frame_report_ds`（数据源管理）和 `frame_report_tp`（报表模板）需提前创建。

---

## 配置要点

- 内置报表（Ureport2）支持数据库和 API 两种数据源类型，通过 `DsTypeEnum` 枚举区分。
- 反向代理配置：`cdp.report.urlPatterns` 定义匹配路径（默认 `/ureport/*`），`cdp.report.targetUrl` 定义转发目标地址。
- `ReportProxyFilter` 通过 `@ConditionalOnMissingClass("UReportConfig")` 条件装配：引入 Ureport2 Starter 时使用内嵌引擎，未引入时启用反向代理转发到独立部署的报表服务。
- 永洪报表配置项：`cdp.report.yonghong.url`（BI 平台地址）、`cdp.report.yonghong.username`（账号）、`cdp.report.yonghong.password`（密码，支持 SM4 加密）。
- 永洪 Token 管理：登录后 Token 缓存在 Redis，`YongHongReportBusinessImpl` 每 28 分钟自动续期。

---

## 代码模式

### 推荐写法

**启用内置报表引擎**

```java
@SpringBootApplication
@EnableUreport  // 开启报表
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

**数据源管理**

<!-- TODO: 补充代码示例 -->

**永洪报表集成**

<!-- TODO: 补充代码示例 -->

### 禁止事项

- **禁止在不使用内置报表时添加 `@EnableUreport` 注解** -- 该注解会初始化 Ureport2 引擎，不使用时不要添加，否则会引发不必要的依赖加载
- **禁止混用单体和微服务 Starter** -- 同一个应用只能引入 `report-boot-starter` 或 `report-cloud-starter` 之一
- **禁止在永洪报表配置中使用明文密码** -- 永洪密码支持 SM4 加密存储，生产环境必须使用加密密码
- **禁止直接操作 `frame_report_ds` 和 `frame_report_tp` 表** -- 通过 `DataReportBusiness` 接口管理报表模板和数据源，保证数据一致性
- **禁止手动管理永洪 Token 生命周期** -- `YongHongReportBusinessImpl` 已封装 Token 的获取、Redis 缓存和定时续期逻辑，不要自行实现
- **禁止修改 `ReportProxyFilter` 的条件装配逻辑** -- 代理过滤器通过 `@ConditionalOnMissingClass` 自动判断是使用内嵌引擎还是反向代理，不要硬编码覆盖
