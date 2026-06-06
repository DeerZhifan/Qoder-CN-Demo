---
trigger: when_referenced
knowledge_source:
  - cdp-design-datascope
  - cdp-module-datascope
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-business-datascope-boot-starter` 或 `leatop-cdp-business-datascope-cloud-starter` 依赖
- 使用 `@DataPermission` 或 `@UncheckDataPermission` 注解
- 配置 `cdp.extension.data-permission` YAML 属性
- 实现 `DataPermissionHandler` 接口
- 操作 `frame_data_scope` 数据库表

---

## 前置依赖

1. Maven 依赖：

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

2. 数据权限规则需在系统管理界面中配置（资源管理 -> 数据实体；角色管理 -> 数据权限）。

3. 数据权限依赖 `IUserHelper` 获取当前用户信息（userId、orgId、tenantId）。

---

## 配置要点

### 注解驱动的数据权限

类级别启用，对所有方法生效：

```java
@DataPermission
@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/list")
    public Message<List<DemoDTO>> list() {
        // SQL 查询自动附加权限条件
        return Message.success(demoService.list());
    }
}
```

方法级别精确控制：

```java
@DataPermission
@GetMapping("/sensitive")
public Message<List<LogDTO>> getSensitiveData() { ... }
```

排除特定方法：

```java
@DataPermission
@RestController
public class DemoController {

    @UncheckDataPermission  // 此方法不做数据权限检查
    @GetMapping("/public")
    public Message<String> publicData() { ... }
}
```

### 配置文件控制拦截范围

```yaml
cdp:
  extension:
    data-permission:
      include-apis:          # 需要数据权限的接口列表
        - /logs/**
        - /xxl-job-admin/**
      exclude-apis:          # 排除的接口
        - /xxl-job-admin/api/**
```

### 权限规则变量

规则中支持以下动态变量，运行时由 `DataScopeBusinessImpl` 自动替换：

| 变量 | 说明 |
|------|------|
| `${tenant_id}` | 当前租户 ID |
| `${user_id}` | 当前用户 ID |
| `${org_id}` | 当前组织 ID |
| `${org_ids}` | 当前组织及其子组织 |
| `*` | 所有租户（通配） |

### 权限范围层级

行权限规则支持 EQ、IN、LIKE 等条件操作符。列权限控制字段的可见性（脱敏或隐藏），在 MyBatis 结果集层面拦截，对无权字段调用 setter 置 null。

### DataPermissionHandler 机制

- `isGlobal()` 返回 `true`：在配置的 API 路径范围内自动生效（由 `DataPermissionHandlerExecutors` MVC 拦截器驱动）。
- `isGlobal()` 返回 `false`：仅在标注了 `@DataPermission` 注解的方法中触发（由 `DataPermissionAdvisor` AOP 驱动）。

### 配置流程

1. **超级管理员**在【资源管理】中添加"数据实体"（定义受控的数据库表）
2. **超级管理员**在【租户管理 - 分配权限】中将数据实体授权给租户
3. **租户管理员**在【角色管理 - 数据权限】中给角色配置行/列访问规则
4. 代码中使用 `@DataPermission` 注解或配置文件指定受控接口

---

## 代码模式

### 推荐写法

**方式一：@DataPermission 注解（推荐）**

```java
import com.leatop.cdp.core.permission.DataPermission;

@DataPermission  // 类级别：所有方法启用数据权限
@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/list")
    public Message<List<DemoDTO>> list() {
        return Message.success(demoService.list());
    }

    @UncheckDataPermission  // 排除此方法
    @GetMapping("/public")
    public Message<String> publicData() { ... }
}
```

**方式二：配置文件（适合批量控制）**

```yaml
cdp:
  extension:
    data-permission:
      include-apis:
        - /logs/**
        - /xxl-job-admin/**
      exclude-apis:
        - /xxl-job-admin/api/**
```

**方式三：自定义 DataPermissionHandler**

```java
@Component("customDataPermissionHandler")
public class CustomHandler implements DataPermissionHandler {

    @Override
    public boolean isGlobal() {
        return false;  // 仅在注解标注的方法中生效
    }

    // 实现权限条件生成逻辑
}
```

在注解中引用自定义 Handler：

```java
@DataPermission(handlers = "customDataPermissionHandler")
@GetMapping("/custom")
public Message<List<DataDTO>> customScoped() { ... }
```

### 禁止事项

- **禁止在业务 SQL 中手动拼接权限过滤条件** -- 应通过 `@DataPermission` 注解或配置文件声明式接入，框架自动改写 SQL
- **禁止在 `@DataPermission` 标注的方法内部直接修改 `DataPermissionHolder`** -- ThreadLocal 状态由框架管理，手动修改会导致嵌套调用场景下权限条件丢失或泄漏
- **禁止忽略 `@UncheckDataPermission` 的作用域** -- 该注解仅跳过当前方法的权限检查，不影响该方法内部调用的其他方法
- **禁止在列权限场景下依赖 SQL SELECT 裁剪** -- 列权限在结果集层面置 null，敏感数据仍会从数据库传输到应用层
- **禁止在规则中使用未定义的变量** -- 仅支持 `${tenant_id}`、`${user_id}`、`${org_id}`、`${org_ids}` 和 `*`，扩展新变量需修改 Java 代码
- **禁止数据规则超过千级规模** -- 全表缓存（limit 1000）加内存过滤方式，规则量级过大需调整缓存策略
- **禁止绕过框架自行实现 SQL 改写** -- 如需自定义，应通过 `@ConditionalOnMissingBean` 机制提供自定义的 `DataPermissionHandler` Bean 替换默认实现
