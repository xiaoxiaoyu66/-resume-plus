import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')
  
  // 获取后端地址，优先使用环境变量，默认 localhost:8080
  const backendUrl = env.VITE_BACKEND_URL || 'http://localhost:8080'
  
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: 3000,
      host: '0.0.0.0', // 允许外部访问
      allowedHosts: ['.cpolar.top', '.ngrok.io', '.ngrok-free.app', 'localhost', '127.0.0.1'], // 允许的内网穿透域名
      proxy: {
        '/api': {
          target: backendUrl,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        }
      }
    }
  }
})
