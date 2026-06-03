<template>
  <div>
    <a-page-header title="场馆管理" sub-title="管理演出场馆信息" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-button type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>
          新增场馆
        </a-button>
      </template>
    </a-page-header>

    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" :pagination="pagination" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a class="table-action-link" @click="openForm(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除该场馆？" @confirm="onDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formVisible" :title="editingId ? '编辑场馆' : '新增场馆'" :confirm-loading="submitting" @ok="onSubmit" width="500px">
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="场馆名称" required><a-input v-model:value="formData.name" placeholder="如：上海 Modern Sky LAB" /></a-form-item>
        <a-form-item label="城市" required><a-input v-model:value="formData.city" placeholder="如：上海" /></a-form-item>
        <a-form-item label="详细地址"><a-input v-model:value="formData.address" placeholder="详细地址" /></a-form-item>
        <a-form-item label="容纳人数"><a-input-number v-model:value="formData.capacity" :min="1" style="width: 100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { venueApi } from '@/api/venue'
import type { VenueItem } from '@/types'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '场馆名称', dataIndex: 'name', key: 'name' },
  { title: '城市', dataIndex: 'city', key: 'city', width: 100 },
  { title: '地址', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '容量', dataIndex: 'capacity', key: 'capacity', width: 100 },
  { title: '操作', key: 'action', width: 140 },
]

const loading = ref(false)
const list = ref<VenueItem[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function fetchList() {
  loading.value = true
  try {
    const res = await venueApi.page({ current: pagination.current, size: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } finally { loading.value = false }
}

function onTableChange(pag: any) { pagination.current = pag.current; fetchList() }

const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive({ name: '', city: '', address: '', capacity: 0 })

function openForm(record?: VenueItem) {
  if (record) { editingId.value = record.id; Object.assign(formData, record) }
  else { editingId.value = null; formData.name = ''; formData.city = ''; formData.address = ''; formData.capacity = 0 }
  formVisible.value = true
}

async function onSubmit() {
  if (!formData.name) { message.warning('请填写场馆名称'); return }
  submitting.value = true
  try {
    if (editingId.value) { await venueApi.update({ id: editingId.value, ...formData }); message.success('更新成功') }
    else { await venueApi.create(formData); message.success('创建成功') }
    formVisible.value = false; fetchList()
  } finally { submitting.value = false }
}

async function onDelete(id: number) { await venueApi.delete(id); message.success('已删除'); fetchList() }

onMounted(fetchList)
</script>
