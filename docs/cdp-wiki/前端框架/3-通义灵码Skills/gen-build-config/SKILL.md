---
type: gen
description: 基于 CDP 前端框架规范，生成 Vite 构建配置文件
input:
  - name: 项目名称
    description: 项目标识名，用于注释标注
    required: true
  - name: 后端API地址
    description: 后端服务地址，如 http://172.17.1.28:28080
    required: true
  - name: 代理路径
    description: API 代理前缀，如 /api/
    required: false
    default: /api/
---

# 生成 Vite 构建配置

基于 CDP 前端框架规范，生成符合项目标准的 `vite.config.ts` 构建配置文件，包含代理配置、构建优化、插件配置等。

## 步骤

### 第一步：生成 vite.config.ts 配置

生成 `vite.config.ts` 文件，包含完整的 CDP 框架标准配置：

```typescript
import vue from "@vitejs/plugin-vue";
import { UserConfig, ConfigEnv, loadEnv, defineConfig } from "vite";

import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

import Icons from "unplugin-icons/vite";
import IconsResolver from "unplugin-icons/resolver";

import { createSvgIconsPlugin } from "vite-plugin-svg-icons";
import mockDevServerPlugin from "vite-plugin-mock-dev-server";

import vueJsx from "@vitejs/plugin-vue-jsx";

import UnoCSS from "unocss/vite";
import { resolve } from "path";

const pathSrc = resolve(__dirname, "src");

// {项目名称} - Vite 构建配置
export default defineConfig(({ mode }: ConfigEnv): UserConfig => {
  const env = loadEnv(mode, process.cwd());
  return {
    resolve: {
      alias: {
        "@": pathSrc,
      },
    },
    css: {
      preprocessorOptions: {
        scss: {
          javascriptEnabled: true,
          additionalData: `
            @use "@/styles/variables.scss" as *;
            @use "@/styles/mixin.scss" as *;
          `,
        },
      },
    },
    base: "./",
    server: {
      host: "0.0.0.0",
      port: Number(env.VITE_APP_PORT),
      open: true,
      proxy: {
        // 主 API 代理 → {后端API地址}
        [env.VITE_APP_BASE_API]: {
          changeOrigin: true,
          target: "{后端API地址}",
          rewrite: (path) =>
            path.replace(new RegExp("^" + env.VITE_APP_BASE_API), ""),
          xfwd: true,
        },
      },
    },
    plugins: [
      vue(),
      // mockDevServerPlugin(), // 开启 MOCK 时取消注释，同时 target 切换到 localhost
      vueJsx(),
      UnoCSS({ hmrTopLevelAwait: false }),
      AutoImport({
        imports: ["vue", "@vueuse/core"],
        resolvers: [ElementPlusResolver(), IconsResolver({})],
        eslintrc: {
          enabled: false,
          filepath: "./.eslintrc-auto-import.json",
          globalsPropValue: true,
        },
        vueTemplate: true,
        dts: false,
      }),
      Components({
        resolvers: [
          ElementPlusResolver(),
          IconsResolver({ enabledCollections: ["ep"] }),
        ],
        dirs: ["src/components", "src/**/components"],
        dts: false,
      }),
      Icons({ autoInstall: true }),
      createSvgIconsPlugin({
        iconDirs: [resolve(pathSrc, "assets/icons")],
        symbolId: "icon-[dir]-[name]",
      }),
    ],
    optimizeDeps: {
      include: [
        "vue",
        "vue-router",
        "pinia",
        "axios",
        "element-plus/es/components/form/style/css",
        "element-plus/es/components/form-item/style/css",
        "element-plus/es/components/button/style/css",
        "element-plus/es/components/input/style/css",
        "element-plus/es/components/input-number/style/css",
        "element-plus/es/components/switch/style/css",
        "element-plus/es/components/upload/style/css",
        "element-plus/es/components/menu/style/css",
        "element-plus/es/components/col/style/css",
        "element-plus/es/components/icon/style/css",
        "element-plus/es/components/row/style/css",
        "element-plus/es/components/tag/style/css",
        "element-plus/es/components/dialog/style/css",
        "element-plus/es/components/loading/style/css",
        "element-plus/es/components/radio/style/css",
        "element-plus/es/components/radio-group/style/css",
        "element-plus/es/components/popover/style/css",
        "element-plus/es/components/scrollbar/style/css",
        "element-plus/es/components/tooltip/style/css",
        "element-plus/es/components/dropdown/style/css",
        "element-plus/es/components/dropdown-menu/style/css",
        "element-plus/es/components/dropdown-item/style/css",
        "element-plus/es/components/sub-menu/style/css",
        "element-plus/es/components/menu-item/style/css",
        "element-plus/es/components/divider/style/css",
        "element-plus/es/components/card/style/css",
        "element-plus/es/components/link/style/css",
        "element-plus/es/components/breadcrumb/style/css",
        "element-plus/es/components/breadcrumb-item/style/css",
        "element-plus/es/components/table/style/css",
        "element-plus/es/components/tree-select/style/css",
        "element-plus/es/components/table-column/style/css",
        "element-plus/es/components/select/style/css",
        "element-plus/es/components/option/style/css",
        "element-plus/es/components/pagination/style/css",
        "element-plus/es/components/tree/style/css",
        "element-plus/es/components/alert/style/css",
        "element-plus/es/components/radio-button/style/css",
        "element-plus/es/components/checkbox-group/style/css",
        "element-plus/es/components/checkbox/style/css",
        "element-plus/es/components/tabs/style/css",
        "element-plus/es/components/tab-pane/style/css",
        "element-plus/es/components/rate/style/css",
        "element-plus/es/components/date-picker/style/css",
        "element-plus/es/components/notification/style/css",
        "element-plus/es/components/image/style/css",
        "element-plus/es/components/statistic/style/css",
        "element-plus/es/components/watermark/style/css",
        "element-plus/es/components/config-provider/style/css",
        "@vueuse/core",
        "sortablejs",
        "path-to-regexp",
        "echarts",
        "@wangeditor/editor",
        "@wangeditor/editor-for-vue",
        "vue-i18n",
      ],
    },
    build: {
      target: "esnext",
      chunkSizeWarningLimit: 2000,
      minify: "terser",
      terserOptions: {
        compress: {
          keep_infinity: true,
          drop_console: true,
          drop_debugger: true,
        },
        format: {
          comments: false,
        },
      },
      rollupOptions: {
        output: {
          entryFileNames: "js/[name].[hash].js",
          chunkFileNames: "js/[name].[hash].js",
          assetFileNames: (assetInfo: any) => {
            const info = assetInfo.name.split(".");
            let extType = info[info.length - 1];
            if (
              /\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/i.test(assetInfo.name)
            ) {
              extType = "media";
            } else if (/\.(png|jpe?g|gif|svg)(\?.*)?$/.test(assetInfo.name)) {
              extType = "img";
            } else if (/\.(woff2?|eot|ttf|otf)(\?.*)?$/i.test(assetInfo.name)) {
              extType = "fonts";
            }
            return `${extType}/[name].[hash].[ext]`;
          },
        },
      },
    },
  };
});
```

### 第二步：配置代理

将模板中的 `{后端API地址}` 替换为用户提供的后端 API 地址。

如需添加额外代理路径（如报表服务），在 `proxy` 对象中追加：

```typescript
proxy: {
  [env.VITE_APP_BASE_API]: {
    changeOrigin: true,
    target: "{后端API地址}",
    rewrite: (path) =>
      path.replace(new RegExp("^" + env.VITE_APP_BASE_API), ""),
    xfwd: true,
  },
  // 追加额外代理路径
  "/ureport": {
    target: "{后端API地址}",
    xfwd: true,
  },
},
```

### 第三步：配置构建优化

确认以下构建优化已正确配置：

1. **Terser 压缩**: `drop_console: true`, `drop_debugger: true`
2. **分包告警阈值**: `chunkSizeWarningLimit: 2000`
3. **资源分类输出**: JS → `js/`, CSS → `css/`, 图片 → `img/`, 字体 → `fonts/`, 媒体 → `media/`
4. **依赖预编译**: `optimizeDeps.include` 包含所有常用 Element Plus 样式模块

## 完成后提醒

- 确认 `.env.development` 中 `VITE_APP_BASE_API` 与代理路径 `{代理路径}` 一致
- 确认 `.env.development` 中 `VITE_APP_PORT` 已设置（默认 9527）
- 修改 `vite.config.ts` 后需重启开发服务器（`pnpm run dev`）
- 仅允许使用 pnpm 作为包管理器
- `auto-import` 的 `dts: false`，新增自动导入配置需手动更新类型声明文件
- 新增 Element Plus 按需组件时，需将其样式模块加入 `optimizeDeps.include`
