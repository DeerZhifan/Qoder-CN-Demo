# CDP 代码规范文档

> 适用版本：CDP 1.0.3-SNAPSHOT 及以上
> 技术栈：Java 17 + Spring Boot 3.5 + Spring Cloud 2025 + MyBatis-Plus 3.5

---

## 目录

1. [如何定义 PO（数据库实体类）](#1-如何定义-po数据库实体类)
2. [如何定义 DTO（数据传输对象）](#2-如何定义-dto数据传输对象)
3. [如何定义 QO（查询参数对象）](#3-如何定义-qo查询参数对象)
4. [Controller 层如何编写](#4-controller-层如何编写)
5. [Business 层如何编写（双部署模式）](#5-business-层如何编写双部署模式)
6. [Service 层如何编写](#6-service-层如何编写)
7. [Mapper/DAO 层如何编写](#7-mapperdao-层如何编写)
8. [如何使用统一响应包装 Message](#8-如何使用统一响应包装-message)
9. [如何处理异常（BusException 和 UncheckedException）](#9-如何处理异常busexception-和-uncheckedexception)
10. [如何使用参数校验（Validation Groups）](#10-如何使用参数校验validation-groups)
11. [MyBatis-Plus 常用查询写法](#11-mybatis-plus-常用查询写法)
12. [如何命名常量类和枚举](#12-如何命名常量类和枚举)
13. [依赖版本如何管理（Maven BOM 规范）](#13-依赖版本如何管理maven-bom-规范)
14. [如何编写 API 文档（Smart-doc 规范）](#14-如何编写-api-文档smart-doc-规范)
15. [Git 提交信息规范](#15-git-提交信息规范)

---

## 1. 如何定义 PO（数据库实体类）

PO（Persistent Object）是与数据库表一一对应的实体类，仅用于 Mapper/DAO 层的数据存取，不应直接暴露给 Controller 层。

### 存放位置

- 业务模块：`*-service` 子模块的 `po` 包（如 `com.leatop.cdp.file.po`）
- 示例模块：`model.po` 包（如 `com.leatop.example.model.po`）

### 命名规则

- 类名以 `PO` 或 `Po` 结尾，与表名对应（如 `NewsItemPo` 对应 `news_item` 表）

### 基类选择

- **推荐**继承 `BasePo<T>`（来自 `com.leatop.cdp.data.po`），自动提供 `tenantId`、`createGmt`、`updateGmt` 字段及自动填充
- 简单场景可不继承基类，但需手动管理审计字段

### 标准写法

```java
package com.leatop.example.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 新闻条目数据实体
 *
 * @author example
 * @date 2025/1/20 15:26
 */
@Accessors(chain = true)
@Data
@TableName("news_item")
public class NewsItemPo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（UUID） */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 文章标题 */
    @TableField(value = "title")
    private String title;

    /** 来源 */
    @TableField(value = "source")
    private String source;

    /** 状态：0-未解析，1-解析成功，2-解析异常 */
    @TableField(value = "status")
    private Integer status;

    /** 租户ID */
    @TableField(value = "tenant_id")
    private String tenantId;

    /** 创建时间 */
    @TableField(value = "create_gmt")
    private LocalDateTime createGmt;

    /** 更新时间 */
    @TableField(value = "update_gmt")
    private LocalDateTime updateGmt;
}
```

### 必须遵守的注解

| 注解 | 用途 | 示例 |
|------|------|------|
| `@TableName` | 指定数据库表名 | `@TableName("news_item")` |
| `@TableId` | 指定主键及生成策略 | `@TableId(value = "id", type = IdType.ASSIGN_UUID)` |
| `@TableField` | 指定列名映射 | `@TableField(value = "title")` |
| `@Data` | 自动生成 getter/setter/toString | Lombok |
| `@Accessors(chain = true)` | 支持链式调用 | Lombok |

### 主键生成策略

| 策略 | 适用场景 |
|------|---------|
| `IdType.ASSIGN_UUID` | 通用主键，UUID 格式 |
| `IdType.ASSIGN_ID` | 雪花算法，数值型主键 |

> 注意：PO 只用于 Mapper/DAO 层，禁止直接作为 Controller 的返回值。PO 与 DTO 之间通过 `BeanUtil.copyProperties()` 转换。每个字段必须有 Javadoc 注释说明业务含义。

---

## 2. 如何定义 DTO（数据传输对象）

DTO（Data Transfer Object）用于 Controller 入参/出参和 Service 层数据传递，是 API 的契约。

### 存放位置

- 业务模块：`*-api` 子模块的 `dto` 包（如 `com.leatop.cdp.file.dto`）
- 示例模块：`model.dto` 包（如 `com.leatop.example.model.dto`）

### 命名规则

- 类名以 `DTO` 或 `Dto` 结尾（如 `NewsItemDto`、`AttachmentInfoDTO`）

### 标准写法

```java
package com.leatop.example.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 新闻条目数据传输对象
 *
 * @author example
 * @date 2025/1/20 15:26
 */
@Accessors(chain = true)
@Data
public class NewsItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private String id;

    /** 文章标题 */
    private String title;

    /** 来源 */
    private String source;

    /** 状态：0-未解析，1-解析成功，2-解析异常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createGmt;

    /** 更新时间 */
    private LocalDateTime updateGmt;
}
```

### 与 PO 的关键区别

| 对比项 | PO | DTO |
|--------|-----|-----|
| 数据库注解 | `@TableName`, `@TableId`, `@TableField` | **无** |
| 用途 | 数据库读写 | API 传输 |
| 字段范围 | 与表列完全对应 | 可裁剪（如隐藏 `tenantId`） |
| 校验注解 | 无 | 可加 `@NotBlank`, `@NotNull` 等 |

### 可选基类

- 继承 `BaseDto`（来自 `com.leatop.cdp.data.dto`）可获得 `tenantId`、`createGmt`、`updateGmt` 公共字段
- 简单场景可不继承任何基类

### 带校验的 DTO 写法

```java
@Data
public class SmsPlatformDTO extends BaseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    /** 编码 */
    @NotBlank
    private String code;

    /** 名称 */
    @NotBlank
    private String name;

    /** 短信类型 */
    @NotNull
    private Integer smsType;
}
```

> 注意：DTO 不要继承 PO，也不要在 DTO 上加 `@TableName`、`@TableField` 等数据库注解。DTO 是 API 的契约，PO 是数据库的映射，两者职责不同。

---

## 3. 如何定义 QO（查询参数对象）

QO（Query Object）用于封装列表查询条件，通常配合分页使用。

### 存放位置

- 业务模块：`*-api` 子模块的 `qo` 包
- 示例模块：`model.qo` 包

### 命名规则

- 分页查询：`XxxPageQo`（如 `NewsItemPageQo`）
- 非分页查询：`XxxQO`（如 `UserQueryQO`）

### 标准写法（分页查询）

```java
package com.leatop.example.model.qo;

import com.leatop.cdp.data.qo.PageQo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 新闻条目分页查询参数
 *
 * @author example
 * @date 2025/1/20 15:26
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Data
public class NewsItemPageQo extends PageQo {

    /** 文章标题（模糊查询） */
    private String title;

    /** 来源（模糊查询） */
    private String source;

    /** 状态（精确查询） */
    private Integer status;
}
```

### PageQo 基类提供的字段

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 当前页码 |
| `size` | int | 10 | 每页条数 |
| `orderBy` | String | null | 排序字段 |
| `searchKey` | String | null | 通用搜索关键字 |

### QO 字段约定

- 字段为 `null` 时表示该条件不生效（在 ServiceImpl 中通过 `LambdaQueryWrapper` 的条件参数控制）
- 只放查询条件字段，不放排序、格式化等逻辑

> 注意：分页参数不要在每个 QO 中重复定义 `page`/`size` 字段，统一继承 `PageQo` 基类。QO 放在 `*-api` 模块中（与 DTO 同级），不放在 `*-service` 模块中。

---

## 4. Controller 层如何编写

Controller 层只做参数接收、参数校验、调用 Service/Business、返回结果，**不编写任何业务逻辑**。

### 标准写法

```java
package com.leatop.example.controller;

import com.leatop.cdp.core.validate.AddGroup;
import com.leatop.cdp.core.validate.UpdateGroup;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.example.model.dto.NewsItemDto;
import com.leatop.example.model.qo.NewsItemPageQo;
import com.leatop.example.service.NewsItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 新闻条目控制器
 *
 * @author example
 * @date 2025/1/20 15:26
 */
@RestController
@RequestMapping("/news_research/news_item")
public class NewsItemController {

    @Autowired
    private NewsItemService newsItemService;

    /**
     * 新增
     *
     * @param dto 新增参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public Message<Boolean> add(@RequestBody @Validated(AddGroup.class) NewsItemDto dto) {
        return newsItemService.add(dto);
    }

    /**
     * 修改
     *
     * @param dto 修改参数
     * @return 操作结果
     */
    @PostMapping("/update")
    public Message<Boolean> update(@RequestBody @Validated(UpdateGroup.class) NewsItemDto dto) {
        return newsItemService.update(dto);
    }

    /**
     * 根据 ID 查询详情
     *
     * @param id 主键 ID
     * @return 详情
     */
    @GetMapping("/queryById")
    public Message<NewsItemDto> queryById(@RequestParam String id) {
        return newsItemService.queryById(id);
    }

    /**
     * 删除（支持批量，多个 id 用 ',' 分隔）
     *
     * @param ids ID 列表
     * @return 操作结果
     */
    @PostMapping("/delete/{ids}")
    public Message<Boolean> delete(@PathVariable("ids") String ids) {
        return newsItemService.delete(ids);
    }

    /**
     * 分页查询
     *
     * @param qo 查询参数
     * @return 分页列表
     */
    @PostMapping("/listPage")
    public Message<Page<NewsItemDto>> listPage(@Validated NewsItemPageQo qo) {
        return newsItemService.listPage(qo);
    }
}
```

### Controller 规范要点

| 规则 | 说明 |
|------|------|
| 返回值 | 统一使用 `Message<T>` 包装 |
| 参数校验 | 新增用 `@Validated(AddGroup.class)`，修改用 `@Validated(UpdateGroup.class)` |
| URL 命名 | 小写 + 下划线，三段式：`/模块名/功能名/操作` |
| 注入方式 | 推荐构造器注入（`@AllArgsConstructor` + `private final`），也接受 `@Autowired` |
| JavaDoc | 每个公开方法**必须**有 JavaDoc（Smart-doc 依赖此生成接口文档） |

> 注意：不要在 Controller 中编写 `if/else` 业务判断、数据库查询、对象转换等逻辑。Controller 的职责仅限于参数接收和调用下游。

---

## 5. Business 层如何编写（双部署模式）

Business 层是 CDP 框架特有的设计，用于支持同一套代码在单体和微服务两种模式下运行。

### 架构说明

```
单体模式：Controller → BusinessImpl（本地调用）→ Service
微服务模式：Controller → FeignClient（远程调用）→ BusinessImpl → Service
```

### Business 接口（位于 `*-api` 模块）

```java
package com.leatop.cdp.form.business;

import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.form.dto.FormCatalogDto;
import com.leatop.cdp.form.qo.FormCatalogPageQo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 表单分类管理
 *
 * @author luowei
 * @date 2024/11/25 10:54
 */
@FeignClient(contextId = "cdpFormCatalogBusiness",
        name = "${cdp.feign.form.name:cdp-form}",
        url = "${cdp.feign.form.url:}",
        path = "${cdp.feign.form.path:}")
public interface FormCatalogBusiness {

    /**
     * 新增
     *
     * @param formCatalogDto 模块dto
     * @return message
     */
    @PostMapping("/form/catalog/add")
    Message<String> add(@RequestBody FormCatalogDto formCatalogDto);

    /**
     * 详情
     *
     * @param id id
     * @return message
     */
    @GetMapping("/form/catalog/get/{id}")
    Message<FormCatalogDto> get(@PathVariable("id") String id);

    /**
     * 分页查询
     *
     * @param formCatalogPageQo 分页查询条件
     * @return message
     */
    @PostMapping("/form/catalog/listPage")
    Message<Page<FormCatalogDto>> listPage(@RequestBody FormCatalogPageQo formCatalogPageQo);
}
```

### Business 实现（位于 `*-service` 模块）

```java
package com.leatop.cdp.form.controller;

import com.leatop.cdp.data.annotation.BusinessService;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.form.business.FormCatalogBusiness;
import com.leatop.cdp.form.dto.FormCatalogDto;
import com.leatop.cdp.form.qo.FormCatalogPageQo;
import com.leatop.cdp.form.service.FormCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 表单分类管理实现
 *
 * @author tyj
 * @date 2024/11/25 11:00
 */
@BusinessService
public class FormCatalogBusinessImpl implements FormCatalogBusiness {

    @Autowired
    private FormCatalogService formCatalogService;

    @Override
    public Message<String> add(@RequestBody FormCatalogDto formCatalogDto) {
        formCatalogService.add(formCatalogDto);
        return Message.success("添加成功");
    }

    @Override
    public Message<FormCatalogDto> get(String id) {
        return Message.success(formCatalogService.get(id));
    }

    @Override
    public Message<Page<FormCatalogDto>> listPage(@RequestBody FormCatalogPageQo qo) {
        return Message.success(formCatalogService.listPage(qo));
    }
}
```

### 关键约定

| 项目 | Business 接口 | Business 实现 |
|------|--------------|--------------|
| 位置 | `*-api` 模块 | `*-service` 模块 |
| 注解 | `@FeignClient` | `@BusinessService` |
| 返回值 | `Message<T>` | `Message<T>` |
| 职责 | 定义 HTTP 契约 | 调用 Service 并包装结果 |

> 注意：Business 实现类使用 `@BusinessService` 注解（来自 `com.leatop.cdp.data.annotation`），不要使用 `@Service`。`@BusinessService` 继承了 `@Service` 功能，同时标识这是业务服务层。示例模块（如 demo1）可跳过 Business 层，Controller 直接调用 Service。

---

## 6. Service 层如何编写

Service 层是业务逻辑的核心实现层，负责数据处理、对象转换、业务校验等。

### Service 接口

```java
package com.leatop.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.example.model.dto.NewsItemDto;
import com.leatop.example.model.po.NewsItemPo;
import com.leatop.example.model.qo.NewsItemPageQo;

/**
 * 新闻条目服务接口
 *
 * @author example
 * @date 2025/1/20 15:26
 */
public interface NewsItemService extends IService<NewsItemPo> {

    /** 新增 */
    Message<Boolean> add(NewsItemDto dto);

    /** 修改 */
    Message<Boolean> update(NewsItemDto dto);

    /** 根据 ID 查询详情 */
    Message<NewsItemDto> queryById(String id);

    /** 删除（支持批量） */
    Message<Boolean> delete(String ids);

    /** 分页查询 */
    Message<Page<NewsItemDto>> listPage(NewsItemPageQo qo);
}
```

### Service 实现

```java
package com.leatop.example.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.leatop.cdp.data.exception.BusException;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.util.BeanUtil;
import com.leatop.cdp.util.StrUtil;
import com.leatop.example.dao.NewsItemDao;
import com.leatop.example.model.dto.NewsItemDto;
import com.leatop.example.model.po.NewsItemPo;
import com.leatop.example.model.qo.NewsItemPageQo;
import com.leatop.example.service.NewsItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 新闻条目服务实现
 *
 * @author example
 * @date 2025/1/20 15:26
 */
@Service
public class NewsItemServiceImpl extends ServiceImpl<NewsItemDao, NewsItemPo>
        implements NewsItemService {

    @Override
    public Message<Boolean> add(NewsItemDto dto) {
        NewsItemPo po = BeanUtil.copyProperties(dto, NewsItemPo.class);
        return Message.success(save(po));
    }

    @Override
    public Message<Boolean> update(NewsItemDto dto) {
        NewsItemPo po = BeanUtil.copyProperties(dto, NewsItemPo.class);
        return Message.success(updateById(po));
    }

    @Override
    public Message<NewsItemDto> queryById(String id) {
        NewsItemPo po = getById(id);
        if (Objects.isNull(po)) {
            throw new BusException("数据不存在，id=" + id);
        }
        return Message.success(BeanUtil.copyProperties(po, NewsItemDto.class));
    }

    @Override
    @Transactional
    public Message<Boolean> delete(String ids) {
        List<String> idList = StrUtil.split(ids, ',');
        if (CollUtil.isEmpty(idList)) {
            throw new BusException("ids 不能为空");
        }
        return Message.success(removeBatchByIds(idList));
    }

    @Override
    public Message<Page<NewsItemDto>> listPage(NewsItemPageQo qo) {
        // 1. 启动分页（必须在查询之前调用）
        com.github.pagehelper.Page<NewsItemPo> page =
                PageHelper.startPage(qo.getPage(), qo.getSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<NewsItemPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(qo.getTitle()), NewsItemPo::getTitle, qo.getTitle());
        wrapper.like(StrUtil.isNotBlank(qo.getSource()), NewsItemPo::getSource, qo.getSource());
        wrapper.eq(Objects.nonNull(qo.getStatus()), NewsItemPo::getStatus, qo.getStatus());
        wrapper.orderByDesc(NewsItemPo::getCreateGmt);

        // 3. 执行查询
        List<NewsItemPo> list = getBaseMapper().selectList(wrapper);

        // 4. PO 转 DTO 并包装分页结果
        List<NewsItemDto> results = BeanUtil.copyToList(list, NewsItemDto.class);
        return Message.success(
                Page.of(page.getPageNum(), page.getPageSize(), page.getTotal(), results));
    }
}
```

### Service 层核心模式

| 模式 | 用法 |
|------|------|
| 继承 | 接口 `extends IService<PO>`，实现类 `extends ServiceImpl<DAO, PO>` |
| 对象转换 | `BeanUtil.copyProperties(source, Target.class)` 单个转换 |
| 列表转换 | `BeanUtil.copyToList(list, Target.class)` 批量转换 |
| 分页 | `PageHelper.startPage()` → 查询 → `Page.of()` |
| 业务异常 | 校验失败抛 `BusException` |
| 事务 | 写操作方法加 `@Transactional` |

> 注意：`PageHelper.startPage()` 必须在查询语句之前调用，且中间不能有其他 SQL 操作。PO 与 DTO 之间的转换使用框架提供的 `BeanUtil`（`com.leatop.cdp.util.BeanUtil`），不要手动逐字段赋值。

---

## 7. Mapper/DAO 层如何编写

Mapper/DAO 层负责数据库操作，继承 MyBatis-Plus 的 `BaseMapper` 获得基础 CRUD 能力。

### 标准写法

```java
package com.leatop.example.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leatop.example.model.po.NewsItemPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新闻条目 Mapper 接口
 *
 * @author example
 * @date 2025/1/20 15:26
 */
@Mapper
public interface NewsItemDao extends BaseMapper<NewsItemPo> {

    // 简单查询直接用 BaseMapper 提供的方法
    // 复杂查询在这里声明方法，SQL 写在对应 XML 文件中
    // 示例：List<NewsItemDto> selectByCondition(@Param("qo") NewsItemPageQo qo);
}
```

### BaseMapper 内置方法（无需编写 SQL）

| 方法 | 说明 |
|------|------|
| `insert(entity)` | 插入一条记录 |
| `deleteById(id)` | 根据主键删除 |
| `updateById(entity)` | 根据主键更新 |
| `selectById(id)` | 根据主键查询 |
| `selectList(wrapper)` | 条件查询列表 |
| `selectCount(wrapper)` | 条件统计数量 |

### 复杂 SQL 的 XML 写法

XML 文件路径：`src/main/resources/mapper/XxxDao.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.leatop.example.dao.NewsItemDao">

    <select id="selectByCondition"
            resultType="com.leatop.example.model.dto.NewsItemDto">
        SELECT id, title, source, status, create_gmt, update_gmt
        FROM news_item
        <where>
            <if test="qo.title != null and qo.title != ''">
                AND title LIKE CONCAT('%', #{qo.title}, '%')
            </if>
            <if test="qo.status != null">
                AND status = #{qo.status}
            </if>
        </where>
        ORDER BY create_gmt DESC
    </select>

</mapper>
```

> 注意：命名统一使用 `Dao` 后缀（如 `NewsItemDao`）。简单 CRUD 使用 MyBatis-Plus 内置方法，只有复杂查询（多表关联、子查询、数据库函数等）才写 XML SQL。XML 的 `namespace` 必须与 Mapper 接口的全限定名一致。

---

## 8. 如何使用统一响应包装 Message

所有 API 接口的返回值必须使用 `Message<T>` 包装，保证前后端交互格式统一。

### Message 结构

```java
public class Message<T> implements Serializable {
    private Integer code;  // 状态码，200 表示成功
    private String msg;    // 提示信息
    private T data;        // 返回数据
}
```

### 常用静态方法

```java
// 成功 — 携带数据
Message.success(data);

// 成功 — 自定义消息
Message.success(data, "操作成功");

// 成功 — 无数据
Message.success();

// 失败 — 自定义消息（code 默认 400）
Message.fail("参数不合法");

// 失败 — 自定义 code 和消息
Message.fail(500, "服务异常");

// 失败 — 使用 ErrorCode 枚举
Message.fail(ErrorCodeEnum.UNAUTHORIZED);
```

### 标准错误码（ErrorCodeEnum）

| 错误码 | 常量 | 含义 |
|--------|------|------|
| 200 | `SUCCESS` | 成功 |
| 400 | `FAIL` | 处理失败 |
| 401 | `UNAUTHORIZED` | 用户未登录 |
| 406 | `EXPIRED` | 认证信息已失效 |
| 407 | `NO_PERMISSIONS` | 无操作权限 |
| 500 | `ERROR` | 服务异常 |
| 60001 | `ILLEGAL_PARAMS` | 不合法参数 |

### 使用示例

```java
// Controller 中
@GetMapping("/queryById")
public Message<NewsItemDto> queryById(@RequestParam String id) {
    return newsItemService.queryById(id);
}

// Service 中 — 成功
return Message.success(BeanUtil.copyProperties(po, NewsItemDto.class));

// Service 中 — 业务失败
if (count > 0) {
    return Message.fail("字典值编码已存在");
}

// 分页结果
return Message.success(Page.of(pageNum, pageSize, total, results));
```

> 注意：所有 Controller 方法必须返回 `Message<T>`，不要返回裸对象或 `ResponseEntity`。分页接口返回 `Message<Page<DTO>>`。

---

## 9. 如何处理异常（BusException 和 UncheckedException）

CDP 框架提供两种异常类型，全局异常处理器已在 `leatop-cdp-common-core` 中注册，无需业务代码额外配置。

### 异常类型对比

| 异常类 | 场景 | HTTP 状态码 | 用户可见 |
|--------|------|------------|---------|
| `BusException` | 业务校验失败 | 400 (BAD_REQUEST) | 是 |
| `UncheckedException` | 系统级错误 | 通过 code 字段返回 | 否（展示通用提示） |

### 使用示例

```java
import com.leatop.cdp.data.exception.BusException;
import com.leatop.cdp.data.exception.UncheckedException;

public class ExampleService {

    public void validateAndProcess(String id, String type) {
        // 场景一：参数/数据校验失败 → BusException
        if (id == null || id.isBlank()) {
            throw new BusException("ID 不能为空");
        }

        // 场景二：业务规则不满足 → BusException
        if (!"VALID_TYPE".equals(type)) {
            throw new BusException("不支持的类型：" + type);
        }

        // 场景三：调用外部系统失败 → UncheckedException（包装原始异常）
        try {
            callExternalService();
        } catch (Exception e) {
            throw new UncheckedException("调用外部服务失败", e);
        }
    }
}
```

### 禁止的做法

```java
// ❌ 禁止：使用原生 RuntimeException
throw new RuntimeException("参数不合法");

// ❌ 禁止：catch 后吞掉异常
try {
    service.doSomething();
} catch (Exception e) {
    log.error("出错了", e);
    // 没有 throw，异常被吞掉
}

// ❌ 禁止：catch 后仅打印日志（全局处理器会统一记录日志）
try {
    service.doSomething();
} catch (Exception e) {
    log.error("出错了", e);
    throw e;  // 重复记录日志
}
```

> 注意：不要直接抛出 `RuntimeException` 或 `IllegalArgumentException`，业务校验用 `BusException`，系统错误用 `UncheckedException`。不要在 Controller/Service 中 try-catch 后吞掉异常，全局异常处理器会统一处理。

---

## 10. 如何使用参数校验（Validation Groups）

CDP 框架使用 Jakarta Validation 注解进行参数校验，通过分组区分新增和修改场景。

### 校验分组

框架提供两个校验分组（来自 `com.leatop.cdp.core.validate`）：

- `AddGroup` — 新增操作的校验规则
- `UpdateGroup` — 修改操作的校验规则

两者都继承 `Default` 分组，意味着不指定 `groups` 的注解在两种场景下都生效。

### DTO 中定义校验规则

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.leatop.cdp.core.validate.UpdateGroup;

@Data
public class NewsItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（修改时必填） */
    @NotBlank(groups = UpdateGroup.class, message = "ID 不能为空")
    private String id;

    /** 文章标题（新增和修改都必填） */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 来源 */
    private String source;

    /** 状态 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
```

### Controller 中触发校验

```java
// 新增：触发 AddGroup + Default 组的校验（id 不校验，title 和 status 校验）
@PostMapping("/add")
public Message<Boolean> add(@RequestBody @Validated(AddGroup.class) NewsItemDto dto) {
    return newsItemService.add(dto);
}

// 修改：触发 UpdateGroup + Default 组的校验（id、title、status 都校验）
@PostMapping("/update")
public Message<Boolean> update(@RequestBody @Validated(UpdateGroup.class) NewsItemDto dto) {
    return newsItemService.update(dto);
}
```

### 常用校验注解

| 注解 | 适用类型 | 说明 |
|------|---------|------|
| `@NotBlank` | String | 不能为 null 且不能为空字符串 |
| `@NotNull` | 任意类型 | 不能为 null |
| `@NotEmpty` | 集合/数组/字符串 | 不能为 null 且不能为空 |
| `@Size(min, max)` | String/集合 | 长度/大小范围 |
| `@Min` / `@Max` | 数字 | 数值范围 |
| `@Pattern` | String | 正则匹配 |

> 注意：使用 `@Validated` 而非 `@Valid`（`@Validated` 支持分组校验）。校验失败由全局异常处理器自动捕获并返回 400 错误，无需手动处理。

---

## 11. MyBatis-Plus 常用查询写法

优先使用 `LambdaQueryWrapper`（类型安全，重构友好），避免硬编码字段名字符串。

### 带条件的查询

```java
// 条件查询 — 字段有值时才加条件（三参数形式）
LambdaQueryWrapper<NewsItemPo> wrapper = new LambdaQueryWrapper<>();
wrapper.like(title != null && !title.isBlank(), NewsItemPo::getTitle, title);
wrapper.like(source != null && !source.isBlank(), NewsItemPo::getSource, source);
wrapper.eq(Objects.nonNull(status), NewsItemPo::getStatus, status);
wrapper.orderByDesc(NewsItemPo::getCreateGmt);

List<NewsItemPo> list = getBaseMapper().selectList(wrapper);
```

### 查询单条记录

```java
// 取第一条匹配，加 LIMIT 1 防止多条时报错
NewsItemPo po = getOne(new LambdaQueryWrapper<NewsItemPo>()
        .eq(NewsItemPo::getSource, source)
        .orderByDesc(NewsItemPo::getCreateGmt)
        .last("LIMIT 1"));
```

### 只更新指定字段

```java
// 避免全字段 update 覆盖其他字段
LambdaUpdateWrapper<NewsItemPo> wrapper = new LambdaUpdateWrapper<>();
wrapper.eq(NewsItemPo::getId, id)
       .set(NewsItemPo::getStatus, status);
update(wrapper);
```

### 统计数量

```java
long count = count(new LambdaQueryWrapper<NewsItemPo>()
        .eq(NewsItemPo::getStatus, status));
```

### 链式查询（Fluent API）

```java
// 使用 ChainWrappers 实现更流畅的查询
long count = ChainWrappers.lambdaQueryChain(newsItemDao)
        .eq(NewsItemPo::getStatus, 1)
        .like(NewsItemPo::getTitle, keyword)
        .count();
```

> 注意：优先使用 `LambdaQueryWrapper`（方法引用，编译期检查），避免使用 `QueryWrapper` 的字符串列名写法。条件查询使用三参数形式 `wrapper.eq(condition, column, val)`，当 `condition` 为 `false` 时自动跳过该条件。

---

## 12. 如何命名常量类和枚举

### 常量类规范

| 规则 | 说明 |
|------|------|
| 后缀 | `Constant`（如 `CdpConstant`、`CacheNameTimeConstant`） |
| 字段命名 | 全大写下划线分隔（`UPPER_SNAKE_CASE`） |
| 修饰符 | `public static final` |
| 定义方式 | 使用 `class` 或 `interface`（接口的字段默认 `public static final`） |

```java
/**
 * CDP 框架常量
 *
 * @author jimy
 * @date 2024/6/15 10:41
 */
public class CdpConstant {

    /** 微服务内部调用鉴权参数名，值为 JWT 格式的 token */
    public static final String MICRO_TOKEN = "CDP-MICRO-TOKEN";
}
```

```java
/**
 * 缓存时间常量
 *
 * @author luowei
 * @date 2024/4/23 10:39
 */
public interface CacheNameTimeConstant {

    /** 1 秒缓存 */
    String CACHE_1SECS = "CACHE_DEFAULT_1SECS";

    /** 5 秒缓存 */
    String CACHE_5SECS = "CACHE_DEFAULT_5SECS";
}
```

### 枚举类规范

| 规则 | 说明 |
|------|------|
| 后缀 | `Enum`（如 `ErrorCodeEnum`、`JavaTypeEnum`） |
| Lombok | `@Getter` 自动生成属性访问方法 |
| 接口 | 可实现业务接口（如 `implements ErrorCode`） |

```java
/**
 * 常用错误码枚举
 *
 * @author luowei
 * @date 2024/4/11 12:05
 */
public enum ErrorCodeEnum implements ErrorCode {

    /** 成功 */
    SUCCESS(200, "成功"),
    /** 处理失败 */
    FAIL(400, "处理失败"),
    /** 服务异常 */
    ERROR(500, "服务异常"),
    /** 用户未登录 */
    UNAUTHORIZED(401, "用户未登录"),
    ;

    private final Integer code;
    private final String msg;

    ErrorCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getMsg() { return msg; }
}
```

> 注意：禁止在业务代码中硬编码魔法值（如 `if (status == 1)`），应提取为常量或枚举。每个常量和枚举值必须有 Javadoc 注释说明含义。

---

## 13. 依赖版本如何管理（Maven BOM 规范）

CDP 采用两层 BOM（Bill of Materials）模式统一管理所有依赖版本，禁止在子模块中直接指定版本号。

### 版本管理层级

```
leatop-cdp-dependencies  ← 管理所有第三方库版本
leatop-cdp-bom           ← 管理所有内部模块版本
子模块 pom.xml            ← 引用依赖时不写版本号
```

### 新增第三方依赖的步骤

**第一步**：在 `leatop-cdp-dependencies/pom.xml` 中声明版本

```xml
<properties>
    <new-lib.version>1.2.3</new-lib.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>new-lib</artifactId>
            <version>${new-lib.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**第二步**：在子模块 `pom.xml` 中引用（不写版本号）

```xml
<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>new-lib</artifactId>
        <!-- 不要在这里写 <version> -->
    </dependency>
</dependencies>
```

### 当前核心依赖版本

| 依赖 | 版本 |
|------|------|
| Spring Boot | 3.5.12 |
| Spring Cloud | 2025.0.0 |
| MyBatis-Plus | 3.5.15 |
| SA-Token | 1.44.0 |
| Hutool | 5.8.43 |
| PageHelper | 6.1.0 |
| EasyExcel | 3.3.3 |

> 注意：子模块的 `pom.xml` 中引用依赖时**绝对不要写版本号**。新增第三方库必须先在 `leatop-cdp-dependencies/pom.xml` 中声明版本。内部模块版本由 `leatop-cdp-bom` 统一管理，当前版本为 `1.0.3-SNAPSHOT`。

---

## 14. 如何编写 API 文档（Smart-doc 规范）

CDP 使用 Smart-doc 自动从 JavaDoc 注释生成 API 文档，**不使用 Swagger**。

### 生成命令

```bash
mvn com.ly.smart-doc:smart-doc-maven-plugin:3.0.3:html
```

### Controller 方法注释模板

Smart-doc 读取 Controller 方法的 JavaDoc 来生成文档，因此注释格式至关重要：

```java
/**
 * 获取所有 API Key 列表
 *
 * @return API Key 列表
 */
@GetMapping("/get_api_keys")
public Message<List<ApiKeyDTO>> getApiKeys() {
    return apiKeyService.getApiKeys();
}

/**
 * 分页查询数据源列信息
 *
 * @param qo 数据源连接信息（含分页参数）
 * @return 分页列信息
 */
@PostMapping("/getColumnPage")
public Message<Page<TableColumnInfoDto>> getColumnPage(@RequestBody DataSourceConnectionInfoQo qo) {
    return dataSourceService.getColumnPage(qo);
}
```

### DTO/QO 字段注释

字段的 Javadoc 会作为参数描述出现在文档中：

```java
public class NewsItemDto {

    /** 主键 */
    private String id;

    /** 文章标题 */
    private String title;

    /** 状态：0-未解析，1-解析成功，2-解析异常 */
    private Integer status;
}
```

### 注释要求

| 位置 | 要求 |
|------|------|
| Controller 类 | 必须有类注释（含 `@author`、`@date`） |
| Controller 方法 | 必须有方法注释（含 `@param`、`@return`） |
| DTO/QO 字段 | 必须有字段注释（说明业务含义） |

> 注意：项目使用 Smart-doc 生成文档，**禁止使用** Swagger 的 `@Api`、`@ApiOperation`、`@ApiModel` 等注解。所有 Controller 公开方法和 DTO/QO 字段必须有 JavaDoc 注释，否则生成的文档缺少描述信息。详细注释规范参见 `doc/comment-spec.md`。

---

## 15. Git 提交信息规范

### 提交格式

```
type: 简要描述
```

使用中文描述，简明扼要说明改动内容。

### 提交类型

| 类型 | 含义 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 增加工作流示例补签申请接口` |
| `fix` | Bug 修复 | `fix: 永洪报表定时续期会话token` |
| `docs` | 文档更新 | `docs: 补全 business Controller 类级 JavaDoc` |
| `refactor` | 代码重构 | `refactor: 提取缓存配置为独立类` |
| `test` | 测试相关 | `test: 增加用户查询接口单元测试` |

### 示例（来自项目实际提交）

```
docs: 更新smart-doc.json，优化接口列表配置
fix: 永洪报表定时续期会话token
docs: 补全 example Controller JavaDoc - fulltext/micro/vacation/lite/sharding
feat（demo）: 增加工作流示例补签申请接口
```

> 注意：提交类型后使用英文冒号 `:` 加空格，描述使用中文。一次提交只做一件事，避免在一个 commit 中混合多种类型的改动。

---

## 附录：各层文件位置速查

### 业务模块结构

```
leatop-cdp-business-xxx/
├── leatop-cdp-business-xxx-api/           # 公共接口
│   └── src/main/java/.../
│       ├── business/XxxBusiness.java       # Feign 接口
│       ├── dto/XxxDTO.java                 # 数据传输对象
│       └── qo/XxxPageQo.java              # 查询参数对象
├── leatop-cdp-business-xxx-service/       # 业务实现
│   └── src/main/java/.../
│       ├── businessImpl/XxxBusinessImpl.java  # Business 实现
│       ├── service/XxxService.java            # Service 接口
│       ├── service/impl/XxxServiceImpl.java   # Service 实现
│       ├── dao/XxxDao.java                    # Mapper 接口
│       └── po/XxxPO.java                      # 数据库实体
│   └── src/main/resources/mapper/XxxDao.xml   # MyBatis XML
├── leatop-cdp-business-xxx-controller/    # REST 接口
│   └── src/main/java/.../controller/XxxController.java
├── leatop-cdp-business-xxx-boot-starter   # 单体部署启动器
└── leatop-cdp-business-xxx-cloud-starter  # 微服务部署启动器
```

### 框架基类位置

| 基类 | 位置 |
|------|------|
| `BasePo<T>` | `leatop-cdp-common-data` → `com.leatop.cdp.data.po` |
| `BaseDto` | `leatop-cdp-common-data` → `com.leatop.cdp.data.dto` |
| `PageQo` | `leatop-cdp-common-data` → `com.leatop.cdp.data.qo` |
| `Message<T>` | `leatop-cdp-common-data` → `com.leatop.cdp.data.message` |
| `BusException` | `leatop-cdp-common-data` → `com.leatop.cdp.data.exception` |
| `UncheckedException` | `leatop-cdp-common-data` → `com.leatop.cdp.data.exception` |
| `AddGroup` / `UpdateGroup` | `leatop-cdp-common-core` → `com.leatop.cdp.core.validate` |
| `@BusinessService` | `leatop-cdp-common-data` → `com.leatop.cdp.data.annotation` |
| `BeanUtil` | `leatop-cdp-common-core` → `com.leatop.cdp.util` |
| `GlobalExceptionHandler` | `leatop-cdp-common-core` → `com.leatop.cdp.core.handler` |

---

*本文档为 CDP 框架代码规范，配合 [注释规范](comment-spec.md) 和 [代码片段库](snippets/) 共同使用。*
