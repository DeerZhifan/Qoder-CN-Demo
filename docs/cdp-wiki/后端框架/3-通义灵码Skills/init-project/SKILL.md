# 初始化 CDP 应用项目

## 描述

基于 CDP 基础开发框架初始化一个完整的单体应用项目骨架。
生成标准的项目目录结构、Maven 配置、主启动类、配置文件和日志配置。

## 输入

请向用户确认以下信息：

1. **项目名称**（英文，如 `leatop-cdp-myapp`）
2. **GroupId**（如 `com.leatop.cdp`）
3. **基础包名**（如 `com.leatop.cdp.myapp`）
4. **CDP 框架版本**（默认 `1.0.3-SNAPSHOT`）
5. **数据库名称**（用于 application-dev.yaml 的 JDBC URL，如 `cdpmyapp`）

---

## 步骤 1：创建 pom.xml

> 继承 `leatop-cdp-parent` 作为父项目，自动获得 BOM 版本管理、Lombok、flyway.version 等配置，无需手动声明 `dependencyManagement`。

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

    <name>{项目名称}</name>
    <description>CDP单体应用项目</description>

    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <flyway.version>9.22.3</flyway.version>
    </properties>

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

## 步骤 2：创建主启动类

文件路径：`src/main/java/{包路径}/Application.java`

> `scanBasePackages` 必须设为 `"com.leatop.cdp"` 以扫描框架内置组件；`@MapperScan` 使用 `**.dao` 通配符匹配所有子包下的 Mapper。

```java
package {基础包名};

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CDP单体应用启动类
 */
@SpringBootApplication(scanBasePackages = "com.leatop.cdp")
@MapperScan("{基础包名}.**.dao")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 步骤 3：创建目录结构

在 `src/main/java/{包路径}/` 下创建以下包目录：

- `business/`
- `business/impl/`
- `controller/`
- `service/`
- `service/impl/`
- `dao/`
- `po/`
- `dto/`
- `qo/`

创建资源目录：
- `src/main/resources/db/migration/mysql/`

## 步骤 4：创建 application.yaml

文件路径：`src/main/resources/application.yaml`

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

## 步骤 5：创建 application-dev.yaml

文件路径：`src/main/resources/application-dev.yaml`

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

## 步骤 6：创建 logback-spring.xml

文件路径：`src/main/resources/logback-spring.xml`

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

## 步骤 7：创建 .gitignore

```gitignore
target/
*.iml
.idea/
*.class
*.jar
*.log
logs/
```

---

## 完成后提醒

1. 配置本项目所使用的 Maven 仓库地址
2. 修改 `application-dev.yaml` 中的 MySQL 连接地址、账号密码
3. 修改 `application-dev.yaml` 中的 Redis 连接地址、密码（Redis 为必需，缺少会导致启动失败）
