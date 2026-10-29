<script setup>
import { computed, onMounted } from "vue";
import { useComptesStore } from "../stores/comptes";

const store = useComptesStore();

onMounted(() => {
  store.fetchComptes();
});

function solde(compte) {
  return Number(compte.soldeActuel ?? compte.solde ?? 0);
}

const soldeTotal = computed(() => store.comptes.reduce((total, compte) => total + solde(compte), 0));

const groupes = computed(() => {
  const parType = new Map();
  for (const compte of store.comptes) {
    const libelle = compte.typeCompte?.libelle ?? "Sans type";
    if (!parType.has(libelle)) parType.set(libelle, []);
    parType.get(libelle).push(compte);
  }

  return [...parType.entries()]
    .map(([libelle, comptes]) => ({
      libelle,
      comptes,
      sousTotal: comptes.reduce((total, compte) => total + solde(compte), 0),
    }))
    .sort((a, b) => {
      if (a.libelle === "Sans type") return 1;
      if (b.libelle === "Sans type") return -1;
      return a.libelle.localeCompare(b.libelle, "fr");
    });
});

function formatMontant(valeur) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "EUR",
  }).format(valeur ?? 0);
}
</script>

<template>
  <header class="page-header">
    <h1>Soldes</h1>
  </header>

  <p v-if="store.error" class="alert">{{ store.error }}</p>
  <p v-else-if="store.loading" class="muted">Chargement…</p>

  <template v-else>
    <table class="soldes-table">
      <thead>
        <tr>
          <th>Compte</th>
          <th>Banque</th>
          <th class="soldes-table__montant">Solde</th>
        </tr>
      </thead>
      <tbody v-for="groupe in groupes" :key="groupe.libelle">
        <tr class="soldes-table__groupe">
          <th colspan="3">{{ groupe.libelle }}</th>
        </tr>
        <tr v-for="compte in groupe.comptes" :key="compte.id">
          <td>
            {{ compte.libelle }}
            <span v-if="compte.code" class="soldes-table__code">{{ compte.code }}</span>
          </td>
          <td>{{ compte.banque?.nom ?? "—" }}</td>
          <td
            class="soldes-table__montant amount"
            :class="solde(compte) < 0 ? 'amount--negative' : 'amount--positive'"
          >
            {{ formatMontant(solde(compte)) }}
          </td>
        </tr>
        <tr class="soldes-table__sous-total">
          <td colspan="2">Sous-total {{ groupe.libelle }}</td>
          <td
            class="soldes-table__montant amount"
            :class="groupe.sousTotal < 0 ? 'amount--negative' : 'amount--positive'"
          >
            {{ formatMontant(groupe.sousTotal) }}
          </td>
        </tr>
      </tbody>
      <tfoot>
        <tr>
          <td colspan="2">Total</td>
          <td
            class="soldes-table__montant amount"
            :class="soldeTotal < 0 ? 'amount--negative' : 'amount--positive'"
          >
            {{ formatMontant(soldeTotal) }}
          </td>
        </tr>
      </tfoot>
    </table>

    <p v-if="!store.comptes.length" class="muted">Aucun compte pour le moment.</p>
  </template>
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

.soldes-table {
  width: 100%;
  max-width: 720px;
  border-collapse: collapse;
}

.soldes-table th,
.soldes-table td {
  text-align: left;
  padding: 12px 10px;
  border-bottom: 1px solid var(--color-line);
  font-size: 14px;
}

.soldes-table th {
  font-family: var(--font-body);
  font-weight: 600;
  color: var(--color-ink-soft);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.soldes-table__code {
  font-size: 12px;
  color: var(--color-ink-soft);
  margin-left: 4px;
}

.soldes-table__montant {
  text-align: right;
  font-size: 16px;
}

.soldes-table__groupe th {
  text-align: left;
  background: var(--color-paper-raised);
  color: var(--color-ink-soft);
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  padding-top: 18px;
}

.soldes-table__sous-total td {
  font-weight: 600;
  border-bottom: 1px solid var(--color-ink-soft);
}

.soldes-table tfoot td {
  border-bottom: none;
  border-top: 2px solid var(--color-ink);
  font-family: var(--font-display);
  font-size: 17px;
  padding-top: 14px;
}
</style>
