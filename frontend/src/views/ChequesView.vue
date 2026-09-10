<script setup>
import { onMounted, reactive, ref } from "vue";
import { useChequesStore } from "../stores/cheques";
import { useComptesStore } from "../stores/comptes";
import { useCategoriesStore } from "../stores/categories";
import { useTransactionsStore } from "../stores/transactions";

const store = useChequesStore();
const comptesStore = useComptesStore();
const categoriesStore = useCategoriesStore();
const transactionsStore = useTransactionsStore();

const nouveauCheque = reactive({
  numero: "",
  beneficiaire: "",
  montant: 0,
  dateEmission: new Date().toISOString().slice(0, 10),
  compteId: "",
});

const editingId = ref(null);
const editBuffer = reactive({
  numero: "",
  beneficiaire: "",
  montant: 0,
  dateEmission: "",
  compteId: "",
});

const encaissingId = ref(null);
const encaisseBuffer = reactive({
  dateEncaissement: new Date().toISOString().slice(0, 10),
  categorieId: "",
});

const linkingId = ref(null);
const linkBuffer = reactive({ transactionId: "" });
const linkError = ref(null);
// Transactions du compte du chèque en cours de rattachement, chargées à la demande (la page
// Chèques ne charge plus l'historique complet des transactions de tous les comptes).
const transactionsPourLiaison = ref([]);

onMounted(() => {
  store.fetchAll();
  comptesStore.fetchComptes();
  categoriesStore.fetchCategories();
});

function compteDe(cheque) {
  const id = cheque.compte?.id;
  if (!id) return cheque.compte;
  return comptesStore.comptes.find((c) => c.id === id) ?? cheque.compte;
}

// Parmi les transactions déjà chargées pour ce rattachement, celles pas déjà liées à un autre
// chèque : ce sont les seules candidates raisonnables pour un rattachement manuel.
function transactionsDisponibles() {
  const transactionsDejaLiees = new Set(
    store.cheques.filter((c) => c.transaction).map((c) => c.transaction.id)
  );
  return transactionsPourLiaison.value.filter((t) => !transactionsDejaLiees.has(t.id));
}

const libellesStatut = {
  EMIS: "Émis",
  ENCAISSE: "Encaissé",
  ANNULE: "Annulé",
};

async function ajouterCheque() {
  if (!nouveauCheque.numero || !nouveauCheque.compteId) return;
  const { compteId, ...champs } = nouveauCheque;
  await store.createCheque({
    ...champs,
    compte: { id: compteId },
  });
  nouveauCheque.numero = "";
  nouveauCheque.beneficiaire = "";
  nouveauCheque.montant = 0;
  nouveauCheque.compteId = "";
}

function commencerEdition(cheque) {
  editingId.value = cheque.id;
  editBuffer.numero = cheque.numero;
  editBuffer.beneficiaire = cheque.beneficiaire;
  editBuffer.montant = cheque.montant;
  editBuffer.dateEmission = cheque.dateEmission;
  editBuffer.compteId = cheque.compte?.id ?? "";
}

function annulerEdition() {
  editingId.value = null;
}

async function enregistrerEdition(cheque) {
  if (!editBuffer.numero || !editBuffer.compteId) return;
  await store.updateCheque(cheque.id, {
    ...cheque,
    numero: editBuffer.numero,
    beneficiaire: editBuffer.beneficiaire,
    montant: editBuffer.montant,
    dateEmission: editBuffer.dateEmission,
    compte: { id: editBuffer.compteId },
  });
  editingId.value = null;
}

function commencerEncaissement(cheque) {
  encaissingId.value = cheque.id;
  encaisseBuffer.dateEncaissement = new Date().toISOString().slice(0, 10);
  encaisseBuffer.categorieId = "";
}

function annulerEncaissement() {
  encaissingId.value = null;
}

async function confirmerEncaissement(cheque) {
  await store.encaisserCheque(cheque.id, encaisseBuffer.dateEncaissement, encaisseBuffer.categorieId || null);
  encaissingId.value = null;
}

async function commencerLiaison(cheque) {
  linkingId.value = cheque.id;
  linkBuffer.transactionId = "";
  linkError.value = null;
  transactionsPourLiaison.value = await transactionsStore.fetchToutesPourCompte(cheque.compte.id);
}

function annulerLiaison() {
  linkingId.value = null;
  linkError.value = null;
  transactionsPourLiaison.value = [];
}

async function confirmerLiaison(cheque) {
  if (!linkBuffer.transactionId) return;
  linkError.value = null;
  try {
    await store.lierTransaction(cheque.id, linkBuffer.transactionId);
    linkingId.value = null;
    transactionsPourLiaison.value = [];
  } catch (err) {
    linkError.value = err.response?.data?.detail ?? "Impossible de lier cette transaction.";
  }
}

function annulerCheque(cheque) {
  store.annulerCheque(cheque.id);
}

function supprimerCheque(cheque) {
  store.deleteCheque(cheque.id);
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
    <h1>Chèques</h1>
  </header>

  <p v-if="store.error" class="alert">{{ store.error }}</p>
  <p v-else-if="store.loading" class="muted">Chargement…</p>

  <table v-else class="cheques-table">
    <thead>
      <tr>
        <th>N°</th>
        <th>Bénéficiaire</th>
        <th>Compte</th>
        <th>Émission</th>
        <th class="text-right">Montant</th>
        <th>Statut</th>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="c in store.cheques" :key="c.id">
        <template v-if="editingId === c.id">
          <td><input v-model="editBuffer.numero" type="text" class="cell-input" /></td>
          <td><input v-model="editBuffer.beneficiaire" type="text" class="cell-input" /></td>
          <td>
            <select v-model="editBuffer.compteId" class="cell-input">
              <option v-for="compte in comptesStore.comptes" :key="compte.id" :value="compte.id">{{ compte.libelle }}</option>
            </select>
          </td>
          <td><input v-model="editBuffer.dateEmission" type="date" class="cell-input" /></td>
          <td class="text-right">
            <input v-model.number="editBuffer.montant" type="number" step="0.01" class="cell-input cell-input--amount" />
          </td>
          <td class="muted">Émis</td>
          <td class="text-right actions">
            <button class="link-button" @click="enregistrerEdition(c)">Enregistrer</button>
            <button class="link-button" @click="annulerEdition">Annuler</button>
          </td>
        </template>
        <template v-else-if="encaissingId === c.id">
          <td colspan="6">
            <div class="encaisser-form">
              <span>Encaisser le chèque {{ c.numero }} le</span>
              <input v-model="encaisseBuffer.dateEncaissement" type="date" class="cell-input" />
              <select v-model="encaisseBuffer.categorieId" class="cell-input">
                <option value="">Sans catégorie</option>
                <option v-for="cat in categoriesStore.categories" :key="cat.id" :value="cat.id">{{ cat.libelle }}</option>
              </select>
            </div>
          </td>
          <td class="text-right actions">
            <button class="link-button" @click="confirmerEncaissement(c)">Confirmer</button>
            <button class="link-button" @click="annulerEncaissement">Annuler</button>
          </td>
        </template>
        <template v-else-if="linkingId === c.id">
          <td colspan="6">
            <div class="encaisser-form">
              <span>Lier le chèque {{ c.numero }} à</span>
              <select v-model="linkBuffer.transactionId" class="cell-input">
                <option value="" disabled>Transaction…</option>
                <option v-for="t in transactionsDisponibles()" :key="t.id" :value="t.id">
                  {{ formatDate(t.dateOperation) }} — {{ t.libelle }} ({{ formatMontant(t.montant) }})
                </option>
              </select>
              <span v-if="!transactionsDisponibles().length" class="muted">Aucune transaction disponible sur ce compte.</span>
              <span v-if="linkError" class="muted link-error">{{ linkError }}</span>
            </div>
          </td>
          <td class="text-right actions">
            <button class="link-button" @click="confirmerLiaison(c)">Confirmer</button>
            <button class="link-button" @click="annulerLiaison">Annuler</button>
          </td>
        </template>
        <template v-else>
          <td>{{ c.numero }}</td>
          <td>{{ c.beneficiaire || "—" }}</td>
          <td>{{ compteDe(c)?.libelle ?? "—" }}</td>
          <td>{{ formatDate(c.dateEmission) }}</td>
          <td class="amount text-right amount--negative">{{ formatMontant(c.montant) }}</td>
          <td>
            <span class="statut-badge" :class="`statut-badge--${c.statut.toLowerCase()}`">
              {{ libellesStatut[c.statut] }}
            </span>
            <span v-if="c.statut === 'ENCAISSE'" class="muted date-encaissement">
              ({{ formatDate(c.dateEncaissement) }} — {{ c.transactionGeneree ? "généré" : "lié" }})
            </span>
          </td>
          <td class="text-right actions">
            <template v-if="c.statut === 'EMIS'">
              <button class="link-button" @click="commencerEdition(c)">Modifier</button>
              <button class="link-button" @click="commencerEncaissement(c)">Encaisser</button>
              <button class="link-button" @click="commencerLiaison(c)">Lier…</button>
              <button class="link-button" @click="annulerCheque(c)">Annuler</button>
            </template>
            <template v-else-if="c.statut === 'ENCAISSE'">
              <button class="link-button" @click="annulerCheque(c)">Annuler</button>
            </template>
            <button class="link-button link-button--danger" @click="supprimerCheque(c)">Supprimer</button>
          </td>
        </template>
      </tr>
      <tr v-if="!store.cheques.length">
        <td colspan="7" class="muted">Aucun chèque pour le moment.</td>
      </tr>
    </tbody>
  </table>

  <form class="new-cheque" @submit.prevent="ajouterCheque">
    <h2>Émettre un chèque</h2>
    <div class="new-cheque__row">
      <input v-model="nouveauCheque.numero" type="text" placeholder="N° de chèque" required />
      <input v-model="nouveauCheque.beneficiaire" type="text" placeholder="Bénéficiaire" />
      <select v-model="nouveauCheque.compteId" required>
        <option value="" disabled>Compte…</option>
        <option v-for="compte in comptesStore.comptes" :key="compte.id" :value="compte.id">{{ compte.libelle }}</option>
      </select>
      <input v-model="nouveauCheque.dateEmission" type="date" required />
      <input v-model.number="nouveauCheque.montant" type="number" step="0.01" placeholder="Montant" required />
      <button type="submit">Émettre</button>
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

.cheques-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 40px;
}

.cheques-table th {
  text-align: left;
  font-size: 12px;
  color: var(--color-ink-soft);
  font-weight: 500;
  padding: 8px 10px;
  border-bottom: 1px solid var(--color-line);
}

.cheques-table td {
  padding: 10px 10px;
  border-bottom: 1px solid var(--color-line);
  font-size: 14px;
}

.text-right {
  text-align: right;
}

.statut-badge {
  display: inline-block;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--color-paper-raised);
  border: 1px solid var(--color-line);
  color: var(--color-ink-soft);
}

.statut-badge--emis {
  color: var(--color-accent);
  border-color: var(--color-accent);
}

.statut-badge--encaisse {
  color: var(--color-positive, #2f6b46);
  border-color: var(--color-positive, #2f6b46);
}

.statut-badge--annule {
  color: var(--color-ink-soft);
  text-decoration: line-through;
}

.date-encaissement {
  font-size: 12px;
  margin-left: 6px;
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

.actions {
  white-space: nowrap;
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

.link-button:hover {
  color: var(--color-accent);
}

.link-button--danger:hover {
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

.new-cheque {
  border-top: 1px solid var(--color-line);
  padding-top: 24px;
  max-width: 820px;
}

.new-cheque h2 {
  font-family: var(--font-display);
  font-size: 18px;
  margin: 0 0 14px;
}

.new-cheque__row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.new-cheque__row input,
.new-cheque__row select {
  flex: 1;
  min-width: 130px;
  padding: 9px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
}

.new-cheque__row button {
  padding: 9px 18px;
  background: var(--color-accent);
  color: var(--color-paper-raised);
  border: none;
  font-size: 14px;
}

.new-cheque__row button:hover {
  background: #26593f;
}
</style>
