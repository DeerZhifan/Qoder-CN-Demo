---
trigger: always_on
---

## 二、四层架构

请求处理链路：`Controller → Business → Service → DAO → Database`

| 层 | 职责 | 注入关系 | 注解 |
|----|------|---------|------|
| **Controller** | REST 端点暴露，参数接收与校验 | 注入 Business | `@RestController` |
| **Business** | `Message<T>` 响应包装，编排多个 Service | 注入 Service | `@BusinessService` |
| **Service** | 核心业务逻辑，PO↔DTO 转换，事务管理 | 注入 DAO 或继承 ServiceImpl | `@Service` |
| **DAO** | 数据库操作 | 继承 `BaseMapper<XxxPo>` | `@MapperScan` 扫描 |

> **简单单体项目**可省略 Business 层，Controller 直接调用 Service。**模块化业务模块**（含 boot-starter/cloud-starter）必须使用完整四层。

---

## 三、模型约定

| 类型 | 命名 | 基类 | 说明 |
|------|------|------|------|
| PO | `XxxPo` | `extends BasePo<XxxPo>` | 数据库实体，放 `po/` 包 |
| DTO | `XxxDto` | 可选 `extends BaseDto` | 数据传输对象，放 `dto/` 包 |
| QO | `XxxQo` | `extends PageQo` | 分页查询参数，放 `qo/` 包 |
| DAO | `XxxDao` | `extends BaseMapper<XxxPo>` | Mapper 接口，放 `dao/` 包 |

### PO 示例

`BasePo<T>`（`com.leatop.cdp.data.po`）继承 MyBatis-Plus `Model<T>`，自动填充 `tenantId`、`createGmt`、`updateGmt`，禁止重复定义这些字段：

```java
@TableName("t_order") @Data @EqualsAndHashCode(callSuper = true) @Accessors(chain = true)
public class OrderPo extends BasePo<OrderPo> implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
    @TableField("order_name")
    private String orderName;
}
```

### DTO / QO 示例

DTO 可继承 `BaseDto` 或直接 `@Data`；QO 继承 `PageQo`（提供 page/size/orderBy/searchKey）：

```java
@Data @Accessors(chain = true)
public class OrderDto implements Serializable {
    private String id;
    private String orderName;
}

@Data @EqualsAndHashCode(callSuper = true)
public class OrderPageQo extends PageQo {
    private String orderName;
}
```

---

## 四、Service 层规范

可直接注入 DAO，也可继承 `ServiceImpl<XxxDao, XxxPo>`（推荐，获得内置 CRUD 方法）：

```java
@Service
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderPo> implements OrderService {
    @Override
    public void add(OrderDto dto) {
        this.save(BeanUtil.toBean(dto, OrderPo.class));   // ServiceImpl 内置方法
    }

    @Override
    public Page<OrderDto> getListPage(OrderPageQo qo) {
        LambdaQueryWrapper<OrderPo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(qo.getOrderName())) {
            wrapper.like(OrderPo::getOrderName, qo.getOrderName());
        }
        try (com.github.pagehelper.Page<OrderPo> ph = PageHelper.startPage(qo.getPage(), qo.getSize())) {
            if (StringUtils.isNotEmpty(qo.getOrderBy())) { ph.setOrderBy(qo.getOrderBy()); }
            List<OrderPo> poList = this.list(wrapper);
            return Page.of(ph.getPageNum(), ph.getPageSize(), ph.getTotal(),
                           BeanUtil.copyToList(poList, OrderDto.class));
        }
    }
}
```

- 对象转换：`cn.hutool.core.bean.BeanUtil`（`toBean` / `copyToList`）
- 分页：`PageHelper.startPage()` + `com.leatop.cdp.data.model.Page.of()`
- 查询：`LambdaQueryWrapper` 动态拼接
- 事务：`@Transactional` 标注在 Service 方法上

---

## 五、Business 层规范

```java
@BusinessService   // com.leatop.cdp.data.annotation.BusinessService
public class OrderBusinessImpl implements OrderBusiness {
    @Autowired
    private OrderService orderService;

    @Override
    public Message<String> add(OrderDto dto) {
        orderService.add(dto);
        return Message.success("添加成功！");
    }

    @Override
    public Message<Page<OrderDto>> getListPage(OrderPageQo qo) {
        return Message.success(orderService.getListPage(qo));
    }
}
```

- `@BusinessService` 是 CDP 自定义注解（内部标注了 `@Service`）
- 统一用 `com.leatop.cdp.data.message.Message` 包装返回值
- `Message.success(data)` / `Message.fail(code, msg)`
- 标准成功消息：添加成功！/ 修改成功！/ 删除成功！

---

## 六、Controller 层规范

```java
@RestController
@AllArgsConstructor          // Lombok 构造器注入（或 @Autowired 字段注入）
@RequestMapping("/order")
public class OrderController {
    private final OrderBusiness orderBusiness;

    @PostMapping("/add")
    public Message<String> add(@RequestBody @Validated(AddGroup.class) OrderDto dto) {
        return orderBusiness.add(dto);
    }

    @GetMapping("/get/{id}")
    public Message<OrderDto> getById(@PathVariable String id) {
        return orderBusiness.getById(id);
    }

    @PostMapping("/listPage")
    public Message<Page<OrderDto>> getListPage(@RequestBody OrderPageQo qo) {
        return orderBusiness.getListPage(qo);
    }
}
```

### 标准 CRUD 路径

| 操作 | 方法 | 路径 |
|------|------|------|
| 新增 | POST | `/add` |
| 修改 | POST | `/update` |
| 删除 | POST | `/delete/{ids}` |
| 批量删除 | POST | `/deleteBatchIds` |
| 列表 | POST | `/list` |
| 分页 | POST | `/listPage` |
| 详情 | GET | `/get/{id}` |

参数校验用 `@Validated` 配合分组：`AddGroup.class`（新增）、`UpdateGroup.class`（修改）。

---

## 七、异常处理

CDP 提供两种异常（`com.leatop.cdp.data.exception`），框架全局处理器（`GlobalExceptionHandler`）自动捕获：

```java
// 业务校验失败 → BusException（errorCode 默认 500）
throw new BusException("订单不存在");
throw new BusException("用户名或密码错误", 401);

// 系统运行时异常 → UncheckedException
throw new UncheckedException("服务调用超时", 500);
throw UncheckedException.of(503, "下游服务不可用");
```

- 异常消息用**中文**，便于前端直接展示
- Service 层抛出异常，Controller 不捕获（交由全局处理器）

---

## 八、安全与性能规范

1. **输入校验**：
   - 使用 `@Validated` 注解 + JSR-303 校验注解（如 `@NotBlank`, `@Size`）
   - 禁止直接拼接 SQL 防止注入攻击
2. **事务管理**：
   - `@Transactional` 注解仅标注在 Service 方法上
   - 避免在循环中频繁提交事务
3. **性能优化**：
   - 避免在循环中执行数据库查询（批量操作优先）
   - 使用 `LambdaQueryWrapper` 按需查询字段，避免 `SELECT *`

---

## 九、代码风格规范

1. **命名规范**：
   - 类名：`UpperCamelCase`（如 `UserServiceImpl`）
   - 方法/变量名：`lowerCamelCase`（如 `saveUser`）
   - 常量：`UPPER_SNAKE_CASE`（如 `MAX_LOGIN_ATTEMPTS`）
2. **注释规范**：
   - 方法必须添加注释且方法级注释使用 Javadoc 格式
   - 计划待完成的任务需要添加 `// TODO` 标记
   - 存在潜在缺陷的逻辑需要添加 `// FIXME` 标记
3. **代码格式化**：
   - 使用 IntelliJ IDEA 默认的 Spring Boot 风格
   - 禁止手动修改代码缩进（依赖 IDE 自动格式化）
4. **日志规范**：
   - 使用 `SLF4J` 记录日志（`@Slf4j`），禁止 `System.out.println`
   - 核心操作需记录 `INFO` 级别日志，异常记录 `ERROR` 级别

---

## 十、必选依赖

默认初始化项目包含基础框架、系统管理、日志组件，须引入以下依赖，缺少任何一项会导致启动失败：

1. `leatop-cdp-common-starter` — 核心基础
2. `leatop-cdp-common-auth` — 认证授权
3. `leatop-cdp-business-system-login` — 登录模块
4. `leatop-cdp-business-system-boot-starter`（微服务用 `cloud-starter`）
5. `leatop-cdp-business-log-boot-starter`（微服务用 `cloud-starter`）
6. `leatop-cdp-base-flyway` — 数据库迁移
7. `mysql-connector-j`（或对应数据库驱动）

`<properties>` 中须声明 `<flyway.version>9.22.3</flyway.version>`。

---

## 十一、禁止事项

- 禁止 Controller 中写业务逻辑或直接操作 DAO
- 禁止 PO 类直接作为接口返回值（必须转为 DTO）
- 禁止模块间循环依赖
- 禁止直接使用 `RedisTemplate` / `RedissonClient`（通过框架封装的 Client 操作）
- 禁止硬编码数据库方言 SQL（需兼容 MySQL/GaussDB/DM/KingBase/GBase/PolarDB）
- 禁止修改已执行的 Flyway 迁移脚本
- 禁止在日志中打印密码、token 等敏感信息
- 禁止在子模块中硬编码依赖版本号（通过 BOM 管理）
- 禁止在 Controller 中自定义 `@ExceptionHandler`（框架已覆盖）
- 禁止抛出原生 `RuntimeException`（使用 `BusException` / `UncheckedException`）