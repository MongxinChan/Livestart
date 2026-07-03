import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { request } from '@/composables/infra/useRequest'

type VisitorRecord = {
  id: number | string
  realName: string
  mobile?: string
  cardNo: string
  cardType?: number
  cardTypeDesc?: string
}

const ID_CARD_WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
const ID_CARD_CHECK_CODES = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
const VALID_PROVINCE_CODES = new Set([
  '11', '12', '13', '14', '15',
  '21', '22', '23',
  '31', '32', '33', '34', '35', '36', '37',
  '41', '42', '43', '44', '45', '46',
  '50', '51', '52', '53', '54',
  '61', '62', '63', '64', '65',
  '71', '81', '82',
])

function resetForm(target: { realName: string; mobile: string; cardNo: string; cardType: number }) {
  target.realName = ''
  target.mobile = ''
  target.cardNo = ''
  target.cardType = 1
}

function normalizeRealName(value: string) {
  return value.replace(/\s+/g, ' ').trimStart()
}

function normalizeMobile(value: string) {
  return value.replace(/\D/g, '').slice(0, 11)
}

function normalizeCardNo(value: string) {
  return value.replace(/\s+/g, '').toUpperCase().slice(0, 18)
}

function isValidIdCard(cardNo: string) {
  if (!/^\d{17}[\dX]$/.test(cardNo)) {
    return false
  }

  if (!VALID_PROVINCE_CODES.has(cardNo.slice(0, 2))) {
    return false
  }

  const birthDateText = cardNo.slice(6, 14)
  const year = Number(birthDateText.slice(0, 4))
  const month = Number(birthDateText.slice(4, 6))
  const day = Number(birthDateText.slice(6, 8))
  const birthDate = new Date(year, month - 1, day)

  if (
    Number.isNaN(birthDate.getTime()) ||
    birthDate.getFullYear() !== year ||
    birthDate.getMonth() !== month - 1 ||
    birthDate.getDate() !== day ||
    birthDate.getTime() > Date.now()
  ) {
    return false
  }

  const sum = ID_CARD_WEIGHTS.reduce((total, weight, index) => {
    return total + Number(cardNo[index]) * weight
  }, 0)

  return ID_CARD_CHECK_CODES[sum % 11] === cardNo[17]
}

function resolveErrorMessage(err: unknown, fallback: string) {
  if (err instanceof Error && err.message.trim()) {
    return err.message.trim()
  }
  return fallback
}

export function useVisitorManager(
  props: { open: boolean },
  emit: {
    (e: 'update:open', val: boolean): void
    (e: 'change'): void
  }
) {
  const loading = ref(false)
  const submitting = ref(false)
  const visitors = ref<VisitorRecord[]>([])

  const isEditMode = ref(false)
  const editingId = ref<number | string | null>(null)

  const form = reactive({
    realName: '',
    mobile: '',
    cardNo: '',
    cardType: 1,
  })

  async function fetchList() {
    loading.value = true
    try {
      const list = await request<VisitorRecord[]>('/api/live-start/admin/v1/visitor/list')
      visitors.value = list || []
    } catch (err) {
      message.error(resolveErrorMessage(err, '加载观演人列表失败'))
    } finally {
      loading.value = false
    }
  }

  async function deleteVisitor(id: number | string) {
    try {
      await request(`/api/live-start/admin/v1/visitor/${id}`, { method: 'DELETE' })
      message.success('观演人已删除')
      await fetchList()
      emit('change')
    } catch (err) {
      message.error(resolveErrorMessage(err, '删除观演人失败'))
    }
  }

  function startEdit(item: VisitorRecord) {
    isEditMode.value = true
    editingId.value = item.id
    form.realName = item.realName || ''
    form.mobile = normalizeMobile(item.mobile || '')
    form.cardNo = normalizeCardNo(item.cardNo || '')
    form.cardType = item.cardType || 1
  }

  function cancelEdit() {
    isEditMode.value = false
    editingId.value = null
    resetForm(form)
  }

  function handleRealNameInput(value: string) {
    form.realName = normalizeRealName(value)
  }

  function handleMobileInput(value: string) {
    form.mobile = normalizeMobile(value)
  }

  function handleCardNoInput(value: string) {
    if (isEditMode.value) {
      return
    }
    form.cardNo = normalizeCardNo(value)
  }

  async function handleSubmit() {
    const realName = form.realName.trim()
    const mobile = normalizeMobile(form.mobile)
    const cardNo = normalizeCardNo(form.cardNo)

    if (!realName) {
      message.warning('请填写真实姓名')
      return
    }

    if (mobile && mobile.length !== 11) {
      message.warning('手机号需要填写 11 位数字')
      return
    }

    if (!isEditMode.value) {
      if (!cardNo) {
        message.warning('请填写证件号')
        return
      }

      if (!isValidIdCard(cardNo)) {
        message.warning('请输入合法的 18 位身份证号，不能只填写长度正确的号码')
        return
      }
    }

    submitting.value = true
    try {
      if (isEditMode.value) {
        await request('/api/live-start/admin/v1/visitor', {
          method: 'PUT',
          body: JSON.stringify({
            id: editingId.value,
            realName,
            mobile,
          }),
        })
        message.success('观演人修改成功')
      } else {
        await request('/api/live-start/admin/v1/visitor', {
          method: 'POST',
          body: JSON.stringify({
            realName,
            cardNo,
            mobile,
            cardType: form.cardType,
          }),
        })
        message.success('观演人添加成功')
      }

      cancelEdit()
      await fetchList()
      emit('change')
    } catch (err) {
      message.error(resolveErrorMessage(err, '观演人保存失败'))
    } finally {
      submitting.value = false
    }
  }

  function handleCancel() {
    cancelEdit()
    emit('update:open', false)
  }

  watch(
    () => props.open,
    (open) => {
      if (open) {
        void fetchList()
      }
    }
  )

  return {
    loading,
    submitting,
    visitors,
    isEditMode,
    editingId,
    form,
    fetchList,
    deleteVisitor,
    startEdit,
    cancelEdit,
    handleSubmit,
    handleCancel,
    handleRealNameInput,
    handleMobileInput,
    handleCardNoInput,
  }
}
