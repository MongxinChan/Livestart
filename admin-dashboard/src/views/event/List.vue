<template>
  <div>
    <a-page-header title="演出管理" sub-title="创建、编辑、上下架演出项目" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-button type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>
          创建演出
        </a-button>
      </template>
    </a-page-header>

    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'posterUrl'">
            <a-image :src="record.posterUrl" :width="60" :height="40" style="object-fit: cover; border-radius: 4px" fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='40'%3E%3Crect fill='%23f0f0f0' width='60' height='40'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%23999' font-size='10'%3E无图%3C/text%3E%3C/svg%3E" />
          </template>
          <template v-if="column.key === 'eventType'">
            <a-tag :color="record.eventType === 0 ? 'blue' : 'purple'">
              {{ record.eventType === 0 ? 'Livehouse' : '演唱会' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColors[record.status]">{{ statusLabels[record.status] }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a class="table-action-link" @click="openForm(record)">编辑</a>
              <a-divider type="vertical" />
              <a-dropdown>
                <a>更多 <DownOutlined /></a>
                <template #overlay>
                  <a-menu @click="({ key }: any) => onAction(key, record)">
                    <a-menu-item key="publish" :disabled="record.status !== 1">上架开售</a-menu-item>
                    <a-menu-item key="shelve" :disabled="record.status !== 2">下架</a-menu-item>
                    <a-menu-item key="terminate" :disabled="record.status >= 3">
                      <span style="color: #ff4d4f">终止售票</span>
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="delete">
                      <span style="color: #ff4d4f">删除</span>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建/编辑弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="editingId ? '编辑演出' : '创建演出'"
      :confirm-loading="submitting"
      @ok="onSubmit"
      width="600px"
      class="form-modal"
    >
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="演出标题" required>
          <a-input v-model:value="formData.title" placeholder="如：「周杰伦」嘉年华世界巡回演唱会" />
        </a-form-item>
        <a-form-item label="演出类型" required>
          <a-radio-group v-model:value="formData.eventType">
            <a-radio :value="0">Livehouse (站票)</a-radio>
            <a-radio :value="1">演唱会 (选座)</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="关联场馆">
          <a-input-number v-model:value="formData.venueId" :min="1" placeholder="场馆 ID" style="width: 100%" />
        </a-form-item>
        <a-form-item label="演出时间" required>
          <a-date-picker v-model:value="formData.startTime" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </a-form-item>
        <a-form-item label="海报图片">
          <a-input v-model:value="formData.posterUrl" placeholder="海报图片 URL 地址" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DownOutlined } from '@ant-design/icons-vue'
import { eventApi } from '@/api/event'
import type { EventItem } from '@/types'

const statusLabels: Record<number, string> = { 0: '已下架', 1: '预售', 2: '在售', 3: '售罄' }
const statusColors: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' }

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '海报', key: 'posterUrl', width: 80 },
  { title: '演出标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '类型', key: 'eventType', width: 110 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 170 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

const loading = ref(false)
const list = ref<EventItem[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function fetchList() {
  loading.value = true
  try {
    const res = await eventApi.page({ current: pagination.current, size: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } catch {
    // axios 拦截器已处理错误
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

// 表单
const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive({
  title: '',
  eventType: 0,
  venueId: null as number | null,
  startTime: '',
  posterUrl: '',
})

function openForm(record?: EventItem) {
  if (record) {
    editingId.value = record.id
    formData.title = record.title
    formData.eventType = record.eventType
    formData.venueId = record.venueId
    formData.startTime = record.startTime
    formData.posterUrl = record.posterUrl
  } else {
    editingId.value = null
    formData.title = ''
    formData.eventType = 0
    formData.venueId = null
    formData.startTime = ''
    formData.posterUrl = ''
  }
  formVisible.value = true
}

async function onSubmit() {
  if (!formData.title || !formData.startTime) {
    message.warning('请填写必填项')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await eventApi.update({ id: editingId.value, ...formData })
      message.success('更新成功')
    } else {
      await eventApi.create(formData)
      message.success('创建成功')
    }
    formVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

// 操作
async function onAction(key: string, record: EventItem) {
  if (key === 'publish') {
    await eventApi.publish(record.id)
    message.success('已上架开售')
    fetchList()
  } else if (key === 'shelve') {
    await eventApi.shelve(record.id)
    message.success('已下架')
    fetchList()
  } else if (key === 'terminate') {
    Modal.confirm({
      title: '确认终止售票？',
      content: '此操作不可逆，确认后该演出将永久停止售票。',
      okType: 'danger',
      onOk: async () => {
        await eventApi.terminate(record.id)
        message.success('已终止')
        fetchList()
      },
    })
  } else if (key === 'delete') {
    Modal.confirm({
      title: '确认删除？',
      content: `将删除演出「${record.title}」，此操作不可恢复。`,
      okType: 'danger',
      onOk: async () => {
        await eventApi.delete(record.id)
        message.success('已删除')
        fetchList()
      },
    })
  }
}

onMounted(fetchList)
</script>
