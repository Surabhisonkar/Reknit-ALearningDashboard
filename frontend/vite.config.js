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
  resolve: {
    // @reknit/storyboard (../packages/storyboard) is linked, not installed, so
    // its react and remotion imports must resolve to this app's copies.
    dedupe: ['react', 'react-dom', 'remotion'],
  },
})
