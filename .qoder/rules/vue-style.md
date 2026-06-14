---
trigger: glob
glob: src/**/*.vue
---
Vue3 统一 `<script setup>` + Composition API + Element Plus；
所有后端请求必须通过 `@/api/*` 模块（底层走 `@/utils/request`），禁止在组件里直接 `import axios` 或拼接 `/api/...` URL。