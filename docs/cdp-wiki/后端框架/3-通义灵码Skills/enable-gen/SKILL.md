# 启用 CDP 代码生成器

## 描述

在已有 CDP 项目中启用代码生成器组件（`leatop-cdp-base-gen`），根据数据库表结构和自定义 Velocity 模板，自动生成符合 CDP 分层规范的前后端代码（Controller、Service、DAO、PO、DTO、QO、Vue 页面等），降低重复编码工作量。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **模板存放目录**（模板文件根目录路径，如 `D:\codeTemplate\`）
3. **代码输出目录**（生成代码存放根目录路径，如 `D:\code\`）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-base-gen` 提供代码生成器的完整功能，包含模板管理、数据源管理、代码渲染和 ZIP 下载。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-gen</artifactId>
</dependency>
```

## 步骤 2：添加配置

> 代码生成器需要配置模板文件和生成代码的存放目录。

在 `application.yaml` 或 `application-dev.yaml` 中添加：

```yaml
cdp:
  gen:
    template-root-path: {模板存放目录}
    code-root-path: {代码输出目录}
```

## 步骤 3：确认数据库表

> 代码生成器依赖以下数据库表，通过 Flyway 自动创建或手动执行 SQL。

确认以下表存在：

| 表名 | 说明 |
|------|------|
| `frame_code_template_code_create_config` | 代码生成配置表 |
| `frame_code_data_source` | 数据源配置表 |
| `frame_code_module_info` | 模块信息表 |
| `frame_code_template_group` | 模板组表 |
| `frame_code_template` | 模板表 |

## 步骤 4：配置数据源

> 在管理界面中添加要读取表结构的数据库连接信息。

在管理界面【代码生成器 - 数据源管理】中：

1. 添加数据库连接（JDBC URL、用户名、密码）
2. 点击"测试连接"验证连通性
3. 保存数据源配置

## 步骤 5：配置模块信息

> 模块信息决定生成代码的包路径和模块归属。

在管理界面【模块信息】中配置：

- 后端包名（如 `com.leatop.cdp.example`）
- 模块名称
- 作者信息

## 步骤 6：管理模板组

> 模板采用二级结构：模板组 -> 模板。一个模板组代表一套完整的代码生成方案。

在管理界面【模板组管理】中：

1. 创建模板组或导入默认模板组
2. 框架提供默认模板：

| 模板文件 | 生成内容 |
|---------|---------|
| `demoController.java.vm` | Spring Controller |
| `demoService.java.vm` | Service 接口 |
| `demoServiceImpl.java.vm` | Service 实现类 |
| `demoDao.java.vm` | DAO 接口（MyBatis-Plus） |
| `demoDAO.xml.vm` | MyBatis XML Mapper |
| `demoPo.java.vm` | PO 实体类 |
| `demoDto.java.vm` | DTO 数据传输对象 |
| `demoQo.java.vm` | QO 查询参数对象 |
| `demoNuiVue.vue.vm` | Vue 前端页面 |

3. 自定义模板使用 Velocity 标准语法（`.vm` 扩展名），可用变量包括 `${params.className}`、`${params.columns}`、`${params.backEndPackage}` 等

## 步骤 7：生成代码

> 选择数据库表，配置字段属性，选择模板组和模块信息后即可生成代码。

1. 在【代码生成配置】中选择数据库表
2. 配置字段属性（是否表单、是否主键、是否列表、是否查询、表单控件类型）
3. 选择模板组和模块信息
4. 预览生成结果或下载 ZIP 包
5. 将生成代码集成到项目中

## 步骤 8：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 访问 `/gen/codeCreateConfig/listPage` 接口确认代码生成器功能可用
3. 生成的代码文件符合 CDP 分层规范（PO/DTO/QO 分离）

---

## 完成后提醒

1. 代码生成器不支持增量生成，每次全量输出，适合初始化阶段使用，生成后的手动修改不会被保留
2. 自定义模板使用 Apache Velocity 语法，模板文件扩展名为 `.vm`
3. 模板内容支持两种存储来源：数据库存储（`DB:` 前缀）和 classpath 文件（`classpath:` 前缀）
4. 数据库列类型通过 `SqlTypeEnum` 自动映射为 Java 类型，前端控件类型也会自动推断
5. 前端页面中复杂控件（如日期选择器）生成后可能需要手动补充控件逻辑代码
6. 配置数据源时会验证数据库连接是否可用，请确保网络可达
