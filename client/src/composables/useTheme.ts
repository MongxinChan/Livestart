import { computed, ref, watchEffect, type CSSProperties } from 'vue'
import { themeConfigs, themeOptions, customVars } from '../styles/themes'
import type { ThemeId } from '../types'

export function useTheme() {
  const activeTheme = ref<ThemeId>('cyberpunk-dark')

  const currentAntTheme = computed(() => ({
    ...themeConfigs[activeTheme.value],
    cssVar: true,
  }))

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

  watchEffect(() => {
    const vars = customVars[activeTheme.value]
    const root = document.documentElement

    // 1. 同步自定义变量到全局 root
    Object.entries(vars).forEach(([key, value]) => {
      root.style.setProperty(key, value)
    })

    // 2. 将核心的 Ant 变量也同步一份到全局 root，确保 teleport 到 body 的组件（如 Modal）能使用基础变量
    const token = themeConfigs[activeTheme.value].token || {}
    const antVars: Record<string, string | number | undefined> = {
      '--ant-color-primary': token.colorPrimary,
      '--ant-color-success': token.colorSuccess,
      '--ant-color-warning': token.colorWarning,
      '--ant-color-error': token.colorError,
      '--ant-color-bg-container': token.colorBgContainer,
      '--ant-color-bg-elevated': token.colorBgElevated,
      '--ant-color-bg-layout': token.colorBgLayout,
      '--ant-color-border-secondary': token.colorBorderSecondary,
    }

    Object.entries(antVars).forEach(([key, value]) => {
      if (value != null) {
        root.style.setProperty(key, String(value))
      } else {
        root.style.removeProperty(key) // 切换主题时如果该字段缺失，主动清理防止残留污染
      }
    })
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
