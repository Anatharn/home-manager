<script setup>
import { ref } from "vue";
import { useTransactionsStore } from "../stores/transactions";
import { useCategoriesStore } from "../stores/categories";
import { useComptesStore } from "../stores/comptes";
import { useChequesStore } from "../stores/cheques";
import { parseCsvAvecEntetes, parseMontantFr, parseDateFr } from "../utils/csv";

const transactionsStore = useTransactionsStore();
const categoriesStore = useCategoriesStore();
const comptesStore = useComptesStore();
const chequesStore = useChequesStore();

const emit = defineEmits(["imported"]);

const ouvert = ref(false);
const nomFichier = ref("");
const lignes = ref([]); // aperçu, une entrée par ligne du CSV
const importing = ref(false);
const resultat = ref(null); // { crees, ignorees, cheques }

function ouvrirPanneau() {
  ouvert.value = true;
  resultat.value = null;
}

function fermerPanneau() {
  ouvert.value = false;
  nomFichier.value = "";
  lignes.value = [];
  resultat.value = null;
}

// Résout une valeur de colonne quel que soit le nom exact de l'en-tête (variations d'accents/casse
// possibles selon l'export bancaire).
function colonne(ligneBrute, ...nomsPossibles) {
  for (const nom of nomsPossibles) {
    const cle = Object.keys(ligneBrute).find((k) => k.toLowerCase() === nom.toLowerCase());
    if (cle) return ligneBrute[cle];
  }
  return "";
}

function analyserLigne(ligneBrute, index) {
  const erreurs = [];

  const dateBrute = colonne(ligneBrute, "Date");
  const dateOperation = parseDateFr(dateBrute);
  if (!dateOperation) erreurs.push(`Date invalide : "${dateBrute}"`);

  const libelle = colonne(ligneBrute, "Détails", "Details") || colonne(ligneBrute, "Imputations");
  if (!libelle) erreurs.push("Libellé manquant");

  const montantBrut = colonne(ligneBrute, "Total", "Montant");
  const montant = parseMontantFr(montantBrut);
  if (montant === null) erreurs.push(`Montant invalide : "${montantBrut}"`);

  const compteCode = colonne(ligneBrute, "Compte").trim();
  const compte = compteCode
    ? comptesStore.comptes.find((c) => (c.code ?? "").trim() === compteCode)
    : null;
  if (!compteCode) erreurs.push("Compte (code) manquant");
  else if (!compte) erreurs.push(`Compte introuvable pour le code "${compteCode}"`);

  const categorieLibelle = colonne(ligneBrute, "Imputations").trim();
  const categorieExistante = categorieLibelle
    ? categoriesStore.categories.some((c) => c.libelle.toLowerCase() === categorieLibelle.toLowerCase())
    : true;

  const tiers = colonne(ligneBrute, "Tiers").trim();
  const commentaireBrut = colonne(ligneBrute, "Commentaires", "Commentaire").trim();
  const achatExtra = colonne(ligneBrute, "Achat extra ordinaire").trim();
  const morceauxCommentaire = [];
  if (tiers) morceauxCommentaire.push(tiers);
  if (commentaireBrut) morceauxCommentaire.push(commentaireBrut);
  if (achatExtra && !/^(false|faux|0)?$/i.test(achatExtra)) morceauxCommentaire.push("Achat extra ordinaire");
  const commentaire = morceauxCommentaire.join(" — ") || null;

  const chequeNumeroBrut = colonne(ligneBrute, "N° Chèque", "N Cheque", "Numero Cheque").trim();
  const chequeNumero = chequeNumeroBrut && chequeNumeroBrut !== "_" ? chequeNumeroBrut : null;

  return {
    index,
    dateBrute,
    dateOperation,
    libelle,
    montantBrut,
    montant,
    compteCode,
    compte,
    categorieLibelle,
    categorieExistante,
    commentaire,
    chequeNumero,
    erreurs,
  };
}

async function onFichierChoisi(event) {
  const fichier = event.target.files?.[0];
  if (!fichier) return;
  nomFichier.value = fichier.name;
  resultat.value = null;
  const texte = await fichier.text();
  const { lignes: lignesBrutes } = parseCsvAvecEntetes(texte);
  lignes.value = lignesBrutes.map((l, i) => analyserLigne(l, i));
}

function lignesValides() {
  return lignes.value.filter((l) => l.erreurs.length === 0);
}

async function confirmerImport() {
  const valides = lignesValides();
  if (!valides.length || importing.value) return;
  importing.value = true;

  // Évite de créer plusieurs fois la même catégorie au sein d'un même import.
  const categoriesCreees = new Map();

  let crees = 0;
  let liees = 0;

  for (const ligne of valides) {
    let categorieId = null;
    if (ligne.categorieLibelle) {
      const existante = categoriesStore.categories.find(
        (c) => c.libelle.toLowerCase() === ligne.categorieLibelle.toLowerCase()
      );
      if (existante) {
        categorieId = existante.id;
      } else if (categoriesCreees.has(ligne.categorieLibelle.toLowerCase())) {
        categorieId = categoriesCreees.get(ligne.categorieLibelle.toLowerCase());
      } else {
        const nouvelle = await categoriesStore.createCategorie({
          libelle: ligne.categorieLibelle,
          couleur: "#8e9592",
        });
        categoriesCreees.set(ligne.categorieLibelle.toLowerCase(), nouvelle.id);
        categorieId = nouvelle.id;
      }
    }

    const transaction = await transactionsStore.createTransaction({
      libelle: ligne.libelle,
      montant: ligne.montant,
      dateOperation: ligne.dateOperation,
      compte: { id: ligne.compte.id },
      categorie: categorieId ? { id: categorieId } : null,
      commentaire: ligne.commentaire,
    });
    crees++;

    if (ligne.chequeNumero) {
      const chequeCorrespondant = chequesStore.cheques.find(
        (c) => c.statut === "EMIS" && c.numero === ligne.chequeNumero && c.compte?.id === ligne.compte.id
      );
      if (chequeCorrespondant) {
        try {
          await chequesStore.lierTransaction(chequeCorrespondant.id, transaction.id);
          liees++;
        } catch {
          // Le chèque aura peut-être été lié entre-temps par une autre ligne : pas bloquant.
        }
      }
    }
  }

  resultat.value = {
    crees,
    ignorees: lignes.value.length - valides.length,
    liees,
  };
  lignes.value = [];
  nomFichier.value = "";
  importing.value = false;
  if (crees > 0) emit("imported");
}
</script>

<template>
  <div class="import-csv">
    <button v-if="!ouvert" class="link-button link-button--accent" @click="ouvrirPanneau">
      Importer un CSV
    </button>

    <div v-else class="import-csv__panel">
      <div class="import-csv__header">
        <h2>Importer des transactions depuis un CSV</h2>
        <button class="link-button" @click="fermerPanneau">Fermer</button>
      </div>

      <p v-if="resultat" class="import-csv__resultat">
        {{ resultat.crees }} transaction(s) créée(s)<span v-if="resultat.liees"> dont {{ resultat.liees }} liée(s) à un chèque</span>.
        <span v-if="resultat.ignorees">{{ resultat.ignorees }} ligne(s) ignorée(s) (erreurs).</span>
      </p>

      <div class="import-csv__row">
        <input type="file" accept=".csv,text/csv" @change="onFichierChoisi" />
        <span v-if="nomFichier" class="muted">{{ nomFichier }}</span>
      </div>

      <p class="muted import-csv__aide">
        Colonnes attendues : Date, Détails (libellé), Total (montant), Compte (code du compte),
        Imputations (catégorie, créée automatiquement si nouvelle), Commentaires, N° Chèque (optionnel).
      </p>

      <div v-if="lignes.length" class="import-csv__preview">
        <table class="import-csv__table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Compte</th>
              <th>Libellé</th>
              <th>Catégorie</th>
              <th class="text-right">Montant</th>
              <th>Chèque</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in lignes" :key="l.index" :class="{ 'import-csv__row--erreur': l.erreurs.length }">
              <td>{{ l.dateBrute }}</td>
              <td>{{ l.compte?.libelle ?? l.compteCode }}</td>
              <td>{{ l.libelle }}</td>
              <td>
                {{ l.categorieLibelle || "—" }}
                <span v-if="l.categorieLibelle && !l.categorieExistante" class="muted">(nouvelle)</span>
              </td>
              <td class="text-right">{{ l.montant ?? l.montantBrut }}</td>
              <td>{{ l.chequeNumero ?? "—" }}</td>
              <td>
                <span v-if="l.erreurs.length" class="import-csv__statut import-csv__statut--erreur" :title="l.erreurs.join(' / ')">
                  {{ l.erreurs[0] }}
                </span>
                <span v-else class="import-csv__statut import-csv__statut--ok">OK</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="import-csv__actions">
          <button
            class="import-csv__submit"
            :disabled="!lignesValides().length || importing"
            @click="confirmerImport"
          >
            {{ importing ? "Import en cours…" : `Importer ${lignesValides().length} transaction(s)` }}
          </button>
          <span v-if="lignes.length !== lignesValides().length" class="muted">
            {{ lignes.length - lignesValides().length }} ligne(s) en erreur seront ignorées.
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.import-csv {
  margin-bottom: 28px;
}

.import-csv__panel {
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  padding: 18px 20px;
}

.import-csv__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.import-csv__header h2 {
  font-family: var(--font-display);
  font-size: 18px;
  margin: 0;
}

.import-csv__row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.import-csv__aide {
  font-size: 12px;
  margin: 0 0 14px;
}

.import-csv__resultat {
  background: #EAF2EC;
  border: 1px solid var(--color-accent);
  padding: 10px 14px;
  font-size: 13px;
  margin: 0 0 14px;
}

.import-csv__preview {
  border-top: 1px solid var(--color-line);
  padding-top: 12px;
}

.import-csv__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-bottom: 14px;
}

.import-csv__table th {
  text-align: left;
  font-size: 11px;
  color: var(--color-ink-soft);
  font-weight: 500;
  padding: 6px 8px;
  border-bottom: 1px solid var(--color-line);
}

.import-csv__table td {
  padding: 6px 8px;
  border-bottom: 1px solid var(--color-line);
}

.import-csv__row--erreur {
  background: #F7E9E6;
}

.import-csv__statut {
  font-size: 12px;
}

.import-csv__statut--ok {
  color: var(--color-positive, #2f6b46);
}

.import-csv__statut--erreur {
  color: var(--color-negative);
}

.import-csv__actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.import-csv__submit {
  padding: 9px 18px;
  background: var(--color-accent);
  color: var(--color-paper-raised);
  border: none;
  font-size: 14px;
}

.import-csv__submit:hover:not(:disabled) {
  background: #26593f;
}

.import-csv__submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.text-right {
  text-align: right;
}

.muted {
  color: var(--color-ink-soft);
  font-size: 13px;
}

.link-button {
  background: none;
  border: none;
  color: var(--color-ink-soft);
  font-size: 13px;
  padding: 0;
  cursor: pointer;
}

.link-button--accent {
  color: var(--color-accent);
  font-size: 14px;
}

.link-button:hover {
  text-decoration: underline;
}
</style>
