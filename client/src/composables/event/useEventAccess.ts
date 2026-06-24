import { computed, ref } from 'vue'
import type { LiveEvent } from '@/types'

const selectedEvent = ref<LiveEvent | null>(null)
const cabinEntryEventId = ref<string | null>(null)

function normalizeEventId(eventId: number | string | null | undefined) {
  if (eventId == null) return null
  const value = String(eventId).trim()
  return /^\d+$/.test(value) ? value : null
}

export function useEventAccess() {
  const selectedEventId = computed(() => normalizeEventId(selectedEvent.value?.id) ?? null)

  function setSelectedEvent(event: LiveEvent | null) {
    selectedEvent.value = event
  }

  function allowCabinEntry(event: LiveEvent) {
    selectedEvent.value = event
    cabinEntryEventId.value = normalizeEventId(event.id)
  }

  function clearCabinEntry() {
    cabinEntryEventId.value = null
  }

  function canAccessCabin(eventId: number | string) {
    const normalizedTargetId = normalizeEventId(eventId)
    return cabinEntryEventId.value != null && normalizedTargetId != null && cabinEntryEventId.value === normalizedTargetId
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
