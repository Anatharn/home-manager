import { defineStore } from "pinia";
import apiClient from "../api/client";
import { messageFromError } from "../utils/errors";

export const useFormatsImportCsvStore = defineStore("formatsImportCsv", {
  state: () => ({
    formats: [],
    loading: false,
    error: null,
  }),

  actions: {
    async fetchFormats() {
      this.loading = true;
      this.error = null;
      try {
        const response = await apiClient.get("/formats-import-csv");
        this.formats = response.data;
      } catch (err) {
        this.error = messageFromError(err, "Impossible de charger les formats d'import CSV. Le backend est-il démarré ?");
      } finally {
        this.loading = false;
      }
    },

    async createFormat(format) {
      const response = await apiClient.post("/formats-import-csv", format);
      this.formats.push(response.data);
      return response.data;
    },

    async updateFormat(id, format) {
      const response = await apiClient.put(`/formats-import-csv/${id}`, format);
      const index = this.formats.findIndex((f) => f.id === id);
      if (index !== -1) {
        this.formats[index] = response.data;
      }
      return response.data;
    },

    async deleteFormat(id) {
      await apiClient.delete(`/formats-import-csv/${id}`);
      this.formats = this.formats.filter((f) => f.id !== id);
    },
  },
});
