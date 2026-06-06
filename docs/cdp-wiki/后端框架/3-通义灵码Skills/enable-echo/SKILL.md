# 启用 CDP 字典回显组件

## 描述

在已有 CDP 项目中启用字典回显组件（`leatop-cdp-base-echo`），通过 `@Echo` 注解自动将 DTO 中的编码字段（字典码、外键 ID、枚举值）转换为对应的显示文本，避免在业务代码中手动查询和拼装。支持批量查询、嵌套对象处理、可选本地缓存。

## 输入

请向用户确认以下信息：

1. **模块名称**（当前模块的 artifactId，如 `leatop-cdp-myapp`）
2. **回显数据源类型**（需要回显的数据类型，如字典、用户、组织等）
3. **是否启用 Guava 本地缓存**（默认不启用）

---

## 步骤 1：添加 Maven 依赖

> `leatop-cdp-base-echo` 通过 `EchoAutoConfiguration` 自动配置，引入依赖即可使用。版本号由父 POM 的 BOM 管理，不需要手动指定。

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-echo</artifactId>
</dependency>
```

## 步骤 2：添加配置

> 以下为回显组件的配置项。AOP 默认开启，Guava 缓存默认关闭。

在 `application-dev.yaml` 中添加：

```yaml
cdp:
  echo:
    enabled: true
    aop-enabled: true
    max-depth: 3
    dict-separator: "###"
    dict-item-separator: ","
    guava-cache:
      enabled: false
      maximum-size: 1000
      refresh-write-time: 2
```

## 步骤 3：实现 LoadService 数据加载接口

> `LoadService` 是回显框架的核心扩展点。每个数据来源对应一个实现，Bean 名称即为 `@Echo(api=...)` 的引用键。

```java
import com.leatop.cdp.base.echo.core.LoadService;
import org.springframework.stereotype.Service;
import java.io.Serializable;
import java.util.*;

@Service("dictionaryEchoServiceImpl")
public class DictionaryEchoService implements LoadService {

    @Override
    public Map<Serializable, Object> findByIds(Set<Serializable> ids) {
        // 批量查询字典表，返回 编码 -> 显示文本
        Map<Serializable, Object> result = new HashMap<>();
        // 示例：从数据库查询字典值
        result.put("1", "启用");
        result.put("0", "禁用");
        return result;
    }
}
```

常见 LoadService 实现场景：

- **字典回显**：根据 parentId（字典类型）+ code 查询字典表
- **用户回显**：根据用户 ID 查询用户表，返回用户姓名
- **组织回显**：根据组织 ID 查询组织表，返回组织名称
- **远程回显**：通过 Feign Client 调用其他微服务（需配合 `beanClass` 参数）

## 步骤 4：在 DTO 中使用 @Echo 注解

> `@Echo` 标记需要回显的字段，`ref` 指定回显值写入的目标字段。不设 `ref` 则回显值直接覆盖当前字段。

```java
import com.leatop.cdp.base.echo.annotation.Echo;
import lombok.Data;

@Data
public class UserDTO {
    private String name;

    @Echo(api = "dictionaryEchoServiceImpl", parentId = "sex_type")
    private String sex;                // 原始值："boy" -> 回显后："男"

    @Echo(api = "dictionaryEchoServiceImpl", ref = "statusText", parentId = "user_status")
    private Integer status;            // 原始值：1

    private String statusText;         // 回显后自动填充："启用"
}
```

## 步骤 5：在方法上添加 @EchoResult 触发回显

> `@EchoResult` 标记在 Service/Controller 方法上，AOP 切面对返回值自动执行回显处理。支持 `Message`、`Page`、`List` 等包装类型。

```java
import com.leatop.cdp.base.echo.annotation.EchoResult;
import com.leatop.cdp.data.message.Message;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @EchoResult  // AOP 自动对返回值执行回显
    @GetMapping("/{id}")
    public Message<UserDTO> getUser(@PathVariable Long id) {
        UserDTO dto = userService.getById(id);
        return Message.success(dto);
        // 返回时 sex 字段值 "boy" -> "男"，statusText 自动填充 "启用"
    }
}
```

## 步骤 6：验证

启动应用，检查以下内容：

1. 控制台无 `NoSuchBeanDefinitionException` 错误
2. 日志中出现 `EchoAutoConfiguration` 初始化信息
3. 调用带 `@EchoResult` 注解的接口，确认返回数据中编码字段已被翻译为显示文本

---

## 完成后提醒

1. `@Echo` 的 `api` 参数值必须与 `LoadService` 实现类的 Spring Bean 名称完全一致，名称不匹配会导致回显静默失败
2. 回显采用批量查询模式，同一个 `api` 的所有字段 ID 合并为一次 `findByIds` 调用，`LoadService` 实现中必须支持批量查询
3. `max-depth` 控制嵌套递归深度（默认 3 层），防止循环引用导致栈溢出
4. Guava 缓存适合数据变化不频繁的场景（如字典数据），高一致性要求的数据建议在 `LoadService` 中自行集成 Redis 缓存
5. Feign 远程调用场景下，`@Echo(beanClass = XxxDTO.class)` 解决反序列化类型丢失问题
6. 高性能场景可设置 `cdp.echo.aop-enabled=false`，手动调用 `EchoService.action()` 获得更精细控制
