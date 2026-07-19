import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      // @ 代表 src 目錄。jsconfig.json 有同樣設定,讓編輯器也看得懂路徑
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
