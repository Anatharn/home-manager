import axios from "axios";

// Le backend Spring est lancé localement par Tauri (voir src-tauri/src/spring_launcher.rs)
// et écoute toujours sur ce port en développement comme en production.
const apiClient = axios.create({
  baseURL: "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

export default apiClient;
