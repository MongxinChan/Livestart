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
      <div style="margin-bottom: 16px; display: flex; align-items: center; gap: 12px">
        <span style="white-space: nowrap">按演出筛选：</span>
        <a-input-number v-model:value="filterEventId" :min="1" placeholder="演出 ID" style="width: 160px" allow-clear />
        <a-button type="primary" @click="onFilter">查询</a-button>
        <a-button @click="onClearFilter">重置</a-button>
      </div>
      <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" :pagination="pagination" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'stock'">
            <a-progress
              :percent="record.totalStock ? Math.round((record.remainingStock / record.totalStock) * 100) : 0"
              :status="record.remainingStock === 0 ? 'exception' : 'active'"
              :format="() => `${record.remainingStock} / ${record.totalStock}`"
              size="small"
            />
          </template>
          <template v-if="column.key === 'price'">
            <span style="color: #ff4d4f; font-weight: 600">¥{{ record.sellingPrice }}</span>
            <span v-if="record.originalPrice !== record.sellingPrice" style="text-decoration: line-through; color: #999; margin-left: 8px; font-size: 12px">¥{{ record.originalPrice }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a class="table-action-link" @click="openForm(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除该票档？" @confirm="onDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formVisible" :title="editingId ? '编辑票档' : '新增票档'" :confirm-loading="submitting" @ok="onSubmit" width="500px">
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="演出 ID" required><a-input-number v-model:value="formData.eventId" :min="1" style="width: 100%" /></a-form-item>
        <a-form-item label="票种名称" required><a-input v-model:value="formData.title" placeholder="如：VIP票" /></a-form-item>
        <a-form-item label="原价"><a-input-number v-model:value="formData.originalPrice" :min="0" :precision="2" prefix="¥" style="width: 100%" /></a-form-item>
        <a-form-item label="售价" required><a-input-number v-model:value="formData.sellingPrice" :min="0" :precision="2" prefix="¥" style="width: 100%" /></a-form-item>
        <a-form-item label="总库存" required><a-input-number v-model:value="formData.totalStock" :min="1" style="width: 100%" /></a-form-item>
        <a-form-item label="限购数量"><a-input-number v-model:value="formData.limitNum" :min="1" style="width: 100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { ticketSkuApi } from '@/api/ticketSku'
import type { TicketSkuItem } from '@/types'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '演出 ID', dataIndex: 'eventId', key: 'eventId', width: 90 },
  { title: '票种名称', dataIndex: 'title', key: 'title' },
  { title: '价格', key: 'price', width: 170 },
  { title: '库存', key: 'stock', width: 200 },
  { title: '限购', dataIndex: 'limitNum', key: 'limitNum', width: 70 },
  { title: '操作', key: 'action', width: 140 },
]

const loading = ref(false)
const list = ref<TicketSkuItem[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function fetchList() {
  loading.value = true
  try {
    const res = await ticketSkuApi.page({ eventId: filterEventId.value || undefined, current: pagination.current, size: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } finally { loading.value = false }
}

function onTableChange(pag: any) { pagination.current = pag.current; fetchList() }

const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive({ eventId: 0, title: '', originalPrice: 0, sellingPrice: 0, totalStock: 0, limitNum: 4 })

const filterEventId = ref<number | null>(null)
function onFilter() { pagination.current = 1; fetchList() }
function onClearFilter() { filterEventId.value = null; pagination.current = 1; fetchList() }

function openForm(record?: TicketSkuItem) {
  if (record) { editingId.value = record.id; Object.assign(formData, record) }
  else { editingId.value = null; formData.eventId = 0; formData.title = ''; formData.originalPrice = 0; formData.sellingPrice = 0; formData.totalStock = 0; formData.limitNum = 4 }
  formVisible.value = true
}

async function onSubmit() {
  if (!formData.title || !formData.sellingPrice) { message.warning('请填写必填项'); return }
  submitting.value = true
  try {
    if (editingId.value) { await ticketSkuApi.update({ id: editingId.value, ...formData } as any); message.success('更新成功') }
    else { await ticketSkuApi.create(formData); message.success('创建成功') }
    formVisible.value = false; fetchList()
  } finally { submitting.value = false }
}

async function onDelete(id: number) { await ticketSkuApi.delete(id); message.success('已删除'); fetchList() }

onMounted(fetchList)
</script>
