# CDP 字典回显组件 设计手册

> 对应使用手册：[cdp-module-echo.md](../3-组件使用手册/cdp-module-echo.md)

## 一、设计目标与背景

在企业应用中，数据库存储的往往是编码值（字典码、外键 ID、枚举值），而前端展示需要的是可读文本（"启用"、"张三"、"研发部"）。传统做法是在每个 Service 方法中手动查询并拼装显示文本，存在以下问题：

1. **重复代码泛滥**：每个返回 DTO 的方法都需要相似的翻译逻辑。
2. **N+1 查询**：列表场景下逐条翻译导致大量数据库调用。
3. **侵入业务逻辑**：翻译代码与核心业务逻辑混在一起，降低可读性。

`leatop-cdp-base-echo` 模块的设计目标是：

1. **声明式回显**：通过 `@Echo` 注解标记需要翻译的字段，`@EchoResult` 触发 AOP 拦截，实现零侵入的自动回显。
2. **批量加载**：同一 `LoadService` 下的所有待翻译 ID 合并为一次 `findByIds()` 调用，彻底消除 N+1 问题。
3. **可插拔数据源**：通过 `LoadService` SPI 接口支持任意数据来源（字典表、用户表、组织表、远程 Feign 调用等）。
4. **可选缓存层**：内置 Guava LoadingCache，对变化不频繁的字典数据提供本地缓存加速。

## 二、整体架构

模块的处理流程分为三个阶段，对应 `EchoService.action()` 方法的三个步骤：

```
@EchoResult 方法返回
       |
       v
  EchoResultAspect (AOP @Around)
       |
       v
  EchoService.action(obj)
       |
       +-- 1. parse()：反射扫描 @Echo 字段，收集待查询的 (LoadKey -> Set<ID>) 映射
       |
       +-- 2. load()：按 LoadKey 分组，调用对应 LoadService.findByIds() 批量查询
       |
       +-- 3. write()：将查询结果通过反射写回 DTO 字段或 EchoVO.echoMap
```

> 设计决策：将 parse/load/write 三阶段分离而非在遍历字段时即查即写。这样做的核心价值是批量合并——parse 阶段遍历整个对象树收集所有待查 ID，load 阶段按 api 分组后每个 LoadService 只调用一次，极大减少远程/数据库调用次数。

## 三、核心设计模式

### 3.1 Strategy 模式：LoadService 可插拔数据源

`LoadService` 是模块的核心扩展点，采用 Strategy（策略）模式。`EchoAutoConfiguration` 通过 `Map<String, LoadService> strategyMap` 注入所有 Spring 容器中的 `LoadService` 实现，key 为 Bean 名称。`@Echo(api = "xxxServiceImpl")` 中的 `api` 值即为策略选择键。

`EchoService` 构造时将所有策略存入 `ConcurrentHashMap<String, LoadService> strategyMap`，load 阶段通过 `strategyMap.get(type.getApi())` 获取对应策略执行查询。

这一设计使得新增回显数据源只需：实现 `LoadService` 接口，注册为 Spring Bean，在 `@Echo` 注解中引用 Bean 名称。无需修改框架任何代码。

### 3.2 AOP + 注解驱动

模块通过两个注解协作实现声明式回显：

- `@Echo`（字段级）：标记需要回显的字段，声明使用哪个 LoadService、回显到哪个目标字段、字典类型等元数据。
- `@EchoResult`（方法级）：触发 `EchoResultAspect` 的 `@Around` 拦截。切面在方法执行后，将返回值交给 `EchoService.action()` 处理。

> 设计决策：AOP 切面的创建受 `@ConditionalOnProperty(prefix = "cdp.echo", name = "aop-enabled")` 控制，默认开启。在某些高性能场景下，开发者可关闭 AOP 自动拦截，转为在代码中手动调用 `EchoService.action()` 以获得更精细的控制。

### 3.3 反射 + 元数据缓存

模块大量使用反射来扫描字段注解和读写字段值。为避免反射的性能开销，设计了两层缓存：

- `ClassManager.CACHE`（`Map<String, List<Field>>`）：以类全限定名为 key，缓存该类中所有标注了 `@Echo` 注解的字段列表。首次扫描后，后续请求直接命中缓存，跳过注解扫描。扫描时自动过滤 static、final、volatile 修饰符的字段。
- `EchoService.CACHE`（`Map<String, FieldParam>`）：以 `类名###字段名` 为 key，缓存字段的 `@Echo` 注解解析结果（`FieldParam` 对象，含 `LoadKey`、字段名等）。避免重复创建 `LoadKey` 对象和注解属性提取。

> 设计决策：两层缓存均使用 `HashMap` 而非 `ConcurrentHashMap`。这是因为字段元数据在类加载后不会变化，缓存的写入只发生在首次访问时，之后全部是读操作。在实际运行中，应用启动后的前几次请求完成缓存预热，后续请求均为无锁读取。

### 3.4 Guava LoadingCache：可选的数据缓存

当 `cdp.echo.guava-cache.enabled = true` 时，`EchoService` 构建一个 `LoadingCache<CacheLoadKeys, Map<Serializable, Object>>`，由 `DefCacheLoader` 提供加载和刷新逻辑。

`CacheLoadKeys` 作为缓存键，封装了 api 名称和待查询的 ID 集合，并重写了 `equals()` 和 `hashCode()`——只有 api 相同且 ID 集合完全一致时才视为同一缓存键。`loadMap()` 方法委托给 `LoadService.findByIds()` 执行实际查询。

`DefCacheLoader` 的 `reload()` 方法通过 `ListeningExecutorService` 在独立线程池中异步刷新缓存，避免阻塞业务请求。线程池大小由 `guava-cache.refresh-thread-pool-size` 配置。

## 四、关键类说明

| 类名 | 职责 |
|------|------|
| `Echo` | 字段级注解，声明 api（LoadService Bean 名称）、ref（目标字段）、parentId（字典类型）、beanClass（强制类型转换） |
| `EchoResult` | 方法级注解，触发 AOP 回显拦截，支持 `ignoreFields` 参数排除特定字段 |
| `EchoResultAspect` | AOP 切面，`@Around` 拦截 `@EchoResult` 方法，将返回值交给 `EchoService` 处理 |
| `LoadService` | 数据加载 SPI 接口，唯一方法 `findByIds(Set<Serializable>)` 返回 ID 到显示值的映射 |
| `EchoService` | 核心引擎，执行 parse -> load -> write 三阶段流程 |
| `ClassManager` | 类元数据管理器，缓存每个类中标注 `@Echo` 的字段列表 |
| `FieldParam` | 字段参数封装，持有 `Echo` 注解、`LoadKey`、字段名、原始值、实际查询值 |
| `LoadKey` | `@Echo` 注解的 api 属性封装，作为 `typeMap` 的 key，重写 `equals`/`hashCode` 以 api 为唯一标识 |
| `CacheLoadKeys` | Guava 缓存键，封装 api + ID 集合 + LoadService 引用，提供 `loadMap()` 方法 |
| `DefCacheLoader` | Guava `CacheLoader` 实现，支持首次加载和异步刷新 |
| `EchoVO` | 回显值容器接口，实现类通过 `getEchoMap()` 提供一个 Map 用于存放回显结果 |
| `Kv` | 通用键值对对象，可用于 LoadService 返回结果的序列化传输 |
| `EchoProperties` | 配置属性类，前缀 `cdp.echo`，含 maxDepth、dictSeparator、guavaCache 子配置 |
| `EchoAutoConfiguration` | Spring Boot 自动配置入口，条件注册 `EchoService` 和 `EchoResultAspect` |

## 五、扩展机制

### 5.1 实现自定义 LoadService

这是最主要的扩展点。每个数据来源对应一个 `LoadService` 实现：

- 字典回显：根据 parentId（字典类型）+ code 查询字典表，返回 `parentId###code -> 显示文本` 的映射。
- 用户回显：根据用户 ID 查询用户表，返回 `userId -> 用户姓名`。
- 组织回显：根据组织 ID 查询组织表，返回 `orgId -> 组织名称`。
- 远程回显：通过 Feign Client 调用其他微服务获取数据，配合 `beanClass` 参数处理反序列化类型丢失。

### 5.2 EchoVO 接口：Map 式回显

当 DTO 实现 `EchoVO` 接口时，回显值不直接写入字段，而是存入 `getEchoMap()` 返回的 Map 中，以字段名为 key。这种模式适用于前端需要同时获取编码值和显示文本的场景，避免覆盖原始编码字段。当 `@Echo.refToThis() = true`（默认）且设置了 `ref` 时，会同时写入 echoMap 和 ref 字段。

### 5.3 替换 EchoService 或 EchoResultAspect

两个核心 Bean 均标注了 `@ConditionalOnMissingBean`，业务方可提供自定义实现覆盖默认行为。例如：扩展 `EchoService` 以支持异步并行加载多个 LoadService。

## 六、模块协作

### 与 CDP 业务模块的关系

`leatop-cdp-base-echo` 是基础设施模块，不依赖任何业务模块。业务模块通过实现 `LoadService` 接口向回显框架注册数据源：

- **leatop-cdp-business-system**：提供字典回显、组织回显、用户回显等 `LoadService` 实现。
- **leatop-cdp-business-file**：可提供文件 ID 到文件名/URL 的回显。

### 与 CDP 其他基础模块的关系

- **leatop-cdp-base-cache**：Guava 缓存是 echo 模块内置的本地缓存方案。如果业务对数据一致性要求更高，可在 `LoadService` 实现中自行集成 Redis 缓存（通过 `leatop-cdp-base-cache`），echo 模块的 Guava 缓存可关闭。
- **leatop-cdp-common-core**：`EchoService` 内部识别框架的 `Message` 包装类和 `Page` 分页类，自动拆包后对内部数据执行回显。

## 七、设计权衡与约束

> 设计决策：采用字段反射而非 getter/setter 方式读写值。反射可直接访问 private 字段，不依赖 JavaBean 命名规范，对 Lombok 生成的类友好。代价是绕过了编译期类型检查，字段名拼写错误只能在运行时发现。通过 `ClassManager` 的缓存机制，反射的性能开销被控制在首次访问时。

> 设计决策：`LoadKey` 仅以 `api` 作为 equals/hashCode 的判定依据。这意味着同一个 `LoadService` 实现类的所有 `@Echo` 字段的 ID 会被合并到一次 `findByIds()` 调用中。这是批量优化的关键——即使 DTO 中有多个字段使用同一个 api 但 parentId 不同，也只产生一次数据库查询。parentId 的区分在 `getEchoValue()` 阶段通过 `parentId###code` 组合键完成。

> 设计决策：`CACHE`（FieldParam 缓存）和 `ClassManager.CACHE` 使用静态 `HashMap`，生命周期与 JVM 一致。这意味着热部署（如 Spring DevTools）时不会自动清除缓存。在开发环境中修改了 `@Echo` 注解的属性后需要重启应用才能生效。这是为生产环境的性能优化做出的合理取舍。

> 设计决策：递归深度限制（`maxDepth` 默认 3）是防御性设计。DTO 之间可能存在嵌套引用（如 `OrderDTO` 包含 `UserDTO`，`UserDTO` 包含 `DepartmentDTO`），parse 和 write 阶段都会递归处理。`maxDepth` 防止循环引用导致栈溢出。框架在达到深度限制时记录 info 日志并跳出，不抛出异常。

> 设计决策：`CacheLoadKeys` 的 `equals()` 要求 api 相同且 ID 集合完全一致才命中缓存。这意味着不同请求即使查询同一个 api，只要 ID 集合不同就不会命中。这导致缓存命中率在 ID 分布离散时较低。设计上倾向于正确性优先——如果按 api 粗粒度缓存，可能返回不包含本次所需 ID 的旧结果。对于高频访问的固定 ID 集合（如字典类型列表），缓存效果显著。

> 设计决策：Feign 场景下，`LoadService` 返回的 `Map<Serializable, Object>` 中的 value 在反序列化后可能丢失原始类型（变为 `LinkedHashMap`）。`@Echo.beanClass` 参数配合 `EchoService` 中的 `JSONTool.fromJson()` 调用解决了这个问题——当检测到 value 为 Map 类型且 beanClass 不是 Object 时，自动进行二次反序列化。这是微服务场景下的必要补偿措施。
