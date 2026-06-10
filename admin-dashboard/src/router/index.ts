import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getAdminToken } from '@/api/http'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Index.vue'),
    meta: { title: '管理员登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '数据看板', requiresAuth: true },
      },
      {
        path: 'event',
        name: 'EventList',
        component: () => import('@/views/event/List.vue'),
        meta: { title: '演出管理', requiresAuth: true },
      },
      {
        path: 'venue',
        name: 'VenueList',
        component: () => import('@/views/venue/List.vue'),
        meta: { title: '场馆管理', requiresAuth: true },
      },
      {
        path: 'ticket-sku',
        name: 'TicketSkuList',
        component: () => import('@/views/ticket-sku/List.vue'),
        meta: { title: '票档管理', requiresAuth: true },
      },
      {
        path: 'performer',
        name: 'PerformerList',
        component: () => import('@/views/performer/List.vue'),
        meta: { title: '艺人管理', requiresAuth: true },
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/List.vue'),
        meta: { title: '用户管理', requiresAuth: true },
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/order/List.vue'),
        meta: { title: '订单管理', requiresAuth: true },
      },
      {
        path: 'settlement',
        name: 'SettlementReport',
        component: () => import('@/views/settlement/Report.vue'),
        meta: { title: '结算报表', requiresAuth: true },
      },
      {
        path: 'style',
        name: 'StyleList',
        component: () => import('@/views/style/List.vue'),
        meta: { title: '风格管理', requiresAuth: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  document.title = `${to.meta.title || 'Livestart'} - 管理后台`

  const token = getAdminToken()
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

  if (to.path === '/login' && token) {
    return '/dashboard'
  }

  if (requiresAuth && !token) {
    return '/login'
  }

  return true
})

export default router
