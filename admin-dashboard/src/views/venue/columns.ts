import type { VenueItem } from '@/types'

export const venueTableColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '场馆名称', dataIndex: 'name', key: 'name' },
  {
    title: '城市',
    dataIndex: 'city',
    key: 'city',
    width: 150,
    sorter: (a: VenueItem, b: VenueItem) => (a.city || '').localeCompare(b.city || ''),
  },
  { title: '地址', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '容量', dataIndex: 'capacity', key: 'capacity', width: 100 },
  { title: '操作', key: 'action', width: 140 },
]
