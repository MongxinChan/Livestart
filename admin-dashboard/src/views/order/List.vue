<template>
  <div>
    <a-page-header title="订单管理" sub-title="查看全平台订单数据（只读）" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-radio-group v-model:value="statusFilter" button-style="solid" @change="fetchList">
          <a-radio-button :value="undefined">全部</a-radio-button>
          <a-radio-button :value="1">待支付</a-radio-button>
          <a-radio-button :value="2">已出票</a-radio-button>
          <a-radio-button :value="3">已取消</a-radio-button>
          <a-radio-button :value="4">已退票</a-radio-button>
        </a-radio-group>
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
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColors[record.status]">{{ statusLabels[record.status] }}</a-tag>
          </template>
          <template v-if="column.key === 'totalAmount'">
            <span style="color: #ff4d4f; font-weight: 600">¥{{ record.totalAmount }}</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { useOrderList } from './useOrderList'

const {
  columns,
  statusLabels,
  statusColors,
  loading,
  list,
  pagination,
  statusFilter,
  fetchList,
  onTableChange,
} = useOrderList()
</script>
