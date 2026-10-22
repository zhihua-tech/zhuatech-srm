<script setup>
import { computed, ref } from "vue";
import AppIcon from "./AppIcon.vue";
import { useAuthStore } from "../stores/auth";
import { useRouter } from "vue-router";
const props = defineProps({ mode: { type: String, default: "admin" } });
const auth = useAuthStore(),
  router = useRouter(),
  mobileOpen = ref(false);
const portal = [
  ["/portal", "home", "工作台", "PORTAL"],
  ["/portal/rfqs", "search", "询报价", "QUOTE_SUBMIT"],
  ["/portal/orders", "order", "采购订单", "ORDER_CONFIRM"],
  ["/portal/delivery", "truck", "送货与到货", "ASN_MANAGE"],
  ["/portal/quality", "quality", "质量整改", "QUALITY_ACTION"],
  ["/portal/settlement", "finance", "对账与发票", "RECONCILIATION_CONFIRM"],
  ["/portal/performance", "chart", "绩效与风险", "PORTAL"],
  ["/portal/profile", "settings", "企业资料", "SUPPLIER_SELF_MANAGE"],
  ["/portal/inbox", "bell", "消息与待办", "MESSAGE"],
];
const admin = [
  ["/admin", "home", "采购工作台", "DASHBOARD"],
  ["/admin/suppliers", "supplier", "供应商管理", "SUPPLIER_READ"],
  ["/admin/sourcing", "search", "寻源询报价", "SOURCING_READ|SOURCING_MANAGE"],
  ["/admin/contracts", "finance", "合同管理", "CONTRACT_READ|CONTRACT_MANAGE"],
  ["/admin/orders", "order", "采购订单", "ORDER_READ|ORDER_MANAGE"],
  ["/admin/quotas", "chart", "配额管理", "QUOTA_READ|QUOTA_MANAGE"],
  ["/admin/delivery", "truck", "交付协同", "DELIVERY_READ|ORDER_MANAGE"],
  ["/admin/quality", "quality", "质量协同", "QUALITY_READ|QUALITY_MANAGE"],
  [
    "/admin/settlement",
    "finance",
    "结算管理",
    "RECONCILIATION_READ|RECONCILIATION_MANAGE",
  ],
  [
    "/admin/performance",
    "chart",
    "绩效与风险",
    "PERFORMANCE_READ|PERFORMANCE_MANAGE",
  ],
  ["/admin/inbox", "bell", "消息与待办", "MESSAGE"],
  ["/admin/audit", "settings", "审计日志", "AUDIT_READ"],
  ["/admin/system", "settings", "系统管理", "*"],
];
function allowed(code) {
  return (
    auth.permissions.includes("*") ||
    code.split("|").some((value) => auth.permissions.includes(value))
  );
}
const menus = computed(() =>
  (props.mode === "portal" ? portal : admin).filter((item) => allowed(item[3])),
);
const date = new Intl.DateTimeFormat("zh-CN", { dateStyle: "long" }).format(
  new Date(),
);
function logout() {
  auth.logout();
  router.push("/login");
}
function close() {
  mobileOpen.value = false;
}
</script>
<template>
  <div class="app-shell" :class="[`mode-${mode}`, { 'nav-open': mobileOpen }]">
    <div class="nav-mask" @click="close"></div>
    <aside>
      <div class="brand">
        <div class="brand-mark">ZH</div>
        <div>
          <b>知华 SRM</b
          ><small>{{
            mode === "portal" ? "供应商门户" : "采购协同平台"
          }}</small>
        </div>
      </div>
      <div class="company" v-if="mode === 'portal'">
        <span>当前账号</span><b>{{ auth.user?.fullName }}</b
        ><small>{{ auth.user?.username }}</small>
      </div>
      <nav>
        <router-link v-for="m in menus" :key="m[0]" :to="m[0]" @click="close"
          ><AppIcon :name="m[1]" /><span>{{ m[2] }}</span></router-link
        >
      </nav>
      <div class="aside-foot">
        <span>企业交付版</span
        ><a href="https://www.zhuatech.cn/" target="_blank">www.zhuatech.cn</a>
      </div>
    </aside>
    <main>
      <header>
        <div class="header-left">
          <button class="menu-toggle" @click="mobileOpen = !mobileOpen">
            ☰</button
          ><span class="workspace">{{
            mode === "portal" ? "供应商工作区" : "采购管理工作区"
          }}</span
          ><span class="period">业务日期 {{ date }}</span>
        </div>
        <div class="header-actions">
          <router-link
            :to="mode === 'portal' ? '/portal/inbox' : '/admin/inbox'"
            class="icon-button"
            title="消息与待办"
            ><AppIcon name="bell"
          /></router-link>
          <div class="avatar">
            {{ auth.user?.fullName?.slice(0, 1) || "用" }}
          </div>
          <div class="user">
            <b>{{ auth.user?.fullName }}</b
            ><small>{{ auth.user?.role }}</small>
          </div>
          <button class="logout" @click="logout">退出</button>
        </div>
      </header>
      <section class="page"><slot /></section>
    </main>
  </div>
</template>
