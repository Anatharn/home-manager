<script setup>
import { onMounted, reactive, ref } from "vue";
import { useFormatsImportCsvStore } from "../stores/formatsImportCsv";
import { useComptesStore } from "../stores/comptes";
import {
  CHAMPS_MAPPING,
  DELIMITEURS,
  SEPARATEURS_DECIMAUX,
  FORMATS_DATE,
  configurationParDefaut,
  configurationVersFormulaire,
  formulaireVersConfiguration,
  parseConfiguration,
  champEstRequis,
} from "../utils/formatsImportCsv";

const store = useFormatsImportCsvStore();
const comptesStore = useComptesStore();

function formulaireVide() {
  const colonnes = {};
  const valeursParDefaut = {};
  CHAMPS_MAPPING.forEach(({ cle }) => {
    colonnes[cle] = "";
    valeursParDefaut[cle] = "";
  });
  return {
    nom: "",
    delimiteur: ",",
    separateurDecimal: ",",
    formatDate: "dd/MM/yyyy",
    lignesEnteteAIgnorer: 0,
    colonnes,
    valeursParDefaut,
    indicateurs: [],
  };
}

const editingId = ref(null);
const formulaire = reactive(formulaireVide());

onMounted(() => {
  store.fetchFormats();
  comptesStore.fetchComptes();
});

function reinitialiserFormulaire() {
  Object.assign(formulaire, formulaireVide());
  editingId.value = null;
}

function chargerFormatHerite() {
  Object.assign(formulaire, {
    nom: formulaire.nom || "Format hérité",
    ...configurationVersFormulaire(configurationParDefaut()),
  });
}

function commencerEdition(format) {
  editingId.value = format.id;
  Object.assign(formulaire, {
    nom: format.nom,
    ...configurationVersFormulaire(parseConfiguration(format)),
  });
}

async function enregistrer() {
  const payload = {
    nom: formulaire.nom,
    configuration: JSON.stringify(formulaireVersConfiguration(formulaire)),
  };
  if (editingId.value) {
    await store.updateFormat(editingId.value, payload);
  } else {
    await store.createFormat(payload);
  }
  reinitialiserFormulaire();
}

function supprimer(format) {
  store.deleteFormat(format.id);
  if (editingId.value === format.id) reinitialiserFormulaire();
}

function ajouterIndicateur() {
  formulaire.indicateurs.push({ colonne: "", libelle: "" });
}

function supprimerIndicateur(index) {
  formulaire.indicateurs.splice(index, 1);
}
</script>

<template>
  <header class="page-header">
    <h1>Formats d'import CSV</h1>
  </header>

  <p class="muted intro">
    Les extraits CSV fournis par les banques ne sont pas tous les mêmes : chaque format
    décrit le délimiteur, le format de date et les colonnes d'un extrait donné. Le format
    voulu se choisit ensuite depuis Transactions > Importer un CSV.
  </p>

  <p v-if="store.error" class="alert">{{ store.error }}</p>
  <p v-else-if="store.loading" class="muted">Chargement…</p>

  <section v-else class="formats-list">
    <article v-for="format in store.formats" :key="format.id" class="format-row">
      <span class="format-row__nom">{{ format.nom }}</span>
      <div class="format-row__actions">
        <button class="btn-link" @click="commencerEdition(format)">Modifier</button>
        <button class="btn-link btn-link--danger" @click="supprimer(format)">Supprimer</button>
      </div>
    </article>

    <p v-if="!store.formats.length" class="muted">
      Aucun format pour le moment — crée le premier ci-dessous.
    </p>
  </section>

  <form class="format-form" @submit.prevent="enregistrer">
    <h2>{{ editingId ? "Modifier le format" : "Nouveau format" }}</h2>

    <label class="format-form__field">
      <span>Nom</span>
      <input v-model="formulaire.nom" type="text" placeholder="ex : Banque X (compte courant)" required />
    </label>

    <div class="format-form__row">
      <label class="format-form__field">
        <span>Délimiteur</span>
        <select v-model="formulaire.delimiteur">
          <option v-for="d in DELIMITEURS" :key="d.valeur" :value="d.valeur">{{ d.label }}</option>
        </select>
      </label>

      <label class="format-form__field">
        <span>Séparateur décimal</span>
        <select v-model="formulaire.separateurDecimal">
          <option v-for="s in SEPARATEURS_DECIMAUX" :key="s.valeur" :value="s.valeur">{{ s.label }}</option>
        </select>
      </label>

      <label class="format-form__field">
        <span>Format de date</span>
        <select v-model="formulaire.formatDate">
          <option v-for="f in FORMATS_DATE" :key="f" :value="f">{{ f }}</option>
        </select>
      </label>

      <label class="format-form__field">
        <span>Lignes à ignorer avant l'en-tête</span>
        <input v-model.number="formulaire.lignesEnteteAIgnorer" type="number" min="0" step="1" />
      </label>
    </div>

    <h3>Colonnes du fichier CSV</h3>
    <p class="muted">
      Pour chaque champ, indique le(s) nom(s) d'en-tête possibles séparés par une virgule (le
      premier trouvé dans le fichier est utilisé — utile pour les variations d'accents/casse).
      Le compte peut en plus avoir une valeur par défaut (utilisée si la colonne est absente ou
      vide) : dans ce cas sa colonne n'est plus obligatoire.
    </p>

    <div class="format-form__mapping">
      <fieldset v-for="champ in CHAMPS_MAPPING" :key="champ.cle" class="format-form__mapping-champ">
        <legend>{{ champ.label }}<span v-if="champEstRequis(champ, formulaire)"> *</span></legend>
        <label class="format-form__field">
          <span>Colonne(s)</span>
          <input
            v-model="formulaire.colonnes[champ.cle]"
            type="text"
            :placeholder="champEstRequis(champ, formulaire) ? 'requis' : 'optionnel'"
            :required="champEstRequis(champ, formulaire)"
          />
        </label>
        <label v-if="champ.supporteValeurParDefaut" class="format-form__field">
          <span>Valeur par défaut si absente</span>
          <select v-model="formulaire.valeursParDefaut[champ.cle]">
            <option value="">Aucune</option>
            <option v-for="compte in comptesStore.comptes" :key="compte.id" :value="compte.code">
              {{ compte.code }} — {{ compte.libelle }}
            </option>
          </select>
        </label>
      </fieldset>
    </div>

    <h3>Indicateurs booléens (optionnel)</h3>
    <p class="muted">
      Colonnes « vrai/faux » dont la valeur, si vraie, est ajoutée au commentaire de la
      transaction (ex. « Achat extra ordinaire »).
    </p>

    <div class="format-form__indicateurs">
      <div v-for="(indicateur, index) in formulaire.indicateurs" :key="index" class="format-form__indicateur-row">
        <input v-model="indicateur.colonne" type="text" placeholder="Nom de la colonne" />
        <input v-model="indicateur.libelle" type="text" placeholder="Libellé ajouté au commentaire" />
        <button type="button" class="btn-link btn-link--danger" @click="supprimerIndicateur(index)">Retirer</button>
      </div>
      <button type="button" class="btn-link" @click="ajouterIndicateur">+ Ajouter un indicateur</button>
    </div>

    <div class="format-form__actions">
      <button type="submit">{{ editingId ? "Enregistrer" : "Créer" }}</button>
      <button type="button" class="btn-link" @click="chargerFormatHerite">
        Pré-remplir avec l'ancien format codé en dur
      </button>
      <button v-if="editingId" type="button" class="btn-link" @click="reinitialiserFormulaire">Annuler</button>
    </div>
  </form>
</template>

<style scoped>
.page-header {
  margin-bottom: 16px;
}

.page-header h1 {
  font-family: var(--font-display);
  font-size: 30px;
  margin: 0;
}

.intro {
  max-width: 640px;
  margin-bottom: 28px;
}

.formats-list {
  display: flex;
  flex-direction: column;
  max-width: 640px;
  margin-bottom: 40px;
  border-top: 1px solid var(--color-line);
}

.format-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--color-line);
}

.format-row__nom {
  font-size: 15px;
}

.format-row__actions {
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
  font-size: 13px;
}

.format-form {
  border-top: 1px solid var(--color-line);
  padding-top: 24px;
  max-width: 640px;
}

.format-form h2 {
  font-family: var(--font-display);
  font-size: 18px;
  margin: 0 0 14px;
}

.format-form h3 {
  font-size: 14px;
  margin: 22px 0 6px;
}

.format-form__row {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.format-form__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: var(--color-ink-soft);
  margin-bottom: 14px;
  flex: 1;
  min-width: 160px;
}

.format-form__field input,
.format-form__field select {
  padding: 9px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--color-ink);
}

.format-form__mapping {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 8px;
}

.format-form__mapping-champ {
  display: flex;
  flex-wrap: wrap;
  gap: 0 14px;
  border: 1px solid var(--color-line);
  padding: 4px 14px 0;
  margin: 0;
}

.format-form__mapping-champ legend {
  padding: 0 6px;
  font-size: 12px;
  color: var(--color-ink-soft);
}

.format-form__indicateurs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.format-form__indicateur-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.format-form__indicateur-row input {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid var(--color-line);
  background: var(--color-paper-raised);
  font-family: var(--font-body);
  font-size: 13px;
}

.format-form__actions {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 18px;
}

.format-form__actions button[type="submit"] {
  padding: 9px 18px;
  background: var(--color-accent);
  color: var(--color-paper-raised);
  border: none;
  font-size: 14px;
}

.format-form__actions button[type="submit"]:hover {
  background: #26593f;
}
</style>
