import { defineStore } from "pinia";
import apiClient from "../api/client";

export const useChequesStore = defineStore("cheques", {
  state: () => ({
    cheques: [],
    loading: false,
    error: null,
  }),

  actions: {
    async fetchAll() {
      this.loading = true;
      this.error = null;
      try {
        const response = await apiClient.get("/cheques");
        this.cheques = response.data;
      } catch (err) {
        this.error = "Impossible de charger les chèques. Le backend est-il démarré ?";
      } finally {
        this.loading = false;
      }
    },

    async createCheque(cheque) {
      const response = await apiClient.post("/cheques", cheque);
      this.cheques.unshift(response.data);
      return response.data;
    },

    async updateCheque(id, cheque) {
      const response = await apiClient.put(`/cheques/${id}`, cheque);
      this._remplacer(id, response.data);
      return response.data;
    },

    async encaisserCheque(id, dateEncaissement, categorieId) {
      const response = await apiClient.post(`/cheques/${id}/encaisser`, {
        dateEncaissement,
        categorieId: categorieId || null,
      });
      this._remplacer(id, response.data);
      return response.data;
    },

    async lierTransaction(id, transactionId) {
      const response = await apiClient.post(`/cheques/${id}/lier-transaction`, { transactionId });
      this._remplacer(id, response.data);
      return response.data;
    },

    async annulerCheque(id) {
      const response = await apiClient.post(`/cheques/${id}/annuler`);
      this._remplacer(id, response.data);
      return response.data;
    },

    async deleteCheque(id) {
      await apiClient.delete(`/cheques/${id}`);
      this.cheques = this.cheques.filter((c) => c.id !== id);
    },

    _remplacer(id, cheque) {
      const index = this.cheques.findIndex((c) => c.id === id);
      if (index !== -1) {
        this.cheques[index] = cheque;
      }
    },
  },
});
