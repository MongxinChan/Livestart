<template>
  <div>
    <a-page-header title="艺人管理" sub-title="管理演出艺人和乐队信息" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-button type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>
          新增艺人
        </a-button>
      </template>
    </a-page-header>

    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'avatar'">
            <a-avatar :src="record.avatarUrl" :size="36">{{ record.name?.charAt(0) }}</a-avatar>
          </template>
          <template v-if="column.key === 'genre'">
            <template v-if="record.genre">
              <a-tag v-for="genre in record.genre.split(',')" :key="genre" color="blue">{{ genre }}</a-tag>
            </template>
            <a-tag v-else color="default">未分类</a-tag>
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

    <a-modal
      v-model:open="formVisible"
      :title="editingId ? '编辑艺人' : '新增艺人'"
      :confirm-loading="submitting"
      @ok="onSubmit"
      width="500px"
    >
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="艺名" required>
          <a-input v-model:value="formData.name" />
        </a-form-item>
        <a-form-item label="头像 URL">
          <a-input v-model:value="formData.avatarUrl" />
        </a-form-item>
        <a-form-item label="风格流派">
          <a-select
            v-model:value="formData.styleIds"
            mode="multiple"
            show-search
            option-filter-prop="name"
            placeholder="请选择风格流派"
            :options="styleOptions"
            :field-names="{ label: 'name', value: 'id' }"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="formData.status" :options="statusOptions" />
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea v-model:value="formData.description" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PlusOutlined } from '@ant-design/icons-vue'
import { usePerformerList } from './usePerformerList'

const statusOptions = [
  { label: '正常', value: 1 },
  { label: '停演', value: 0 },
]

const {
  columns,
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
} = usePerformerList()
</script>
