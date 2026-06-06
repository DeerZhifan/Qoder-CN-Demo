---
trigger: when_referenced
knowledge_source:
  - cdp-design-common-data
  - cdp-module-common-data
  - 02-standard-po.java
  - 03-standard-dto.java
  - 04-standard-qo.java
  - 05-standard-dao.java
  - 06-standard-service-interface.java
  - 07-standard-service-impl.java
  - 08-standard-controller.java
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 创建新的业务实体 CRUD 代码
- 生成或修改 PO、DTO、QO、DAO、Service、Business、Controller 文件
- 讨论四层架构（Controller -> Business -> Service -> DAO）的代码组织

---

## 前置依赖

1. 项目已引入 `leatop-cdp-common-starter` 依赖（提供 BasePo、BaseDto、PageQo 等基类）。
2. 启动类已配置 `@MapperScan("{包名}.**.dao")` 扫描 DAO 接口。
3. 已引入 `com.github.pagehelper` 分页插件（由 `leatop-cdp-common-starter` 传递引入）。

---

## 配置要点

### 四层架构

请求处理链路：`Controller -> Business -> Service -> DAO -> Database`

| 层 | 职责 | 注解 |
|----|------|------|
| Controller | REST 端点，参数接收与校验，调用 Business | `@RestController` |
| Business | `Message<T>` 响应包装，编排多个 Service | `@BusinessService` |
| Service | 核心业务逻辑，PO/DTO 转换，事务管理 | `@Service` |
| DAO | 数据库操作，继承 `BaseMapper<XxxPo>` | `@Mapper` 或 `@MapperScan` |

> 简单单体项目可省略 Business 层，Controller 直接调用 Service。模块化业务模块必须使用完整四层。

### 模型命名约定

| 类型 | 后缀 | 基类 | 所在包 |
|------|------|------|--------|
| PO | `Po` | 无基类或 `BasePo<T>` / `CommonEntity<T>` | `po/` |
| DTO | `Dto` | 无基类或 `BaseDto` | `dto/` |
| QO | `PageQo` | `PageQo` | `qo/` |
| DAO | `Dao` | `BaseMapper<XxxPo>` | `dao/` |

### 包目录结构

```
{基础包}/
├── controller/       # Controller 层
├── business/         # Business 接口
│   └── impl/         # Business 实现
├── service/          # Service 接口
│   └── impl/         # Service 实现
├── dao/              # DAO 接口
├── po/               # PO 实体
├── dto/              # DTO 对象
└── qo/               # QO 查询参数
```

---

## 代码模式

### 推荐写法

**PO 规范：**

- 类名以 `Po` 结尾，使用 `@TableName` 指定表名
- 使用 `@TableId(type = IdType.ASSIGN_UUID)` 指定 UUID 主键
- 使用 `@TableField` 指定列名（与属性名一致时可省略）
- 实现 `Serializable`，声明 `serialVersionUID`
- 使用 `@Accessors(chain = true)` 支持链式调用
- 不包含业务逻辑，仅做字段映射

**DTO 规范：**

- 类名以 `Dto` 结尾
- 不含 `@TableName`、`@TableField` 等数据库注解
- 实现 `Serializable`，声明 `serialVersionUID`
- 字段与 PO 保持语义一致，可裁剪（如不对外暴露 `tenantId`）

**QO 规范：**

- 类名以 `PageQo` 结尾，继承 `com.leatop.cdp.data.qo.PageQo`
- 只放查询条件字段，不放排序、格式化等逻辑
- 字段为 null 时表示该条件不生效

**DAO 规范：**

- 继承 `BaseMapper<XxxPo>`，加 `@Mapper` 注解
- 简单查询用 BaseMapper 内置方法，复杂查询声明方法后在 XML 中实现

**Service 规范：**

- 接口继承 `IService<XxxPo>`
- 实现类继承 `ServiceImpl<XxxDao, XxxPo>`
- PO 与 DTO 转换用 `BeanUtil.copyProperties` / `BeanUtil.copyToList`
- 分页使用 `PageHelper.startPage()` + `Page.of()` 包装结果
- 条件查询用 `LambdaQueryWrapper`，避免硬编码字段名
- 业务异常统一抛 `BusException`

**Business 规范：**

- 使用 `@BusinessService` 注解（内含 `@Service`）
- 统一用 `Message.success()` / `Message.fail()` 包装返回值

**Controller 规范：**

- 返回值统一用 `Message<T>` 包装
- 新增用 `AddGroup`，修改用 `UpdateGroup` 做分组校验
- 标准路径：`/add`、`/update`、`/delete/{ids}`、`/get/{id}`、`/listPage`

### 禁止事项

- **禁止 Controller 中写业务逻辑或直接操作 DAO**
- **禁止 PO 类直接作为接口返回值** -- 必须转为 DTO
- **禁止在 DTO 中使用数据库注解**（`@TableName`、`@TableField`）
- **禁止在 QO 中硬编码排序逻辑** -- 排序由 `PageQo` 基类的 `orderBy` 字段统一处理
- **禁止在 Service 中返回 `Message<T>`** -- `Message` 包装只在 Business 层或 Controller 层完成
- **禁止在循环中逐条执行数据库查询** -- 应批量操作
- **禁止抛出原生 `RuntimeException`** -- 使用 `BusException` / `UncheckedException`
- **禁止在 Controller 中自定义 `@ExceptionHandler`** -- 框架已提供全局异常处理器
