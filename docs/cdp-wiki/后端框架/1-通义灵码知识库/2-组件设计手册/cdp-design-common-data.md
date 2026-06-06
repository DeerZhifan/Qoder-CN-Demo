# CDP 基础数据模块设计手册

> 对应使用手册：[cdp-module-common-data.md](../3-组件使用手册/cdp-module-common-data.md)

## 一、设计目标与背景

在企业级多模块项目中，每个业务模块都需要处理以下共性问题：数据库实体的通用字段（租户、时间戳）管理、统一响应结构、标准化错误码体系、分页模型以及异常语义分层。如果各模块各自实现，会导致字段命名不一致、响应格式混乱、异常处理方式零散等问题。

`leatop-cdp-common-data` 的设计目标是：

1. **提供统一的 PO/DTO/QO 基类层次**，通过继承自动获得租户隔离、审计字段等横切能力，消除重复定义。
2. **建立标准化的响应和错误码契约**，使所有模块的 API 输出格式一致，降低前后端协作成本。
3. **按语义对异常分层**，让全局异常处理器能根据异常类型精确映射 HTTP 状态码和响应体。
4. **提供与框架无关的分页模型**，不强制绑定 MyBatis-Plus 的 IPage 接口，便于跨层传递。

> 设计决策：本模块不依赖 Spring 容器，只依赖 MyBatis-Plus 注解和 Lombok，确保可以被所有层（包括纯 Java 的 API 模块）安全引用，不会引入 Web 或数据库连接池等运行时依赖。

## 二、整体架构

```
leatop-cdp-common-data
├── po/                   PO 基类层次（与 MyBatis-Plus 对接）
│   ├── BasePo            租户 + 时间戳基类
│   ├── CommonEntity      UUID 主键基��
│   ├── CommonInfoEntity  审计字段基类（继承 CommonEntity）
│   └── CommonFlowId      工作流专用主键基类（继承 BasePo）
├── dto/                  DTO 基类与通用传输对象
│   ├── BaseDto           租户 + 时间戳 DTO 基类
│   ├── CurrentUserDto    当前登录用户完整信息
│   └── BaseUserInfo      轻量用户信息（ThreadLocal 传递用）
├── qo/                   查询参数对象
│   └── PageQo            分页查询参数基类
├── model/                分页模型
│   ├── Page<T>           框架自有分页容器
│   ���── PageUtils         分页计算工具
├── message/              统一响应与错误码
│   ├── Message<T>        统一响应包装
│   ├── ErrorCode         错误码接口
│   └── ErrorCodeEnum     框架标准错误码枚举
├── exception/            异常类层次
│   ├── BusException          业务异常
│   ├── UncheckedException    运行时异常基类
│   ├── ServiceUncheckedException  服务级异常（支持 i18n）
│   ├── UnauthorizedException 认证授权异常
│   └── ValidateException     参数校验异常
├── annotation/           元注解
│   ├── BusinessService   业务服务标记（继承 @Service）
│   ├── ServiceScope      服务暴露范围（web/micro）
│   ├── ChangeEvent       数据变更事件声明
│   └── EncryptField      字段加密标记
└── constants/            常量定义
    ├── CoreConstants     会话 Key、Token 名称等
    ├── HeaderConstant    HTTP 请求头常量
    └── Dict              业务字典（UserType、RoleType 等）
```

## 三、核心设计模式

### 1. Template Method（模板方法） -- PO 基类层次

PO 基类层次采用模板方法模式：`BasePo` 和 `CommonEntity` 分别定义不同维度的通用字段，业务子类通过继承选择所需的字段组合。配合 MyBatis-Plus 的 `@TableField(fill = FieldFill.INSERT)` 注解声明填充时机，实际的填充逻辑由 `leatop-cdp-common-starter` 中的 `BaseColumnsHandler` 执行。

> 设计决策：`BasePo` 和 `CommonEntity` 是两棵独立的继承树，而非单一链条。这是因为 `BasePo` 面向需要租户隔离但主键策略自定义的场景，`CommonEntity` 面向统一 UUID 主键但不需要租户字段的场景。`CommonFlowId` 继承 `BasePo` 并补充 UUID 主键，专为工作流表设计（使用 `id_` 作为列名以兼容 Flowable）。

参与类：`BasePo`、`CommonEntity`、`CommonInfoEntity`、`CommonFlowId`、`BaseColumnsHandler`、`MybatisColumnsHandler`。

### 2. Strategy（策略模式） -- 错误码接口

`ErrorCode` 是一个仅包含 `getCode()` / `getMsg()` 的接口，框架提供了默认实现 `ErrorCodeEnum`。业务模块可自行实现该接口定义领域错误码，然后通过 `Message.fail(ErrorCode)` 统一输出。这避免了在单一枚举中堆积所有业务错误码导致的冲突和膨胀。

参与类：`ErrorCode`���接口）、`ErrorCodeEnum`（默认实现）、`Message`（消费方）。

### 3. Marker Annotation（标记注解） -- 元注解体系

`@BusinessService`、`@ServiceScope`、`@ChangeEvent`、`@EncryptField` 采用标记注解模式，将横切关注点的元数据声明在代码中，由框架层的 AOP 拦截器或 MyBatis 拦截器在运行时读取并执行。注解本身不包含逻辑，仅携带配置信息。

参与类：`BusinessService`、`ServiceScope`、`ChangeEvent`��`EncryptField`。

## 四、关键类说明

| 类名 | 包 | 职责 | 设计角色 |
|------|-----|------|---------|
| `BasePo<T>` | `data.po` | 提供 tenantId / createGmt / updateGmt 三个自动填充字段，继承 MyBatis-Plus `Model<T>` 支持 ActiveRecord | PO 层模板基类 |
| `CommonEntity<T>` | `data.po` | 提供 UUID 主键（`@TableId(type=ASSIGN_UUID)`），覆写 `pkVal()` | PO 层独立主键基类 |
| `CommonInfoEntity<T>` | `data.po` | 在 CommonEntity 基础上增加 createdBy / createdOn / modifiedBy / modifiedOn 四个审计字段 | 审计增强基类 |
| `CommonFlowId<T>` | `data.po` | 继承 BasePo，添加 UUID 主键（列名 `id_`），兼容 Flowable 引擎表命名 | 工作流专用基类 |
| `BaseDto` | `data.dto` | 定义 tenantId / createGmt / updateGmt 的 String 类型 DTO 对应 | DTO 层模板基类 |
| `CurrentUserDto` | `data.dto` | 承载完整的当前用户上下文：账户、机构、角色、权限列表、额外扩展数据 | 用户上下文载体 |
| `BaseUserInfo` | `data.dto` | CurrentUserDto 的轻量投影，仅含 userId / userName / account / tenantId / orgId / token | ThreadLocal 传递载��� |
| `Page<T>` | `data.model` | 框架自有分页容器，包含 pageNumber / pageSize / totalCount / result，页码从 1 开始 | 分页值对象 |
| `PageQo` | `data.qo` | 分页查询参数基类，含 page / size / orderBy / searchKey，支持链式调用 | 查询参数基类 |
| `Message<T>` | `data.message` | 统一 API 响应包装，使用 @Builder 模式构建，提供 success / fail / result 系列静态工厂方法 | API 响应契约 |
| `BusException` | `data.exception` | 业务校验异常，携带 errorCode 字段（默认 500），由 GlobalExceptionHandler 映射为 HTTP 400 | 业务异常 |
| `UncheckedException` | `data.exception` | 运行时异常基类，支持 MessageFormat 参数化消息和异常链传递 | 系统异常基类 |
| `ServiceUncheckedException` | `data.exception` | 服务级异常，预留 i18n 参数（`value` 字段），由处理器映射为 HTTP 500 | 服务异常 |
| `UnauthorizedException` | `data.exception` | 认证/授权异常，接受 ErrorCode 参数，由处理器映射为 HTTP 401 | 认证异常 |

## 五、扩展机制

### 1. 自定义错误码

实现 `ErrorCode` 接口即可定义业务领域的错误码枚举，通过 `Message.fail(ErrorCode)` 输出。无需修改框架代码，也不会与 `ErrorCodeEnum` 冲突。

### 2. 自定义 PO 基类

如果业务模块需要不同于框架默认的通用字段组合（例如不需要租户字段但需要审计字段），可以直接继承 `CommonInfoEntity`。如果需要完全自定义，可以直接继承 MyBatis-Plus 的 `Model<T>`，但会失去框架自动填充支持。

### 3. 覆盖自动填充处理器

`BaseColumnsHandler` 是抽象类，定义了 `doInsertFill()` 和 `doUpdateFill()` 两个模板方法。业务项目可注册自己的 `MetaObjectHandler` Bean 来替换默认的 `MybatisColumnsHandler`（通过 `@ConditionalOnMissingBean` 实现），在模板方法中追加自定义的填充逻辑，例如填充 createdBy / modifiedBy。

### 4. 字段加密标记

在 PO 字段上标注 `@EncryptField`，`BaseColumnsHandler` 在 insert/update 时会自动调用 `ClassUtil.encryptFields()` 对标记字段进行加密。加密功能可通过 `cdp.extension.enable-encrypt=false` 全局关闭。

## 六、模块协作

```
leatop-cdp-common-data（本模块）
    ↑ 被依赖
    ├── leatop-cdp-common-core    （引用 Message、异常类、CurrentUserDto）
    ├── leatop-cdp-common-starter （BaseColumnsHandler 读取 BasePo 的 @TableField 注解执行填充）
    └── 所有 business-xxx-api 模块（PO/DTO/QO 基类继承）

数据流向：
1. 业务 PO 继承 BasePo → MyBatis-Plus INSERT 时触发 BaseColumnsHandler.insertFill()
   → 通过 IUserHelper.getTenantId() 获取当前租户 ID 写入 tenantId 字段
2. Controller 返回 Message<T> → GlobalResponseBodyHandler 处理脱敏
   → 前端收到 {code, msg, data} 标准结构
3. Service 抛出 BusException → GlobalExceptionHandler 捕获
   → 返回 Message.fail(errorCode, message) + HTTP 400
```

## 七、设计权衡与约束

### 权衡

1. **两棵 PO 继承树 vs 单一继承链**：选择两棵树（BasePo / CommonEntity）是为了避免不需要租户隔离的表被强制添加 tenant_id 字段。代价是业务开发者需要理解两种基类的适用场景。

2. **Page<T> 自建 vs 复用 MyBatis-Plus IPage**：自建 `Page<T>` 是为了让 API 层（`*-api` 模块）不依赖 MyBatis-Plus，保持接口定义的纯净。代价是需要在 Service 层做 IPage 到 Page 的转换。

3. **BusException 默认 errorCode=500 而非 400**：这是历史设计，目的是让未指定 errorCode 的业务异常在响应体 code 字段中显示 500，与 HTTP 状态码（400）形成区分。新代码建议显式传递业务错误码。

### 已知限制

- `BasePo` 的 `createGmt` / `updateGmt` 使用 `LocalDateTime`，而 `BaseDto` 中对应字段为 `String` 类型，需要业务代码自行处理转换。
- `CommonInfoEntity` 的 createdBy / modifiedBy 字段使用 `@TableField(fill = FieldFill.INSERT/UPDATE)` 声明了填充时机，但默认的 `MybatisColumnsHandler` 中 `doInsertFill()` 和 `doUpdateFill()` 均为空实现，需要业务项目覆盖才能真正填充创建人/修改人。
- `CurrentUserDto` 内嵌了 `CurrentUserOrgInfo` 和 `CurrentUserCompanyInfo` 两个静态内部类，耦合了组织架构的具体模型，不适合组织架构差异较大的项目直接使用。

### 演进方向

- 考虑统一 `BasePo` 和 `CommonEntity` 的时间戳字段名称（当前分别为 createGmt/updateGmt 和 createdOn/modifiedOn），减少概念混淆。
- `Page<T>` 可增加与 MyBatis-Plus `IPage` 的双向转换工具方法，降低 Service 层的样板代码。
