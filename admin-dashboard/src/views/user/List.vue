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
        :expandable="{ expandedRowRender }"
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
              <template #bodyCell="{ column: col, record: visitor }">
                <template v-if="col.key === 'idCard'">{{ maskIdCard(visitor.idCard) }}</template>
              </template>
            </a-table>
          </div>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { userApi } from '@/api/user'
import type { UserItem, VisitorItem } from '@/types'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '真实姓名', dataIndex: 'realName', key: 'realName' },
  { title: '手机号', key: 'phone', width: 140 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
]

const visitorColumns = [
  { title: '姓名', dataIndex: 'realName', key: 'realName' },
  { title: '证件号', key: 'idCard' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
]

const loading = ref(false)
const list = ref<UserItem[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const visitorMap = reactive<Record<number, VisitorItem[]>>({})
const loadingVisitors = reactive<Record<number, boolean>>({})

function maskPhone(phone: string) {
  if (!phone || phone.length < 7) return phone
  return phone.substring(0, 3) + '****' + phone.substring(7)
}

function maskIdCard(idCard: string) {
  if (!idCard || idCard.length < 10) return idCard
  return idCard.substring(0, 4) + '**********' + idCard.substring(idCard.length - 4)
}

async function fetchList() {
  loading.value = true
  try {
    const res = await userApi.page({ current: pagination.current, size: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } finally { loading.value = false }
}

function onTableChange(pag: any) { pagination.current = pag.current; fetchList() }

// 展开行时加载观演人
function expandedRowRender() { return null } // 模板中已定义

onMounted(fetchList)
</script>
