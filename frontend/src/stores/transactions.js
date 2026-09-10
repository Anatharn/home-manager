import { defineStore } from "pinia";
import apiClient from "../api/client";

const TRI_DEFAUT = ["dateOperation,desc", "id,desc"];

export const useTransactionsStore = defineStore("transactions", {
  state: () => ({
    transactions: [], // contenu de la page courante uniquement
    page: 0,
    size: 50,
    totalPages: 0,
    totalElements: 0,
    compteId: null,
    loading: false,
    error: null,
  }),

  actions: {
    // Charge une page de transactions (paginé côté serveur : on n'envoie jamais tout
    // l'historique au front). Mémorise le contexte (compteId/page/size) pour permettre un
    // refetch() après création/modification/suppression.
    async fetchPage({ compteId = null, page = 0, size = this.size } = {}) {
      this.loading = true;
      this.error = null;
      this.compteId = compteId;
      this.page = page;
      this.size = size;
      try {
        const params = new URLSearchParams();
        params.set("page", page);
        params.set("size", size);
        TRI_DEFAUT.forEach((tri) => params.append("sort", tri));
        if (compteId) params.set("compteId", compteId);

        const response = await apiClient.get("/transactions", { params });
        this.transactions = response.data.content;
        this.totalPages = response.data.page.totalPages;
        this.totalElements = response.data.page.totalElements;
      } catch (err) {
        this.error = "Impossible de charger les transactions.";
      } finally {
        this.loading = false;
      }
    },

    refetch() {
      return this.fetchPage({ compteId: this.compteId, page: this.page, size: this.size });
    },

    async createTransaction(transaction) {
      const response = await apiClient.post("/transactions", transaction);
      return response.data;
    },

    async updateTransaction(id, transaction) {
      const response = await apiClient.put(`/transactions/${id}`, transaction);
      return response.data;
    },

    async deleteTransaction(id) {
      await apiClient.delete(`/transactions/${id}`);
    },

    // Récupère l'historique complet d'UN compte (hors pagination affichée), pour les besoins
    // ponctuels où il faut vraiment tout voir (ex. choisir une transaction existante à lier à
    // un chèque). Ne touche pas à l'état paginé de la page en cours.
    async fetchToutesPourCompte(compteId) {
      const params = new URLSearchParams();
      params.set("compteId", compteId);
      params.set("size", 1000);
      params.set("sort", "dateOperation,desc");
      const response = await apiClient.get("/transactions", { params });
      return response.data.content;
    },
  },
});
