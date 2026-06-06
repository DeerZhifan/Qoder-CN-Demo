---
trigger: when_referenced
knowledge_source:
  - cdp-coding-standards
  - cdp-faq
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 创建或修改 `src/test/java` 下的测试类
- 使用 `@SpringBootTest`、`@WebMvcTest`、`@DataJpaTest` 等 Spring 测试注解
- 使用 MockMvc 进行 Controller 层测试
- 使用 `@MockBean`、`@SpyBean` 进行 Mock 注入
- 编写单元测试或集成测试

---

## 前置依赖

1. Maven 依赖（Spring Boot Starter 已包含，通常无需额外引入）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

2. 测试框架由 `spring-boot-starter-test` 统一提供，包含 JUnit 5、Mockito、AssertJ、MockMvc 等。

---

## 配置要点

### 测试类位置

- 测试类放在 `src/test/java` 目录下，包路径与被测类保持一致
- 示例：被测类 `com.leatop.example.service.impl.NewsItemServiceImpl` 对应测试类 `com.leatop.example.service.impl.NewsItemServiceImplTest`

### 测试类命名

| 测试类型 | 命名规则 | 示例 |
|----------|----------|------|
| 单元测试 | `XxxTest` | `NewsItemServiceImplTest` |
| 集成测试 | `XxxIT` 或 `XxxIntegrationTest` | `NewsItemControllerIT` |

### 测试方法命名

- 使用中文或英文描述测试意图，推荐格式：`test_方法名_场景_预期结果`
- 或使用 `@DisplayName` 注解标注中文描述

```java
@Test
@DisplayName("根据ID查询新闻 - 存在时返回详情")
void test_queryById_whenExists_shouldReturnDto() { ... }

@Test
@DisplayName("新增新闻 - 标题为空时抛出异常")
void test_add_whenTitleBlank_shouldThrowException() { ... }
```

---

## 代码模式

### 推荐写法

**Service 层单元测试：**

```java
package com.leatop.example.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("新闻条目服务测试")
class NewsItemServiceImplTest {

    @Mock
    private NewsItemDao newsItemDao;

    @InjectMocks
    private NewsItemServiceImpl newsItemService;

    @Test
    @DisplayName("根据ID查询 - 数据存在时返回DTO")
    void test_queryById_whenExists_shouldReturnDto() {
        // given
        NewsItemPo po = new NewsItemPo().setId("1").setTitle("测试标题");
        when(newsItemDao.selectById("1")).thenReturn(po);

        // when
        Message<NewsItemDto> result = newsItemService.queryById("1");

        // then
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getTitle()).isEqualTo("测试标题");
        verify(newsItemDao).selectById("1");
    }
}
```

**Controller 层集成测试（MockMvc）：**

```java
package com.leatop.example.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsItemController.class)
@DisplayName("新闻条目控制器测试")
class NewsItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsItemService newsItemService;

    @Test
    @DisplayName("查询详情接口 - 正常返回")
    void test_queryById_shouldReturn200() throws Exception {
        // given
        NewsItemDto dto = new NewsItemDto();
        dto.setId("1");
        dto.setTitle("测试标题");
        when(newsItemService.queryById("1"))
            .thenReturn(Message.success(dto));

        // when & then
        mockMvc.perform(get("/news_research/news_item/queryById")
                .param("id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("测试标题"));
    }
}
```

**测试数据管理：**

- 使用 `@BeforeEach` 初始化测试数据，保证每个测试方法独立
- 测试数据使用固定值，不使用无种子的随机数据
- 集成测试使用独立的测试数据库或 H2 内存数据库

```java
@BeforeEach
void setUp() {
    testDto = new NewsItemDto();
    testDto.setId("test-001");
    testDto.setTitle("单元测试标题");
    testDto.setStatus(0);
}
```

**断言风格：**

- 推荐使用 AssertJ（`assertThat`）进行断言，语义更清晰
- 避免使用 JUnit 4 的 `Assert.assertEquals` 旧写法

```java
// 推荐：AssertJ
assertThat(result).isNotNull();
assertThat(result.getTitle()).isEqualTo("期望值");
assertThat(list).hasSize(3).extracting("name").contains("测试");

// 也可使用 JUnit 5
assertEquals("期望值", result.getTitle());
assertNotNull(result);
```

### 禁止事项

- **禁止测试连接生产数据库** -- 测试环境必须使用独立的测试数据库或内存数据库（H2），绝不能连接生产或预发环境数据库
- **禁止使用无种子的随机测试数据** -- 随机数据导致测试不可重现，应使用固定值或带种子的 Random
- **禁止测试方法之间存在执行顺序依赖** -- 每个测试方法必须独立，不依赖其他测试的执行结果或副作用
- **禁止在测试中硬编码环境相关配置** -- 如 IP、端口、文件路径等，应通过 `@TestPropertySource` 或 `application-test.yaml` 管理
- **禁止忽略失败的测试** -- 不要通过 `@Disabled` 长期屏蔽失败测试，应及时修复或删除
- **禁止测试类中编写业务逻辑** -- 测试类只负责验证行为，不应包含被复用的业务方法
- **禁止省略断言** -- 每个测试方法必须包含至少一个断言，不能只调用方法不验证结果
