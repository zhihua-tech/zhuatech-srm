/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../views/LoginView.vue";
import PortalDashboard from "../views/portal/PortalDashboard.vue";
import AdminDashboard from "../views/admin/AdminDashboard.vue";
import SupplierList from "../views/admin/SupplierList.vue";
import SourcingAdmin from "../views/admin/SourcingAdmin.vue";
import SystemAdmin from "../views/admin/SystemAdmin.vue";
import RfqList from "../views/portal/RfqList.vue";
import SupplierProfile from "../views/portal/SupplierProfile.vue";
import ResourcePage from "../views/ResourcePage.vue";

const routes = [
  { path: "/", redirect: "/login" },
  { path: "/login", component: LoginView, meta: { public: true } },
  { path: "/admin", component: AdminDashboard, meta: { mode: "admin" } },
  {
    path: "/admin/suppliers",
    component: SupplierList,
    meta: { mode: "admin" },
  },
  {
    path: "/admin/sourcing",
    component: SourcingAdmin,
    meta: { mode: "admin" },
  },
  {
    path: "/admin/system",
    component: SystemAdmin,
    meta: { mode: "admin" },
  },
  {
    path: "/admin/:module",
    component: ResourcePage,
    props: true,
    meta: { mode: "admin" },
  },
  { path: "/portal", component: PortalDashboard, meta: { mode: "portal" } },
  { path: "/portal/rfqs", component: RfqList, meta: { mode: "portal" } },
  {
    path: "/portal/profile",
    component: SupplierProfile,
    meta: { mode: "portal" },
  },
  {
    path: "/portal/:module",
    component: ResourcePage,
    props: true,
    meta: { mode: "portal" },
  },
];
const router = createRouter({ history: createWebHistory(), routes });
router.beforeEach((to) => {
  const user = JSON.parse(localStorage.getItem("zhuatech-srm-user") || "null");
  if (to.meta.public)
    return user ? (user.role === "SUPPLIER" ? "/portal" : "/admin") : true;
  if (!user || !localStorage.getItem("zhuatech-srm-token")) return "/login";
  if (user.role === "SUPPLIER" && to.path.startsWith("/admin"))
    return "/portal";
  if (user.role !== "SUPPLIER" && to.path.startsWith("/portal"))
    return "/admin";
  return true;
});
export default router;
