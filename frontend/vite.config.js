import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const pkg = JSON.parse(readFileSync(fileURLToPath(new URL("package.json", import.meta.url)), "utf-8"));

// Config Vite adaptée aux contraintes de Tauri :
// - port fixe (1420) attendu par tauri.conf.json et autorisé côté CORS Spring
// - ne pas clear la console pour garder les logs Rust visibles
export default defineConfig({
  plugins: [vue()],

  define: {
    __APP_VERSION__: JSON.stringify(pkg.version),
  },

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
