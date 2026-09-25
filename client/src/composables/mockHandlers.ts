import { apiState } from './sessionState'
import { createSettlementResult, mockEvents, mockHotSearches, mockOrders, mockVisitors } from './mockData'

const mockArtistWallet = {
  artistId: 20099,
  availableAmount: 1280.5,
  frozenAmount: 200,
  totalEarned: 3680.5,
  totalWithdrawn: 2200,
}
const mockArtistCommissions = [
  { id: 1, orderNo: 'MOCK-20260924001', ticketAmount: 680, taxAmount: 13.6, actualAmount: 54.4, status: 1, createTime: '2026-09-22T10:15:00' },
  { id: 2, orderNo: 'MOCK-20260923002', ticketAmount: 520, taxAmount: 10.4, actualAmount: 41.6, status: 0, createTime: '2026-09-23T14:20:00' },
]
const mockArtistWithdrawals: Array<Record<string, unknown>> = [
  { id: 1, amount: 200, status: 2, accountType: 'ALIPAY', accountNo: 'artist@example.com', accountName: '模拟艺人', createTime: '2026-09-20T09:00:00' },
]

function getMockEventPrices(event: (typeof mockEvents)[number]) {
  return event.skus.map((sku) => sku.price)
}

export async function handleMockRequest(url: string, options: RequestInit = {}) {
  return new Promise((resolve, reject) => {
    window.setTimeout(() => {
      if (url.includes('/api/live-start/admin/v1/user/avatar') && options.method === 'POST') {
        const file = options.body instanceof FormData ? options.body.get('file') : null
        resolve(file instanceof File ? URL.createObjectURL(file) : '')
        return
      }

      if (url.includes('/api/live-start/admin/v1/user') && options.method === 'PUT') {
        const reqData = JSON.parse((options.body as string) || '{}')
        apiState.currentUser = {
          ...(apiState.currentUser || {}),
          ...reqData,
          phone: reqData.phone || apiState.phone,
        }
        resolve(true)
        return
      }

      if (url.includes('/api/live-start/admin/v1/user/me') && (!options.method || options.method === 'GET')) {
        resolve(apiState.currentUser)
        return
      }

      if (url.includes('/api/live-start/distribution/v1/artist/wallet')) {
        resolve({ ...mockArtistWallet })
        return
      }

      if (url.includes('/api/live-start/distribution/v1/artist/commission/page')) {
        const params = new URLSearchParams(url.split('?')[1] || '')
        const pageNo = Math.max(Number(params.get('pageNo') || 1), 1)
        const pageSize = Math.max(Number(params.get('pageSize') || 8), 1)
        const start = (pageNo - 1) * pageSize
        resolve({ records: mockArtistCommissions.slice(start, start + pageSize), total: mockArtistCommissions.length, current: pageNo, size: pageSize })
        return
      }

      if (url.includes('/api/live-start/distribution/v1/artist/withdrawals/') && url.endsWith('/cancel') && options.method === 'POST') {
        const pathParts = url.split('/')
        const id = Number(pathParts[pathParts.length - 2])
        const record = mockArtistWithdrawals.find((item) => Number(item.id) === id)
        if (!record || record.status !== 0) {
          reject(new Error('当前提现申请不可取消'))
          return
        }
        record.status = 4
        mockArtistWallet.availableAmount += Number(record.amount || 0)
        mockArtistWallet.frozenAmount -= Number(record.amount || 0)
        resolve(true)
        return
      }

      if (url.includes('/api/live-start/distribution/v1/artist/withdrawals') && options.method === 'POST') {
        const reqData = JSON.parse((options.body as string) || '{}')
        const amount = Number(reqData.amount || 0)
        if (amount <= 0 || amount > mockArtistWallet.availableAmount) {
          reject(new Error('可提现余额不足'))
          return
        }
        const record = { id: Date.now(), amount, status: 0, accountType: reqData.accountType, accountNo: reqData.accountNo, accountName: reqData.accountName, createTime: new Date().toISOString() }
        mockArtistWithdrawals.unshift(record)
        mockArtistWallet.availableAmount -= amount
        mockArtistWallet.frozenAmount += amount
        resolve(record)
        return
      }

      if (url.includes('/api/live-start/distribution/v1/artist/withdrawals')) {
        const params = new URLSearchParams(url.split('?')[1] || '')
        const pageNo = Math.max(Number(params.get('pageNo') || 1), 1)
        const pageSize = Math.max(Number(params.get('pageSize') || 8), 1)
        const start = (pageNo - 1) * pageSize
        resolve({ records: mockArtistWithdrawals.slice(start, start + pageSize), total: mockArtistWithdrawals.length, current: pageNo, size: pageSize })
        return
      }

      if (url.includes('/api/live-start/engine/event/') && !url.endsWith('/list')) {
        const eventId = Number(url.substring(url.lastIndexOf('/') + 1))
        const event = mockEvents.find((item) => Number(item.id) === eventId) ?? null
        resolve(event)
        return
      }

      if (url.includes('/api/live-start/search/event') || url.includes('/api/live-start/engine/event/list')) {
        if (url.includes('/api/live-start/search/event')) {
          const params = new URLSearchParams(url.split('?')[1] || '')
          const keyword = params.get('keyword')?.trim() || ''
          const eventType = params.get('eventType')
          const city = params.get('city')?.trim() || ''
          const minPrice = params.get('minPrice')
          const maxPrice = params.get('maxPrice')
          const pageNum = Math.max(Number(params.get('pageNum') || 1), 1)
          const pageSize = Math.max(Number(params.get('pageSize') || 10), 1)

          const typeToText: Record<string, string> = { '0': 'Livehouse', '1': '演唱会' }

          const filtered = mockEvents.filter((event) => {
            if (keyword && !event.title.includes(keyword)) return false
            if (eventType !== null && eventType !== '' && typeToText[eventType] !== event.type) return false
            if (city && !(event.city || '').includes(city)) return false

            const prices = getMockEventPrices(event)
            if (!prices.some((price) =>
              (minPrice === null || minPrice === '' || price >= Number(minPrice))
              && (maxPrice === null || maxPrice === '' || price <= Number(maxPrice)))) return false
            return true
          })

          const start = (pageNum - 1) * pageSize
          resolve({
            records: filtered.slice(start, start + pageSize),
            total: filtered.length,
            size: pageSize,
            current: pageNum,
            pages: Math.ceil(filtered.length / pageSize),
          })
          return
        }

        resolve(mockEvents)
        return
      }

      if (url.includes('/api/live-start/search/hot')) {
        resolve([...mockHotSearches].sort((a, b) => b.score - a.score))
        return
      }

      if (url.includes('/api/live-start/search/click')) {
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

      if (url.includes('/api/live-start/engine/order/token')) {
        const params = new URLSearchParams(url.split('?')[1])
        const skuId = params.get('skuId')
        const sku = mockEvents.flatMap((event) => event.skus).find((item) => Number(item.id) === Number(skuId))

        if (sku && sku.stock <= 0) {
          reject(new Error('该票种已售罄，请更换其他票档'))
          return
        }

        resolve(`pathtoken_${Math.random().toString(36).substring(2, 10)}_${skuId}`)
        return
      }

      if (url.includes('/api/live-start/engine/order/create/')) {
        const pathToken = url.substring(url.lastIndexOf('/') + 1)
        const reqData = JSON.parse((options.body as string) || '{}')

        if (!pathToken.startsWith('pathtoken_')) {
          reject(new Error('URL Token 无效，安全校验失败'))
          return
        }

        const sku = mockEvents.flatMap((event) => event.skus).find((item) => Number(item.id) === Number(reqData.skuId))
        if (sku && sku.stock <= 0) {
          reject(new Error('库存不足，已被其他用户抢完'))
          return
        }
        if (sku) {
          sku.stock = Math.max(0, sku.stock - reqData.count)
        }

        const targetEvent = mockEvents.find((event) => event.skus.some((item) => Number(item.id) === Number(reqData.skuId)))
        const orderNo = `17172${Date.now()}${Math.floor(Math.random() * 1000)}`

        mockOrders.unshift({
          orderNo,
          eventTitle: targetEvent ? targetEvent.title : '热门演出票',
          skuId: reqData.skuId,
          skuTitle: sku ? sku.name : '普通票',
          price: sku ? sku.price : 100,
          count: reqData.count,
          totalAmount: (sku ? sku.price : 100) * reqData.count,
          status: 0,
          statusDesc: '待支付(剩余15分10秒)',
          createTime: new Date().toLocaleString(),
          checkCode: '',
          isChecked: 0,
        })
        resolve(orderNo)
        return
      }

      if (url.includes('/api/live-start/engine/order/detail/')) {
        const orderNo = decodeURIComponent(url.substring(url.lastIndexOf('/') + 1))
        const order = mockOrders.find((item) => item.orderNo === orderNo)
        if (!order) {
          reject(new Error('订单不存在'))
          return
        }
        resolve({
          orderNo,
          ticketItems: Array.from({ length: order.count }, (_, index) => ({
            id: `${orderNo}-${index + 1}`,
            visitorId: index + 1,
            checkCode: index === 0 ? order.checkCode : `${order.checkCode}-${index + 1}`,
            isChecked: index === 0 ? order.isChecked : 0,
          })),
        })
        return
      }

      if (url.includes('/api/live-start/engine/order/page')) {
        resolve({ records: mockOrders, total: mockOrders.length, size: 10, current: 1 })
        return
      }

      if (url.includes('/api/live-start/engine/order/pay-callback')) {
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

      if (url.includes('/api/live-start/engine/order/cancel')) {
        const reqData = JSON.parse((options.body as string) || '{}')
        const order = mockOrders.find((item) => item.orderNo === reqData.orderNo)
        if (!order) {
          reject(new Error('订单不存在'))
          return
        }

        order.status = 2
        order.statusDesc = '已取消，库存已安全回流'
        const sku = mockEvents.flatMap((event) => event.skus).find((item) => Number(item.id) === Number(order.skuId))
        if (sku) {
          sku.stock += order.count
        }
        resolve(true)
        return
      }

      if (url.includes('/api/live-start/engine/order/refund')) {
        const reqData = JSON.parse((options.body as string) || '{}')
        const order = mockOrders.find((item) => item.orderNo === reqData.orderNo)
        if (!order) {
          reject(new Error('订单不存在'))
          return
        }

        order.status = 3
        order.statusDesc = '已退票，资金与库存已回流'
        const sku = mockEvents.flatMap((event) => event.skus).find((item) => Number(item.id) === Number(order.skuId))
        if (sku) {
          sku.stock += order.count
        }
        resolve(true)
        return
      }

      if (url.includes('/api/live-start/settlement/trigger')) {
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
