import { theme as antTheme } from 'ant-design-vue'
import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'
import type { ThemeId, ThemeOption } from '@/types'

/** 可选主题列表 */
export const themeOptions: ThemeOption[] = [
  { id: 'cyberpunk-dark', name: '赛博暗黑', icon: '🌌' },
  { id: 'minimalist-light', name: '高雅极简', icon: '☀️' },
  { id: 'damai-crimson', name: '大麦炽红', icon: '🔴' },
  { id: 'showstart-neon', name: '秀动荧光', icon: '🟢' },
]

/**
 * 四套 Ant Design ConfigProvider 主题 Token
 * 每套主题 = Ant Design Algorithm + Token 覆写
 */
export const themeConfigs: Record<ThemeId, ThemeConfig> = {
  'cyberpunk-dark': {
    algorithm: antTheme.darkAlgorithm,
    token: {
      colorPrimary: '#ff2d55',
      colorSuccess: '#00ffcc',
      colorWarning: '#ffb703',
      colorError: '#ff3b30',
      colorBgContainer: '#12141c',
      colorBgElevated: '#1a1d28',
      colorBgLayout: '#0a0b0e',
      colorBorderSecondary: 'rgba(255, 45, 85, 0.15)',
      borderRadius: 12,
      fontFamily: "'Outfit', 'Inter', 'Noto Sans SC', sans-serif",
    },
  },
  'minimalist-light': {
    algorithm: antTheme.defaultAlgorithm,
    token: {
      colorPrimary: '#1a73e8',
      colorSuccess: '#10b981',
      colorWarning: '#f59e0b',
      colorError: '#ef4444',
      colorBgContainer: '#ffffff',
      colorBgElevated: '#ffffff',
      colorBgLayout: '#f5f7fb',
      colorBorderSecondary: 'rgba(26, 115, 232, 0.1)',
      borderRadius: 12,
      fontFamily: "'Outfit', 'Inter', 'Noto Sans SC', sans-serif",
    },
  },
  'damai-crimson': {
    algorithm: antTheme.darkAlgorithm,
    token: {
      colorPrimary: '#ff1268',
      colorSuccess: '#ffc000',
      colorWarning: '#ff6a00',
      colorError: '#ff3b30',
      colorBgContainer: '#141414',
      colorBgElevated: '#1e1e1e',
      colorBgLayout: '#0a0a0a',
      colorBorderSecondary: 'rgba(255, 18, 104, 0.2)',
      borderRadius: 12,
      fontFamily: "'Outfit', 'Inter', 'Noto Sans SC', sans-serif",
    },
  },
  'showstart-neon': {
    algorithm: antTheme.darkAlgorithm,
    token: {
      colorPrimary: '#20e3b2',
      colorSuccess: '#c026d3',
      colorWarning: '#eab308',
      colorError: '#ef4444',
      colorBgContainer: '#1a1b24',
      colorBgElevated: '#22232e',
      colorBgLayout: '#121319',
      colorBorderSecondary: 'rgba(32, 227, 178, 0.18)',
      borderRadius: 12,
      fontFamily: "'Outfit', 'Inter', 'Noto Sans SC', sans-serif",
    },
  },
}

/**
 * 自定义 CSS 变量映射（Ant Design 覆盖不到的效果）
 * 这些变量会注入到根元素 style 上
 */
export interface CustomThemeVars {
  '--ls-bg-primary': string
  '--ls-bg-card': string
  '--ls-glass-bg': string
  '--ls-glass-border': string
  '--ls-glass-shadow': string
  '--ls-neon-glow': string
  '--ls-logo-gradient': string
  '--ls-nav-bg': string
  '--ls-input-bg': string
  '--ls-text-secondary': string
  '--ls-accent-rgb': string
}

export const customVars: Record<ThemeId, CustomThemeVars> = {
  'cyberpunk-dark': {
    '--ls-bg-primary': '#0a0b0e',
    '--ls-bg-card': 'rgba(22, 25, 34, 0.7)',
    '--ls-glass-bg': 'rgba(18, 20, 28, 0.6)',
    '--ls-glass-border': 'rgba(255, 255, 255, 0.04)',
    '--ls-glass-shadow': '0 8px 32px 0 rgba(0, 0, 0, 0.55)',
    '--ls-neon-glow': '0 0 15px rgba(255, 45, 85, 0.25)',
    '--ls-logo-gradient': 'linear-gradient(135deg, #ff2d55 0%, #00ffcc 100%)',
    '--ls-nav-bg': 'rgba(10, 11, 14, 0.88)',
    '--ls-input-bg': 'rgba(10, 11, 14, 0.85)',
    '--ls-text-secondary': '#90a0c7',
    '--ls-accent-rgb': '255, 45, 85',
  },
  'minimalist-light': {
    '--ls-bg-primary': '#f5f7fb',
    '--ls-bg-card': 'rgba(255, 255, 255, 0.85)',
    '--ls-glass-bg': 'rgba(255, 255, 255, 0.8)',
    '--ls-glass-border': 'rgba(26, 115, 232, 0.08)',
    '--ls-glass-shadow': '0 8px 32px 0 rgba(31, 38, 135, 0.08)',
    '--ls-neon-glow': '0 4px 16px rgba(26, 115, 232, 0.15)',
    '--ls-logo-gradient': 'linear-gradient(135deg, #1a73e8 0%, #00d2ff 100%)',
    '--ls-nav-bg': 'rgba(255, 255, 255, 0.9)',
    '--ls-input-bg': 'rgba(255, 255, 255, 0.95)',
    '--ls-text-secondary': '#64748b',
    '--ls-accent-rgb': '26, 115, 232',
  },
  'damai-crimson': {
    '--ls-bg-primary': '#0a0a0a',
    '--ls-bg-card': 'rgba(30, 30, 30, 0.7)',
    '--ls-glass-bg': 'rgba(20, 20, 20, 0.65)',
    '--ls-glass-border': 'rgba(255, 255, 255, 0.03)',
    '--ls-glass-shadow': '0 8px 32px 0 rgba(0, 0, 0, 0.75)',
    '--ls-neon-glow': '0 0 18px rgba(255, 18, 104, 0.4)',
    '--ls-logo-gradient': 'linear-gradient(135deg, #ff1268 0%, #ff6a00 100%)',
    '--ls-nav-bg': 'rgba(8, 8, 8, 0.92)',
    '--ls-input-bg': 'rgba(8, 8, 8, 0.9)',
    '--ls-text-secondary': '#aaaaaa',
    '--ls-accent-rgb': '255, 18, 104',
  },
  'showstart-neon': {
    '--ls-bg-primary': '#121319',
    '--ls-bg-card': 'rgba(26, 28, 38, 0.65)',
    '--ls-glass-bg': 'rgba(22, 25, 34, 0.65)',
    '--ls-glass-border': 'rgba(255, 255, 255, 0.04)',
    '--ls-glass-shadow': '0 8px 32px 0 rgba(0, 0, 0, 0.65)',
    '--ls-neon-glow': '0 0 18px rgba(32, 227, 178, 0.35)',
    '--ls-logo-gradient': 'linear-gradient(135deg, #20e3b2 0%, #c026d3 100%)',
    '--ls-nav-bg': 'rgba(15, 16, 22, 0.9)',
    '--ls-input-bg': 'rgba(15, 16, 22, 0.85)',
    '--ls-text-secondary': '#8797a8',
    '--ls-accent-rgb': '32, 227, 178',
  },
}
