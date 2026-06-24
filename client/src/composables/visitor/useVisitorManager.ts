import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { request } from '@/composables/infra/useRequest'

export function useVisitorManager(props: { open: boolean }, emit: any) {
  const loading = ref(false)
  const submitting = ref(false)
  const visitors = ref<any[]>([])

  const isEditMode = ref(false)
  const editingId = ref<number | null>(null)

  const form = reactive({
    realName: '',
    mobile: '',
    cardNo: '',
    cardType: 1,
  })

  async function fetchList() {
    loading.value = true
    try {
      const list = await request<any[]>('/api/live-start/admin/v1/visitor/list')
      visitors.value = list || []
    } catch (err: any) {
      message.error(`加载观演人失败: ${err.message}`)
    } finally {
      loading.value = false
    }
  }

  async function deleteVisitor(id: number) {
    try {
      await request(`/api/live-start/admin/v1/visitor/${id}`, { method: 'DELETE' })
      message.success('已成功删除观演人')
      void fetchList()
      emit('change')
    } catch (err: any) {
      message.error(`删除失败: ${err.message}`)
    }
  }

  function startEdit(item: any) {
    isEditMode.value = true
    editingId.value = item.id
    form.realName = item.realName
    form.mobile = item.mobile || ''
    form.cardNo = item.cardNo
  }

  function cancelEdit() {
    isEditMode.value = false
    editingId.value = null
    form.realName = ''
    form.mobile = ''
    form.cardNo = ''
  }

  async function handleSubmit() {
    if (!form.realName.trim()) {
      message.warning('请填写真实姓名')
      return
    }

    if (!isEditMode.value && (!form.cardNo.trim() || form.cardNo.length !== 18)) {
      message.warning('请输入 18 位二代身份证号码')
      return
    }

    submitting.value = true
    try {
      if (isEditMode.value) {
        await request('/api/live-start/admin/v1/visitor', {
          method: 'PUT',
          body: JSON.stringify({
            id: editingId.value,
            realName: form.realName,
            mobile: form.mobile,
          }),
        })
        message.success('观演人修改成功')
      } else {
        await request('/api/live-start/admin/v1/visitor', {
          method: 'POST',
          body: JSON.stringify({
            realName: form.realName,
            cardNo: form.cardNo,
            mobile: form.mobile,
            cardType: 1,
          }),
        })
        message.success('观演人添加成功')
      }
      cancelEdit()
      void fetchList()
      emit('change')
    } catch (err: any) {
      message.error(`操作失败: ${err.message}`)
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
    (newVal) => {
      if (newVal) {
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
  }
}
