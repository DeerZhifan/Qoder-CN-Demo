# 知识库管理后台 - 技术方案文档

> **版本**: 1.0  
> **日期**: 2026-06-07  
> **作者**: 后端技术专家组  

---

## 一、技术选型概述

基于Spring Boot 3.x生态构建的知识库管理后台，采用以下核心技术栈：

| 技术组件 | 版本 | 说明 |
|---------|------|------|
| Java | 17 (LTS) | Spring Boot 3.x强制要求JDK 17+ |
| Spring Boot | 3.2.5 | 2024年发布的稳定LTS版本 |
| MyBatis-Plus | 3.5.9 | 与Spring Boot 3完全兼容的starter版本 |
| H2 Database | 2.2.224 | 开发环境内存数据库 |
| Lombok | 1.18.32 | 支持JDK 17+的最新稳定版 |
| JUnit 5 | 5.10.2 | Spring Boot 3.2内置版本 |
| Maven | 3.9.x | 项目构建工具 |

### 1.1 版本兼容性验证

**Spring Boot 3.2.x + MyBatis-Plus 3.5.9组合**是经过生产验证的稳定搭配：

- ✅ Spring Boot 3.2.5基于Spring Framework 6.1.x，全面支持Jakarta EE规范
- ✅ MyBatis-Plus 3.5.9提供专用的`mybatis-plus-spring-boot3-starter`，适配Spring Boot 3自动配置机制
- ✅ 已解决`factoryBeanObjectType`类型错误（MyBatis-Spring 3.0.3+修复）
- ✅ 支持Java 17密封类、模式匹配等新特性

**关键依赖注意事项**：
- ⚠️ 必须使用`mybatis-plus-spring-boot3-starter`而非旧版`mybatis-plus-boot-starter`
- ⚠️ Lombok版本不低于1.18.30，推荐1.18.32以避免JDK 17模块系统冲突
- ⚠️ 禁止在项目中重复引入MyBatis或MyBatis-Spring，避免版本冲突

---

## 二、Maven依赖清单

### 2.1 pom.xml完整配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.knowledgebase</groupId>
    <artifactId>knowledge-base-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>Knowledge Base Backend</name>
    <description>知识库管理后台</description>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <lombok.version>1.18.32</lombok.version>
        <h2.version>2.2.224</h2.version>
    </properties>

    <dependencies>
        <!-- ========== Web层 ========== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- ========== 持久层 ========== -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- H2数据库（仅开发环境） -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>${h2.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- ========== 工具类 ========== -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- 参数校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- ========== 测试框架 ========== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- JUnit 5 Jupiter（Spring Boot 3.2.5内置5.10.2，无需显式声明版本） -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
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

### 2.2 依赖说明

| 依赖项 | 作用域 | 说明 |
|--------|--------|------|
| `spring-boot-starter-web` | compile | Web容器（Tomcat）、Spring MVC、Jackson |
| `mybatis-plus-spring-boot3-starter` | compile | MyBatis-Plus核心功能+自动配置 |
| `h2` | runtime | 仅在运行时需要，不打包进JAR |
| `lombok` | provided | 编译期注解处理，不打包进JAR |
| `spring-boot-starter-validation` | compile | Jakarta Validation API实现 |
| `spring-boot-starter-test` | test | 包含JUnit 5、Mockito、AssertJ等 |

---

## 三、application.yml关键配置

### 3.1 完整配置文件示例

```yaml
server:
  port: 8080

spring:
  application:
    name: knowledge-base-backend
  
  # ========== H2数据库配置 ==========
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:kbdb;DB_CLOSE_DELAY=-1;MODE=MySQL
    username: sa
    password: 
  
  # H2控制台（仅dev profile启用）
  h2:
    console:
      enabled: true
      path: /h2-console
      settings:
        web-allow-others: false  # 生产环境必须为false
  
  # ========== MyBatis-Plus配置 ==========
  mybatis-plus:
    # Mapper XML文件位置
    mapper-locations: classpath*:/mapper/**/*.xml
    # 实体类包路径
    type-aliases-package: com.knowledgebase.entity
    # 全局配置
    global-config:
      db-config:
        # 主键策略：雪花算法
        id-type: ASSIGN_ID
        # 逻辑删除字段名
        logic-delete-field: deleted
        # 逻辑删除值：1表示已删除
        logic-delete-value: 1
        # 逻辑未删除值：0表示未删除
        logic-not-delete-value: 0
    # 配置项
    configuration:
      # 开启驼峰命名转换
      map-underscore-to-camel-case: true
      # 日志输出（开发环境）
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  
  # ========== Profile配置 ==========
  profiles:
    active: dev

# ========== 日志配置 ==========
logging:
  level:
    root: INFO
    com.knowledgebase: DEBUG
    com.baomidou.mybatisplus: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# ========== CORS跨域配置 ==========
cors:
  allowed-origins: http://localhost:5173,http://localhost:3000
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true
  max-age: 3600
```

### 3.2 多Profile配置

**application-dev.yml**（开发环境）：
```yaml
spring:
  h2:
    console:
      enabled: true
  
  mybatis-plus:
    configuration:
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

logging:
  level:
    com.knowledgebase: DEBUG
```

**application-prod.yml**（生产环境）：
```yaml
spring:
  h2:
    console:
      enabled: false  # 生产环境禁用H2控制台
  
  datasource:
    # 切换为MySQL/PostgreSQL等生产数据库
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/knowledge_base?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}

  mybatis-plus:
    configuration:
      log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl  # 使用SLF4J日志

logging:
  level:
    com.knowledgebase: INFO
    com.baomidou.mybatisplus: WARN
```

### 3.3 配置要点说明

#### H2数据库配置要点

1. **内存模式**：`jdbc:h2:mem:kbdb`表示数据存储在内存中，应用重启后数据丢失
2. **延迟关闭**：`DB_CLOSE_DELAY=-1`确保最后一个连接断开时不立即销毁数据库
3. **MySQL兼容模式**：`MODE=MySQL`使H2行为更接近MySQL，减少SQL语法差异
4. **控制台安全**：
   - 开发环境：`enabled: true`便于调试
   - 生产环境：必须设置`enabled: false`防止安全风险
   - `web-allow-others: false`限制只能本地访问

#### MyBatis-Plus配置要点

1. **逻辑删除**：通过`logic-delete-field`统一配置软删除字段
2. **主键策略**：`ASSIGN_ID`使用雪花算法生成分布式唯一ID
3. **驼峰转换**：自动将数据库下划线命名转换为Java驼峰命名
4. **日志控制**：开发环境使用StdOutImpl便于调试，生产环境切换为Slf4jImpl

---

## 四、统一响应格式设计

### 4.1 Result<T>类实现

```java
package com.knowledgebase.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 统一API响应结果封装
 * 
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    
    /**
     * 状态码
     * 200 - 成功
     * 400 - 请求参数错误
     * 401 - 未认证
     * 403 - 无权限
     * 404 - 资源不存在
     * 500 - 服务器内部错误
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null, System.currentTimeMillis());
    }
    
    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, System.currentTimeMillis());
    }
    
    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, System.currentTimeMillis());
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
    
    /**
     * 失败响应（默认500）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null, System.currentTimeMillis());
    }
    
    /**
     * 参数错误响应
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null, System.currentTimeMillis());
    }
    
    /**
     * 未认证响应
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null, System.currentTimeMillis());
    }
    
    /**
     * 无权限响应
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null, System.currentTimeMillis());
    }
    
    /**
     * 资源不存在响应
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null, System.currentTimeMillis());
    }
}
```

### 4.2 响应示例

**成功响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "Spring Boot入门教程",
    "content": "..."
  },
  "timestamp": 1717737600000
}
```

**失败响应**：
```json
{
  "code": 400,
  "message": "标题不能为空",
  "data": null,
  "timestamp": 1717737600000
}
```

**分页响应**（配合MyBatis-Plus Page对象）：
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {"id": 1, "title": "文章1"},
      {"id": 2, "title": "文章2"}
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  },
  "timestamp": 1717737600000
}
```

### 4.3 全局异常处理器

```java
package com.knowledgebase.common.exception;

import com.knowledgebase.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return Result.badRequest(message);
    }
    
    /**
     * 绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        log.warn("参数绑定失败: {}", message);
        return Result.badRequest(message);
    }
    
    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统内部错误，请联系管理员");
    }
}
```

---

## 五、软删除实现策略

### 5.1 实体类配置

使用MyBatis-Plus的`@TableLogic`注解实现软删除：

```java
package com.knowledgebase.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("kb_article")
public class Article {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String title;
    
    private String content;
    
    /**
     * 逻辑删除字段
     * 0 - 未删除
     * 1 - 已删除
     */
    @TableLogic
    private Integer deleted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
```

### 5.2 自动配置生效

在`application.yml`中已配置全局逻辑删除：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted          # 全局逻辑删除字段名
      logic-delete-value: 1                # 已删除标记值
      logic-not-delete-value: 0            # 未删除标记值
```

**效果**：
- 执行`deleteById()`时，自动生成`UPDATE kb_article SET deleted=1 WHERE id=? AND deleted=0`
- 执行`selectList()`时，自动附加`WHERE deleted=0`条件
- 执行`updateById()`时，不会更新已删除的记录

### 5.3 手动指定逻辑删除字段

如果某个表使用不同的字段名，可以在实体类上单独指定：

```java
@TableLogic(value = "0", delval = "1")
private Integer isDeleted;  // 自定义字段名
```

### 5.4 物理删除场景

如需真正删除数据（如数据清理任务），使用`BaseMapper`的`deletePhysicalById()`方法：

```java
// 物理删除（绕过逻辑删除）
articleMapper.deletePhysicalById(id);
```

或在Service层注入`SqlSession`执行原生SQL。

### 5.5 注意事项

⚠️ **重要约束**：
1. 逻辑删除字段必须是整数类型（Integer/Long），不建议使用Boolean
2. 已删除的数据不会出现在查询结果中，除非手动添加`.eq("deleted", 1)`条件
3. 关联查询时需要特别注意子查询中的逻辑删除条件
4. 统计总数（`selectCount`）也会自动过滤已删除数据

---

## 六、版本管理设计思路

### 6.1 版本表结构设计

每次发布创建新版本记录，支持知识库内容的版本追溯：

```sql
-- 知识库文章版本表
CREATE TABLE kb_article_version (
    id BIGINT PRIMARY KEY,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    version_no INT NOT NULL COMMENT '版本号（从1开始递增）',
    title VARCHAR(500) NOT NULL COMMENT '标题快照',
    content TEXT COMMENT '内容快照',
    change_summary VARCHAR(1000) COMMENT '变更说明',
    created_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_article_version (article_id, version_no DESC)
);
```

### 6.2 版本实体类

```java
package com.knowledgebase.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("kb_article_version")
public class ArticleVersion {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 关联的文章ID
     */
    private Long articleId;
    
    /**
     * 版本号（从1开始递增）
     */
    private Integer versionNo;
    
    /**
     * 标题快照
     */
    private String title;
    
    /**
     * 内容快照
     */
    private String content;
    
    /**
     * 变更说明
     */
    private String changeSummary;
    
    /**
     * 创建人
     */
    private String createdBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

### 6.3 版本管理Service实现

```java
package com.knowledgebase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledgebase.entity.Article;
import com.knowledgebase.entity.ArticleVersion;
import com.knowledgebase.mapper.ArticleMapper;
import com.knowledgebase.mapper.ArticleVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ArticleVersionService extends ServiceImpl<ArticleVersionMapper, ArticleVersion> {
    
    private final ArticleVersionMapper versionMapper;
    
    /**
     * 创建新版本
     * 
     * @param article 当前文章
     * @param changeSummary 变更说明
     * @return 新版本号
     */
    @Transactional
    public Integer createNewVersion(Article article, String changeSummary) {
        // 1. 查询当前最大版本号
        LambdaQueryWrapper<ArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleVersion::getArticleId, article.getId())
               .orderByDesc(ArticleVersion::getVersionNo)
               .last("LIMIT 1");
        
        ArticleVersion latestVersion = versionMapper.selectOne(wrapper);
        int nextVersionNo = (latestVersion != null ? latestVersion.getVersionNo() : 0) + 1;
        
        // 2. 创建新版本记录
        ArticleVersion newVersion = new ArticleVersion();
        newVersion.setArticleId(article.getId());
        newVersion.setVersionNo(nextVersionNo);
        newVersion.setTitle(article.getTitle());
        newVersion.setContent(article.getContent());
        newVersion.setChangeSummary(changeSummary);
        newVersion.setCreatedBy(getCurrentUserId()); // 从上下文获取当前用户
        newVersion.setCreateTime(LocalDateTime.now());
        
        versionMapper.insert(newVersion);
        
        return nextVersionNo;
    }
    
    /**
     * 查询文章的所有版本
     */
    public List<ArticleVersion> getVersionsByArticleId(Long articleId) {
        LambdaQueryWrapper<ArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleVersion::getArticleId, articleId)
               .orderByDesc(ArticleVersion::getVersionNo);
        return versionMapper.selectList(wrapper);
    }
    
    /**
     * 回滚到指定版本
     */
    @Transactional
    public void rollbackToVersion(Long articleId, Integer versionNo) {
        ArticleVersion targetVersion = getVersionById(articleId, versionNo);
        if (targetVersion == null) {
            throw new BusinessException("版本不存在");
        }
        
        // 更新文章内容为指定版本
        Article article = new Article();
        article.setId(articleId);
        article.setTitle(targetVersion.getTitle());
        article.setContent(targetVersion.getContent());
        article.setUpdateTime(LocalDateTime.now());
        
        // 这里调用ArticleService更新文章，会自动触发新版本创建
        // articleService.updateArticle(article, "回滚到版本" + versionNo);
    }
    
    private ArticleVersion getVersionById(Long articleId, Integer versionNo) {
        LambdaQueryWrapper<ArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleVersion::getArticleId, articleId)
               .eq(ArticleVersion::getVersionNo, versionNo);
        return versionMapper.selectOne(wrapper);
    }
    
    private String getCurrentUserId() {
        // TODO: 从SecurityContext或Session获取当前用户ID
        return "system";
    }
}
```

### 6.4 版本管理策略

**触发时机**：
- 文章创建时：自动生成版本1
- 文章更新时：创建新版本（版本号+1）
- 手动回滚时：创建新版本（内容为历史版本快照）

**存储策略**：
- 全量存储：每个版本保存完整的标题和内容快照
- 优点：查询历史版本简单快速，无需合并差异
- 缺点：占用存储空间较大（适合中小规模知识库）

**优化方案**（可选）：
- 对于大型文档，可采用差异存储（只保存变更部分）
- 定期归档旧版本到冷存储
- 限制保留的版本数量（如最多保留50个版本）

---

## 七、CORS跨域配置

### 7.1 CORS配置类

```java
package com.knowledgebase.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {
    
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;
    
    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;
    
    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;
    
    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;
    
    @Value("${cors.max-age:3600}")
    private long maxAge;
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的源
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        
        // 允许的HTTP方法
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        
        // 允许的请求头
        if ("*".equals(allowedHeaders)) {
            config.addAllowedHeader("*");
        } else {
            config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }
        
        // 是否允许携带凭证
        config.setAllowCredentials(allowCredentials);
        
        // 预检请求缓存时间（秒）
        config.setMaxAge(maxAge);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
```

### 7.2 前端对接说明

**Vue前端（端口5173）访问后端（端口8080）**：

```javascript
// vite.config.js
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

或使用CORS直接访问：

```javascript
// axios配置
axios.defaults.baseURL = 'http://localhost:8080'
axios.defaults.withCredentials = true  // 如需携带Cookie
```

---

## 八、项目结构规划

```
knowledge-base-backend/
├── src/main/java/com/knowledgebase/
│   ├── KnowledgeBaseApplication.java          # 启动类
│   ├── common/                                 # 公共模块
│   │   ├── result/                             # 统一响应
│   │   │   └── Result.java
│   │   ├── exception/                          # 异常处理
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   └── constant/                           # 常量定义
│   ├── config/                                 # 配置类
│   │   ├── CorsConfig.java                     # CORS配置
│   │   └── MyBatisPlusConfig.java              # MyBatis-Plus插件配置
│   ├── entity/                                 # 实体类
│   │   ├── Article.java
│   │   └── ArticleVersion.java
│   ├── mapper/                                 # Mapper接口
│   │   ├── ArticleMapper.java
│   │   └── ArticleVersionMapper.java
│   ├── service/                                # 业务层
│   │   ├── ArticleService.java
│   │   └── ArticleVersionService.java
│   ├── controller/                             # 控制器
│   │   └── ArticleController.java
│   └── dto/                                    # 数据传输对象
│       ├── ArticleDTO.java
│       └── ArticleQueryDTO.java
├── src/main/resources/
│   ├── application.yml                         # 主配置文件
│   ├── application-dev.yml                     # 开发环境配置
│   ├── application-prod.yml                    # 生产环境配置
│   └── mapper/                                 # MyBatis XML
│       └── ArticleMapper.xml
├── src/test/java/                              # 测试代码
└── pom.xml                                     # Maven配置
```

---

## 九、开发规范建议

### 9.1 代码规范

1. **实体类命名**：使用名词单数形式，如`Article`、`Category`
2. **Mapper命名**：实体名+Mapper后缀，如`ArticleMapper`
3. **Service命名**：实体名+Service后缀，如`ArticleService`
4. **Controller命名**：实体名+Controller后缀，如`ArticleController`
5. **DTO命名**：功能+DTO后缀，如`ArticleCreateDTO`、`ArticleQueryDTO`

### 9.2 接口设计规范

- RESTful风格：`GET /articles`、`POST /articles`、`PUT /articles/{id}`、`DELETE /articles/{id}`
- 统一使用前缀：`/api/v1/xxx`
- 所有接口返回`Result<T>`格式

### 9.3 数据库规范

- 表名：小写+下划线，如`kb_article`
- 字段名：小写+下划线，如`create_time`
- 必备字段：`id`（主键）、`create_time`、`update_time`、`deleted`（逻辑删除）

---

## 十、后续扩展方向

### 10.1 生产环境升级路径

1. **数据库迁移**：H2 → MySQL 8.0 / PostgreSQL 15
2. **缓存集成**：Redis（使用Spring Data Redis）
3. **认证授权**：集成Spring Security + JWT
4. **文档生成**：集成SpringDoc OpenAPI（Swagger 3）
5. **监控告警**：集成Actuator + Prometheus + Grafana

### 10.2 性能优化

1. **分页查询**：使用MyBatis-Plus的`Page`对象
2. **批量操作**：使用`saveBatch()`、`updateBatchById()`
3. **索引优化**：为常用查询字段添加数据库索引
4. **缓存策略**：热点数据使用Redis缓存

---

## 十一、参考资源

- [Spring Boot 3.2官方文档](https://docs.spring.io/spring-boot/docs/3.2.5/reference/html/)
- [MyBatis-Plus官方文档](https://baomidou.com/)
- [H2 Database官方文档](https://www.h2database.com/html/main.html)
- [Lombok官方文档](https://projectlombok.org/)
- [JUnit 5用户指南](https://junit.org/junit5/docs/current/user-guide/)

---

**文档版本历史**：

| 版本 | 日期 | 修改说明 |
|------|------|----------|
| 1.0 | 2026-06-07 | 初始版本，完成技术选型和方案设计 |

