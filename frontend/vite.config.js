import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  // "/" everywhere except GitHub Pages, which serves the site from /<repo>/
  // (the Pages workflow sets BASE_PATH).
  base: process.env.BASE_PATH || '/',
  plugins: [
    react(),
    tailwindcss(),
  ],
})
