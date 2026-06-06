# CDP 通用工具类模块设计手册

> 对应使用手册：[cdp-module-common-util.md](../3-组件使用手册/cdp-module-common-util.md)

## 一、设计目标与背景

`leatop-cdp-common-util` 是 CDP 框架最底层的公共模块，为上层所有模块提供无状态的工具方法。其设计目标：

1. **统一技术栈** -- 将 Hutool、Jackson、Apache Commons 等第三方库的常用功能收敛到框架层，业务代码只依赖 CDP 工具类，避免直接耦合第三方 API。当需要替换底层实现（如从 Hutool 切换到其他库）时，只需修改工具类内部，业务代码无感知。
2. **消除重复代码** -- 加密解密、JSON 序列化、字符串处理等是每个业务模块都会用到的基础能力，集中提供可减少复制粘贴和不一致性。
3. **零依赖负担** -- 全部工具类均为静态方法，不依赖 Spring 容器（`SpringELUtils` 依赖 spring-expression 但不依赖 ApplicationContext），任何 Java 代码均可直接调用。

## 二、整体架构

模块按功能域将工具类分为四个类别：

**加密安全类** -- `AesUtil`、`RSAUtil`、`Sm4CodecUtil`、`MD5Util`、`Base64Utils`、`CdpPasswordEncoder`、`SensitiveValueCodecUtils`。覆盖对称加密（AES/SM4）、非对称加密（RSA）、哈希（MD5/BCrypt）和编码（Base64）。其中 `SensitiveValueCodecUtils` 是一个门面类，通过算法前缀标识（`{sm4}`、`{aes}`）实现多算法的统一编解码接口。

**数据处理类** -- `JSONTool`、`BeanUtil`、`StrUtil`、`CollectionUtil`、`NumberUtil`、`ObjectUtil`、`StreamDistinctByKeyUtil`、`Convert2CamelCaseOrLowerCase`。其中 `JSONTool` 基于 Jackson 并预配置了 Java 8 时间类型的序列化格式；`BeanUtil` 委托 Hutool 实现属性拷贝，额外提供了反射执行方法的能力。

**ID 与标识类** -- `IdUtils`（雪花算法 + NanoId）、`UUIDTool`。提供分布式唯一 ID 生成和短 ID 生成。

**基础设施类** -- `SpringELUtils`（SpEL 表达式解析）、`IPUtils`（真实 IP 提取）、`XmlUtils`（XML 处理）、`ZipUtils`（压缩解压）、`StrPool`（字符串常量池）、`CASServiceUtil`（CAS 单点登录工具）。

> 设计决策：工具类采用"薄封装"策略 -- 对 Hutool 等库的方法做一层转发而非深度包装。这样既隔离了第三方 API，又避免了过度抽象带来的维护成本。以 `StrUtil` 为例，其 `isNotBlank()`、`split()` 等方法内部直接调用 `cn.hutool.core.util.StrUtil` 的对应方法，保持了行为一致性。

## 三、关键类说明

| 类名 | 底层依赖 | 设计要点 |
|------|---------|---------|
| `JSONTool` | Jackson | 维护全局单例 ObjectMapper，预注册 JavaTimeModule 处理 LocalDateTime 等类型，避免每次序列化都创建新实例 |
| `BeanUtil` | Hutool BeanUtil + ReflectUtil | 除属性拷贝外提供 `executeMethod()` 反射调用，供框架内部动态调用 Bean 方法 |
| `SpringELUtils` | Spring Expression | 维护全局单例 SpelExpressionParser，提供 `convertToTemplate()` 自动补全模板前后缀，被锁模块等多处引用 |
| `SensitiveValueCodecUtils` | AesUtil + Sm4CodecUtil | 统一入口，编码结果带 `{algId}` 前缀，解码时自动识别算法，支持多算法共存和平滑迁移 |
| `IdUtils` | Hutool SnowflakeId + NanoId | 雪花算法用于数据库主键，NanoId 用于短链接和临时标识，默认 16 位 URL 安全字符 |
| `StrPool` | 无 | 接口形式的字符串常量池，定义了 UTF-8、JSON Content-Type、常用分隔符等近百个常量，减少魔法字符串 |
| `StreamDistinctByKeyUtil` | JDK ConcurrentHashMap | 利用 `putIfAbsent` 原子操作实现 Stream 按字段去重的 Predicate，线程安全 |

## 四、扩展机制

模块本身不提供 SPI 或插件机制，扩展方式为：

1. **新增工具类** -- 在 `com.leatop.cdp.util` 包下添加新的静态工具类，遵循私有构造器 + 抛出 `IllegalStateException` 的防实例化模式。
2. **替换底层实现** -- 修改现有工具类的方法体即可切换底层库。由于上层模块只依赖 CDP 的工具类接口，替换对业务代码透明。
3. **JSONTool 定制** -- 通过 `JSONTool.getObjectMapper()` 获取全局 ObjectMapper 实例进行额外配置（如注册自定义序列化器），但需注意这是全局共享的单例。

> 设计决策：`Base64Utils` 放置在 `org.springframework.util` 包下而非 `com.leatop.cdp.util`，这是因为 Spring Framework 6.x 移除了该类，此处作为兼容性补丁保留，确保升级 Spring Boot 3.x 后旧代码不需要修改。
