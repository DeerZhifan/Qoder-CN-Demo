# CDP 业务模块通用架构模式

> 本文档描述 CDP 框架中业务模块的 5 子模块架构模式。所有 `leatop-cdp-business` 下的业务域模块（system、log、file、message、workflow 等）均遵循此模式。后续各业务模块设计手册引用本文档以避免重复说明。

---

## 一、概述

CDP 业务模块采用 **5 子模块拆分**策略，核心设计目标有三：

1. **关注点分离** -- 将接口契约、业务编排、数据访问、REST 暴露、部署适配分置于独立的 Maven 模块中，每层职责清晰、边界明确。
2. **双部署同源** -- 同一套业务逻辑既可以 Spring Boot 单体方式运行，也可以 Spring Cloud 微服务方式运行，无需修改业务代码。
3. **可替换性** -- 上层模块依赖抽象接口而非具体实现；在微服务模式下，service 层被 Feign 代理自动替换为远程调用，切换对调用方透明。

> 设计决策：将 Business 接口放在 api 而非 service 中，是为了让该接口同时充当 Feign 远程调用的契约和本地服务的编程接口，从而实现"一个接口，两种运行时绑定"。

---

## 二、模块结构与职责

以 `leatop-cdp-business-system` 为参考，标准业务模块由以下 5 个子模块构成：

```
leatop-cdp-business-xxx/
  leatop-cdp-business-xxx-api             (1) 接口契约层
  leatop-cdp-business-xxx-service         (2) 业务实现层
  leatop-cdp-business-xxx-controller      (3) REST 控制层
  leatop-cdp-business-xxx-boot-starter    (4) 单体部署启动器
  leatop-cdp-business-xxx-cloud-starter   (5) 微服务部署启动器
```

### 2.1 api -- 接口契约层

**包结构：** `business/`、`dto/`、`qo/`、`enums/`、`service/`（扩展 SPI）

**核心产物：**

- **Business 接口** -- 标注 `@FeignClient`，定义该业务域对外暴露的全部操作方法。接口方法上使用 `@PostMapping` / `@GetMapping` 声明路径，参数使用 `@RequestBody`、`@PathVariable`、`@RequestParam` 注解。以 `UserBusiness` 为例，它同时是微服务远程调用的 Feign 契约和单体模式下本地 Bean 的接口定义。
- **DTO / QO** -- DTO 用于数据传输（入参和返回），QO 用于查询条件。两者均置于 api 模块，供 controller、service 及其他业务模块共同引用。
- **枚举** -- 与该业务域绑定的枚举常量。
- **SPI 扩展接口** -- 如 `UserExtraDataService`，允许下游应用在不修改框架代码的前提下注入自定义逻辑。

> 设计决策：api 模块不包含任何 Spring Bean 实现，仅有接口和数据类，以保证它可以被任意模块安全依赖而不引入多余的运行时组件。

### 2.2 service -- 业务实现层

**包结构：** `businessImpl/`、`service/`、`dao/`、`po/`

**核心产物：**

- **BusinessImpl** -- 实现 api 层的 Business 接口，标注 `@BusinessService`（继承自 `@Service`，语义标识为业务编排层）。它负责跨 Service 的编排和事务管理，不直接操作数据库。以 `RoleBusinessImpl` 为例，它注入 `RoleService`、`RoleOperatorService`、`UserRoleService` 等完成角色的增删改查编排。
- **Service / ServiceImpl** -- 面向单表或单一聚合根的数据操作，继承 MyBatis-Plus `IService<PO>` 接口。
- **DAO (Mapper)** -- 继承 `BaseMapper<PO>`，由 MyBatis-Plus 提供基础 CRUD，复杂查询通过 XML Mapper 扩展。
- **PO** -- 与数据库表一一对应的持久化对象，通常继承 `BasePo<T>` 获取 `tenantId`、`createGmt`、`updateGmt` 等公共字段。

### 2.3 controller -- REST 控制层

Controller 注入的是 **Business 接口**而非 Service，保证与 service 实现解耦。Controller 职责限于参数校验、权限标注和返回值包装，不包含业务逻辑。以 `RoleController` 为例，其每个方法仅一行委托调用 `RoleBusiness` 对应方法。

### 2.4 boot-starter -- 单体部署启动器

自动配置类 `CdpSystemAutoConfiguration` 使用以下注解：

- `@AutoConfiguration` -- Spring Boot 3.x 自动配置入口。
- `@ComponentScan` -- 扫描当前包（含 businessImpl、service 子包），将所有 `@BusinessService`、`@Service`、`@Component` 注册为本地 Bean。
- `@MapperScan({"com.leatop.cdp.system.dao"})` -- 扫描 DAO 接口并生成 MyBatis 代理。

**依赖关系：** 同时引入 controller 和 service 两个模块。Business 接口的调用在运行时直接绑定到本地 BusinessImpl 实例。

### 2.5 cloud-starter -- 微服务部署启动器

自动配置类 `CdpSystemCloudAutoConfiguration` 的关键差异：

- `@ComponentScan(basePackages = {"com.leatop.cdp.system.controller"})` -- 仅扫描 controller 包，**不扫描** businessImpl 和 service。
- `@EnableFeignClients(basePackages = {"com.leatop.cdp.system.business"})` -- 扫描 api 层的 `@FeignClient` 接口，由 Spring Cloud OpenFeign 生成远程代理。

**依赖关系：** 引入 controller 和 api（不引入 service），并附加 `spring-cloud-starter-openfeign`。Controller 注入的 Business 接口在运行时被 Feign 代理所填充，所有方法调用转为 HTTP 远程请求。

---

## 三、分层数据流

一个标准的写请求从进入到落库，经过以下层次：

```
HTTP Request
  -> Controller (参数校验 + @Validated)
    -> Business 接口 (由 BusinessImpl 或 Feign 代理实现)
      -> BusinessImpl (业务编排 + 事务 @Transactional)
        -> Service (单表逻辑)
          -> DAO / Mapper (MyBatis-Plus 数据访问)
            -> Database
```

数据模型在各层之间的流转遵循如下规则：

- **Controller 层**接收 DTO 或 QO，返回 `Message<DTO>`。
- **BusinessImpl 层**在 DTO 与 PO 之间通过 `BeanUtil.copyProperties` 进行转换。
- **DAO 层**仅操作 PO，不感知 DTO。

> 设计决策：BusinessImpl 作为"编排层"独立于 Service 存在，目的是避免 Service 之间的循环调用，并提供统一的事务边界。

---

## 四、双部署模式设计

两个 starter 的区别可以归结为一句话：**boot-starter 本地绑定，cloud-starter 远程代理**。

| 维度 | boot-starter (单体) | cloud-starter (微服务) |
|------|---------------------|------------------------|
| 扫描范围 | controller + service + businessImpl + dao | 仅 controller |
| Business 绑定 | 本地 `@BusinessService` 实例 | Feign 远程代理 |
| 额外依赖 | mybatis-spring | spring-cloud-starter-openfeign |
| 配置注册 | `@MapperScan` 注册 DAO | `@EnableFeignClients` 注册 Feign |

应用只需在 `pom.xml` 中切换引入的 starter，无需修改任何业务代码即可完成部署模式切换。

---

## 五、Feign 接口的双重角色

api 层的 Business 接口（如 `UserBusiness`、`RoleBusiness`）标注了 `@FeignClient` 注解，具有两种运行时身份：

1. **单体模式** -- `@FeignClient` 注解被忽略（未启用 Feign），接口由 service 模块中的 `@BusinessService` 实现类提供本地 Bean。
2. **微服务模式** -- `@EnableFeignClients` 生效，Spring Cloud 为该接口生成 HTTP 代理。接口方法上的 `@PostMapping("system/role/add")` 等注解被 Feign 解析为远程调用路径。

`@FeignClient` 的 `url` 属性引用配置项（如 `${cdp.feign.system.url:}`），在单体模式下为空字符串，Feign 不生效；微服务模式下由 Nacos 服务发现或显式配置提供目标地址。

> 设计决策：将 Feign 注解直接放在 Business 接口上而非创建独立的 Feign 接口，是为了避免维护两套方法签名，降低接口同步成本。

---

## 六、数据模型约定

| 缩写 | 全称 | 所属模块 | 职责 |
|------|------|----------|------|
| PO | Persistent Object | service | 与数据库表一一对应，仅在 DAO 和 Service 层流转 |
| DTO | Data Transfer Object | api | 跨层数据传输，Controller 入参 / 返回值，Business 方法签名 |
| QO | Query Object | api | 封装查询条件，通常用于分页和列表接口 |

转换规则：

- PO -> DTO：在 BusinessImpl 层完成，使用 `BeanUtil.copyProperties` 或手动映射。
- DTO -> PO：同样在 BusinessImpl 层完成，写入前进行属性拷贝。
- QO 不直接转换为 PO，而是在 BusinessImpl 或 Service 层解析为 MyBatis-Plus `LambdaQueryWrapper` 条件。

---

## 七、验证机制

CDP 使用 Jakarta Bean Validation 配合**分组校验（Validation Groups）**区分新增和修改场景：

- `AddGroup` -- 新增场景校验组，继承 `Default`。
- `UpdateGroup` -- 修改场景校验组，继承 `Default`。

两者定义在 `com.leatop.cdp.core.validate` 包中。

在 Business 接口和 Controller 方法上通过 `@Validated(AddGroup.class)` 或 `@Validated(UpdateGroup.class)` 指定分组。例如在 `RoleBusiness` 中，`addRole` 使用 `AddGroup`，`updateRole` 使用 `UpdateGroup`。DTO 中的字段通过 `@NotNull(groups = UpdateGroup.class)` 标注仅在修改时必填的校验规则（如 `id` 字段）。

---

## 八、扩展指南

创建一个新的业务模块 `leatop-cdp-business-xxx` 的步骤：

1. **创建 5 个子模块目录**，参照上述结构。
2. **api 模块**：定义 Business 接口并标注 `@FeignClient`；创建 DTO、QO、枚举类。
3. **service 模块**：编写 PO 并标注 `@TableName`；创建 DAO 继承 `BaseMapper<PO>`；创建 Service 继承 `IService<PO>`；创建 BusinessImpl 标注 `@BusinessService` 并实现 Business 接口。
4. **controller 模块**：创建 Controller 注入 Business 接口，委托调用。
5. **boot-starter**：创建自动配置类，使用 `@AutoConfiguration` + `@ComponentScan` + `@MapperScan`；在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册。
6. **cloud-starter**：创建自动配置类，使用 `@AutoConfiguration` + `@ComponentScan(basePackages = controller包)` + `@EnableFeignClients(basePackages = business包)`；同样注册到 imports 文件。
7. **版本管理**：在 `leatop-cdp-bom` 中添加新模块的版本声明。

> 设计决策：新模块应严格遵循依赖方向（api <- service <- controller <- starter），禁止反向依赖或循环依赖。Business 接口是唯一允许跨模块引用的公开契约。
