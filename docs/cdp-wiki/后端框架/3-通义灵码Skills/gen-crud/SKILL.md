# 生成标准 CRUD 代码

## 描述

基于 CDP 框架的四层架构（Controller -> Business -> Service -> DAO），根据用户提供的实体信息，生成完整的标准 CRUD 代码文件，包括 PO、DTO、QO、DAO、Service 接口、Service 实现、Business 接口、Business 实现、Controller 共 9 个文件。

## 输入

请向用户确认以下信息：

1. **实体中文名**（如 `新闻条目`）
2. **实体英文名**（UpperCamelCase，如 `NewsItem`）
3. **数据库表名**（如 `news_item`）
4. **所在模块/基础包名**（如 `com.leatop.example`）
5. **字段列表**（每个字段包含：字段名、Java 类型、数据库列名、中文说明、是否查询条件、查询方式 like/eq）
6. **Controller 路径前缀**（如 `/news_research/news_item`）

---

## 步骤 1：生成 PO 文件

文件路径：`src/main/java/{包路径}/po/{实体名}Po.java`

```java
package {基础包名}.po;

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
 * {实体中文名} PO
 */
@Accessors(chain = true)
@Data
@TableName("{表名}")
public class {实体名}Po implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键（UUID）
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    // --- 根据字段列表逐个生成 ---
    /**
     * {字段中文说明}
     */
    @TableField(value = "{数据库列名}")
    private {Java类型} {字段名};

    /**
     * 租户ID
     */
    @TableField(value = "tenant_id")
    private String tenantId;

    /**
     * 创建时间
     */
    @TableField(value = "create_gmt")
    private LocalDateTime createGmt;

    /**
     * 更新时间
     */
    @TableField(value = "update_gmt")
    private LocalDateTime updateGmt;
}
```

> 每个字段使用 `@TableField` 标注数据库列名，使用 Javadoc 注释说明字段含义。`tenantId`、`createGmt`、`updateGmt` 为标准字段，必须包含。

---

## 步骤 2：生成 DTO 文件

文件路径：`src/main/java/{包路径}/dto/{实体名}Dto.java`

```java
package {基础包名}.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * {实体中文名} DTO
 */
@Accessors(chain = true)
@Data
public class {实体名}Dto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    // --- 根据字段列表逐个生成（不含 tenantId） ---
    /**
     * {字段中文说明}
     */
    private {Java类型} {字段名};

    /**
     * 创建时间
     */
    private LocalDateTime createGmt;

    /**
     * 更新时间
     */
    private LocalDateTime updateGmt;
}
```

> DTO 不含数据库注解（`@TableName`、`@TableField`），不暴露 `tenantId`。字段与 PO 保持语义一致，可裁剪。

---

## 步骤 3：生成 QO 文件

文件路径：`src/main/java/{包路径}/qo/{实体名}PageQo.java`

```java
package {基础包名}.qo;

import com.leatop.cdp.data.qo.PageQo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * {实体中文名} 分页查询参数
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Data
public class {实体名}PageQo extends PageQo {

    // --- 仅包含标记为"查询条件"的字段 ---
    /**
     * {字段中文说明}（{查询方式}查询）
     */
    private {Java类型} {字段名};
}
```

> QO 继承 `PageQo`，自动获得 `page`、`size`、`orderBy`、`searchKey` 分页参数。只放查询条件字段，字段为 null 时该条件不生效。

---

## 步骤 4：生成 DAO 文件

文件路径：`src/main/java/{包路径}/dao/{实体名}Dao.java`

```java
package {基础包名}.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import {基础包名}.po.{实体名}Po;
import org.apache.ibatis.annotations.Mapper;

/**
 * {实体中文名} DAO
 */
@Mapper
public interface {实体名}Dao extends BaseMapper<{实体名}Po> {

    // 简单查询直接用 BaseMapper 提供的方法，复杂查询在这里声明，XML 中实现
}
```

---

## 步骤 5：生成 Service 接口

文件路径：`src/main/java/{包路径}/service/{实体名}Service.java`

```java
package {基础包名}.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leatop.cdp.data.model.Page;
import {基础包名}.dto.{实体名}Dto;
import {基础包名}.po.{实体名}Po;
import {基础包名}.qo.{实体名}PageQo;

/**
 * {实体中文名} Service 接口
 */
public interface {实体名}Service extends IService<{实体名}Po> {

    /**
     * 新增
     */
    void add({实体名}Dto dto);

    /**
     * 修改
     */
    void update({实体名}Dto dto);

    /**
     * 根据 ID 查询详情
     */
    {实体名}Dto queryById(String id);

    /**
     * 删除（支持批量，多个 id 用 ',' 分隔）
     */
    void delete(String ids);

    /**
     * 分页查询
     */
    Page<{实体名}Dto> listPage({实体名}PageQo qo);
}
```

> Service 接口继承 `IService<PO类型>`，获得 MyBatis-Plus 内置 CRUD 能力。业务方法不返回 `Message<T>`，`Message` 包装在 Business 层完成。

---

## 步骤 6：生成 Service 实现

文件路径：`src/main/java/{包路径}/service/impl/{实体名}ServiceImpl.java`

```java
package {基础包名}.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.leatop.cdp.data.exception.BusException;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.util.BeanUtil;
import com.leatop.cdp.util.StrUtil;
import {基础包名}.dao.{实体名}Dao;
import {基础包名}.dto.{实体名}Dto;
import {基础包名}.po.{实体名}Po;
import {基础包名}.qo.{实体名}PageQo;
import {基础包名}.service.{实体名}Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * {实体中文名} Service 实现
 */
@Service
public class {实体名}ServiceImpl extends ServiceImpl<{实体名}Dao, {实体名}Po>
        implements {实体名}Service {

    @Override
    public void add({实体名}Dto dto) {
        {实体名}Po po = BeanUtil.copyProperties(dto, {实体名}Po.class);
        save(po);
    }

    @Override
    public void update({实体名}Dto dto) {
        {实体名}Po po = BeanUtil.copyProperties(dto, {实体名}Po.class);
        updateById(po);
    }

    @Override
    public {实体名}Dto queryById(String id) {
        {实体名}Po po = getById(id);
        if (Objects.isNull(po)) {
            throw new BusException("数据不存在，id=" + id);
        }
        return BeanUtil.copyProperties(po, {实体名}Dto.class);
    }

    @Override
    @Transactional
    public void delete(String ids) {
        List<String> idList = StrUtil.split(ids, ',');
        if (CollUtil.isEmpty(idList)) {
            throw new BusException("ids 不能为空");
        }
        removeBatchByIds(idList);
    }

    @Override
    public Page<{实体名}Dto> listPage({实体名}PageQo qo) {
        com.github.pagehelper.Page<{实体名}Po> page =
                PageHelper.startPage(qo.getPage(), qo.getSize());

        LambdaQueryWrapper<{实体名}Po> wrapper = new LambdaQueryWrapper<>();
        // --- 根据字段列表生成查询条件 ---
        // 模糊查询示例：
        // wrapper.like(StrUtil.isNotBlank(qo.getXxx()), {实体名}Po::getXxx, qo.getXxx());
        // 精确查询示例：
        // wrapper.eq(Objects.nonNull(qo.getXxx()), {实体名}Po::getXxx, qo.getXxx());
        // 排序
        wrapper.orderByDesc({实体名}Po::getCreateGmt);

        List<{实体名}Po> list = getBaseMapper().selectList(wrapper);
        List<{实体名}Dto> results = BeanUtil.copyToList(list, {实体名}Dto.class);
        return Page.of(page.getPageNum(), page.getPageSize(), page.getTotal(), results);
    }
}
```

> 根据用户提供的查询方式（like/eq）生成对应的 wrapper 条件。模糊查询用 `wrapper.like()`，精确查询用 `wrapper.eq()`，字段不为空时才加条件。

---

## 步骤 7：生成 Business 接口

文件路径：`src/main/java/{包路径}/business/{实体名}Business.java`

```java
package {基础包名}.business;

import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import {基础包名}.dto.{实体名}Dto;
import {基础包名}.qo.{实体名}PageQo;

/**
 * {实体中文名} Business 接口
 */
public interface {实体名}Business {

    /**
     * 新增
     */
    Message<String> add({实体名}Dto dto);

    /**
     * 修改
     */
    Message<String> update({实体名}Dto dto);

    /**
     * 根据 ID 查询详情
     */
    Message<{实体名}Dto> queryById(String id);

    /**
     * 删除（支持批量，多个 id 用 ',' 分隔）
     */
    Message<String> delete(String ids);

    /**
     * 分页查询
     */
    Message<Page<{实体名}Dto>> listPage({实体名}PageQo qo);
}
```

---

## 步骤 8：生成 Business 实现

文件路径：`src/main/java/{包路径}/business/impl/{实体名}BusinessImpl.java`

```java
package {基础包名}.business.impl;

import com.leatop.cdp.data.annotation.BusinessService;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import {基础包名}.business.{实体名}Business;
import {基础包名}.dto.{实体名}Dto;
import {基础包名}.qo.{实体名}PageQo;
import {基础包名}.service.{实体名}Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {实体中文名} Business 实现
 */
@BusinessService
public class {实体名}BusinessImpl implements {实体名}Business {

    @Autowired
    private {实体名}Service {实体名小驼峰}Service;

    @Override
    public Message<String> add({实体名}Dto dto) {
        {实体名小驼峰}Service.add(dto);
        return Message.success("添加成功！");
    }

    @Override
    public Message<String> update({实体名}Dto dto) {
        {实体名小驼峰}Service.update(dto);
        return Message.success("修改成功！");
    }

    @Override
    public Message<{实体名}Dto> queryById(String id) {
        return Message.success({实体名小驼峰}Service.queryById(id));
    }

    @Override
    public Message<String> delete(String ids) {
        {实体名小驼峰}Service.delete(ids);
        return Message.success("删除成功！");
    }

    @Override
    public Message<Page<{实体名}Dto>> listPage({实体名}PageQo qo) {
        return Message.success({实体名小驼峰}Service.listPage(qo));
    }
}
```

> `@BusinessService` 是 CDP 自定义注解（内含 `@Service`）。标准成功消息：`添加成功！` / `修改成功！` / `删除成功！`。`{实体名小驼峰}` 指将实体英文名首字母小写，如 `NewsItem` -> `newsItem`。

---

## 步骤 9：生成 Controller 文件

文件路径：`src/main/java/{包路径}/controller/{实体名}Controller.java`

```java
package {基础包名}.controller;

import com.leatop.cdp.core.validate.AddGroup;
import com.leatop.cdp.core.validate.UpdateGroup;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import {基础包名}.business.{实体名}Business;
import {基础包名}.dto.{实体名}Dto;
import {基础包名}.qo.{实体名}PageQo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * {实体中文名} Controller
 */
@RestController
@RequestMapping("{Controller路径前缀}")
public class {实体名}Controller {

    @Autowired
    private {实体名}Business {实体名小驼峰}Business;

    /**
     * 新增
     *
     * @param dto 新增参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public Message<String> add(@RequestBody @Validated(AddGroup.class) {实体名}Dto dto) {
        return {实体名小驼峰}Business.add(dto);
    }

    /**
     * 修改
     *
     * @param dto 修改参数
     * @return 操作结果
     */
    @PostMapping("/update")
    public Message<String> update(@RequestBody @Validated(UpdateGroup.class) {实体名}Dto dto) {
        return {实体名小驼峰}Business.update(dto);
    }

    /**
     * 根据 ID 查询详情
     *
     * @param id 主键 ID
     * @return 详情
     */
    @GetMapping("/get/{id}")
    public Message<{实体名}Dto> queryById(@PathVariable String id) {
        return {实体名小驼峰}Business.queryById(id);
    }

    /**
     * 删除（支持批量，多个 id 用 ',' 分隔）
     *
     * @param ids ID 列表
     * @return 操作结果
     */
    @PostMapping("/delete/{ids}")
    public Message<String> delete(@PathVariable("ids") String ids) {
        return {实体名小驼峰}Business.delete(ids);
    }

    /**
     * 分页查询
     *
     * @param qo 查询参数
     * @return 分页列表
     */
    @PostMapping("/listPage")
    public Message<Page<{实体名}Dto>> listPage(@Validated {实体名}PageQo qo) {
        return {实体名小驼峰}Business.listPage(qo);
    }
}
```

> Controller 只做参数接收、参数校验、调用 Business、返回结果。不在 Controller 中写业务逻辑。每个方法使用 Javadoc 注释（供 Smart-doc 生成接口文档）。

---

## 完成后提醒

1. 确认启动类 `@MapperScan` 已覆盖生成的 DAO 包路径
2. 如需参数校验，在 DTO 字段上添加 `javax.validation` 注解（如 `@NotBlank`、`@Size`），并配合 `AddGroup` / `UpdateGroup` 分组
3. 如有复杂 SQL 查询需求，在 DAO 接口中声明方法，并在 `src/main/resources/mapper/{实体名}Dao.xml` 中编写 SQL
4. Service 层中 PO/DTO 转换使用 `com.leatop.cdp.util.BeanUtil`，不要手动逐字段赋值
5. 如需对应的建表 SQL 脚本，可使用 `gen-flyway-migration` 技能生成 Flyway 迁移脚本
