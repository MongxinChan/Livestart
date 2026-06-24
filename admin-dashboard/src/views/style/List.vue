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
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
      >
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

    <a-modal
      v-model:open="formVisible"
      :title="editingId ? '编辑风格' : '新增风格'"
      :confirm-loading="submitting"
      @ok="onSubmit"
      width="500px"
    >
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="风格名称" required>
          <a-input v-model:value="formData.name" placeholder="如：摇滚" />
        </a-form-item>
        <a-form-item label="风格代码" required>
          <a-input v-model:value="formData.code" placeholder="如：ROCK（唯一标识）" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="formData.description" :rows="3" placeholder="该风格的简要描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PlusOutlined } from '@ant-design/icons-vue'
import { useStyleList } from './useStyleList'

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
  openForm,
  onSubmit,
  onDelete,
} = useStyleList()
</script>
