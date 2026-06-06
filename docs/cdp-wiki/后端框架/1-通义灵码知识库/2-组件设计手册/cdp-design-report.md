# CDP 报表引擎 设计手册

> 对应使用手册：[cdp-module-report.md](../3-组件使用手册/cdp-module-report.md)

## 一、设计目标与背景

报表组件（`leatop-cdp-business-report`）为 CDP 框架提供两种报表方案的统一接入层：

1. **内置报表（Ureport2）** -- 轻量级报表，支持数据库和 API 数据源，模板在线编辑，适合简单报表需求。
2. **永洪报表集成** -- 对接永洪 BI 平台，适合复杂分析报表场景。

> 设计决策：不自研报表渲染引擎，而是封装两种成熟方案的接入适配，通过反向代理和 Token 管理降低集成复杂度。

## 二、整体架构

```
┌──────────────────────────────────────────────────┐
│                   Controller 层                   │
│  DataReportController      YongHongReportController│
└───────────┬───────────────────────┬──────────────┘
            │                       │
            v                       v
┌───────────────────┐   ┌──────────────────────┐
│DataReportBusinessImpl│   │YongHongReportBusinessImpl│
│- 模板 CRUD         │   │- Token 管理（Redis缓存）  │
│- 数据源管理         │   │- 定时续期（28分钟轮询）   │
│- 导出               │   │- 登录/登出              │
└───────────────────┘   └──────────────────────┘
            │                       │
            v                       v
┌───────────────────┐   ┌──────────────────────┐
│ ReportFileDAO     │   │ Redis (Token Cache)  │
│ ReportDatasourceDAO│   │ 永洪 BI HTTP API     │
└───────────────────┘   └──────────────────────┘

┌──────────────────────────────────────────────────┐
│              ReportProxyFilter                    │
│  ← 反向代理 Ureport2 请求到独立部署的报表服务       │
│  ← 仅在未引入 UReportConfig 时激活                │
└──────────────────────────────────────────────────┘
```

### 双部署模式

报表模块遵循 CDP 标准的双 starter 模式：

- `report-boot-starter`：单体部署，直接引入。
- `report-cloud-starter`：微服务部署，通过 Nacos 注册。

## 三、关键类说明

| 类名 | 职责 |
|------|------|
| `DataReportBusinessImpl` | 内置报表业务实现，管理报表模板（`frame_report_tp`）和数据源（`frame_report_ds`），支持 MySQL 和 API 两种数据源类型 |
| `YongHongReportBusinessImpl` | 永洪报表集成实现，负责登录获取 Token、Redis 缓存、定时续期和 Token 校验 |
| `ReportProxyFilter` | Ureport2 反向代理过滤器，将匹配 `/ureport/*` 的请求转发到配置的报表服务地址 |
| `ReportProxyConfig` | 过滤器注册配置，通过 `@ConditionalOnMissingClass("UReportConfig")` 条件激活 |
| `DsTypeEnum` | 数据源类型枚举（MySQL、API 等） |
| `ReportDatasourcePO` / `ReportFilePO` | 数据源和报表模板的数据库实体 |
| `DataReportBusiness` / `YongHongReportBusiness` | 业务接口定义（api 模块） |

## 四、扩展机制

1. **新增数据源类型**：在 `DsTypeEnum` 中添加新类型，在 `DataReportBusinessImpl.testDatasource()` 和 `saveOrUpdateDatasource()` 中增加对应的处理逻辑。
2. **自定义代理规则**：修改 `cdp.report.urlPatterns` 和 `cdp.report.targetUrl` 配置可调整反向代理的路径匹配和目标地址，`ReportProxyFilter` 支持多目标负载均衡（基于路径 hash）。
3. **替换报表引擎**：`ReportProxyFilter` 的条件装配机制允许在引入 Ureport2 starter 时自动禁用代理，直接使用内嵌引擎。
4. **永洪报表定制**：通过 `cdp.report.yonghong.*` 配置项调整永洪 BI 的地址、账号和 session 超时时间。永洪密码支持 SM4 加密存储。
