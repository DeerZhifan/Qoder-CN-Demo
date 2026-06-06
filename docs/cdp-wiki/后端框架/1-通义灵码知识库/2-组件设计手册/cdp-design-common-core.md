# CDP 核心公共模块设计手册

> 对应使用手册：[cdp-module-filter.md](../3-组件使用手册/cdp-module-filter.md)、[cdp-module-exception.md](../3-组件使用手册/cdp-module-exception.md)

## 一、设计目标与背景

Web 应用的每个 HTTP 请求都需要经过一系列横切处理：链路追踪、安全防护、请求体缓存、认证鉴权、数据脱敏、异常转换。如果这些逻辑分散在各业务 Controller 中，会导致大量重复代码和不一致的行为。

`leatop-cdp-common-core` 的设计目标是：

1. **构建标准化的请求处理管道**，通过过滤器链和 ControllerAdvice 实现对请求/响应的统一拦截处理，业务代码零配置即可获得链路追踪、CSRF 防护、请求体可重复读等能力。
2. **提供全局异常处理器**，将框架异常类层次精确映射为 HTTP 状态码和 Message 响应体，避免业务代码手写 try-catch。
3. **定义 SPI 接口**（IUserHelper、PermissionManage），将用户体系和权限体系解耦，使核心模块不依赖具体的认证实现。
4. **通过 ThreadLocal 管理请求级上下文**（Token、租户、数据权限），在过滤器链入口设置、出口清理，保证线程安全。

> 设计决策：本模块通过 Spring Boot 自动配置（`CdpCommonAutoConfiguration` + `@ComponentScan`）注册所有组件，业务模块引入 starter 即可自动生效，无需手动声明 Bean 或导入配置类。

## 二、整体架构

```
HTTP 请求
    │
    ▼
┌─────────────────────────────────────────────────────┐
│              Filter Chain（Servlet 层）              │
│                                                     │
│  CheckCSRFFilter (@Order=-1)                        │
│    └─ 校验 POST/PUT/DELETE 请求来源                  │
│  MDCRequestFilter (@Order=0)                        │
│    └─ 设置 TRACE_ID → MDC，清理 ThreadLocal         │
│  RepeatedlyRequestFilter (@Order=1)                 │
│    └─ 包装请求体为可重复读                            │
│                                                     │
│  ┌─ BaseRequestFilter（抽象基类）──────────────────┐ │
│  │  shouldNotFilter: 静态资源白名单 + OPTIONS 跳过  │ │
│  └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────┐
│           Spring MVC 拦截器层                        │
│  CdpSaInterceptor → 认证鉴权（SA-Token）            │
└─────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────┐
│           Controller → Service → Mapper             │
└─────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────┐
│         @RestControllerAdvice 层                     │
│                                                     │
│  GlobalExceptionHandler                             │
│    └─ 异常类型 → HTTP 状态码 + Message 映射          │
│  GlobalResponseBodyHandler                          │
│    └─ @DataMasking 数据脱敏处理                      │
└─────────────────────────────────────────────────────┘
    │
    ▼
HTTP 响应
```

模块内部包结���：

```
leatop-cdp-common-core
├── filter/          过滤器链
├── handler/         全局异常处理 + 响应处理
├── masking/         数据脱敏引擎
├── api/             SPI 接口（IUserHelper、PermissionManage）
├── holder/          ThreadLocal 上下文持有者
├── tenant/          租户隔离机制
├── permission/      数据权限机制
├── properties/      配置属性类
├── page/            通用分页查询支持
├── spring/          Spring 容器工具
├── i18n/            国际化支持
├── validate/        校验分组
├── constant/        常量
└── util/            工具类
```

## 三、核心设计模式

### 1. Chain of Responsibility（责任链） -- 过滤器链

三个过滤器通过 `@Order` 注解编排执行顺序，形成职责链：CheckCSRFFilter(-1) -> MDCRequestFilter(0) -> RepeatedlyRequestFilter(1)。每个过滤器职责单一，通过 `chain.doFilter()` 传递控制权。所有过滤器继承 `BaseRequestFilter`，共享静态资源白名单跳过逻辑。

> 设计决策：选择 Servlet Filter 而非 Spring Interceptor，是因为 CSRF 校验和请求体包装需要在 Spring DispatcherServlet 之前生效。MDCRequestFilter 的 `finally` 块负责清理 MDC、DataPermissionHolder、TenantContentHolder 三个 ThreadLocal，确保线程池复用时不会发生上下文泄漏。

参与类：`BaseRequestFilter`、`CheckCSRFFilter`、`MDCRequestFilter`、`RepeatedlyRequestFilter`。

### 2. Template Method（模板方法） -- BaseRequestFilter

`BaseRequestFilter` 继承 Spring 的 `OncePerRequestFilter`，覆写 `shouldNotFilter()` 方法实现统一的白名单过滤。子类只需实现 `doFilterInternal()` 编写业务逻辑，无需重复处理静态资源跳过、OPTIONS 预检等公共判断。白名单结果通过请求属性 `ALREADY_CHECK_FILTERED` 缓存，避免同一请求重复计算。

参与类：`BaseRequestFilter`（模板）、`CheckCSRFFilter`/`MDCRequestFilter`/`RepeatedlyRequestFilter`（具体实现）。

### 3. Strategy（策略模式） -- 数据脱敏

`MaskingStrategy` 枚举定义了 7 种脱敏策略（CHINESE_NAME、ID_CARD、PHONE、ADDRESS、EMAIL、PASSWORD、CUSTOMIZE），每种策略封装了一个 `Function<MaskingParam, String>` 脱敏函数和一个 `Function<String, Boolean>` 判定函数。`MaskingUtil` 作为上下文，通过反射读取字段上的 `@DataMaskingField` 注解获取策略，调用对应函数执行脱敏。

> 设计决策：使用枚举而非接口实现策略模式，是因为脱敏规则相对固定（姓名、手机、身份证等），枚举的穷举性保证了类型安全。CUSTOMIZE 策略通过 start/end 参数支持自定义位置遮蔽，覆盖了长尾需求。

参与类：`MaskingStrategy`、`MaskingParam`、`MaskingUtil`、`DataMaskingField`（注解）、`DataMasking`（方法级注解）。

### 4. Service Provider Interface -- IUserHelper

`IUserHelper` 定义了获取当前用户信息、租户 ID、角色类型、权限列表等方法。核心模块通过 `@Autowired(required=false)` 注入此接口，不强制要求实现存在。具体实现由 `leatop-cdp-business-system` 模块提供（基于 SA-Token）。这种 SPI 设计使核心模块与认证框架完全解耦。

参与类：`IUserHelper`（接口）、`PermissionManage`（接口）、`GlobalResponseBodyHandler`/`BaseColumnsHandler`/`CustomTenantHandler`（消费方）。

### 5. Decorator（装饰器模式） -- 请求/响应包装

`RepeatedlyRequestWrapper` 装饰 `HttpServletRequest`，将请求体缓存到字节数组使其可重复读取。`CdpResponseWrapper` 装饰 `HttpServletResponse`，通过 `BufferedServletOutputStream` 缓存响应内容以支持 API 加密场景。两者都不改变原始接口契约。

参与类：`RepeatedlyRequestWrapper`、`BufferedServletInputStream`、`CdpResponseWrapper`、`BufferedServletOutputStream`。

## 四、关键类说明

| 类名 | 包 | 职责 | 设计角色 |
|------|-----|------|---------|
| `BaseRequestFilter` | `core.filter` | 过滤器抽象基类，内置静态资源和 OPTIONS 白名单，缓存判断结果 | 模板基类 |
| `CheckCSRFFilter` | `core.filter` | CSRF 防护，校验 POST/PUT/DELETE 的 Referer/Origin 头是否在白名单内 | 责任链节点 |
| `MDCRequestFilter` | `core.filter` | 设置 SLF4J MDC 的 TRACE_ID，请求结束时清理所有 ThreadLocal 上下文 | 责任链节点 + 上下文管理 |
| `RepeatedlyRequestFilter` | `core.filter` | 将非 multipart 请求包装为可重复读，支持 API 加密场景的响应包装 | 责任链节点 + 装饰器触发 |
| `GlobalExceptionHandler` | `core.handler` | 全局异常处理器，按异常类型映射 HTTP 状态码，递归查找异常链中的 BusException | 异常映射器 |
| `GlobalResponseBodyHandler` | `core.handler` | 同时实现 ResponseBodyAdvice 和 RequestBodyAdviceAdapter，处理响应脱敏和请求反脱敏 | 响应/请求拦截器 |
| `IUserHelper` | `core.api` | 获取当前用户信息的 SPI 接口，定义 getCurrentUserInfo / getTenantId / getRoleTypes 等方法 | SPI 接口 |
| `PermissionManage` | `core.api` | 权限管理 SPI 接口，获取用户禁止权限列表、清除权限缓存 | SPI 接口 |
| `CdpTokenHolder` | `core.holder` | 基于 NamedThreadLocal 存储当前线程的 BaseUserInfo（userId / token / tenantId） | ThreadLocal 持有者 |
| `TenantContentHolder` | `core.tenant` | 管理当前线程的租户 ID 和忽略租户表名集合，支持按表粒度跳过租户过滤 | ThreadLocal 持有者 |
| `DataPermissionHolder` | `core.permission` | 管理当前线程的数据权限条件列表和已执行处理器集合 | ThreadLocal 持有者 |
| `CustomTenantHandler` | `core.tenant` | 实现 MyBatis-Plus 的 TenantLineHandler，根据 TenantContentHolder 和 IUserHelper 动态注入租户条件 | 租户拦截器 |
| `CdpExtensionProperties` | `core.properties` | 配置前缀 `cdp.extension`，管理 CSRF 白名单、加密开关、数据库 ID 映射、数据权限路径 | 配置属性 |
| `MaskingStrategy` | `core.masking` | 枚举定义 7 种脱敏策略，每种包含脱敏函数和判定函数 | 策略枚举 |
| `MaskingUtil` | `core.masking` | 脱敏执行引擎，递归处理 Message / Page / Collection / Array / POJO 中的脱敏字段 | 脱敏上下文 |
| `SpringBeanManager` | `core.spring` | 静态 BeanFactory 持有者，提供 `getBean(Class)` 方法供非 Spring 管理的类获取 Bean | 容器桥接 |
| `Condition` | `core.page` | 通用查询条件对象，支持从 Map 参数按命名规约自动解析为查询条件列表 | 查询构建器 |

## 五、扩展机制

### 1. 实现 IUserHelper 对接自定义用户体系

核心模块不绑定任何认证框架。业务项目只需实现 `IUserHelper` 接口并注册为 Spring Bean，框架中所有依赖用户信息的组件（自动填充、租户处理、脱敏权限判断）自动生效。接口提供了 `getCurrentUserInfo()` / `getTenantId()` / `getRoleTypes()` / `isTenantAdmin()` 等方法。

### 2. 自定义过滤器接入

业务模块新增过滤器时，建议继承 `BaseRequestFilter` 以复用静态资源白名单逻辑，`@Order` 值设置为大于 1。框架的三个过滤器占用 -1、0、1 三个优先级。

### 3. 扩展数据脱敏策略

`MaskingStrategy.CUSTOMIZE` 支持通过 `@DataMaskingField(strategy=CUSTOMIZE, start="3", end="7")` 指定自定义遮蔽区间。如果需要全新的脱敏类型，需要在 `MaskingStrategy` 枚举中添加新值（枚举的限制）。

### 4. 覆盖全局异常处理

`GlobalExceptionHandler` 使用 `@RestControllerAdvice` 注册，业务项目可通过声明更高优先级的 `@RestControllerAdvice`（带 `@Order`）或在同包下覆盖来定制异常处理逻辑。

### 5. 租户隔离定制

通过 `TenantProperties`（前缀 `cdp.tenant`）配置排除表、租户列名、是否忽略空租户。运行时可调用 `TenantContentHolder.setIgnoreTable("table_name")` 临时跳过某张表的租户过滤，也可使用 `@TenantUnaware` 注解在方法级别声明忽略租户。

## 六、模块协作

```
leatop-cdp-common-data
    │ 提供 Message / 异常类 / PO 基类 / CurrentUserDto
    ▼
leatop-cdp-common-core（本模块）
    │ 提供过滤器 / 异常处理 / SPI 接口 / ThreadLocal 上下文
    ▼
leatop-cdp-common-starter
    │ CdpCommonAutoConfiguration 扫描 core 包注册所有 Bean
    │ MybatisPlusConfig 注册 MybatisPlusInterceptor / 自动填充 / 数据权限拦截
    ▼
leatop-cdp-business-system（实现 IUserHelper / PermissionManage）
    │
    ▼
leatop-cdp-business-auth（SA-Token 认证集成）
```

**关键数据流：**

1. 请求进入 -> `CheckCSRFFilter` 校验来源 -> `MDCRequestFilter` 设置 TRACE_ID + 初始化 ThreadLocal -> `RepeatedlyRequestFilter` 包装请求体 -> 业务处理 -> `MDCRequestFilter.finally` 清理所有 ThreadLocal。

2. Controller 方法标注 `@DataMasking` -> `GlobalResponseBodyHandler.supports()` 通过 `IUserHelper.getRoleTypes()` 判断是否超管（超管不脱敏）-> `beforeBodyWrite()` 检查微服务内部调用（MICRO_TOKEN 头）不脱敏 -> 其他场景调用 `MaskingUtil.objectMasking()` 递归处理。

3. 请求体读取 -> `GlobalResponseBodyHandler.afterBodyRead()` 检查 `@DataMaskingField` 字段值是否为脱敏后的掩码，如果未修改则设为 null，防止脱敏数据被误写入数据库。

## 七、设计权衡与约束

### 权衡

1. **Filter vs Interceptor**：CSRF 和请求体包装选择 Servlet Filter 是为了在 DispatcherServlet 之前拦截。认证鉴权选择 Spring Interceptor（CdpSaInterceptor，位于 auth 模块），因为认证需要访问 Spring 上下文和 SA-Token。两层配合形成完整的请求处理管道。

2. **ThreadLocal 集中清理 vs 各自管理**：选择在 `MDCRequestFilter.finally` 中统一清理 MDC、DataPermissionHolder、TenantContentHolder，而非让每个设置者自行清理。优点是清理逻辑集中不易遗漏；代价是 MDCRequestFilter 隐式知道了数据权限和租户模块的 ThreadLocal，存在一定耦合。

3. **GlobalResponseBodyHandler 同时实现请求和响应拦截**：将脱敏处理（响应）和反脱敏检查（请求）放在同一个类中，是因为两者围绕同一组 `@DataMaskingField` 注解工作，逻辑高度内聚。代价是类职责略多于单一响应处理。

4. **异常递归查找（最多 10 层）**：`GlobalExceptionHandler.processUnknownException()` 会沿异常链向上查找 `BusException`，最大深度 10 层。这解决了 Spring 事务代理等场景中业务异常被包装为其他异常的问题，但递归深度限制为硬编码值。

### 已知限制

- `BaseRequestFilter` 的白名单路径是硬编码的，不支持通过配置文件追加自定义白名单。
- `CheckCSRFFilter` 仅在 `allowedOrigins` 非空时生效，如果未配置则完全不做 CSRF 校验，可能在未配置环境下遗漏安全防护。
- `CdpTokenHolder` 使用 `NamedThreadLocal`，在异步场景（`@Async`、线程池）中不会自动传递，需要业务代码手动拷贝上下文。
- `MaskingStrategy` 为枚举类型，新增脱敏策略需要修改框架源码，不支持运行时动态扩展。
- `GlobalResponseBodyHandler` 中判断微服务调用不脱敏的逻辑仅检查了 MICRO_TOKEN 头是否存在，尚未验证 Token 的有效性（源码标注了 TODO）。

### 演进方向

- 考虑将 `BaseRequestFilter` 的白名单改为可配置模式，通过 `CdpExtensionProperties` 支持追加自定义忽略路径。
- 引入 `TaskDecorator` 或 `TransmittableThreadLocal` 解决异步场景下 CdpTokenHolder / TenantContentHolder 的传递问题。
- `MaskingStrategy` 可考虑引入 SPI 机制，允许业务模块注册自定义脱敏策略，而非局限于枚举穷举。
