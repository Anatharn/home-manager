<script setup>
import { onMounted, ref } from "vue";
import { RouterView } from "vue-router";
import BackendGate from "./components/BackendGate.vue";
import apiClient from "./api/client";

const frontVersion = __APP_VERSION__;
const backVersion = ref(null);

onMounted(async () => {
  try {
    const response = await apiClient.get("/version");
    backVersion.value = response.data.version;
  } catch {
    backVersion.value = null;
  }
});
</script>

<template>
  <BackendGate>
    <aside class="sidebar">
      <div class="sidebar__mark">Home Butler</div>
      <nav class="sidebar__nav">
        <div class="sidebar__group">
          <span class="sidebar__group-label">Gestion</span>
          <RouterLink to="/soldes" class="sidebar__link">Soldes</RouterLink>
          <RouterLink to="/transactions" class="sidebar__link">Transactions</RouterLink>
          <RouterLink to="/cheques" class="sidebar__link">Chèques</RouterLink>
        </div>

        <div class="sidebar__group">
          <span class="sidebar__group-label">Administration</span>
          <RouterLink to="/comptes" class="sidebar__link">Comptes</RouterLink>
          <RouterLink to="/banques" class="sidebar__link">Banques</RouterLink>
          <RouterLink to="/types-compte" class="sidebar__link">Types de compte</RouterLink>
          <RouterLink to="/categories" class="sidebar__link">Catégories</RouterLink>
          <RouterLink to="/formats-import-csv" class="sidebar__link">Formats d'import CSV</RouterLink>
        </div>

        <RouterLink to="/configuration" class="sidebar__link">Configuration</RouterLink>
      </nav>
      <div class="sidebar__foot">
        Front v{{ frontVersion }} · Back {{ backVersion ? `v${backVersion}` : "…" }}
      </div>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </BackendGate>
</template>

<style scoped>
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--color-ink);
  color: var(--color-paper);
  display: flex;
  flex-direction: column;
  padding: 28px 20px;
}

.sidebar__mark {
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 0.01em;
  margin-bottom: 40px;
}

.sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.sidebar__link {
  text-decoration: none;
  color: rgba(246, 244, 239, 0.7);
  padding: 8px 10px;
  border-radius: 4px;
  font-size: 14px;
}

.sidebar__link:hover,
.sidebar__link.router-link-active {
  color: var(--color-paper);
  background: rgba(246, 244, 239, 0.08);
}

.sidebar__group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 10px 0;
}

.sidebar__group-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: rgba(246, 244, 239, 0.4);
  padding: 4px 10px;
}

.sidebar__group .sidebar__link {
  padding-left: 20px;
}

.sidebar__foot {
  font-size: 12px;
  color: rgba(246, 244, 239, 0.4);
}

.content {
  flex: 1;
  min-width: 0;
  padding: 40px 48px;
  overflow-y: auto;
}
</style>
