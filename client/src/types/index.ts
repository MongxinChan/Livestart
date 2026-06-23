export interface EventSku {
  id: number
  name: string
  price: number
  stock: number
  total: number
}

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
  status?: number
  statusText?: string
  started?: boolean
}

export interface HotSearch {
  keyword: string
  score: number
}

export type OrderStatus = 0 | 1 | 2 | 3

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

export interface ShardData {
  tableName: string
  orders: number
  tickets: number
  revenue: number
  dbShard: string
}

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

export interface CarouselSlide {
  title: string
  desc: string
  tag: string
  image: string
  eventId: number
}

export interface Visitor {
  id: number
  name: string
  idCard: string
  checked: boolean
}

export type GrabStatus = 'idle' | 'fetching_token' | 'grabbing' | 'success' | 'failed'

export interface LiveShard {
  tableName: string
  visible: boolean
  height: number
  revenue: number
}

export type ThemeId = 'cyberpunk-dark' | 'minimalist-light' | 'damai-crimson' | 'showstart-neon'

export interface ThemeOption {
  id: ThemeId
  name: string
  icon: string
}

export interface TicketReminder {
  id: number
  eventId: number
  eventTitle: string
  ticketStage: number
  status: number
  statusDesc: string
  saleStartTime: string
  remindTime: string
  reminderMessage: string
}

export type ViewId = 'square' | 'cabin' | 'orders' | 'reminders'
