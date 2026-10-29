<script setup>
import { onMounted, reactive, ref } from "vue";
import { useTypesCompteStore } from "../stores/typesCompte";

const store = useTypesCompteStore();

const nouveauType = reactive({
  libelle: "",
});

const editingId = ref(null);
const editBuffer = reactive({ libelle: "" });

onMounted(() => {
  store.fetchTypesCompte();
});

async function ajouterType() {
  if (!nouveauType.libelle) return;
  await store.createTypeCompte({ ...nouveauType });
  nouveauType.libelle = "";
}

function commencerEdition(typeCompte) {
  editingId.value = typeCompte.id;
  editBuffer.libelle = typeCompte.libelle;
}

function annulerEdition() {
  editingId.value = null;
}

async function enregistrerEdition(typeCompte) {
  if (!editBuffer.libelle) return;
  await store.updateTypeCompte(typeCompte.id, { ...typeCompte, libelle: editBuffer.libelle });
  editingId.value = null;
}

function supprimerType(typeCompte) {
  store.deleteTypeCompte(typeCompte.id);
}
</script>

<template>
  <header class="page-header">
    <h1>Types de compte</h1>
  </header>

  <p v-if="store.error" class="alert">{{ store.error }}</p>
  <p v-else-if="store.loading" class="muted">Chargement…</p>

  <section v-else class="types-list">
    <article v-for="typeCompte in store.typesCompte" :key="typeCompte.id" class="type-row">
      <template v-if="editingId === typeCompte.id">
        <input
          v-model="editBuffer.libelle"
          type="text"
          class="type-row__input"
          @keyup.enter="enregistrerEdition(typeCompte)"
          @keyup.esc="annulerEdition"
        />
        <div class="type-row__actions">
          <button class="btn-link" @click="enregistrerEdition(typeCompte)">Enregistrer</button>
          <button class="btn-link" @click="annulerEdition">Annuler</button>
        </div>
      </template>
      <template v-else>
        <span class="type-row__libelle">{{ typeCompte.libelle }}</span>
        <div class="type-row__actions">
          <button class="btn-link" @click="commencerEdition(typeCompte)">Modifier</button>
          <button class="btn-link btn-link--danger" @click="supprimerType(typeCompte)">Supprimer</button>
        </div>
      </template>
    </article>

    <p v-if="!store.typesCompte.length" class="muted">
      Aucun type de compte pour le moment — ajoute le premier ci-dessous.
    </p>
  </section>

  <form class="new-type" @submit.prevent="ajouterType">
    <h2>Ajouter un type de compte</h2>
    <div class="new-type__row">
      <input v-model="nouveauType.libelle" type="text" placeholder="Libellé (ex : Compte courant)" required />
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

.types-list {
  display: flex;
  flex-direction: column;
  max-width: 520px;
  margin-bottom: 40px;
  border-top: 1px solid var(--color-line);
}

.type-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--color-line);
}

.type-row__libelle {
  font-size: 15px;
}

.type-row__input {
  flex: 1;
  padding: 7px 8px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
}

.type-row__actions {
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

.new-type {
  border-top: 1px solid var(--color-line);
  padding-top: 24px;
  max-width: 520px;
}

.new-type h2 {
  font-family: var(--font-display);
  font-size: 18px;
  margin: 0 0 14px;
}

.new-type__row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.new-type__row input {
  flex: 1;
  min-width: 140px;
  padding: 9px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
}

.new-type__row button {
  padding: 9px 18px;
  background: var(--color-accent);
  color: var(--color-paper-raised);
  border: none;
  font-size: 14px;
}

.new-type__row button:hover {
  background: #26593f;
}
</style>
