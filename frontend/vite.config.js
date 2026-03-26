import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/api/products': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/orders': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/api/inventory': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/payments': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
