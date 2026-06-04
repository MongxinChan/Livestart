<template>
  <div>
    <a-page-header title="艺人管理" sub-title="管理演出艺人/歌手信息" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-button type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>
          新增艺人
        </a-button>
      </template>
    </a-page-header>

    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" :pagination="pagination" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'avatar'">
            <a-avatar :src="record.avatarUrl" :size="36">{{ record.name?.charAt(0) }}</a-avatar>
          </template>
          <template v-if="column.key === 'genre'">
            <template v-if="record.genre">
              <a-tag v-for="g in record.genre.split(',')" :key="g" color="blue">{{ g }}</a-tag>
            </template>
            <a-tag v-else color="gray">未分类</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a class="table-action-link" @click="openForm(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除该艺人？" @confirm="onDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formVisible" :title="editingId ? '编辑艺人' : '新增艺人'" :confirm-loading="submitting" @ok="onSubmit" width="500px">
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="艺名" required><a-input v-model:value="formData.name" /></a-form-item>
        <a-form-item label="头像 URL"><a-input v-model:value="formData.avatarUrl" /></a-form-item>
        <a-form-item label="风格流派">
          <a-select
            v-model:value="formData.styleIds"
            mode="multiple"
            show-search
            option-filter-prop="name"
            placeholder="请选择风格流派（支持搜索）"
            :options="styleOptions"
            :field-names="{ label: 'name', value: 'id' }"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="简介"><a-textarea v-model:value="formData.description" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { performerApi } from '@/api/performer'
import { styleApi } from '@/api/style'
import type { PerformerItem } from '@/types'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '头像', key: 'avatar', width: 70 },
  { title: '艺名', dataIndex: 'name', key: 'name' },
  { title: '风格', key: 'genre', width: 150 },
  { title: '简介', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 140 },
]

const loading = ref(false)
const list = ref<PerformerItem[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function fetchList() {
  loading.value = true
  try {
    const res = await performerApi.page({ current: pagination.current, size: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } finally { loading.value = false }
}

function onTableChange(pag: any) { pagination.current = pag.current; fetchList() }

const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive({ name: '', avatarUrl: '', styleIds: [] as number[], description: '' })
const styleOptions = ref<any[]>([])

async function fetchStyleOptions() {
  try {
    const res = await styleApi.page({ current: 1, size: 100 })
    styleOptions.value = res?.records || []
  } catch (e) {
    console.error('加载风格选项失败', e)
  }
}

function openForm(record?: PerformerItem) {
  if (record) { 
    editingId.value = record.id; 
    formData.name = record.name || '';
    formData.avatarUrl = record.avatarUrl || '';
    formData.styleIds = (record as any).styleIds || [];
    formData.description = record.description || '';
  }
  else { 
    editingId.value = null; 
    formData.name = ''; 
    formData.avatarUrl = ''; 
    formData.styleIds = []; 
    formData.description = '' 
  }
  formVisible.value = true
}

async function onSubmit() {
  if (!formData.name) { message.warning('请填写艺名'); return }
  submitting.value = true
  try {
    if (editingId.value) { await performerApi.update({ id: editingId.value, ...formData } as any); message.success('更新成功') }
    else { await performerApi.create(formData); message.success('创建成功') }
    formVisible.value = false; fetchList()
  } finally { submitting.value = false }
}

async function onDelete(id: number) { await performerApi.delete(id); message.success('已删除'); fetchList() }

onMounted(() => {
  fetchList()
  fetchStyleOptions()
})
</script>
