# 初始化 CDP 业务模块

## 描述

在已有的 CDP 多模块项目中，新增一个完整的业务模块骨架。
生成 api、service、controller、boot-starter、cloud-starter 五个子模块及标准目录结构。

## 命名规则

```
业务模块：   {项目名称}-{模块名称}
子模块：     {项目名称}-{模块名称}-api / -service / -controller / -boot-starter / -cloud-starter
```

## 输入

请向用户确认以下信息：

1. **项目名称**（当前项目的 artifactId，如 `leatop-cdp-myapp`）
2. **模块名称**（英文，如 `order`）
3. **模块中文描述**（如 `订单管理`）
4. **首个业务实体名称**（如 `Order`）
5. **数据库表名**（如 `t_order`）
6. **API 路径前缀**（如 `order`）

---

## 模块结构概览

```
{项目名称}-{模块名称}/
├── pom.xml                                        # 聚合 POM
├── {项目名称}-{模块名称}-api/                      # Feign 接口、DTO、QO
├── {项目名称}-{模块名称}-service/                  # Business、Service、DAO、PO
├── {项目名称}-{模块名称}-controller/               # REST 端点
├── {项目名称}-{模块名称}-boot-starter/             # 单体部署自动配置
└── {项目名称}-{模块名称}-cloud-starter/            # 微服务部署自动配置
```

依赖链路：

```
boot-starter  → service + controller
cloud-starter → controller + api + openfeign
controller    → api + common-core
service       → api + common-starter
api           → common-data + feign + spring-webmvc
```

---

## 步骤 1：创建聚合 POM

文件路径：`{项目名称}-{模块名称}/pom.xml`

> parent 指向项目根 POM，通过 `dependencyManagement` 管理内部子模块版本。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.leatop.cdp</groupId>
        <artifactId>{项目名称}</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>{项目名称}-{模块名称}</artifactId>
    <packaging>pom</packaging>
    <modules>
        <module>{项目名称}-{模块名称}-api</module>
        <module>{项目名称}-{模块名称}-controller</module>
        <module>{项目名称}-{模块名称}-service</module>
        <module>{项目名称}-{模块名称}-boot-starter</module>
        <module>{项目名称}-{模块名称}-cloud-starter</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.leatop.cdp</groupId>
                <artifactId>{项目名称}-{模块名称}-api</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.leatop.cdp</groupId>
                <artifactId>{项目名称}-{模块名称}-service</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.leatop.cdp</groupId>
                <artifactId>{项目名称}-{模块名称}-controller</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

</project>
```

---

## 步骤 2：创建 api 子模块

### 2.1 pom.xml

文件路径：`{项目名称}-{模块名称}/{项目名称}-{模块名称}-api/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.leatop.cdp</groupId>
        <artifactId>{项目名称}-{模块名称}</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>{项目名称}-{模块名称}-api</artifactId>

    <dependencies>
        <!-- Open Feign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-openfeign-core</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>io.github.openfeign</groupId>
            <artifactId>feign-core</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
        </dependency>

        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>leatop-cdp-common-data</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 2.2 Business 接口

文件路径：`{项目名称}-{模块名称}-api/src/main/java/com/leatop/cdp/{模块名称}/business/{实体名}Business.java`

```java
package com.leatop.cdp.{模块名称}.business;

import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.{模块名称}.dto.{实体名}DTO;
import com.leatop.cdp.{模块名称}.qo.{实体名}PageQO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * {模块中文描述} - 业务接口
 */
@FeignClient(contextId = "cdp{实体名}Business", name = "${cdp.feign.{模块名称}.name:cdp-{模块名称}}",
        url = "${cdp.feign.{模块名称}.url:}", path = "${cdp.feign.{模块名称}.path:}")
public interface {实体名}Business {

    /**
     * 新增
     */
    @PostMapping("/{API路径前缀}/add")
    Message<String> add(@RequestBody {实体名}DTO dto);

    /**
     * 修改
     */
    @PostMapping("/{API路径前缀}/update")
    Message<String> update(@RequestBody {实体名}DTO dto);

    /**
     * 删除
     */
    @PostMapping("/{API路径前缀}/delete/{ids}")
    Message<String> delete(@PathVariable("ids") String ids);

    /**
     * 详情
     */
    @GetMapping("/{API路径前缀}/get/{id}")
    Message<{实体名}DTO> getById(@PathVariable("id") String id);

    /**
     * 分页查询
     */
    @PostMapping("/{API路径前缀}/listPage")
    Message<Page<{实体名}DTO>> listPage(@RequestBody {实体名}PageQO qo);
}
```

### 2.3 DTO

文件路径：`{项目名称}-{模块名称}-api/src/main/java/com/leatop/cdp/{模块名称}/dto/{实体名}DTO.java`

```java
package com.leatop.cdp.{模块名称}.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * {模块中文描述} - 数据传输对象
 */
@Data
@Accessors(chain = true)
public class {实体名}DTO implements Serializable {

    private String id;
}
```

### 2.4 QO

文件路径：`{项目名称}-{模块名称}-api/src/main/java/com/leatop/cdp/{模块名称}/qo/{实体名}PageQO.java`

```java
package com.leatop.cdp.{模块名称}.qo;

import com.leatop.cdp.data.qo.PageQo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * {模块中文描述} - 分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class {实体名}PageQO extends PageQo {
}
```

---

## 步骤 3：创建 service 子模块

### 3.1 pom.xml

文件路径：`{项目名称}-{模块名称}/{项目名称}-{模块名称}-service/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.leatop.cdp</groupId>
        <artifactId>{项目名称}-{模块名称}</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>{项目名称}-{模块名称}-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>leatop-cdp-common-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>{项目名称}-{模块名称}-api</artifactId>
        </dependency>
    </dependencies>

</project>
```

### 3.2 PO

文件路径：`{项目名称}-{模块名称}-service/src/main/java/com/leatop/cdp/{模块名称}/po/{实体名}PO.java`

```java
package com.leatop.cdp.{模块名称}.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.leatop.cdp.data.po.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * {模块中文描述} - 数据库实体
 */
@TableName("{数据库表名}")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class {实体名}PO extends BasePo<{实体名}PO> implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
}
```

### 3.3 DAO

文件路径：`{项目名称}-{模块名称}-service/src/main/java/com/leatop/cdp/{模块名称}/dao/{实体名}DAO.java`

```java
package com.leatop.cdp.{模块名称}.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leatop.cdp.{模块名称}.po.{实体名}PO;

/**
 * {模块中文描述} - Mapper 接口
 */
public interface {实体名}DAO extends BaseMapper<{实体名}PO> {
}
```

### 3.4 Service 接口

文件路径：`{项目名称}-{模块名称}-service/src/main/java/com/leatop/cdp/{模块名称}/service/{实体名}Service.java`

```java
package com.leatop.cdp.{模块名称}.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.{模块名称}.dto.{实体名}DTO;
import com.leatop.cdp.{模块名称}.po.{实体名}PO;
import com.leatop.cdp.{模块名称}.qo.{实体名}PageQO;

/**
 * {模块中文描述} - Service 接口
 */
public interface {实体名}Service extends IService<{实体名}PO> {

    /**
     * 新增
     */
    void add({实体名}DTO dto);

    /**
     * 修改
     */
    void update({实体名}DTO dto);

    /**
     * 删除
     */
    void deleteByIds(String ids);

    /**
     * 详情
     */
    {实体名}DTO getDetailById(String id);

    /**
     * 分页查询
     */
    Page<{实体名}DTO> listPage({实体名}PageQO qo);
}
```

### 3.5 Service 实现

文件路径：`{项目名称}-{模块名称}-service/src/main/java/com/leatop/cdp/{模块名称}/service/impl/{实体名}ServiceImpl.java`

```java
package com.leatop.cdp.{模块名称}.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.leatop.cdp.data.exception.BusException;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.{模块名称}.dao.{实体名}DAO;
import com.leatop.cdp.{模块名称}.dto.{实体名}DTO;
import com.leatop.cdp.{模块名称}.po.{实体名}PO;
import com.leatop.cdp.{模块名称}.qo.{实体名}PageQO;
import com.leatop.cdp.{模块名称}.service.{实体名}Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * {模块中文描述} - Service 实现
 */
@Service
public class {实体名}ServiceImpl extends ServiceImpl<{实体名}DAO, {实体名}PO> implements {实体名}Service {

    @Override
    public void add({实体名}DTO dto) {
        {实体名}PO po = BeanUtil.toBean(dto, {实体名}PO.class);
        this.save(po);
    }

    @Override
    public void update({实体名}DTO dto) {
        {实体名}PO po = BeanUtil.toBean(dto, {实体名}PO.class);
        this.updateById(po);
    }

    @Override
    public void deleteByIds(String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        this.removeByIds(idList);
    }

    @Override
    public {实体名}DTO getDetailById(String id) {
        {实体名}PO po = this.getById(id);
        if (po == null) {
            throw new BusException("数据不存在");
        }
        return BeanUtil.toBean(po, {实体名}DTO.class);
    }

    @Override
    public Page<{实体名}DTO> listPage({实体名}PageQO qo) {
        LambdaQueryWrapper<{实体名}PO> wrapper = new LambdaQueryWrapper<>();
        try (com.github.pagehelper.Page<{实体名}PO> ph = PageHelper.startPage(qo.getPage(), qo.getSize())) {
            if (StringUtils.isNotEmpty(qo.getOrderBy())) {
                ph.setOrderBy(qo.getOrderBy());
            }
            List<{实体名}PO> poList = this.list(wrapper);
            return Page.of(ph.getPageNum(), ph.getPageSize(), ph.getTotal(),
                    BeanUtil.copyToList(poList, {实体名}DTO.class));
        }
    }
}
```

### 3.6 Business 实现

文件路径：`{项目名称}-{模块名称}-service/src/main/java/com/leatop/cdp/{模块名称}/business/impl/{实体名}BusinessImpl.java`

```java
package com.leatop.cdp.{模块名称}.business.impl;

import com.leatop.cdp.data.annotation.BusinessService;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.{模块名称}.business.{实体名}Business;
import com.leatop.cdp.{模块名称}.dto.{实体名}DTO;
import com.leatop.cdp.{模块名称}.qo.{实体名}PageQO;
import com.leatop.cdp.{模块名称}.service.{实体名}Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {模块中文描述} - Business 实现
 */
@BusinessService
public class {实体名}BusinessImpl implements {实体名}Business {

    @Autowired
    private {实体名}Service {实体名小写}Service;

    @Override
    public Message<String> add({实体名}DTO dto) {
        {实体名小写}Service.add(dto);
        return Message.success("添加成功！");
    }

    @Override
    public Message<String> update({实体名}DTO dto) {
        {实体名小写}Service.update(dto);
        return Message.success("修改成功！");
    }

    @Override
    public Message<String> delete(String ids) {
        {实体名小写}Service.deleteByIds(ids);
        return Message.success("删除成功！");
    }

    @Override
    public Message<{实体名}DTO> getById(String id) {
        return Message.success({实体名小写}Service.getDetailById(id));
    }

    @Override
    public Message<Page<{实体名}DTO>> listPage({实体名}PageQO qo) {
        return Message.success({实体名小写}Service.listPage(qo));
    }
}
```

### 3.7 Flyway 迁移目录

创建目录：`{项目名称}-{模块名称}-service/src/main/resources/db/migration/mysql/`

---

## 步骤 4：创建 controller 子模块

### 4.1 pom.xml

文件路径：`{项目名称}-{模块名称}/{项目名称}-{模块名称}-controller/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.leatop.cdp</groupId>
        <artifactId>{项目名称}-{模块名称}</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>{项目名称}-{模块名称}-controller</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>{项目名称}-{模块名称}-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>leatop-cdp-common-core</artifactId>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 4.2 Controller

文件路径：`{项目名称}-{模块名称}-controller/src/main/java/com/leatop/cdp/{模块名称}/controller/{实体名}Controller.java`

```java
package com.leatop.cdp.{模块名称}.controller;

import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.{模块名称}.business.{实体名}Business;
import com.leatop.cdp.{模块名称}.dto.{实体名}DTO;
import com.leatop.cdp.{模块名称}.qo.{实体名}PageQO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * {模块中文描述} - REST 接口
 */
@RestController
@RequestMapping("/{API路径前缀}")
public class {实体名}Controller {

    @Autowired
    private {实体名}Business {实体名小写}Business;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Message<String> add(@RequestBody {实体名}DTO dto) {
        return {实体名小写}Business.add(dto);
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Message<String> update(@RequestBody {实体名}DTO dto) {
        return {实体名小写}Business.update(dto);
    }

    /**
     * 删除
     */
    @PostMapping("/delete/{ids}")
    public Message<String> delete(@PathVariable("ids") String ids) {
        return {实体名小写}Business.delete(ids);
    }

    /**
     * 详情
     */
    @GetMapping("/get/{id}")
    public Message<{实体名}DTO> getById(@PathVariable("id") String id) {
        return {实体名小写}Business.getById(id);
    }

    /**
     * 分页查询
     */
    @PostMapping("/listPage")
    public Message<Page<{实体名}DTO>> listPage(@RequestBody {实体名}PageQO qo) {
        return {实体名小写}Business.listPage(qo);
    }
}
```

---

## 步骤 5：创建 boot-starter 子模块

### 5.1 pom.xml

文件路径：`{项目名称}-{模块名称}/{项目名称}-{模块名称}-boot-starter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.leatop.cdp</groupId>
        <artifactId>{项目名称}-{模块名称}</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>{项目名称}-{模块名称}-boot-starter</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>{项目名称}-{模块名称}-service</artifactId>
        </dependency>
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>{项目名称}-{模块名称}-controller</artifactId>
        </dependency>
    </dependencies>

</project>
```

### 5.2 自动配置类

文件路径：`{项目名称}-{模块名称}-boot-starter/src/main/java/com/leatop/cdp/{模块名称}/Cdp{实体名}AutoConfiguration.java`

```java
package com.leatop.cdp.{模块名称};

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * {模块中文描述} - 单体部署自动配置
 */
@AutoConfiguration
@ComponentScan("com.leatop.cdp.{模块名称}")
@MapperScan("com.leatop.cdp.{模块名称}.dao")
@Slf4j
public class Cdp{实体名}AutoConfiguration {
}
```

### 5.3 自动配置注册

文件路径：`{项目名称}-{模块名称}-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.leatop.cdp.{模块名称}.Cdp{实体名}AutoConfiguration
```

---

## 步骤 6：创建 cloud-starter 子模块

### 6.1 pom.xml

文件路径：`{项目名称}-{模块名称}/{项目名称}-{模块名称}-cloud-starter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.leatop.cdp</groupId>
        <artifactId>{项目名称}-{模块名称}</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>{项目名称}-{模块名称}-cloud-starter</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>{项目名称}-{模块名称}-controller</artifactId>
        </dependency>
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>{项目名称}-{模块名称}-api</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
    </dependencies>

</project>
```

### 6.2 自动配置类

文件路径：`{项目名称}-{模块名称}-cloud-starter/src/main/java/com/leatop/cdp/{模块名称}/Cdp{实体名}CloudAutoConfiguration.java`

```java
package com.leatop.cdp.{模块名称};

import com.leatop.cdp.{模块名称}.business.{实体名}Business;
import com.leatop.cdp.{模块名称}.controller.{实体名}Controller;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * {模块中文描述} - 微服务部署自动配置
 */
@Configuration
@AutoConfiguration
@ComponentScan(basePackageClasses = {{实体名}Controller.class})
@EnableFeignClients(basePackageClasses = {{实体名}Business.class})
public class Cdp{实体名}CloudAutoConfiguration {
}
```

### 6.3 自动配置注册

文件路径：`{项目名称}-{模块名称}-cloud-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.leatop.cdp.{模块名称}.Cdp{实体名}CloudAutoConfiguration
```

---

## 步骤 7：注册到父工程

在项目根 `pom.xml` 的 `<modules>` 中添加：

```xml
<module>{项目名称}-{模块名称}</module>
```

在 `{项目名称}-main/pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- {模块中文描述}模块 -->
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>{项目名称}-{模块名称}-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 完成后提醒

1. 补充 DTO/PO 中的业务字段
2. 在 `db/migration/mysql/` 下创建 Flyway 建表脚本
3. 执行 `mvn clean install -DskipTests=true` 验证编译通过
