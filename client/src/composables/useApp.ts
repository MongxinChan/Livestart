import { onMounted } from 'vue'
import { useTheme } from './useTheme'
import { useNav } from './useNav'
import { useAuth } from './useAuth'

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
