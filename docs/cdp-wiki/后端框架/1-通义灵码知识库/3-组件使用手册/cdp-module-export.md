# 如何使用 CDP 通用导入导出组件

## 概述

导入导出组件（`leatop-cdp-base-export`）提供通用的 Excel 导入导出功能，支持通过配置化方式定义导入导出模板，也支持自定义数据处理逻辑。底层基于 EasyExcel 和 Jxls-POI 实现。

## 启用方式

**1. 添加 Maven 依赖：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-export</artifactId>
</dependency>
```

**2. 配置数据库表：**

组件依赖以下数据库表（通过 Flyway 自动创建或手动执行 SQL）：

- `frame_excel_export_config` — 导出配置表
- `frame_excel_import_config` — 导入配置表

**3. 添加配置：**

```yaml
cdp:
  export:
    # 导出文件存放根目录
    export-root-path: D:\export\
```

## 核心 API

### DataExportService — 导出数据处理接口

```java
public interface DataExportService {
    // 对导出数据进行自定义处理，返回处理后的数据
    List<List<Object>> exportData(List<List<Object>> finalList);
}
```

框架提供 `DefaultDataExportServiceImpl` 默认实现（不对数据做额外处理）。如需自定义导出逻辑，实现该接口并注册为 Spring Bean。

### DataInsertService — 导入数据处理接口

```java
public interface DataInsertService {
    // 将导入的数据插入数据库
    void insertData(List<Map<String, String>> dataList, String tableName);
}
```

框架提供 `DefaultDataInsertServiceImpl` 默认实现（直接插入数据库）。如需自定义导入逻辑，实现该接口并注册为 Spring Bean。

### 内置 REST 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/export/excelExportConfig/add` | POST | 添加导出配置 |
| `/export/excelExportConfig/update` | POST | 更新导出配置 |
| `/export/excelExportConfig/get/{id}` | GET | 获取导出配置 |
| `/export/excelExportConfig/delete/{ids}` | POST | 删除导出配置 |
| `/export/excelExportConfig/listPage` | POST | 分页查询导出配置 |
| `/export/excelExportConfig/export` | POST | 执行导出，下载 Excel |

## 使用示例

### 自定义导出逻辑

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

### 自定义导入逻辑

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

## 使用流程

1. 在管理界面配置导出/导入模板（选择数据表、配置列映射）
2. 前端引入导入/导出组件，绑定配置编码
3. 如需自定义逻辑，实现 `DataExportService` 或 `DataInsertService` 接口，并在前端组件的 `interfaceUrl` 参数中指定自定义接口地址

## 注意事项

> 注意：默认导入实现仅执行数据插入，不包含数据校验逻辑。生产环境建议实现自定义 `DataInsertService` 进行数据校验。

> 注意：导出文件存放在 `cdp.export.export-root-path` 配置的目录下，请确保该目录存在且应用有写权限。

> 注意：导入导出配置通过管理界面维护，配置数据存储在 `frame_excel_export_config` 和 `frame_excel_import_config` 表中。
