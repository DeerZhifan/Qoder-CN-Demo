/**
 * [场景:RSA密码加密] — 登录时 RSA 加密密码传输
 *
 * 源码参考: src/cdp-common/utils/rsa.ts
 */

import rsa from "@/cdp-common/utils/rsa";
import { useUserStore } from "@/cdp-admin/store/modules/user";
import type { LoginData } from "@/cdp-common-frame/common/api/login/types";

/**
 * 登录表单提交示例
 *
 * 密码使用 RSA 1024 位公钥加密后传输
 * 后端持有私钥解密
 */
async function handleLogin(formData: { username: string; password: string }) {
  const userStore = useUserStore();

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
      // code=200 或 code=10020，正常处理
      // router.push("/");
    } else if (response.data?.code === 10010) {
      // 跳转修改密码页
      // router.push("/change-password");
    }
  } catch (error) {
    ElMessage.error("登录失败");
  }
}

/**
 * 2048 位 RSA 加密（用于更长的敏感数据）
 */
function encryptSensitiveData(data: string): string {
  return rsa.encrypt2048(data);
}
