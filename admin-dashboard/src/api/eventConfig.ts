import http from './http'

export interface EventConfigItem {
  eventId: number
  selectionMode: number         // 0:自动配座 1:手动选座
  isVerifyRequired: number      // 0:否 1:是
  maxTicketsPerUser: number
  refundPolicyType: number      // 0:不可退 1:全额退 2:阶梯退票
  tier1FreeRefundHours: number
  tier2PartialRefundHours: number
  tier2RefundFeeRate: number
  isTransferable: number        // 0:否 1:是
  isWaitingAllowed: number      // 0:否 1:是
}

export const eventConfigApi = {
  getByEventId: (eventId: number) =>
    http.get<any, EventConfigItem>(`/api/merchant-admin/event-config/${eventId}`),

  update: (data: EventConfigItem) =>
    http.put<any, void>('/api/merchant-admin/event-config/update', data),
}
