<script setup>
import { onMounted, ref } from "vue";
import apiClient from "../api/client";

// Tant que le backend Spring (lancé en process enfant par Tauri) n'a pas fini
// de démarrer, le port 8080 ne répond pas. On patiente ici, en réessayant
// régulièrement, plutôt que de laisser les vues planter sur une erreur réseau.
// On ping /version (pas /comptes) : cet endpoint ne dépend pas de la base de
// données, donc l'app reste utilisable (notamment la vue Configuration) même
// si la base configurée est injoignable — voir bootstrap-mode: lazy côté backend.
const PING_INTERVAL_MS = 600;
const MAX_ATTEMPTS = 50; // ~30s avant d'abandonner et de proposer un réessai manuel

const ready = ref(false);
const gaveUp = ref(false);
const attempt = ref(0);

async function pingUntilReady() {
  gaveUp.value = false;
  attempt.value = 0;

  while (attempt.value < MAX_ATTEMPTS) {
    attempt.value += 1;
    try {
      await apiClient.get("/version", { timeout: 1500 });
      ready.value = true;
      return;
    } catch {
      await new Promise((resolve) => setTimeout(resolve, PING_INTERVAL_MS));
    }
  }

  gaveUp.value = true;
}

onMounted(pingUntilReady);
</script>

<template>
  <div v-if="ready" class="backend-gate">
    <slot />
  </div>

  <div v-else class="backend-gate__splash">
    <div class="backend-gate__mark">Home Butler</div>

    <template v-if="!gaveUp">
      <div class="backend-gate__spinner" />
      <p class="backend-gate__label">Démarrage du backend…</p>
    </template>

    <template v-else>
      <p class="backend-gate__label backend-gate__label--error">
        Le backend met trop de temps à répondre.
      </p>
      <button class="backend-gate__retry" @click="pingUntilReady">Réessayer</button>
    </template>
  </div>
</template>

<style scoped>
.backend-gate {
  display: contents;
}

.backend-gate__splash {
  min-height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  background: var(--color-paper);
  color: var(--color-ink);
}

.backend-gate__mark {
  font-family: var(--font-display);
  font-size: 28px;
  letter-spacing: 0.01em;
  color: var(--color-ink);
}

.backend-gate__spinner {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 3px solid var(--color-line);
  border-top-color: var(--color-accent);
  animation: backend-gate-spin 0.8s linear infinite;
}

.backend-gate__label {
  font-size: 14px;
  color: var(--color-ink-soft);
}

.backend-gate__label--error {
  color: var(--color-negative);
}

.backend-gate__retry {
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  color: var(--color-ink);
  padding: 8px 18px;
  border-radius: 4px;
  font-size: 14px;
}

.backend-gate__retry:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

@keyframes backend-gate-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
