# 初始化 CDP 多模块应用项目

## 描述

基于 CDP 基础开发框架初始化一个多模块单体应用项目骨架。
生成父工程、main 主模块和一个示例业务模块（含 api/service/controller/boot-starter/cloud-starter 五子模块），以及标准配置文件。

## 命名规则

```
父工程：     {项目名称}
主模块：     {项目名称}-main
业务模块：   {项目名称}-{模块名称}
业务子模块： {项目名称}-{模块名称}-api / -service / -controller / -boot-starter / -cloud-starter
```

## 输入

请向用户确认以下信息：

1. **项目名称**（英文，如 `leatop-cdp-myapp`）
2. **CDP 框架版本**（默认 `1.0.3-SNAPSHOT`）
3. **数据库名称**（如 `cdpmyapp`）
4. **首个业务模块名称**（英文，如 `order`）
5. **业务模块中文描述**（如 `订单管理`）
6. **首个业务实体名称**（如 `Order`）
7. **数据库表名**（如 `t_order`）
8. **API 路径前缀**（如 `order`）

> 基础包名根据模块自动确定：主模块 `com.leatop.cdp`，业务模块 `com.leatop.cdp.{模块名称}`。

---

## 目标结构

```
{项目名称}/
├── pom.xml                                           # 父工程（pom 聚合）
├── .gitignore
├── {项目名称}-main/                                   # 主模块（Spring Boot 入口）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/leatop/cdp/Application.java
│       └── resources/
│           ├── application.yaml
│           ├── application-dev.yaml
│           └── logback-spring.xml
└── {项目名称}-{模块名称}/                              # 业务模块（pom 聚合）
    ├── pom.xml
    ├── {项目名称}-{模块名称}-api/
    ├── {项目名称}-{模块名称}-service/
    ├── {项目名称}-{模块名称}-controller/
    ├── {项目名称}-{模块名称}-boot-starter/
    └── {项目名称}-{模块名称}-cloud-starter/
```

---

## 步骤 1：创建父工程 pom.xml

文件路径：`pom.xml`

> 父工程继承 `leatop-cdp-parent`，`packaging` 为 `pom`，聚合 main 模块和业务模块。不声明 dependencies，所有依赖由子模块各自声明。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.leatop.cdp</groupId>
        <artifactId>leatop-cdp-parent</artifactId>
        <version>{CDP版本}</version>
    </parent>

    <artifactId>{项目名称}</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>{项目名称}-main</module>
        <module>{项目名称}-{模块名称}</module>
    </modules>

    <name>{项目名称}</name>
    <description>CDP单体应用项目（父工程）</description>

    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <flyway.version>9.22.3</flyway.version>
    </properties>

</project>
```

---

## 步骤 2：创建 main 主模块

### 2.1 pom.xml

文件路径：`{项目名称}-main/pom.xml`

> main 模块是唯一可运行的 Spring Boot 模块，引入 CDP 必选依赖和业务模块的 boot-starter。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.leatop.cdp</groupId>
        <artifactId>{项目名称}</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>{项目名称}-main</artifactId>
    <name>{项目名称}-main</name>
    <description>CDP单体应用主模块</description>

    <dependencies>
        <!-- CDP核心基础 -->
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>leatop-cdp-common-starter</artifactId>
        </dependency>

        <!-- CDP认证授权 -->
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>leatop-cdp-common-auth</artifactId>
        </dependency>

        <!-- 登录模块 -->
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>leatop-cdp-business-system-login</artifactId>
        </dependency>

        <!-- 系统管理boot-starter -->
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>leatop-cdp-business-system-boot-starter</artifactId>
        </dependency>

        <!-- 日志模块 -->
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>leatop-cdp-business-log-boot-starter</artifactId>
        </dependency>

        <!-- Flyway数据库迁移 -->
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>leatop-cdp-base-flyway</artifactId>
        </dependency>

        <!-- MySQL驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>

        <!-- {模块中文描述}模块 -->
        <dependency>
            <groupId>com.leatop.cdp</groupId>
            <artifactId>{项目名称}-{模块名称}-boot-starter</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

### 2.2 启动类

文件路径：`{项目名称}-main/src/main/java/com/leatop/cdp/Application.java`

```java
package com.leatop.cdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CDP单体应用启动类
 */
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@MapperScan("com.leatop.cdp.**.dao")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2.3 application.yaml

文件路径：`{项目名称}-main/src/main/resources/application.yaml`

```yaml
server:
  port: 28080

spring:
  application:
    name: ${APP_NAME:{项目名称}}
  servlet:
    multipart:
      max-file-size: 150MB
      max-request-size: 150MB
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8

  flyway:
    enabled: ${FLYWAY_ENABLED:false}
    clean-disabled: true
    baseline-on-migrate: true
    validate-on-migrate: false
    baseline-version: 1.0.0
    locations: classpath:db/migration/{vendor}

  profiles:
    active: dev

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      table-underline: true
      id-type: ASSIGN_ID
```

### 2.4 application-dev.yaml

文件路径：`{项目名称}-main/src/main/resources/application-dev.yaml`

```yaml
spring:
  flyway:
    enabled: true

  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      filters: stat
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: ${DB_URL:jdbc:mysql://172.17.1.28:3306/{数据库名称}?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&allowMultiQueries=true&serverTimezone=Asia/Shanghai&useSSL=false}
      username: ${DB_USERNAME:cdp}
      password: ${DB_PASSWORD:yfzx@2025}
      validation-query: SELECT 'x' FROM DUAL
      initial-size: 1
      min-idle: 1
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: false
      max-pool-prepared-statement-per-connection-size: 20
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin
        reset-enable: false
        allow: ""
      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 1000
          merge-sql: true
        wall:
          enabled: false
          config:
            drop-table-allow: false
            drop-view-allow: true
            none-base-statement-allow: true

  data:
    redis:
      host: 172.17.1.28
      port: 6379
      password: "!QAZ2wsx3edc"
      database: 10

  cache:
    type: redis
    cache-names: app-cache
    redis:
      use-key-prefix: true
      key-prefix: "${spring.application.name}-"
      time-to-live: 100000
      cache-null-values: false

logging:
  level:
    root: info
    com.leatop.cdp: debug
```

### 2.5 logback-spring.xml

文件路径：`{项目名称}-main/src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="false">

    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="app"/>
    <springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="./logs"/>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <charset>UTF-8</charset>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [tid=%X{tid}] -- [%15.15t] %-40.40logger{39}: %m%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>7</maxHistory>
            <maxFileSize>50MB</maxFileSize>
            <totalSizeCap>2GB</totalSizeCap>
            <cleanHistoryOnStart>true</cleanHistoryOnStart>
        </rollingPolicy>
        <encoder>
            <charset>UTF-8</charset>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [tid=%X{tid}] -- [%15.15t] %-40.40logger{39}: %m%n</pattern>
        </encoder>
    </appender>

    <springProfile name="dev,local">
        <root level="DEBUG">
            <appender-ref ref="STDOUT"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>

    <springProfile name="test">
        <root level="INFO">
            <appender-ref ref="STDOUT"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>

</configuration>
```

---

## 步骤 3：创建业务模块聚合 POM

文件路径：`{项目名称}-{模块名称}/pom.xml`

> 业务模块的 parent 指向项目根 POM。通过 `dependencyManagement` 管理内部子模块版本，子模块间引用不写版本号。

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

## 步骤 4：创建 api 子模块

### 4.1 pom.xml

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

### 4.2 Business 接口

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

### 4.3 DTO

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

### 4.4 QO

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

## 步骤 5：创建 service 子模块

### 5.1 pom.xml

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

### 5.2 PO

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

### 5.3 DAO

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

### 5.4 Service 接口

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

### 5.5 Service 实现

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

### 5.6 Business 实现

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

### 5.7 Flyway 迁移目录

创建目录：`{项目名称}-{模块名称}-service/src/main/resources/db/migration/mysql/`

---

## 步骤 6：创建 controller 子模块

### 6.1 pom.xml

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

### 6.2 Controller

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

## 步骤 7：创建 boot-starter 子模块

### 7.1 pom.xml

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

### 7.2 自动配置类

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

### 7.3 自动配置注册

文件路径：`{项目名称}-{模块名称}-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.leatop.cdp.{模块名称}.Cdp{实体名}AutoConfiguration
```

---

## 步骤 8：创建 cloud-starter 子模块

### 8.1 pom.xml

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

### 8.2 自动配置类

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

### 8.3 自动配置注册

文件路径：`{项目名称}-{模块名称}-cloud-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.leatop.cdp.{模块名称}.Cdp{实体名}CloudAutoConfiguration
```

---

## 步骤 9：创建 .gitignore

文件路径：`.gitignore`

```gitignore
target/
*.iml
.idea/
*.class
*.jar
*.log
logs/
.flattened-pom.xml
```

---

## 完成后提醒

1. 配置本项目所使用的 Maven 仓库地址
2. 修改 `application-dev.yaml` 中的 MySQL 连接地址、账号密码
3. 修改 `application-dev.yaml` 中的 Redis 连接地址、密码（Redis 为必需，缺少会导致启动失败）
4. 补充 DTO/PO 中的业务字段，在 `db/migration/mysql/` 下创建 Flyway 建表脚本
5. 执行 `mvn clean install -DskipTests=true` 验证编译通过
