<template>
  <div>
    <a-page-header title="结算报表" sub-title="查看结算单与收入统计" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-space>
          <a-input-number v-model:value="triggerEventId" :min="1" placeholder="演出 ID" style="width: 140px" />
          <a-button type="primary" :loading="triggering" @click="onTrigger">
            <template #icon><ThunderboltOutlined /></template>
            触发结算
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <!-- 统计概览 -->
    <a-row :gutter="16" style="margin-bottom: 24px">
      <a-col :span="6">
        <a-card class="stat-card" :bordered="false">
          <a-statistic title="总票房收入" :value="statsData.totalRevenue" prefix="¥" :value-style="{ color: '#52c41a', fontWeight: 700 }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card" :bordered="false">
          <a-statistic title="平台佣金" :value="statsData.totalCommission" prefix="¥" :value-style="{ color: '#fa8c16', fontWeight: 700 }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card" :bordered="false">
          <a-statistic title="净结算额" :value="statsData.totalNetAmount" prefix="¥" :value-style="{ color: '#1677ff', fontWeight: 700 }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card" :bordered="false">
          <a-statistic title="结算单数" :value="statsData.totalOrders" :value-style="{ fontWeight: 700 }" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 结算单列表 -->
    <a-card title="结算单列表" :bordered="false">
      <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" :pagination="pagination" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'orange'">{{ record.status === 1 ? '已结算' : '待结算' }}</a-tag>
          </template>
          <template v-if="column.key === 'revenue'">
            <span style="color: #52c41a; font-weight: 600">¥{{ record.totalRevenue?.toLocaleString() }}</span>
          </template>
          <template v-if="column.key === 'net'">
            <span style="color: #1677ff; font-weight: 600">¥{{ record.netAmount?.toLocaleString() }}</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'
import { settlementApi } from '@/api/settlement'
import type { SettlementItem, SettlementStats } from '@/types'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '演出', dataIndex: 'eventName', key: 'eventName', ellipsis: true },
  { title: '总售出', dataIndex: 'totalSold', key: 'totalSold', width: 90 },
  { title: '总票房', key: 'revenue', width: 140 },
  { title: '佣金', dataIndex: 'commission', key: 'commission', width: 100 },
  { title: '净结算', key: 'net', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  { title: '结算时间', dataIndex: 'settleTime', key: 'settleTime', width: 170 },
]

const loading = ref(false)
const list = ref<SettlementItem[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const statsData = reactive<SettlementStats>({ totalRevenue: 0, totalCommission: 0, totalNetAmount: 0, totalOrders: 0 })

async function fetchList() {
  loading.value = true
  try {
    const res = await settlementApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } finally { loading.value = false }
}

async function fetchStats() {
  try {
    const res = await settlementApi.stats()
    if (res) Object.assign(statsData, res)
  } catch { /* ignore */ }
}

function onTableChange(pag: any) { pagination.current = pag.current; fetchList() }

const triggerEventId = ref<number>(101)
const triggering = ref(false)

async function onTrigger() {
  if (!triggerEventId.value) { message.warning('请输入演出 ID'); return }
  triggering.value = true
  try {
    await settlementApi.trigger(triggerEventId.value)
    message.success('结算已触发')
    fetchList()
    fetchStats()
  } finally { triggering.value = false }
}

onMounted(() => { fetchList(); fetchStats() })
</script>
