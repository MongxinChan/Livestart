<template>
  <a-config-provider :theme="currentAntTheme">
    <a-layout :style="rootStyle" class="ls-app-root">
      <!-- 顶部导航栏 -->
      <a-layout-header class="ls-header">
        <div class="ls-header-inner">
          <!-- Logo -->
          <div class="ls-logo" @click="navigateTo('square')">
            <ThunderboltOutlined class="ls-logo-icon" />
            <span class="text-gradient ls-logo-text">LIVESTART</span>
          </div>

          <!-- 视图导航 -->
          <a-segmented
            v-model:value="activeView"
            :options="navOptions"
            size="large"
            @change="onNavChange"
          />

          <!-- 右侧控制 -->
          <a-space :size="12">
            <!-- Mock/网关开关 -->
            <a-space :size="6" align="center">
              <a-badge :status="apiState.isMock ? 'default' : 'success'" />
              <span style="font-size: 12px; color: var(--ls-text-secondary)">
                {{ apiState.isMock ? '离线 Mock' : '网关联调' }}
              </span>
              <a-switch
                v-model:checked="apiState.isMock"
                size="small"
                checked-children="Mock"
                un-checked-children="Real"
              />
            </a-space>

            <!-- 主题选择 -->
            <a-dropdown :trigger="['click']">
              <a-button>
                <template #icon><BgColorsOutlined /></template>
                {{ currentThemeLabel }}
                <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="onThemeChange">
                  <a-menu-item v-for="t in themeOptions" :key="t.id">
                    <a-space>
                      <span>{{ t.icon }}</span>
                      <span>{{ t.name }}</span>
                      <CheckOutlined v-if="activeTheme === t.id" style="color: var(--ant-color-primary)" />
                    </a-space>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>

            <!-- 用户 -->
            <a-space :size="8" align="center">
              <a-avatar :size="30" style="background: var(--ls-logo-gradient, #1890ff)">陈</a-avatar>
              <span style="font-size: 13px; font-weight: 600">陈孟欣</span>
            </a-space>
          </a-space>
        </div>
      </a-layout-header>

      <!-- 主内容 -->
      <a-layout-content class="ls-content">
        <EventSquare
          v-if="activeView === 'square'"
          @select-event="selectEventForCabin"
        />
        <TicketOrderCabin
          v-else-if="activeView === 'cabin'"
          :selected-event="selectedEvent"
          @back-to-square="navigateTo('square')"
        />
        <TicketOrderCabin
          v-else-if="activeView === 'orders'"
          :selected-event="null"
          @back-to-square="navigateTo('square')"
        />
        <MerchantSettlement v-else-if="activeView === 'settlement'" />
      </a-layout-content>
    </a-layout>
  </a-config-provider>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type CSSProperties } from 'vue'
import {
  ThunderboltOutlined,
  BgColorsOutlined,
  DownOutlined,
  CheckOutlined,
  SearchOutlined,
  ShoppingOutlined,
  FundOutlined,
} from '@ant-design/icons-vue'
import { themeConfigs, themeOptions, customVars } from './styles/themes'
import { apiState } from './composables/useRequest'
import type { ThemeId, LiveEvent } from './types'
import EventSquare from './components/EventSquare.vue'
import TicketOrderCabin from './components/TicketOrderCabin.vue'
import MerchantSettlement from './components/MerchantSettlement.vue'

// --- 主题 ---
const activeTheme = ref<ThemeId>('cyberpunk-dark')

const currentAntTheme = computed(() => themeConfigs[activeTheme.value])
const currentThemeLabel = computed(() => {
  const t = themeOptions.find(x => x.id === activeTheme.value)
  return t ? `${t.icon} ${t.name}` : '主题'
})

const rootStyle = computed<CSSProperties>(() => {
  const vars = customVars[activeTheme.value]
  const style: Record<string, string> = {
    minHeight: '100vh',
    transition: 'all 0.3s ease',
  }
  for (const [k, v] of Object.entries(vars)) {
    style[k] = v
  }
  return style as CSSProperties
})

function onThemeChange(info: { key: string | number }) {
  activeTheme.value = info.key as ThemeId
}

// --- 导航 ---
type ViewId = 'square' | 'cabin' | 'orders' | 'settlement'
const activeView = ref<string>('square')
const selectedEvent = ref<LiveEvent | null>(null)

const navOptions = [
  { value: 'square', label: '演出发现', icon: SearchOutlined },
  { value: 'orders', label: '电子票包', icon: ShoppingOutlined },
  { value: 'settlement', label: '商户核算', icon: FundOutlined },
]

function onNavChange(val: string | number) {
  activeView.value = val as string
  if (val === 'square') selectedEvent.value = null
}

function navigateTo(view: ViewId) {
  activeView.value = view
  if (view === 'square') selectedEvent.value = null
}

function selectEventForCabin(event: LiveEvent) {
  selectedEvent.value = event
  activeView.value = 'cabin'
}

// --- 骨架屏移除 ---
onMounted(() => {
  const skeleton = document.querySelector('.app-skeleton') as HTMLElement | null
  if (skeleton) {
    setTimeout(() => {
      skeleton.style.opacity = '0'
      setTimeout(() => skeleton.remove(), 300)
    }, 400)
  }
})
</script>

<style scoped>
.ls-header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
}
.ls-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.ls-logo-icon {
  font-size: 24px;
  animation: heartbeat 2s infinite ease-in-out;
}
.ls-logo-text {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.1rem;
  font-family: 'Outfit', sans-serif;
}
.ls-content {
  max-width: 1300px;
  width: 100%;
  margin: 28px auto;
  padding: 0 20px;
}
</style>
