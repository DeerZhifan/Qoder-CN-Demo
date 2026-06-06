# 如何使用 CDP 报表组件

## 概述

报表组件（`leatop-cdp-business-report`）提供两种报表方案：内置报表（基于 Ureport2）和永洪报表集成。内置报表支持多种数据源接入、丰富的报表模板和多格式导出。

## 启用方式

### 内置报表（Ureport2）

```xml
<dependency>
    <groupId>com.bstek.ureport</groupId>
    <artifactId>ureport2-springboot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-report-boot-starter</artifactId>
</dependency>
```

在启动类添加注解：

```java
@SpringBootApplication
@EnableUreport  // 开启报表
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

### 永洪报表集成

详见 `business-report-yonghong.md`。

## 核心功能

- **数据源管理（DataReport）**：支持数据库、API 接口、Spring Bean 等多种数据源
- **报表模板**：支持表格、柱状图、折线图、饼图等多种展示形式
- **导出**：支持 PDF、Excel、CSV 等格式导出
- **永洪报表集成（YongHongReport）**：对接永洪 BI 平台

## 相关数据库表

| 表名 | 说明 |
|------|------|
| `frame_report_ds` | 报表数据源管理表 |
| `frame_report_tp` | 报表模板表 |

## 注意事项

> 注意：`@EnableUreport` 注解开启内置报表引擎，不使用时不要添加该注解。

> 注意：报表数据源配置通过管理界面维护，支持动态 SQL 查询。

> 注意：永洪报表集成需要独立部署永洪 BI 服务端，CDP 通过 API 对接。
