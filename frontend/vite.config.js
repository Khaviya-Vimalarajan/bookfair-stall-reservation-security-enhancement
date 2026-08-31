import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import basicSsl from '@vitejs/plugin-basic-ssl'

export default defineConfig({
  plugins: [react(), basicSsl()],
  define: {
    global: 'globalThis',
  },
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'https://localhost:8443',
      changeOrigin: true,
      secure: false
    },
    '/uploads': {
      target: 'https://localhost:8443',
      changeOrigin: true,
      secure: false
    },
    '/ws': {
      target: 'https://localhost:8443',
      changeOrigin: true,
      secure: false,
      ws: true
    }
  }
}
})
