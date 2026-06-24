export const eventTableColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '海报', key: 'posterUrl', width: 80 },
  { title: '演出标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '类型', key: 'eventType', width: 110 },
  { title: '出演艺人', key: 'performerName', width: 120 },
  { title: '风格', key: 'genre', width: 150 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 170 },
  { title: '开票阶段', key: 'ticketStage', width: 90 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]
