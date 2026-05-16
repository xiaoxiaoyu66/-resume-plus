import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')
  
  // 获取后端地址，优先使用环境变量，默认 localhost:8080
  const backendUrl = env.VITE_BACKEND_URL || 'http://localhost:8080'
  
  const isProd = mode === 'production'

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
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            // 将 element-plus 拆为独立 chunk
            if (id.includes('node_modules/element-plus')) {
              return 'ui-element-plus'
            }
            // 将 jspdf + html2canvas 拆为导出相关 chunk
            if (id.includes('jspdf') || id.includes('html2canvas')) {
              return 'export-pdf'
            }
            // highlight.js 语言包很大，单独拆开
            if (id.includes('highlight.js')) {
              return 'highlight'
            }
            // marked 拆为独立 Markdown 渲染 chunk
            if (id.includes('marked')) {
              return 'markdown'
            }
            // 其他 node_modules 统一为 vendor
            if (id.includes('node_modules')) {
              return 'vendor'
            }
          }
        }
      },
      // 生产环境启用源码映射方便调试，可选
      sourcemap: isProd ? false : true
    }
  }
})
