import type { HotSearch, LiveEvent, Order, SettlementResult } from '@/types'

export let mockVisitors = [
  { id: 201, userId: 10086, realName: '陈孟欣(开发者)', cardType: 1, cardTypeDesc: '身份证', cardNo: '3301**********1234', mobile: '188****8888' },
  { id: 202, userId: 10086, realName: '张学友(模拟)', cardType: 1, cardTypeDesc: '身份证', cardNo: '4402**********9988', mobile: '199****9999' },
  { id: 203, userId: 10086, realName: '李四(模拟)', cardType: 1, cardTypeDesc: '身份证', cardNo: '1103**********5678', mobile: '177****7777' },
]

export const mockEvents: LiveEvent[] = [
  {
    id: 101,
    title: '「万能青年旅店」2026 巡回音乐会 - 上海站',
    type: 'Livehouse',
    cover: 'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&q=80&w=600',
    date: '2026-06-25 19:30',
    venue: '上海 Modern Sky LAB',
    city: '上海市 上海市',
    artist: '万能青年旅店',
    minPrice: 280,
    tags: ['独立摇滚', 'Livehouse', '热卖中'],
    skus: [
      { id: 1011, name: '学生票(预售)', price: 280, stock: 0, total: 100 },
      { id: 1012, name: '普通票(全价)', price: 380, stock: 15, total: 300 },
      { id: 1013, name: 'VIP 票(含优先入场)', price: 580, stock: 2, total: 80 },
    ],
  },
  {
    id: 102,
    title: '「周杰伦」嘉年华世界巡回演唱会 - 杭州站',
    type: '演唱会',
    cover: 'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&q=80&w=600',
    date: '2026-07-12 19:00',
    venue: '杭州奥体中心体育场(大莲花)',
    city: '浙江省 杭州市',
    artist: '周杰伦',
    minPrice: 580,
    tags: ['流行巨星', '体育场', '准点抢票'],
    skus: [
      { id: 1021, name: '看台 580', price: 580, stock: 45, total: 2000 },
      { id: 1022, name: '看台 980', price: 980, stock: 12, total: 3000 },
      { id: 1023, name: '内场 1680', price: 1680, stock: 0, total: 1500 },
      { id: 1024, name: '内场 2000(极速抢票)', price: 2000, stock: 8, total: 1000 },
    ],
  },
  {
    id: 103,
    title: '「重塑雕像的权利」A RE-TREAD OVERTURE 特别专场',
    type: 'Livehouse',
    cover: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&q=80&w=600',
    date: '2026-08-08 20:00',
    venue: '深圳 HOU Live',
    city: '广东省 深圳市',
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
    city: '广东省 广州市',
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

export let mockHotSearches: HotSearch[] = [
  { keyword: '万能青年旅店', score: 9823 },
  { keyword: '周杰伦 杭州', score: 8540 },
  { keyword: '秀动 Livehouse', score: 7120 },
  { keyword: '陈奕迅 广州', score: 6245 },
  { keyword: '重塑 深圳站', score: 5410 },
]

export const mockOrders: Order[] = [
  {
    orderNo: '171725890012345678',
    title: '「万能青年旅店」2026 巡回音乐会',
    skuId: 1012,
    skuName: '普通票(全价)',
    price: 380,
    count: 2,
    totalAmount: 760,
    status: 0,
    statusDesc: '待支付(剩余14分32秒)',
    createTime: '2026-06-02 10:15:00',
    checkCode: '',
    isChecked: 0,
  },
  {
    orderNo: '171725800088888888',
    title: '「重塑雕像的权利」A RE-TREAD OVERTURE 特别专场',
    skuId: 1031,
    skuName: '全价票',
    price: 320,
    count: 1,
    totalAmount: 320,
    status: 1,
    statusDesc: '出票成功(待核销)',
    createTime: '2026-06-02 09:20:00',
    checkCode: 'RE-TREAD-8888-9999',
    isChecked: 0,
  },
]

export function createSettlementResult(eventId: string): SettlementResult {
  const targetEvent = mockEvents.find((event) => event.id === Number(eventId)) || mockEvents[0]
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

  return {
    eventId,
    eventName: targetEvent.title,
    totalSold,
    totalRevenue,
    commission,
    netAmount: totalRevenue - commission,
    status: 1,
    settleTime: new Date().toLocaleString(),
    shards,
  }
}
