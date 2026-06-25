<template>
  <div>
    <a-page-header title="结算报表" sub-title="按当前账号可见范围展示结算单与收入统计" :ghost="false" class="page-header">
      <template #extra>
        <a-space>
          <a-input-number v-model:value="eventIdFilter" :min="1" placeholder="演出 ID" class="event-id-input" />
          <a-button @click="onRefresh">刷新</a-button>
          <a-button type="primary" :loading="triggering" @click="onTriggerVisible">
            <template #icon><ThunderboltOutlined /></template>
            结算当前可见演出
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <ShardScanCard :visible="showVisualization" :state="scanState" />

    <SettlementStatsRow :stats="statsData" />

    <SettlementTable
      :columns="columns"
      :list="list"
      :loading="loading"
      :pagination="pagination"
      @change="onTableChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'
import { settlementApi } from '@/api/settlement'
import { useShardScanAnimation } from './useShardScanAnimation'
import { useSettlementList } from './useSettlementList'
import { useSettlementStats } from './useSettlementStats'
import ShardScanCard from './ShardScanCard.vue'
import SettlementStatsRow from './SettlementStatsRow.vue'
import SettlementTable from './SettlementTable.vue'

const { columns, loading, list, pagination, fetchList, onTableChange } = useSettlementList()
const { statsData, fetchStats } = useSettlementStats()
const { state: scanState, showVisualization, start: startScan, reset: resetScan, syncFromShards } = useShardScanAnimation()

const triggering = computed(() => scanState.value.scanning)
const eventIdFilter = ref<number>()

async function reloadPageData() {
  const shardData = await settlementApi.shards(eventIdFilter.value ?? undefined).catch(() => [])
  await Promise.all([
    fetchList(eventIdFilter.value),
    fetchStats(eventIdFilter.value),
  ])
  syncFromShards(Array.isArray(shardData) ? shardData : [])
}

async function onRefresh() {
  await reloadPageData()
}

async function onTriggerVisible() {
  try {
    await settlementApi.triggerVisible()
    await reloadPageData()
    startScan({
      shards: [],
      onComplete: async () => {
        await reloadPageData()
        message.success('结算已按当前账号可见范围刷新')
      },
    })
  } catch (err: any) {
    resetScan()
    message.error('结算刷新失败：' + (err?.message ?? '未知错误'))
  }
}

onMounted(async () => {
  await reloadPageData()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.event-id-input {
  width: 140px;
}
</style>
