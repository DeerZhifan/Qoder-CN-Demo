# 如何使用 CDP 基础数据类

## 概述

基础数据模块（`leatop-cdp-common-data`）提供了框架中所有模块共用的基础类，包括 PO 基类、DTO 基类、统一响应包装 `Message`、标准错误码 `ErrorCodeEnum` 和异常类体系。业务模块的实体类和数据传输对象应继承这些基类。

## PO 基类

### BasePo — 标准 PO 基类

```java
import com.leatop.cdp.data.po.BasePo;

@Data
@TableName("my_table")
public class UserPO extends BasePo<UserPO> {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String username;
    private String email;
}
```

`BasePo` 内置字段（自动填充）：

| 字段 | 类型 | 说明 | 填充时机 |
|------|------|------|---------|
| `tenantId` | String | 租户 ID | INSERT |
| `createGmt` | Date | 创建时间 | INSERT |
| `updateGmt` | Date | 更新时间 | INSERT + UPDATE |

### CommonEntity — UUID 主键基类

```java
import com.leatop.cdp.data.po.CommonEntity;

@Data
@TableName("my_table")
public class OrderPO extends CommonEntity<OrderPO> {
    // 已内置 UUID 主键（@TableId(type=IdType.ASSIGN_UUID)）
    private String orderNo;
    private BigDecimal amount;
}
```

### CommonInfoEntity — 带审计字段的基类

继承 `CommonEntity`，额外提供审计字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `createdBy` | String | 创建人 |
| `createdOn` | Date | 创建时间 |
| `modifiedBy` | String | 修改人 |
| `modifiedOn` | Date | 修改时间 |

## DTO 基类

### BaseDto

```java
import com.leatop.cdp.data.dto.BaseDto;

@Data
public class UserDTO extends BaseDto {
    private String id;
    private String username;
}
```

`BaseDto` 内置字段：`tenantId`、`createGmt`、`updateGmt`。

## 统一响应 Message\<T\>

所有 Controller 接口统一返回 `Message<T>`：

```java
import com.leatop.cdp.data.message.Message;

// 成功响应
Message.success();                         // code=200, msg="成功"
Message.success(data);                     // code=200, data=数据对象
Message.success(data, "操作成功");          // 自定义消息

// 失败响应
Message.fail("操作失败");                   // code=400
Message.fail(500, "服务异常");              // 自定义错误码
Message.fail(ErrorCodeEnum.UNAUTHORIZED);  // 使用枚举

// 自定义响应
Message.result(200, "ok", data);
```

## 标准错误码 ErrorCodeEnum

| 枚举值 | code | msg |
|--------|------|-----|
| `SUCCESS` | 200 | 成功 |
| `FAIL` | 400 | 处理失败 |
| `ERROR` | 500 | 服务异常 |
| `UNAUTHORIZED` | 401 | 用户未登录 |
| `EXPIRED` | 406 | 认证信息已失效 |
| `NO_PERMISSIONS` | 407 | 无操作权限 |
| `ILLEGAL_PARAMS` | 60001 | 不合法参数 |

可实现 `ErrorCode` 接口自定义错误码：

```java
import com.leatop.cdp.data.message.ErrorCode;

public enum BizErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(40001, "订单不存在"),
    STOCK_NOT_ENOUGH(40002, "库存不足");

    private final int code;
    private final String msg;
    // 构造函数、getCode()、getMsg()
}
```

## 注意事项

> 注意：PO 类必须继承 `BasePo` 或 `CommonEntity`，以获得租户隔离和审计字段的自动填充。

> 注意：`tenantId` 字段由框架自动填充，不需要业务代码手动设置。

> 注意：Controller 返回值统一使用 `Message<T>` 包装，不要直接返回业务对象。

> 注意：`BasePo` 继承了 MyBatis-Plus 的 `Model`，支持 ActiveRecord 模式（如 `entity.selectById()`），但推荐使用 Mapper 进行数据操作。
