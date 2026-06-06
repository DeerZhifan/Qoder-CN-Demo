# 启用 CDP 自定义表单引擎

## 描述

在已有 CDP 项目中启用自定义表单组件（`leatop-cdp-business-form`），提供低代码表单设计和管理能力，支持可视化设计表单、配置列表页面、与工作流集成，实现元数据驱动的动态表单。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`boot` 单体部署 或 `cloud` 微服务部署，默认 `boot`）
3. **是否需要工作流集成**（表单提交后触发审批流程，默认否）

---

## 步骤 1：添加 Maven 依赖

> 根据部署模式引入对应的 Starter。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加（二选一）：

**单体部署：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-form-boot-starter</artifactId>
</dependency>
```

**微服务部署：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-form-cloud-starter</artifactId>
</dependency>
```

## 步骤 2：初始化数据库

> 表单模块需要元数据管理表、表单设计表、列表设计表等。通过 Flyway 迁移脚本或手动执行 SQL 创建。

确保以下数据库对象已创建：

- 元数据表：表单元对象表、字段定义表
- 设计态表：表单设计表、按钮配置表、列表设计表、列表列配置表
- 辅助表：表单分类目录表、值来源配置表、数据库配置表

同时确保数据库用户具有 DDL 权限，因为运行态会根据元数据定义动态创建业务数据表。

## 步骤 3：配置表单元数据

> 通过 `FormMetaObjectBusiness` 定义表单对应的数据库表结构，通过 `FormMetaFieldBusiness` 定义表中各列的类型、校验规则、显示名称。

使用管理后台可视化界面操作，或通过 API 调用：

```java
@Autowired
private FormMetaObjectBusiness formMetaObjectBusiness;

@Autowired
private FormMetaFieldBusiness formMetaFieldBusiness;

// 创建元对象后，调用 syncById 同步数据库表结构
formMetaObjectBusiness.syncById(metaObjectId);
```

## 步骤 4：设计表单和列表

> 通过 `DesignFormBusiness` 管理表单布局，通过 `FormDesignListBusiness` 管理列表页面。支持 `generateDefaultForms()` 自动生成默认布局。

```java
@Autowired
private DesignFormBusiness designFormBusiness;

// 根据元对象自动生成默认的表单布局和列表配置
// 在此基础上通过可视化设计器微调即可
```

表单布局支持 PC 端和移动端两套配置，通过 `platformType` 参数区分（0=PC，1=移动端）。使用 `copyFormDesignConfig()` 可在多端之间复制布局。

## 步骤 5：配置工作流集成（可选）

> 如需表单提交后自动触发审批流程，通过 `FormWorkflowService` 和 `FormWorkflowController` 配置表单与工作流的绑定关系。

前提条件：项目已启用工作流组件（`leatop-cdp-business-workflow-boot-starter`）。

表单提交时自动触发审批流程，审批结果回写到表单记录状态，实现"低代码表单 + 流程审批"的完整业务闭环。

## 步骤 6：验证

启动应用，检查以下内容：

1. 控制台无表单模块相关初始化异常
2. 元数据管理 API 可正常调用（创建元对象、添加字段）
3. 调用 `syncById()` 后对应的数据库表已自动创建
4. 通过 `FormRecordBusiness.saveOrUpdate()` 可正常保存表单数据
5. 通过 `FormRecordBusiness.listRecord()` 可正常查询表单数据

---

## 完成后提醒

1. 元数据（MetaObject/MetaField）变更后必须调用 `syncById()` 同步数据库表结构
2. 运行态使用 `JdbcTemplate` 操作动态表，不要为运行态业务表创建 PO 类
3. 使用 `ParameterUtils` 工具类将前端请求参数转换为 `FilterField` 列表，不要直接构造
4. 表单配置支持通过 `exportMetaData()` / `importMetaData()` 跨环境迁移
5. 不要同时引入 `form-boot-starter` 和 `form-cloud-starter`
6. 如需工作流集成，需同时启用工作流组件
