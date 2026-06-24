export interface EventItem {
  id: number
  title: string
  eventType: number
  venueId: number
  startTime: string
  posterUrl: string
  status: number
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

export interface TicketSkuItem {
  id: number
  eventId: number
  title: string
  originalPrice: number
  sellingPrice: number
  totalStock: number
  stage1Stock?: number
  stage2Stock?: number
  stage2Released?: number
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
  stage1Stock?: number
  stage2Stock?: number
  limitNum: number
}

export interface PerformerItem {
  id: number
  name: string
  avatarUrl: string
  genre?: string
  description: string
  styleIds?: number[]
  status?: number
}

export interface PerformerSaveReq {
  name: string
  avatarUrl: string
  description: string
  genre?: string
  styleIds?: number[]
  status?: number
}

export interface UserItem {
  id: number
  username: string
  realName: string
  phone: string
  mail: string
  createTime: string
}

export interface VisitorItem {
  id: number
  userId: number
  realName: string
  idType: number
  idCard: string
  phone: string
}

export interface OrderItem {
  id: number
  orderNo: string
  userId: number
  username: string
  eventId: number
  eventTitle: string
  skuId: number
  skuName: string
  ticketCount: number
  totalAmount: number
  status: number
  statusDesc?: string
  createTime: string
}

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

export interface StyleItem {
  id: number
  name: string
  code: string
  description: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface ApiResult<T = any> {
  code: string
  message: string
  data: T
  requestId: string
}
