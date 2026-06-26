<template>
  <a-config-provider :theme="currentAntTheme" :get-popup-container="getPopupContainer">
    <a-layout :style="rootStyle" class="ls-app-root">
      <Navbar
        :active-view="activeNavKey"
        :nav-options="navOptions"
        :is-scrolled="isScrolled"
        :nav-search-visible="navSearchVisible"
        :nav-search-query="navSearchQuery"
        :nav-suggest="navSuggest"
        :active-theme="activeTheme"
        :theme-options="themeOptions"
        :current-user="apiState.currentUser"
        @click-logo="navigateTo('square')"
        @nav-change="onNavChange"
        @toggle-search="toggleNavSearch"
        @search-input="onNavSearchInput"
        @search-select="onNavSearchSelect"
        @search-blur="onNavSearchBlur"
        @theme-change="onThemeChange"
        @open-visitor-modal="showVisitorModal = true"
        @open-auth-modal="showAuthModal = true"
        @logout="handleLogout"
      />

      <a-layout-content class="ls-content">
        <router-view v-slot="{ Component }">
          <Transition name="ls-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </a-layout-content>

      <AuthModal />
      <VisitorManagerModal v-model:open="showVisitorModal" />
    </a-layout>
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiState } from '@/composables/infra/useRequest'
import { useApp } from '@/composables/app/useApp'
import Navbar from '@/components/Navbar.vue'
import AuthModal from '@/components/AuthModal.vue'
import VisitorManagerModal from '@/components/VisitorManagerModal.vue'

const route = useRoute()
const router = useRouter()

const {
  activeTheme,
  currentAntTheme,
  themeOptions,
  rootStyle,
  onThemeChange,
  navOptions,
  onNavChange,
  showAuthModal,
  showVisitorModal,
  handleLogout,
  isScrolled,
  navSearchVisible,
  navSearchQuery,
  navSuggest,
  toggleNavSearch,
  onNavSearchInput,
} = useApp()

const activeNavKey = computed(() => String(route.meta.navKey || 'square'))

function navigateTo(view: string) {
  onNavChange(view)
}

function onNavSearchSelect(val: string) {
  navSearchQuery.value = val
  void router.push({ name: 'Square', query: { keyword: val } })
}

function onNavSearchBlur() {
  if (!navSearchQuery.value) {
    navSearchVisible.value = false
  }
}

function getPopupContainer() {
  return document.querySelector('.ls-app-root') as HTMLElement || document.body
}

watch(
  () => route.query.auth,
  (auth) => {
    if (auth === '1' && !apiState.token) {
      showAuthModal.value = true
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.ls-content {
  max-width: 1300px;
  width: 100%;
  margin: 28px auto;
  padding: 0 20px;
}

.ls-fade-enter-active,
.ls-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.ls-fade-enter-from,
.ls-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
