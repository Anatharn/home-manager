// Le backend renvoie { code: "database_unavailable", message: "..." } (503) quand il tourne mais
// n'arrive pas à joindre la base configurée (voir GlobalExceptionHandler côté backend) — dans ce
// cas on affiche son message, plus utile que le message générique "le backend est-il démarré ?"
// qui ne s'applique pas puisque le backend, lui, répond bien.
export function messageFromError(err, fallback) {
  if (err.response?.data?.code === "database_unavailable") {
    return err.response.data.message;
  }
  return fallback;
}
