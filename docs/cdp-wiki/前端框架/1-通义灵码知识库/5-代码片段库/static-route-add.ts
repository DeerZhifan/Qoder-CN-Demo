/**
 * [场景:添加静态路由]
 *
 * 静态路由适用于: 登录页、错误页、公开页面等不需要权限控制的页面
 * 动态路由（菜单页面）由后端 initMenu 接口返回，不在此处配置
 *
 * 源码参考: src/cdp-common-frame/router/constantRoutes.ts
 */

import type { RouteRecordRaw } from "vue-router";

// 方式一：嵌套在 Layout 内的页面（有侧边栏/导航栏）
const routeWithLayout: RouteRecordRaw = {
  path: "/my-page",
  component: () => import("@/cdp-admin/layout/index.vue"),
  children: [
    {
      path: "",
      name: "MyPage",
      component: () => import("@/cdp-common-frame/my-module/views/index.vue"),
      meta: {
        title: "我的页面",
        icon: "document",  // 菜单图标
        hidden: false,      // 是否在菜单中隐藏
        affix: false,       // 标签页是否固定
        keepAlive: false,   // 是否缓存组件
      },
    },
  ],
};

// 方式二：独立页面（无 Layout，如登录页）
const routeStandalone: RouteRecordRaw = {
  path: "/public-page",
  name: "PublicPage",
  component: () => import("@/cdp-common-frame/my-module/views/public.vue"),
  meta: { title: "公开页面" },
};

// 导出（在 _changeable/router.ts 中合并）
export const constantRoutes: RouteRecordRaw[] = [
  routeWithLayout,
  routeStandalone,
];

/**
 * 聚合方式 — 在 src/_changeable/router.ts 中:
 *
 * import { constantRoutes as myRoutes } from "@/cdp-common-frame/my-module/router";
 *
 * export const constantRoutes = [
 *   ...cdpRoutes,
 *   ...myRoutes,    // ← 合并自定义路由
 * ];
 *
 * 如果是免登录页面，还需在 permission.ts 的 whiteList 中添加路径:
 * const whiteList = ["/login", "/public-page"];
 */
