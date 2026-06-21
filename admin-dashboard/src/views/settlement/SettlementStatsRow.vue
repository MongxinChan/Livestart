<template>
  <a-row :gutter="16" class="settlement-stats-row">
    <a-col :span="6" v-for="card in cards" :key="card.title">
      <a-card class="stat-card" :bordered="false">
        <a-statistic
          :title="card.title"
          :value="card.value"
          :prefix="card.prefix"
          :value-style="{ color: card.color, fontWeight: 700 }"
        />
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SettlementStats } from '@/types'
import { buildStatsCards } from './settlement.types'

interface Props {
  stats: SettlementStats
}

const props = defineProps<Props>()

const cards = computed(() => buildStatsCards(props.stats))
</script>

<style scoped>
.settlement-stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}
</style>
