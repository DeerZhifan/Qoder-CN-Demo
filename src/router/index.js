import { createRouter, createWebHistory } from 'vue-router'
import DocumentList from '@/views/knowledge-base/DocumentList.vue'
import DocumentEditor from '@/views/knowledge-base/DocumentEditor.vue'

const routes = [
  {
    path: '/',
    redirect: '/knowledge-base/list'
  },
  {
    path: '/knowledge-base',
    component: () => import('@/layouts/KnowledgeBaseLayout.vue'),
    children: [
      {
        path: 'list',
        name: 'DocumentList',
        component: DocumentList,
        meta: { title: '文档列表' }
      },
      {
        path: 'editor/:id?',
        name: 'DocumentEditor',
        component: DocumentEditor,
        meta: { title: '文档编辑' }
      }
    ]
  },
  {
    path: '/wiki',
    component: () => import('@/layouts/WikiLayout.vue'),
    children: [
      {
        path: ':pathMatch(.*)*',
        component: () => import('@/components/WikiContent.vue'),
        meta: { title: 'Wiki浏览' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
