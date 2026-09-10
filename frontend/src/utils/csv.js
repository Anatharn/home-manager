// Petit parseur CSV (RFC 4180 minimal) : gere les champs entre guillemets contenant des
// virgules ou des guillemets echappes (""), ce qu'un simple split(",") ne sait pas faire.
// Necessaire ici car les releves bancaires exportent des montants du type "700,00 EUR"
// (virgule decimale) dans un fichier par ailleurs delimite par des virgules.
export function parseCsv(text) {
  // Retire le BOM UTF-8 eventuel (fichiers exportes depuis Excel/Windows).
  if (text.charCodeAt(0) === 0xfeff) {
    text = text.slice(1);
  }

  const rows = [];
  let row = [];
  let field = "";
  let inQuotes = false;

  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (inQuotes) {
      if (c === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        field += c;
      }
      continue;
    }
    if (c === '"') {
      inQuotes = true;
    } else if (c === ",") {
      row.push(field);
      field = "";
    } else if (c === "\r") {
      // ignore, gere par le \n qui suit
    } else if (c === "\n") {
      row.push(field);
      rows.push(row);
      row = [];
      field = "";
    } else {
      field += c;
    }
  }
  if (field.length > 0 || row.length > 0) {
    row.push(field);
    rows.push(row);
  }

  return rows.filter((r) => r.some((cell) => cell.trim() !== ""));
}

// Transforme les lignes brutes en objets {entete: valeur}, a partir de la premiere ligne
// utilisee comme en-tete. Les colonnes sans en-tete nomme (ex. la colonne "FALSE" de certains
// exports) sont ignorees par nos mappings nommes.
export function parseCsvAvecEntetes(text) {
  const rows = parseCsv(text);
  if (rows.length === 0) return { entetes: [], lignes: [] };
  const entetes = rows[0].map((h) => h.trim());
  const lignes = rows.slice(1).map((r) => {
    const obj = {};
    entetes.forEach((h, i) => {
      obj[h] = (r[i] ?? "").trim();
    });
    return obj;
  });
  return { entetes, lignes };
}

// "700,00 EUR" / "-1 350,92 EUR" -> 700 / -1350.92 (accepte aussi le symbole EUR)
export function parseMontantFr(brut) {
  if (!brut) return null;
  let s = String(brut).trim();
  s = s.replace(/[^0-9,.\-]/g, "");
  s = s.replace(",", ".");
  if (s === "" || s === "-") return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

// "05/01/2026" -> "2026-01-05"
export function parseDateFr(brut) {
  const m = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(String(brut ?? "").trim());
  if (!m) return null;
  const [, j, mo, a] = m;
  return `${a}-${mo.padStart(2, "0")}-${j.padStart(2, "0")}`;
}
