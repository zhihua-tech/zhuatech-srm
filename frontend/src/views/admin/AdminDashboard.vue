<script setup>
import { onMounted, ref } from "vue";
import AppShell from "../../components/AppShell.vue";
import MetricGrid from "../../components/MetricGrid.vue";
import StatusPill from "../../components/StatusPill.vue";
import http from "../../api/http";
const loading = ref(true),
  error = ref(""),
  dashboard = ref({ metrics: [], workItems: [] });
async function load() {
  loading.value = true;
  error.value = "";
  try {
    dashboard.value = (await http.get("/admin/dashboard")).data.data;
  } catch (e) {
    error.value = e.response?.data?.message || "工作台加载失败";
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
<template>
  <AppShell mode="admin"
    ><div class="page-head">
      <div>
        <span class="breadcrumb">采购管理 / 工作台</span>
        <h1>采购工作台</h1>
        <p>供应商、寻源、订单、质量及到期事项的实时概览。</p>
      </div>
      <div class="head-actions">
        <router-link class="secondary" to="/admin/audit">查看审计</router-link
        ><router-link class="primary" to="/admin/sourcing"
          >发起寻源</router-link
        >
      </div>
    </div>
    <div v-if="error" class="state-box error-state">
      <b>{{ error }}</b
      ><button class="secondary" @click="load">重新加载</button>
    </div>
    <template v-else
      ><div v-if="loading" class="state-box">
        <span class="loader"></span>
        <p>正在汇总工作台数据…</p>
      </div>
      <template v-else
        ><MetricGrid :items="dashboard.metrics" />
        <div class="dashboard-grid">
          <section class="panel wide">
            <div class="panel-head">
              <div>
                <h2>我的待办</h2>
                <p>需要当前账号处理的业务事项</p>
              </div>
              <router-link to="/admin/inbox">查看全部</router-link>
            </div>
            <div v-if="!dashboard.workItems.length" class="state-box compact">
              <b>暂无待办</b>
              <p>当前没有需要处理的任务。</p>
            </div>
            <div v-else class="task-list">
              <router-link
                v-for="item in dashboard.workItems"
                :key="item.id"
                :to="item.link || '/admin/inbox'"
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
                <h2>常用入口</h2>
                <p>进入核心业务模块</p>
              </div>
            </div>
            <div class="quick-links">
              <router-link to="/admin/suppliers"
                ><b>供应商管理</b><span>准入、资质与状态</span></router-link
              ><router-link to="/admin/quotas"
                ><b>配额管理</b><span>方案、执行与预警</span></router-link
              ><router-link to="/admin/orders"
                ><b>采购订单</b><span>导入、版本与异常</span></router-link
              ><router-link to="/admin/quality"
                ><b>质量协同</b><span>NCR 与整改闭环</span></router-link
              >
            </div>
          </section>
        </div></template
      ></template
    ></AppShell
  >
</template>
