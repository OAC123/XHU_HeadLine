import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '@/views/layout/index.vue'
import LoginView from '@/views/Login/index.vue'
import UserView from '@/views/user/index.vue'
import PortView from '@/port/index.vue'
import PortEditor from '@/port/editor.vue'
import CommentView from '@/views/comment/index.vue'


const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
  },
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        redirect: '/dashboard', // 修改：默认重定向到仪表盘
      },
      {
        path: '/dashboard', // 新增：仪表盘路由
        name: 'Dashboard',
        // 使用路由懒加载，优化首屏性能
        component: () => import('@/views/dashboard/index.vue'),
      },
      {
        path: '/user',
        name: 'User',
        component: UserView,
      },
      {
        path: '/port',
        name: 'Port',
        component: PortView,
      },
      {
        path: '/port/editor/:id?',
        name: 'PortEditor',
        component: PortEditor,
      },
      // 评论路由
      {
        path: '/comments',
        name: 'Comments',
        component: CommentView,
      },
       // 审核管理路由
      {
        path: '/audit',
        name: 'Audit',
        component: () => import('@/port/audit.vue'),
      },
      
    ],
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// 简单路由守卫：未登录跳转登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('auth_token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/') // 修改：已登录状态访问登录页，跳转到首页（仪表盘）
  } else {
    next()
  }
})

export default router