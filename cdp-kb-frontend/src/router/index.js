import { createRouter, createWebHistory } from 'vue-router'
import DocumentList from '../views/DocumentList.vue'
import DocumentEditor from '../views/DocumentEditor.vue'
import CategoryManage from '../views/CategoryManage.vue'

const routes = [
  { path: '/', redirect: '/documents' },
  { path: '/documents', component: DocumentList },
  { path: '/documents/create', component: DocumentEditor },
  { path: '/documents/:id/edit', component: DocumentEditor, props: true },
  { path: '/categories', component: CategoryManage }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
