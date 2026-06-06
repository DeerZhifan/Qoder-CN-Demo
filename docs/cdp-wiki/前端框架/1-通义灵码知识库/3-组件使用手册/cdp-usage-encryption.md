# 如何使用加密功能

> 版本: v1.0 | 最后更新: 2026-04-07 | 搜索关键词: 加密, AES, RSA, 密码加密, 接口加密, crypto-js, jsencrypt, encrypt

---

## 概述

CDP Web 提供两种加密能力：RSA 非对称加密（用于密码等敏感字段）和 AES 对称加密（用于接口请求/响应数据）。加密工具位于 `src/cdp-common/utils/aes.ts` 和 `src/cdp-common/utils/rsa.ts`。

## RSA 密码加密

登录密码等敏感字段在提交前通过 RSA 公钥加密，后端使用私钥解密。

### 使用方式

```typescript
import rsa from "@/cdp-common/utils/rsa";

// 1024 位 RSA 加密（默认）
const encryptedPassword = rsa.encrypt(password);

// 2048 位 RSA 加密（更高安全性）
const encryptedPassword2048 = rsa.encrypt2048(password);
```

### 完整登录示例

```vue
<script setup lang="ts">
import rsa from "@/cdp-common/utils/rsa";
import { useUserStore } from "@/cdp-admin/store/modules/user";

const userStore = useUserStore();

async function handleLogin() {
  const loginData = {
    username: form.username,
    password: rsa.encrypt(form.password),  // RSA 加密密码
    loginMode: "Account",
  };
  await userStore.login(loginData);
}
</script>
```

### 公钥配置

RSA 公钥硬编码在 `src/cdp-common/utils/rsa.ts` 中，分为 1024 位和 2048 位两种。如需更换密钥对，修改该文件中的 `PUBLIC_KEY` 和 `PUBLIC_KEY_2048` 常量，并同步更新后端私钥。

## AES 接口加密

AES 加密用于对接口请求数据加密、响应数据解密，防止数据在传输过程中被窃取。

### 开启方式

在环境变量文件（`.env.development` / `.env.production`）中配置：

```bash
# 开启接口加密
VITE_API_ENCRYPT = 'true'

# 不需要加密的接口（支持 Ant 风格通配符）
VITE_IGNORE_ENCRYPT_API = '/login/**,/test/**'
```

### 加密流程

1. **请求加密**：拦截器检测 `VITE_API_ENCRYPT = 'true'` 且接口不在排除列表中 → 对请求 body 执行 AES ECB 加密 → 添加请求头 `alg: AES`
2. **响应解密**：拦截器检测响应头 `alg: AES` → 对响应体执行 AES ECB 解密 → 返回明文数据

### 加密细节

- 算法：AES ECB 模式，PKCS7 填充
- 密钥：硬编码在 `src/cdp-common/utils/aes.ts` 中（`dfswaesk20201118`，16 字节）
- JSON 数据：直接对 JSON 字符串加密
- 表单数据：将数据 JSON 序列化后加密，放入 `__encryptedData` 字段
- FormData（文件上传）：自动跳过加密

### 手动加解密

```typescript
import aes from "@/cdp-common/utils/aes";

// 加密
const encrypted = aes.encrypt("明文数据");

// 解密
const decrypted = aes.decrypt(encrypted);
```

### 排除接口配置

`VITE_IGNORE_ENCRYPT_API` 支持 Ant 风格通配符：

```bash
# 多个规则用逗号分隔
VITE_IGNORE_ENCRYPT_API = '/login/**,/captcha/**,/public/**'

# ** 匹配任意路径
# * 匹配单层路径
# ? 匹配单个字符
```

## 注意事项

> 注意：AES 加密自动跳过 `FormData` 类型的请求（文件上传），无需手动处理。

> 注意：开启接口加密后，后端也必须配置相同的 AES 密钥和加解密逻辑，否则接口调用会失败。

> 注意：RSA 公钥更换时需要前后端同步更新，前端修改 `rsa.ts`，后端修改对应的私钥配置。

> 注意：`VITE_API_ENCRYPT` 默认为 `'false'`（关闭）。生产环境如需开启，在 `.env.production` 中设置为 `'true'`。

> 注意：AES 密钥（`dfswaesk20201118`）硬编码在前端代码中，仅提供传输层加密，不能替代 HTTPS。
