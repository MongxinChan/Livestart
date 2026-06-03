import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') },
  },
  server: {
    port: 3001,
    open: true,
    proxy: {
      '/api/merchant-admin': { target: 'http://127.0.0.1:8003', changeOrigin: true },
      '/api/admin': { target: 'http://127.0.0.1:8002', changeOrigin: true },
      '/api/engine': { target: 'http://127.0.0.1:8004', changeOrigin: true },
      '/api/settlement': { target: 'http://127.0.0.1:8007', changeOrigin: true },
    },
  },
})
