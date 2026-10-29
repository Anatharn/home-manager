import { defineStore } from "pinia";
import apiClient from "../api/client";
import { messageFromError } from "../utils/errors";

export const useCategoriesStore = defineStore("categories", {
  state: () => ({
    categories: [],
    loading: false,
    error: null,
  }),

  actions: {
    async fetchCategories() {
      this.loading = true;
      this.error = null;
      try {
        const response = await apiClient.get("/categories");
        this.categories = response.data;
      } catch (err) {
        this.error = messageFromError(err, "Impossible de charger les catégories. Le backend est-il démarré ?");
      } finally {
        this.loading = false;
      }
    },

    async createCategorie(categorie) {
      const response = await apiClient.post("/categories", categorie);
      this.categories.push(response.data);
      return response.data;
    },

    async updateCategorie(id, categorie) {
      const response = await apiClient.put(`/categories/${id}`, categorie);
      const index = this.categories.findIndex((c) => c.id === id);
      if (index !== -1) {
        this.categories[index] = response.data;
      }
      return response.data;
    },

    async deleteCategorie(id) {
      await apiClient.delete(`/categories/${id}`);
      this.categories = this.categories.filter((c) => c.id !== id);
    },
  },
});
