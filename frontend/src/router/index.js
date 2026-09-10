import { createRouter, createWebHistory } from "vue-router";
import ComptesView from "../views/ComptesView.vue";
import TransactionsView from "../views/TransactionsView.vue";
import BanquesView from "../views/BanquesView.vue";
import CategoriesView from "../views/CategoriesView.vue";
import ChequesView from "../views/ChequesView.vue";

const routes = [
  { path: "/", redirect: "/comptes" },
  { path: "/comptes", name: "comptes", component: ComptesView },
  { path: "/transactions", name: "transactions-global", component: TransactionsView },
  {
    path: "/comptes/:compteId/transactions",
    name: "transactions",
    component: TransactionsView,
    props: true,
  },
  { path: "/banques", name: "banques", component: BanquesView },
  { path: "/categories", name: "categories", component: CategoriesView },
  { path: "/cheques", name: "cheques", component: ChequesView },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
