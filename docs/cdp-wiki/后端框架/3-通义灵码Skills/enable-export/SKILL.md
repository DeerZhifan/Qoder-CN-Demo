# 启用 CDP 通用导入导出组件

## 描述

在已有 CDP 项目中启用通用导入导出组件（`leatop-cdp-base-export`），支持通过配置化方式定义 Excel 导入导出模板，底层基于 EasyExcel（配置式导出）和 Jxls-POI（模板式导出）双引擎实现。支持自定义数据处理策略。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **导出文件存放目录**（如 `D:\export\`，默认 `D:\export\`）
3. **是否需要自定义导出/导入逻辑**（默认使用框架内置实现）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-base-export` 通过 `ExportAutoConfiguration` 自动配置，引入依赖即可使用。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-export</artifactId>
</dependency>
```

## 步骤 2：添加配置

> 导出文件存放在 `export-root-path` 配置的目录下，请确保该目录存在且应用有写权限。

在 `application-dev.yaml` 中添加：

```yaml
cdp:
  export:
    export-root-path: {导出文件存放目录}
```

## 步骤 3：确认数据库表

> 组件依赖以下配置表存储导入导出模板定义，通过 Flyway 自动创建。如未启用 Flyway，需手动执行对应 SQL 脚本。

| 表名 | 说明 |
|------|------|
| `frame_excel_export_config` | 导出配置表（列映射、SQL 查询、行数限制等） |
| `frame_excel_import_config` | 导入配置表 |

## 步骤 4：配置导出模板

> 通过管理界面或内置 REST 接口维护导出配置。关键字段说明：

- `columnConfigJson`：列映射配置（JSON），每个列包含数据库列名、Excel 列名、列索引
- `sqlStr`：数据查询 SQL
- `dataLimit`：最大导出行数限制
- `type`：导出类型（0=配置导出 EasyExcel，1=模板导出 Jxls-POI）

内置配置管理接口：

- 添加配置：`POST /export/excelExportConfig/add`
- 执行导出：`POST /export/excelExportConfig/export`

## 步骤 5：（可选）实现自定义导出策略

> 默认实现 `DefaultDataExportServiceImpl` 不对数据做额外处理。如需数据脱敏、格式转换、字典翻译等，实现 `DataExportService` 接口。

```java
import com.leatop.cdp.base.export.service.DataExportService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service("customExportService")
public class CustomExportService implements DataExportService {

    @Override
    public List<List<Object>> exportData(List<List<Object>> finalList) {
        // 对数据进行脱敏、格式化等自定义处理
        for (List<Object> row : finalList) {
            // 自定义逻辑
        }
        return finalList;
    }
}
```

## 步骤 6：（可选）实现自定义导入策略

> 默认实现 `DefaultDataInsertServiceImpl` 直接执行批量 INSERT，不包含数据校验。生产环境建议实现自定义 `DataInsertService`。

```java
import com.leatop.cdp.base.export.service.DataInsertService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service("customInsertService")
public class CustomInsertService implements DataInsertService {

    @Override
    public void insertData(List<Map<String, String>> dataList, String tableName) {
        // 自定义数据校验和插入逻辑
        for (Map<String, String> row : dataList) {
            // 校验 + 插入
        }
    }
}
```

## 步骤 7：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 访问 `POST /export/excelExportConfig/listPage` 确认接口可用
3. 通过管理界面或 API 添加一条导出配置，执行 `POST /export/excelExportConfig/export` 验证导出功能

---

## 完成后提醒

1. 导入导出配置通过管理界面维护，配置数据存储在 `frame_excel_export_config` 和 `frame_excel_import_config` 表中
2. 默认导入实现不包含数据校验，生产环境必须实现自定义 `DataInsertService`
3. 导出配置的 `sqlStr` 直接作为 SQL 执行，需结合数据权限模块或限制配置维护权限防止 SQL 注入风险
4. 当前导出为同步 HTTP 响应，通过 `dataLimit` 限制最大行数，超大数据量场景需考虑异步导出方案
5. 策略实例通过方法参数传入，同一模块可针对不同配置编码使用不同策略
