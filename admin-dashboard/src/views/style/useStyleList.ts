import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { styleApi } from '@/api/style'
import type { StyleItem } from '@/types'
import { styleTableColumns } from './columns'

export function useStyleList() {
  const loading = ref(false)
  const list = ref<StyleItem[]>([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

  async function fetchList() {
    loading.value = true
    try {
      const res = await styleApi.page({ current: pagination.current, size: pagination.pageSize })
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
  const formData = reactive({ name: '', code: '', description: '' })

  function resetForm() {
    editingId.value = null
    formData.name = ''
    formData.code = ''
    formData.description = ''
  }

  function openForm(record?: StyleItem) {
    if (record) {
      editingId.value = record.id
      Object.assign(formData, record)
    } else {
      resetForm()
    }
    formVisible.value = true
  }

  async function onSubmit() {
    if (!formData.name || !formData.code) {
      message.warning('请填写名称和代码')
      return
    }
    submitting.value = true
    try {
      if (editingId.value) {
        await styleApi.update({ id: editingId.value, ...formData })
        message.success('更新成功')
      } else {
        await styleApi.create(formData)
        message.success('创建成功')
      }
      formVisible.value = false
      void fetchList()
    } finally {
      submitting.value = false
    }
  }

  async function onDelete(id: number) {
    await styleApi.delete(id)
    message.success('已删除')
    void fetchList()
  }

  onMounted(() => {
    void fetchList()
  })

  return {
    columns: styleTableColumns,
    loading,
    list,
    pagination,
    onTableChange,
    formVisible,
    submitting,
    editingId,
    formData,
    openForm,
    onSubmit,
    onDelete,
  }
}
