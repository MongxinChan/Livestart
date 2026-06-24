import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { apiState, request } from '@/composables/infra/useRequest'
import type { EventSku, GrabStatus, LiveEvent, Visitor } from '@/types'
import { resolveEventStageMeta } from '@/utils/eventStage'

function normalizeId(value: number | string | null | undefined) {
  if (value == null) return null
  const normalizedValue = String(value).trim()
  return /^\d+$/.test(normalizedValue) ? normalizedValue : null
}

export function useTicketOrderCabin(
  props: { selectedEvent: LiveEvent | null },
  emit: {
    (e: 'backToSquare'): void
    (e: 'goToOrders'): void
  }
) {
  const activeSkuId = ref<string | null>(null)
  const ticketCount = ref(1)

  const activeSku = computed<EventSku | null>(() => {
    if (!props.selectedEvent || !activeSkuId.value) return null
    return props.selectedEvent.skus.find((sku) => String(sku.id) === activeSkuId.value) || null
  })

  const totalPrice = computed(() => (activeSku.value ? activeSku.value.price : 0) * ticketCount.value)
  const eventStageMeta = computed(() => resolveEventStageMeta(props.selectedEvent))

  const visitorList = ref<Visitor[]>([])
  const showVisitorManager = ref(false)

  async function fetchVisitors() {
    if (apiState.isMock) {
      visitorList.value = [
        { id: 201, name: '陈孟欣(开发者)', idCard: '3301**********1234', checked: true },
        { id: 202, name: '张学友(模拟)', idCard: '4402**********9988', checked: false },
        { id: 203, name: '李四(模拟)', idCard: '1103**********5678', checked: false },
      ]
      ticketCount.value = 1
      return
    }
    try {
      const list = await request<any[]>('/api/live-start/admin/v1/visitor/list')
      visitorList.value = (list || []).map((visitor, index) => ({
        id: visitor.id,
        name: visitor.realName,
        idCard: visitor.cardNo,
        checked: index === 0,
      }))
      ticketCount.value = visitorList.value.filter((visitor) => visitor.checked).length || 1
    } catch (err) {
      console.error('拉取观演人列表失败', err)
    }
  }

  function toggleVisitor(visitor: Visitor) {
    visitor.checked = !visitor.checked
    const count = visitorList.value.filter((item) => item.checked).length
    if (count > 0) {
      ticketCount.value = count
    } else {
      visitor.checked = true
    }
  }

  function onCountChange(val: number | null) {
    const nextCount = val || 1
    visitorList.value.forEach((visitor, index) => {
      visitor.checked = index < nextCount
    })
  }

  const grabStatus = ref<GrabStatus>('idle')
  const grabProgress = ref(0)
  const pathToken = ref('')
  const createdOrderNo = ref('')
  const grabError = ref('')

  async function triggerGrab() {
    if (!activeSku.value) return
    if (!eventStageMeta.value.canGrab) {
      message.warning(`当前阶段为“${eventStageMeta.value.statusText}”，暂时不能抢票`)
      return
    }
    const checkedVisitors = visitorList.value.filter((visitor) => visitor.checked)
    if (checkedVisitors.length !== ticketCount.value) {
      message.warning('观演人数与购买数量不符，无法提交下单')
      return
    }

    grabStatus.value = 'fetching_token'

    setTimeout(async () => {
      try {
        pathToken.value = await request<string>(`/api/live-start/engine/order/token?skuId=${activeSku.value!.id}`)
        grabStatus.value = 'grabbing'
        grabProgress.value = 15

        const interval = setInterval(() => {
          if (grabProgress.value < 90) {
            grabProgress.value += Math.floor(10 + Math.random() * 20)
          }
        }, 250)

        try {
          const orderNo = await request<string>(`/api/live-start/engine/order/create/${pathToken.value}`, {
            method: 'POST',
            body: JSON.stringify({
              skuId: activeSku.value!.id,
              count: ticketCount.value,
              visitorIds: checkedVisitors.map((visitor) => visitor.id),
            }),
          })

          clearInterval(interval)
          grabProgress.value = 100
          setTimeout(() => {
            createdOrderNo.value = orderNo
            grabStatus.value = 'success'
          }, 400)
        } catch (err: any) {
          clearInterval(interval)
          grabError.value = err.message
          grabStatus.value = 'failed'
        }
      } catch (err: any) {
        grabError.value = err.message
        grabStatus.value = 'failed'
      }
    }, 1200)
  }

  function resetGrab() {
    grabStatus.value = 'idle'
    grabProgress.value = 0
    pathToken.value = ''
    createdOrderNo.value = ''
    grabError.value = ''
  }

  const showStressPanel = ref(false)
  const stressConcurrency = ref(100)
  const stressInterval = ref(20)
  const stressRunning = ref(false)
  const stressTested = ref(false)
  const stressLogContainer = ref<HTMLElement | null>(null)
  const activeStressTasks = ref<Array<ReturnType<typeof setTimeout>>>([])

  const stressResults = reactive({
    total: 0,
    success: 0,
    rateLimit: 0,
    wafBlock: 0,
    soldOut: 0,
  })

  const stressLogs = ref<string[]>([])

  const successRate = computed(() => {
    if (stressResults.total === 0) return 0
    return Math.round((stressResults.success / stressResults.total) * 100)
  })

  function addLog(msg: string) {
    const now = new Date()
    const timeStr = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}.${String(now.getMilliseconds()).padStart(3, '0')}`
    stressLogs.value.push(`[${timeStr}] ${msg}`)

    setTimeout(() => {
      if (stressLogContainer.value) {
        stressLogContainer.value.scrollTop = stressLogContainer.value.scrollHeight
      }
    }, 10)
  }

  function stopStressTest() {
    stressRunning.value = false
    activeStressTasks.value.forEach((task) => clearTimeout(task))
    activeStressTasks.value = []
    addLog('压测已手动停止')
  }

  async function runStressTest() {
    if (!activeSku.value) {
      message.warning('请先选择一个有效的票档规格')
      return
    }
    const checkedVisitors = visitorList.value.filter((visitor) => visitor.checked)
    if (checkedVisitors.length !== ticketCount.value) {
      message.warning('观演人数与购买数量不符，无法提交下单')
      return
    }

    stressRunning.value = true
    stressTested.value = true
    stressResults.total = 0
    stressResults.success = 0
    stressResults.rateLimit = 0
    stressResults.wafBlock = 0
    stressResults.soldOut = 0
    stressLogs.value = []
    activeStressTasks.value = []

    addLog(`启动压测，目标并发 ${stressConcurrency.value}，请求间隔 ${stressInterval.value}ms`)
    addLog(`目标票档：${activeSku.value.name} (¥${activeSku.value.price})`)

    const totalRequests = stressConcurrency.value
    const interval = stressInterval.value

    const executeSingleRequest = async (index: number) => {
      if (!stressRunning.value) return

      addLog(`请求 #${index} -> 正在校验安全令牌`)

      if (Math.random() < 0.05) {
        stressResults.total++
        stressResults.wafBlock++
        addLog(`请求 #${index} -> [WAF BLOCKED] 被风控拦截`)
        return
      }

      if (Math.random() < 0.15) {
        stressResults.total++
        stressResults.rateLimit++
        addLog(`请求 #${index} -> [RATE LIMIT] 命中限流 (HTTP 429)`)
        return
      }

      try {
        const token = await request<string>(`/api/live-start/engine/order/token?skuId=${activeSku.value!.id}`)
        addLog(`请求 #${index} -> Token 获取成功 [${token.substring(0, 16)}...]`)

        if (!stressRunning.value) return

        const orderNo = await request<string>(`/api/live-start/engine/order/create/${token}`, {
          method: 'POST',
          body: JSON.stringify({
            skuId: activeSku.value!.id,
            count: ticketCount.value,
            visitorIds: checkedVisitors.map((visitor) => visitor.id),
          }),
        })

        if (!stressRunning.value) return

        stressResults.total++
        stressResults.success++
        addLog(`请求 #${index} -> [SUCCESS] 订单号 ${orderNo}`)
      } catch (err: any) {
        if (!stressRunning.value) return

        stressResults.total++
        const errMsg = err.message || ''
        if (errMsg.includes('售罄') || errMsg.includes('库存不足') || errMsg.includes('拦截')) {
          stressResults.soldOut++
          addLog(`请求 #${index} -> [SOLD OUT] ${errMsg}`)
        } else if (errMsg.includes('限流') || errMsg.includes('429')) {
          stressResults.rateLimit++
          addLog(`请求 #${index} -> [RATE LIMIT] ${errMsg}`)
        } else {
          stressResults.rateLimit++
          addLog(`请求 #${index} -> [FAILED] ${errMsg}`)
        }
      }
    }

    const promises: Promise<void>[] = []
    for (let i = 1; i <= totalRequests; i++) {
      const promise = new Promise<void>((resolve) => {
        const task = setTimeout(async () => {
          if (stressRunning.value) {
            await executeSingleRequest(i)
          }
          resolve()
        }, (i - 1) * interval)
        activeStressTasks.value.push(task)
      })
      promises.push(promise)
    }

    await Promise.all(promises)

    if (stressRunning.value) {
      stressRunning.value = false
      addLog(`压测结束。总请求 ${stressResults.total}，成功 ${stressResults.success}，限流 ${stressResults.rateLimit}，WAF ${stressResults.wafBlock}，售罄 ${stressResults.soldOut}`)
      message.success('并发压测结束，请到电子票夹查看已生成订单')
    }
  }

  watch(
    () => props.selectedEvent,
    (event) => {
      if (event && event.skus.length > 0) {
        activeSkuId.value = normalizeId(event.skus[0].id)
      }
      void fetchVisitors()
    }
  )

  onMounted(() => {
    void fetchVisitors()
    if (props.selectedEvent && props.selectedEvent.skus.length > 0) {
      activeSkuId.value = normalizeId(props.selectedEvent.skus[0].id)
    }
  })

  return {
    activeSkuId,
    ticketCount,
    activeSku,
    totalPrice,
    eventStageMeta,
    visitorList,
    showVisitorManager,
    fetchVisitors,
    toggleVisitor,
    onCountChange,
    grabStatus,
    grabProgress,
    pathToken,
    createdOrderNo,
    grabError,
    triggerGrab,
    resetGrab,
    showStressPanel,
    stressConcurrency,
    stressInterval,
    stressRunning,
    stressTested,
    stressLogContainer,
    stressResults,
    stressLogs,
    successRate,
    stopStressTest,
    runStressTest,
  }
}
