import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { request, apiState } from '@/composables/infra/useRequest'
import type { Order, TicketItem } from '@/types'

export function useMyTickets() {
  const orders = ref<Order[]>([])

  async function fetchOrders() {
    try {
      const data = await request<{ records: Order[] }>('/api/live-start/engine/order/page?current=1&size=50')
      orders.value = (data.records || []).map((order) => ({ ...order }))
    } catch (err) {
      console.error('拉取订单失败', err)
    }
  }

  function orderStatusColor(status: number) {
    const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'default', 3: 'error' }
    return map[status] || 'default'
  }

  const showCheckout = ref(false)
  const payingOrder = ref<Order | null>(null)
  const isPaying = ref(false)
  const showTicketDetails = ref(false)
  const ticketDetailLoading = ref(false)
  const ticketItems = ref<TicketItem[]>([])

  async function openTicketDetails(order: Order) {
    showTicketDetails.value = true
    ticketDetailLoading.value = true
    ticketItems.value = []
    try {
      const detail = await request<{ ticketItems: TicketItem[] }>(
        `/api/live-start/engine/order/detail/${encodeURIComponent(order.orderNo)}`
      )
      ticketItems.value = detail.ticketItems || []
    } catch (err: any) {
      message.error(`加载电子票失败: ${err.message}`)
    } finally {
      ticketDetailLoading.value = false
    }
  }

  function openCheckoutModal(order: Order) {
    payingOrder.value = order
    showCheckout.value = true
  }

  async function startPayment() {
    if (!payingOrder.value || isPaying.value) return
    isPaying.value = true
    if (!apiState.isMock) {
      try {
        const payFormHtml = await request<string>(`/api/live-start/engine/order/pay/alipay?orderNo=${payingOrder.value.orderNo}`)
        const div = document.createElement('div')
        div.innerHTML = payFormHtml
        const form = div.querySelector('form')
        if (!form) throw new Error('支付宝表单解析失败')
        document.body.appendChild(form)
        form.submit()
      } catch (err: any) {
        message.error(`发起支付宝支付失败: ${err.message}`)
      } finally {
        isPaying.value = false
      }
      return
    }

    try {
      await request('/api/live-start/engine/order/pay-callback', {
        method: 'POST',
        body: JSON.stringify({
          orderNo: payingOrder.value.orderNo,
          tradeNo: `TRADE-${Date.now()}`,
          payAmount: payingOrder.value.totalAmount,
        }),
      })
      showCheckout.value = false
      message.success('支付成功，电子票已出票')
      void fetchOrders()
    } catch (err: any) {
      message.error(`支付对账失败: ${err.message}`)
    } finally {
      isPaying.value = false
    }
  }

  async function cancelOrder(orderNo: string) {
    try {
      await request('/api/live-start/engine/order/cancel', { method: 'POST', body: JSON.stringify({ orderNo }) })
      message.success('取消成功，库存已回流')
      void fetchOrders()
    } catch (err: any) {
      message.error(`取消失败: ${err.message}`)
    }
  }

  async function refundOrder(orderNo: string) {
    Modal.confirm({
      title: '确定要申请退票吗？',
      content: '资金与门票库存都会退回。',
      async onOk() {
        try {
          await request('/api/live-start/engine/order/refund', { method: 'POST', body: JSON.stringify({ orderNo }) })
          message.success('退票成功，库存已归还')
          void fetchOrders()
        } catch (err: any) {
          message.error(`退票失败: ${err.message}`)
        }
      },
    })
  }


  onMounted(() => {
    void fetchOrders()
  })

  return {
    orders,
    showCheckout,
    payingOrder,
    isPaying,
    showTicketDetails,
    ticketDetailLoading,
    ticketItems,
    isMock: apiState.isMock,
    fetchOrders,
    orderStatusColor,
    openCheckoutModal,
    openTicketDetails,
    startPayment,
    cancelOrder,
    refundOrder,
  }
}
