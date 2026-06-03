import { reactive } from 'vue'
import type { LiveEvent, HotSearch, Order, SettlementResult } from '@/types'

// ========== 全局 API 状态 ==========
export const apiState = reactive({
  isMock: true,
  gatewayUrl: 'http://localhost:8888',
  userId: '10086',
  token: 'mock-user-token-9988',
  currentUser: { username: '陈孟欣 (模拟开发)', realName: '陈孟欣' } as any,
})

// ========== Mock 数据 ==========
export const MOCK_EVENTS: LiveEvent[] = [
  {
    id: 101,
    title: '「万能青年旅店」2026"河北墨麒麟"巡回音乐会 - 上海站',
    type: 'Livehouse',
    cover: 'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=600',
    date: '2026-06-25 19:30',
    venue: '上海 Modern Sky LAB',
    artist: '万能青年旅店',
    minPrice: 280,
    tags: ['独立摇滚', 'Livehouse', '热卖中'],
    skus: [
      { id: 1011, name: '学生票 (预售)', price: 280, stock: 0, total: 100 },
      { id: 1012, name: '普通票 (全价)', price: 380, stock: 15, total: 300 },
      { id: 1013, name: 'VIP 票 (含优先入场)', price: 580, stock: 2, total: 80 },
    ],
  },
  {
    id: 102,
    title: '「周杰伦」嘉年华世界巡回演唱会 - 杭州站',
    type: '演唱会',
    cover: 'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=600',
    date: '2026-07-12 19:00',
    venue: '杭州奥体中心体育场 (大莲花)',
    artist: '周杰伦',
    minPrice: 580,
    tags: ['流行巨星', '体育场', '准点抢票'],
    skus: [
      { id: 1021, name: '看台 580', price: 580, stock: 45, total: 2000 },
      { id: 1022, name: '看台 980', price: 980, stock: 12, total: 3000 },
      { id: 1023, name: '内场 1680', price: 1680, stock: 0, total: 1500 },
      { id: 1024, name: '内场 2000 (极速抢票)', price: 2000, stock: 8, total: 1000 },
    ],
  },
  {
    id: 103,
    title: '「重塑雕像的权利」"A RE-TREAD OVERTURE" 特别专场',
    type: 'Livehouse',
    cover: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=600',
    date: '2026-08-08 20:00',
    venue: '深圳 HOU Live',
    artist: '重塑雕像的权利',
    minPrice: 320,
    tags: ['后朋克', '极致美学', '特惠中'],
    skus: [
      { id: 1031, name: '全价票', price: 320, stock: 28, total: 500 },
      { id: 1032, name: '现场票', price: 380, stock: 50, total: 100 },
    ],
  },
  {
    id: 104,
    title: '「陈奕迅」FEAR and DREAMS 世界巡回演唱会 - 广州站',
    type: '演唱会',
    cover: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&q=80&w=600',
    date: '2026-08-20 19:30',
    venue: '广州大学城体育中心体育场',
    artist: '陈奕迅',
    minPrice: 680,
    tags: ['华语金曲', '万人现场', '即将开售'],
    skus: [
      { id: 1041, name: '看台 680', price: 680, stock: 350, total: 5000 },
      { id: 1042, name: '看台 980', price: 980, stock: 150, total: 4000 },
      { id: 1043, name: '内场 1580', price: 1580, stock: 0, total: 2000 },
      { id: 1044, name: '内场 1980', price: 1980, stock: 30, total: 1500 },
    ],
  },
]

export let MOCK_HOT_SEARCHES: HotSearch[] = [
  { keyword: '万能青年旅店', score: 9823 },
  { keyword: '周杰伦 杭州', score: 8540 },
  { keyword: '秀动 Livehouse', score: 7120 },
  { keyword: '陈奕迅 广州', score: 6245 },
  { keyword: '重塑 深圳站', score: 5410 },
]

export const MOCK_ORDERS: Order[] = [
  {
    orderNo: '171725890012345678',
    title: '「万能青年旅店」2026"河北墨麒麟"巡回音乐会',
    skuId: 1012,
    skuName: '普通票 (全价)',
    price: 380,
    count: 2,
    totalAmount: 760,
    status: 1,
    statusDesc: '待支付 (剩余14分52秒)',
    createTime: '2026-06-02 10:15:00',
    checkCode: '',
    isChecked: 0,
  },
  {
    orderNo: '171725800088888888',
    title: '「重塑雕像的权利」"A RE-TREAD OVERTURE" 特别专场',
    skuId: 1031,
    skuName: '全价票',
    price: 320,
    count: 1,
    totalAmount: 320,
    status: 2,
    statusDesc: '出票成功 (待核销)',
    createTime: '2026-06-02 09:20:00',
    checkCode: 'RE-TREAD-8888-9999',
    isChecked: 0,
  },
]

// ========== Mock 处理器 ==========
function handleMock(url: string, options: RequestInit = {}): Promise<any> {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      // 演出列表
      if (url.includes('/api/search/event') || url.includes('/api/engine/event/list')) {
        resolve(MOCK_EVENTS)
        return
      }
      // 热搜
      if (url.includes('/api/search/hot')) {
        resolve([...MOCK_HOT_SEARCHES].sort((a, b) => b.score - a.score))
        return
      }
      // 热搜点击
      if (url.includes('/api/search/click')) {
        const params = new URLSearchParams(url.split('?')[1])
        const kw = params.get('keyword')
        if (kw) {
          const item = MOCK_HOT_SEARCHES.find(h => h.keyword === kw)
          if (item) item.score += 250
          else MOCK_HOT_SEARCHES.push({ keyword: kw, score: 250 })
        }
        resolve(true)
        return
      }
      // 抢票 Token
      if (url.includes('/api/engine/order/token')) {
        const params = new URLSearchParams(url.split('?')[1])
        const skuId = params.get('skuId')
        const allSkus = MOCK_EVENTS.flatMap(e => e.skus)
        const sku = allSkus.find(s => s.id === Number(skuId))
        if (sku && sku.stock <= 0) {
          reject(new Error('JVM级本地售罄拦截器判定：该票种已售罄，阻断后续所有请求！'))
          return
        }
        resolve('pathtoken_' + Math.random().toString(36).substring(2, 10) + '_' + skuId)
        return
      }
      // 创建订单
      if (url.includes('/api/engine/order/create/')) {
        const pathToken = url.substring(url.lastIndexOf('/') + 1)
        const reqData = JSON.parse((options.body as string) || '{}')

        if (!pathToken || !pathToken.startsWith('pathtoken_')) {
          reject(new Error('安全校验失败：URL Token 无效或防刷重放拦截！'))
          return
        }

        const skuId = reqData.skuId
        const allSkus = MOCK_EVENTS.flatMap(e => e.skus)
        const sku = allSkus.find(s => s.id === Number(skuId))

        if (sku && sku.stock <= 0) {
          reject(new Error('该票档库存不足，已被 JVM L1 本地极速拦截！'))
          return
        }
        if (sku) sku.stock = Math.max(0, sku.stock - reqData.count)

        const targetEvent = MOCK_EVENTS.find(e => e.skus.some(s => s.id === Number(skuId)))
        const orderNo = '17172' + Date.now() + Math.floor(Math.random() * 1000)

        MOCK_ORDERS.unshift({
          orderNo,
          title: targetEvent ? targetEvent.title : '热门演出票',
          skuId,
          skuName: sku ? sku.name : '普通票',
          price: sku ? sku.price : 100,
          count: reqData.count,
          totalAmount: (sku ? sku.price : 100) * reqData.count,
          status: 1,
          statusDesc: '待支付 (剩余15分00秒)',
          createTime: new Date().toLocaleString(),
          checkCode: '',
          isChecked: 0,
        })
        resolve(orderNo)
        return
      }
      // 订单列表
      if (url.includes('/api/engine/order/page')) {
        resolve({ records: MOCK_ORDERS, total: MOCK_ORDERS.length, size: 10, current: 1 })
        return
      }
      // 支付回调
      if (url.includes('/api/engine/order/pay-callback')) {
        const reqData = JSON.parse((options.body as string) || '{}')
        const order = MOCK_ORDERS.find(o => o.orderNo === reqData.orderNo)
        if (order) {
          order.status = 2
          order.statusDesc = '出票成功 (待核销)'
          order.checkCode = 'TICKET-' + Math.floor(1000 + Math.random() * 9000) + '-' + Math.floor(1000 + Math.random() * 9000)
          resolve(true)
        } else {
          reject(new Error('订单不存在'))
        }
        return
      }
      // 取消订单
      if (url.includes('/api/engine/order/cancel')) {
        const reqData = JSON.parse((options.body as string) || '{}')
        const order = MOCK_ORDERS.find(o => o.orderNo === reqData.orderNo)
        if (order) {
          order.status = 3
          order.statusDesc = '已取消 (库存已自动安全归还)'
          const allSkus = MOCK_EVENTS.flatMap(e => e.skus)
          const sku = allSkus.find(s => s.id === order.skuId)
          if (sku) sku.stock += order.count
          resolve(true)
        } else {
          reject(new Error('订单不存在'))
        }
        return
      }
      // 退票
      if (url.includes('/api/engine/order/refund')) {
        const reqData = JSON.parse((options.body as string) || '{}')
        const order = MOCK_ORDERS.find(o => o.orderNo === reqData.orderNo)
        if (order) {
          order.status = 4
          order.statusDesc = '已退票 (资金与库存已秒级回流)'
          const allSkus = MOCK_EVENTS.flatMap(e => e.skus)
          const sku = allSkus.find(s => s.id === order.skuId)
          if (sku) sku.stock += order.count
          resolve(true)
        } else {
          reject(new Error('订单不存在'))
        }
        return
      }
      // 票房结算
      if (url.includes('/api/settlement/trigger')) {
        const params = new URLSearchParams(url.split('?')[1])
        const eventId = params.get('eventId') || '101'
        const targetEvent = MOCK_EVENTS.find(e => e.id === Number(eventId)) || MOCK_EVENTS[0]

        const shards = []
        let totalSold = 0
        let totalRevenue = 0

        for (let i = 0; i < 16; i++) {
          const orderCount = Math.floor(5 + Math.random() * 45)
          const ticketCount = Math.floor(orderCount * (1 + Math.random() * 2))
          const shardRevenue = ticketCount * targetEvent.minPrice
          totalSold += ticketCount
          totalRevenue += shardRevenue
          shards.push({
            tableName: `t_order_item_${i}`,
            orders: orderCount,
            tickets: ticketCount,
            revenue: shardRevenue,
            dbShard: i < 8 ? 'ds_order_0' : 'ds_order_1',
          })
        }

        const commission = Math.round(totalRevenue * 0.05)
        resolve({
          eventId,
          eventName: targetEvent.title,
          totalSold,
          totalRevenue,
          commission,
          netAmount: totalRevenue - commission,
          status: 1,
          settleTime: new Date().toLocaleString(),
          shards,
        } as SettlementResult)
        return
      }

      reject(new Error('未定义该 Mock 接口: ' + url))
    }, 600)
  })
}

// ========== 统一请求函数 ==========
export async function request<T = any>(url: string, options: RequestInit = {}): Promise<T> {
  if (apiState.isMock) {
    return handleMock(url, options)
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'User-Id': apiState.userId,
    Authorization: apiState.token,
    ...(options.headers as Record<string, string> || {}),
  }

  const response = await fetch(url, { ...options, headers })

  if (!response.ok) {
    if (response.status === 429) {
      throw new Error('抢票请求过于拥挤，限流防刷判罚触发！请稍后再试 (HTTP 429)')
    }
    const errData = await response.json().catch(() => ({}))
    throw new Error(errData.message || `网络异常 (HTTP ${response.status})`)
  }

  const resJson = await response.json()
  if (resJson.code !== '0' && resJson.code !== 0 && resJson.code !== '200') {
    throw new Error(resJson.message || '业务请求失败')
  }
  return resJson.data
}
