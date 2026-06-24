export const orderStatusLabels: Record<number, string> = {
  0: '待支付',
  1: '已出票',
  2: '已取消',
  3: '已退票',
}

export const orderStatusColors: Record<number, string> = {
  0: 'orange',
  1: 'green',
  2: 'default',
  3: 'red',
}

export const orderTableColumns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 220 },
  { title: '用户', dataIndex: 'username', key: 'username', width: 120 },
  { title: '演出', dataIndex: 'eventTitle', key: 'eventTitle', ellipsis: true },
  { title: '票种', dataIndex: 'skuName', key: 'skuName', width: 140 },
  { title: '数量', dataIndex: 'ticketCount', key: 'ticketCount', width: 80 },
  { title: '金额', key: 'totalAmount', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '下单时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
]
