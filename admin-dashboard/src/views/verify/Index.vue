<template>
  <div class="verify-page">
    <a-card title="现场验票" :bordered="false">
      <a-row :gutter="16" class="stats-row">
        <a-col :xs="24" :sm="12" :md="6"><a-card size="small"><a-statistic title="电子票总数" :value="stats.totalCount" /></a-card></a-col>
        <a-col :xs="24" :sm="12" :md="6"><a-card size="small"><a-statistic title="已验票" :value="stats.checkedCount" /></a-card></a-col>
        <a-col :xs="24" :sm="12" :md="6"><a-card size="small"><a-statistic title="未验票" :value="stats.uncheckedCount" /></a-card></a-col>
        <a-col :xs="24" :sm="12" :md="6"><a-card size="small"><a-statistic title="入场率" :value="Number(stats.checkedRate || 0)" suffix="%" :precision="2" /></a-card></a-col>
      </a-row>

      <a-form layout="inline" class="verify-form" @submit.prevent>
        <a-form-item label="电子票码">
          <a-input v-model:value="form.checkCode" allow-clear placeholder="输入或粘贴用户电子票码" style="width: 360px" @pressEnter="handleVerify" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="loading" @click="handleVerify">模拟扫码验票</a-button>
          <a-button class="refresh-btn" @click="loadStats">刷新统计</a-button>
        </a-form-item>
      </a-form>

      <a-alert v-if="errorMessage" class="result-alert" type="error" show-icon :message="errorMessage" />

      <a-descriptions v-if="verifyResult" class="result-box" bordered size="small" title="验票结果" :column="2">
        <a-descriptions-item label="订单号">{{ verifyResult.orderNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="票码">{{ verifyResult.checkCode || '-' }}</a-descriptions-item>
        <a-descriptions-item label="演出 ID">{{ verifyResult.eventId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="票档 ID">{{ verifyResult.skuId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="观演人 ID">{{ verifyResult.visitorId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ verifyResult.status || '-' }}</a-descriptions-item>
        <a-descriptions-item label="核验时间" :span="2">{{ formatTime(verifyResult.checkedAt) }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card class="record-card" title="最近验票记录" :bordered="false">
      <a-table size="small" row-key="id" :columns="columns" :data-source="records" :pagination="false" :locale="{ emptyText: '暂无验票记录' }" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { verify } from '@/api/order'

const form = reactive({ checkCode: '' })
const stats = reactive({ totalCount: 0, checkedCount: 0, uncheckedCount: 0, checkedRate: 0 })
const loading = ref(false)
const verifyResult = ref<any>(null)
const errorMessage = ref('')
const records = ref<any[]>([])

const columns = [
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '票码', dataIndex: 'checkCode', key: 'checkCode' },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '结果', dataIndex: 'result', key: 'result' },
  { title: '说明', dataIndex: 'reason', key: 'reason' }
]

function unwrapResponse(res: any) {
  return res?.data?.data ?? res?.data ?? res
}

function formatTime(value: any) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString()
}

async function loadStats() {
  try {
    const response = await fetch('/api/live-start/engine/order/verify/stats')
    const data = unwrapResponse(await response.json()) || {}
    stats.totalCount = Number(data.totalCount || 0)
    stats.checkedCount = Number(data.checkedCount || 0)
    stats.uncheckedCount = Number(data.uncheckedCount || 0)
    stats.checkedRate = Number(data.checkedRate || 0)
  } catch (error) {
    console.warn('加载验票统计失败', error)
  }
}

async function handleVerify() {
  const checkCode = form.checkCode.trim()
  if (!checkCode) {
    message.warning('请输入电子票码')
    return
  }
  loading.value = true
  errorMessage.value = ''
  verifyResult.value = null
  try {
    const data = unwrapResponse(await verify(checkCode)) || {}
    verifyResult.value = data
    message.success('验票成功')
    records.value.unshift({ id: Date.now(), time: formatTime(data.checkedAt || new Date()), checkCode, orderNo: data.orderNo || '-', result: '成功', reason: data.status || '已入场' })
    form.checkCode = ''
    await loadStats()
  } catch (error: any) {
    const reason = error?.response?.data?.message || error?.message || '验票失败'
    errorMessage.value = reason
    message.error(reason)
    records.value.unshift({ id: Date.now(), time: formatTime(new Date()), checkCode, orderNo: '-', result: '失败', reason })
  } finally {
    loading.value = false
    records.value = records.value.slice(0, 10)
  }
}

onMounted(loadStats)
</script>

<style scoped>
.verify-page { padding: 24px; }
.stats-row { margin-bottom: 24px; }
.verify-form { margin-bottom: 16px; }
.refresh-btn { margin-left: 8px; }
.result-alert, .result-box, .record-card { margin-top: 16px; }
</style>
