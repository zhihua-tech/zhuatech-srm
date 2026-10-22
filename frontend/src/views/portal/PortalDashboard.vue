<script setup>
import { onMounted, ref } from "vue";
import AppShell from "../../components/AppShell.vue";
import MetricGrid from "../../components/MetricGrid.vue";
import StatusPill from "../../components/StatusPill.vue";
import { useAuthStore } from "../../stores/auth";
import http from "../../api/http";
const auth = useAuthStore(),
  loading = ref(true),
  error = ref(""),
  dashboard = ref({ metrics: [], workItems: [] });
async function load() {
  loading.value = true;
  error.value = "";
  try {
    dashboard.value = (await http.get("/portal/dashboard")).data.data;
  } catch (e) {
    error.value = e.response?.data?.message || "工作台加载失败";
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
<template>
  <AppShell mode="portal"
    ><div class="page-head">
      <div>
        <span class="breadcrumb">供应商门户 / 工作台</span>
        <h1>{{ auth.user?.fullName }}，您好</h1>
        <p>请优先处理临近截止时间的询价、订单、交付及质量事项。</p>
      </div>
      <router-link class="primary" to="/portal/delivery"
        >创建送货通知</router-link
      >
    </div>
    <div v-if="error" class="state-box error-state">
      <b>{{ error }}</b
      ><button class="secondary" @click="load">重新加载</button>
    </div>
    <template v-else
      ><div v-if="loading" class="state-box">
        <span class="loader"></span>
        <p>正在加载供应商工作台…</p>
      </div>
      <template v-else
        ><MetricGrid :items="dashboard.metrics" />
        <div class="dashboard-grid">
          <section class="panel wide">
            <div class="panel-head">
              <div>
                <h2>业务待办</h2>
                <p>询价、订单、交付、整改和对账事项</p>
              </div>
              <router-link to="/portal/inbox">查看全部</router-link>
            </div>
            <div v-if="!dashboard.workItems.length" class="state-box compact">
              <b>暂无待办</b>
              <p>当前没有需要处理的业务事项。</p>
            </div>
            <div v-else class="task-list">
              <router-link
                v-for="item in dashboard.workItems"
                :key="item.id"
                :to="item.link || '/portal/inbox'"
                class="task-row"
                ><div>
                  <StatusPill :text="item.kind" /><b>{{ item.title }}</b>
                  <p>{{ item.content }}</p>
                </div>
                <span>{{
                  item.dueAt?.replace("T", " ").slice(0, 16) || "无截止时间"
                }}</span></router-link
              >
            </div>
          </section>
          <section class="panel quick-panel">
            <div class="panel-head">
              <div>
                <h2>协同入口</h2>
                <p>进入常用业务模块</p>
              </div>
            </div>
            <div class="quick-links">
              <router-link to="/portal/rfqs"
                ><b>询报价</b><span>报价与澄清</span></router-link
              ><router-link to="/portal/orders"
                ><b>采购订单</b><span>确认数量与交期</span></router-link
              ><router-link to="/portal/quality"
                ><b>质量整改</b><span>原因、措施与证据</span></router-link
              ><router-link to="/portal/settlement"
                ><b>对账发票</b><span>确认、异议与开票</span></router-link
              >
            </div>
          </section>
        </div></template
      ></template
    ></AppShell
  >
</template>
