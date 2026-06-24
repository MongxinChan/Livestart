export const orderStatusLabels: Record<number, string> = {
  1: '待支付',
  2: '已出票',
  3: '已取消',
  4: '已退票',
}

export const orderStatusColors: Record<number, string> = {
  1: 'orange',
  2: 'green',
  3: 'default',
  4: 'red',
}

export const orderTableColumns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200 },
  { title: '用户', dataIndex: 'username', key: 'username', width: 100 },
  { title: '演出', dataIndex: 'eventTitle', key: 'eventTitle', ellipsis: true },
  { title: '票种', dataIndex: 'skuName', key: 'skuName', width: 120 },
  { title: '数量', dataIndex: 'ticketCount', key: 'ticketCount', width: 70 },
  { title: '金额', key: 'totalAmount', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '下单时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
]
