---
trigger: when_referenced
knowledge_source:
  - cdp-design-form
  - cdp-module-form
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-business-form-boot-starter` 或 `leatop-cdp-business-form-cloud-starter` 依赖
- 使用 `FormMetaObjectBusiness`、`FormMetaFieldBusiness`、`DesignFormBusiness`、`FormRecordBusiness` 等表单接口
- 使用 `FormWorkflowService` / `FormWorkflowController` 进行表单与工作流集成
- 操作 `FormConfig` 配置类或 `JdbcTemplate` 动态 SQL 执行
- 使用 `FilterField`、`OrderField` 等运行态查询参数模型

---

## 前置依赖

1. Maven 依赖（单体/微服务二选一）：

```xml
<!-- 单体部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-form-boot-starter</artifactId>
</dependency>

<!-- 微服务部署 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-form-cloud-starter</artifactId>
</dependency>
```

2. 表单模块的数据库表需通过 Flyway 或手动创建（表单元数据表、设计配置表等）。

3. 运行态动态表由元数据定义驱动自动创建，需确保数据库用户具有 DDL 权限。

---

## 配置要点

- 表单引擎分为设计态和运行态：设计态管理元数据和布局配置，运行态基于元数据动态构建 SQL 读写业务数据。
- 运行态使用 `JdbcTemplate` 直接操作业务表（而非 MyBatis-Plus 实体映射），因为表结构由元数据动态定义。
- `FormMetaObjectBusiness` 管理元数据对象（对应数据库表），`FormMetaFieldBusiness` 管理字段（对应表列）。
- `DesignFormBusiness` 管理表单布局，支持通过 `platformType` 参数区分 PC 端（0）和移动端（1）。
- `FormRecordBusiness` 是运行态核心，通过 `FilterField` 实现动态过滤查询。
- 表单支持与工作流集成，通过 `FormWorkflowService` 实现"表单 + 审批流"闭环。

---

## 代码模式

### 推荐写法

**元数据导入导出（跨环境迁移）**

```java
@Autowired
private FormMetaObjectBusiness formMetaObjectBusiness;

// 导出元数据配置为 JSON
String json = formMetaObjectBusiness.exportMetaData(metaObjectId);

// 在目标环境导入
formMetaObjectBusiness.importMetaData(json);
```

**自动生成默认表单布局**

<!-- TODO: 补充代码示例 -->

**运行态数据查询（FilterField 动态过滤）**

<!-- TODO: 补充代码示例 -->

**表单与工作流集成**

<!-- TODO: 补充代码示例 -->

### 禁止事项

- **禁止绕过 `FormRecordBusiness` 直接操作运行态业务表** -- 运行态表结构由元数据动态定义，直接操作会跳过字段校验和数据转换逻辑
- **禁止为运行态业务表手动创建 PO 类** -- 表结构是动态的，无法预先生成实体类，必须通过 `JdbcTemplate` + 动态 SQL 操作
- **禁止修改元数据后不同步数据库表** -- 元数据（MetaObject/MetaField）变更后需调用 `syncById()` 同步更新关联的数据库表结构
- **禁止混用单体和微服务 Starter** -- 同一个应用只能引入 `form-boot-starter` 或 `form-cloud-starter` 之一
- **禁止在表单设计中硬编码 `platformType` 值** -- 使用常量（0=PC，1=移动端）区分，通过 `copyFormDesignConfig()` 在多端之间复制布局
- **禁止直接构造 `FilterField` 对象传入前端参数** -- 使用 `ParameterUtils` 工具类将前端请求参数转换为 `FilterField` 列表
