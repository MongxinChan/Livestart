<template>
  <div>
    <a-page-header title="风格管理" sub-title="管理演出风格/流派分类" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-button type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>
          新增风格
        </a-button>
      </template>
    </a-page-header>

    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" :pagination="pagination" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'">
            <a-tag color="geekblue">{{ record.code }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a class="table-action-link" @click="openForm(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除该风格？" @confirm="onDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formVisible" :title="editingId ? '编辑风格' : '新增风格'" :confirm-loading="submitting" @ok="onSubmit" width="500px">
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="风格名称" required><a-input v-model:value="formData.name" placeholder="如：摇滚" /></a-form-item>
        <a-form-item label="风格代码" required><a-input v-model:value="formData.code" placeholder="如：ROCK（唯一标识）" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="formData.description" :rows="3" placeholder="该风格的简要描述" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { styleApi } from '@/api/style'
import type { StyleItem } from '@/types'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '风格名称', dataIndex: 'name', key: 'name' },
  { title: '代码', key: 'code', width: 120 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 140 },
]

const loading = ref(false)
const list = ref<StyleItem[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

async function fetchList() {
  loading.value = true
  try {
    const res = await styleApi.page({ current: pagination.current, size: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } finally { loading.value = false }
}

function onTableChange(pag: any) { pagination.current = pag.current; fetchList() }

const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formData = reactive({ name: '', code: '', description: '' })

function openForm(record?: StyleItem) {
  if (record) { editingId.value = record.id; Object.assign(formData, record) }
  else { editingId.value = null; formData.name = ''; formData.code = ''; formData.description = '' }
  formVisible.value = true
}

async function onSubmit() {
  if (!formData.name || !formData.code) { message.warning('请填写名称和代码'); return }
  submitting.value = true
  try {
    if (editingId.value) { await styleApi.update({ id: editingId.value, ...formData }); message.success('更新成功') }
    else { await styleApi.create(formData); message.success('创建成功') }
    formVisible.value = false; fetchList()
  } finally { submitting.value = false }
}

async function onDelete(id: number) { await styleApi.delete(id); message.success('已删除'); fetchList() }

onMounted(fetchList)
</script>
