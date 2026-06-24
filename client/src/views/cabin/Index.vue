<template>
  <TicketOrderCabin
    :selected-event="selectedEvent"
    @back-to-square="goBackToSquare"
    @go-to-orders="goToOrders"
  />
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TicketOrderCabin from '@/components/TicketOrderCabin.vue'
import { fetchEventById } from '@/composables/event/useEventCatalog'
import { useEventAccess } from '@/composables/event/useEventAccess'

const route = useRoute()
const router = useRouter()
const { selectedEvent, setSelectedEvent, clearCabinEntry } = useEventAccess()

function normalizeRouteEventId(id: unknown) {
  if (id == null) return null
  const value = String(id).trim()
  return /^\d+$/.test(value) ? value : null
}

async function ensureSelectedEvent() {
  const eventId = normalizeRouteEventId(route.params.id)
  if (!eventId) return

  const event = await fetchEventById(eventId)
  setSelectedEvent(event)
}

function goBackToSquare() {
  clearCabinEntry()
  void router.push({ name: 'Square' })
}

function goToOrders() {
  clearCabinEntry()
  void router.push({ name: 'Orders' })
}

watch(() => route.params.id, () => {
  void ensureSelectedEvent()
}, { immediate: true })
</script>
