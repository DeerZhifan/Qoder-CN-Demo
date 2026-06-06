---
trigger: when_referenced
knowledge_source:
  - cdp-design-gen
  - cdp-module-gen
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-gen` 依赖
- 使用代码生成器管理界面或 REST 接口（`/gen/codeCreateConfig/*`）
- 编写或修改 Velocity 模板文件（`.vm`）
- 操作 `frame_code_template`、`frame_code_template_group`、`frame_code_data_source`、`frame_code_module_info`、`frame_code_template_code_create_config` 表

---

## 前置依赖

1. Maven 依赖：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-gen</artifactId>
</dependency>
```

2. 配置代码生成路径：

```yaml
cdp:
  gen:
    template-root-path: D:\codeTemplate\
    code-root-path: D:\code\
```

3. 数据库中需包含以下表（通过 Flyway 自动创建）：
   - `frame_code_template_code_create_config` -- 代码生成配置表
   - `frame_code_data_source` -- 数据源配置表
   - `frame_code_module_info` -- 模块信息表
   - `frame_code_template_group` -- 模板组表
   - `frame_code_template` -- 模板表

---

## 配置要点

- 模板采用 **模板组 -> 模板** 二级结构，一个模板组代表一套完整的代码生成方案。
- 模板内容支持两种存储来源：数据库存储（路径以 `DB:` 前缀标识）和 classpath 文件（路径以 `classpath:` 前缀标识）。
- `VelocityUtils` 封装了两个独立引擎：`stringEngine`（渲染数据库模板）和 `classPathEngine`（渲染 classpath 模板），根据模板路径前缀自动选择。
- `TemplateTypeEnum` 定义模板类型：PO、DTO、QO、MAPPER、SERVICE、CONTROLLER、FRONT_END_VUE 等，每种类型对应确定的输出文件路径和后缀。
- 数据库列类型通过 `SqlTypeEnum` -> `JavaTypeEnum` -> `FormTypeEnum`/`FrontWebTypeEnum` 自动推断 Java 类型和前端控件类型。
- 生成器接口受 CDP 统一认证拦截，`@TenantUnaware` 注解标记其业务服务跳过租户过滤。

---

## 代码模式

### 推荐写法

**Velocity 模板变量**

```velocity
## 可用模板变量
${params.backEndPackage}    ## 后端包名
${params.className}         ## 类名（首字母大写）
${params.functionName}      ## 功能名称
${params.requestPath}       ## 请求路径
${params.author}            ## 作者
$date                       ## 当前日期
${params.columns}           ## 表字段列表
```

**使用流程**

1. 在【数据源管理】中添加数据库连接
2. 在【模块信息】中配置包名、模块名
3. 在【模板组管理】中创建或导入模板组
4. 在【代码生成配置】中选择表、配置字段属性、选择模板组和模块
5. 预览或下载生成的代码（ZIP 包）

**自定义模板扩展**

1. 在 `TemplateTypeEnum` 中新增枚举值
2. 在 `CodeCreateConfigBusinessServiceImpl.getFileName()` 中添加对应路径生成规则
3. 上传 `.vm` 文件到模板组，使用 Velocity 标准语法

**扩展外部数据源**

通过管理界面或 `CodeDataSourceController` 添加外部数据库连接，生成器通过 JDBC 直连读取表结构，不依赖 CDP 主数据源。

### 禁止事项

- **禁止在已有手动修改的代码上重新生成覆盖** -- 代码生成器不支持增量生成，每次全量输出，适合初始化阶段使用
- **禁止在模板中硬编码包名或类名** -- 必须使用模板变量（`${params.backEndPackage}`、`${params.className}`）
- **禁止手动创建 VelocityEngine 实例** -- 使用框架提供的 `VelocityUtils`，避免并发场景下 StringResourceLoader 全局仓库互相干扰
- **禁止忽略字段属性配置** -- 生成前必须配置每个字段的属性（是否表单、是否主键、控件类型等）
- **禁止在高并发场景下频繁触发代码生成** -- 临时文件通过定时任务延迟 5-7 秒删除，高并发下存在竞态风险
- **禁止在 `frame_code_template` 的 `template_content` 中存储超大模板** -- TEXT 字段对数据库有存储压力
