# 如何使用 CDP 代码生成器组件

## 概述

代码生成器组件（`leatop-cdp-base-gen`）根据数据库表结构和自定义模板文件，自动生成前后端代码（Controller、Service、DAO、PO、DTO、QO、Vue 页面等），提高业务开发效率。底层基于 Apache Velocity 模板引擎。

## 启用方式

**1. 添加 Maven 依赖：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-gen</artifactId>
</dependency>
```

**2. 配置数据库表：**

组件依赖以下数据库表（通过 Flyway 自动创建或手动执行 SQL）：

- `frame_code_template_code_create_config` — 代码生成配置表
- `frame_code_data_source` — 数据源配置表
- `frame_code_module_info` — 模块信息表
- `frame_code_template_group` — 模板组表
- `frame_code_template` — 模板表

**3. 添加配置：**

```yaml
cdp:
  gen:
    # 模板文件存放根目录
    template-root-path: D:\codeTemplate\
    # 生成代码存放根目录
    code-root-path: D:\code\
```

## 使用流程

### 1. 配置数据源

在管理界面【代码生成器 - 数据源管理】中添加要读取表结构的数据库连接信息。

### 2. 配置模块信息

在【模块信息】中配置生成代码所属模块的包名、模块名等参数。

### 3. 管理模板组

在【模板组管理】中创建或导入模板组。框架提供默认模板，包含以下文件：

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

### 4. 创建代码生成配置

1. 选择数据库表
2. 配置字段属性（是否表单、是否主键、是否列表、是否查询、表单控件类型）
3. 选择模板组
4. 选择模块信息
5. 预览或下载生成的代码

## Velocity 模板变量

自定义模板时可使用以下变量：

| 变量 | 说明 |
|------|------|
| `${params.backEndPackage}` | 后端包名 |
| `${params.className}` | 类名（首字母大写） |
| `${params.functionName}` | 功能名称 |
| `${params.requestPath}` | 请求路径 |
| `${params.author}` | 作者 |
| `$date` | 当前日期 |
| `${params.columns}` | 表字段列表 |

## 内置 REST 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/gen/codeCreateConfig/add` | POST | 添加生成配置 |
| `/gen/codeCreateConfig/update` | POST | 更新生成配置 |
| `/gen/codeCreateConfig/get/{id}` | GET | 获取生成配置 |
| `/gen/codeCreateConfig/delete/{ids}` | POST | 删除生成配置 |
| `/gen/codeCreateConfig/listPage` | POST | 分页查询配置 |
| `/gen/codeCreateConfig/getTablePage` | POST | 获取数据库表列表 |

## 注意事项

> 注意：生成的代码遵循 CDP 框架分层规范（PO/DTO/QO 分离），可直接集成到项目中。

> 注意：自定义模板使用 Apache Velocity 语法，模板文件扩展名为 `.vm`。

> 注意：配置数据源时会验证数据库连接是否可用，请确保网络可达。

> 注意：前端页面中复杂控件（如日期选择器）生成后可能需要手动补充控件逻辑代码。
