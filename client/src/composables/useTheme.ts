import { computed, ref, type CSSProperties } from 'vue'
import { themeConfigs, themeOptions, customVars } from '../styles/themes'
import type { ThemeId } from '../types'

export function useTheme() {
  const activeTheme = ref<ThemeId>('cyberpunk-dark')

  const currentAntTheme = computed(() => themeConfigs[activeTheme.value])

  const currentThemeLabel = computed(() => {
    const selected = themeOptions.find((item) => item.id === activeTheme.value)
    return selected ? `${selected.icon} ${selected.name}` : '主题'
  })

  const rootStyle = computed<CSSProperties>(() => {
    const vars = customVars[activeTheme.value]
    const style: Record<string, string> = {
      minHeight: '100vh',
      transition: 'all 0.3s ease',
    }
    for (const [key, value] of Object.entries(vars)) {
      style[key] = value
    }
    return style as CSSProperties
  })

  function onThemeChange(info: { key: string | number }) {
    activeTheme.value = info.key as ThemeId
  }

  return {
    activeTheme,
    currentAntTheme,
    currentThemeLabel,
    themeOptions,
    rootStyle,
    onThemeChange,
  }
}
