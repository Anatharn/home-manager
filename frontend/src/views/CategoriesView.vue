<script setup>
import { onMounted, reactive, ref } from "vue";
import { useCategoriesStore } from "../stores/categories";

const store = useCategoriesStore();

const nouvelleCategorie = reactive({
  libelle: "",
  couleur: "#3a7057",
});

const editingId = ref(null);
const editBuffer = reactive({ libelle: "", couleur: "#3a7057" });

onMounted(() => {
  store.fetchCategories();
});

async function ajouterCategorie() {
  if (!nouvelleCategorie.libelle) return;
  await store.createCategorie({ ...nouvelleCategorie });
  nouvelleCategorie.libelle = "";
  nouvelleCategorie.couleur = "#3a7057";
}

function commencerEdition(categorie) {
  editingId.value = categorie.id;
  editBuffer.libelle = categorie.libelle;
  editBuffer.couleur = categorie.couleur || "#3a7057";
}

function annulerEdition() {
  editingId.value = null;
}

async function enregistrerEdition(categorie) {
  if (!editBuffer.libelle) return;
  await store.updateCategorie(categorie.id, { ...categorie, libelle: editBuffer.libelle, couleur: editBuffer.couleur });
  editingId.value = null;
}

function supprimerCategorie(categorie) {
  store.deleteCategorie(categorie.id);
}
</script>

<template>
  <header class="page-header">
    <h1>Catégories</h1>
  </header>

  <p v-if="store.error" class="alert">{{ store.error }}</p>
  <p v-else-if="store.loading" class="muted">Chargement…</p>

  <section v-else class="categories-list">
    <article v-for="categorie in store.categories" :key="categorie.id" class="categorie-row">
      <template v-if="editingId === categorie.id">
        <input v-model="editBuffer.couleur" type="color" class="categorie-row__couleur" />
        <input
          v-model="editBuffer.libelle"
          type="text"
          class="categorie-row__input"
          @keyup.enter="enregistrerEdition(categorie)"
          @keyup.esc="annulerEdition"
        />
        <div class="categorie-row__actions">
          <button class="btn-link" @click="enregistrerEdition(categorie)">Enregistrer</button>
          <button class="btn-link" @click="annulerEdition">Annuler</button>
        </div>
      </template>
      <template v-else>
        <span class="categorie-row__pastille" :style="{ background: categorie.couleur || '#999' }"></span>
        <span class="categorie-row__libelle">{{ categorie.libelle }}</span>
        <div class="categorie-row__actions">
          <button class="btn-link" @click="commencerEdition(categorie)">Modifier</button>
          <button class="btn-link btn-link--danger" @click="supprimerCategorie(categorie)">Supprimer</button>
        </div>
      </template>
    </article>

    <p v-if="!store.categories.length" class="muted">
      Aucune catégorie pour le moment — ajoute la première ci-dessous.
    </p>
  </section>

  <form class="new-categorie" @submit.prevent="ajouterCategorie">
    <h2>Ajouter une catégorie</h2>
    <div class="new-categorie__row">
      <input v-model="nouvelleCategorie.couleur" type="color" class="new-categorie__couleur" />
      <input v-model="nouvelleCategorie.libelle" type="text" placeholder="Libellé (ex : Alimentation)" required />
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

.categories-list {
  display: flex;
  flex-direction: column;
  max-width: 520px;
  margin-bottom: 40px;
  border-top: 1px solid var(--color-line);
}

.categorie-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--color-line);
}

.categorie-row__pastille {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
}

.categorie-row__libelle {
  flex: 1;
  font-size: 15px;
}

.categorie-row__couleur {
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--color-line);
  background: none;
  flex-shrink: 0;
}

.categorie-row__input {
  flex: 1;
  padding: 7px 8px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
}

.categorie-row__actions {
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

.new-categorie {
  border-top: 1px solid var(--color-line);
  padding-top: 24px;
  max-width: 520px;
}

.new-categorie h2 {
  font-family: var(--font-display);
  font-size: 18px;
  margin: 0 0 14px;
}

.new-categorie__row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.new-categorie__couleur {
  width: 40px;
  height: 40px;
  padding: 0;
  border: 1px solid var(--color-line);
  background: none;
  flex-shrink: 0;
}

.new-categorie__row input[type="text"] {
  flex: 1;
  min-width: 140px;
  padding: 9px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
}

.new-categorie__row button {
  padding: 9px 18px;
  background: var(--color-accent);
  color: var(--color-paper-raised);
  border: none;
  font-size: 14px;
}

.new-categorie__row button:hover {
  background: #26593f;
}
</style>
