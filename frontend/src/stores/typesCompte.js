import { defineStore } from "pinia";
import apiClient from "../api/client";
import { messageFromError } from "../utils/errors";

export const useTypesCompteStore = defineStore("typesCompte", {
  state: () => ({
    typesCompte: [],
    loading: false,
    error: null,
  }),

  actions: {
    async fetchTypesCompte() {
      this.loading = true;
      this.error = null;
      try {
        const response = await apiClient.get("/types-compte");
        this.typesCompte = response.data;
      } catch (err) {
        this.error = messageFromError(err, "Impossible de charger les types de compte. Le backend est-il démarré ?");
      } finally {
        this.loading = false;
      }
    },

    async createTypeCompte(typeCompte) {
      const response = await apiClient.post("/types-compte", typeCompte);
      this.typesCompte.push(response.data);
      return response.data;
    },

    async updateTypeCompte(id, typeCompte) {
      const response = await apiClient.put(`/types-compte/${id}`, typeCompte);
      const index = this.typesCompte.findIndex((t) => t.id === id);
      if (index !== -1) {
        this.typesCompte[index] = response.data;
      }
      return response.data;
    },

    async deleteTypeCompte(id) {
      await apiClient.delete(`/types-compte/${id}`);
      this.typesCompte = this.typesCompte.filter((t) => t.id !== id);
    },
  },
});
