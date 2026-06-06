---
type: gen
description: 基于 CDP 前端框架规范，生成环境配置文件（.env.*）
input:
  - name: 环境名称
    description: 环境标识，如 development、production、staging
    required: true
  - name: API基础路径
    description: API 代理前缀，如 /api/
    required: true
  - name: 端口号
    description: 开发服务器端口号，如 9527（仅开发环境需要）
    required: false
    default: "9527"
---

# 生成环境配置文件

基于 CDP 前端框架规范，生成 `.env.{环境名称}` 环境配置文件，包含标准的 `VITE_` 前缀变量定义。

## 步骤

### 第一步：生成 .env.{环境名称} 文件

根据环境类型生成对应的环境变量文件。

**开发环境模板**（当环境名称为 development 时）：

```bash
## 开发环境
NODE_ENV='development'

# 应用端口
VITE_APP_PORT = {端口号}

# 代理前缀
VITE_APP_BASE_API = '{API基础路径}'

# 开启接口加密
VITE_API_ENCRYPT = 'false'
VITE_IGNORE_ENCRYPT_API = '/login/**,/test/**'

# 微服务管理页面-ip地址
VITE_MICRO_MANAGER_IP_NACOS = 'http://172.17.1.83:8848/'
VITE_MICRO_MANAGER_IP_SKYWALKING = 'http://172.17.1.83:18080/'
VITE_MICRO_MANAGER_IP_GATEWAY = 'http://172.17.1.28:28080'
VITE_MICRO_MANAGER_IP_SENTINEL = 'http://172.17.1.83:8858/'
```

**生产环境模板**（当环境名称为 production 时）：

```bash
## 生产环境
NODE_ENV='production'

# 代理前缀
VITE_APP_BASE_API = '{API基础路径}'

# 开启接口加密
VITE_API_ENCRYPT = 'false'
VITE_IGNORE_ENCRYPT_API = '/login/**,/test/**'

# 微服务管理页面-ip地址
VITE_MICRO_MANAGER_IP_NACOS = 'http://172.17.1.83:8848/'
VITE_MICRO_MANAGER_IP_SKYWALKING = 'http://172.17.1.83:18080/'
VITE_MICRO_MANAGER_IP_GATEWAY = 'http://172.17.1.28:28080'
VITE_MICRO_MANAGER_IP_SENTINEL = 'http://172.17.1.83:8858/'
```

**自定义环境模板**（当环境名称为其他值，如 staging、test 时）：

```bash
## {环境名称}环境
NODE_ENV='{环境名称}'

# 应用端口（开发类环境需要）
VITE_APP_PORT = {端口号}

# 代理前缀
VITE_APP_BASE_API = '{API基础路径}'

# 开启接口加密
VITE_API_ENCRYPT = 'false'
VITE_IGNORE_ENCRYPT_API = '/login/**,/test/**'

# 微服务管理页面-ip地址（根据实际环境修改）
VITE_MICRO_MANAGER_IP_NACOS = ''
VITE_MICRO_MANAGER_IP_SKYWALKING = ''
VITE_MICRO_MANAGER_IP_GATEWAY = ''
VITE_MICRO_MANAGER_IP_SENTINEL = ''
```

### 第二步：配置 VITE_ 前缀变量

所有需要在前端代码中通过 `import.meta.env` 访问的变量**必须**以 `VITE_` 开头。

根据用户需求，在生成的文件中追加自定义变量：

```bash
# 自定义业务变量（示例）
VITE_APP_TITLE = '系统名称'
VITE_UPLOAD_URL = '/api/upload'
```

### 第三步：更新类型声明（如有新增变量）

如果新增了自定义 `VITE_` 变量，需同步更新 `src/typings/env.d.ts`：

```typescript
interface ImportMetaEnv {
  // 已有变量
  VITE_APP_PORT: string;
  VITE_APP_BASE_API: string;
  VITE_API_ENCRYPT: string;
  VITE_IGNORE_ENCRYPT_API: string;
  VITE_MICRO_MANAGER_IP_NACOS: string;
  VITE_MICRO_MANAGER_IP_SKYWALKING: string;
  VITE_MICRO_MANAGER_IP_GATEWAY: string;
  VITE_MICRO_MANAGER_IP_SENTINEL: string;
  // 新增变量
  VITE_APP_TITLE: string;
  VITE_UPLOAD_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
```

## 完成后提醒

- 环境变量名**必须**以 `VITE_` 开头，否则前端代码无法通过 `import.meta.env` 访问
- `import.meta.env` 中的值始终是**字符串类型**，布尔判断请使用 `=== 'true'` 而非 truthy 判断
- 修改 `.env.*` 文件后**必须重启**开发服务器（`pnpm run dev`），Vite 不支持环境变量热更新
- 生产环境（`.env.production`）通常不需要 `VITE_APP_PORT`
- 生产环境 `VITE_APP_BASE_API` 通常设为 `'/'`（由 Nginx 反向代理处理）
- 如果新增了自定义变量，请同步更新 `src/typings/env.d.ts` 类型声明文件
- 敏感信息（密钥、密码）不应放在 `.env.*` 文件中，应使用 `public/config/config.js` 运行时配置或后端管理
