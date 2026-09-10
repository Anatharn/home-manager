<script setup>
import { onMounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { useComptesStore } from "../stores/comptes";
import { useBanquesStore } from "../stores/banques";
import CompteCard from "../components/CompteCard.vue";

const store = useComptesStore();
const banquesStore = useBanquesStore();
const router = useRouter();

const nouveauCompte = reactive({
  libelle: "",
  iban: "",
  code: "",
  solde: 0,
  banqueId: "",
});

onMounted(() => {
  store.fetchComptes();
  banquesStore.fetchBanques();
});

async function ajouterCompte() {
  if (!nouveauCompte.libelle || !nouveauCompte.code) return;
  const { banqueId, ...champs } = nouveauCompte;
  await store.createCompte({
    ...champs,
    banque: banqueId ? { id: banqueId } : null,
  });
  nouveauCompte.libelle = "";
  nouveauCompte.iban = "";
  nouveauCompte.code = "";
  nouveauCompte.solde = 0;
  nouveauCompte.banqueId = "";
}

function ouvrirCompte(compte) {
  router.push({ name: "transactions", params: { compteId: compte.id } });
}

function supprimerCompte(compte) {
  store.deleteCompte(compte.id);
}

function changerBanque(compte, banqueId) {
  store.updateCompte(compte.id, {
    ...compte,
    banque: banqueId ? { id: banqueId } : null,
  });
}

function formatMontant(valeur) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "EUR",
  }).format(valeur ?? 0);
}
</script>

<template>
  <header class="page-header">
    <div>
      <h1>Comptes</h1>
      <p class="page-header__solde">
        Solde total :
        <span
          class="amount"
          :class="store.soldeTotal < 0 ? 'amount--negative' : 'amount--positive'"
        >
          {{ formatMontant(store.soldeTotal) }}
        </span>
      </p>
    </div>
  </header>

  <p v-if="store.error" class="alert">{{ store.error }}</p>
  <p v-else-if="store.loading" class="muted">Chargement…</p>

  <section v-else class="comptes-grid">
    <CompteCard
      v-for="compte in store.comptes"
      :key="compte.id"
      :compte="compte"
      :banques="banquesStore.banques"
      @open="ouvrirCompte"
      @delete="supprimerCompte"
      @change-banque="changerBanque"
    />

    <p v-if="!store.comptes.length" class="muted">
      Aucun compte pour le moment — ajoute le premier ci-dessous.
    </p>
  </section>

  <form class="new-compte" @submit.prevent="ajouterCompte">
    <h2>Ajouter un compte</h2>
    <div class="new-compte__row">
      <input v-model="nouveauCompte.libelle" type="text" placeholder="Libellé (ex : Compte courant)" required />
      <input v-model="nouveauCompte.iban" type="text" placeholder="IBAN (optionnel)" />
      <input v-model="nouveauCompte.code" type="text" placeholder="Code" required />
      <input v-model.number="nouveauCompte.solde" type="number" step="0.01" placeholder="Solde initial" />
      <select v-model="nouveauCompte.banqueId">
        <option value="">Sans banque</option>
        <option v-for="banque in banquesStore.banques" :key="banque.id" :value="banque.id">
          {{ banque.nom }}
        </option>
      </select>
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
  margin: 0 0 6px;
}

.page-header__solde {
  color: var(--color-ink-soft);
  margin: 0;
}

.comptes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 40px;
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

.new-compte {
  border-top: 1px solid var(--color-line);
  padding-top: 24px;
  max-width: 640px;
}

.new-compte h2 {
  font-family: var(--font-display);
  font-size: 18px;
  margin: 0 0 14px;
}

.new-compte__row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.new-compte__row input,
.new-compte__row select {
  flex: 1;
  min-width: 140px;
  padding: 9px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
}

.new-compte__row button {
  padding: 9px 18px;
  background: var(--color-accent);
  color: var(--color-paper-raised);
  border: none;
  font-size: 14px;
}

.new-compte__row button:hover {
  background: #26593f;
}
</style>
