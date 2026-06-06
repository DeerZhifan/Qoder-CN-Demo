# CDP 通用导入导出 设计手册

> 对应使用手册：[cdp-module-export.md](../3-组件使用手册/cdp-module-export.md)

## 一、设计目标与背景

企业应用中 Excel 导入导出是高频需求，但各业务模块往往各自实现，导致代码重复、维护成本高、导出格式不统一。`leatop-cdp-base-export` 模块的设计目标是：

1. **配置驱动**：通过数据库表（`frame_excel_export_config`、`frame_excel_import_config`）存储导入导出模板配置，业务人员可在管理界面动态维护列映射、SQL 查询、数据行数限制等，无需修改代码即可新增或调整导入导出方案。
2. **可扩展的数据处理**：通过 Strategy 接口（`DataExportService`、`DataInsertService`）将数据加工和数据入库逻辑解耦，框架提供默认实现的同时允许业务方按需替换。
3. **双引擎输出**：同时集成 EasyExcel（配置式逐行写入）和 Jxls-POI（模板式填充），覆盖"按配置导出"和"按模板导出"两种主流场景。

> 设计决策：选择将导入导出配置持久化到数据库而非静态文件，是为了支持运行期动态调整，降低每次修改导出方案时的发布成本。

## 二、整体架构

模块采用经典的三层结构，各层职责清晰：

```
Controller 层
  ExcelExportConfigController / ExcelImportConfigController
      |
Business 层（编排层）
  ExcelExportConfigBusinessService / ExcelImportConfigBusinessService
      |           |              |
Service 层    DataSourceService   Strategy 接口
  (配置CRUD)   (SQL执行/元数据)    DataExportService / DataInsertService
      |
  DAO 层 (MyBatis-Plus)
  ExcelExportConfigDao / ExcelImportConfigDao
```

- **Controller 层**：接收 HTTP 请求，负责参数校验（基于 `@Validated` 分组校验 `AddGroup`/`UpdateGroup`）。
- **Business 层**：核心编排逻辑所在，协调配置读取、SQL 数据查询、数据加工、Excel 文件生成与响应输出。
- **Service 层**：纯粹的配置 CRUD 和数据源操作，不包含业务编排。
- **Strategy 接口**：`DataExportService` 和 `DataInsertService` 作为策略扩展点，由 Business 层在运行时选择调用默认实现或自定义实现。

自动装配通过 `ExportAutoConfiguration` 完成，使用 `@AutoConfiguration` + `@ComponentScan` + `@MapperScan` 实现零配置接入。

## 三、核心设计模式

### 3.1 Strategy 模式 -- 数据处理可替换

导出和导入的数据处理逻辑通过策略接口解耦：

- **导出策略**：`DataExportService` 定义 `exportData(List<List<Object>>)` 方法。`ExcelExportConfigBusinessServiceImpl` 内部持有默认实现 `DefaultDataExportServiceImpl`（通过 `@Qualifier("defaultDataExportService")` 注入），当外部调用 `dataExport()` 时传入 `null` 则使用默认策略，否则使用调用方提供的自定义策略。
- **导入策略**：`DataInsertService` 定义 `insertData(List<Map<String,String>>, String)` 方法。`DefaultDataInsertServiceImpl` 使用 `JdbcTemplate.batchUpdate()` 配合 `BatchPreparedStatementSetter` 实现批量插入，并自动为缺少 `id` 列的数据补充 UUID 主键。

> 设计决策：策略实例通过方法参数传入而非仅靠 Spring 容器注入，这样同一个模块可以针对不同配置编码使用不同策略，灵活度更高。

### 3.2 双引擎导出 -- EasyExcel 与 Jxls-POI

`ExcelExportConfigBusinessServiceImpl.dataExport()` 根据 `type` 参数分流：

- **type=0（配置导出）**：使用 EasyExcel 的 `write().sheet().head().doWrite()` 链式 API，结合从配置中读取的列头和 SQL 查询结果逐行写入。注册了 `SqlDateConverter` 和 `SqlTimestampConverter` 处理 `java.sql.Date` 和 `java.sql.Timestamp` 类型。
- **type=1（模板导出）**：接收前端上传的 Excel 模板文件，通过 `JxlsPoiTemplateFillerBuilder` 构建 `JxlsTemplateFiller`，将 SQL 查询结果以 `Map<String, Object>` 形式填充到模板的占位符中。

### 3.3 配置化列映射

列配置以 JSON 形式存储在 `ExcelExportConfigPo.columnConfigJson` 字段中，运行时反序列化为 `List<ExcelExportColumnConfigDto>`。每个列配置项包含数据库列名、Excel 列名、Excel 列索引等信息，按索引排序后驱动导出表头生成和数据列对齐。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| `ExportAutoConfiguration` | Spring Boot 自动配置入口，扫描组件和 Mapper |
| `ExcelExportConfigBusinessServiceImpl` | 导出核心编排：配置加载、SQL 查询、策略调度、文件生成与清理 |
| `ExcelImportConfigBusinessServiceImpl` | 导入核心编排：配置加载、Excel 解析、类型校验、策略调度 |
| `DataExportService` / `DefaultDataExportServiceImpl` | 导出数据加工策略接口及默认实现（直接透传） |
| `DataInsertService` / `DefaultDataInsertServiceImpl` | 导入数据入库策略接口及默认实现（JdbcTemplate 批量插入） |
| `DataSourceServiceImpl` | 封装 JdbcTemplate 和 JDBC 元数据查询，提供 SQL 执行与表结构探测 |
| `SqlParserUtil` | 基于 JSqlParser 的 SQL 语法校验工具，防止非法 SQL 注入配置 |
| `JavaTypeEnum` | Java 类型枚举，提供 `typeValid()` 方法对导入数据按列类型做格式校验 |
| `SqlTypeEnum` | 数据库类型枚举，提供 `getJavaType()` 将 SQL 类型映射为 Java 类型 |
| `SqlDateConverter` / `SqlTimestampConverter` | EasyExcel 自定义 Converter，处理 `java.sql.Date` 和 `java.sql.Timestamp` 的 Excel 输出格式 |
| `FileUtil` | 文件工具类：时间戳文件名生成、目录清理、压缩打包、内存映射读取 |

## 五、扩展机制

1. **自定义导出数据处理**：实现 `DataExportService` 接口，在业务 Controller 中调用 `ExcelExportConfigBusinessService.dataExport()` 时传入自定义实例。适用于数据脱敏、格式转换、字典翻译等场景。
2. **自定义导入数据入库**：实现 `DataInsertService` 接口，覆盖默认的批量 INSERT 逻辑。适用于需要数据校验、去重、关联更新等复杂入库场景。
3. **导出引擎扩展**：当前 `type` 参数支持 0（配置导出）和 1（模板导出）两种模式。如需增加新的导出格式（如 CSV、PDF），可在 `ExcelExportConfigBusinessServiceImpl.dataExport()` 中扩展分支逻辑。
4. **类型校验扩展**：通过扩展 `JavaTypeEnum` 和 `SqlTypeEnum` 枚举，可支持新的数据库类型到 Java 类型的映射以及对应的导入格式校验。

## 六、模块协作（简要）

- **依赖 `leatop-cdp-common-core`**：使用 `BusException` 进行业务异常抛出，使用 `Message` 统一响应封装，使用 `@BusinessService` 注解标记业务层。
- **依赖 `leatop-cdp-common-util`**：使用 `UUIDTool`、`BeanUtil`、`JSONTool`、`CollectionUtil`、`StrUtil` 等工具类。
- **依赖 `leatop-cdp-common-data`**：使用 `Page` 分页模型、`JdbcUtils` 元数据查询工具。
- **依赖 MyBatis-Plus**：PO 继承 `Model<T>` 使用 ActiveRecord 模式，配合 `LambdaQueryWrapper` 实现类型安全查询。
- **依赖 PageHelper**：分页查询使用 `PageHelper.startPage()` + `PageInfo` 模式。

## 七、设计权衡与约束（简要）

1. **SQL 直接执行的安全风险**：导出配置中的 `sqlStr` 字段直接作为 SQL 执行，虽然通过 `SqlParserUtil` 做了语法校验，但未做权限粒度控制。生产环境应结合数据权限模块（datascope）或限制配置维护权限。
2. **同步导出的性能边界**：当前导出为同步 HTTP 响应，通过 `dataLimit` 字段限制最大行数。超大数据量场景下可能需要引入异步导出（生成文件后通知下载），当前架构预留了文件落盘路径（`exportRootPath`），具备演进基础。
3. **临时文件清理机制**：导出完成后通过 `ScheduledExecutorService` 延迟 5 秒清理临时文件。每次请求创建一个新的线程池实例，高并发场景下可能产生过多线程，建议演进为共享的调度线程池或定时扫描清理。
4. **默认导入无校验**：`DefaultDataInsertServiceImpl` 直接拼接 INSERT 语句执行批量插入，不包含业务层校验。生产环境必须实现自定义 `DataInsertService` 以保障数据质量。
