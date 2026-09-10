<script setup>
import { RouterView } from "vue-router";
import BackendGate from "./components/BackendGate.vue";
</script>

<template>
  <BackendGate>
    <aside class="sidebar">
      <div class="sidebar__mark">Home Butler</div>
      <nav class="sidebar__nav">
        <RouterLink to="/comptes" class="sidebar__link">Comptes</RouterLink>
        <RouterLink to="/transactions" class="sidebar__link">Transactions</RouterLink>
        <RouterLink to="/cheques" class="sidebar__link">Chèques</RouterLink>
        <RouterLink to="/banques" class="sidebar__link">Banques</RouterLink>
        <RouterLink to="/categories" class="sidebar__link">Catégories</RouterLink>
      </nav>
      <div class="sidebar__foot">POC — base H2</div>
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
