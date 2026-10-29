import { defineStore } from "pinia";
import apiClient from "../api/client";

export const useConfigurationStore = defineStore("configuration", {
  state: () => ({
    database: {
      hote: "",
      port: 3306,
      nomBase: "",
      utilisateur: "",
      motDePasse: "",
      configuree: false,
    },
    loading: false,
    saving: false,
    error: null,
    saveError: null,
    saved: false,
  }),

  actions: {
    async fetchDatabase() {
      this.loading = true;
      this.error = null;
      try {
        const response = await apiClient.get("/configuration/database");
        this.database = { ...response.data, motDePasse: "" };
      } catch (err) {
        this.error = "Impossible de charger la configuration. Le backend est-il démarré ?";
      } finally {
        this.loading = false;
      }
    },

    async saveDatabase(config) {
      this.saving = true;
      this.saveError = null;
      this.saved = false;
      try {
        const response = await apiClient.put("/configuration/database", config);
        this.database = { ...response.data, motDePasse: "" };
        this.saved = true;
      } catch (err) {
        this.saveError =
          err.response?.data?.message ?? "Impossible d'enregistrer la configuration.";
      } finally {
        this.saving = false;
      }
    },
  },
});
