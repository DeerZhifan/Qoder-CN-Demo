# 如何使用 CDP 配置加密组件

## 概述

配置加密组件（`leatop-cdp-common-jasypt`）基于 Jasypt（Java Simplified Encryption）框架，使用 SM4 国密算法对配置文件中的敏感信息（数据库密码、Redis 密码、API 密钥等）进行加密，防止明文泄露。

## 启用方式

组件通过 `leatop-cdp-common-starter` 自动引入，无需额外添加依赖。

**在 `application.yaml` 中配置加密密钥：**

```yaml
jasypt:
  encryptor:
    bean: customEncryptor           # 使用 CDP 自定义加密器
    key: your-16-bytes-or-more-key  # 加密密钥（>=16 字节）
```

## 使用方式

### 步骤 1：生成加密值

可通过代码调用 `Sm4CodecUtil` 生成密文：

```java
import com.leatop.cdp.util.Sm4CodecUtil;

String key = "your-16-bytes-or-more-key";
String plaintext = "myDatabasePassword123";
String encrypted = Sm4CodecUtil.encodeSms4ToHex(key, plaintext);
System.out.println(encrypted);  // 输出十六进制密文
```

### 步骤 2：在配置文件中使用 ENC() 包裹

```yaml
spring:
  datasource:
    username: root
    password: ENC(a1b2c3d4e5f6...)    # SM4 加密后的十六进制密文
  data:
    redis:
      password: ENC(f6e5d4c3b2a1...)
```

### 步骤 3：应用启动时自动解密

Spring Boot 启动时，Jasypt 自动识别 `ENC(...)` 格式的值，调用 `CustomEncryptor` 进行 SM4 解密，业务代码中读到的是明文值。

## 加密器实现

框架提供 `CustomEncryptor`（Bean 名称：`customEncryptor`），使用 SM4 国密算法：

- **加密：** SM4 对称加密 + 十六进制编码
- **解密：** 十六进制解码 + SM4 对称解密
- **密钥要求：** `jasypt.encryptor.key` 必须 >= 16 字节

## 密钥安全建议

### 方式一：环境变量传递密钥（推荐）

```yaml
jasypt:
  encryptor:
    key: ${JASYPT_KEY}  # 从环境变量读取
```

```bash
export JASYPT_KEY=your-16-bytes-or-more-key
java -jar app.jar
```

### 方式二：JVM 参数传递

```bash
java -Djasypt.encryptor.key=your-16-bytes-or-more-key -jar app.jar
```

## 注意事项

> 注意：`jasypt.encryptor.key` 密钥长度必须 >= 16 字节，否则启动时抛出 `IllegalArgumentException`。

> 注意：加密密钥不要硬编码在配置文件中提交到代码仓库，应通过环境变量或 JVM 参数传递。

> 注意：使用 SM4 国密算法加密，符合国内信创安全合规要求。

> 注意：只有 `ENC(...)` 格式包裹的值才会被自动解密，普通明文值不受影响。
