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
          <!-- 城市列精细化折行展示，显示xx市，在xx省下 -->
          <template v-if="column.key === 'city'">
            <div style="display: flex; flex-direction: column; line-height: 1.4">
              <span style="font-weight: 600; font-size: 14px; color: var(--ant-color-text)">
                {{ record.city?.includes('/') ? record.city.split('/')[1] : record.city }}
              </span>
              <span style="font-size: 11px; color: #8c8c8c" v-if="record.city?.includes('/')">
                {{ record.city.split('/')[0] }}
              </span>
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
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
        <a-form-item label="场馆名称" required>
          <a-input v-model:value="formData.name" placeholder="如：上海 Modern Sky LAB" />
        </a-form-item>
        <a-form-item label="城市" required>
          <a-cascader
            v-model:value="selectedCity"
            :options="chinaCities"
            placeholder="请选择省份与城市"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="详细地址">
          <a-input v-model:value="formData.address" placeholder="详细地址" />
        </a-form-item>
        <a-form-item label="容量">
          <a-input-number v-model:value="formData.capacity" :min="1" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PlusOutlined } from '@ant-design/icons-vue'
import { useVenueList } from './useVenueList'

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
  selectedCity,
  chinaCities,
  openForm,
  onSubmit,
  onDelete
} = useVenueList()
</script>
