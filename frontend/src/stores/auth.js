/* Copyright 2026 Shanghai Rujing Zhihua Information Technology Co., Ltd. · https://www.zhuatech.cn/ */
import { defineStore } from "pinia";
import http from "../api/http";
export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: JSON.parse(localStorage.getItem("zhuatech-srm-user") || "null"),
    permissions: JSON.parse(
      localStorage.getItem("zhuatech-srm-permissions") || "[]",
    ),
  }),
  getters: { isSupplier: (state) => state.user?.role === "SUPPLIER" },
  actions: {
    async login(username, password) {
      const data = (await http.post("/auth/login", { username, password })).data
        .data;
      localStorage.setItem("zhuatech-srm-token", data.token);
      const context = (await http.get("/auth/me")).data.data;
      this.user = context.user;
      this.permissions = context.permissions || [];
      localStorage.setItem("zhuatech-srm-user", JSON.stringify(this.user));
      localStorage.setItem(
        "zhuatech-srm-permissions",
        JSON.stringify(this.permissions),
      );
      return this.user;
    },
    logout() {
      this.user = null;
      this.permissions = [];
      localStorage.removeItem("zhuatech-srm-token");
      localStorage.removeItem("zhuatech-srm-user");
      localStorage.removeItem("zhuatech-srm-permissions");
    },
  },
});
