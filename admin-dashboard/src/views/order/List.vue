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
      <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" :pagination="pagination" @change="onTableChange">
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
import { ref, reactive, onMounted } from 'vue'
import { orderApi } from '@/api/order'

const statusLabels: Record<number, string> = { 1: '待支付', 2: '已出票', 3: '已取消', 4: '已退票' }
const statusColors: Record<number, string> = { 1: 'orange', 2: 'green', 3: 'default', 4: 'red' }

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200 },
  { title: '用户', dataIndex: 'username', key: 'username', width: 100 },
  { title: '演出', dataIndex: 'eventTitle', key: 'eventTitle', ellipsis: true },
  { title: '票种', dataIndex: 'skuName', key: 'skuName', width: 120 },
  { title: '数量', dataIndex: 'ticketCount', key: 'ticketCount', width: 70 },
  { title: '金额', key: 'totalAmount', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '下单时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
]

const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const statusFilter = ref<number | undefined>(undefined)

async function fetchList() {
  loading.value = true
  try {
    const res = await orderApi.page({ status: statusFilter.value, current: pagination.current, size: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } finally { loading.value = false }
}

function onTableChange(pag: any) { pagination.current = pag.current; fetchList() }

onMounted(fetchList)
</script>
