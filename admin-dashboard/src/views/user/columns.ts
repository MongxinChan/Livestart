export const userTableColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '真实姓名', dataIndex: 'realName', key: 'realName' },
  { title: '手机号', key: 'phone', width: 140 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
]

export const userVisitorColumns = [
  { title: '姓名', dataIndex: 'realName', key: 'realName' },
  { title: '证件号', key: 'idCard' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
]
