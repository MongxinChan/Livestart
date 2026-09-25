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
          <a-input v-model:value="form.checkCode" allow-clear placeholder="输入或扫描电子票码" class="code-input" @pressEnter="handleVerify" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="loading" @click="handleVerify">核销入场</a-button>
          <a-button class="refresh-btn" @click="openScanner"><CameraOutlined />扫码</a-button>
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
      <a-table size="small" row-key="id" :columns="columns" :data-source="records" :pagination="false" :loading="recordsLoading" :scroll="{ x: 760 }" :locale="{ emptyText: '暂无验票记录' }" />
    </a-card>
    <a-modal v-model:open="scannerOpen" title="扫描电子票" :footer="null" @cancel="closeScanner" @afterClose="closeScanner">
      <video ref="videoElement" class="scanner-video" autoplay muted playsinline />
      <a-alert v-if="scannerError" type="error" show-icon :message="scannerError" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CameraOutlined } from '@ant-design/icons-vue'
import { BrowserQRCodeReader } from '@zxing/browser'
import type { IScannerControls } from '@zxing/browser'
import { orderApi } from '@/api/order'

const form = reactive({ checkCode: '' })
const stats = reactive({ totalCount: 0, checkedCount: 0, uncheckedCount: 0, checkedRate: 0 })
const loading = ref(false)
const verifyResult = ref<any>(null)
const errorMessage = ref('')
const records = ref<any[]>([])
const recordsLoading = ref(false)
const scannerOpen = ref(false)
const scannerError = ref('')
const videoElement = ref<HTMLVideoElement | null>(null)
let scannerControls: IScannerControls | undefined
let statsTimer: ReturnType<typeof setInterval> | undefined

const columns = [
  { title: '时间', dataIndex: 'checkedAt', key: 'checkedAt', customRender: ({ text }: { text: string }) => formatTime(text) },
  { title: '票码', dataIndex: 'checkCode', key: 'checkCode' },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '演出 ID', dataIndex: 'eventId', key: 'eventId' },
  { title: '核销人 ID', dataIndex: 'checkedBy', key: 'checkedBy' },
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
    const data = unwrapResponse(await orderApi.verifyStats()) || {}
    stats.totalCount = Number(data.totalCount || 0)
    stats.checkedCount = Number(data.checkedCount || 0)
    stats.uncheckedCount = Number(data.uncheckedCount || 0)
    stats.checkedRate = Number(data.checkedRate || 0)
  } catch (error) {
    console.warn('加载验票统计失败', error)
  }
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    records.value = unwrapResponse(await orderApi.verifyRecords()) || []
  } catch (error) {
    console.warn('加载验票记录失败', error)
  } finally {
    recordsLoading.value = false
  }
}

async function openScanner() {
  scannerError.value = ''
  scannerOpen.value = true
  await new Promise(resolve => setTimeout(resolve, 0))
  if (!videoElement.value || !scannerOpen.value) return
  try {
    const controls = await new BrowserQRCodeReader().decodeFromVideoDevice(undefined, videoElement.value, (result) => {
      if (!result) return
      form.checkCode = result.getText()
      closeScanner()
    })
    if (scannerOpen.value) scannerControls = controls
    else controls.stop()
  } catch {
    scannerError.value = '无法打开摄像头，请检查浏览器权限及 HTTPS 连接。'
  }
}

function closeScanner() {
  scannerControls?.stop()
  scannerControls = undefined
  scannerOpen.value = false
}

async function handleVerify() {
  if (loading.value) return
  const checkCode = form.checkCode.trim()
  if (!checkCode) {
    message.warning('请输入电子票码')
    return
  }
  loading.value = true
  errorMessage.value = ''
  verifyResult.value = null
  try {
    const data = unwrapResponse(await orderApi.verify(checkCode)) || {}
    verifyResult.value = data
    message.success('验票成功')
    form.checkCode = ''
    stats.checkedCount += 1
    stats.uncheckedCount = Math.max(stats.uncheckedCount - 1, 0)
    stats.checkedRate = stats.totalCount ? stats.checkedCount / stats.totalCount * 100 : 0
    void loadRecords()
  } catch (error: any) {
    const reason = error?.response?.data?.message || error?.message || '验票失败'
    errorMessage.value = reason
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
  loadRecords()
  statsTimer = setInterval(loadStats, 30000)
})
onUnmounted(() => {
  if (statsTimer) clearInterval(statsTimer)
  closeScanner()
})
</script>

<style scoped>
.verify-page { padding: 24px; }
.stats-row { margin-bottom: 24px; }
.verify-form { margin-bottom: 16px; }
.refresh-btn { margin-left: 8px; }
.result-alert, .result-box, .record-card { margin-top: 16px; }
.code-input { width: min(360px, 75vw); }
.scanner-video { width: 100%; aspect-ratio: 4 / 3; object-fit: cover; background: #111; }
</style>
