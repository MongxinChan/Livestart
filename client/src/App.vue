<template>
  <a-config-provider :theme="currentAntTheme">
    <a-layout :style="rootStyle" class="ls-app-root">
      <Navbar
        :active-view="activeView"
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
        <Transition name="ls-fade" mode="out-in">
          <EventSquare
            v-if="activeView === 'square'"
            :nav-keyword="navSearchQuery"
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
          <MyReminders
            v-else-if="activeView === 'reminders'"
            @back-to-square="navigateTo('square')"
          />
        </Transition>
      </a-layout-content>

      <AuthModal />
      <VisitorManagerModal v-model:open="showVisitorModal" />
    </a-layout>
  </a-config-provider>
</template>

<script setup lang="ts">
import { apiState } from './composables/useRequest'
import { useApp } from './composables/useApp'
import Navbar from './components/Navbar.vue'
import EventSquare from './components/EventSquare.vue'
import TicketOrderCabin from './components/TicketOrderCabin.vue'
import MyTickets from './components/MyTickets.vue'
import MyReminders from './components/MyReminders.vue'
import AuthModal from './components/AuthModal.vue'
import VisitorManagerModal from './components/VisitorManagerModal.vue'

const {
  activeTheme,
  currentAntTheme,
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
  handleLogout,
  isScrolled,
  navSearchVisible,
  navSearchQuery,
  navSuggest,
  toggleNavSearch,
  onNavSearchInput,
} = useApp()

function onNavSearchSelect(val: string) {
  navSearchQuery.value = val
  navigateTo('square')
}

function onNavSearchBlur() {
  setTimeout(() => {
    if (!navSearchQuery.value) {
      navSearchVisible.value = false
    }
  }, 200)
}
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
