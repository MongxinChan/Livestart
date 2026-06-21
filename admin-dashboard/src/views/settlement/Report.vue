<template>
  <div>
    <a-page-header title="结算报表" sub-title="查看结算单与收入统计" :ghost="false" class="page-header">
      <template #extra>
        <a-space>
          <a-input-number v-model:value="triggerEventId" :min="1" placeholder="演出 ID" class="event-id-input" />
          <a-button type="primary" :loading="triggering" @click="onTrigger">
            <template #icon><ThunderboltOutlined /></template>
            触发结算对账
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
import { ref, onMounted, computed } from 'vue'
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
const { state: scanState, showVisualization, start: startScan, reset: resetScan } = useShardScanAnimation()

const triggering = computed(() => scanState.value.scanning)
const triggerEventId = ref<number>(101)

async function onTrigger() {
  if (!triggerEventId.value) {
    message.warning('请输入演出 ID')
    return
  }

  try {
    await settlementApi.trigger(triggerEventId.value)

    startScan({
      eventId: triggerEventId.value,
      onComplete: async () => {
        await Promise.all([fetchList(), fetchStats()])
        message.success('16 张物理订单表票房结算核准对账完成！')
      },
    })
  } catch (err: any) {
    resetScan()
    message.error('触发物理表对账失败: ' + (err?.message ?? '未知错误'))
  }
}

onMounted(() => {
  fetchList()
  fetchStats()
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

