---
trigger: when_referenced
knowledge_source:
  - cdp-design-export
  - cdp-module-export
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-export` 依赖
- 使用 `DataExportService`、`DataInsertService` 接口
- 配置 `cdp.export` 相关参数
- 操作 `frame_excel_export_config`、`frame_excel_import_config` 数据表
- 使用 EasyExcel 或 Jxls-POI 进行 Excel 导入导出

---

## 前置依赖

1. Maven 依赖：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-export</artifactId>
</dependency>
```

2. 数据库表：`frame_excel_export_config`（导出配置）、`frame_excel_import_config`（导入配置），通过 Flyway 自动创建。

3. 配置导出文件存放目录：

```yaml
cdp:
  export:
    export-root-path: D:\export\
```

---

## 配置要点

### 导出配置表（frame_excel_export_config）

通过管理界面或直接操作数据库维护导出模板，关键字段：

- `columnConfigJson`：列映射配置（JSON 格式），每个列包含数据库列名、Excel 列名、列索引
- `sqlStr`：数据查询 SQL，通过 `SqlParserUtil` 做语法校验
- `dataLimit`：最大导出行数限制

### 双引擎导出

| type 值 | 引擎 | 说明 |
|---------|------|------|
| 0 | EasyExcel | 配置导出：根据列配置和 SQL 结果逐行写入 |
| 1 | Jxls-POI | 模板导出：上传 Excel 模板，填充占位符 |

### 内置 REST 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/export/excelExportConfig/add` | POST | 添加导出配置 |
| `/export/excelExportConfig/update` | POST | 更新导出配置 |
| `/export/excelExportConfig/get/{id}` | GET | 获取导出配置 |
| `/export/excelExportConfig/delete/{ids}` | POST | 删除导出配置 |
| `/export/excelExportConfig/listPage` | POST | 分页查询导出配置 |
| `/export/excelExportConfig/export` | POST | 执行导出，下载 Excel |

---

## 代码模式

### 推荐写法

**自定义导出数据处理：**

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

**自定义导入数据处理：**

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

**使用流程：**

1. 在管理界面配置导出/导入模板（选择数据表、配置列映射）
2. 前端引入导入/导出组件，绑定配置编码
3. 如需自定义逻辑，实现 `DataExportService` 或 `DataInsertService` 接口，在前端组件的 `interfaceUrl` 参数中指定自定义接口地址

### 禁止事项

- **禁止在生产环境使用默认 `DefaultDataInsertServiceImpl`** -- 默认实现仅执行数据插入，不包含数据校验逻辑，必须实现自定义 `DataInsertService` 保障数据质量
- **禁止在导出配置的 `sqlStr` 中编写不受控的 SQL** -- 虽有 `SqlParserUtil` 语法校验，但未做权限粒度控制，应结合数据权限模块或限制配置维护权限
- **禁止忽略 `dataLimit` 导出行数限制** -- 当前为同步 HTTP 导出，不设限制可能导致超时或内存溢出
- **禁止在导出完成后不清理临时文件** -- 框架通过 `ScheduledExecutorService` 延迟 5 秒清理，高并发场景建议演进为共享调度线程池
- **禁止直接使用 EasyExcel 或 POI API 绕过框架** -- 应通过 `DataExportService` 策略接口或框架内置接口操作，保持一致性
- **禁止忽略导出目录权限** -- `cdp.export.export-root-path` 配置的目录必须存在且应用有写权限
