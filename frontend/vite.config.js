import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  const backendTarget = env.VITE_BACKEND_PROXY_TARGET || 'http://127.0.0.1:8001'
  const basePath = env.VITE_BASE_PATH || '/member/'

  return {
    base: basePath,
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: {
        '/member-api': {
          target: backendTarget,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/member-api/, '/api')
        },
        '/member-uploads': {
          target: backendTarget,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/member-uploads/, '/uploads')
        }
      }
    }
  }
})