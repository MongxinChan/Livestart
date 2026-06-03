// ========== 演出 ==========
export interface EventItem {
  id: number
  title: string
  eventType: number     // 0:Livehouse 1:演唱会
  venueId: number
  startTime: string
  posterUrl: string
  status: number        // 0:下架 1:预售 2:在售 3:售罄
  performerId?: number
  performerName?: string
  ticketStage?: number
}

export interface EventSaveReq {
  title: string
  eventType: number
  venueId: number | null
  startTime: string
  posterUrl: string
  performerId?: number | null
  ticketStage?: number
}


export interface EventUpdateReq extends EventSaveReq {
  id: number
}

// ========== 场馆 ==========
export interface VenueItem {
  id: number
  name: string
  city: string
  address: string
  capacity: number
}

export interface VenueSaveReq {
  name: string
  city: string
  address: string
  capacity: number
}

// ========== 票档 ==========
export interface TicketSkuItem {
  id: number
  eventId: number
  title: string
  originalPrice: number
  sellingPrice: number
  totalStock: number
  remainingStock: number
  limitNum: number
  version: number
}

export interface TicketSkuSaveReq {
  eventId: number
  title: string
  originalPrice: number
  sellingPrice: number
  totalStock: number
  limitNum: number
}

// ========== 艺人 ==========
export interface PerformerItem {
  id: number
  name: string
  avatarUrl: string
  genre: string
  description: string
}

export interface PerformerSaveReq {
  name: string
  avatarUrl: string
  genre: string
  description: string
}

// ========== 用户 ==========
export interface UserItem {
  id: number
  username: string
  realName: string
  phone: string
  mail: string
  createTime: string
}

// ========== 观演人 ==========
export interface VisitorItem {
  id: number
  userId: number
  realName: string
  idType: number
  idCard: string
  phone: string
}

// ========== 订单 ==========
export interface OrderItem {
  id: number
  orderSn: string
  userId: number
  username: string
  eventTitle: string
  skuName: string
  ticketCount: number
  totalAmount: number
  status: number
  createTime: string
}

// ========== 结算 ==========
export interface SettlementItem {
  id: number
  eventId: number
  eventName: string
  totalSold: number
  totalRevenue: number
  commission: number
  netAmount: number
  status: number
  settleTime: string
}

export interface SettlementStats {
  totalRevenue: number
  totalCommission: number
  totalNetAmount: number
  totalOrders: number
}

// ========== 风格/主题 ==========
export interface StyleItem {
  id: number
  name: string
  description: string
}

// ========== 通用分页响应 ==========
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ========== 后端统一响应 ==========
export interface ApiResult<T = any> {
  code: string
  message: string
  data: T
  requestId: string
}
