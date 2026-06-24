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

async function ensureSelectedEvent() {
  const eventId = Number(route.params.id)
  if (!Number.isFinite(eventId) || eventId <= 0) return
  if (selectedEvent.value?.id === eventId) return

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
