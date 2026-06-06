---
trigger: when_referenced
knowledge_source:
  - cdp-design-echo
  - cdp-module-echo
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-base-echo` 依赖
- 使用 `@Echo`、`@EchoResult` 注解
- 实现 `LoadService` 接口
- 实现 `EchoVO` 接口
- 配置 `cdp.echo` 相关参数

---

## 前置依赖

1. Maven 依赖：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-base-echo</artifactId>
</dependency>
```

2. 组件通过 `EchoAutoConfiguration` 自动配置，引入依赖即可使用。

3. 至少一个 `LoadService` 实现类注册为 Spring Bean（如字典回显、用户回显、组织回显等）。

---

## 配置要点

```yaml
cdp:
  echo:
    enabled: true                     # 是否启用
    aop-enabled: true                 # 是否启用 AOP 拦截（默认开启）
    max-depth: 3                      # 嵌套对象最大递归深度
    dict-separator: "###"             # 字典类型与编码的分隔符
    dict-item-separator: ","          # 多个字典项之间的分隔符
    guava-cache:
      enabled: false                  # 是否启用本地缓存
      maximum-size: 1000              # 缓存最大条目
      refresh-write-time: 2           # 缓存刷新间隔（分钟）
```

### @Echo 注解参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `api` | `LoadService` 实现类的 Spring Bean 名称 | 必填 |
| `ref` | 回显值写入的目标字段名，为空则写入当前字段 | `""` |
| `parentId` | 字典类型或父级 ID，用于区分不同字典 | `""` |
| `beanClass` | 强制类型转换（Feign 反序列化场景） | `Object.class` |

### 回显处理流程

```
@EchoResult 方法返回 -> AOP 拦截 -> parse（扫描 @Echo 字段，收集 ID）
  -> load（按 api 分组，调用 LoadService.findByIds 批量查询）
  -> write（将查询结果写回 DTO 对应字段）
```

### 支持的返回值类型

单个对象、`List`、`Set`、`Page`、`Message` 包装类，以及嵌套对象（深度由 `max-depth` 控制）。

---

## 代码模式

### 推荐写法

**步骤一：实现 LoadService 数据加载接口**

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
        // 从数据库查询字典值
        return result;
    }
}
```

**步骤二：在 DTO 中使用 @Echo 注解**

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

**步骤三：在方法上添加 @EchoResult 触发回显**

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
    }
}
```

**EchoVO 接口：Map 式回显（保留原始编码值）**

当 DTO 实现 `EchoVO` 接口时，回显值存入 `getEchoMap()` 返回的 Map 中，以字段名为 key，前端可同时获取编码值和显示文本。

### 禁止事项

- **禁止在 `@Echo` 的 `api` 参数中使用不存在的 Bean 名称** -- 名称不匹配会导致回显静默失败，不会抛出异常
- **禁止在业务 Service 中手动编写字典翻译逻辑** -- 应通过 `@Echo` 注解声明式回显，避免重复代码和 N+1 查询
- **禁止在列表场景逐条调用翻译方法** -- `@Echo` 已自动将同一 api 的所有 ID 合并为一次 `findByIds` 批量查询
- **禁止忽略 `max-depth` 递归深度限制** -- DTO 嵌套引用可能导致循环，默认深度 3 层是防御性设计
- **禁止在 `LoadService.findByIds()` 中执行单条查询** -- 必须实现批量查询逻辑，否则失去批量合并的优化效果
- **禁止在高性能场景下依赖 AOP 自动拦截而不评估开销** -- 可通过 `cdp.echo.aop-enabled=false` 关闭 AOP，手动调用 `EchoService.action()` 获得更精细控制
- **禁止在 Feign 远程调用场景忽略 `beanClass` 参数** -- 反序列化后 value 可能丢失原始类型，需通过 `beanClass` 指定目标类型
