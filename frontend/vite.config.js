import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 后端服务地址，仅用于开发态代理转发。
 * 与 backend/src/main/resources/application.yml 的 server.port 绑定：任一侧改端口必须同步另一侧。
 */
const BACKEND_TARGET = 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    // 固定开发端口，后端 CorsConfig 的放行源按此值配置，避免跨域配置随机漂移。
    port: 5173,
    proxy: {
      // 前端统一请求同源 /api，由 dev server 转发；后端路由本身即 /api/hello，因此不做路径重写。
      '/api': {
        target: BACKEND_TARGET,
        changeOrigin: true,
      },
    },
  },
})
