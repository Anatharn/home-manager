<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useTransactionsStore } from "../stores/transactions";
import { useCategoriesStore } from "../stores/categories";
import { useComptesStore } from "../stores/comptes";
import { useChequesStore } from "../stores/cheques";
import { useFormatsImportCsvStore } from "../stores/formatsImportCsv";
import { parseCsvAvecEntetes, parseMontant, parseDateAvecFormat } from "../utils/csv";
import { CHAMPS_MAPPING, resoudreColonne, resoudreValeur, parseConfiguration } from "../utils/formatsImportCsv";

const CLE_DERNIER_FORMAT = "importCsv.dernierFormatId";

const transactionsStore = useTransactionsStore();
const categoriesStore = useCategoriesStore();
const comptesStore = useComptesStore();
const chequesStore = useChequesStore();
const formatsStore = useFormatsImportCsvStore();

const emit = defineEmits(["imported"]);

const ouvert = ref(false);
const formatSelectionneId = ref(null);
const nomFichier = ref("");
const lignes = ref([]); // aperçu, une entrée par ligne du CSV
const entetesDetectees = ref([]); // en-têtes réellement lus dans le fichier (aide au diagnostic)
const importing = ref(false);
const resultat = ref(null); // { crees, ignorees, liees, echecs }

const configuration = computed(() => {
  const format = formatsStore.formats.find((f) => f.id === formatSelectionneId.value);
  return format ? parseConfiguration(format) : null;
});

const champsAffiches = computed(() => {
  if (!configuration.value) return [];
  return CHAMPS_MAPPING.filter(
    (c) => (configuration.value.colonnes[c.cle] ?? []).length || configuration.value.valeursParDefaut?.[c.cle]
  ).map((c) => ({
    label: c.label,
    colonnes: (configuration.value.colonnes[c.cle] ?? []).join(" / ") || "aucune colonne",
    valeurParDefaut: configuration.value.valeursParDefaut?.[c.cle],
  }));
});

// Quand des lignes sont en erreur, la cause est le plus souvent un format qui ne correspond pas au
// fichier (mauvais délimiteur, lignes d'en-tête à ignorer, noms de colonnes différents) : on
// confronte les colonnes attendues par le format aux en-têtes réellement lus.
const diagnostic = computed(() => {
  if (!configuration.value || !lignes.value.length) return null;
  if (lignes.value.every((l) => l.erreurs.length === 0)) return null;
  const entetesMinuscules = entetesDetectees.value.map((e) => e.toLowerCase());
  const colonnesIntrouvables = CHAMPS_MAPPING.filter((c) => c.requis)
    .filter((c) => !configuration.value.valeursParDefaut?.[c.cle])
    .map((c) => ({ label: c.label, attendues: configuration.value.colonnes[c.cle] ?? [] }))
    .filter((c) => !c.attendues.some((nom) => entetesMinuscules.includes(nom.toLowerCase())));
  return {
    entetes: entetesDetectees.value,
    colonnesIntrouvables,
    delimiteurSuspect: entetesDetectees.value.length <= 1,
  };
});

watch(formatSelectionneId, (id) => {
  lignes.value = [];
  entetesDetectees.value = [];
  nomFichier.value = "";
  resultat.value = null;
  if (id) localStorage.setItem(CLE_DERNIER_FORMAT, String(id));
});

function ouvrirPanneau() {
  ouvert.value = true;
  resultat.value = null;
  if (formatsStore.formats.length) {
    if (!formatSelectionneId.value) choisirFormatParDefaut();
  } else {
    formatsStore.fetchFormats().then(choisirFormatParDefaut);
  }
}

function choisirFormatParDefaut() {
  const dernierId = Number(localStorage.getItem(CLE_DERNIER_FORMAT));
  if (dernierId && formatsStore.formats.some((f) => f.id === dernierId)) {
    formatSelectionneId.value = dernierId;
  } else if (formatsStore.formats.length === 1) {
    formatSelectionneId.value = formatsStore.formats[0].id;
  }
}

function fermerPanneau() {
  ouvert.value = false;
  nomFichier.value = "";
  lignes.value = [];
  resultat.value = null;
}

function analyserLigne(ligneBrute, index, config) {
  const erreurs = [];

  const dateBrute = resoudreValeur(ligneBrute, config, "date");
  const dateOperation = parseDateAvecFormat(dateBrute, config.formatDate);
  if (!dateOperation) erreurs.push(`Date invalide : "${dateBrute}"`);

  const libelle = resoudreValeur(ligneBrute, config, "libelle");
  if (!libelle) erreurs.push("Libellé manquant");

  const montantBrut = resoudreValeur(ligneBrute, config, "montant");
  const montant = parseMontant(montantBrut, config.separateurDecimal);
  if (montant === null) erreurs.push(`Montant invalide : "${montantBrut}"`);

  const compteCode = resoudreValeur(ligneBrute, config, "compte").trim();
  const compte = compteCode
    ? comptesStore.comptes.find((c) => (c.code ?? "").trim() === compteCode)
    : null;
  if (!compteCode) erreurs.push("Compte (code) manquant");
  else if (!compte) erreurs.push(`Compte introuvable pour le code "${compteCode}"`);

  const categorieLibelle = resoudreValeur(ligneBrute, config, "categorie").trim();
  const categorieExistante = categorieLibelle
    ? categoriesStore.categories.some((c) => c.libelle.toLowerCase() === categorieLibelle.toLowerCase())
    : true;

  const tiers = resoudreValeur(ligneBrute, config, "tiers").trim();
  const commentaireBrut = resoudreValeur(ligneBrute, config, "commentaire").trim();
  const morceauxCommentaire = [];
  if (tiers) morceauxCommentaire.push(tiers);
  if (commentaireBrut) morceauxCommentaire.push(commentaireBrut);
  for (const indicateur of config.indicateurs ?? []) {
    const valeurBrute = resoudreColonne(ligneBrute, [indicateur.colonne]).trim();
    if (valeurBrute && !/^(false|faux|0)?$/i.test(valeurBrute)) {
      morceauxCommentaire.push(indicateur.libelle || indicateur.colonne);
    }
  }
  const commentaire = morceauxCommentaire.join(" — ") || null;

  const chequeNumeroBrut = resoudreValeur(ligneBrute, config, "numeroCheque").trim();
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

// Beaucoup d'extraits bancaires sont encodés en Windows-1252 (Latin-1) et non en UTF-8 : lus en
// UTF-8, leurs accents deviennent "�" ("Libellé" -> "Libell�") et les colonnes ne correspondent
// plus au format. On tente l'UTF-8 strict, et on retombe sur Windows-1252 s'il est invalide.
function decoderFichier(buffer) {
  try {
    return new TextDecoder("utf-8", { fatal: true }).decode(buffer);
  } catch {
    return new TextDecoder("windows-1252").decode(buffer);
  }
}

async function onFichierChoisi(event) {
  const fichier = event.target.files?.[0];
  if (!fichier || !configuration.value) return;
  nomFichier.value = fichier.name;
  resultat.value = null;
  const texte = decoderFichier(await fichier.arrayBuffer());
  const { entetes, lignes: lignesBrutes } = parseCsvAvecEntetes(
    texte,
    configuration.value.delimiteur,
    configuration.value.lignesEnteteAIgnorer
  );
  entetesDetectees.value = entetes;
  lignes.value = lignesBrutes.map((l, i) => analyserLigne(l, i, configuration.value));

  const enErreur = lignes.value.filter((l) => l.erreurs.length);
  if (enErreur.length) {
    console.warn(
      `[Import CSV] ${enErreur.length}/${lignes.value.length} ligne(s) en erreur. En-têtes lus :`,
      entetes,
      "Configuration :",
      configuration.value,
      "Détail :",
      enErreur.map((l) => ({ ligne: l.index + 1, erreurs: l.erreurs, brut: lignesBrutes[l.index] }))
    );
  }
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
  const echecs = []; // lignes refusées par le backend : { ligne, libelle, message }

  for (const ligne of valides) {
    try {
      liees += await importerLigne(ligne, categoriesCreees);
      crees++;
    } catch (err) {
      const message = messageEchecBackend(err);
      console.error(`[Import CSV] Échec ligne ${ligne.index + 1} (${ligne.libelle}) :`, message, err);
      echecs.push({ ligne: ligne.index + 1, libelle: ligne.libelle, message });
    }
  }

  resultat.value = {
    crees,
    ignorees: lignes.value.length - valides.length,
    liees,
    echecs,
  };
  lignes.value = [];
  nomFichier.value = "";
  importing.value = false;
  if (crees > 0) emit("imported");
}

function messageEchecBackend(err) {
  const data = err.response?.data;
  const detail = (typeof data === "string" ? data : data?.message || data?.error) || err.message;
  return err.response ? `HTTP ${err.response.status} — ${detail}` : detail;
}

// Crée la transaction d'une ligne (et sa catégorie si besoin) ; renvoie 1 si un chèque a été lié.
async function importerLigne(ligne, categoriesCreees) {
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

  if (ligne.chequeNumero) {
    const chequeCorrespondant = chequesStore.cheques.find(
      (c) => c.statut === "EMIS" && c.numero === ligne.chequeNumero && c.compte?.id === ligne.compte.id
    );
    if (chequeCorrespondant) {
      try {
        await chequesStore.lierTransaction(chequeCorrespondant.id, transaction.id);
        return 1;
      } catch {
        // Le chèque aura peut-être été lié entre-temps par une autre ligne : pas bloquant.
      }
    }
  }
  return 0;
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

      <div v-if="resultat?.echecs?.length" class="import-csv__diagnostic">
        <strong>{{ resultat.echecs.length }} ligne(s) refusée(s) par le serveur :</strong>
        <ul>
          <li v-for="e in resultat.echecs" :key="e.ligne">Ligne {{ e.ligne }} ({{ e.libelle }}) : {{ e.message }}</li>
        </ul>
      </div>

      <p v-if="formatsStore.loading" class="muted">Chargement des formats…</p>

      <p v-else-if="!formatsStore.formats.length" class="muted">
        Aucun format d'import n'est configuré. <RouterLink to="/formats-import-csv">Crée-en un</RouterLink>
        pour décrire les colonnes de tes extraits bancaires.
      </p>

      <template v-else>
        <div class="import-csv__row">
          <label class="import-csv__format-label">
            <span>Format</span>
            <select v-model="formatSelectionneId">
              <option :value="null" disabled>Choisir un format…</option>
              <option v-for="format in formatsStore.formats" :key="format.id" :value="format.id">
                {{ format.nom }}
              </option>
            </select>
          </label>
        </div>

        <div class="import-csv__row">
          <input
            type="file"
            accept=".csv,text/csv"
            :disabled="!formatSelectionneId"
            @change="onFichierChoisi"
          />
          <span v-if="nomFichier" class="muted">{{ nomFichier }}</span>
        </div>

        <p v-if="configuration" class="muted import-csv__aide">
          Colonnes attendues :
          <template v-for="(champ, index) in champsAffiches" :key="champ.label">
            {{ champ.label }} ({{ champ.colonnes }}<span v-if="champ.valeurParDefaut"> — défaut : "{{ champ.valeurParDefaut }}"</span>)<span v-if="index < champsAffiches.length - 1">, </span>
          </template>
        </p>
      </template>

      <div v-if="lignes.length" class="import-csv__preview">
        <div v-if="diagnostic" class="import-csv__diagnostic">
          <p>
            <strong>En-têtes lus dans le fichier :</strong>
            {{ diagnostic.entetes.length ? diagnostic.entetes.map((e) => `"${e}"`).join(", ") : "aucun" }}
          </p>
          <p v-if="diagnostic.delimiteurSuspect">
            Une seule colonne détectée : le délimiteur du format ne correspond probablement pas à celui du fichier.
          </p>
          <p v-for="c in diagnostic.colonnesIntrouvables" :key="c.label">
            Colonne « {{ c.label }} » introuvable (attendue : {{ c.attendues.join(" / ") || "aucune colonne configurée" }}).
          </p>
          <p v-if="diagnostic.colonnesIntrouvables.length" class="muted">
            Vérifie le format (délimiteur, lignes d'en-tête à ignorer, noms de colonnes) dans
            <RouterLink to="/formats-import-csv">les formats d'import</RouterLink>.
          </p>
        </div>

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
                <span v-if="l.erreurs.length" class="import-csv__statut import-csv__statut--erreur">
                  {{ l.erreurs.join(" / ") }}
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

.import-csv__format-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-ink-soft);
}

.import-csv__format-label select {
  padding: 8px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--color-ink);
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

.import-csv__diagnostic {
  background: #F7E9E6;
  border: 1px solid var(--color-negative);
  padding: 10px 14px;
  font-size: 13px;
  margin: 0 0 14px;
}

.import-csv__diagnostic p,
.import-csv__diagnostic ul {
  margin: 4px 0;
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
