// Description et (dé)sérialisation des "formats d'import CSV" (voir FormatImportCsv côté
// backend) : chaque format décrit comment lire un extrait bancaire donné (les banques
// n'exportent pas toutes leurs CSV de la même façon) — délimiteur, séparateur décimal, format
// de date, et à quelle(s) colonne(s) correspond chaque champ de transaction.
//
// Le backend stocke la configuration telle quelle (colonne TEXT), en JSON ; ce module est la
// seule source de vérité sur la structure de ce JSON.

export const CHAMPS_MAPPING = [
  { cle: "date", label: "Date", requis: true },
  { cle: "libelle", label: "Libellé", requis: true },
  { cle: "montant", label: "Montant", requis: true },
  // Seul champ pouvant avoir une valeur par défaut : les autres n'ont pas de sens à défauter
  // (une date ou un montant par défaut créeraient des transactions incorrectes en silence).
  // Un défaut choisi le rend non requis : la colonne peut alors être absente du CSV.
  { cle: "compte", label: "Compte (code)", requis: true, supporteValeurParDefaut: true },
  { cle: "categorie", label: "Catégorie", requis: false },
  { cle: "tiers", label: "Tiers", requis: false },
  { cle: "commentaire", label: "Commentaire", requis: false },
  { cle: "numeroCheque", label: "N° chèque", requis: false },
];

export const DELIMITEURS = [
  { valeur: ",", label: "Virgule ( , )" },
  { valeur: ";", label: "Point-virgule ( ; )" },
  { valeur: "\t", label: "Tabulation" },
];

export const SEPARATEURS_DECIMAUX = [
  { valeur: ",", label: "Virgule (1234,56)" },
  { valeur: ".", label: "Point (1234.56)" },
];

export const FORMATS_DATE = ["dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy", "dd.MM.yyyy"];

// Configuration correspondant à l'ancien mapping codé en dur (avant que le format soit
// configurable) : sert de point de départ pré-rempli pour créer le premier format.
export function configurationParDefaut() {
  return {
    delimiteur: ",",
    separateurDecimal: ",",
    formatDate: "dd/MM/yyyy",
    // Nombre de lignes à sauter avant la ligne d'en-tête (certains extraits bancaires
    // commencent par quelques lignes d'informations avant la vraie ligne d'en-tête).
    lignesEnteteAIgnorer: 0,
    colonnes: {
      date: ["Date"],
      libelle: ["Détails", "Details", "Imputations"],
      montant: ["Total", "Montant"],
      compte: ["Compte"],
      categorie: ["Imputations"],
      tiers: ["Tiers"],
      commentaire: ["Commentaires", "Commentaire"],
      numeroCheque: ["N° Chèque", "N Cheque", "Numero Cheque"],
    },
    // Compte utilisé quand la colonne "compte" est absente du fichier ou vide sur la ligne
    // (voir CHAMPS_MAPPING : seul ce champ supporte une valeur par défaut).
    valeursParDefaut: {},
    indicateurs: [{ colonne: "Achat extra ordinaire", libelle: "Achat extra ordinaire" }],
  };
}

// Un format peut arriver mal formé (édité à la main côté base) : on retombe sur une
// configuration minimale plutôt que de planter l'import.
export function parseConfiguration(format) {
  try {
    const config = JSON.parse(format.configuration);
    return {
      delimiteur: config.delimiteur ?? ",",
      separateurDecimal: config.separateurDecimal ?? ",",
      formatDate: config.formatDate ?? "dd/MM/yyyy",
      lignesEnteteAIgnorer: Number.isInteger(config.lignesEnteteAIgnorer) ? config.lignesEnteteAIgnorer : 0,
      colonnes: config.colonnes ?? {},
      valeursParDefaut: config.valeursParDefaut ?? {},
      indicateurs: Array.isArray(config.indicateurs) ? config.indicateurs : [],
    };
  } catch {
    return configurationParDefaut();
  }
}

// Représentation "formulaire" : les listes de colonnes possibles s'éditent comme du texte
// libre séparé par des virgules (ex. "Détails, Details, Imputations").
export function configurationVersFormulaire(configuration) {
  const colonnes = {};
  const valeursParDefaut = {};
  CHAMPS_MAPPING.forEach(({ cle, supporteValeurParDefaut }) => {
    colonnes[cle] = (configuration.colonnes?.[cle] ?? []).join(", ");
    if (supporteValeurParDefaut) {
      valeursParDefaut[cle] = configuration.valeursParDefaut?.[cle] ?? "";
    }
  });
  return {
    delimiteur: configuration.delimiteur ?? ",",
    separateurDecimal: configuration.separateurDecimal ?? ",",
    formatDate: configuration.formatDate ?? "dd/MM/yyyy",
    lignesEnteteAIgnorer: configuration.lignesEnteteAIgnorer ?? 0,
    colonnes,
    valeursParDefaut,
    indicateurs: (configuration.indicateurs ?? []).map((i) => ({ ...i })),
  };
}

export function formulaireVersConfiguration(formulaire) {
  const colonnes = {};
  const valeursParDefaut = {};
  CHAMPS_MAPPING.forEach(({ cle, supporteValeurParDefaut }) => {
    colonnes[cle] = String(formulaire.colonnes?.[cle] ?? "")
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
    if (supporteValeurParDefaut) {
      const valeurParDefaut = String(formulaire.valeursParDefaut?.[cle] ?? "").trim();
      if (valeurParDefaut) valeursParDefaut[cle] = valeurParDefaut;
    }
  });
  return {
    delimiteur: formulaire.delimiteur,
    separateurDecimal: formulaire.separateurDecimal,
    formatDate: formulaire.formatDate,
    lignesEnteteAIgnorer: Math.max(0, Number.parseInt(formulaire.lignesEnteteAIgnorer, 10) || 0),
    colonnes,
    valeursParDefaut,
    indicateurs: (formulaire.indicateurs ?? []).filter((i) => i.colonne?.trim()),
  };
}

// Un champ requis (ex. compte) cesse de l'être si une valeur par défaut le couvre : la colonne
// peut alors être absente du CSV sans bloquer l'import.
export function champEstRequis(champ, formulaire) {
  if (champ.supporteValeurParDefaut && String(formulaire.valeursParDefaut?.[champ.cle] ?? "").trim()) {
    return false;
  }
  return champ.requis;
}

// Résout la valeur d'un champ dans une ligne CSV brute (objet {entete: valeur}) en tentant
// chaque nom de colonne possible dans l'ordre, insensible à la casse (variations d'accents/
// casse possibles selon l'export bancaire).
export function resoudreColonne(ligneBrute, nomsPossibles) {
  for (const nom of nomsPossibles ?? []) {
    const cle = Object.keys(ligneBrute).find((k) => k.toLowerCase() === nom.toLowerCase());
    if (cle && ligneBrute[cle]) return ligneBrute[cle];
  }
  return "";
}

// Résout la valeur d'un champ de la configuration (mapping "colonnes") pour une ligne donnée,
// en retombant sur la valeur par défaut du format si la colonne est absente ou vide.
export function resoudreValeur(ligneBrute, configuration, cle) {
  const brut = resoudreColonne(ligneBrute, configuration.colonnes?.[cle]);
  return brut || configuration.valeursParDefaut?.[cle] || "";
}
