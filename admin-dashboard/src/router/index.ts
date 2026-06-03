import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '数据看板' },
      },
      {
        path: 'event',
        name: 'EventList',
        component: () => import('@/views/event/List.vue'),
        meta: { title: '演出管理' },
      },
      {
        path: 'venue',
        name: 'VenueList',
        component: () => import('@/views/venue/List.vue'),
        meta: { title: '场馆管理' },
      },
      {
        path: 'ticket-sku',
        name: 'TicketSkuList',
        component: () => import('@/views/ticket-sku/List.vue'),
        meta: { title: '票档管理' },
      },
      {
        path: 'performer',
        name: 'PerformerList',
        component: () => import('@/views/performer/List.vue'),
        meta: { title: '艺人管理' },
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/List.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/order/List.vue'),
        meta: { title: '订单管理' },
      },
      {
        path: 'settlement',
        name: 'SettlementReport',
        component: () => import('@/views/settlement/Report.vue'),
        meta: { title: '结算报表' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  document.title = `${(to.meta as any).title || 'Livestart'} - 管理后台`
  next()
})

export default router
