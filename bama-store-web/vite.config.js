import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 后台管理端构建配置
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5174,          // 避开小程序 H5 预览的 5173
    open: true,
    proxy: {
      // 开发期走代理，前端统一请求 /api，由 Vite 转发到后端，规避跨域与地址硬编码
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
