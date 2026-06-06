# 如何使用 CDP 字典回显组件

## 概述

字典回显组件（`leatop-cdp-base-echo`）通过 `@Echo` 注解自动将 DTO 中的编码字段（如字典码、外键 ID）转换为对应的显示文本，避免在业务代码中手动查询和拼装。支持批量查询、嵌套对象、本地缓存。

## 启用方式

**1. 添加 Maven 依赖：**

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-echo</artifactId>
</dependency>
```

组件通过 `EchoAutoConfiguration` 自动配置，引入依赖即可使用。

## 核心注解

### @Echo — 标记需要回显的字段

```java
@Echo(
    api = "dictionaryEchoServiceImpl",  // LoadService 实现类的 Bean 名称
    ref = "statusText",                 // 回显结果写入的目标字段名
    parentId = "user_status"            // 字典类型（多键查找时使用）
)
private Integer status;
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `api` | `LoadService` 实现类的 Spring Bean 名称 | 必填 |
| `ref` | 回显值写入的目标字段名，为空则写入当前字段 | `""` |
| `parentId` | 字典类型或父级 ID，用于区分不同字典 | `""` |
| `beanClass` | 强制类型转换（Feign 场景） | `Object.class` |

### @EchoResult — 触发回显处理

标记在 Service/Controller 方法上，AOP 切面会对返回值自动执行回显处理。

```java
@EchoResult
public UserDTO getUser(Long id) { ... }
```

## 核心接口

### LoadService — 数据加载接口

```java
public interface LoadService {
    // 根据 ID 集合批量查询，返回 ID → 显示文本 的映射
    Map<Serializable, Object> findByIds(Set<Serializable> ids);
}
```

## 配置项

```yaml
cdp:
  echo:
    enabled: true                     # 是否启用
    aop-enabled: true                 # 是否启用 AOP 拦截
    max-depth: 3                      # 嵌套对象最大递归深度
    dict-separator: "###"             # 字典类型与编码的分隔符
    dict-item-separator: ","          # 多个字典项之间的分隔符
    guava-cache:
      enabled: false                  # 是否启用本地缓存
      maximum-size: 1000              # 缓存最大条目
      refresh-write-time: 2           # 缓存刷新间隔（分钟）
```

## 使用示例

### 步骤 1：实现 LoadService

```java
import com.leatop.cdp.base.echo.core.LoadService;
import org.springframework.stereotype.Service;
import java.io.Serializable;
import java.util.*;

@Service("dictionaryEchoServiceImpl")
public class DictionaryEchoService implements LoadService {

    @Override
    public Map<Serializable, Object> findByIds(Set<Serializable> ids) {
        // 批量查询字典表，返回 编码 → 显示文本
        Map<Serializable, Object> result = new HashMap<>();
        // 示例：从数据库查询字典值
        result.put("1", "启用");
        result.put("0", "禁用");
        result.put("boy", "男");
        result.put("girl", "女");
        return result;
    }
}
```

### 步骤 2：在 DTO 中使用 @Echo

```java
import com.leatop.cdp.base.echo.annotation.Echo;
import lombok.Data;

@Data
public class UserDTO {
    private String name;

    @Echo(api = "dictionaryEchoServiceImpl", parentId = "sex_type")
    private String sex;                // 原始值："boy"

    @Echo(api = "dictionaryEchoServiceImpl", ref = "statusText", parentId = "user_status")
    private Integer status;            // 原始值：1

    private String statusText;         // 回显后自动填充："启用"
}
```

### 步骤 3：在方法上添加 @EchoResult

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
        // 返回时 sex 字段值 "boy" → "男"，statusText 自动填充 "启用"
    }
}
```

## 处理流程

```
@EchoResult 方法返回 → AOP 拦截 → 解析（扫描 @Echo 字段，收集 ID）
  → 加载（按 api 分组，调用 LoadService.findByIds 批量查询）
  → 写入（将查询结果写回 DTO 对应字段）
```

## 支持的返回值类型

- 单个对象：`UserDTO`
- 集合：`List<UserDTO>`、`Set<UserDTO>`
- 分页：`Page<UserDTO>`
- 响应包装：`Message<UserDTO>`、`Message<List<UserDTO>>`
- 嵌套对象：自动递归处理（深度由 `max-depth` 控制）

## 注意事项

> 注意：`@Echo` 的 `api` 参数值必须是 Spring 容器中 `LoadService` 实现类的 Bean 名称，名称不匹配会导致回显失败。

> 注意：回显采用批量查询模式，同一个 `api` 的所有字段 ID 合并为一次 `findByIds` 调用，避免 N+1 查询。

> 注意：`max-depth` 控制嵌套递归深度，防止循环引用导致栈溢出，默认 3 层。

> 注意：启用 Guava 缓存后，字典数据按 `refresh-write-time` 间隔自动刷新，适合数据变化不频繁的场景。
