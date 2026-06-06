# 加密模块设计手册

> 版本: v1.0 | 最后更新: 2026-04-06 | 搜索关键词: AES, RSA, 加密, 解密, crypto-js, jsencrypt, 接口加密

---

## 一、加密体系概览

CDP Web 提供两种加密能力：

| 加密方式 | 算法 | 用途 | 源码位置 |
|---------|------|------|---------|
| AES | AES-ECB + PKCS7 | API 请求/响应体加密（对称加密） | `src/cdp-common/utils/aes.ts` |
| RSA | RSA 1024/2048 位 | 登录密码等敏感数据传输（非对称加密） | `src/cdp-common/utils/rsa.ts` |

---

## 二、AES 加密模块

源码位置：`src/cdp-common/utils/aes.ts`

### 配置

| 参数 | 值 |
|------|-----|
| 算法 | AES |
| 模式 | ECB（电子密码本） |
| 填充 | PKCS7 |
| 密钥 | 16 字节硬编码字符串 `"dfswaesk20201118"` |
| 依赖 | crypto-js |

### API

```typescript
import aes from "@/cdp-common/utils/aes";

// 加密（返回 Base64 字符串）
const encrypted = aes.encrypt("明文数据");

// 解密（返回原始字符串）
const decrypted = aes.decrypt(encrypted);
```

### 实现

```typescript
import CryptoJS from "crypto-js";

const key = CryptoJS.enc.Utf8.parse("dfswaesk20201118");

function encrypt(data: string) {
  const secretData = CryptoJS.enc.Utf8.parse(data);
  const encrypted = CryptoJS.AES.encrypt(secretData, key, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7,
  });
  return encrypted.toString(); // Base64 编码
}

function decrypt(data: string) {
  const decrypt = CryptoJS.AES.decrypt(data, key, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7,
  });
  return CryptoJS.enc.Utf8.stringify(decrypt).toString();
}
```

---

## 三、RSA 加密模块

源码位置：`src/cdp-common/utils/rsa.ts`

### 双密钥配置

| 密钥 | 位数 | 用途 |
|------|------|------|
| `PUBLIC_KEY` | 1024 位 | 登录密码加密（短数据） |
| `PUBLIC_KEY_2048` | 2048 位 | 长数据加密 |

> 注意: 仅提供公钥加密，私钥在后端，前端无法解密。

### API

```typescript
import rsa from "@/cdp-common/utils/rsa";

// 1024 位加密（用于登录密码）
const encryptedPassword = rsa.encrypt(password);

// 2048 位加密（用于长数据）
const encryptedData = rsa.encrypt2048(sensitiveData);
```

### 实现

```typescript
import JSEncrypt from "jsencrypt";

const PUBLIC_KEY = "MFwwDQYJ..."; // 1024 位公钥
const PUBLIC_KEY_2048 = "MIIBIjAN..."; // 2048 位公钥

const encryptor = new JSEncrypt();
encryptor.setPublicKey(PUBLIC_KEY);

function encrypt(data: string): string {
  return encryptor.encrypt(data).toString();
}

const encryptor2048 = new JSEncrypt();
encryptor2048.setPublicKey(PUBLIC_KEY_2048);

function encrypt2048(data: string): string {
  return encryptor2048.encrypt(data).toString();
}
```

---

## 四、API 接口加密链路

### 总开关

| 环境变量 | 说明 |
|---------|------|
| `VITE_API_ENCRYPT` | `"true"` 启用接口加密，`"false"` 关闭 |
| `VITE_IGNORE_ENCRYPT_API` | 逗号分隔的白名单路径（Ant 风格通配符） |

### 请求加密流程

源码位置：`src/cdp-common/utils/request.ts:115-137`

```
发送请求 →
  ① FormData? → 跳过加密（文件上传场景）
  ② checkEncryptApi(url) 检查是否需要加密
     └── VITE_API_ENCRYPT !== "true" → 不加密
     └── URL 匹配 VITE_IGNORE_ENCRYPT_API 白名单 → 不加密
     └── 需要加密 →
         ├── typeof data == "string"（JSON 格式）
         │   → config.data = aes.encrypt(data)
         └── typeof data == "object"（表单格式）
             → config.data = { __encryptedData: aes.encrypt(JSON.stringify(data)) }
         → 设置 headers["alg"] = "AES"
```

### 响应解密流程

源码位置：`src/cdp-common/utils/request.ts:146-155`

```
接收响应 →
  检查 response.headers["alg"] == "AES"
  ├── 是 → respData = JSON.parse(aes.decrypt(response.data))
  └── 否 → 直接使用原始数据
```

### Ant 路径匹配器

白名单使用 Ant 风格通配符：

| 通配符 | 含义 | 示例 |
|--------|------|------|
| `?` | 单个非 `/` 字符 | `/api/v?/user` 匹配 `/api/v1/user` |
| `*` | 任意非 `/` 字符序列 | `/api/*/list` 匹配 `/api/user/list` |
| `**` | 任意字符序列（含 `/`） | `/api/**` 匹配所有 `/api/` 下路径 |
| `{a,b}` | 备选项 | `/api/{user,role}/list` |

配置示例：
```
VITE_IGNORE_ENCRYPT_API=/auth/login,/auth/captcha,/upload/**
```

---

## 五、使用场景

### 场景一：登录密码 RSA 加密

```typescript
import rsa from "@/cdp-common/utils/rsa";

function handleLogin() {
  const loginData = {
    username: formData.username,
    password: rsa.encrypt(formData.password), // RSA 加密密码
  };
  loginApi(loginData);
}
```

### 场景二：全局 API 加密

```bash
# .env.production
VITE_API_ENCRYPT=true
VITE_IGNORE_ENCRYPT_API=/auth/login,/auth/captcha,/file/upload/**
```

启用后，除白名单外的所有 API 请求自动 AES 加密，无需业务代码改动。

### 场景三：手动加解密

```typescript
import aes from "@/cdp-common/utils/aes";

// 加密敏感数据
const encrypted = aes.encrypt(JSON.stringify({ idCard: "xxx", phone: "xxx" }));

// 解密
const data = JSON.parse(aes.decrypt(encrypted));
```

---

## 六、配置参考

| 环境 | VITE_API_ENCRYPT | VITE_IGNORE_ENCRYPT_API | 说明 |
|------|-----------------|------------------------|------|
| 开发 | `false` | - | 关闭加密，便于调试 |
| 测试 | `true` | `/auth/**,/file/**` | 开启加密，白名单免密 |
| 生产 | `true` | `/auth/**,/file/**` | 开启加密 |

---

## 七、常见陷阱

1. **开发环境关闭加密**: `VITE_API_ENCRYPT=false`，避免调试困难
2. **FormData 自动跳过**: 文件上传（`FormData`）不会被加密，无需额外配置
3. **JSON vs 表单加密差异**: JSON 数据直接加密字符串，表单数据包装为 `{ __encryptedData: "..." }`，后端需对应处理
4. **RSA 数据长度限制**: 1024 位 RSA 最大加密 117 字节（936 bit），超长数据用 2048 位或 AES
5. **密钥安全**: AES 密钥硬编码在前端代码中，仅提供传输层保护，不能替代 HTTPS
