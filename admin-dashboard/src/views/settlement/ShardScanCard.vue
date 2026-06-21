<template>
  <a-card v-if="visible" :bordered="false" class="shard-scan-card">
    <template #title>
      <span class="card-title">
        <ThunderboltOutlined class="title-icon" />
        ShardingSphere 16 个订单分片表 (ds_order_0 / ds_order_1) 数据热力扫描
      </span>
    </template>
    <template #extra>
      <a-badge :status="state.scanning ? 'processing' : 'success'" :text="state.scanning ? '多线程扫描中...' : '对账完成'" />
    </template>

    <!-- SVG 柱状图 -->
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
          :class="{
            'item-scanning': state.scanIndex === i,
            'item-scanned': state.scanIndex > i,
            'item-pending': state.scanIndex < i,
          }"
        >
          <span>t_order_{{ i }}</span>
          <LoadingOutlined v-if="state.scanIndex === i" spin class="icon-scanning" />
          <CheckCircleOutlined v-else-if="state.scanIndex > i" class="icon-scanned" />
          <MinusCircleOutlined v-else class="icon-pending" />
        </div>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
import { ThunderboltOutlined, LoadingOutlined, CheckCircleOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import type { ShardScanState } from './settlement.types'

interface Props {
  visible: boolean
  state: ShardScanState
}

defineProps<Props>()
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
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-family: monospace;
  border-width: 1px;
  border-style: solid;
  transition: all 0.2s;
}

.item-pending {
  opacity: 0.35;
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
}

.icon-scanned {
  color: #52c41a;
}

.icon-pending {
  opacity: 0.3;
}
</style>
