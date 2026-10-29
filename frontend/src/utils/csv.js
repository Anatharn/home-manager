// Petit parseur CSV (RFC 4180 minimal) : gere les champs entre guillemets contenant des
// delimiteurs ou des guillemets echappes (""), ce qu'un simple split() ne sait pas faire.
// Le delimiteur est parametrable car les extraits bancaires ne l'utilisent pas tous
// (virgule, point-virgule, tabulation...) — voir formatsImportCsv.js.
export function parseCsv(text, delimiteur = ",") {
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
    } else if (c === delimiteur) {
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
// exports) sont ignorees par nos mappings nommes. "lignesAIgnorer" saute un nombre de lignes
// avant l'en-tete (certains extraits bancaires commencent par quelques lignes d'informations
// avant la vraie ligne d'en-tete).
export function parseCsvAvecEntetes(text, delimiteur = ",", lignesAIgnorer = 0) {
  const rows = parseCsv(text, delimiteur).slice(lignesAIgnorer);
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

// "700,00 EUR" / "-1 350,92 EUR" -> 700 / -1350.92 (separateur decimal parametrable : certains
// exports utilisent "1234.56" plutot que "1234,56").
export function parseMontant(brut, separateurDecimal = ",") {
  if (!brut) return null;
  let s = String(brut).trim();
  // retire tout sauf chiffres, signe et separateurs (espaces/symbole monetaire compris)
  s = s.replace(/[^0-9,.\-]/g, "");
  if (separateurDecimal === ",") {
    s = s.replace(/\./g, "").replace(",", ".");
  } else {
    s = s.replace(/,/g, "");
  }
  if (s === "" || s === "-") return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

// Parse une date selon un format a jetons dd/MM/yyyy (jetons "dd", "MM", "yyyy", separateur
// libre parmi / - .), ex. "05/01/2026" avec "dd/MM/yyyy" -> "2026-01-05".
export function parseDateAvecFormat(brut, format = "dd/MM/yyyy") {
  const valeur = String(brut ?? "").trim();
  if (!valeur) return null;

  const jetons = format.match(/dd|MM|yyyy/g);
  if (!jetons || jetons.length !== 3) return null;

  const regexFormat = format
    .replace(/[.]/g, "\\.")
    .replace(/dd|MM|yyyy/g, (jeton) => (jeton === "yyyy" ? "(\\d{4})" : "(\\d{1,2})"));

  const m = new RegExp(`^${regexFormat}$`).exec(valeur);
  if (!m) return null;

  const parJeton = {};
  jetons.forEach((jeton, i) => {
    parJeton[jeton] = m[i + 1];
  });

  return `${parJeton.yyyy}-${parJeton.MM.padStart(2, "0")}-${parJeton.dd.padStart(2, "0")}`;
}
