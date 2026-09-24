import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// React 플러그인: JSX 변환 + Fast Refresh(저장 시 상태 유지 새로고침)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 백엔드 CORS 허용 origin이 5173 하나라서, 포트가 바뀌지 않도록 고정
    strictPort: true,
  },
})
