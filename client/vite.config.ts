import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    open: true,
    proxy: {
      // 网关模式：统一通过 Gateway 8888 转发
      // 直连模式：按微服务端口分别转发（开发阶段绕过网关）

      // 购票引擎 → 端口 8004
      '/api/engine': {
        target: 'http://127.0.0.1:8004',
        changeOrigin: true,
      },
      // 搜索服务 → 端口 8006
      '/api/search': {
        target: 'http://127.0.0.1:8006',
        changeOrigin: true,
      },
      // 结算服务 → 端口 8007
      '/api/settlement': {
        target: 'http://127.0.0.1:8007',
        changeOrigin: true,
      },
      // 商户后台管理 → 端口 8003
      '/api/merchant-admin': {
        target: 'http://127.0.0.1:8003',
        changeOrigin: true,
      },
      // 用户服务 → 端口 8002
      '/api/admin': {
        target: 'http://127.0.0.1:8002',
        changeOrigin: true,
      },
      // C端用户服务 → 经 Gateway(8888) 做 token 校验后路由至 8002
      '/api/live-start/admin': {
        target: 'http://127.0.0.1:8888',
        changeOrigin: true,
      },
      // 购票引擎服务 → 经 Gateway(8888) 做 token 校验后路由至 8004
      '/api/live-start/engine': {
        target: 'http://127.0.0.1:8888',
        changeOrigin: true,
      },
    },
  },
})
