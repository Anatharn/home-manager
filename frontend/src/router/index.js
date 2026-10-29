import { createRouter, createWebHistory } from "vue-router";
import ComptesView from "../views/ComptesView.vue";
import SoldesView from "../views/SoldesView.vue";
import TransactionsView from "../views/TransactionsView.vue";
import BanquesView from "../views/BanquesView.vue";
import CategoriesView from "../views/CategoriesView.vue";
import ChequesView from "../views/ChequesView.vue";
import TypesCompteView from "../views/TypesCompteView.vue";
import ConfigurationView from "../views/ConfigurationView.vue";
import FormatsImportCsvView from "../views/FormatsImportCsvView.vue";

const routes = [
  { path: "/", redirect: "/soldes" },
  { path: "/comptes", name: "comptes", component: ComptesView },
  { path: "/soldes", name: "soldes", component: SoldesView },
  { path: "/transactions", name: "transactions-global", component: TransactionsView },
  {
    path: "/comptes/:compteId/transactions",
    name: "transactions",
    component: TransactionsView,
    props: true,
  },
  { path: "/banques", name: "banques", component: BanquesView },
  { path: "/types-compte", name: "types-compte", component: TypesCompteView },
  { path: "/categories", name: "categories", component: CategoriesView },
  { path: "/cheques", name: "cheques", component: ChequesView },
  { path: "/formats-import-csv", name: "formats-import-csv", component: FormatsImportCsvView },
  { path: "/configuration", name: "configuration", component: ConfigurationView },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
