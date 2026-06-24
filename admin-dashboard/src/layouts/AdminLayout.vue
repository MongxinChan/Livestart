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
          <a-badge :count="3" :offset="[-4, 4]">
            <BellOutlined style="font-size: 18px; cursor: pointer" />
          </a-badge>

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

const router = useRouter()
const route = useRoute()

const collapsed = ref(false)
const selectedKeys = ref<string[]>([route.path])
const adminRealName = ref('系统管理员')

const currentTitle = computed(() => route.meta.title || '')

const currentUserType = computed(() => getAdminSession()?.userType)
const isSuper = computed(() => currentUserType.value === UserRole.SuperAdmin)
/** 内容子菜单：超管完整可见；场地管理员只看演出，仍展示分组 */
const canSeeContentSubmenu = computed(() => isSuper.value || currentUserType.value === UserRole.VenueAdmin)
/** 运营子菜单：场地管理员只能看到订单，超管完整 */
const canSeeOperationSubmenu = computed(() => isSuper.value || currentUserType.value === UserRole.VenueAdmin)

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
</script>
