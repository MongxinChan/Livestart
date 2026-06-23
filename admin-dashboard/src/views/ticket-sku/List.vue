<template>
  <div>
    <a-page-header title="票档管理" sub-title="管理各演出的票种与库存" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-button type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>
          新增票档
        </a-button>
      </template>
    </a-page-header>

    <a-card :bordered="false">
      <div style="margin-bottom: 16px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
        <span style="white-space: nowrap">按演出筛选:</span>
        <a-radio-group v-model:value="filterMode" button-style="solid">
          <a-radio-button value="id">按 ID</a-radio-button>
          <a-radio-button value="name">按名称</a-radio-button>
        </a-radio-group>
        <a-input-number
          v-if="filterMode === 'id'"
          v-model:value="filterEventIdInput"
          :min="1"
          placeholder="输入演出 ID"
          style="width: 200px"
        />
        <a-select
          v-else
          v-model:value="filterEventIdInput"
          show-search
          allow-clear
          placeholder="选择演出"
          style="width: 320px"
          :options="eventOptions"
          :filter-option="filterEventOption"
          :not-found-content="eventOptions.length === 0 ? '暂无演出数据' : '无匹配项'"
        />
        <a-button type="primary" @click="onFilter">查询</a-button>
        <a-button @click="onClearFilter">重置</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'event'">
            <div style="display: flex; flex-direction: column; line-height: 1.4">
              <span style="font-weight: 600">{{ eventTitleMap[record.eventId] || '未知演出' }}</span>
              <span style="font-size: 11px; color: #8c8c8c">ID: {{ record.eventId }}</span>
            </div>
          </template>

          <template v-if="column.key === 'stock'">
            <a-progress
              :percent="record.totalStock ? Math.round((record.remainingStock / record.totalStock) * 100) : 0"
              :status="record.remainingStock === 0 ? 'exception' : 'active'"
              :format="() => `${record.remainingStock} / ${record.totalStock}`"
              size="small"
            />
            <div style="font-size: 11px; color: #8c8c8c; margin-top: 4px">
              一开: {{ record.stage1Stock ?? 0 }} / 二开: {{ record.stage2Stock ?? 0 }}
            </div>
            <div style="margin-top: 6px">
              <a-tag :color="record.stage2Released ? 'orange' : 'default'">
                {{ record.stage2Released ? 'Released' : 'Pending' }}
              </a-tag>
            </div>
          </template>

          <template v-if="column.key === 'price'">
            <span style="color: #ff4d4f; font-weight: 600">￥{{ record.sellingPrice }}</span>
            <span
              v-if="record.originalPrice !== record.sellingPrice"
              style="text-decoration: line-through; color: #999; margin-left: 8px; font-size: 12px"
            >
              ￥{{ record.originalPrice }}
            </span>
          </template>

          <template v-if="column.key === 'action'">
            <a class="table-action-link" @click="openForm(record)">编辑</a>
            <a-divider type="vertical" />
            <a class="table-action-link" @click="openIncreaseStock(record)">增发库存</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除该票档？" @confirm="onDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formVisible"
      :title="editingId ? '编辑票档' : '新增票档'"
      :confirm-loading="submitting"
      @ok="onSubmit"
      width="500px"
    >
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="演出" required>
          <a-select
            v-model:value="formData.eventId"
            show-search
            placeholder="选择演出"
            :options="eventOptions"
            :filter-option="filterEventOption"
          />
        </a-form-item>
        <a-form-item label="票种名称" required>
          <a-input v-model:value="formData.title" placeholder="如：VIP票 / 学生票 / 预售票" />
        </a-form-item>
        <a-form-item label="原价">
          <a-input-number v-model:value="formData.originalPrice" :min="0" :precision="2" prefix="￥" style="width: 100%" />
        </a-form-item>
        <a-form-item label="售价" required>
          <a-input-number v-model:value="formData.sellingPrice" :min="0" :precision="2" prefix="￥" style="width: 100%" />
        </a-form-item>
        <a-form-item v-if="!editingId" label="总库存" required>
          <a-input-number v-model:value="formData.totalStock" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item v-else label="总库存">
          <a-input-number :value="formData.totalStock" disabled style="width: 100%" />
        </a-form-item>
        <a-form-item label="一开数量">
          <a-input-number v-model:value="formData.stage1Stock" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="二开数量">
          <a-input-number v-model:value="formData.stage2Stock" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="限购数量">
          <a-input-number v-model:value="formData.limitNum" :min="1" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="increaseStockVisible"
      title="增发库存"
      :confirm-loading="increaseStockSubmitting"
      @ok="onSubmitIncreaseStock"
      width="420px"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="票档">
          <a-input :value="increaseStockTarget ? `${increaseStockTarget.title} (ID: ${increaseStockTarget.id})` : ''" disabled />
        </a-form-item>
        <a-form-item label="当前库存">
          <a-input :value="increaseStockTarget ? `${increaseStockTarget.remainingStock} / ${increaseStockTarget.totalStock}` : ''" disabled />
        </a-form-item>
        <a-form-item label="增发数量" required>
          <a-input-number v-model:value="increaseStockCount" :min="1" :precision="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { ticketSkuApi } from '@/api/ticketSku'
import { eventApi } from '@/api/event'
import type { TicketSkuItem, EventItem } from '@/types'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '关联演出', key: 'event', width: 280 },
  { title: '票种名称', dataIndex: 'title', key: 'title' },
  { title: '价格', key: 'price', width: 170 },
  { title: '库存', key: 'stock', width: 220 },
  { title: '限购', dataIndex: 'limitNum', key: 'limitNum', width: 70 },
  { title: '操作', key: 'action', width: 140 },
]

const loading = ref(false)
const list = ref<TicketSkuItem[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`,
})

const eventOptions = ref<{ label: string; value: number }[]>([])
const eventTitleMap = reactive<Record<number, string>>({})

function filterEventOption(input: string, option: { label: string; value: number }) {
  return (option.label || '').toLowerCase().includes(input.toLowerCase())
    || String(option.value).includes(input)
}

async function fetchEventOptions() {
  try {
    const res = await eventApi.page({ current: 1, size: 500 })
    const records = res?.records || []
    eventOptions.value = records.map((e: EventItem) => ({ label: `${e.title} (ID: ${e.id})`, value: e.id }))
    records.forEach((e: EventItem) => { eventTitleMap[e.id] = e.title })
    if (records.length === 0) {
      message.warning('暂无演出数据')
    }
  } catch (err) {
    console.error('fetch events failed', err)
    message.error('演出列表加载失败')
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await ticketSkuApi.page({
      eventId: filterEventId.value || undefined,
      current: pagination.current,
      size: pagination.pageSize,
    })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive({
  eventId: 0,
  title: '',
  originalPrice: 0,
  sellingPrice: 0,
  totalStock: 0,
  stage1Stock: 0,
  stage2Stock: 0,
  limitNum: 4,
})
const increaseStockVisible = ref(false)
const increaseStockSubmitting = ref(false)
const increaseStockTarget = ref<TicketSkuItem | null>(null)
const increaseStockCount = ref<number>(1)

const filterMode = ref<'id' | 'name'>('id')
const filterEventIdInput = ref<number | null>(null)
const filterEventId = ref<number | null>(null)

function onFilter() {
  filterEventId.value = filterEventIdInput.value
  pagination.current = 1
  fetchList()
}

function onClearFilter() {
  filterEventIdInput.value = null
  filterEventId.value = null
  pagination.current = 1
  fetchList()
}

function openForm(record?: TicketSkuItem) {
  if (record) {
    editingId.value = record.id
    Object.assign(formData, record)
  } else {
    editingId.value = null
    formData.eventId = 0
    formData.title = ''
    formData.originalPrice = 0
    formData.sellingPrice = 0
    formData.totalStock = 0
    formData.stage1Stock = 0
    formData.stage2Stock = 0
    formData.limitNum = 4
  }
  formVisible.value = true
}

watch(
  () => formData.totalStock,
  (newTotalStock, oldTotalStock) => {
    if (editingId.value || newTotalStock == null || Number.isNaN(newTotalStock)) {
      return
    }
    const previousTotalStock = oldTotalStock ?? 0
    if (formData.stage1Stock === previousTotalStock && (formData.stage2Stock ?? 0) === 0) {
      formData.stage1Stock = newTotalStock
    }
    if ((formData.stage1Stock ?? 0) + (formData.stage2Stock ?? 0) > newTotalStock) {
      formData.stage2Stock = Math.max(newTotalStock - (formData.stage1Stock ?? 0), 0)
    }
  }
)

watch(
  () => formData.stage1Stock,
  (newStage1Stock) => {
    if (editingId.value || newStage1Stock == null) {
      return
    }
    if ((newStage1Stock ?? 0) + (formData.stage2Stock ?? 0) > (formData.totalStock ?? 0)) {
      formData.stage2Stock = Math.max((formData.totalStock ?? 0) - (newStage1Stock ?? 0), 0)
    }
  }
)

function openIncreaseStock(record: TicketSkuItem) {
  increaseStockTarget.value = record
  increaseStockCount.value = 1
  increaseStockVisible.value = true
}

async function onSubmit() {
  if (!formData.title || !formData.sellingPrice) {
    message.warning('请填写必填项')
    return
  }
  if (!formData.eventId) {
    message.warning('请选择演出')
    return
  }
  if (!editingId.value && !formData.totalStock) {
    message.warning('请填写总库存')
    return
  }
  if (!editingId.value && formData.stage1Stock + formData.stage2Stock > formData.totalStock) {
    message.warning('一开和二开数量之和不能超过总库存')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await ticketSkuApi.update({ id: editingId.value, ...formData } as any)
      message.success('更新成功')
    } else {
      await ticketSkuApi.create(formData)
      message.success('创建成功')
    }
    formVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

async function onSubmitIncreaseStock() {
  if (!increaseStockTarget.value) return
  if (!increaseStockCount.value || increaseStockCount.value <= 0) {
    message.warning('请输入正确的增发数量')
    return
  }
  increaseStockSubmitting.value = true
  try {
    await ticketSkuApi.increaseStock({
      skuId: increaseStockTarget.value.id,
      count: increaseStockCount.value,
    })
    message.success('库存增发成功')
    increaseStockVisible.value = false
    await fetchList()
  } finally {
    increaseStockSubmitting.value = false
  }
}

async function onDelete(id: number) {
  await ticketSkuApi.delete(id)
  message.success('删除成功')
  fetchList()
}

onMounted(() => {
  fetchEventOptions()
  fetchList()
})
</script>
