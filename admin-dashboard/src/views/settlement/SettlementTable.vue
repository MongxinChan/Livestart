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
        <template v-if="column.key === 'eventTitle'">
          <div class="event-cell">
            <div class="event-title">{{ record.eventTitle }}</div>
            <div class="event-subline">艺人：{{ record.performerName || '未绑定' }}</div>
          </div>
        </template>
        <template v-else-if="column.key === 'status'">
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
import { SETTLEMENT_STATUS_META, type AntTableChange, type SettlementSortField, type SettlementSortOrder, type SettlementStatusValue } from './settlement.types'

interface Props {
  columns: any[]
  list: SettlementItem[]
  loading: boolean
  pagination: { current: number; pageSize: number; total: number }
}

interface Emits {
  (e: 'change', pag: AntTableChange): void
}

defineProps<Props>()
const emit = defineEmits<Emits>()

const statusMeta = SETTLEMENT_STATUS_META

function onChange(pag: { current?: number; pageSize?: number }, _filters: unknown, sorter: any) {
  emit('change', {
    current: pag.current || 1,
    pageSize: pag.pageSize || 10,
    sortField: sorter?.field as SettlementSortField | undefined,
    sortOrder: sorter?.order as SettlementSortOrder | undefined,
  })
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
.event-cell {
  min-width: 220px;
}

.event-title {
  color: #1f1f1f;
  font-weight: 600;
}

.event-subline {
  margin-top: 2px;
  color: #8c8c8c;
  font-size: 12px;
}

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
