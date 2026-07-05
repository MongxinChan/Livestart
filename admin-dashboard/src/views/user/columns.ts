const USER_TYPE_LABEL_MAP: Record<number, string> = {
  1: '普通用户',
  2: '艺人',
  3: '场地管理员',
  4: '超级管理员',
}

export function formatUserType(userType?: number) {
  if (!userType) return '未设置'
  return USER_TYPE_LABEL_MAP[userType] || `未知类型(${userType})`
}

export function formatVerifiedStatus(isVerified?: number) {
  return isVerified === 1 ? '已认证' : '未认证'
}

export const userTypeOptions = [
  { label: '全部类型', value: undefined },
  { label: '普通用户', value: 1 },
  { label: '艺人', value: 2 },
  { label: '场地管理员', value: 3 },
  { label: '超级管理员', value: 4 },
]

export const userTableColumns = [
  {
    title: '用户ID',
    dataIndex: 'id',
    key: 'id',
    width: 180,
    sorter: true,
  },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 180 },
  { title: '真实姓名', dataIndex: 'realName', key: 'realName', width: 140 },
  { title: '手机号', key: 'phone', width: 140 },
  { title: '身份认证', key: 'isVerified', width: 120 },
  { title: '用户类型', key: 'userType', width: 140 },
  { title: '账号状态', key: 'status', width: 120 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
]

export const userVisitorColumns = [
  { title: '观演人姓名', dataIndex: 'realName', key: 'realName', width: 160 },
  { title: '证件类型', dataIndex: 'cardTypeDesc', key: 'cardTypeDesc', width: 140 },
  { title: '证件号', key: 'cardNo', width: 220 },
  { title: '手机号', key: 'mobile', width: 140 },
]
