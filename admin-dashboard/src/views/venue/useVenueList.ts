import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { venueApi } from '@/api/venue'
import { useExcelImport } from '@/composables/useExcelImport'
import { chinaCities } from '@/utils/chinaCities'
import type { VenueItem } from '@/types'
import { venueTableColumns } from './columns'

export function useVenueList() {
  const loading = ref(false)
  const list = ref<VenueItem[]>([])
  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    pageSizeOptions: ['10', '20', '50', '100'],
    showTotal: (total: number) => `共 ${total} 条`,
  })

  async function fetchList() {
    loading.value = true
    try {
      const res = await venueApi.page({ current: pagination.current, size: pagination.pageSize })
      list.value = res?.records || []
      pagination.total = res?.total || 0
    } finally {
      loading.value = false
    }
  }

  function onTableChange(pag: any) {
    pagination.current = pag.current
    pagination.pageSize = pag.pageSize
    void fetchList()
  }

  const formVisible = ref(false)
  const submitting = ref(false)
  const editingId = ref<number | null>(null)
  const formData = reactive({ name: '', city: '', address: '', capacity: 0 })
  const selectedCity = ref<string[]>([])

  function resetForm() {
    editingId.value = null
    formData.name = ''
    formData.city = ''
    formData.address = ''
    formData.capacity = 0
    selectedCity.value = []
  }

  function openForm(record?: VenueItem) {
    if (record) {
      editingId.value = record.id
      Object.assign(formData, record)
      selectedCity.value = record.city ? record.city.split('/') : []
    } else {
      resetForm()
    }
    formVisible.value = true
  }

  async function onSubmit() {
    if (!formData.name) {
      message.warning('请填写场馆名称')
      return
    }
    if (selectedCity.value.length === 0) {
      message.warning('请选择城市')
      return
    }

    formData.city = selectedCity.value.join('/')
    submitting.value = true
    try {
      if (editingId.value) {
        await venueApi.update({ id: editingId.value, ...formData })
        message.success('更新成功')
      } else {
        await venueApi.create(formData)
        message.success('创建成功')
      }
      formVisible.value = false
      void fetchList()
    } finally {
      submitting.value = false
    }
  }

  async function onDelete(id: number) {
    await venueApi.delete(id)
    message.success('已删除')
    void fetchList()
  }

  onMounted(() => {
    void fetchList()
  })

  const { beforeUpload } = useExcelImport(venueApi.importExcel, fetchList)

  return {
    columns: venueTableColumns,
    loading,
    list,
    pagination,
    onTableChange,
    formVisible,
    submitting,
    editingId,
    formData,
    selectedCity,
    chinaCities,
    openForm,
    beforeUpload,
    onSubmit,
    onDelete,
  }
}
