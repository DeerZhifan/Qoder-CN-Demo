# 如何使用 CDP 工具类

## 概述

工具类模块（`leatop-cdp-common-util`）提供了一组常用的工具类，涵盖加密解密、JSON 处理、字符串操作、集合操作、Bean 拷贝、密码编码等。大部分工具类基于 Hutool 封装，部分提供独立实现。

## 常用工具类一览

### 加密解密类

| 类名 | 说明 | 关键方法 |
|------|------|---------|
| `AesUtil` | AES 对称加密 | `encrypt(val)`, `decrypt(encodeVal)` |
| `RSAUtil` | RSA 2048 非对称加密 | 公钥加密、私钥解密 |
| `Sm4CodecUtil` | SM4 国密加密 | `encodeSms4ToHex()`, `decodeSms4HexToString()` |
| `MD5Util` | MD5 哈希 | `md5(input)` |
| `Base64Utils` | Base64 编解码 | `encode()`, `decode()` |
| `CdpPasswordEncoder` | BCrypt 密码编码 | `encode(raw)`, `matches(raw, encoded)` |

### 数据处理类

| 类名 | 说明 | 关键方法 |
|------|------|---------|
| `JSONTool` | Jackson JSON 序列化 | `toJson(obj)`, `fromJson(json, clazz)` |
| `StrUtil` | 字符串操作 | `isNotBlank()`, `isEmpty()`, `split()` |
| `CollectionUtil` | 集合操作 | `isEmpty()`, `isNotEmpty()` |
| `BeanUtil` | Bean 拷贝 | `copyProperties(src, target)` |
| `NumberUtil` | 数值转换 | 类型安全的数值操作 |
| `ObjectUtil` | 对象操作 | 空判断、比较 |

### 系统工具类

| 类名 | 说明 | 关键方法 |
|------|------|---------|
| `IPUtils` | IP 地址提取 | 从 HttpServletRequest 提取真实 IP |
| `IdUtils` | ID 生成 | `nanoId(length)` |
| `UUIDTool` | UUID 生成 | `randomUUID()` |
| `SpringELUtils` | SpEL 表达式求值 | 解析 Spring 表达式 |
| `XmlUtils` | XML 处理 | XML 解析和序列化 |
| `ZipUtils` | ZIP 压缩 | 文件压缩和解压缩 |

## 使用示例

### AES 加密解密

```java
import com.leatop.cdp.util.AesUtil;

String encrypted = AesUtil.encrypt("敏感数据");
String decrypted = AesUtil.decrypt(encrypted);
```

### JSON 序列化

```java
import com.leatop.cdp.util.JSONTool;

// 对象转 JSON
String json = JSONTool.toJson(userDTO);

// JSON 转对象
UserDTO dto = JSONTool.fromJson(json, UserDTO.class);
```

> 注意：`JSONTool` 内置了 `LocalDate`、`LocalDateTime`、`LocalTime` 的序列化支持，日期格式为 `yyyy-MM-dd HH:mm:ss`。

### 密码编码

```java
import com.leatop.cdp.util.CdpPasswordEncoder;

CdpPasswordEncoder encoder = new CdpPasswordEncoder();
String encoded = encoder.encode("rawPassword");
boolean matches = encoder.matches("rawPassword", encoded);
```

### SM4 国密加密

```java
import com.leatop.cdp.util.Sm4CodecUtil;

String encrypted = Sm4CodecUtil.encodeSms4ToHex("key16bytesOrMore", "plaintext");
String decrypted = Sm4CodecUtil.decodeSms4HexToString("key16bytesOrMore", encrypted);
```

## 注意事项

> 注意：`AesUtil` 使用默认密钥 `dfswaesk20201118`，生产环境建议使用自定义密钥。

> 注意：`Sm4CodecUtil` 的密钥长度必须 >= 16 字节，否则抛出异常。

> 注意：大部分工具类为静态方法调用，无需注入 Spring Bean。

> 注意：优先使用框架提供的工具类，避免重复引入第三方库（如 Apache Commons）。
