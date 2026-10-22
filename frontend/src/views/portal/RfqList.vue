<script setup>
import { onMounted, ref } from "vue";
import AppShell from "../../components/AppShell.vue";
import StatusPill from "../../components/StatusPill.vue";
import http from "../../api/http";
const events = ref([]),
  selected = ref(null),
  detail = ref(null),
  loading = ref(true),
  saving = ref(false),
  error = ref(""),
  success = ref(""),
  quote = ref({
    amount: null,
    taxRate: 13,
    leadDays: 15,
    validUntil: "",
    remark: "",
    attachmentKey: null,
  }),
  question = ref("");
async function load() {
  loading.value = true;
  error.value = "";
  try {
    events.value = (await http.get("/portal/sourcing-events")).data.data || [];
    if (events.value.length) await choose(events.value[0]);
  } catch (e) {
    error.value = e.response?.data?.message || "询价数据加载失败";
  } finally {
    loading.value = false;
  }
}
async function choose(item) {
  selected.value = item;
  error.value = "";
  try {
    detail.value = (
      await http.get(`/portal/sourcing-events/${item.id}`)
    ).data.data;
  } catch (e) {
    error.value = e.response?.data?.message || "询价详情加载失败";
  }
}
async function submitQuote() {
  saving.value = true;
  error.value = "";
  success.value = "";
  try {
    await http.post(
      `/portal/sourcing-events/${selected.value.id}/quotes`,
      quote.value,
    );
    success.value = "正式报价已提交并生成新的报价版本。";
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || "报价提交失败";
  } finally {
    saving.value = false;
  }
}
async function ask() {
  if (!question.value.trim()) return;
  error.value = "";
  try {
    await http.post(
      `/portal/sourcing-events/${selected.value.id}/clarifications`,
      { content: question.value.trim() },
    );
    question.value = "";
    success.value = "澄清问题已提交。";
  } catch (e) {
    error.value = e.response?.data?.message || "澄清问题提交失败";
  }
}
onMounted(load);
</script>
<template>
  <AppShell mode="portal"
    ><div class="page-head">
      <div>
        <span class="breadcrumb">供应商门户 / 询报价</span>
        <h1>询报价工作台</h1>
        <p>查看定向邀请、需求明细、提交有版本记录的报价和澄清问题。</p>
      </div>
      <button class="secondary" @click="load">刷新</button>
    </div>
    <div v-if="success" class="notice success-notice">
      <b>已完成</b><span>{{ success }}</span
      ><button @click="success = ''">关闭</button>
    </div>
    <div v-if="error" class="notice error-notice">
      <b>操作提示</b><span>{{ error }}</span
      ><button @click="error = ''">关闭</button>
    </div>
    <div v-if="loading" class="state-box">
      <span class="loader"></span>
      <p>正在加载询价项目…</p>
    </div>
    <div v-else-if="!events.length" class="state-box panel">
      <b>暂无受邀询价</b>
      <p>当前没有需要响应的 RFQ 或 RFP 项目。</p>
    </div>
    <div v-else class="rfq-layout">
      <section class="panel rfq-list">
        <button
          v-for="item in events"
          :key="item.id"
          class="rfq-card"
          :class="{ active: selected?.id === item.id }"
          @click="choose(item)"
        >
          <div>
            <span>{{ item.eventNo }}</span
            ><StatusPill :text="item.status" />
          </div>
          <b>{{ item.title }}</b>
          <p>{{ item.category }} · {{ item.type }}</p>
          <small
            >截止 {{ item.deadline?.replace("T", " ").slice(0, 16) }}</small
          >
        </button>
      </section>
      <section v-if="detail" class="panel quote-detail">
        <div class="detail-head">
          <div>
            <span>{{ detail.event.eventNo }}</span>
            <h2>{{ detail.event.title }}</h2>
            <p>{{ detail.event.category }} · 负责人 {{ detail.event.owner }}</p>
          </div>
          <StatusPill :text="detail.event.status" />
        </div>
        <div class="requirement">
          <div>
            <span>项目类型</span><b>{{ detail.event.type }}</b>
          </div>
          <div>
            <span>报价截止</span
            ><b>{{ detail.event.deadline?.replace("T", " ").slice(0, 16) }}</b>
          </div>
          <div>
            <span>预算金额</span><b>{{ detail.event.budget }}</b>
          </div>
          <div>
            <span>邀请 / 响应</span
            ><b>{{ detail.event.invited }} / {{ detail.event.responded }}</b>
          </div>
        </div>
        <div class="quote-section">
          <div class="section-title">
            <h3>采购需求明细</h3>
            <span>共 {{ detail.items.length }} 项</span>
          </div>
          <div class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>行号</th>
                  <th>物料编码</th>
                  <th>物料名称</th>
                  <th>规格</th>
                  <th>数量</th>
                  <th>单位</th>
                  <th>需求日期</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="line in detail.items" :key="line.id">
                  <td>{{ line.lineNo }}</td>
                  <td>
                    <b>{{ line.materialCode }}</b>
                  </td>
                  <td>{{ line.materialName }}</td>
                  <td>{{ line.specification || "—" }}</td>
                  <td>{{ line.quantity }}</td>
                  <td>{{ line.unit }}</td>
                  <td>{{ line.requiredDate }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <form class="quote-form" @submit.prevent="submitQuote">
          <div class="section-title">
            <h3>提交正式报价</h3>
            <span>重复提交将形成新版本</span>
          </div>
          <div class="form-grid">
            <label
              ><span>含税总金额 *</span
              ><input
                v-model.number="quote.amount"
                type="number"
                min="0.01"
                step="0.01"
                required /></label
            ><label
              ><span>税率（%）*</span
              ><input
                v-model.number="quote.taxRate"
                type="number"
                min="0"
                max="100"
                step="0.01"
                required /></label
            ><label
              ><span>交付周期（天）*</span
              ><input
                v-model.number="quote.leadDays"
                type="number"
                min="1"
                required /></label
            ><label
              ><span>报价有效期 *</span
              ><input v-model="quote.validUntil" type="date" required /></label
            ><label class="full-row"
              ><span>报价说明</span
              ><textarea v-model="quote.remark" maxlength="500"></textarea>
            </label>
          </div>
          <div class="form-actions">
            <button class="primary" :disabled="saving">
              {{ saving ? "正在提交…" : "提交正式报价" }}
            </button>
          </div>
        </form>
        <div class="clarification-box">
          <label
            ><span>澄清问题</span
            ><textarea
              v-model="question"
              maxlength="1000"
              placeholder="请填写需要采购方澄清的问题"
            ></textarea></label
          ><button class="secondary" @click="ask">提交澄清</button>
        </div>
      </section>
    </div></AppShell
  >
</template>
