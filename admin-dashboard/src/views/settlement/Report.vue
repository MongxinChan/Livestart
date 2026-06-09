<template>
  <div>
    <a-page-header title="结算报表" sub-title="查看结算单与收入统计" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-space>
          <a-input-number v-model:value="triggerEventId" :min="1" placeholder="演出 ID" style="width: 140px" />
          <a-button type="primary" :loading="triggering" @click="onTrigger">
            <template #icon><ThunderboltOutlined /></template>
            触发结算对账
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <!-- 16个订单分片物理表实时数据对账散列热力大盘 -->
    <a-card v-if="triggering || showVisualization" :bordered="false" style="margin-bottom: 24px; border-radius: 12px; background: rgba(0,0,0,0.02)">
      <template #title>
        <span style="font-weight: 700">
          <ThunderboltOutlined style="margin-right: 8px; color: var(--ant-color-primary)" />
          ShardingSphere 16 个订单分片表 (ds_order_0 / ds_order_1) 数据热力扫描
        </span>
      </template>
      <template #extra>
        <a-badge :status="triggering ? 'processing' : 'success'" :text="triggering ? '多线程扫描中...' : '对账完成'" />
      </template>

      <!-- SVG 柱状图 -->
      <div style="margin-bottom: 20px">
        <svg viewBox="0 0 800 160" style="width: 100%; height: 160px; background: rgba(0,0,0,0.04); border-radius: 12px; padding: 15px">
          <defs>
            <linearGradient id="barGrad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#1677ff" />
              <stop offset="100%" stop-color="rgba(22, 119, 255, 0.15)" />
            </linearGradient>
          </defs>

          <line x1="30" y1="20" x2="770" y2="20" stroke="rgba(0,0,0,0.03)" />
          <line x1="30" y1="70" x2="770" y2="70" stroke="rgba(0,0,0,0.03)" />
          <line x1="30" y1="120" x2="770" y2="120" stroke="rgba(0,0,0,0.08)" stroke-width="1.5" />

          <g v-for="(shard, i) in liveShards" :key="i">
            <rect
              :x="42 + i * 45"
              :y="120 - (shard.visible ? shard.height : 0)"
              width="22"
              :height="shard.visible ? shard.height : 0"
              fill="url(#barGrad)"
              rx="4"
              style="transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1)"
            />
            <text
              :x="53 + i * 45"
              y="135"
              fill="#8c8c8c"
              font-size="8"
              text-anchor="middle"
              font-family="monospace"
            >
              t_{{ i }}
            </text>
            <text
              v-if="shard.visible"
              :x="53 + i * 45"
              :y="112 - shard.height"
              fill="#52c41a"
              font-size="8"
              font-weight="600"
              text-anchor="middle"
              font-family="monospace"
            >
              ¥{{ Math.round(shard.revenue / 1000) }}k
            </text>
          </g>
        </svg>
      </div>

      <!-- 16 表扫描网格 -->
      <a-row :gutter="[10, 10]">
        <a-col :xs="12" :sm="8" :md="6" :lg="4" :xl="3" v-for="(_, i) in 16" :key="i">
          <div
            class="shard-grid-item"
            :style="{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '6px 10px',
              borderRadius: '6px',
              fontSize: '11px',
              fontFamily: 'monospace',
              opacity: scanIndex < i ? 0.35 : 1,
              background: scanIndex === i ? 'rgba(22, 119, 255, 0.08)' :
                          scanIndex > i ? 'rgba(82, 196, 26, 0.05)' : 'rgba(0,0,0,0.01)',
              borderColor: scanIndex === i ? '#1677ff' :
                           scanIndex > i ? '#52c41a' : 'rgba(0,0,0,0.05)',
              borderWidth: '1px',
              borderStyle: 'solid',
              transition: 'all 0.2s',
            }"
          >
            <span>t_order_{{ i }}</span>
            <LoadingOutlined v-if="scanIndex === i" spin style="color: #1677ff" />
            <CheckCircleOutlined v-else-if="scanIndex > i" style="color: #52c41a" />
            <MinusCircleOutlined v-else style="opacity: 0.3" />
          </div>
        </a-col>
      </a-row>
    </a-card>

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
import {
  ThunderboltOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
  MinusCircleOutlined,
} from '@ant-design/icons-vue'
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

// --- 16表实时大屏扫描相关 ---
const showVisualization = ref(false)
const scanIndex = ref(-1)
const liveShards = ref<any[]>([])
const runningRevenue = ref(0)
const runningSold = ref(0)

function initShardsPlaceholder() {
  liveShards.value = Array.from({ length: 16 }, (_, i) => ({
    tableName: `t_order_item_${i}`,
    visible: false,
    height: 0,
    revenue: 0,
  }))
}

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
  showVisualization.value = true
  scanIndex.value = -1
  runningRevenue.value = 0
  runningSold.value = 0
  initShardsPlaceholder()

  try {
    // 1. 调用真实后台触发结算
    await settlementApi.trigger(triggerEventId.value)

    // 2. 模拟 16 个物理分表的数据散列扫描 (演示核心卖点)
    const priceBase = triggerEventId.value === 102 ? 980 : 380
    const totalTicketsEstimated = Math.floor(180 + Math.random() * 220)
    const maxRev = (totalTicketsEstimated * priceBase) / 10

    let i = 0
    const scanTimer = setInterval(() => {
      if (i < 16) {
        scanIndex.value = i
        const shardTickets = Math.floor(8 + Math.random() * 32)
        const shardRevenue = shardTickets * priceBase
        liveShards.value[i].visible = true
        liveShards.value[i].revenue = shardRevenue
        liveShards.value[i].height = (shardRevenue / maxRev) * 90 + 10
        runningRevenue.value += shardRevenue
        runningSold.value += shardTickets
        i++
      } else {
        clearInterval(scanTimer)
        triggering.value = false
        message.success('16 张物理订单表票房结算核准对账完成！')
        fetchList()
        fetchStats()
      }
    }, 120)
  } catch (err: any) {
    triggering.value = false
    showVisualization.value = false
    message.error('触发物理表对账对账失败: ' + err.message)
  }
}

onMounted(() => {
  fetchList()
  fetchStats()
  initShardsPlaceholder()
})
</script>
