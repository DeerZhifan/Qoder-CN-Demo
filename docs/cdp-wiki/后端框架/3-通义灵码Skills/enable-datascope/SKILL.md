# 启用 CDP 数据权限

## 描述

在已有 CDP 项目中启用数据权限组件（`leatop-cdp-business-datascope`），基于 RBAC 模型实现行级和列级数据访问控制。通过 `@DataPermission` 注解或配置文件指定受控接口，框架自动将权限规则转换为 SQL 查询条件，无需修改业务 SQL。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **部署模式**（`boot` 单体 或 `cloud` 微服务，默认 `boot`）
3. **受控接口**（需要数据权限的接口路径，如 `/demo/**`）

---

## 步骤 1：添加 Maven 依赖

> 数据权限组件依赖系统管理组件（`system`），必须同时引入。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

**单体模式（boot）：**

```xml
<!-- 数据权限组件 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-datascope-boot-starter</artifactId>
</dependency>
<!-- 系统管理组件（必须） -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-system-boot-starter</artifactId>
</dependency>
```

**微服务模式（cloud）：**

```xml
<!-- 数据权限组件 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-datascope-cloud-starter</artifactId>
</dependency>
<!-- 系统管理组件（必须） -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-business-system-cloud-starter</artifactId>
</dependency>
```

## 步骤 2：配置数据权限拦截范围

> 通过配置文件指定哪些 API 路径需要全局数据权限拦截。也可以不配置此项，改为在代码中使用 `@DataPermission` 注解精确控制。

在 `application.yaml` 中添加（可选）：

```yaml
cdp:
  extension:
    data-permission:
      include-apis:          # 需要数据权限的接口列表（Ant 风格）
        - /{受控接口路径}/**
      exclude-apis:          # 排除的接口
        - /{排除路径}/**
```

## 步骤 3：配置数据权限规则

> 数据权限规则需在系统管理界面中完成配置，不通过代码直接定义。以下为管理界面的操作流程。

配置流程：

1. **超级管理员**在【资源管理】中添加"数据实体"（定义受控的数据库表，资源类型为 `type=5`）
2. **超级管理员**在【租户管理 - 分配权限】中将数据实体授权给租户
3. **租户管理员**在【角色管理 - 数据权限】中给角色配置行/列访问规则

权限规则支持以下动态变量：

| 变量 | 说明 |
|------|------|
| `${tenant_id}` | 当前租户 ID |
| `${user_id}` | 当前用户 ID |
| `${org_id}` | 当前组织 ID |
| `${org_ids}` | 当前组织及其子组织 |
| `*` | 所有租户（通配） |

## 步骤 4：编写示例代码

> 以下示例展示两种数据权限接入方式：注解驱动和自定义 Handler。

**方式一：@DataPermission 注解（推荐）**

```java
import com.leatop.cdp.core.permission.DataPermission;
import com.leatop.cdp.core.permission.UncheckDataPermission;

@DataPermission  // 类级别：所有方法启用数据权限
@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/list")
    public Message<List<DemoDTO>> list() {
        // SQL 查询自动附加权限条件
        return Message.success(demoService.list());
    }

    @UncheckDataPermission  // 此方法不做数据权限检查
    @GetMapping("/public")
    public Message<String> publicData() {
        return Message.success("公开数据");
    }
}
```

**方式二：方法级别精确控制**

```java
@RestController
@RequestMapping("/demo")
public class DemoController {

    @DataPermission  // 仅此方法启用数据权限
    @GetMapping("/sensitive")
    public Message<List<LogDTO>> getSensitiveData() {
        return Message.success(logService.list());
    }

    @GetMapping("/open")
    public Message<String> openData() {
        return Message.success("无权限控制");
    }
}
```

**方式三：自定义 DataPermissionHandler**

```java
import com.leatop.cdp.core.permission.DataPermissionHandler;
import org.springframework.stereotype.Component;

@Component("customDataPermissionHandler")
public class CustomHandler implements DataPermissionHandler {

    @Override
    public boolean isGlobal() {
        return false;  // 仅在注解标注的方法中生效
    }

    // 实现权限条件生成逻辑
}
```

在注解中引用：

```java
@DataPermission(handlers = "customDataPermissionHandler")
@GetMapping("/custom")
public Message<List<DataDTO>> customScoped() { ... }
```

## 步骤 5：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 使用不同角色的用户登录，访问受控接口，确认返回数据范围符合角色权限配置
3. 检查 SQL 日志（P6SPY），确认查询语句中自动附加了 WHERE 权限条件
4. 访问标注 `@UncheckDataPermission` 的接口，确认不附加权限条件
5. 列权限配置的字段在返回结果中为 null

---

## 完成后提醒

1. 数据权限本质是将访问规则转换为 SQL WHERE 条件，需确保受控表在 SQL 中存在
2. `@DataPermission` 和 `@UncheckDataPermission` 可组合使用：类级别启用 + 方法级别排除
3. `@UncheckDataPermission` 仅跳过当前方法的权限检查，不影响该方法内部调用的其他方法
4. 列权限在结果集层面实现（置 null），不在 SQL SELECT 层面裁剪字段
5. 数据规则采用全表缓存（limit 1000），规则量级超过千级需调整缓存策略
6. 变量种类的扩展（`${user_id}` 等）需修改 Java 代码，当前不支持通过配置动态添加新变量类型
7. 如需自定义 SQL 改写逻辑，可通过 `@ConditionalOnMissingBean` 机制提供自定义的 `DataPermissionHandler` Bean 替换默认实现
