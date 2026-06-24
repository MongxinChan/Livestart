import { onMounted } from 'vue'
import { useTheme } from '@/composables/app/useTheme'
import { useNav } from '@/composables/app/useNav'
import { useAuth } from '@/composables/app/useAuth'

export function useApp() {
  onMounted(() => {
    const skeleton = document.querySelector('.app-skeleton') as HTMLElement | null
    if (!skeleton) return

    window.setTimeout(() => {
      skeleton.style.opacity = '0'
      window.setTimeout(() => skeleton.remove(), 300)
    }, 400)
  })

  return {
    ...useTheme(),
    ...useNav(),
    ...useAuth(),
  }
}
