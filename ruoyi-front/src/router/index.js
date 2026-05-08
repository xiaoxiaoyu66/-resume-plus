import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Home',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/Chat.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue')
  },
  {
    path: '/user-info',
    name: 'UserInfo',
    component: () => import('@/views/UserInfo.vue')
  },
  {
    path: '/chat/history',
    name: 'ChatHistory',
    component: () => import('@/views/ChatHistory.vue')
  },
  {
    path: '/resume',
    name: 'ResumeList',
    component: () => import('@/views/resume/list/index.vue')
  },
  {
    path: '/resume/edit/:id',
    name: 'ResumeEdit',
    component: () => import('@/views/resume/edit/index.vue'),
    meta: { fullScreen: true }
  },
  {
    path: '/jiangcheng',
    name: 'Jiangcheng',
    component: () => import('@/views/jiangcheng/index.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = getToken()
  
  // 如果是公开页面，直接放行
  if (to.meta.public) {
    // 如果已登录，跳转到首页
    if (token && to.path === '/login') {
      next('/chat')
    } else {
      next()
    }
    return
  }
  
  // 需要登录的页面
  if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
