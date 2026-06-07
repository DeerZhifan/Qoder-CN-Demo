// Mock分类树数据
export const mockCategoryTree = [
  {
    id: 1,
    parentId: null,
    name: '技术文档',
    sortOrder: 1,
    children: [
      {
        id: 2,
        parentId: 1,
        name: '后端开发',
        sortOrder: 1,
        children: []
      },
      {
        id: 3,
        parentId: 1,
        name: '前端开发',
        sortOrder: 2,
        children: []
      }
    ]
  },
  {
    id: 4,
    parentId: null,
    name: '产品文档',
    sortOrder: 2,
    children: []
  }
]

// Mock文档列表数据
export const mockDocuments = [
  {
    id: 1,
    categoryId: 2,
    categoryName: '后端开发',
    title: 'Spring Boot入门指南',
    content: '# Spring Boot入门指南\n\n## 概述\n\nSpring Boot是一个用于简化Spring应用开发的框架...',
    status: 'PUBLISHED',
    version: 2,
    publishTime: '2026-06-07T10:00:00',
    createTime: '2026-06-01T09:00:00',
    createBy: '张三',
    updateTime: '2026-06-07T10:00:00',
    updateBy: '张三'
  },
  {
    id: 2,
    categoryId: 3,
    categoryName: '前端开发',
    title: 'Vue3 Composition API',
    content: '# Vue3 Composition API\n\n## 简介\n\nComposition API是Vue3引入的新特性...',
    status: 'DRAFT',
    version: 1,
    publishTime: null,
    createTime: '2026-06-05T14:00:00',
    createBy: '李四',
    updateTime: '2026-06-05T14:00:00',
    updateBy: '李四'
  },
  {
    id: 3,
    categoryId: 2,
    categoryName: '后端开发',
    title: 'MyBatis Plus最佳实践',
    content: '# MyBatis Plus最佳实践\n\n## 介绍\n\nMyBatis Plus是一个MyBatis的增强工具...',
    status: 'PUBLISHED',
    version: 3,
    publishTime: '2026-06-06T15:30:00',
    createTime: '2026-05-28T10:00:00',
    createBy: '王五',
    updateTime: '2026-06-06T15:30:00',
    updateBy: '王五'
  },
  {
    id: 4,
    categoryId: 3,
    categoryName: '前端开发',
    title: 'TypeScript高级类型技巧',
    content: '# TypeScript高级类型技巧\n\n## 泛型约束\n\nTypeScript的泛型系统非常强大...',
    status: 'OFFLINE',
    version: 2,
    publishTime: '2026-06-03T09:00:00',
    createTime: '2026-05-25T11:00:00',
    createBy: '赵六',
    updateTime: '2026-06-04T16:00:00',
    updateBy: '赵六'
  }
]

// Mock文档版本历史数据
export const mockDocumentVersions = {
  1: [
    {
      versionId: 101,
      version: 2,
      title: 'Spring Boot入门指南',
      content: '# Spring Boot入门指南\n\n## 概述\n\nSpring Boot是一个用于简化Spring应用开发的框架...',
      createTime: '2026-06-07T10:00:00',
      createBy: '张三'
    },
    {
      versionId: 102,
      version: 1,
      title: 'Spring Boot基础',
      content: '# Spring Boot基础\n\n## 初始版本内容...',
      createTime: '2026-06-01T09:00:00',
      createBy: '张三'
    }
  ],
  2: [
    {
      versionId: 201,
      version: 1,
      title: 'Vue3 Composition API',
      content: '# Vue3 Composition API\n\n## 简介\n\nComposition API是Vue3引入的新特性...',
      createTime: '2026-06-05T14:00:00',
      createBy: '李四'
    }
  ],
  3: [
    {
      versionId: 301,
      version: 3,
      title: 'MyBatis Plus最佳实践',
      content: '# MyBatis Plus最佳实践\n\n## 介绍\n\nMyBatis Plus是一个MyBatis的增强工具...',
      createTime: '2026-06-06T15:30:00',
      createBy: '王五'
    },
    {
      versionId: 302,
      version: 2,
      title: 'MyBatis Plus使用指南',
      content: '# MyBatis Plus使用指南\n\n## 第二版内容...',
      createTime: '2026-06-02T10:00:00',
      createBy: '王五'
    },
    {
      versionId: 303,
      version: 1,
      title: 'MyBatis Plus入门',
      content: '# MyBatis Plus入门\n\n## 初始版本...',
      createTime: '2026-05-28T10:00:00',
      createBy: '王五'
    }
  ]
}
