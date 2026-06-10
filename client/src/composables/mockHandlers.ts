import { apiState } from './sessionState'
import { createSettlementResult, mockEvents, mockHotSearches, mockOrders, mockVisitors } from './mockData'

export async function handleMockRequest(url: string, options: RequestInit = {}) {
  return new Promise((resolve, reject) => {
    window.setTimeout(() => {
      if (url.includes('/api/search/event') || url.includes('/api/engine/event/list')) {
        resolve(mockEvents)
        return
      }

      if (url.includes('/api/search/hot')) {
        resolve([...mockHotSearches].sort((a, b) => b.score - a.score))
        return
      }

      if (url.includes('/api/search/click')) {
        const params = new URLSearchParams(url.split('?')[1])
        const keyword = params.get('keyword')
        if (keyword) {
          const item = mockHotSearches.find((entry) => entry.keyword === keyword)
          if (item) {
            item.score += 250
          } else {
            mockHotSearches.push({ keyword, score: 250 })
          }
        }
        resolve(true)
        return
      }

      if (url.includes('/api/engine/order/token')) {
        const params = new URLSearchParams(url.split('?')[1])
        const skuId = params.get('skuId')
        const sku = mockEvents.flatMap((event) => event.skus).find((item) => item.id === Number(skuId))

        if (sku && sku.stock <= 0) {
          reject(new Error('该票种已售罄，拦截后续请求'))
          return
        }

        resolve(`pathtoken_${Math.random().toString(36).substring(2, 10)}_${skuId}`)
        return
      }

      if (url.includes('/api/engine/order/create/')) {
        const pathToken = url.substring(url.lastIndexOf('/') + 1)
        const reqData = JSON.parse((options.body as string) || '{}')

        if (!pathToken.startsWith('pathtoken_')) {
          reject(new Error('URL Token 无效，安全校验失败'))
          return
        }

        const sku = mockEvents.flatMap((event) => event.skus).find((item) => item.id === Number(reqData.skuId))
        if (sku && sku.stock <= 0) {
          reject(new Error('库存不足，已被本地拦截'))
          return
        }
        if (sku) {
          sku.stock = Math.max(0, sku.stock - reqData.count)
        }

        const targetEvent = mockEvents.find((event) => event.skus.some((item) => item.id === Number(reqData.skuId)))
        const orderNo = `17172${Date.now()}${Math.floor(Math.random() * 1000)}`

        mockOrders.unshift({
          orderNo,
          title: targetEvent ? targetEvent.title : '热门演出票',
          skuId: reqData.skuId,
          skuName: sku ? sku.name : '普通票',
          price: sku ? sku.price : 100,
          count: reqData.count,
          totalAmount: (sku ? sku.price : 100) * reqData.count,
          status: 0,
          statusDesc: '待支付(剩余15分00秒)',
          createTime: new Date().toLocaleString(),
          checkCode: '',
          isChecked: 0,
        })
        resolve(orderNo)
        return
      }

      if (url.includes('/api/engine/order/page')) {
        resolve({ records: mockOrders, total: mockOrders.length, size: 10, current: 1 })
        return
      }

      if (url.includes('/api/engine/order/pay-callback')) {
        const reqData = JSON.parse((options.body as string) || '{}')
        const order = mockOrders.find((item) => item.orderNo === reqData.orderNo)
        if (!order) {
          reject(new Error('订单不存在'))
          return
        }

        order.status = 1
        order.statusDesc = '出票成功(待核销)'
        order.checkCode = `TICKET-${Math.floor(1000 + Math.random() * 9000)}-${Math.floor(1000 + Math.random() * 9000)}`
        resolve(true)
        return
      }

      if (url.includes('/api/engine/order/cancel')) {
        const reqData = JSON.parse((options.body as string) || '{}')
        const order = mockOrders.find((item) => item.orderNo === reqData.orderNo)
        if (!order) {
          reject(new Error('订单不存在'))
          return
        }

        order.status = 2
        order.statusDesc = '已取消(库存已安全归还)'
        const sku = mockEvents.flatMap((event) => event.skus).find((item) => item.id === order.skuId)
        if (sku) {
          sku.stock += order.count
        }
        resolve(true)
        return
      }

      if (url.includes('/api/engine/order/refund')) {
        const reqData = JSON.parse((options.body as string) || '{}')
        const order = mockOrders.find((item) => item.orderNo === reqData.orderNo)
        if (!order) {
          reject(new Error('订单不存在'))
          return
        }

        order.status = 3
        order.statusDesc = '已退票(资金与库存已回流)'
        const sku = mockEvents.flatMap((event) => event.skus).find((item) => item.id === order.skuId)
        if (sku) {
          sku.stock += order.count
        }
        resolve(true)
        return
      }

      if (url.includes('/api/settlement/trigger')) {
        const params = new URLSearchParams(url.split('?')[1])
        resolve(createSettlementResult(params.get('eventId') || '101'))
        return
      }

      if (url.includes('/api/live-start/admin/v1/visitor/list')) {
        resolve(mockVisitors)
        return
      }

      if (url.includes('/api/live-start/admin/v1/visitor') && options.method === 'POST') {
        const reqData = JSON.parse((options.body as string) || '{}')
        const id = Date.now()
        const rawCard = reqData.cardNo || ''
        const desensitizedCard =
          rawCard.length === 18 ? rawCard.replace(/^(\d{4})\d{10}(\d{4})$/, '$1**********$2') : rawCard

        mockVisitors.push({
          id,
          userId: Number(apiState.userId || 10086),
          realName: reqData.realName,
          cardType: reqData.cardType || 1,
          cardTypeDesc: reqData.cardType === 1 ? '身份证' : '其他证件',
          cardNo: desensitizedCard,
          mobile: reqData.mobile || '',
        })
        resolve(true)
        return
      }

      if (url.includes('/api/live-start/admin/v1/visitor') && options.method === 'PUT') {
        const reqData = JSON.parse((options.body as string) || '{}')
        const visitor = mockVisitors.find((item) => item.id === reqData.id)

        if (!visitor) {
          reject(new Error('未找到该观演人'))
          return
        }

        if (reqData.realName) {
          visitor.realName = reqData.realName
        }
        if (reqData.mobile !== undefined) {
          visitor.mobile = reqData.mobile
        }
        resolve(true)
        return
      }

      if (url.includes('/api/live-start/admin/v1/visitor/') && options.method === 'DELETE') {
        const lastSlash = url.lastIndexOf('/')
        const id = Number(url.substring(lastSlash + 1))
        const next = mockVisitors.filter((item) => item.id !== id)
        mockVisitors.splice(0, mockVisitors.length, ...next)
        resolve(true)
        return
      }

      reject(new Error(`未定义的 Mock 接口: ${url}`))
    }, 600)
  })
}
