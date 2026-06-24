<template>
  <div>
    <a-page-header title="用户管理" sub-title="平台注册用户与观演人信息" :ghost="false" style="margin-bottom: 24px" />

    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
        @expand="onExpand"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'phone'">
            <span>{{ maskPhone(record.phone) }}</span>
          </template>
        </template>
        <template #expandedRowRender="{ record }">
          <div style="padding: 8px 0">
            <a-tag color="blue" style="margin-bottom: 8px">观演人列表</a-tag>
            <a-table
              :columns="visitorColumns"
              :data-source="visitorMap[record.id] || []"
              :loading="loadingVisitors[record.id]"
              row-key="id"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column: visitorColumn, record: visitor }">
                <template v-if="visitorColumn.key === 'idCard'">{{ maskIdCard(visitor.idCard) }}</template>
              </template>
            </a-table>
          </div>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { useUserList } from './useUserList'

const {
  columns,
  visitorColumns,
  loading,
  list,
  pagination,
  visitorMap,
  loadingVisitors,
  maskPhone,
  maskIdCard,
  onTableChange,
  onExpand,
} = useUserList()
</script>
