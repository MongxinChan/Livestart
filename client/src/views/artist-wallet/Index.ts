import { h, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { request } from '@/composables/infra/useRequest'

interface Wallet {
  availableAmount: number
  frozenAmount: number
  totalEarned: number
  totalWithdrawn: number
}

interface Commission {
  id: number
  orderNo: string
  ticketAmount: number
  taxAmount: number
  actualAmount: number
  status: number
  createTime?: string
}

interface Withdrawal {
  id: number
  amount: number
  status: number
  accountType: string
  accountNo: string
  createTime?: string
}

interface PageResult<T> {
  records: T[]
  total: number
  current?: number
  size?: number
}

export function useArtistWalletPage() {
  const loading = ref(false)
  const walletLoaded = ref(false)
  const withdrawing = ref(false)
  const commissionLoading = ref(false)
  const withdrawalLoading = ref(false)
  const cancellingId = ref<number | null>(null)
  const wallet = reactive<Wallet>({ availableAmount: 0, frozenAmount: 0, totalEarned: 0, totalWithdrawn: 0 })
  const withdrawForm = reactive({ amount: undefined as number | undefined, accountType: 'ALIPAY', accountNo: '', accountName: '' })
  const commissionQuery = reactive({ pageNo: 1, pageSize: 8 })
  const withdrawalQuery = reactive({ pageNo: 1, pageSize: 8 })
  const commissionPage = reactive<PageResult<Commission>>({ records: [], total: 0 })
  const withdrawalPage = reactive<PageResult<Withdrawal>>({ records: [], total: 0 })

  const commissionColumns = [
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', ellipsis: true },
    { title: '购票金额', dataIndex: 'ticketAmount', key: 'ticketAmount', customRender: ({ text }: { text: number }) => `¥${money(text)}` },
    { title: '代扣税费', dataIndex: 'taxAmount', key: 'taxAmount', customRender: ({ text }: { text: number }) => `¥${money(text)}` },
    { title: '实得金额', dataIndex: 'actualAmount', key: 'actualAmount' },
    { title: '状态', dataIndex: 'status', key: 'status' },
    { title: '时间', dataIndex: 'createTime', key: 'createTime' },
  ]

  const withdrawalColumns = [
    { title: '金额', dataIndex: 'amount', key: 'amount' },
    { title: '账户类型', dataIndex: 'accountType', key: 'accountType' },
    { title: '收款账号', dataIndex: 'accountNo', key: 'accountNo', ellipsis: true },
    { title: '状态', dataIndex: 'status', key: 'status' },
    { title: '申请时间', dataIndex: 'createTime', key: 'createTime' },
    { title: '操作', key: 'action', fixed: 'right' },
  ]

  function money(value: unknown) {
    return Number(value || 0).toFixed(2)
  }

  function formatTime(value?: string) {
    if (!value) return '-'
    return value.replace('T', ' ').substring(0, 16)
  }

  function commissionStatusText(status: number) {
    return ['待结算', '已结算', '已取消'][status] || '未知'
  }

  function commissionStatusColor(status: number) {
    return ['orange', 'green', 'default'][status] || 'default'
  }

  function withdrawalStatusText(status: number) {
    return ['待审核', '处理中', '已完成', '已拒绝', '已取消'][status] || '未知'
  }

  function withdrawalStatusColor(status: number) {
    return ['orange', 'blue', 'green', 'red', 'default'][status] || 'default'
  }

  async function loadWallet() {
    const data = await request<Wallet>('/api/live-start/distribution/v1/artist/wallet')
    wallet.availableAmount = Number(data?.availableAmount || 0)
    wallet.frozenAmount = Number(data?.frozenAmount || 0)
    wallet.totalEarned = Number(data?.totalEarned || 0)
    wallet.totalWithdrawn = Number(data?.totalWithdrawn || 0)
    walletLoaded.value = true
  }

  async function loadCommissions() {
    commissionLoading.value = true
    try {
      const data = await request<PageResult<Commission>>(`/api/live-start/distribution/v1/artist/commission/page?pageNo=${commissionQuery.pageNo}&pageSize=${commissionQuery.pageSize}`)
      commissionPage.records = data?.records || []
      commissionPage.total = Number(data?.total || 0)
    } catch (err: any) {
      message.error(err.message || '佣金明细加载失败')
    } finally {
      commissionLoading.value = false
    }
  }

  async function loadWithdrawals() {
    withdrawalLoading.value = true
    try {
      const data = await request<PageResult<Withdrawal>>(`/api/live-start/distribution/v1/artist/withdrawals?pageNo=${withdrawalQuery.pageNo}&pageSize=${withdrawalQuery.pageSize}`)
      withdrawalPage.records = data?.records || []
      withdrawalPage.total = Number(data?.total || 0)
    } catch (err: any) {
      message.error(err.message || '提现记录加载失败')
    } finally {
      withdrawalLoading.value = false
    }
  }

  async function refresh() {
    loading.value = true
    try {
      await Promise.all([loadWallet(), loadCommissions(), loadWithdrawals()])
    } catch (err: any) {
      message.error(err.message || '钱包加载失败')
    } finally {
      loading.value = false
    }
  }

  async function submitWithdrawal() {
    if (!withdrawForm.amount || withdrawForm.amount <= 0 || withdrawForm.amount > wallet.availableAmount) {
      message.warning('提现金额必须大于 0 且不超过可提现余额')
      return
    }
    if (!withdrawForm.accountNo.trim() || !withdrawForm.accountName.trim()) {
      message.warning('请填写收款账号和收款人姓名')
      return
    }
    withdrawing.value = true
    try {
      await request('/api/live-start/distribution/v1/artist/withdrawals', {
        method: 'POST',
        body: JSON.stringify({
          requestNo: `WEB-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
          amount: withdrawForm.amount,
          accountType: withdrawForm.accountType,
          accountNo: withdrawForm.accountNo.trim(),
          accountName: withdrawForm.accountName.trim(),
        }),
      })
      message.success('提现申请已提交')
      withdrawForm.amount = undefined
      withdrawForm.accountNo = ''
      withdrawForm.accountName = ''
      await Promise.all([loadWallet(), loadWithdrawals()])
    } catch (err: any) {
      message.error(err.message || '提现申请失败')
    } finally {
      withdrawing.value = false
    }
  }

  function cancelWithdrawal(record: Withdrawal) {
    Modal.confirm({
      title: '取消提现申请',
      content: '取消后冻结金额会释放回可提现余额。',
      okText: '确认取消',
      cancelText: '暂不取消',
      onOk: async () => {
        cancellingId.value = record.id
        try {
          await request(`/api/live-start/distribution/v1/artist/withdrawals/${record.id}/cancel`, { method: 'POST' })
          message.success('提现申请已取消')
          await Promise.all([loadWallet(), loadWithdrawals()])
        } catch (err: any) {
          message.error(err.message || '取消提现失败')
        } finally {
          cancellingId.value = null
        }
      },
    })
  }

  onMounted(() => {
    void refresh()
  })

  return {
    h,
    ReloadOutlined,
    loading,
    walletLoaded,
    withdrawing,
    commissionLoading,
    withdrawalLoading,
    cancellingId,
    wallet,
    withdrawForm,
    commissionQuery,
    withdrawalQuery,
    commissionPage,
    withdrawalPage,
    commissionColumns,
    withdrawalColumns,
    money,
    formatTime,
    commissionStatusText,
    commissionStatusColor,
    withdrawalStatusText,
    withdrawalStatusColor,
    refresh,
    loadCommissions,
    loadWithdrawals,
    submitWithdrawal,
    cancelWithdrawal,
  }
}
