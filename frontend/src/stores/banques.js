import { defineStore } from "pinia";
import apiClient from "../api/client";
import { messageFromError } from "../utils/errors";

export const useBanquesStore = defineStore("banques", {
  state: () => ({
    banques: [],
    loading: false,
    error: null,
  }),

  actions: {
    async fetchBanques() {
      this.loading = true;
      this.error = null;
      try {
        const response = await apiClient.get("/banques");
        this.banques = response.data;
      } catch (err) {
        this.error = messageFromError(err, "Impossible de charger les banques. Le backend est-il démarré ?");
      } finally {
        this.loading = false;
      }
    },

    async createBanque(banque) {
      const response = await apiClient.post("/banques", banque);
      this.banques.push(response.data);
      return response.data;
    },

    async updateBanque(id, banque) {
      const response = await apiClient.put(`/banques/${id}`, banque);
      const index = this.banques.findIndex((b) => b.id === id);
      if (index !== -1) {
        this.banques[index] = response.data;
      }
      return response.data;
    },

    async deleteBanque(id) {
      await apiClient.delete(`/banques/${id}`);
      this.banques = this.banques.filter((b) => b.id !== id);
    },
  },
});
