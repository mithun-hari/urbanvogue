import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false,
      },
      '/api/products': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        secure: false,
      },
      '/orders': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
      },
      '/api/inventory': {
        target: 'http://localhost:8086',
        changeOrigin: true,
        secure: false,
      },
      '/payments': {
        target: 'http://localhost:8087',
        changeOrigin: true,
        secure: false,
      },
      '/api/notifications': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
