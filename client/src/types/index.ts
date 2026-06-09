/** 票档 SKU */
export interface EventSku {
  id: number
  name: string
  price: number
  stock: number
  total: number
}

/** 演出信息 */
export interface LiveEvent {
  id: number
  title: string
  type: string
  cover: string
  date: string
  venue: string
  city?: string
  artist: string
  minPrice: number
  tags: string[]
  skus: EventSku[]
  ticketStage?: number
}


/** 热搜词 */
export interface HotSearch {
  keyword: string
  score: number
}

/** 订单状态：0待支付 1已支付 2已取消 3已退票 */
export type OrderStatus = 0 | 1 | 2 | 3

/** 订单 */
export interface Order {
  orderNo: string
  title: string
  skuId: number
  skuName: string
  price: number
  count: number
  totalAmount: number
  status: OrderStatus
  statusDesc: string
  createTime: string
  checkCode: string
  isChecked: number
}

/** 分表数据 */
export interface ShardData {
  tableName: string
  orders: number
  tickets: number
  revenue: number
  dbShard: string
}

/** 结算结果 */
export interface SettlementResult {
  eventId: string
  eventName: string
  totalSold: number
  totalRevenue: number
  commission: number
  netAmount: number
  status: number
  settleTime: string
  shards: ShardData[]
}

/** 轮播数据 */
export interface CarouselSlide {
  title: string
  desc: string
  tag: string
  image: string
  eventId: number
}

/** 实名观演人 */
export interface Visitor {
  id: number
  name: string
  idCard: string
  checked: boolean
}

/** 抢票状态 */
export type GrabStatus = 'idle' | 'fetching_token' | 'grabbing' | 'success' | 'failed'

/** 柱状图可视化分片 */
export interface LiveShard {
  tableName: string
  visible: boolean
  height: number
  revenue: number
}

/** 主题 ID */
export type ThemeId = 'cyberpunk-dark' | 'minimalist-light' | 'damai-crimson' | 'showstart-neon'

/** 主题配置项 */
export interface ThemeOption {
  id: ThemeId
  name: string
  icon: string
}
