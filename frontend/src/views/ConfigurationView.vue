<script setup>
import { onMounted, reactive, watch } from "vue";
import { useConfigurationStore } from "../stores/configuration";

const store = useConfigurationStore();

const form = reactive({
  hote: "",
  port: 3306,
  nomBase: "",
  utilisateur: "",
  motDePasse: "",
});

onMounted(async () => {
  await store.fetchDatabase();
  Object.assign(form, store.database);
});

watch(
  () => store.database,
  (database) => Object.assign(form, database),
);

async function enregistrer() {
  await store.saveDatabase({ ...form });
  form.motDePasse = "";
}
</script>

<template>
  <header class="page-header">
    <h1>Configuration</h1>
  </header>

  <section class="config-section">
    <h2>Base de données</h2>
    <p class="config-section__intro">
      Connexion à une base MariaDB distante. Une fois enregistrée, l'application doit être
      redémarrée pour que la nouvelle connexion soit prise en compte.
    </p>

    <p v-if="store.error" class="alert">{{ store.error }}</p>
    <p v-else-if="store.loading" class="muted">Chargement…</p>

    <form v-else class="db-form" @submit.prevent="enregistrer">
      <label class="db-form__field">
        <span>Hôte</span>
        <input v-model="form.hote" type="text" placeholder="ex : 192.168.1.50" required />
      </label>

      <label class="db-form__field db-form__field--port">
        <span>Port</span>
        <input v-model.number="form.port" type="number" placeholder="3306" required />
      </label>

      <label class="db-form__field">
        <span>Nom de la base</span>
        <input v-model="form.nomBase" type="text" placeholder="ex : home_butler" required />
      </label>

      <label class="db-form__field">
        <span>Utilisateur</span>
        <input v-model="form.utilisateur" type="text" placeholder="ex : home_butler_user" required />
      </label>

      <label class="db-form__field">
        <span>Mot de passe</span>
        <input
          v-model="form.motDePasse"
          type="password"
          :placeholder="store.database.configuree ? 'Inchangé si laissé vide' : ''"
          autocomplete="new-password"
        />
      </label>

      <div class="db-form__actions">
        <button type="submit" :disabled="store.saving">
          {{ store.saving ? "Enregistrement…" : "Enregistrer" }}
        </button>
        <span v-if="store.saved" class="db-form__saved">Enregistré — redémarre l'application pour l'appliquer.</span>
        <span v-if="store.saveError" class="db-form__error">{{ store.saveError }}</span>
      </div>
    </form>
  </section>
</template>

<style scoped>
.page-header {
  margin-bottom: 28px;
}

.page-header h1 {
  font-family: var(--font-display);
  font-size: 30px;
  margin: 0;
}

.config-section {
  max-width: 520px;
}

.config-section h2 {
  font-family: var(--font-display);
  font-size: 20px;
  margin: 0 0 8px;
}

.config-section__intro {
  color: var(--color-ink-soft);
  font-size: 14px;
  margin: 0 0 20px;
}

.alert {
  background: #F7E9E6;
  border: 1px solid var(--color-negative);
  color: var(--color-negative);
  padding: 12px 16px;
  font-size: 14px;
}

.muted {
  color: var(--color-ink-soft);
  font-size: 14px;
}

.db-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.db-form__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: var(--color-ink-soft);
}

.db-form__field input {
  padding: 9px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--color-ink);
}

.db-form__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 4px;
}

.db-form__actions button {
  padding: 9px 18px;
  background: var(--color-accent);
  color: var(--color-paper-raised);
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.db-form__actions button:hover {
  background: #26593f;
}

.db-form__actions button:disabled {
  opacity: 0.6;
  cursor: default;
}

.db-form__saved {
  color: var(--color-accent);
  font-size: 13px;
}

.db-form__error {
  color: var(--color-negative);
  font-size: 13px;
}
</style>
