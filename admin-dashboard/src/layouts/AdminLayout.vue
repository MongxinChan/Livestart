<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      :trigger="null"
      theme="dark"
      :width="220"
    >
      <div class="admin-logo" @click="$router.push('/dashboard')">
        <ThunderboltOutlined class="admin-logo-icon" />
        <span class="admin-logo-text" :class="{ collapsed }">LIVESTART</span>
      </div>

      <a-menu v-model:selectedKeys="selectedKeys" theme="dark" mode="inline" @click="onMenuClick">
        <a-menu-item key="/dashboard">
          <template #icon><DashboardOutlined /></template>
          <span>数据看板</span>
        </a-menu-item>

        <a-sub-menu v-if="canSeeContentSubmenu" key="content">
          <template #icon><AppstoreOutlined /></template>
          <template #title>内容管理</template>
          <a-menu-item key="/event">演出管理</a-menu-item>
          <a-menu-item key="/ticket-sku">票档管理</a-menu-item>
          <a-menu-item v-if="isSuper" key="/venue">场馆管理</a-menu-item>
          <a-menu-item v-if="isSuper" key="/performer">艺人管理</a-menu-item>
          <a-menu-item v-if="isSuper" key="/style">风格管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu v-if="canSeeOperationSubmenu" key="operation">
          <template #icon><TeamOutlined /></template>
          <template #title>运营管理</template>
          <a-menu-item v-if="isSuper" key="/user">用户管理</a-menu-item>
          <a-menu-item key="/order">订单管理</a-menu-item>
        </a-sub-menu>

        <a-menu-item key="/settlement">
          <template #icon><FundOutlined /></template>
          <span>结算报表</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="admin-header">
        <div class="admin-header-left">
          <component
            :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined"
            style="font-size: 18px; cursor: pointer"
            @click="collapsed = !collapsed"
          />

          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/dashboard">首页</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="admin-header-right">
          <a-popover
            v-model:open="notificationOpen"
            placement="bottomRight"
            trigger="click"
            overlay-class-name="settlement-notification-popover"
            @openChange="handleNotificationOpenChange"
          >
            <template #content>
              <div class="notification-panel">
                <div class="notification-panel__header">
                  <div>
                    <div class="notification-panel__title">结算通知</div>
                    <div class="notification-panel__subtitle">异常优先展示，点击后自动标记为已读</div>
                  </div>
                  <a-button size="small" @click="refreshNotifications">刷新</a-button>
                </div>

                <a-spin :spinning="notificationLoading">
                  <a-empty
                    v-if="notifications.length === 0"
                    description="当前没有结算通知"
                  />

                  <div v-else class="notification-list">
                    <button
                      v-for="item in notifications"
                      :key="item.notificationKey"
                      type="button"
                      class="notification-item"
                      :class="{
                        'notification-item--unread': !item.read,
                        'notification-item--exception': item.type === 'exception',
                      }"
                      @click="openSettlementNotification(item)"
                    >
                      <div class="notification-item__meta">
                        <div class="notification-item__meta-left">
                          <span v-if="!item.read" class="notification-dot"></span>
                          <a-tag :color="tagColorOf(item.type)">
                            {{ item.typeLabel }}
                          </a-tag>
                        </div>
                        <span class="notification-item__time">{{ formatNotificationTime(item.updateTime) }}</span>
                      </div>
                      <div class="notification-item__title">{{ item.eventTitle }}</div>
                      <div class="notification-item__subtitle">
                        艺人：{{ item.performerName || '未绑定' }} · 演出序号：{{ item.eventId }}
                      </div>
                      <div class="notification-item__desc">{{ item.description }}</div>
                    </button>
                  </div>
                </a-spin>
              </div>
            </template>

            <a-badge :count="actionableCount" :offset="[-2, 6]">
              <div class="notification-trigger" :class="{ 'notification-trigger--active': actionableCount > 0 }">
                <BellOutlined class="notification-trigger__icon" />
              </div>
            </a-badge>
          </a-popover>

          <a-dropdown>
            <a-space style="cursor: pointer">
              <a-avatar :size="28" style="background: #1677ff">{{ adminRealName.charAt(0) }}</a-avatar>
              <span style="font-weight: 500">{{ adminRealName }}</span>
              <DownOutlined />
            </a-space>

            <template #overlay>
              <a-menu @click="handleMenuClick">
                <a-menu-item key="profile">个人设置</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content style="margin: 0">
        <div class="page-container">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>

        <div class="admin-footer">
          Livestart Admin Dashboard &copy; 2026 陈孟欣 · Powered by Vue 3 + Ant Design Vue
        </div>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  AppstoreOutlined,
  BellOutlined,
  DashboardOutlined,
  DownOutlined,
  FundOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  TeamOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import { clearAdminSession, getAdminSession, UserRole } from '@/api/http'
import { useSettlementNotifications } from '@/composables/useSettlementNotifications'
import type { SettlementNotificationItem } from '@/types'

const router = useRouter()
const route = useRoute()

const collapsed = ref(false)
const selectedKeys = ref<string[]>([route.path])
const adminRealName = ref('系统管理员')
const notificationOpen = ref(false)

const currentTitle = computed(() => route.meta.title || '')
const currentUserType = computed(() => getAdminSession()?.userType)
const isSuper = computed(() => currentUserType.value === UserRole.SuperAdmin)
const canSeeContentSubmenu = computed(() => isSuper.value || currentUserType.value === UserRole.VenueAdmin)
const canSeeOperationSubmenu = computed(() => isSuper.value || currentUserType.value === UserRole.VenueAdmin)

const {
  loading: notificationLoading,
  notifications,
  actionableCount,
  fetchNotifications,
  markAsRead,
} = useSettlementNotifications()

watch(
  () => route.path,
  (path) => {
    selectedKeys.value = [path]
  }
)

onMounted(() => {
  const adminUser = getAdminSession()
  if (adminUser?.realName) {
    adminRealName.value = adminUser.realName
  }

  fetchNotifications().catch(() => {
    // 由 http 拦截器统一提示
  })
})

function handleMenuClick({ key }: { key: string }) {
  if (key === 'logout') {
    Modal.confirm({
      title: '安全退出',
      content: '确定要退出当前管理后台登录吗？',
      okText: '确定',
      cancelText: '取消',
      onOk() {
        clearAdminSession('manual-logout')
        message.success('已安全退出登录')
        router.push('/login')
      },
    })
    return
  }

  if (key === 'profile') {
    message.info('个人设置功能建设中')
  }
}

function onMenuClick({ key }: { key: string }) {
  router.push(key)
}

function handleNotificationOpenChange(open: boolean) {
  notificationOpen.value = open
  if (open) {
    refreshNotifications()
  }
}

function refreshNotifications() {
  fetchNotifications(true).catch(() => {
    // 由 http 拦截器统一提示
  })
}

async function openSettlementNotification(item: SettlementNotificationItem) {
  if (!item.read) {
    try {
      await markAsRead(item.notificationKey)
    } catch {
      // 已由 http 拦截器提示
    }
  }
  notificationOpen.value = false
  router.push({ path: '/settlement', query: { eventId: String(item.eventId) } })
}

function formatNotificationTime(value?: string) {
  if (!value) return '未知时间'
  const time = dayjs(value)
  if (!time.isValid()) return value
  return time.format('MM-DD HH:mm')
}

function tagColorOf(type: SettlementNotificationItem['type']) {
  if (type === 'exception') return 'red'
  if (type === 'pending') return 'orange'
  return 'blue'
}
</script>

<style scoped>
.notification-panel {
  width: 380px;
}

.notification-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1px solid #d9d9d9;
  border-radius: 999px;
  background: linear-gradient(180deg, #ffffff 0%, #f5f5f5 100%);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.notification-trigger:hover {
  border-color: #91caff;
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.16);
}

.notification-trigger--active {
  border-color: #ffd666;
  background: linear-gradient(180deg, #fff7e6 0%, #ffe7ba 100%);
  box-shadow: 0 10px 24px rgba(250, 173, 20, 0.22);
}

.notification-trigger__icon {
  color: #d48806;
  font-size: 18px;
}

.notification-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.notification-panel__title {
  color: #1f1f1f;
  font-size: 15px;
  font-weight: 600;
}

.notification-panel__subtitle {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}

.notification-list {
  display: flex;
  max-height: 440px;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
}

.notification-item {
  width: 100%;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #fafafa 100%);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.notification-item:hover {
  border-color: #91caff;
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.10);
}

.notification-item--unread {
  border-color: #adc6ff;
  box-shadow: inset 0 0 0 1px rgba(22, 119, 255, 0.12);
}

.notification-item--exception {
  background: linear-gradient(180deg, #fff2f0 0%, #fffaf8 100%);
  border-color: #ffccc7;
}

.notification-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notification-item__meta-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1677ff;
  flex: 0 0 auto;
}

.notification-item__time {
  color: #8c8c8c;
  font-size: 12px;
}

.notification-item__title {
  margin-top: 8px;
  color: #1f1f1f;
  font-weight: 600;
  line-height: 1.5;
}

.notification-item__subtitle,
.notification-item__desc {
  margin-top: 6px;
  color: #595959;
  font-size: 12px;
  line-height: 1.6;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 20px;
  color: #fff;
  cursor: pointer;
}

.admin-logo-icon {
  font-size: 24px;
  color: #ffd666;
}

.admin-logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  opacity: 1;
  transition: opacity 0.2s ease;
}

.admin-logo-text.collapsed {
  opacity: 0;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.admin-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.page-container {
  min-height: calc(100vh - 64px - 53px);
  padding: 24px;
  background: #f5f7fa;
}

.admin-footer {
  padding: 16px 24px;
  color: #8c8c8c;
  font-size: 13px;
  text-align: center;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
</style>
