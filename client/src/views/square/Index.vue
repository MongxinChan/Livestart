<template>
  <EventSquare
    :nav-keyword="navKeyword"
    @select-event="handleSelectEvent"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EventSquare from '@/components/EventSquare.vue'
import { useEventAccess } from '@/composables/event/useEventAccess'
import type { LiveEvent } from '@/types'

const route = useRoute()
const router = useRouter()
const { setSelectedEvent } = useEventAccess()

const navKeyword = computed(() => String(route.query.keyword || ''))

function handleSelectEvent(event: LiveEvent) {
  setSelectedEvent(event)
  void router.push({ name: 'EventDetail', params: { id: event.id } })
}
</script>
