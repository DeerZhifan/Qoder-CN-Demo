---
trigger: when_referenced
knowledge_source:
  - cdp-design-jasypt
  - cdp-module-jasypt
---

## 适用场景

当代码涉及以下内容时，本规则自动生效：

- 引入 `leatop-cdp-common-jasypt` 依赖
- 配置文件中使用 `ENC(...)` 格式包裹敏感值
- 配置 `jasypt.encryptor` 相关参数
- 使用 `Sm4CodecUtil` 进行加解密操作
- 涉及数据库密码、Redis 密码、API 密钥等敏感配置项

---

## 前置依赖

1. 组件通过 `leatop-cdp-common-starter` 自动引入，无需额外添加 Maven 依赖。

2. 需要在 `application.yaml` 中配置加密器和密钥：

```yaml
jasypt:
  encryptor:
    bean: customEncryptor           # 使用 CDP 自定义加密器（SM4 国密算法）
    key: ${JASYPT_KEY}              # 加密密钥（>= 16 字节），推荐通过环境变量传递
```

3. `Sm4CodecUtil` 位于 `leatop-cdp-common-util` 模块，用于生成密文。

---

## 配置要点

### ENC() 格式

在 YAML 配置中，使用 `ENC(密文)` 包裹加密后的值。Spring Boot 启动时 Jasypt 自动识别并调用 `CustomEncryptor` 进行 SM4 解密，业务代码读到的是明文值。

```yaml
spring:
  datasource:
    password: ENC(a1b2c3d4e5f6...)    # SM4 加密后的十六进制密文
  data:
    redis:
      password: ENC(f6e5d4c3b2a1...)
```

### 密钥管理

密钥通过 `jasypt.encryptor.key` 配置，支持所有 Spring 属性源（环境变量、JVM 参数、配置中心等）：

**方式一：环境变量传递（推荐）**

```yaml
jasypt:
  encryptor:
    key: ${JASYPT_KEY}
```

```bash
export JASYPT_KEY=your-16-bytes-or-more-key
java -jar app.jar
```

**方式二：JVM 参数传递**

```bash
java -Djasypt.encryptor.key=your-16-bytes-or-more-key -jar app.jar
```

### 算法说明

`CustomEncryptor` 使用 SM4 国密对称加密算法（GB/T 32907-2016），ECB 模式，Hex 编码输出。密钥长度必须 >= 16 字节，否则启动时抛出 `IllegalArgumentException`。

---

## 代码模式

### 推荐写法

**生成加密值 -- 使用 Sm4CodecUtil：**

```java
import com.leatop.cdp.util.Sm4CodecUtil;

String key = "your-16-bytes-or-more-key";
String plaintext = "myDatabasePassword123";
String encrypted = Sm4CodecUtil.encodeSms4ToHex(key, plaintext);
System.out.println(encrypted);  // 输出十六进制密文，填入 ENC() 中
```

**在配置文件中使用加密值：**

```yaml
spring:
  datasource:
    username: root
    password: ENC(a1b2c3d4e5f6...)    # 将上一步的 encrypted 值填入
```

**密钥通过环境变量传递：**

```yaml
jasypt:
  encryptor:
    bean: customEncryptor
    key: ${JASYPT_KEY}
```

### 禁止事项

- **禁止将加密密钥硬编码在配置文件中提交到代码仓库** -- 密钥应通过环境变量（`${JASYPT_KEY}`）或 JVM 参数传递，防止密钥泄露
- **禁止在配置文件中保留敏感信息的明文值并提交到版本控制** -- 数据库密码、Redis 密码、API 密钥等必须使用 `ENC(...)` 格式加密后提交
- **禁止使用短于 16 字节的密钥** -- SM4 算法要求密钥 >= 16 字节，否则 `CustomEncryptor` 在 `afterPropertiesSet()` 阶段会抛出 `IllegalArgumentException` 阻止应用启动
- **禁止绕过 `CustomEncryptor` 直接使用 Jasypt 默认加密器** -- CDP 强制使用 SM4 国密算法以满足信创合规要求，`jasypt.encryptor.bean` 必须指定为 `customEncryptor`
- **禁止在业务代码中手动解密 `ENC(...)` 值** -- Jasypt 在 Spring Boot 启动阶段自动完成解密，业务代码通过 `@Value` 或 `Environment` 读取时已经是明文
- **禁止在日志中输出解密后的敏感配置值** -- 避免敏感信息泄露到日志文件
