/**
 * [场景:Mock数据配置] — vite-plugin-mock-dev-server 配置
 *
 * 启用步骤:
 * 1. 取消注释 vite.config.ts 中的 mockDevServerPlugin()
 * 2. 在 mock/ 目录下创建 Mock 文件
 * 3. 重启开发服务器
 */

// ============================================================
// mock/example.mock.ts — Mock 数据文件示例
// ============================================================

import { defineMock } from "vite-plugin-mock-dev-server";

export default defineMock([
  // GET 请求
  {
    url: "/api/example/list",
    method: "GET",
    body: {
      code: 200,
      msg: "success",
      data: {
        list: [
          { id: "1", name: "示例一", status: 1, createTime: "2026-01-01" },
          { id: "2", name: "示例二", status: 0, createTime: "2026-01-02" },
        ],
        total: 2,
      },
    },
  },

  // POST 请求（JSON）
  {
    url: "/api/example/page",
    method: "POST",
    body: ({ body }) => {
      // body 是请求参数
      const { pageNum = 1, pageSize = 10 } = body || {};
      const list = Array.from({ length: pageSize }, (_, i) => ({
        id: String((pageNum - 1) * pageSize + i + 1),
        name: `数据 ${(pageNum - 1) * pageSize + i + 1}`,
        status: i % 2,
        createTime: "2026-04-06",
      }));
      return {
        code: 200,
        msg: "success",
        data: { list, total: 100 },
      };
    },
  },

  // 带延迟的请求
  {
    url: "/api/example/detail/:id",
    method: "GET",
    delay: 500, // 模拟 500ms 延迟
    body: ({ params }) => ({
      code: 200,
      data: {
        id: params.id,
        name: `详情 ${params.id}`,
        description: "这是一条 Mock 数据",
      },
    }),
  },
]);

/**
 * vite.config.ts 中启用 Mock:
 *
 * import mockDevServerPlugin from "vite-plugin-mock-dev-server";
 *
 * plugins: [
 *   mockDevServerPlugin(),  // 取消注释
 * ],
 *
 * server: {
 *   proxy: {
 *     "/api/": {
 *       target: "http://localhost:9527", // 指向本地
 *     },
 *   },
 * },
 */
