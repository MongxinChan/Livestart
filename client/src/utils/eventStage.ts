import type { LiveEvent } from '@/types'

export interface EventStageMeta {
  stageLabel: string
  stageColor: string
  statusText: string
  timeText: string
  canGrab: boolean
  hasStarted: boolean
}

function formatDateTime(input?: string): string {
  if (!input) return ''
  const date = new Date(input)
  if (Number.isNaN(date.getTime())) return input
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

export function resolveEventStageMeta(event: LiveEvent | null | undefined): EventStageMeta {
  if (!event) {
    return {
      stageLabel: '待定',
      stageColor: 'default',
      statusText: '暂无演出信息',
      timeText: '',
      canGrab: false,
      hasStarted: false,
    }
  }

  const stageLabel = event.ticketStage === 2 ? '二开' : '一开'
  const stageColor = event.ticketStage === 2 ? 'orange' : 'blue'
  const startAt = event.date ? new Date(event.date) : null
  const hasStarted = typeof event.started === 'boolean'
    ? event.started
    : !!startAt && !Number.isNaN(startAt.getTime()) && startAt.getTime() <= Date.now()
  const startTimeText = formatDateTime(event.date)
  const backendStatusText = event.statusText?.trim()

  if (hasStarted) {
    return {
      stageLabel,
      stageColor,
      statusText: backendStatusText || '演唱会已开演',
      timeText: startTimeText ? `开演时间：${startTimeText}` : '',
      canGrab: false,
      hasStarted: true,
    }
  }

  if (backendStatusText) {
    const canGrab = backendStatusText.includes('抢票中')
    return {
      stageLabel,
      stageColor,
      statusText: backendStatusText,
      timeText: startTimeText ? `开演时间：${startTimeText}` : '',
      canGrab,
      hasStarted: false,
    }
  }

  if (event.status === 2) {
    return {
      stageLabel,
      stageColor,
      statusText: `${stageLabel}抢票中`,
      timeText: startTimeText ? `开演时间：${startTimeText}` : '',
      canGrab: true,
      hasStarted: false,
    }
  }

  if (event.status === 1) {
    return {
      stageLabel,
      stageColor,
      statusText: `${stageLabel}待开售`,
      timeText: startTimeText ? `开演时间：${startTimeText}` : '',
      canGrab: false,
      hasStarted: false,
    }
  }

  if (event.status === 3) {
    return {
      stageLabel,
      stageColor,
      statusText: `${stageLabel}已售罄`,
      timeText: startTimeText ? `开演时间：${startTimeText}` : '',
      canGrab: false,
      hasStarted: false,
    }
  }

  return {
    stageLabel,
    stageColor,
    statusText: '暂未开售',
    timeText: startTimeText ? `开演时间：${startTimeText}` : '',
    canGrab: false,
    hasStarted: false,
  }
}
