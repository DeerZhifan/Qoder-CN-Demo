---
trigger: when_referenced
knowledge_source:
  - docs/knowledge-base/2-组件设计手册/07-module-encryption.md
  - docs/knowledge-base/3-组件使用手册/cdp-usage-encryption.md
  - docs/knowledge-base/5-代码片段库/rsa-password-encrypt.ts
---

# CDP 加密模块开发规范

## 适用场景

本规则适用于 CDP Web 前端框架下涉及数据加密的开发任务，包括：登录密码 RSA 加密传输、API 接口 AES 加密通信、敏感数据手动加解密、JWT Token 解码、加密配置与白名单管理。

## 前置依赖

- RSA 加密工具：`src/cdp-common/utils/rsa.ts`（依赖 `jsencrypt`）
- AES 加密工具：`src/cdp-common/utils/aes.ts`（依赖 `crypto-js`）
- JWT 解码：`jwt-decode`（已在项目依赖中）
- 请求拦截器：`src/cdp-common/utils/request.ts`（内置 AES 加解密链路）
- 环境变量：`VITE_API_ENCRYPT`（AES 接口加密总开关）、`VITE_IGNORE_ENCRYPT_API`（加密白名单）

## 配置要点

### RSA 密码加密

| 配置项 | 说明 |
|--------|------|
| 算法 | RSA 1024 位（短数据）/ 2048 位（长数据） |
| 公钥位置 | `src/cdp-common/utils/rsa.ts` 中的 `PUBLIC_KEY` / `PUBLIC_KEY_2048` |
| 私钥 | 仅在后端保存，前端无法解密 |
| 数据长度限制 | 1024 位最大加密 117 字节，超长数据使用 2048 位或 AES |

### AES 接口加密

| 配置项 | 说明 |
|--------|------|
| 算法 | AES-ECB，PKCS7 填充 |
| 密钥 | `dfswaesk20201118`（16 字节，硬编码在 `aes.ts` 中） |
| 请求加密 | 拦截器自动处理，添加 `headers["alg"] = "AES"` |
| 响应解密 | 检测响应头 `alg: AES` 后自动解密 |

### 环境变量配置

```bash
# .env.development — 开发环境关闭加密，便于调试
VITE_API_ENCRYPT = 'false'

# .env.production — 生产环境开启加密
VITE_API_ENCRYPT = 'true'
VITE_IGNORE_ENCRYPT_API = '/auth/login,/auth/captcha,/file/upload/**'
```

白名单支持 Ant 风格通配符：`?`（单字符）、`*`（单层路径）、`**`（任意路径）、`{a,b}`（备选项）。

## 代码模式

### 推荐写法

#### RSA 加密登录密码

```typescript
import rsa from "@/cdp-common/utils/rsa";
import { useUserStore } from "@/cdp-admin/store/modules/user";
import type { LoginData } from "@/cdp-common-frame/common/api/login/types";

async function handleLogin(formData: { username: string; password: string }) {
  const userStore = useUserStore();

  const loginData: LoginData = {
    username: formData.username,
    password: rsa.encrypt(formData.password), // RSA 1024 位加密
  };

  const response = await userStore.login(loginData);

  // login() 响应码:
  // 200   — 登录成功，跳转首页
  // 10010 — 需修改密码
  // 10020 — 需二次验证
  // 10030 — 二次登录 token 过期
}
```

#### RSA 2048 位加密（长数据）

```typescript
import rsa from "@/cdp-common/utils/rsa";

// 超过 117 字节的敏感数据使用 2048 位
const encryptedData = rsa.encrypt2048(JSON.stringify(sensitivePayload));
```

#### AES 手动加解密

```typescript
import aes from "@/cdp-common/utils/aes";

// 加密敏感数据
const encrypted = aes.encrypt(JSON.stringify({ idCard: "xxx", phone: "xxx" }));

// 解密
const data = JSON.parse(aes.decrypt(encrypted));
```

#### AES 接口加密（自动模式）

```bash
# .env.production — 开启后无需业务代码改动
VITE_API_ENCRYPT = 'true'
VITE_IGNORE_ENCRYPT_API = '/auth/**,/file/**'
```

加密流程由请求拦截器自动处理：
- JSON 数据 → 直接 AES 加密字符串
- 表单数据 → 包装为 `{ __encryptedData: aes.encrypt(data) }`
- FormData（文件上传）→ 自动跳过

#### JWT Token 解码

```typescript
import { jwtDecode } from "jwt-decode";

interface TokenPayload {
  sub: string;
  exp: number;
  roles: string[];
}

const payload = jwtDecode<TokenPayload>(token);
const isExpired = payload.exp * 1000 < Date.now();
```

#### 公钥配置更换

```typescript
// src/cdp-common/utils/rsa.ts
const PUBLIC_KEY = "MFwwDQYJ..."; // 替换为新的 1024 位公钥
const PUBLIC_KEY_2048 = "MIIBIjAN..."; // 替换为新的 2048 位公钥
```

更换公钥后需同步更新后端对应的私钥配置。

### 禁止事项

1. **禁止自行实现加密逻辑** — 必须使用框架提供的 `aes.ts` 和 `rsa.ts`，不要自行引入加密库或编写加密算法
2. **禁止在前端存储或硬编码 RSA 私钥** — 私钥仅在后端保存，前端只持有公钥用于加密
3. **禁止直接传输明文密码** — 登录密码必须经过 `rsa.encrypt()` 加密后再提交
4. **禁止将 AES 密钥作为唯一安全措施** — AES 密钥硬编码在前端代码中，仅提供传输层保护，生产环境必须配合 HTTPS
5. **禁止在开发环境开启接口加密** — `VITE_API_ENCRYPT` 在开发环境设为 `false`，避免调试困难
6. **禁止用 1024 位 RSA 加密超长数据** — 1024 位最大加密 117 字节，超长数据使用 `rsa.encrypt2048()` 或 AES
7. **禁止手动处理 FormData 的加密** — 请求拦截器已自动跳过 FormData 类型，无需额外配置
8. **禁止修改请求拦截器中的加密逻辑** — 加密链路已在 `request.ts` 中统一实现，业务代码不应介入
