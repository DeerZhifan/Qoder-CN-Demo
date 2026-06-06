# CDP 配置加密 设计手册

> 对应使用手册：[cdp-module-jasypt.md](../3-组件使用手册/cdp-module-jasypt.md)

## 一、设计目标与背景

企业应用的配置文件中常包含数据库密码、Redis 密码、API 密钥等敏感信息。将这些信息以明文形式存储在 `application.yaml` 中并提交到代码仓库，存在严重的安全隐患。`leatop-cdp-common-jasypt` 模块的设计目标是：

1. **透明加解密**：利用 Jasypt Spring Boot Starter 的 `ENC(...)` 机制，在应用启动时自动解密配置项，业务代码完全无感知。
2. **国密合规**：使用 SM4 对称加密算法替代 Jasypt 默认的 PBE 算法，满足国内信创环境下的安全合规要求。
3. **极简集成**：模块通过 `leatop-cdp-common-starter` 自动引入，仅需配置加密密钥即可使用，无需额外编码。

> 设计决策：选择 SM4 而非 AES 或 Jasypt 默认的 PBEWithMD5AndDES，是因为 SM4 属于国家密码管理局发布的商用密码算法标准（GB/T 32907-2016），在政务和央企项目中通常是合规性的硬性要求。

## 二、整体架构

模块的整体设计非常精简，核心只有一个类 `CustomEncryptor`，它作为 Jasypt 与 SM4 算法之间的桥接层：

```
Jasypt Spring Boot Starter
  |
  | （通过 jasypt.encryptor.bean=customEncryptor 指定）
  v
CustomEncryptor (implements StringEncryptor, InitializingBean)
  |
  | （委托加解密）
  v
Sm4CodecUtil（纯 SM4 算法实现，位于 leatop-cdp-common-util）
```

**工作流程**：Spring Boot 启动时，Jasypt 的 `EnableEncryptablePropertiesBeanFactoryPostProcessor` 扫描所有 `ENC(...)` 格式的属性值，调用 `jasypt.encryptor.bean` 指定的 `StringEncryptor` 实现进行解密。CDP 通过将该 bean 指向 `CustomEncryptor`，将解密逻辑委托给自研的 SM4 实现。

模块的 Maven 依赖结构：

- `jasypt-spring-boot-starter`：提供 Spring Boot 环境下 `ENC(...)` 属性的自动解密基础设施
- `jasypt`（org.jasypt）：提供 `StringEncryptor` 接口定义
- `leatop-cdp-common-util`：提供 `Sm4CodecUtil` 算法实现

## 三、关键类说明

### CustomEncryptor

该类是模块的唯一核心组件，承担三重职责：

1. **Jasypt 桥接**：实现 `org.jasypt.encryption.StringEncryptor` 接口，覆写 `encrypt()` 和 `decrypt()` 方法，将调用转发给 `Sm4CodecUtil` 的静态方法。加密产出十六进制字符串（Hex 编码），解密接受十六进制字符串输入。
2. **密钥管理**：通过 `@Value("${jasypt.encryptor.key:}")` 从 Spring 环境中读取密钥。实现 `InitializingBean` 接口，在 `afterPropertiesSet()` 中执行密钥校验 -- 若未配置密钥则回退到 `Sm4CodecUtil.DEFAULT_KEY`，若密钥长度不足 16 字节则抛出 `IllegalArgumentException` 阻止应用启动。
3. **Bean 注册**：通过 `@Component("customEncryptor")` 注册到 Spring 容器，bean 名称与 `jasypt.encryptor.bean` 配置值对应。常量 `BEAN_NAME` 和 `ENCRYPTOR_BEAN_PROPERTY` 便于其他模块引用。

> 设计决策：密钥校验放在 `afterPropertiesSet()` 而非构造函数中，是因为 `@Value` 注入发生在构造之后、属性设置阶段。使用 `InitializingBean` 确保校验时密钥已就绪。

### Sm4CodecUtil

位于 `leatop-cdp-common-util` 模块，是纯 Java 实现的 SM4 分组密码算法工具类，不依赖任何第三方密码库（如 Bouncy Castle）。核心特征：

- **ECB 模式**：以 16 字节为一个分组，逐块加密/解密，明文不足 16 字节时以 `\0` 填充
- **32 轮 Feistel 网络**：实现标准 SM4 的轮函数 F、S 盒非线性变换、L 线性变换、密钥扩展算法
- **Hex 编解码**：对外暴露 `encodeSms4ToHex()` 和 `decodeSms4HexToString()` 两个静态方法，输入输出均为字符串，便于配置文件使用
- **默认密钥**：通过 `System.getProperty("sm4_key", "1234567890123456")` 提供回退密钥，但生产环境应始终显式配置

## 四、扩展机制

1. **替换加密算法**：如需将 SM4 替换为 AES 或其他算法，只需创建新的 `StringEncryptor` 实现类，注册为 Spring Bean，并修改 `jasypt.encryptor.bean` 配置指向新 bean 名称。`CustomEncryptor` 的桥接模式使得算法切换不影响框架其他部分。
2. **密钥来源扩展**：当前密钥通过 `@Value` 从 Spring Environment 读取，支持环境变量、JVM 参数、配置中心等所有 Spring 属性源。如需对接硬件加密机（HSM）或密钥管理服务（KMS），可在 `CustomEncryptor` 中替换密钥获取逻辑。
3. **加密范围扩展**：Jasypt 的 `ENC(...)` 机制不限于特定配置项，任何 Spring 属性值均可加密。除数据库密码外，还可用于 Redis 密码、MQ 连接串、第三方 API 密钥等场景。
