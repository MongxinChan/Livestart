import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getAdminToken, getCurrentUserType, UserRole, type UserRoleValue } from '@/api/http'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    /** 仅这些角色可见；不配置默认任何后台已登录用户都能访问 */
    allowRoles?: UserRoleValue[]
  }
}

/** 只有超管可见的菜单 */
const SUPER_ONLY: UserRoleValue[] = [UserRole.SuperAdmin]
/** 超管 + 场地管理员都能看（数据按角色过滤） */
const ADMIN_AND_VENUE: UserRoleValue[] = [UserRole.SuperAdmin, UserRole.VenueAdmin]

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
        meta: { title: '数据看板', requiresAuth: true, allowRoles: ADMIN_AND_VENUE },
      },
      {
        path: 'event',
        name: 'EventList',
        component: () => import('@/views/event/List.vue'),
        meta: { title: '演出管理', requiresAuth: true, allowRoles: ADMIN_AND_VENUE },
      },
      {
        path: 'venue',
        name: 'VenueList',
        component: () => import('@/views/venue/List.vue'),
        meta: { title: '场馆管理', requiresAuth: true, allowRoles: SUPER_ONLY },
      },
      {
        path: 'ticket-sku',
        name: 'TicketSkuList',
        component: () => import('@/views/ticket-sku/List.vue'),
        meta: { title: '票档管理', requiresAuth: true, allowRoles: ADMIN_AND_VENUE },
      },
      {
        path: 'performer',
        name: 'PerformerList',
        component: () => import('@/views/performer/List.vue'),
        meta: { title: '艺人管理', requiresAuth: true, allowRoles: SUPER_ONLY },
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/List.vue'),
        meta: { title: '用户管理', requiresAuth: true, allowRoles: SUPER_ONLY },
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/order/List.vue'),
        meta: { title: '订单管理', requiresAuth: true, allowRoles: ADMIN_AND_VENUE },
      },
      {
        path: 'settlement',
        name: 'SettlementReport',
        component: () => import('@/views/settlement/Report.vue'),
        meta: { title: '结算报表', requiresAuth: true, allowRoles: ADMIN_AND_VENUE },
      },
      {
        path: 'style',
        name: 'StyleList',
        component: () => import('@/views/style/List.vue'),
        meta: { title: '风格管理', requiresAuth: true, allowRoles: SUPER_ONLY },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from) => {
  document.title = `${to.meta.title || 'Livestart'} - 管理后台`

  const token = getAdminToken()
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

  console.log('[Router Guard]', {
    to: to.path,
    from: from.path,
    hasToken: !!token,
    requiresAuth,
    allowRoles: to.meta.allowRoles,
    currentUserType: getCurrentUserType(),
  })

  if (to.path === '/login' && token) {
    console.log('[Router Guard] 已登录用户访问登录页，重定向到 /dashboard')
    return '/dashboard'
  }

  if (requiresAuth && !token) {
    console.log('[Router Guard] 未登录用户访问需要认证的页面，重定向到 /login')
    return '/login'
  }

  // 角色守卫：路由 meta.allowRoles 限定时校验
  if (token && to.meta.allowRoles && to.meta.allowRoles.length > 0) {
    const userType = getCurrentUserType()
    if (!userType) {
      // userType 未加载：这通常意味着 /me 接口还没调用或调用失败
      // 清理 token 并要求重新登录
      console.error('[Router Guard] userType 未加载，清理 session 并跳转登录页')
      return '/login'
    }
    if (!to.meta.allowRoles.includes(userType)) {
      // userType 不匹配：用户角色不符合要求
      console.error('[Router Guard] 用户角色不符合要求', {
        userType,
        requiredRoles: to.meta.allowRoles,
      })
      // 这里不应该重定向到 /dashboard，因为 /dashboard 也有角色限制
      // 应该显示无权限页面或跳转到第一个有权限的页面
      // 暂时跳转到登录页，让用户重新登录
      return '/login'
    }
  }

  console.log('[Router Guard] 通过守卫检查')
  return true
})

export default router
