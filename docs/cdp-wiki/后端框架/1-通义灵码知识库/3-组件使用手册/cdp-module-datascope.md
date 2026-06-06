# 如何使用 CDP 数据权限组件

## 概述

数据权限组件（`leatop-cdp-business-datascope`）基于 RBAC 模型，实现行级和列级数据访问控制。通过 `@DataPermission` 注解或配置文件指定需要权限控制的接口，框架自动将权限规则转换为 SQL 查询条件。

## 启用方式

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

## 使用方式

### 方式一：@DataPermission 注解

```java
import com.leatop.cdp.core.permission.DataPermission;

@DataPermission  // 类级别：所有方法启用数据权限
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

标注在方法上可精确控制单个接口：

```java
@DataPermission
@GetMapping("/sensitive")
public Message<List<LogDTO>> getSensitiveData() { ... }
```

使用 `@UncheckDataPermission` 可排除特定方法：

```java
@DataPermission
@RestController
public class DemoController {

    @UncheckDataPermission  // 此方法不做数据权限检查
    @GetMapping("/public")
    public Message<String> publicData() { ... }
}
```

### 方式二：配置文件

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

## 权限规则变量

规则中支持以下动态变量，运行时自动替换：

| 变量 | 说明 |
|------|------|
| `${tenant_id}` | 当前租户 ID |
| `${user_id}` | 当前用户 ID |
| `${org_id}` | 当前组织 ID |
| `${org_ids}` | 当前组织及其子组织 |
| `*` | 所有租户（通配） |

## 配置流程

1. **超级管理员**在【资源管理】中添加"数据实体"（定义受控的数据库表）
2. **超级管理员**在【租户管理 - 分配权限】中将数据实体授权给租户
3. **租户管理员**在【角色管理 - 数据权限】中给角色配置行/列访问规则
4. 代码中使用 `@DataPermission` 注解或配置文件指定受控接口

## 注意事项

> 注意：数据权限的本质是将访问规则转换为 SQL WHERE 条件附加到查询语句上，需确保受控表在 SQL 中存在。

> 注意：行权限规则支持 EQ、IN、LIKE 等条件操作符，列权限控制字段的可见性（脱敏或隐藏）。

> 注意：`@DataPermission` 和 `@UncheckDataPermission` 可组合使用，类级别启用 + 方法级别排除。

> 注意：数据权限规则存储在 `frame_data_scope` 表中，按角色配置。
