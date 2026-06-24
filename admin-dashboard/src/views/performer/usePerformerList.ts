import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { performerApi } from '@/api/performer'
import { styleApi } from '@/api/style'
import type { PerformerItem } from '@/types'
import { performerTableColumns } from './columns'

export function usePerformerList() {
  const loading = ref(false)
  const list = ref<PerformerItem[]>([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

  async function fetchList() {
    loading.value = true
    try {
      const res = await performerApi.page({ current: pagination.current, size: pagination.pageSize })
      list.value = res?.records || []
      pagination.total = res?.total || 0
    } finally {
      loading.value = false
    }
  }

  function onTableChange(pag: any) {
    pagination.current = pag.current
    void fetchList()
  }

  const formVisible = ref(false)
  const submitting = ref(false)
  const editingId = ref<number | null>(null)
  const formData = reactive({ name: '', avatarUrl: '', styleIds: [] as number[], description: '' })
  const styleOptions = ref<any[]>([])

  async function fetchStyleOptions() {
    try {
      const res = await styleApi.page({ current: 1, size: 100 })
      styleOptions.value = res?.records || []
    } catch (err) {
      console.error('加载风格选项失败', err)
    }
  }

  function resetForm() {
    editingId.value = null
    formData.name = ''
    formData.avatarUrl = ''
    formData.styleIds = []
    formData.description = ''
  }

  function openForm(record?: PerformerItem) {
    if (record) {
      editingId.value = record.id
      formData.name = record.name || ''
      formData.avatarUrl = record.avatarUrl || ''
      formData.styleIds = (record as any).styleIds || []
      formData.description = record.description || ''
    } else {
      resetForm()
    }
    formVisible.value = true
  }

  async function onSubmit() {
    if (!formData.name) {
      message.warning('请填写艺名')
      return
    }
    submitting.value = true
    try {
      if (editingId.value) {
        await performerApi.update({ id: editingId.value, ...formData } as any)
        message.success('更新成功')
      } else {
        await performerApi.create(formData as any)
        message.success('创建成功')
      }
      formVisible.value = false
      void fetchList()
    } finally {
      submitting.value = false
    }
  }

  async function onDelete(id: number) {
    await performerApi.delete(id)
    message.success('已删除')
    void fetchList()
  }

  onMounted(() => {
    void fetchList()
    void fetchStyleOptions()
  })

  return {
    columns: performerTableColumns,
    loading,
    list,
    pagination,
    onTableChange,
    formVisible,
    submitting,
    editingId,
    formData,
    styleOptions,
    openForm,
    onSubmit,
    onDelete,
  }
}
