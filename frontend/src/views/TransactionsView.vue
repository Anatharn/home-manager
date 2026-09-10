<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useTransactionsStore } from "../stores/transactions";
import { useCategoriesStore } from "../stores/categories";
import { useComptesStore } from "../stores/comptes";
import { useChequesStore } from "../stores/cheques";
import ImportTransactionsCsv from "../components/ImportTransactionsCsv.vue";

const props = defineProps({
  compteId: {
    type: [String, Number],
    default: null,
  },
});

const isGlobal = computed(() => !props.compteId);

const store = useTransactionsStore();
const categoriesStore = useCategoriesStore();
const comptesStore = useComptesStore();
const chequesStore = useChequesStore();

const nouvelleTransaction = reactive({
  libelle: "",
  montant: 0,
  dateOperation: new Date().toISOString().slice(0, 10),
  categorieId: "",
  compteId: "",
  chequeId: "",
  commentaire: "",
});
const chequeLinkError = ref(null);

const editingId = ref(null);
const editBuffer = reactive({
  libelle: "",
  montant: 0,
  dateOperation: "",
  categorieId: "",
  compteId: "",
  commentaire: "",
});

const linkingTransactionId = ref(null);
const linkTransactionBuffer = reactive({ chequeId: "" });
const linkTransactionError = ref(null);

onMounted(() => {
  store.fetchPage({ compteId: props.compteId });
  if (isGlobal.value) {
    comptesStore.fetchComptes();
  }
  categoriesStore.fetchCategories();
  chequesStore.fetchAll();
});

function pagePrecedente() {
  if (store.page > 0) store.fetchPage({ compteId: props.compteId, page: store.page - 1 });
}

function pageSuivante() {
  if (store.page + 1 < store.totalPages) store.fetchPage({ compteId: props.compteId, page: store.page + 1 });
}

function changerTaillePage(event) {
  const size = Number(event.target.value);
  store.fetchPage({ compteId: props.compteId, page: 0, size });
}

function categorieDe(transaction) {
  const id = transaction.categorie?.id;
  if (!id) return null;
  return categoriesStore.categories.find((c) => c.id === id) ?? transaction.categorie;
}

function compteDe(transaction) {
  const id = transaction.compte?.id;
  if (!id) return transaction.compte;
  return comptesStore.comptes.find((c) => c.id === id) ?? transaction.compte;
}

function chequeDe(transaction) {
  return chequesStore.cheques.find((c) => c.transaction?.id === transaction.id) ?? null;
}

// Chèques émis du compte concerné par la nouvelle transaction : seuls ceux-là peuvent être
// liés (lier-transaction n'accepte qu'un chèque EMIS du même compte).
function chequesDisponibles() {
  const compteId = isGlobal.value ? nouvelleTransaction.compteId : props.compteId;
  if (!compteId) return [];
  return chequesStore.cheques.filter(
    (c) => c.statut === "EMIS" && String(c.compte?.id) === String(compteId)
  );
}

// Idem, mais pour une transaction déjà existante : chèques émis du même compte qu'elle.
function chequesDisponiblesPour(transaction) {
  const compteId = transaction.compte?.id;
  if (!compteId) return [];
  return chequesStore.cheques.filter(
    (c) => c.statut === "EMIS" && String(c.compte?.id) === String(compteId)
  );
}

async function ajouterTransaction() {
  if (!nouvelleTransaction.libelle) return;
  if (isGlobal.value && !nouvelleTransaction.compteId) return;
  chequeLinkError.value = null;
  const { categorieId, compteId, chequeId, ...champs } = nouvelleTransaction;
  const transactionCreee = await store.createTransaction({
    ...champs,
    compte: { id: isGlobal.value ? compteId : props.compteId },
    categorie: categorieId ? { id: categorieId } : null,
  });
  if (chequeId) {
    try {
      await chequesStore.lierTransaction(chequeId, transactionCreee.id);
    } catch (err) {
      chequeLinkError.value =
        err.response?.data?.detail ?? "La transaction a été créée, mais n'a pas pu être liée au chèque.";
    }
  }
  nouvelleTransaction.libelle = "";
  nouvelleTransaction.montant = 0;
  nouvelleTransaction.categorieId = "";
  nouvelleTransaction.compteId = "";
  nouvelleTransaction.chequeId = "";
  nouvelleTransaction.commentaire = "";
  // La transaction créée est la plus récente : elle apparaît en tête, donc on revient à la
  // première page plutôt que de rester sur la page courante (qui pourrait ne plus l'inclure).
  await store.fetchPage({ compteId: props.compteId, page: 0 });
}

function commencerEdition(transaction) {
  editingId.value = transaction.id;
  editBuffer.libelle = transaction.libelle;
  editBuffer.montant = transaction.montant;
  editBuffer.dateOperation = transaction.dateOperation;
  editBuffer.categorieId = transaction.categorie?.id ?? "";
  editBuffer.compteId = transaction.compte?.id ?? "";
  editBuffer.commentaire = transaction.commentaire ?? "";
}

function annulerEdition() {
  editingId.value = null;
}

async function enregistrerEdition(transaction) {
  if (!editBuffer.libelle) return;
  await store.updateTransaction(transaction.id, {
    ...transaction,
    libelle: editBuffer.libelle,
    montant: editBuffer.montant,
    dateOperation: editBuffer.dateOperation,
    categorie: editBuffer.categorieId ? { id: editBuffer.categorieId } : null,
    compte: { id: isGlobal.value ? editBuffer.compteId : props.compteId },
    commentaire: editBuffer.commentaire || null,
  });
  editingId.value = null;
  await store.refetch();
}

async function supprimerTransaction(transaction) {
  await store.deleteTransaction(transaction.id);
  // Si on supprime le dernier élément d'une page qui n'est plus la première, reculer d'une
  // page plutôt que d'afficher une page désormais vide.
  const page = store.page > 0 && store.transactions.length === 1 ? store.page - 1 : store.page;
  await store.fetchPage({ compteId: props.compteId, page });
}

function commencerLiaisonCheque(transaction) {
  linkingTransactionId.value = transaction.id;
  linkTransactionBuffer.chequeId = "";
  linkTransactionError.value = null;
}

function annulerLiaisonCheque() {
  linkingTransactionId.value = null;
  linkTransactionError.value = null;
}

async function confirmerLiaisonCheque(transaction) {
  if (!linkTransactionBuffer.chequeId) return;
  linkTransactionError.value = null;
  try {
    await chequesStore.lierTransaction(linkTransactionBuffer.chequeId, transaction.id);
    linkingTransactionId.value = null;
    await store.refetch();
  } catch (err) {
    linkTransactionError.value = err.response?.data?.detail ?? "Impossible de lier cette transaction.";
  }
}

function formatMontant(valeur) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "EUR",
  }).format(valeur ?? 0);
}

// "2026-01-05" -> "05/01/2026" (l'API renvoie toujours les dates au format ISO)
function formatDate(iso) {
  if (!iso) return "";
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y}`;
}
</script>

<template>
  <header class="page-header">
    <RouterLink v-if="!isGlobal" to="/comptes" class="back-link">&larr; Retour aux comptes</RouterLink>
    <h1>{{ isGlobal ? "Toutes les transactions" : "Transactions" }}</h1>
  </header>

  <ImportTransactionsCsv v-if="isGlobal" @imported="store.fetchPage({ compteId: props.compteId, page: 0 })" />

  <p v-if="store.error" class="alert">{{ store.error }}</p>
  <p v-else-if="store.loading" class="muted">Chargement…</p>

  <table v-else class="transactions-table">
    <thead>
      <tr>
        <th>Date</th>
        <th v-if="isGlobal">Compte</th>
        <th>Libellé</th>
        <th>Catégorie</th>
        <th>Commentaire</th>
        <th class="text-right">Montant</th>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="t in store.transactions" :key="t.id">
        <template v-if="editingId === t.id">
          <td><input v-model="editBuffer.dateOperation" type="date" class="cell-input" /></td>
          <td v-if="isGlobal">
            <select v-model="editBuffer.compteId" class="cell-input">
              <option v-for="c in comptesStore.comptes" :key="c.id" :value="c.id">
                {{ c.libelle }}{{ c.banque ? " (" + c.banque.nom + ")" : "" }}
              </option>
            </select>
          </td>
          <td><input v-model="editBuffer.libelle" type="text" class="cell-input" @keyup.enter="enregistrerEdition(t)" @keyup.esc="annulerEdition" /></td>
          <td>
            <select v-model="editBuffer.categorieId" class="cell-input">
              <option value="">Sans catégorie</option>
              <option v-for="c in categoriesStore.categories" :key="c.id" :value="c.id">{{ c.libelle }}</option>
            </select>
          </td>
          <td><input v-model="editBuffer.commentaire" type="text" placeholder="Commentaire" class="cell-input" /></td>
          <td class="text-right">
            <input v-model.number="editBuffer.montant" type="number" step="0.01" class="cell-input cell-input--amount" />
          </td>
          <td class="text-right">
            <button class="link-button" @click="enregistrerEdition(t)">Enregistrer</button>
            <button class="link-button" @click="annulerEdition">Annuler</button>
          </td>
        </template>
        <template v-else-if="linkingTransactionId === t.id">
          <td :colspan="isGlobal ? 6 : 5">
            <div class="encaisser-form">
              <span>Lier la transaction « {{ t.libelle }} » au chèque</span>
              <select v-model="linkTransactionBuffer.chequeId" class="cell-input">
                <option value="" disabled>Chèque…</option>
                <option v-for="c in chequesDisponiblesPour(t)" :key="c.id" :value="c.id">
                  n°{{ c.numero }}{{ c.beneficiaire ? " — " + c.beneficiaire : "" }}
                </option>
              </select>
              <span v-if="!chequesDisponiblesPour(t).length" class="muted">Aucun chèque émis disponible sur ce compte.</span>
              <span v-if="linkTransactionError" class="muted link-error">{{ linkTransactionError }}</span>
            </div>
          </td>
          <td class="text-right">
            <button class="link-button" @click="confirmerLiaisonCheque(t)">Confirmer</button>
            <button class="link-button" @click="annulerLiaisonCheque">Annuler</button>
          </td>
        </template>
        <template v-else>
          <td>{{ formatDate(t.dateOperation) }}</td>
          <td v-if="isGlobal">
            {{ compteDe(t)?.libelle ?? "—" }}
            <span v-if="compteDe(t)?.banque" class="muted compte-banque">({{ compteDe(t).banque.nom }})</span>
          </td>
          <td>
            {{ t.libelle }}
            <span v-if="chequeDe(t)" class="cheque-badge" :title="'Lié au chèque n°' + chequeDe(t).numero">
              🖊 n°{{ chequeDe(t).numero }}
            </span>
          </td>
          <td>
            <span v-if="categorieDe(t)" class="categorie-badge">
              <span class="categorie-badge__pastille" :style="{ background: categorieDe(t).couleur || '#999' }"></span>
              {{ categorieDe(t).libelle }}
            </span>
            <span v-else class="muted">—</span>
          </td>
          <td class="commentaire-cell" :title="t.commentaire">{{ t.commentaire || "—" }}</td>
          <td
            class="amount text-right"
            :class="Number(t.montant) < 0 ? 'amount--negative' : 'amount--positive'"
          >
            {{ formatMontant(t.montant) }}
          </td>
          <td class="text-right">
            <button class="link-button" @click="commencerEdition(t)">Modifier</button>
            <button v-if="!chequeDe(t)" class="link-button" @click="commencerLiaisonCheque(t)">Lier…</button>
            <button class="link-button" @click="supprimerTransaction(t)">Supprimer</button>
          </td>
        </template>
      </tr>
      <tr v-if="!store.transactions.length">
        <td :colspan="isGlobal ? 7 : 6" class="muted">Aucune transaction.</td>
      </tr>
    </tbody>
  </table>

  <div v-if="store.totalElements" class="pagination">
    <label class="pagination__taille">
      Afficher
      <select :value="store.size" @change="changerTaillePage">
        <option :value="5">5</option>
        <option :value="10">10</option>
        <option :value="25">25</option>
        <option :value="50">50</option>
      </select>
      par page
    </label>

    <template v-if="store.totalPages > 1">
      <button class="link-button" :disabled="store.page === 0" @click="pagePrecedente">&larr; Précédente</button>
      <span class="muted">
        Page {{ store.page + 1 }} sur {{ store.totalPages }}
      </span>
      <button class="link-button" :disabled="store.page + 1 >= store.totalPages" @click="pageSuivante">Suivante &rarr;</button>
    </template>

    <span class="muted">({{ store.totalElements }} transaction{{ store.totalElements > 1 ? "s" : "" }} au total)</span>
  </div>

  <form class="new-transaction" @submit.prevent="ajouterTransaction">
    <h2>Ajouter une transaction</h2>
    <div class="new-transaction__row">
      <input v-model="nouvelleTransaction.dateOperation" type="date" required />
      <select v-if="isGlobal" v-model="nouvelleTransaction.compteId" required>
        <option value="" disabled>Compte…</option>
        <option v-for="c in comptesStore.comptes" :key="c.id" :value="c.id">
          {{ c.libelle }}{{ c.banque ? " (" + c.banque.nom + ")" : "" }}
        </option>
      </select>
      <input v-model="nouvelleTransaction.libelle" type="text" placeholder="Libellé" required />
      <select v-model="nouvelleTransaction.categorieId">
        <option value="">Sans catégorie</option>
        <option v-for="c in categoriesStore.categories" :key="c.id" :value="c.id">{{ c.libelle }}</option>
      </select>
      <select v-model="nouvelleTransaction.chequeId" title="Lier cette transaction à un chèque émis">
        <option value="">Sans chèque</option>
        <option v-for="c in chequesDisponibles()" :key="c.id" :value="c.id">
          Chèque n°{{ c.numero }}{{ c.beneficiaire ? " — " + c.beneficiaire : "" }}
        </option>
      </select>
      <input v-model="nouvelleTransaction.commentaire" type="text" placeholder="Commentaire (optionnel)" />
      <input v-model.number="nouvelleTransaction.montant" type="number" step="0.01" placeholder="Montant" required />
      <button type="submit">Ajouter</button>
    </div>
    <p v-if="chequeLinkError" class="cheque-link-error">{{ chequeLinkError }}</p>
  </form>
</template>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.back-link {
  display: inline-block;
  font-size: 13px;
  color: var(--color-ink-soft);
  text-decoration: none;
  margin-bottom: 10px;
}

.page-header h1 {
  font-family: var(--font-display);
  font-size: 30px;
  margin: 0;
}

.transactions-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 16px;
}

.transactions-table th {
  text-align: left;
  font-size: 12px;
  color: var(--color-ink-soft);
  font-weight: 500;
  padding: 8px 10px;
  border-bottom: 1px solid var(--color-line);
}

.transactions-table td {
  padding: 10px 10px;
  border-bottom: 1px solid var(--color-line);
  font-size: 14px;
}

.text-right {
  text-align: right;
}

.categorie-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.categorie-badge__pastille {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.compte-banque {
  font-size: 12px;
  margin-left: 4px;
}

.commentaire-cell {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-ink-soft);
}

.cheque-badge {
  display: inline-block;
  font-size: 11px;
  color: var(--color-ink-soft);
  margin-left: 6px;
}

.cheque-link-error {
  color: var(--color-negative);
  font-size: 13px;
  margin: 10px 0 0;
}

.cell-input {
  width: 100%;
  padding: 6px 7px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 13px;
}

.cell-input--amount {
  text-align: right;
}

.encaisser-form {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--color-ink-soft);
}

.encaisser-form .cell-input {
  width: auto;
}

.link-error {
  color: var(--color-negative);
}

.link-button {
  background: none;
  border: none;
  color: var(--color-ink-soft);
  font-size: 13px;
  padding: 0;
  margin-left: 10px;
}

.link-button:first-child {
  margin-left: 0;
}

.link-button:hover:not(:disabled) {
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
}

.pagination {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin: 0 0 28px;
  font-size: 13px;
}

.pagination__taille {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-ink-soft);
}

.pagination__taille select {
  padding: 4px 6px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 13px;
}

.pagination .link-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.new-transaction {
  border-top: 1px solid var(--color-line);
  padding-top: 24px;
  max-width: 760px;
}

.new-transaction h2 {
  font-family: var(--font-display);
  font-size: 18px;
  margin: 0 0 14px;
}

.new-transaction__row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.new-transaction__row input,
.new-transaction__row select {
  flex: 1;
  min-width: 130px;
  padding: 9px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
}

.new-transaction__row button {
  padding: 9px 18px;
  background: var(--color-accent);
  color: var(--color-paper-raised);
  border: none;
  font-size: 14px;
}

.new-transaction__row button:hover {
  background: #26593f;
}
</style>
