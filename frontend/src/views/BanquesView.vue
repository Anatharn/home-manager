<script setup>
import { onMounted, reactive, ref } from "vue";
import { useBanquesStore } from "../stores/banques";

const store = useBanquesStore();

const nouvelleBanque = reactive({
  nom: "",
});

const editingId = ref(null);
const editBuffer = reactive({ nom: "" });

onMounted(() => {
  store.fetchBanques();
});

async function ajouterBanque() {
  if (!nouvelleBanque.nom) return;
  await store.createBanque({ ...nouvelleBanque });
  nouvelleBanque.nom = "";
}

function commencerEdition(banque) {
  editingId.value = banque.id;
  editBuffer.nom = banque.nom;
}

function annulerEdition() {
  editingId.value = null;
}

async function enregistrerEdition(banque) {
  if (!editBuffer.nom) return;
  await store.updateBanque(banque.id, { ...banque, nom: editBuffer.nom });
  editingId.value = null;
}

function supprimerBanque(banque) {
  store.deleteBanque(banque.id);
}
</script>

<template>
  <header class="page-header">
    <h1>Banques</h1>
  </header>

  <p v-if="store.error" class="alert">{{ store.error }}</p>
  <p v-else-if="store.loading" class="muted">Chargement…</p>

  <section v-else class="banques-list">
    <article v-for="banque in store.banques" :key="banque.id" class="banque-row">
      <template v-if="editingId === banque.id">
        <input
          v-model="editBuffer.nom"
          type="text"
          class="banque-row__input"
          @keyup.enter="enregistrerEdition(banque)"
          @keyup.esc="annulerEdition"
        />
        <div class="banque-row__actions">
          <button class="btn-link" @click="enregistrerEdition(banque)">Enregistrer</button>
          <button class="btn-link" @click="annulerEdition">Annuler</button>
        </div>
      </template>
      <template v-else>
        <span class="banque-row__nom">{{ banque.nom }}</span>
        <div class="banque-row__actions">
          <button class="btn-link" @click="commencerEdition(banque)">Modifier</button>
          <button class="btn-link btn-link--danger" @click="supprimerBanque(banque)">Supprimer</button>
        </div>
      </template>
    </article>

    <p v-if="!store.banques.length" class="muted">
      Aucune banque pour le moment — ajoute la première ci-dessous.
    </p>
  </section>

  <form class="new-banque" @submit.prevent="ajouterBanque">
    <h2>Ajouter une banque</h2>
    <div class="new-banque__row">
      <input v-model="nouvelleBanque.nom" type="text" placeholder="Nom (ex : Crédit Mutuel)" required />
      <button type="submit">Ajouter</button>
    </div>
  </form>
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

.banques-list {
  display: flex;
  flex-direction: column;
  max-width: 520px;
  margin-bottom: 40px;
  border-top: 1px solid var(--color-line);
}

.banque-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--color-line);
}

.banque-row__nom {
  font-size: 15px;
}

.banque-row__input {
  flex: 1;
  padding: 7px 8px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
}

.banque-row__actions {
  display: flex;
  gap: 14px;
  flex-shrink: 0;
}

.btn-link {
  background: none;
  border: none;
  padding: 0;
  color: var(--color-accent);
  font-size: 13px;
  cursor: pointer;
}

.btn-link:hover {
  text-decoration: underline;
}

.btn-link--danger {
  color: var(--color-negative);
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
  padding: 14px 4px;
}

.new-banque {
  border-top: 1px solid var(--color-line);
  padding-top: 24px;
  max-width: 520px;
}

.new-banque h2 {
  font-family: var(--font-display);
  font-size: 18px;
  margin: 0 0 14px;
}

.new-banque__row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.new-banque__row input {
  flex: 1;
  min-width: 140px;
  padding: 9px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
}

.new-banque__row button {
  padding: 9px 18px;
  background: var(--color-accent);
  color: var(--color-paper-raised);
  border: none;
  font-size: 14px;
}

.new-banque__row button:hover {
  background: #26593f;
}
</style>
