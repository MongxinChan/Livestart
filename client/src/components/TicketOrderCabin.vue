<template>
  <div class="order-cabin">

    <!-- ========== 抢票操作舱 ========== -->
    <a-card v-if="selectedEvent" class="glass-panel" :bordered="false" style="margin-bottom: 28px">
      <a-button type="link" @click="$emit('backToSquare')" style="padding: 0; margin-bottom: 20px; font-weight: 600">
        <template #icon><ArrowLeftOutlined /></template>
        返回演出广场
      </a-button>

      <a-row :gutter="[36, 24]">
        <!-- 左侧：演出海报 -->
        <a-col :xs="24" :md="8">
          <div style="border-radius: 14px; overflow: hidden; border: 1px solid rgba(var(--ls-accent-rgb), 0.15); box-shadow: var(--ls-neon-glow); margin-bottom: 16px">
            <img :src="selectedEvent.cover" :alt="selectedEvent.title" style="width: 100%; height: 280px; object-fit: cover" />
          </div>
          <h3 style="font-size: 1.1rem; font-weight: 700; line-height: 1.4; margin-bottom: 10px">{{ selectedEvent.title }}</h3>
          <div style="font-size: 13px; color: var(--ls-text-secondary); display: flex; flex-direction: column; gap: 5px">
            <span><CalendarOutlined /> {{ selectedEvent.date }}</span>
            <span><EnvironmentOutlined /> {{ selectedEvent.venue }}</span>
          </div>
        </a-col>

        <!-- 右侧：抢票操作 -->
        <a-col :xs="24" :md="16">
          <!-- 票档选择 -->
          <h4 style="font-size: 0.95rem; font-weight: 600; margin-bottom: 12px">
            <TagOutlined style="margin-right: 6px" /> 选择票档规格
          </h4>
          <a-radio-group v-model:value="activeSkuId" style="width: 100%; margin-bottom: 20px">
            <a-space direction="vertical" style="width: 100%">
              <a-radio-button
                v-for="sku in selectedEvent.skus"
                :key="sku.id"
                :value="sku.id"
                :disabled="sku.stock <= 0"
                style="width: 100%; height: auto; padding: 12px 16px; border-radius: 10px; display: flex; justify-content: space-between; align-items: center"
              >
                <div style="display: flex; justify-content: space-between; width: 100%; align-items: center">
                  <span style="font-weight: 600">{{ sku.name }}</span>
                  <a-space>
                    <a-tag v-if="sku.stock <= 0" color="error">JVM L1 已售罄</a-tag>
                    <a-tag v-else color="success">余 {{ sku.stock }} 张</a-tag>
                    <span style="font-weight: 800; font-size: 1.05rem">¥ {{ sku.price }}</span>
                  </a-space>
                </div>
              </a-radio-button>
            </a-space>
          </a-radio-group>

          <!-- 实名观演人 -->
          <h4 style="font-size: 0.95rem; font-weight: 600; margin-bottom: 10px">
            <TeamOutlined style="margin-right: 6px" /> 勾选实名观演人
            <span style="font-size: 11px; color: var(--ls-text-secondary); font-weight: normal; margin-left: 6px">(观演人数须与购票张数一致)</span>
          </h4>
          <a-row :gutter="[12, 12]" style="margin-bottom: 20px">
            <a-col :xs="24" :sm="8" v-for="v in visitorList" :key="v.id">
              <a-card
                size="small"
                hoverable
                :class="{ 'glow-card': true }"
                :style="{ borderColor: v.checked ? 'var(--ant-color-primary)' : undefined }"
                @click="toggleVisitor(v)"
              >
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <span style="font-weight: 600; font-size: 13px">{{ v.name }}</span>
                  <a-checkbox :checked="v.checked" />
                </div>
                <div style="font-size: 11px; color: var(--ls-text-secondary); font-family: monospace; margin-top: 4px">
                  {{ v.idCard }}
                </div>
              </a-card>
            </a-col>
          </a-row>

          <!-- 购买数量 -->
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px">
            <span style="font-weight: 600">购买数量：</span>
            <a-space>
              <a-input-number v-model:value="ticketCount" :min="1" :max="6" size="large" @change="onCountChange" />
              <span style="font-size: 12px; color: var(--ls-text-secondary)">(每人限购 6 张)</span>
            </a-space>
          </div>

          <!-- 抢票控制面板 -->
          <a-card :bordered="false" style="background: rgba(0,0,0,0.08); border: 1px dashed rgba(var(--ls-accent-rgb), 0.2); border-radius: 14px">
            <!-- 待抢票 -->
            <div v-if="grabStatus === 'idle'">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px">
                <span style="font-size: 13px; color: var(--ls-text-secondary)">合计：</span>
                <span style="font-size: 1.5rem; font-weight: 800">¥ {{ totalPrice }}</span>
              </div>
              <a-button type="primary" block size="large" :disabled="!activeSku || activeSku.stock <= 0" @click="triggerGrab">
                <template #icon><ThunderboltOutlined /></template>
                {{ activeSku && activeSku.stock <= 0 ? '该规格已被 JVM L1 本地极速拦截' : '立即开始高并发极速抢票' }}
              </a-button>
            </div>

            <!-- 获取 Token -->
            <div v-else-if="grabStatus === 'fetching_token'" style="display: flex; gap: 20px; align-items: center; padding: 8px 0">
              <div class="fingerprint-scanner" style="color: var(--ant-color-primary)">
                <ScanOutlined style="font-size: 28px" />
              </div>
              <div>
                <h5 style="font-weight: 700; margin-bottom: 2px">🛡️ WAF/防刷盾指纹安全扫描中</h5>
                <p style="font-size: 12px; color: var(--ls-text-secondary); margin: 0">正在校对设备防刷指纹与签名，动态获取单次抢票 Path Token...</p>
              </div>
            </div>

            <!-- 排队落库 -->
            <div v-else-if="grabStatus === 'grabbing'" style="text-align: center; padding: 8px 0">
              <a-progress :percent="grabProgress" :stroke-color="{ from: 'var(--ant-color-primary)', to: 'var(--ant-color-success)' }" :show-info="false" style="margin-bottom: 12px" />
              <div style="display: flex; justify-content: space-between; margin-bottom: 6px">
                <a-tag color="success"><SafetyCertificateOutlined /> Token 校验通过</a-tag>
                <span style="font-size: 11px; color: var(--ls-text-secondary); font-family: monospace">{{ pathToken.substring(0, 18) }}...</span>
              </div>
              <h5 style="font-weight: 700; margin-bottom: 3px">
                <LoadingOutlined spin /> RocketMQ 削峰异步落库中 ({{ grabProgress }}%)
              </h5>
              <p style="font-size: 12px; color: var(--ls-text-secondary); margin: 0">本地事务隔离去重，正在将高并发下单请求写入持久化物理库中...</p>
            </div>

            <!-- 成功 -->
            <a-result v-else-if="grabStatus === 'success'" status="success" title="下单排队已落库！抢票成功" :sub-title="'订单流水单号：' + createdOrderNo">
              <template #extra>
                <a-space>
                  <a-button @click="resetGrab">再抢一单</a-button>
                  <a-button type="primary" @click="resetGrab">查看订单</a-button>
                </a-space>
              </template>
            </a-result>

            <!-- 失败 -->
            <a-result v-else-if="grabStatus === 'failed'" status="error" title="抢票拦截触发！" :sub-title="grabError">
              <template #extra>
                <a-button type="primary" @click="resetGrab">重新选择规格</a-button>
              </template>
            </a-result>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <!-- ========== 订单中心 ========== -->
    <a-card class="glass-panel" :bordered="false">
      <template #title>
        <span style="font-size: 1.15rem; font-weight: 800">
          <FileTextOutlined style="margin-right: 8px" /> 我的购票电子门票夹
        </span>
      </template>

      <a-empty v-if="orders.length === 0" description="您目前还没有购票订单哦，快去演出广场挑选抢购吧！" />

      <div v-else style="display: flex; flex-direction: column; gap: 20px">
        <a-card
          v-for="order in orders"
          :key="order.orderNo"
          size="small"
          :bordered="true"
          style="border-radius: 14px"
        >
          <!-- 订单头部 -->
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid rgba(var(--ls-accent-rgb), 0.08)">
            <span style="font-size: 12px; color: var(--ls-text-secondary)">
              流水单号: <span style="font-family: monospace; font-weight: 500">{{ order.orderNo }}</span>
            </span>
            <a-tag :color="orderStatusColor(order.status)">{{ order.statusDesc }}</a-tag>
          </div>

          <!-- 拟物门票 -->
          <div class="ticket-card">
            <div class="ticket-left">
              <a-tag color="processing" style="margin-bottom: 8px; font-size: 11px">ELECTRONIC TICKET / 电子入场券</a-tag>
              <h4 style="font-size: 0.9rem; font-weight: 700; margin-bottom: 6px; line-height: 1.4">{{ order.title }}</h4>
              <div style="font-size: 12px; color: var(--ls-text-secondary); display: flex; flex-direction: column; gap: 3px">
                <span>票档：<b>{{ order.skuName }} (¥{{ order.price }} × {{ order.count }}张)</b></span>
                <span>下单：{{ order.createTime }}</span>
              </div>
              <div style="margin-top: 12px; font-size: 11px; color: var(--ls-text-secondary); opacity: 0.85">
                温馨提示：入场时请配合工作人员核销。实名制一票一证，防伪防复制。
              </div>
            </div>
            <div class="ticket-right">
              <div v-if="order.status === 2 && order.isChecked" class="stamp-used">已入场</div>
              <template v-else-if="order.status === 2 && order.checkCode">
                <QrcodeOutlined style="font-size: 36px; color: var(--ant-color-success); margin-bottom: 6px" />
                <span style="font-size: 11px; font-family: monospace; font-weight: 600; color: var(--ant-color-success)">{{ order.checkCode.substring(0, 11) }}</span>
              </template>
              <template v-else-if="order.status === 1">
                <LoadingOutlined style="font-size: 28px; color: var(--ant-color-warning); margin-bottom: 6px" spin />
                <span style="font-size: 11px; color: var(--ant-color-warning); font-weight: 600">等待支付</span>
              </template>
              <template v-else>
                <StopOutlined style="font-size: 28px; color: var(--ls-text-secondary); margin-bottom: 6px" />
                <span style="font-size: 11px; color: var(--ls-text-secondary)">客票已作废</span>
              </template>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; padding-top: 10px; border-top: 1px solid rgba(var(--ls-accent-rgb), 0.08)">
            <template v-if="order.status === 1">
              <a-button size="small" @click="cancelOrder(order.orderNo)">取消订单</a-button>
              <a-button size="small" type="primary" @click="openCheckoutModal(order)">
                <template #icon><WalletOutlined /></template>
                前往收银台
              </a-button>
            </template>
            <template v-if="order.status === 2 && !order.isChecked">
              <a-button size="small" danger @click="refundOrder(order.orderNo)">退票申请</a-button>
              <a-button size="small" type="primary" @click="performCheckCode(order)">
                <template #icon><CheckCircleOutlined /></template>
                模拟入场核销
              </a-button>
            </template>
          </div>
        </a-card>
      </div>
    </a-card>

    <!-- ========== 收银台弹窗 ========== -->
    <a-modal v-model:open="showCheckout" :footer="null" :width="440" centered destroy-on-close>
      <template #title>
        <span><SafetyCertificateOutlined style="color: var(--ant-color-success); margin-right: 8px" />Livestart 统一收银结算中心</span>
      </template>

      <template v-if="payingOrder">
        <a-card size="small" :bordered="false" style="margin-bottom: 20px; border-radius: 12px; background: rgba(0,0,0,0.06)">
          <h5 style="font-weight: 700; margin-bottom: 4px">{{ payingOrder.title }}</h5>
          <p style="font-size: 12px; color: var(--ls-text-secondary); margin-bottom: 8px">{{ payingOrder.skuName }} × {{ payingOrder.count }}张</p>
          <div style="font-size: 1.6rem; font-weight: 900; text-align: center; font-family: 'Outfit'">¥ {{ payingOrder.totalAmount }}</div>
        </a-card>

        <a-radio-group v-model:value="payMethod" style="width: 100%; margin-bottom: 20px">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-radio-button value="wx" style="width: 100%; height: 60px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 10px">
                <WechatOutlined style="font-size: 22px; color: #09bb07" />
                <span style="font-size: 12px">微信支付</span>
              </a-radio-button>
            </a-col>
            <a-col :span="12">
              <a-radio-button value="alipay" style="width: 100%; height: 60px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 10px">
                <AlipayCircleOutlined style="font-size: 22px; color: #108ee9" />
                <span style="font-size: 12px">支付宝</span>
              </a-radio-button>
            </a-col>
          </a-row>
        </a-radio-group>

        <div style="text-align: center; margin-bottom: 20px">
          <div style="width: 140px; height: 140px; background: #fff; padding: 10px; border-radius: 12px; display: inline-flex; align-items: center; justify-content: center">
            <QrcodeOutlined :style="{ fontSize: '100px', color: payMethod === 'wx' ? '#09bb07' : '#108ee9' }" />
          </div>
          <p style="font-size: 12px; color: var(--ls-text-secondary); margin-top: 8px">
            <LoadingOutlined spin /> 等待扫码支付...安全对账连接已建立
          </p>
        </div>

        <a-button type="primary" block size="large" @click="confirmMockPay">
          确认模拟已扫码付款 (触发出票回调)
        </a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined, TagOutlined, TeamOutlined, ThunderboltOutlined,
  ScanOutlined, SafetyCertificateOutlined, LoadingOutlined,
  FileTextOutlined, QrcodeOutlined, StopOutlined, WalletOutlined,
  CheckCircleOutlined, CalendarOutlined, EnvironmentOutlined,
  WechatOutlined, AlipayCircleOutlined,
} from '@ant-design/icons-vue'
import { request, apiState } from '@/composables/useRequest'
import type { LiveEvent, EventSku, Order, Visitor, GrabStatus } from '@/types'

const props = defineProps<{
  selectedEvent: LiveEvent | null
}>()

const emit = defineEmits<{
  backToSquare: []
}>()

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
  visitorList.value.forEach((v, i) => { v.checked = i < n })
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
          fetchOrders()
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

// --- 订单 ---
const orders = ref<Order[]>([])

async function fetchOrders() {
  try {
    const data = await request<{ records: Order[] }>('/api/engine/order/page')
    orders.value = data.records
  } catch (err) {
    console.error('拉取订单失败', err)
  }
}

function orderStatusColor(status: number) {
  const map: Record<number, string> = { 1: 'warning', 2: 'success', 3: 'default', 4: 'error' }
  return map[status] || 'default'
}

// --- 收银台 ---
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
    // 支付音效
    const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.connect(gain)
    gain.connect(audioCtx.destination)
    osc.type = 'sine'
    osc.frequency.setValueAtTime(1046.50, audioCtx.currentTime)
    gain.gain.setValueAtTime(0.08, audioCtx.currentTime)
    osc.start()
    setTimeout(() => osc.stop(), 180)
  } catch (_) { /* 忽略音频错误 */ }

  try {
    await request('/api/engine/order/pay-callback', {
      method: 'POST',
      body: JSON.stringify({ orderNo: payingOrder.value.orderNo, tradeNo: 'TRADE-' + Date.now() }),
    })
    showCheckout.value = false
    message.success('支付成功！电子票已出票')
    fetchOrders()
  } catch (err: any) {
    message.error('支付对账失败: ' + err.message)
  }
}

async function cancelOrder(orderNo: string) {
  try {
    await request('/api/engine/order/cancel', { method: 'POST', body: JSON.stringify({ orderNo }) })
    message.success('取消成功！Redis/DB 物理库存已双向回流归还！')
    fetchOrders()
  } catch (err: any) {
    message.error('取消失败: ' + err.message)
  }
}

async function refundOrder(orderNo: string) {
  Modal.confirm({
    title: '确定要申请退票吗？',
    content: '资金与门票库存都将退回。',
    async onOk() {
      try {
        await request('/api/engine/order/refund', { method: 'POST', body: JSON.stringify({ orderNo }) })
        message.success('退票成功！库存已在 JVM Map 与 Redis 中双向归还。')
        fetchOrders()
      } catch (err: any) {
        message.error('退票失败: ' + err.message)
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
  } catch (_) { /* 忽略 */ }

  order.isChecked = 1
  message.success('核销成功！盖章已入场 [USED]，该电子票已安全失效。')
}

// --- 生命周期 ---
watch(() => props.selectedEvent, (ev) => {
  if (ev && ev.skus.length > 0) activeSkuId.value = ev.skus[0].id
  fetchVisitors()
})

onMounted(() => {
  fetchOrders()
  fetchVisitors()
  if (props.selectedEvent && props.selectedEvent.skus.length > 0) {
    activeSkuId.value = props.selectedEvent.skus[0].id
  }
})
</script>
