import { computed, ref } from 'vue'
import type { LiveEvent } from '@/types'

const selectedEvent = ref<LiveEvent | null>(null)
const cabinEntryEventId = ref<number | null>(null)

export function useEventAccess() {
  const selectedEventId = computed(() => selectedEvent.value?.id ?? null)

  function setSelectedEvent(event: LiveEvent | null) {
    selectedEvent.value = event
  }

  function allowCabinEntry(event: LiveEvent) {
    selectedEvent.value = event
    cabinEntryEventId.value = event.id
  }

  function clearCabinEntry() {
    cabinEntryEventId.value = null
  }

  function canAccessCabin(eventId: number) {
    return cabinEntryEventId.value === eventId
  }

  return {
    selectedEvent,
    selectedEventId,
    cabinEntryEventId,
    setSelectedEvent,
    allowCabinEntry,
    clearCabinEntry,
    canAccessCabin,
  }
}
