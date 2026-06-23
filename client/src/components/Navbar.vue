<template>
  <header :class="['ls-header', { 'ls-header--scrolled': isScrolled }]">
    <div class="ls-header-inner">

      <!-- Logo -->
      <div class="ls-logo" @click="$emit('clickLogo')">
        <ThunderboltOutlined class="ls-logo-icon" />
        <span class="text-gradient ls-logo-text">LIVESTART</span>
      </div>

      <!-- 主导航 Tab -->
      <nav class="ls-nav">
        <button
          v-for="item in navOptions"
          :key="item.value"
          :class="['ls-nav-tab', { active: activeView === item.value }]"
          @click="$emit('navChange', item.value)"
        >
          <component :is="item.icon" class="ls-nav-tab-icon" />
          <span class="ls-nav-tab-label">{{ item.label }}</span>
          <span class="ls-nav-tab-underline" />
        </button>
      </nav>

      <!-- 右侧控制区 -->
      <div class="ls-header-right">

        <!-- Mock/网关开关 -->
        <div v-if="isMockEnv" class="ls-mock-toggle">
          <a-badge :status="apiState.isMock ? 'default' : 'success'" />
          <span class="ls-mock-label">{{ apiState.isMock ? '离线 Mock' : '网关联调' }}</span>
          <a-switch
            v-model:checked="apiState.isMock"
            size="small"
            checked-children="Mock"
            un-checked-children="Real"
          />
        </div>

        <!-- 搜索框（收缩/展开） -->
        <div :class="['ls-search-wrap', { expanded: navSearchVisible }]">
          <Transition name="ls-search">
            <div v-if="navSearchVisible" class="ls-search-box">
              <a-auto-complete
                :value="navSearchQuery"
                :options="navSuggest.map(s => ({ value: s }))"
                placeholder="搜索演出、艺人..."
                class="ls-search-input"
                :open="navSuggest.length > 0"
                @search="$emit('searchInput', $event)"
                @select="$emit('searchSelect', $event)"
                @blur="onSearchBlur"
              >
                <template #prefix><SearchOutlined /></template>
              </a-auto-complete>
            </div>
          </Transition>
          <a-tooltip title="搜索演出" placement="bottom">
            <button class="ls-icon-btn" @click="$emit('toggleSearch')">
              <SearchOutlined v-if="!navSearchVisible" />
              <CloseOutlined v-else />
            </button>
          </a-tooltip>
        </div>

        <!-- 主题切换（纯图标） -->
        <a-tooltip title="切换主题" placement="bottom">
          <a-dropdown :trigger="['click']">
            <button class="ls-icon-btn">
              <BgColorsOutlined />
            </button>
            <template #overlay>
              <a-menu @click="$emit('themeChange', $event)">
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
        </a-tooltip>

        <!-- 用户区域 -->
        <template v-if="currentUser">
          <a-dropdown :trigger="['click']">
            <div class="ls-user-trigger">
              <a-avatar :size="30" style="background: var(--ls-logo-gradient, #1890ff); flex-shrink: 0">
                {{ (currentUser.realName || currentUser.username || 'U').substring(0, 1) }}
              </a-avatar>
              <span class="ls-user-name">{{ currentUser.realName || currentUser.username }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="visitor" @click="$emit('openVisitorModal')">
                  <template #icon><TeamOutlined /></template>
                  常用观演人
                </a-menu-item>
                <a-menu-item key="logout" @click="$emit('logout')">
                  <template #icon><LogoutOutlined /></template>
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <template v-else>
          <a-button type="primary" size="small" ghost class="ls-login-btn" @click="$emit('openAuthModal')">
            登录 / 注册
          </a-button>
        </template>

      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import {
  ThunderboltOutlined,
  BgColorsOutlined,
  CheckOutlined,
  TeamOutlined,
  LogoutOutlined,
  SearchOutlined,
  CloseOutlined,
} from '@ant-design/icons-vue'
import { apiState } from '../composables/useRequest'

const isMockEnv = import.meta.env.VITE_USE_MOCK === 'true'

const props = defineProps<{
  activeView: string
  navOptions: Array<{ value: string; label: string; icon: any }>
  isScrolled: boolean
  navSearchVisible: boolean
  navSearchQuery: string
  navSuggest: Array<string>
  activeTheme: string
  themeOptions: Array<{ id: string; name: string; icon: string }>
  currentUser: any
}>()

const emit = defineEmits<{
  (e: 'clickLogo'): void
  (e: 'navChange', view: string): void
  (e: 'toggleSearch'): void
  (e: 'searchInput', keyword: string): void
  (e: 'searchSelect', keyword: string): void
  (e: 'themeChange', themeEvent: any): void
  (e: 'openVisitorModal'): void
  (e: 'openAuthModal'): void
  (e: 'logout'): void
  (e: 'searchBlur'): void
}>()

function onSearchBlur() {
  setTimeout(() => {
    emit('searchBlur')
  }, 200)
}
</script>

<style scoped>
/* ---- 导航栏容器 ---- */
.ls-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
  padding: 0 32px;
  background: var(--ls-nav-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--ls-glass-border);
  box-shadow: var(--ls-glass-shadow);
  transition: background 0.3s ease, backdrop-filter 0.3s ease, box-shadow 0.3s ease;
  width: 100%;
}

/* Scroll 后加深效果 */
.ls-header--scrolled {
  background: color-mix(in srgb, var(--ls-nav-bg) 95%, transparent);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.4);
  border-bottom-color: rgba(var(--ls-accent-rgb), 0.15);
}

.ls-header-inner {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

/* ---- Logo ---- */
.ls-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
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

/* ---- 主导航 Tab ---- */
.ls-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  justify-content: center;
}

.ls-nav-tab {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 18px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--ls-text-secondary);
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  border-radius: 8px;
  transition: color 0.2s ease, background 0.2s ease, transform 0.15s ease;
}

.ls-nav-tab:hover {
  color: var(--ls-text-primary, #fff);
  background: rgba(var(--ls-accent-rgb), 0.08);
  transform: translateY(-1px);
}

.ls-nav-tab.active {
  color: var(--ant-color-primary);
}

.ls-nav-tab-icon {
  font-size: 15px;
  transition: transform 0.2s ease;
}
.ls-nav-tab:hover .ls-nav-tab-icon {
  transform: scale(1.15);
}

.ls-nav-tab-label {
  white-space: nowrap;
}

/* 激活态下划线 */
.ls-nav-tab-underline {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 60%;
  height: 2px;
  background: var(--ant-color-primary);
  border-radius: 2px;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 0 8px rgba(var(--ls-accent-rgb), 0.5);
}
.ls-nav-tab.active .ls-nav-tab-underline {
  transform: translateX(-50%) scaleX(1);
}

/* ---- 右侧控制区 ---- */
.ls-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* Mock 开关 */
.ls-mock-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ls-text-secondary);
  padding: 4px 10px;
  border-radius: 20px;
  background: rgba(var(--ls-accent-rgb), 0.05);
  border: 1px solid rgba(var(--ls-accent-rgb), 0.1);
}
.ls-mock-label {
  white-space: nowrap;
}

/* 通用图标按钮 */
.ls-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(var(--ls-accent-rgb), 0.06);
  border-radius: 10px;
  cursor: pointer;
  color: var(--ls-text-secondary);
  font-size: 16px;
  transition: background 0.2s ease, color 0.2s ease, transform 0.15s ease;
  flex-shrink: 0;
}
.ls-icon-btn:hover {
  background: rgba(var(--ls-accent-rgb), 0.15);
  color: var(--ant-color-primary);
  transform: scale(1.08);
}

/* ---- 搜索框 ---- */
.ls-search-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ls-search-box {
  overflow: hidden;
}

.ls-search-input {
  width: 220px;
}

/* 搜索框展开动画 */
.ls-search-enter-active,
.ls-search-leave-active {
  transition: width 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.2s ease;
}
.ls-search-enter-from,
.ls-search-leave-to {
  width: 0 !important;
  opacity: 0;
}
.ls-search-enter-to {
  width: 220px;
  opacity: 1;
}

/* ---- 用户区域 ---- */
.ls-user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px 4px 4px;
  border-radius: 20px;
  border: 1px solid transparent;
  transition: background 0.2s, border-color 0.2s;
}
.ls-user-trigger:hover {
  background: rgba(var(--ls-accent-rgb), 0.08);
  border-color: rgba(var(--ls-accent-rgb), 0.15);
}
.ls-user-name {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ls-login-btn {
  white-space: nowrap;
}

/* ---- 响应式 ---- */
@media (max-width: 1024px) {
  .ls-logo-text {
    font-size: 18px;
    letter-spacing: 0.05rem;
  }
  .ls-mock-label {
    display: none;
  }
  .ls-user-name {
    max-width: 60px;
  }
}

@media (max-width: 768px) {
  .ls-header {
    padding: 0 16px;
  }
  .ls-logo-text {
    display: none;
  }
  .ls-search-wrap {
    display: none;
  }
  .ls-mock-toggle {
    display: none;
  }
  .ls-header-inner {
    gap: 12px;
  }
  .ls-nav-tab-label {
    display: none;
  }
  .ls-nav-tab {
    padding: 6px 12px;
  }
}
</style>
