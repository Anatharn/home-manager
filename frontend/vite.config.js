import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// Config Vite adaptée aux contraintes de Tauri :
// - port fixe (1420) attendu par tauri.conf.json et autorisé côté CORS Spring
// - ne pas clear la console pour garder les logs Rust visibles
export default defineConfig({
  plugins: [vue()],

  clearScreen: false,

  server: {
    port: 1420,
    strictPort: true,
  },

  envPrefix: ["VITE_", "TAURI_"],

  build: {
    target: "esnext",
    minify: "esbuild",
    sourcemap: !!process.env.TAURI_DEBUG,
  },
});
