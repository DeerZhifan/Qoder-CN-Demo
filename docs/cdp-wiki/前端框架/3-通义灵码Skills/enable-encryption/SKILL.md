---
type: enable
description: 为 CDP 前端项目启用 RSA 密码加密功能
---

# 启用 RSA 密码加密

## 描述

为 CDP 前端项目启用 RSA 密码加密功能，确保登录密码在传输过程中通过 RSA 公钥加密，后端使用私钥解密。本技能将引导你完成依赖安装、加密工具创建、公钥 API 对接、登录流程集成和验证。

## 前提条件

- 后端已部署 RSA 密钥对（至少 1024 位，推荐 2048 位），并提供获取公钥的 API 接口
- 项目使用 CDP 标准登录模块（`src/cdp-common-frame/common/api/login/`）
- 包管理器为 pnpm

## 步骤

### 步骤 1：安装依赖

安装 RSA 加密库 jsencrypt：

```bash
pnpm add jsencrypt
```

### 步骤 2：创建加密工具

创建文件 `src/utils/encrypt.ts`，封装 RSA 加密逻辑：

```typescript
/**
 * RSA 密码加密工具
 *
 * 使用 jsencrypt 进行 RSA 公钥加密
 * 公钥从后端获取或使用预配置的静态公钥
 *
 * 参考: docs/knowledge-base/5-代码片段库/rsa-password-encrypt.ts
 */
import JSEncrypt from "jsencrypt";

// 默认公钥（与后端约定的 RSA 公钥）
const PUBLIC_KEY = "{替换为后端提供的RSA公钥}";

const encryptor = new JSEncrypt();
encryptor.setPublicKey(PUBLIC_KEY);

/**
 * RSA 加密（1024 位）
 * @param data 待加密的明文数据（如密码）
 * @returns Base64 编码的密文
 */
export function encrypt(data: string): string {
  const result = encryptor.encrypt(data);
  if (!result) {
    throw new Error("RSA 加密失败，请检查公钥配置");
  }
  return result;
}

/**
 * 使用动态公钥加密
 * @param data 待加密的明文数据
 * @param publicKey 动态获取的公钥
 * @returns Base64 编码的密文
 */
export function encryptWithKey(data: string, publicKey: string): string {
  const enc = new JSEncrypt();
  enc.setPublicKey(publicKey);
  const result = enc.encrypt(data);
  if (!result) {
    throw new Error("RSA 加密失败，请检查公钥");
  }
  return result;
}

export default { encrypt, encryptWithKey };
```

### 步骤 3：获取公钥 API

如果后端提供动态获取公钥的接口，在登录 API 模块中添加公钥获取方法：

**文件路径：** `src/cdp-common-frame/common/api/login/index.ts`

```typescript
import { get } from "@/cdp-common/utils/request";

/**
 * 获取 RSA 公钥
 */
export function getPublicKeyApi() {
  return get<{ publicKey: string }>("/auth/publicKey");
}
```

### 步骤 4：集成到登录流程

修改登录逻辑，在提交密码前进行 RSA 加密：

**文件路径：** 登录页面组件（如 `src/cdp-common-frame/common/views/login/index.vue`）

```typescript
import rsa from "@/cdp-common/utils/rsa";
import { useUserStore } from "@/cdp-admin/store/modules/user";
import type { LoginData } from "@/cdp-common-frame/common/api/login/types";

const userStore = useUserStore();

async function handleLogin(formData: { username: string; password: string }) {
  const loginData: LoginData = {
    username: formData.username,
    password: rsa.encrypt(formData.password), // RSA 加密密码
  };

  try {
    const response = await userStore.login(loginData);

    // login() 的 4 种响应码:
    // code=200:   登录成功 → 跳转首页
    // code=10010: 需要修改密码 → 跳转修改密码页
    // code=10020: 需要二次验证 → 跳转二次登录页
    // code=10030: 二次登录 token 过期 → 重新登录

    if (!response) {
      router.push("/");
    } else if (response.data?.code === 10010) {
      router.push("/change-password");
    }
  } catch (error) {
    ElMessage.error("登录失败");
  }
}
```

如果使用动态公钥模式：

```typescript
import { encrypt, encryptWithKey } from "@/utils/encrypt";
import { getPublicKeyApi } from "@/cdp-common-frame/common/api/login";

async function handleLogin(formData: { username: string; password: string }) {
  // 动态获取公钥
  const { data } = await getPublicKeyApi();
  const publicKey = data.publicKey;

  const loginData: LoginData = {
    username: formData.username,
    password: encryptWithKey(formData.password, publicKey),
  };

  await userStore.login(loginData);
}
```

### 步骤 5：验证

1. 启动开发服务器：`pnpm run dev`
2. 打开浏览器开发者工具 → Network 面板
3. 执行登录操作，检查登录请求的 Request Payload
4. 确认 `password` 字段的值为 Base64 编码的密文（类似 `aGVsbG8gd29ybGQ=...`），而非明文密码
5. 确认后端能正确解密并验证密码，登录流程正常

## 完成后提醒

1. **确保后端配置私钥** — RSA 私钥必须与前端使用的公钥配对，否则后端无法解密。更换密钥对时前后端需同步更新
2. **公钥建议缓存到 Pinia Store** — 如果使用动态获取公钥模式，将公钥缓存到 Store 中避免每次登录都请求公钥接口
3. **使用 HTTPS** — RSA 加密仅保护密码字段，传输层安全仍需 HTTPS 保障。生产环境务必启用 HTTPS
4. **不要将私钥放前端** — RSA 私钥仅在后端保存和使用，前端代码中只能包含公钥
5. 如果项目已有 `src/cdp-common/utils/rsa.ts`，优先使用框架内置的加密工具，无需重复创建
