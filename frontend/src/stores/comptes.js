import { defineStore } from "pinia";
import apiClient from "../api/client";
import { messageFromError } from "../utils/errors";

export const useComptesStore = defineStore("comptes", {
  state: () => ({
    comptes: [],
    loading: false,
    error: null,
  }),

  actions: {
    async fetchComptes() {
      this.loading = true;
      this.error = null;
      try {
        const response = await apiClient.get("/comptes");
        this.comptes = response.data;
      } catch (err) {
        this.error = messageFromError(err, "Impossible de charger les comptes. Le backend est-il démarré ?");
      } finally {
        this.loading = false;
      }
    },

    async createCompte(compte) {
      const response = await apiClient.post("/comptes", compte);
      this.comptes.push(response.data);
      return response.data;
    },

    async updateCompte(id, compte) {
      const response = await apiClient.put(`/comptes/${id}`, compte);
      const index = this.comptes.findIndex((c) => c.id === id);
      if (index !== -1) {
        this.comptes[index] = response.data;
      }
      return response.data;
    },

    async deleteCompte(id) {
      await apiClient.delete(`/comptes/${id}`);
      this.comptes = this.comptes.filter((c) => c.id !== id);
    },
  },
});
