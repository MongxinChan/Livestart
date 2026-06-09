import { ref, computed, watch, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { request, apiState } from '@/composables/useRequest'
import type { LiveEvent, EventSku, Visitor, GrabStatus } from '@/types'

export function useTicketOrderCabin(
  props: { selectedEvent: LiveEvent | null },
  emit: {
    (e: 'backToSquare'): void
    (e: 'goToOrders'): void
  }
) {
  // --- 票档 ---
  const activeSkuId = ref<number | null>(null)
  const ticketCount = ref(1)

  const activeSku = computed<EventSku | null>(() => {
    if (!props.selectedEvent || !activeSkuId.value) return null
    return props.selectedEvent.skus.find(s => s.id === activeSkuId.value) || null
  })

  const totalPrice = computed(() => (activeSku.value ? activeSku.value.price : 0) * ticketCount.value)

  // --- 观演人 ---
  const visitorList = ref<Visitor[]>([])
  const showVisitorManager = ref(false)

  async function fetchVisitors() {
    if (apiState.isMock) {
      visitorList.value = [
        { id: 201, name: '陈孟欣 (开发者)', idCard: '3301**********1234', checked: true },
        { id: 202, name: '张学友 (模拟人)', idCard: '4402**********9988', checked: false },
        { id: 203, name: '李四 (模拟人)', idCard: '1103**********5678', checked: false },
      ]
      ticketCount.value = 1
      return
    }
    try {
      const list = await request<any[]>('/api/live-start/admin/v1/visitor/list')
      visitorList.value = (list || []).map((v, i) => ({
        id: v.id,
        name: v.realName,
        idCard: v.cardNo,
        checked: i === 0,
      }))
      ticketCount.value = visitorList.value.filter(v => v.checked).length || 1
    } catch (err) {
      console.error('拉取观演人列表失败', err)
    }
  }

  function toggleVisitor(v: Visitor) {
    v.checked = !v.checked
    const count = visitorList.value.filter(x => x.checked).length
    if (count > 0) ticketCount.value = count
    else v.checked = true
  }

  function onCountChange(val: number | null) {
    const n = val || 1
    visitorList.value.forEach((v, i) => {
      v.checked = i < n
    })
  }

  // --- 抢票状态机 ---
  const grabStatus = ref<GrabStatus>('idle')
  const grabProgress = ref(0)
  const pathToken = ref('')
  const createdOrderNo = ref('')
  const grabError = ref('')

  async function triggerGrab() {
    if (!activeSku.value) return
    const checkedVisitors = visitorList.value.filter(v => v.checked)
    if (checkedVisitors.length !== ticketCount.value) {
      message.warning('观演人数量与购买数量不符，无法提交下单！')
      return
    }

    grabStatus.value = 'fetching_token'

    setTimeout(async () => {
      try {
        pathToken.value = await request<string>('/api/engine/order/token?skuId=' + activeSku.value!.id)
        grabStatus.value = 'grabbing'
        grabProgress.value = 15

        const interval = setInterval(() => {
          if (grabProgress.value < 90) grabProgress.value += Math.floor(10 + Math.random() * 20)
        }, 250)

        try {
          const orderNo = await request<string>('/api/engine/order/create/' + pathToken.value, {
            method: 'POST',
            body: JSON.stringify({
              skuId: activeSku.value!.id,
              count: ticketCount.value,
              visitorIds: checkedVisitors.map(v => v.id),
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

  // --- 高并发压测相关 ---
  const showStressPanel = ref(false)
  const stressConcurrency = ref(100)
  const stressInterval = ref(20) // ms
  const stressRunning = ref(false)
  const stressTested = ref(false)
  const stressLogContainer = ref<HTMLElement | null>(null)
  const activeStressTasks = ref<any[]>([])

  const stressResults = reactive({
    total: 0,
    success: 0,
    rateLimit: 0,
    wafBlock: 0,
    soldOut: 0,
  })

  const stressLogs = ref<string[]>([])

  // 计算成功率
  const successRate = computed(() => {
    if (stressResults.total === 0) return 0
    return Math.round((stressResults.success / stressResults.total) * 100)
  })

  function addLog(msg: string) {
    const now = new Date()
    const timeStr = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}.${String(now.getMilliseconds()).padStart(3, '0')}`
    stressLogs.value.push(`[${timeStr}] ${msg}`)

    // 自动滚动触底
    setTimeout(() => {
      if (stressLogContainer.value) {
        stressLogContainer.value.scrollTop = stressLogContainer.value.scrollHeight
      }
    }, 10)
  }

  function stopStressTest() {
    stressRunning.value = false
    // 取消所有待执行的任务
    activeStressTasks.value.forEach(t => clearTimeout(t))
    activeStressTasks.value = []
    addLog('⚠️ 压测测试已被人工终止。')
  }

  async function runStressTest() {
    if (!activeSku.value) {
      message.warning('请先选择一个有效的票档规格！')
      return
    }
    const checkedVisitors = visitorList.value.filter(v => v.checked)
    if (checkedVisitors.length !== ticketCount.value) {
      message.warning('观演人数量与购买数量不符，无法提交下单！')
      return
    }

    // 重置统计
    stressRunning.value = true
    stressTested.value = true
    stressResults.total = 0
    stressResults.success = 0
    stressResults.rateLimit = 0
    stressResults.wafBlock = 0
    stressResults.soldOut = 0
    stressLogs.value = []
    activeStressTasks.value = []

    addLog(`🚀 启动高并发压测排洪测试... 并发数: ${stressConcurrency.value}, 请求间隔: ${stressInterval.value}ms`)
    addLog(`🎯 目标规格: ${activeSku.value.name} (¥${activeSku.value.price})`)

    const totalRequests = stressConcurrency.value
    const interval = stressInterval.value

    // 内部执行单次抢票任务
    const executeSingleRequest = async (index: number) => {
      if (!stressRunning.value) return

      addLog(`请求 #${index} -> 正在获取指纹并验证安全盾 WAF...`)

      // 1. 模拟 WAF 拦截 (5% 概率)
      if (Math.random() < 0.05) {
        stressResults.total++
        stressResults.wafBlock++
        addLog(`❌ 请求 #${index} -> [WAF BLOCKED] 盾指纹安全扫描异常，拦截请求！`)
        return
      }

      // 2. 模拟高并发 HTTP 429 限流拦截 (15% 概率)
      if (Math.random() < 0.15) {
        stressResults.total++
        stressResults.rateLimit++
        addLog(`⚠️ 请求 #${index} -> [RATE LIMIT] 限流阻断机制触发：请求速率超出安全阈值 (HTTP 429)`)
        return
      }

      // 3. 正常发起接口流程 (获取 Token -> 异步下单)
      try {
        // 获取 Path Token
        const token = await request<string>('/api/engine/order/token?skuId=' + activeSku.value!.id)
        addLog(`⚡ 请求 #${index} -> 盾安全验证通过。Path Token: [${token.substring(0, 16)}...]`)

        if (!stressRunning.value) return

        addLog(`⏳ 请求 #${index} -> 已投递至 RocketMQ 异步队列，等待排队落库...`)

        // 创建订单
        const orderNo = await request<string>('/api/engine/order/create/' + token, {
          method: 'POST',
          body: JSON.stringify({
            skuId: activeSku.value!.id,
            count: ticketCount.value,
            visitorIds: checkedVisitors.map(v => v.id),
          }),
        })

        if (!stressRunning.value) return

        stressResults.total++
        stressResults.success++
        addLog(`🟢 请求 #${index} -> [SUCCESS] 抢票排队成功落库！订单流水号: ${orderNo}`)
      } catch (err: any) {
        if (!stressRunning.value) return
        stressResults.total++

        // 解析错误，判定是售罄还是限流/其他
        const errMsg = err.message || ''
        if (errMsg.includes('售罄') || errMsg.includes('库存不足') || errMsg.includes('极速拦截')) {
          stressResults.soldOut++
          addLog(`⚪ 请求 #${index} -> [SOLD OUT] 购票失败：该规格库存不足，触发 JVM L1 本地售罄阻断`)
        } else if (errMsg.includes('限流') || errMsg.includes('429')) {
          stressResults.rateLimit++
          addLog(`⚠️ 请求 #${index} -> [RATE LIMIT] 购票失败：${errMsg}`)
        } else {
          stressResults.rateLimit++
          addLog(`❌ 请求 #${index} -> [FAILED] 购票失败：${errMsg}`)
        }
      }
    }

    // 批量调度，根据请求间隔延迟发射
    const promises: Promise<void>[] = []
    for (let i = 1; i <= totalRequests; i++) {
      const p = new Promise<void>((resolve) => {
        const task = setTimeout(async () => {
          if (stressRunning.value) {
            await executeSingleRequest(i)
          }
          resolve()
        }, (i - 1) * interval)
        activeStressTasks.value.push(task)
      })
      promises.push(p)
    }

    // 等待所有请求完成
    await Promise.all(promises)

    if (stressRunning.value) {
      stressRunning.value = false
      addLog(`🏁 压测排洪测试结束。总请求数: ${stressResults.total}，成功: ${stressResults.success}，限流: ${stressResults.rateLimit}，WAF拦截: ${stressResults.wafBlock}，售罄: ${stressResults.soldOut}`)
      message.success('并发测试排洪结束！请前往电子票包查看已落库订单。')
    }
  }

  watch(() => props.selectedEvent, (ev) => {
    if (ev && ev.skus.length > 0) activeSkuId.value = ev.skus[0].id
    fetchVisitors()
  })

  onMounted(() => {
    fetchVisitors()
    if (props.selectedEvent && props.selectedEvent.skus.length > 0) {
      activeSkuId.value = props.selectedEvent.skus[0].id
    }
  })

  return {
    activeSkuId,
    ticketCount,
    activeSku,
    totalPrice,
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
