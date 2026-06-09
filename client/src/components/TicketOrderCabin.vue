<template>
  <div class="order-cabin">

    <!-- ========== 抢票操作舱 ========== -->
    <a-card v-if="selectedEvent" class="glass-panel" :bordered="false" style="margin-bottom: 28px">
      <a-button type="link" @click="$emit('backToSquare')" style="padding: 0; margin-bottom: 20px; font-weight: 600">
        <template #icon><ArrowLeftOutlined /></template>
        返回演出广场
      </a-button>

      <a-row :gutter="[36, 24]">
        <!-- 左侧：演出海报 -->
        <a-col :xs="24" :md="8">
          <div style="border-radius: 14px; overflow: hidden; border: 1px solid rgba(var(--ls-accent-rgb), 0.15); box-shadow: var(--ls-neon-glow); margin-bottom: 16px">
            <img :src="selectedEvent.cover" :alt="selectedEvent.title" style="width: 100%; height: 280px; object-fit: cover" />
          </div>
          <h3 style="font-size: 1.1rem; font-weight: 700; line-height: 1.4; margin-bottom: 10px">{{ selectedEvent.title }}</h3>
          <div style="font-size: 13px; color: var(--ls-text-secondary); display: flex; flex-direction: column; gap: 5px">
            <span><CalendarOutlined /> {{ selectedEvent.date }}</span>
            <span><EnvironmentOutlined /> {{ selectedEvent.venue }}</span>
          </div>
        </a-col>

        <!-- 右侧：抢票操作 -->
        <a-col :xs="24" :md="16">
          <!-- 票档选择 -->
          <h4 style="font-size: 0.95rem; font-weight: 600; margin-bottom: 12px">
            <TagOutlined style="margin-right: 6px" /> 选择票档规格
          </h4>
          <a-radio-group v-model:value="activeSkuId" style="width: 100%; margin-bottom: 20px">
            <a-space direction="vertical" style="width: 100%">
              <a-radio-button
                v-for="sku in selectedEvent.skus"
                :key="sku.id"
                :value="sku.id"
                :disabled="sku.stock <= 0"
                style="width: 100%; height: auto; padding: 12px 16px; border-radius: 10px; display: flex; justify-content: space-between; align-items: center"
              >
                <div style="display: flex; justify-content: space-between; width: 100%; align-items: center">
                  <span style="font-weight: 600">{{ sku.name }}</span>
                  <a-space>
                    <a-tag v-if="sku.stock <= 0" color="error">JVM L1 已售罄</a-tag>
                    <a-tag v-else color="success">余 {{ sku.stock }} 张</a-tag>
                    <span style="font-weight: 800; font-size: 1.05rem">¥ {{ sku.price }}</span>
                  </a-space>
                </div>
              </a-radio-button>
            </a-space>
          </a-radio-group>

          <!-- 实名观演人 -->
          <h4 style="font-size: 0.95rem; font-weight: 600; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center">
            <span>
              <TeamOutlined style="margin-right: 6px" /> 勾选实名观演人
              <span style="font-size: 11px; color: var(--ls-text-secondary); font-weight: normal; margin-left: 6px">(观演人数须与购票张数一致)</span>
            </span>
            <a-button type="link" size="small" style="padding: 0" @click="showVisitorManager = true">
              <template #icon><SettingOutlined /></template>
              ⚙️ 管理观演人
            </a-button>
          </h4>
          <a-row :gutter="[12, 12]" style="margin-bottom: 20px">
            <a-col :xs="24" :sm="8" v-for="v in visitorList" :key="v.id">
              <a-card
                size="small"
                hoverable
                :class="{ 'glow-card': true }"
                :style="{ borderColor: v.checked ? 'var(--ant-color-primary)' : undefined }"
                @click="toggleVisitor(v)"
              >
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <span style="font-weight: 600; font-size: 13px">{{ v.name }}</span>
                  <a-checkbox :checked="v.checked" />
                </div>
                <div style="font-size: 11px; color: var(--ls-text-secondary); font-family: monospace; margin-top: 4px">
                  {{ v.idCard }}
                </div>
              </a-card>
            </a-col>
          </a-row>

          <!-- 购买数量 -->
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px">
            <span style="font-weight: 600">购买数量：</span>
            <a-space>
              <a-input-number v-model:value="ticketCount" :min="1" :max="6" size="large" @change="onCountChange" />
              <span style="font-size: 12px; color: var(--ls-text-secondary)">(每人限购 6 张)</span>
            </a-space>
          </div>

          <!-- 抢票控制面板 -->
          <a-card :bordered="false" style="background: rgba(0,0,0,0.08); border: 1px dashed rgba(var(--ls-accent-rgb), 0.2); border-radius: 14px">
            <!-- 演示模式切换开关 -->
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px dashed rgba(var(--ls-accent-rgb), 0.15)">
              <span style="font-weight: 600; font-size: 13px; display: flex; align-items: center; gap: 6px">
                <ThunderboltOutlined style="color: var(--ant-color-primary)" />
                开启高并发压测演示模式 (答辩专用)
              </span>
              <a-switch v-model:checked="showStressPanel" :disabled="grabStatus !== 'idle' || stressRunning" />
            </div>

            <!-- 手动抢票控制区 -->
            <div v-if="!showStressPanel">
              <!-- 待抢票 -->
              <div v-if="grabStatus === 'idle'">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px">
                  <span style="font-size: 13px; color: var(--ls-text-secondary)">合计：</span>
                  <span style="font-size: 1.5rem; font-weight: 800">¥ {{ totalPrice }}</span>
                </div>
                <a-button type="primary" block size="large" :disabled="!activeSku || activeSku.stock <= 0" @click="triggerGrab">
                  <template #icon><ThunderboltOutlined /></template>
                  {{ activeSku && activeSku.stock <= 0 ? '该规格已被 JVM L1 本地极速拦截' : '立即开始高并发极速抢票' }}
                </a-button>
              </div>

              <!-- 获取 Token -->
              <div v-else-if="grabStatus === 'fetching_token'" style="display: flex; gap: 20px; align-items: center; padding: 8px 0">
                <div class="fingerprint-scanner" style="color: var(--ant-color-primary)">
                  <ScanOutlined style="font-size: 28px" />
                </div>
                <div>
                  <h5 style="font-weight: 700; margin-bottom: 2px">🛡️ WAF/防刷盾指纹安全扫描中</h5>
                  <p style="font-size: 12px; color: var(--ls-text-secondary); margin: 0">正在校对设备防刷指纹与签名，动态获取单次抢票 Path Token...</p>
                </div>
              </div>

              <!-- 排队落库 -->
              <div v-else-if="grabStatus === 'grabbing'" style="text-align: center; padding: 8px 0">
                <a-progress :percent="grabProgress" :stroke-color="{ from: 'var(--ant-color-primary)', to: 'var(--ant-color-success)' }" :show-info="false" style="margin-bottom: 12px" />
                <div style="display: flex; justify-content: space-between; margin-bottom: 6px">
                  <a-tag color="success"><SafetyCertificateOutlined /> Token 校验通过</a-tag>
                  <span style="font-size: 11px; color: var(--ls-text-secondary); font-family: monospace">{{ pathToken.substring(0, 18) }}...</span>
                </div>
                <h5 style="font-weight: 700; margin-bottom: 3px">
                  <LoadingOutlined spin /> RocketMQ 削峰异步落库中 ({{ grabProgress }}%)
                </h5>
                <p style="font-size: 12px; color: var(--ls-text-secondary); margin: 0">本地事务隔离去重，正在将高并发下单请求写入持久化物理库中...</p>
              </div>

              <!-- 成功 -->
              <a-result v-else-if="grabStatus === 'success'" status="success" title="下单排队已落库！抢票成功" :sub-title="'订单流水单号：' + createdOrderNo">
                <template #extra>
                  <a-space>
                    <a-button @click="resetGrab">再抢一单</a-button>
                    <a-button type="primary" @click="$emit('goToOrders')">查看订单</a-button>
                  </a-space>
                </template>
              </a-result>

              <!-- 失败 -->
              <a-result v-else-if="grabStatus === 'failed'" status="error" title="抢票拦截触发！" :sub-title="grabError">
                <template #extra>
                  <a-button type="primary" @click="resetGrab">重新选择规格</a-button>
                </template>
              </a-result>
            </div>

            <!-- 高并发压测面板 -->
            <div v-else>
              <!-- 控制区 -->
              <div style="margin-bottom: 16px">
                <a-row :gutter="16" align="middle">
                  <a-col :span="12">
                    <div style="font-size: 12px; margin-bottom: 4px; color: var(--ls-text-secondary)">
                      并发用户数：<b style="color: var(--ant-color-primary)">{{ stressConcurrency }}</b>
                    </div>
                    <a-slider v-model:value="stressConcurrency" :min="10" :max="500" :step="10" :disabled="stressRunning" />
                  </a-col>
                  <a-col :span="12">
                    <div style="font-size: 12px; margin-bottom: 4px; color: var(--ls-text-secondary)">
                      请求间隔：<b style="color: var(--ant-color-primary)">{{ stressInterval }} ms</b>
                    </div>
                    <a-slider v-model:value="stressInterval" :min="0" :max="500" :step="5" :disabled="stressRunning" />
                  </a-col>
                </a-row>
                
                <div style="margin-top: 12px; display: flex; gap: 10px">
                  <a-button type="primary" :danger="stressRunning" block :loading="stressRunning" @click="stressRunning ? stopStressTest() : runStressTest()">
                    <template #icon><ThunderboltOutlined /></template>
                    {{ stressRunning ? '压测排洪中 (点击停止)...' : '启动并发排洪测试' }}
                  </a-button>
                </div>
              </div>

              <!-- 压测大盘展示 -->
              <div v-if="stressTested" style="margin-top: 16px; border-top: 1px dashed rgba(255,255,255,0.06); padding-top: 16px">
                <a-row :gutter="16" style="margin-bottom: 16px">
                  <!-- 左侧 SVG 环形进度 -->
                  <a-col :xs="24" :md="10" style="display: flex; flex-direction: column; align-items: center; justify-content: center">
                    <div style="position: relative; width: 110px; height: 110px">
                      <!-- SVG 圆环 -->
                      <svg viewBox="0 0 100 100" style="width: 100%; height: 100%; transform: rotate(-90deg)">
                        <!-- 背景环 -->
                        <circle cx="50" cy="50" r="40" stroke="rgba(255,255,255,0.06)" stroke-width="8" fill="none" />
                        <!-- 成功进度环 -->
                        <circle
                          cx="50"
                          cy="50"
                          r="40"
                          stroke="var(--ant-color-success)"
                          stroke-width="8"
                          fill="none"
                          :stroke-dasharray="2 * Math.PI * 40"
                          :stroke-dashoffset="2 * Math.PI * 40 * (1 - successRate / 100)"
                          stroke-linecap="round"
                          style="transition: stroke-dashoffset 0.15s ease, stroke 0.3s"
                        />
                      </svg>
                      <!-- 中心文字 -->
                      <div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center">
                        <span style="font-size: 10px; color: var(--ls-text-secondary)">抢票成功率</span>
                        <span style="font-size: 1.25rem; font-weight: 800; font-family: 'Outfit'; color: var(--ant-color-success)">{{ successRate }}%</span>
                      </div>
                    </div>
                    <div style="font-size: 11px; color: var(--ls-text-secondary); margin-top: 8px; text-align: center">
                      已发请求: <b style="color: var(--ant-color-primary)">{{ stressResults.total }}</b> / {{ stressConcurrency }}
                    </div>
                  </a-col>

                  <!-- 4色指标统计 -->
                  <a-col :xs="24" :md="14" style="display: flex; flex-direction: column; justify-content: center">
                    <div style="display: flex; flex-direction: column; gap: 6px">
                      <!-- 成功 -->
                      <div class="stress-metric-card" style="border-left-color: var(--ant-color-success)">
                        <span style="font-size: 11px; color: var(--ls-text-secondary)">🟢 抢票成功：</span>
                        <span style="font-weight: 700; color: var(--ant-color-success); font-family: monospace">{{ stressResults.success }}</span>
                      </div>
                      <!-- 限流 -->
                      <div class="stress-metric-card" style="border-left-color: var(--ant-color-warning)">
                        <span style="font-size: 11px; color: var(--ls-text-secondary)">🟠 限流拦截 (429)：</span>
                        <span style="font-weight: 700; color: var(--ant-color-warning); font-family: monospace">{{ stressResults.rateLimit }}</span>
                      </div>
                      <!-- WAF -->
                      <div class="stress-metric-card" style="border-left-color: var(--ant-color-error)">
                        <span style="font-size: 11px; color: var(--ls-text-secondary)">🔴 WAF盾拦截：</span>
                        <span style="font-weight: 700; color: var(--ant-color-error); font-family: monospace">{{ stressResults.wafBlock }}</span>
                      </div>
                      <!-- 售罄 -->
                      <div class="stress-metric-card" style="border-left-color: rgba(255,255,255,0.3)">
                        <span style="font-size: 11px; color: var(--ls-text-secondary)">⚪ 本地售罄拦截：</span>
                        <span style="font-weight: 700; color: rgba(255,255,255,0.7); font-family: monospace">{{ stressResults.soldOut }}</span>
                      </div>
                    </div>
                  </a-col>
                </a-row>

                <!-- 终端日志流 -->
                <div style="margin-top: 12px">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px">
                    <span style="font-size: 12px; font-weight: 600; color: var(--ls-text-secondary)">💻 极客终端实时日志流:</span>
                    <a-button type="link" size="small" style="padding: 0; font-size: 11px" @click="stressLogs = []">清空终端</a-button>
                  </div>
                  <div
                    ref="stressLogContainer"
                    style="height: 180px; overflow-y: auto; background: #08090d; border: 1px solid rgba(255,255,255,0.06); border-radius: 8px; padding: 10px; font-family: monospace; font-size: 11px; color: #00ffcc; line-height: 1.5; text-align: left;"
                  >
                    <div v-if="stressLogs.length === 0" style="color: rgba(255,255,255,0.25); text-align: center; margin-top: 70px">
                      等待高并发测试启动...
                    </div>
                    <div v-for="(log, idx) in stressLogs" :key="idx" style="white-space: pre-wrap">
                      {{ log }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <!-- ========== 常用观演人管理弹窗 ========== -->
    <VisitorManagerModal v-model:open="showVisitorManager" @change="fetchVisitors" />
  </div>
</template>

<script setup lang="ts">
import {
  ArrowLeftOutlined, TagOutlined, TeamOutlined, ThunderboltOutlined,
  ScanOutlined, SafetyCertificateOutlined, LoadingOutlined,
  CalendarOutlined, EnvironmentOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import type { LiveEvent } from '@/types'
import VisitorManagerModal from './VisitorManagerModal.vue'
import { useTicketOrderCabin } from './useTicketOrderCabin'

const props = defineProps<{
  selectedEvent: LiveEvent | null
}>()

const emit = defineEmits<{
  backToSquare: []
  goToOrders: []
}>()

const {
  activeSkuId,
  ticketCount,
  activeSku,
  totalPrice,
  visitorList,
  showVisitorManager,
  fetchVisitors,
  toggleVisitor,
  onCountChange,
  grabStatus,
  grabProgress,
  pathToken,
  createdOrderNo,
  grabError,
  triggerGrab,
  resetGrab,
  showStressPanel,
  stressConcurrency,
  stressInterval,
  stressRunning,
  stressTested,
  stressLogContainer,
  stressResults,
  stressLogs,
  successRate,
  stopStressTest,
  runStressTest,
} = useTicketOrderCabin(props, emit)
</script>
