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
            <a-space v-if="isMockEnv" :size="6" align="center">
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
              <template v-if="apiState.currentUser">
                <a-dropdown :trigger="['click']">
                  <a-space style="cursor: pointer">
                    <a-avatar :size="30" style="background: var(--ls-logo-gradient, #1890ff)">
                      {{ (apiState.currentUser.realName || apiState.currentUser.username || 'U').substring(0, 1) }}
                    </a-avatar>
                    <span style="font-size: 13px; font-weight: 600">{{ apiState.currentUser.realName || apiState.currentUser.username }}</span>
                  </a-space>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item key="visitor" @click="showVisitorModal = true">
                        <template #icon><TeamOutlined /></template>
                        常用观演人
                      </a-menu-item>
                      <a-menu-item key="logout" @click="handleLogout">
                        <template #icon><LogoutOutlined /></template>
                        退出登录
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </template>
              <template v-else>
                <a-button type="primary" size="small" ghost @click="showAuthModal = true">
                  登录 / 注册
                </a-button>
              </template>
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
          @go-to-orders="navigateTo('orders')"
        />
        <MyTickets
          v-else-if="activeView === 'orders'"
          @back-to-square="navigateTo('square')"
        />
      </a-layout-content>

      <!-- ========== 登录/注册快捷通道模态框 ========== -->
      <a-modal v-model:open="showAuthModal" :footer="null" :width="380" centered destroy-on-close>
        <template #title>
          <span style="font-weight: 800; font-size: 1.1rem">
            <UserOutlined style="margin-right: 8px; color: var(--ant-color-primary)" />
            快捷验证登录与自动注册
          </span>
        </template>

        <a-typography-paragraph type="secondary" style="font-size: 12px; margin-bottom: 18px">
          已支持手机验证码快捷登录。未注册手机号将自动在后台进行隐式自动注册及社交资料建档。
        </a-typography-paragraph>

        <a-form layout="vertical" @finish="handleAuthSubmit">
          <a-form-item label="手机号" required>
            <a-input v-model:value="authForm.phone" placeholder="请输入11位登录手机号" size="large" maxlength="11">
              <template #prefix><MobileOutlined style="color: rgba(255,255,255,0.25)" /></template>
            </a-input>
          </a-form-item>

          <a-form-item label="短信验证码" required>
            <a-space style="width: 100%">
              <a-input v-model:value="authForm.code" placeholder="请输入6位验证码" size="large" maxlength="6" style="flex: 1">
                <template #prefix><SafetyOutlined style="color: rgba(255,255,255,0.25)" /></template>
              </a-input>
              <a-button size="large" :disabled="countdown > 0" @click="sendVerificationCode" style="min-width: 120px">
                {{ countdown > 0 ? `${countdown}s 重试` : '获取验证码' }}
              </a-button>
            </a-space>
          </a-form-item>

          <a-button type="primary" block size="large" html-type="submit" :loading="authLoading" style="margin-top: 10px">
            立即登录 / 自动注册
          </a-button>
        </a-form>
      </a-modal>

      <!-- ========== 常用观演人管理模态框 ========== -->
      <VisitorManagerModal v-model:open="showVisitorModal" />
    </a-layout>
  </a-config-provider>
</template>

<script setup lang="ts">
import {
  ThunderboltOutlined,
  BgColorsOutlined,
  DownOutlined,
  CheckOutlined,
  UserOutlined,
  MobileOutlined,
  SafetyOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue'
import { apiState } from './composables/useRequest'
import { useApp } from './composables/useApp'
import EventSquare from './components/EventSquare.vue'
import TicketOrderCabin from './components/TicketOrderCabin.vue'
import MyTickets from './components/MyTickets.vue'
import VisitorManagerModal from './components/VisitorManagerModal.vue'

const isMockEnv = import.meta.env.VITE_USE_MOCK === 'true'

const {
  activeTheme,
  currentAntTheme,
  currentThemeLabel,
  themeOptions,
  rootStyle,
  onThemeChange,
  activeView,
  selectedEvent,
  navOptions,
  onNavChange,
  navigateTo,
  selectEventForCabin,
  showAuthModal,
  showVisitorModal,
  authLoading,
  countdown,
  authForm,
  sendVerificationCode,
  handleAuthSubmit,
  handleLogout,
} = useApp()
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
