import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { request, apiState } from '@/composables/infra/useRequest'
import type { Order } from '@/types'

export function useMyTickets() {
  const orders = ref<Order[]>([])

  async function fetchOrders() {
    try {
      const data = await request<{ records: Order[] }>('/api/live-start/engine/order/page?current=1&size=50')
      orders.value = data.records
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
  const payMethod = ref('wx')

  function openCheckoutModal(order: Order) {
    payingOrder.value = order
    showCheckout.value = true
  }

  async function confirmMockPay() {
    if (!payingOrder.value) return
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
      const osc = audioCtx.createOscillator()
      const gain = audioCtx.createGain()
      osc.connect(gain)
      gain.connect(audioCtx.destination)
      osc.type = 'sine'
      osc.frequency.setValueAtTime(1046.5, audioCtx.currentTime)
      gain.gain.setValueAtTime(0.08, audioCtx.currentTime)
      osc.start()
      setTimeout(() => osc.stop(), 180)
    } catch (_) {
      // 忽略音频错误
    }

    if (payMethod.value === 'alipay' && !apiState.isMock) {
      try {
        const payFormHtml = await request<string>(`/api/live-start/engine/order/pay/alipay?orderNo=${payingOrder.value.orderNo}`)
        const div = document.createElement('div')
        div.innerHTML = payFormHtml
        document.body.appendChild(div)
        const form = div.querySelector('form')
        if (form) {
          form.submit()
        } else {
          message.error('支付宝表单解析失败')
        }
        return
      } catch (err: any) {
        message.error(`发起支付宝支付失败: ${err.message}`)
        return
      }
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

  function performCheckCode(order: Order) {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
      const osc = audioCtx.createOscillator()
      const gain = audioCtx.createGain()
      osc.connect(gain)
      gain.connect(audioCtx.destination)
      osc.type = 'triangle'
      osc.frequency.setValueAtTime(120, audioCtx.currentTime)
      gain.gain.setValueAtTime(0.12, audioCtx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.15)
      osc.start()
      setTimeout(() => osc.stop(), 150)
    } catch (_) {
      // 忽略音频错误
    }

    order.isChecked = 1
    message.success('核销成功，该电子票已失效')
  }

  onMounted(() => {
    void fetchOrders()
  })

  return {
    orders,
    showCheckout,
    payingOrder,
    payMethod,
    fetchOrders,
    orderStatusColor,
    openCheckoutModal,
    confirmMockPay,
    cancelOrder,
    refundOrder,
    performCheckCode,
  }
}
