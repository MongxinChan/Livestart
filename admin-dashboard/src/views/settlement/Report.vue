<template>
  <div>
    <a-page-header title="结算报表" sub-title="支持按演出序号、演出名、艺人名搜索，并按关键指标排序" :ghost="false" class="page-header">
      <template #extra>
        <a-space wrap>
          <a-input
            v-model:value="searchKeyword"
            allow-clear
            placeholder="搜索演出序号 / 演出名 / 艺人名"
            class="search-input"
            @pressEnter="onSearch"
          />
          <a-button type="primary" @click="onSearch">搜索</a-button>
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'
import { settlementApi } from '@/api/settlement'
import { useShardScanAnimation } from './useShardScanAnimation'
import { useSettlementList } from './useSettlementList'
import { useSettlementStats } from './useSettlementStats'
import ShardScanCard from './ShardScanCard.vue'
import SettlementStatsRow from './SettlementStatsRow.vue'
import SettlementTable from './SettlementTable.vue'

const route = useRoute()
const { columns, loading, list, pagination, keyword, fetchList, search, clearSearchState, onTableChange } = useSettlementList()
const { statsData, fetchStats } = useSettlementStats()
const { state: scanState, showVisualization, start: startScan, reset: resetScan, syncFromShards } = useShardScanAnimation()

const triggering = computed(() => scanState.value.scanning)
const searchKeyword = ref(keyword.value)

async function reloadPageData() {
  const shardData = await settlementApi.shards().catch(() => [])
  await Promise.all([
    fetchList(),
    fetchStats(),
  ])
  syncFromShards(Array.isArray(shardData) ? shardData : [])
}

async function onSearch() {
  await search(searchKeyword.value)
  await fetchStats()
}

async function onRefresh() {
  searchKeyword.value = ''
  clearSearchState()
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
  await syncRouteFilter()
})

watch(
  () => route.query.eventId,
  async () => {
    await syncRouteFilter()
  }
)

async function syncRouteFilter() {
  const eventId = normalizeRouteEventId(route.query.eventId)
  if (eventId !== undefined) {
    searchKeyword.value = String(eventId)
    await search(String(eventId), eventId)
    await fetchStats(eventId)
    const shardData = await settlementApi.shards(eventId).catch(() => [])
    syncFromShards(Array.isArray(shardData) ? shardData : [])
    return
  }

  await reloadPageData()
}

function normalizeRouteEventId(value: unknown): number | undefined {
  if (typeof value !== 'string' || !value.trim()) {
    return undefined
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.search-input {
  width: 320px;
}
</style>
