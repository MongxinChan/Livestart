import { createRouter, createWebHistory } from 'vue-router'
import { apiState } from '@/composables/infra/useRequest'
import { fetchEventById } from '@/composables/event/useEventCatalog'
import { useEventAccess } from '@/composables/event/useEventAccess'

function normalizeRouteEventId(id: unknown) {
  if (id == null) return null
  const value = String(id).trim()
  return /^\d+$/.test(value) ? value : null
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/ClientLayout.vue'),
      children: [
        {
          path: '',
          name: 'Square',
          component: () => import('@/views/square/Index.vue'),
          meta: { title: '演出广场', navKey: 'square' },
        },
        {
          path: 'event/:id',
          name: 'EventDetail',
          component: () => import('@/views/event-detail/Index.vue'),
          meta: { title: '演出详情', navKey: 'square' },
        },
        {
          path: 'order/cabin/:id',
          name: 'OrderCabin',
          component: () => import('@/views/cabin/Index.vue'),
          meta: { title: '票仓下单', navKey: 'square' },
        },
        {
          path: 'orders',
          name: 'Orders',
          component: () => import('@/views/orders/Index.vue'),
          meta: { title: '我的票夹', navKey: 'orders', requiresAuth: true },
        },
        {
          path: 'reminders',
          name: 'Reminders',
          component: () => import('@/views/reminders/Index.vue'),
          meta: { title: '我的提醒', navKey: 'reminders', requiresAuth: true },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  document.title = `${to.meta.title || 'Livestart'} - 客户端`

  if (to.meta.requiresAuth && !apiState.token) {
    return {
      name: 'Square',
      query: { auth: '1', redirect: to.fullPath },
    }
  }

  if (to.name === 'OrderCabin') {
    const eventId = normalizeRouteEventId(to.params.id)
    const { canAccessCabin, setSelectedEvent } = useEventAccess()
    if (!eventId) {
      return { name: 'Square' }
    }

    if (!canAccessCabin(eventId)) {
      const event = await fetchEventById(eventId)
      if (event) {
        setSelectedEvent(event)
        return { name: 'EventDetail', params: { id: eventId } }
      }
      return { name: 'Square' }
    }
  }

  return true
})

export default router
