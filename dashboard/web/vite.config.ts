import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/',
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  optimizeDeps: {
    include: ['three'],
  },
  resolve: {
    alias: {
      'three': 'three/build/three.module.js'
    }
  }
})