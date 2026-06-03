<template>
  <div class="settlement-view" style="display: flex; flex-direction: column; gap: 24px">

    <!-- 1. 控制面板 -->
    <a-card class="glass-panel" :bordered="false">
      <h3 style="font-size: 1.25rem; font-weight: 800; margin-bottom: 10px">
        <BarChartOutlined style="margin-right: 8px" />
        主办方票房资金分布式核算中心
      </h3>
      <a-typography-paragraph style="max-width: 850px; margin-bottom: 20px" type="secondary">
        针对分库分表多表对账的学术痛点，本看板跨节点多线程扫描分片库 ds_order 下的 16 张物理订单分表
        (t_order_item_0 ~ t_order_item_15)，将分布式订单归散、核准抽佣及应结对账以实时数据可视化大屏的形式动态重现。
      </a-typography-paragraph>

      <a-card size="small" :bordered="false" style="border-radius: 12px; background: rgba(0,0,0,0.06)">
        <a-row :gutter="16" align="middle" justify="space-between">
          <a-col>
            <a-space>
              <span style="font-weight: 600">选择核算场次：</span>
              <a-select v-model:value="selectedEventId" style="min-width: 300px" size="large">
                <a-select-option value="101">万能青年旅店 Modern Sky LAB</a-select-option>
                <a-select-option value="102">周杰伦 杭州奥体中心体育场</a-select-option>
                <a-select-option value="103">重塑雕像的权利 深圳 HOU Live</a-select-option>
                <a-select-option value="104">陈奕迅 广州大学城体育中心</a-select-option>
              </a-select>
            </a-space>
          </a-col>
          <a-col>
            <a-button type="primary" size="large" :loading="isSettling" @click="triggerSettlement">
              <template #icon><FundProjectionScreenOutlined /></template>
              {{ isSettling ? '多物理表资金线程对账核查中...' : '开始跨 16 张物理表核算票房' }}
            </a-button>
          </a-col>
        </a-row>
      </a-card>
    </a-card>

    <!-- 2. 柱状图可视化 -->
    <a-card v-if="isSettling || showVisualization" class="glass-panel" :bordered="false">
      <template #title>
        <span>
          <BarChartOutlined style="margin-right: 8px" />
          16 个订单分片表 (Sharding Order Table) 数据实时散列热力大屏
        </span>
      </template>
      <template v-if="isSettling" #extra>
        <a-badge status="success" text="ACTIVE MULTI-THREADING PORT..." />
      </template>

      <!-- SVG 柱状图 -->
      <div style="margin-bottom: 24px">
        <svg viewBox="0 0 800 200" style="width: 100%; height: 200px; background: rgba(0,0,0,0.12); border-radius: 12px; padding: 15px">
          <defs>
            <linearGradient id="barGrad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="var(--ant-color-primary)" />
              <stop offset="100%" stop-color="rgba(var(--ls-accent-rgb), 0.15)" />
            </linearGradient>
          </defs>

          <line x1="30" y1="20" x2="770" y2="20" stroke="rgba(255,255,255,0.03)" />
          <line x1="30" y1="70" x2="770" y2="70" stroke="rgba(255,255,255,0.03)" />
          <line x1="30" y1="120" x2="770" y2="120" stroke="rgba(255,255,255,0.03)" />
          <line x1="30" y1="170" x2="770" y2="170" stroke="rgba(255,255,255,0.08)" stroke-width="1.5" />

          <g v-for="(shard, i) in liveShards" :key="i">
            <rect
              :x="42 + i * 45"
              :y="170 - (shard.visible ? shard.height : 0)"
              width="24"
              :height="shard.visible ? shard.height : 0"
              fill="url(#barGrad)"
              rx="4"
              style="transition: all 0.4s cubic-bezier(0.19, 1, 0.22, 1)"
            />
            <text
              :x="54 + i * 45"
              y="185"
              fill="var(--ls-text-secondary)"
              font-size="8"
              text-anchor="middle"
              font-family="monospace"
              style="opacity: 0.85"
            >
              t_{{ i }}
            </text>
            <text
              v-if="shard.visible"
              :x="54 + i * 45"
              :y="162 - shard.height"
              fill="var(--ant-color-success)"
              font-size="7"
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
              opacity: scanIndex < i ? 0.3 : 1,
              background: scanIndex === i ? 'rgba(var(--ls-accent-rgb), 0.12)' :
                          scanIndex > i ? 'rgba(var(--ls-accent-rgb), 0.04)' : 'rgba(255,255,255,0.01)',
              borderColor: scanIndex === i ? 'var(--ant-color-primary)' :
                           scanIndex > i ? 'var(--ant-color-success)' : 'rgba(var(--ls-accent-rgb), 0.1)',
              border: '1px solid',
            }"
          >
            <span>t_item_{{ i }}</span>
            <LoadingOutlined v-if="scanIndex === i" spin />
            <CheckCircleOutlined v-else-if="scanIndex > i" style="color: var(--ant-color-success)" />
            <MinusCircleOutlined v-else style="opacity: 0.3" />
          </div>
        </a-col>
      </a-row>
    </a-card>

    <!-- 3. 结算汇总 -->
    <template v-if="settleResult">
      <a-row :gutter="[20, 20]">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="glass-panel glow-card" :bordered="false" style="border-left: 5px solid var(--ant-color-primary)">
            <a-statistic
              title="票房总销售额"
              :value="isSettling ? runningRevenue : settleResult.totalRevenue"
              prefix="¥"
              :value-style="{ fontWeight: 800, fontSize: '1.6rem', fontFamily: 'Outfit' }"
            />
            <div style="font-size: 11px; color: var(--ls-text-secondary); margin-top: 4px">由 16 张分片物理表汇总对账</div>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="glass-panel glow-card" :bordered="false" style="border-left: 5px solid var(--ant-color-success)">
            <a-statistic
              title="核准已出电子客票"
              :value="isSettling ? runningSold : settleResult.totalSold"
              suffix="张"
              :value-style="{ fontWeight: 800, fontSize: '1.6rem', fontFamily: 'Outfit' }"
            />
            <div style="font-size: 11px; color: var(--ls-text-secondary); margin-top: 4px">由 RocketMQ 顺利落库出单</div>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="glass-panel glow-card" :bordered="false" style="border-left: 5px solid var(--ant-color-warning)">
            <a-statistic
              title="平台服务费抽成 (5%)"
              :value="isSettling ? Math.round(runningRevenue * 0.05) : settleResult.commission"
              prefix="¥"
              :value-style="{ fontWeight: 800, fontSize: '1.6rem', fontFamily: 'Outfit' }"
            />
            <div style="font-size: 11px; color: var(--ls-text-secondary); margin-top: 4px">平台 API 与抢票服务保障佣金</div>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="glass-panel glow-card" :bordered="false" style="border-left: 5px solid #a855f7">
            <a-statistic
              title="商户应结算净额"
              :value="isSettling ? Math.round(runningRevenue * 0.95) : settleResult.netAmount"
              prefix="¥"
              :value-style="{ fontWeight: 800, fontSize: '1.6rem', fontFamily: 'Outfit', color: '#a855f7' }"
            />
            <div style="font-size: 11px; color: var(--ls-text-secondary); margin-top: 4px">扣除平台流量抽成后的净票房</div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 16 表详情 -->
      <a-card v-if="!isSettling" class="glass-panel" :bordered="false">
        <template #title>
          <span><AppstoreOutlined style="margin-right: 8px" /> ShardingSphere 16 张物理订单分片表散列详情</span>
        </template>
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :sm="12" :lg="8" :xl="6" v-for="shard in settleResult.shards" :key="shard.tableName">
            <a-card size="small" hoverable class="glow-card" :bordered="true" style="border-radius: 12px">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
                <span style="font-family: monospace; font-weight: 700">{{ shard.tableName }}</span>
                <a-tag :color="shard.dbShard === 'ds_order_0' ? 'success' : 'purple'">{{ shard.dbShard }}</a-tag>
              </div>
              <div style="display: flex; justify-content: space-between; font-size: 12px; color: var(--ls-text-secondary); margin-bottom: 8px">
                <span>订单: <b>{{ shard.orders }}</b> 笔</span>
                <span>门票: <b>{{ shard.tickets }}</b> 张</span>
              </div>
              <a-divider style="margin: 6px 0" dashed />
              <div style="display: flex; justify-content: space-between; align-items: center">
                <span style="font-size: 12px; color: var(--ls-text-secondary)">本表金额:</span>
                <span style="font-weight: 700; font-size: 0.95rem">¥ {{ shard.revenue.toLocaleString() }}</span>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </a-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  BarChartOutlined, FundProjectionScreenOutlined,
  LoadingOutlined, CheckCircleOutlined, MinusCircleOutlined,
  AppstoreOutlined,
} from '@ant-design/icons-vue'
import { request } from '@/composables/useRequest'
import type { SettlementResult, LiveShard } from '@/types'

// --- 状态 ---
const selectedEventId = ref('101')
const isSettling = ref(false)
const showVisualization = ref(false)
const settleResult = ref<SettlementResult | null>(null)
const scanIndex = ref(-1)
const liveShards = ref<LiveShard[]>([])
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

async function triggerSettlement() {
  isSettling.value = true
  settleResult.value = null
  showVisualization.value = true
  scanIndex.value = -1
  runningRevenue.value = 0
  runningSold.value = 0
  initShardsPlaceholder()

  try {
    const data = await request<SettlementResult>(`/api/settlement/trigger?eventId=${selectedEventId.value}`)
    const revenues = data.shards.map(s => s.revenue)
    const maxRev = Math.max(...revenues) || 1

    let i = 0
    const scanTimer = setInterval(() => {
      if (i < 16) {
        scanIndex.value = i
        const shardData = data.shards[i]
        liveShards.value[i].visible = true
        liveShards.value[i].revenue = shardData.revenue
        liveShards.value[i].height = (shardData.revenue / maxRev) * 135 + 8
        runningRevenue.value += shardData.revenue
        runningSold.value += shardData.tickets
        i++
      } else {
        clearInterval(scanTimer)
        settleResult.value = data
        isSettling.value = false
        message.success('16 张分片表票房核算完成！')
      }
    }, 120)
  } catch (err: any) {
    message.error('票房结算对账失败: ' + err.message)
    isSettling.value = false
    showVisualization.value = false
  }
}

onMounted(() => {
  initShardsPlaceholder()
})
</script>
