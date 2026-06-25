<template>
  <a-card title="结算单列表" :bordered="false">
    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      row-key="id"
      :pagination="pagination"
      @change="onChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusMeta[record.status as SettlementStatusValue].color">
            {{ statusMeta[record.status as SettlementStatusValue].label }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'revenue'">
          <span class="amount-revenue">¥{{ formatAmount(record.totalSalesAmount) }}</span>
        </template>
        <template v-else-if="column.key === 'commission'">
          <span class="amount-commission">¥{{ formatAmount(record.commissionAmount) }}</span>
        </template>
        <template v-else-if="column.key === 'net'">
          <span class="amount-net">¥{{ formatAmount(record.settlementAmount) }}</span>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import type { SettlementItem } from '@/types'
import { SETTLEMENT_STATUS_META, type AntTablePaginationChange, type SettlementStatusValue } from './settlement.types'

interface Props {
  columns: any[]
  list: SettlementItem[]
  loading: boolean
  pagination: { current: number; pageSize: number; total: number }
}

interface Emits {
  (e: 'change', pag: AntTablePaginationChange): void
}

defineProps<Props>()
const emit = defineEmits<Emits>()

const statusMeta = SETTLEMENT_STATUS_META

function onChange(pag: AntTablePaginationChange) {
  emit('change', pag)
}

function formatAmount(value?: number) {
  if (value == null) {
    return '0.00'
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}
</script>

<style scoped>
.amount-revenue {
  color: #52c41a;
  font-weight: 600;
}

.amount-commission {
  color: #fa8c16;
  font-weight: 600;
}

.amount-net {
  color: #1677ff;
  font-weight: 600;
}
</style>
