<template>
  <a-card v-if="visible" :bordered="false" class="shard-scan-card">
    <template #title>
      <span class="card-title">
        <ThunderboltOutlined class="title-icon" />
        默认展示 16 张物理订单分表的真实结算明细
      </span>
    </template>
    <template #extra>
      <a-badge :status="state.scanning ? 'processing' : 'success'" :text="state.scanning ? '分表扫描中...' : '真实金额已同步'" />
    </template>

    <div class="chart-container">
      <svg viewBox="0 0 800 160" class="bar-chart">
        <defs>
          <linearGradient id="barGrad" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stop-color="#1677ff" />
            <stop offset="100%" stop-color="rgba(22, 119, 255, 0.15)" />
          </linearGradient>
        </defs>

        <line x1="30" y1="20" x2="770" y2="20" stroke="rgba(0,0,0,0.03)" />
        <line x1="30" y1="70" x2="770" y2="70" stroke="rgba(0,0,0,0.03)" />
        <line x1="30" y1="120" x2="770" y2="120" stroke="rgba(0,0,0,0.08)" stroke-width="1.5" />

        <g v-for="(shard, i) in state.shards" :key="i">
          <rect
            :x="42 + i * 45"
            :y="120 - (shard.visible ? shard.height : 0)"
            width="22"
            :height="shard.visible ? shard.height : 0"
            fill="url(#barGrad)"
            rx="4"
            class="bar-rect"
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
            ¥{{ formatCompactAmount(shard.revenue) }}
          </text>
        </g>
      </svg>
    </div>

    <a-row :gutter="[10, 10]">
      <a-col :xs="12" :sm="8" :md="6" :lg="4" :xl="3" v-for="shard in state.shards" :key="shard.index">
        <div
          class="shard-grid-item"
          :class="{
            'item-scanning': state.scanIndex === shard.index && state.scanning,
            'item-scanned': !state.scanning || state.scanIndex >= shard.index,
            'item-pending': state.scanning && state.scanIndex < shard.index,
          }"
        >
          <div class="grid-text">
            <span class="grid-name">{{ shard.tableName }}</span>
            <span class="grid-line">出票 {{ shard.tickets }}</span>
            <span class="grid-line">票房 ¥{{ formatAmount(shard.revenue) }}</span>
            <span class="grid-line">佣金 ¥{{ formatAmount(shard.commissionAmount) }}</span>
            <span class="grid-line">实结 ¥{{ formatAmount(shard.settlementAmount) }}</span>
          </div>
          <LoadingOutlined v-if="state.scanIndex === shard.index && state.scanning" spin class="icon-scanning" />
          <CheckCircleOutlined v-else class="icon-scanned" />
        </div>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
import { ThunderboltOutlined, LoadingOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import type { ShardScanState } from './settlement.types'

interface Props {
  visible: boolean
  state: ShardScanState
}

defineProps<Props>()

function formatAmount(value: number) {
  return Number(value ?? 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function formatCompactAmount(value: number) {
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w`
  }
  return Number(value ?? 0).toFixed(0)
}
</script>

<style scoped>
.shard-scan-card {
  margin-bottom: 24px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.02);
}

.card-title {
  font-weight: 700;
}

.title-icon {
  margin-right: 8px;
  color: var(--ant-color-primary);
}

.chart-container {
  margin-bottom: 20px;
}

.bar-chart {
  width: 100%;
  height: 160px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 12px;
  padding: 15px;
}

.bar-rect {
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
}

.shard-grid-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-family: monospace;
  border-width: 1px;
  border-style: solid;
  transition: all 0.2s;
  min-height: 110px;
}

.grid-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.grid-name {
  color: #262626;
  font-weight: 700;
  margin-bottom: 2px;
}

.grid-line {
  color: #595959;
}

.item-pending {
  opacity: 0.5;
  background: rgba(0, 0, 0, 0.01);
  border-color: rgba(0, 0, 0, 0.05);
}

.item-scanning {
  opacity: 1;
  background: rgba(22, 119, 255, 0.08);
  border-color: #1677ff;
}

.item-scanned {
  opacity: 1;
  background: rgba(82, 196, 26, 0.05);
  border-color: #52c41a;
}

.icon-scanning {
  color: #1677ff;
  margin-top: 2px;
}

.icon-scanned {
  color: #52c41a;
  margin-top: 2px;
}
</style>
