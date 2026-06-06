# 启用 CDP 配置加密

## 描述

在已有 CDP 项目中启用配置加密组件（`leatop-cdp-common-jasypt`），基于 Jasypt 框架使用 SM4 国密算法对配置文件中的敏感信息进行加密，防止数据库密码、Redis 密码、API 密钥等以明文形式提交到代码仓库。

## 输入

请向用户确认以下信息：

1. **加密密钥**（`jasypt.encryptor.key`，>= 16 字节，推荐通过环境变量传递）
2. **需要加密的配置项**（如数据库密码、Redis 密码等）
3. **密钥传递方式**（环境变量 或 JVM 参数，默认环境变量）

---

## 步骤 1：确认 Maven 依赖

> `leatop-cdp-common-jasypt` 通过 `leatop-cdp-common-starter` 自动引入，通常无需额外添加依赖。

确认 `pom.xml` 中已引入 starter：

```xml
<dependency>
    <groupId>com.leatop.cdp</groupId>
    <artifactId>leatop-cdp-common-starter</artifactId>
</dependency>
```

## 步骤 2：配置加密器

> `jasypt.encryptor.bean` 必须指定为 `customEncryptor`，这是 CDP 自定义的 SM4 加密器 Bean 名称。密钥长度必须 >= 16 字节。

在 `application.yaml` 中添加：

```yaml
jasypt:
  encryptor:
    bean: customEncryptor           # 使用 CDP 自定义加密器（SM4 国密算法）
    key: ${JASYPT_KEY}              # 加密密钥，通过环境变量传递
```

**环境变量方式（推荐）：**

```bash
export JASYPT_KEY=your-16-bytes-or-more-key
java -jar app.jar
```

**JVM 参数方式：**

```bash
java -Djasypt.encryptor.key=your-16-bytes-or-more-key -jar app.jar
```

## 步骤 3：生成加密值

> 使用 `Sm4CodecUtil` 工具类对明文进行 SM4 加密，输出十六进制密文字符串。

编写工具类或测试方法生成密文：

```java
import com.leatop.cdp.util.Sm4CodecUtil;

public class EncryptTool {
    public static void main(String[] args) {
        String key = "your-16-bytes-or-more-key";

        // 加密数据库密码
        String dbPassword = "myDatabasePassword123";
        String encryptedDb = Sm4CodecUtil.encodeSms4ToHex(key, dbPassword);
        System.out.println("数据库密码密文: " + encryptedDb);

        // 加密 Redis 密码
        String redisPassword = "myRedisPassword";
        String encryptedRedis = Sm4CodecUtil.encodeSms4ToHex(key, redisPassword);
        System.out.println("Redis 密码密文: " + encryptedRedis);

        // 验证解密
        String decrypted = Sm4CodecUtil.decodeSms4HexToString(key, encryptedDb);
        System.out.println("解密验证: " + decrypted);
    }
}
```

## 步骤 4：在配置文件中使用 ENC() 占位符

> 将步骤 3 生成的密文用 `ENC(...)` 格式包裹，替换配置文件中的明文敏感值。只有 `ENC(...)` 格式包裹的值才会被自动解密，普通明文值不受影响。

```yaml
spring:
  datasource:
    username: root
    password: ENC({数据库密码密文})         # 替换为步骤 3 生成的密文
  data:
    redis:
      password: ENC({Redis密码密文})       # 替换为步骤 3 生成的密文
```

## 步骤 5：验证

启动应用，检查以下内容：

1. 应用正常启动，无 `IllegalArgumentException`（密钥长度校验通过）
2. 数据库连接成功（证明密码已正确解密）
3. Redis 连接成功（如果加密了 Redis 密码）
4. 控制台日志中不出现明文密码

---

## 完成后提醒

1. 加密密钥绝不要硬编码在配置文件中提交到代码仓库，必须通过环境变量或 JVM 参数传递
2. 所有敏感配置值（数据库密码、Redis 密码、API 密钥等）都应使用 `ENC(...)` 格式加密后提交
3. `jasypt.encryptor.bean` 必须为 `customEncryptor`，CDP 使用 SM4 国密算法满足信创合规要求
4. SM4 密钥长度必须 >= 16 字节，不足时应用启动将被阻止
5. 业务代码通过 `@Value` 或 `Environment` 读取配置时已经是明文，无需手动解密
6. 任何 Spring 属性值均可使用 `ENC(...)` 加密，不限于数据库和 Redis 密码
