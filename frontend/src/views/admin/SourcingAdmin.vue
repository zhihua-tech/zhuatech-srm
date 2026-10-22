<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ -->
<script setup>
import { onMounted, reactive, ref } from "vue";
import AppShell from "../../components/AppShell.vue";
import BusinessCreateModal from "../../components/BusinessCreateModal.vue";
import StatusPill from "../../components/StatusPill.vue";
import http from "../../api/http";

const events = ref([]),
  selected = ref(null),
  quotes = ref([]),
  loading = ref(true),
  error = ref(""),
  success = ref(""),
  showCreate = ref(false),
  saving = ref(false);
const evaluation = reactive({
  quoteId: null,
  priceScore: 80,
  deliveryScore: 80,
  qualityScore: 80,
  serviceScore: 80,
  comment: "",
});
const award = reactive({ quoteId: null, reason: "", comment: "" });
async function load(preferredId) {
  loading.value = true;
  try {
    events.value = (await http.get("/admin/sourcing-events")).data.data || [];
    const event =
      events.value.find(
        (item) => item.id === (preferredId || selected.value?.id),
      ) || events.value[0];
    if (event) await choose(event);
  } catch (e) {
    error.value = e.response?.data?.message || "寻源项目加载失败";
  } finally {
    loading.value = false;
  }
}
async function choose(event) {
  selected.value = event;
  try {
    quotes.value =
      (await http.get(`/admin/sourcing-events/${event.id}/compare`)).data
        .data || [];
  } catch (e) {
    error.value = e.response?.data?.message || "比价数据加载失败";
  }
}
function editEvaluation(quote) {
  Object.assign(evaluation, {
    quoteId: quote.id,
    priceScore: 80,
    deliveryScore: 80,
    qualityScore: 80,
    serviceScore: 80,
    comment: "",
  });
}
async function saveEvaluation() {
  saving.value = true;
  try {
    await http.post(
      `/admin/sourcing-events/${selected.value.id}/quotes/${evaluation.quoteId}/evaluate`,
      evaluation,
    );
    success.value = "综合评审已保存。";
    evaluation.quoteId = null;
    await choose(selected.value);
  } catch (e) {
    error.value = e.response?.data?.message || "综合评审保存失败";
  } finally {
    saving.value = false;
  }
}
async function requestAward() {
  saving.value = true;
  try {
    await http.post(`/admin/sourcing-events/${selected.value.id}/award`, {
      quoteId: Number(award.quoteId),
      reason: award.reason,
    });
    success.value = "定标已提交审批。";
    award.reason = "";
    await load(selected.value.id);
  } catch (e) {
    error.value = e.response?.data?.message || "定标提交失败";
  } finally {
    saving.value = false;
  }
}
async function approve(approved) {
  if (!award.comment.trim()) {
    error.value = "请填写审批意见";
    return;
  }
  saving.value = true;
  try {
    await http.post(
      `/admin/sourcing-events/${selected.value.id}/award/approval`,
      { approved, comment: award.comment },
    );
    success.value = approved ? "定标已通过并通知参与方。" : "定标已驳回。";
    award.comment = "";
    await load(selected.value.id);
  } catch (e) {
    error.value = e.response?.data?.message || "定标审批失败";
  } finally {
    saving.value = false;
  }
}
async function created() {
  showCreate.value = false;
  await load();
}
onMounted(() => load());
</script>
<template>
  <AppShell mode="admin"
    ><div class="page-head">
      <div>
        <span class="breadcrumb">采购管理 / 寻源询报价</span>
        <h1>寻源、比价与定标</h1>
        <p>管理 RFQ/RFP、定向邀请、报价版本、综合评审和定标审批。</p>
      </div>
      <div class="head-actions">
        <button class="secondary" @click="load()">刷新</button
        ><button class="primary" @click="showCreate = true">新建询价</button>
      </div>
    </div>
    <div v-if="success" class="notice success-notice">
      <b>操作成功</b><span>{{ success }}</span
      ><button @click="success = ''">关闭</button>
    </div>
    <div v-if="error" class="notice error-notice">
      <b>操作失败</b><span>{{ error }}</span
      ><button @click="error = ''">关闭</button>
    </div>
    <div v-if="loading" class="state-box panel">
      <span class="loader"></span>
    </div>
    <div v-else-if="!events.length" class="state-box panel">
      <b>暂无寻源项目</b>
      <p>创建并定向邀请供应商参与报价。</p>
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
          <small>邀请 {{ item.invited }} / 响应 {{ item.responded }}</small>
        </button>
      </section>
      <section class="panel quote-detail">
        <div class="detail-head">
          <div>
            <span>{{ selected.eventNo }}</span>
            <h2>{{ selected.title }}</h2>
            <p>
              {{ selected.category }} · 截止
              {{ selected.deadline?.replace("T", " ").slice(0, 16) }}
            </p>
          </div>
          <StatusPill :text="selected.status" />
        </div>
        <div class="quote-section">
          <div class="section-title">
            <h3>供应商报价比较</h3>
            <span>{{ quotes.length }} 份最新报价</span>
          </div>
          <div class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>供应商</th>
                  <th>版本</th>
                  <th>含税金额</th>
                  <th>税率</th>
                  <th>交期</th>
                  <th>综合分</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="quote in quotes" :key="quote.id">
                  <td>
                    <b>{{ quote.supplierName }}</b>
                  </td>
                  <td>V{{ quote.version }}</td>
                  <td>{{ quote.amount }}</td>
                  <td>{{ quote.taxRate }}%</td>
                  <td>{{ quote.leadDays }} 天</td>
                  <td>{{ quote.evaluationScore ?? "待评审" }}</td>
                  <td>
                    <button class="link-button" @click="editEvaluation(quote)">
                      综合评审
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <form
          v-if="evaluation.quoteId"
          class="quote-form"
          @submit.prevent="saveEvaluation"
        >
          <div class="section-title">
            <h3>基础综合评审</h3>
            <button
              class="link-button"
              type="button"
              @click="evaluation.quoteId = null"
            >
              取消
            </button>
          </div>
          <div class="form-grid">
            <label
              ><span>价格得分 *</span
              ><input
                v-model.number="evaluation.priceScore"
                type="number"
                min="0"
                max="100"
                required /></label
            ><label
              ><span>交期得分 *</span
              ><input
                v-model.number="evaluation.deliveryScore"
                type="number"
                min="0"
                max="100"
                required /></label
            ><label
              ><span>质量得分 *</span
              ><input
                v-model.number="evaluation.qualityScore"
                type="number"
                min="0"
                max="100"
                required /></label
            ><label
              ><span>服务得分 *</span
              ><input
                v-model.number="evaluation.serviceScore"
                type="number"
                min="0"
                max="100"
                required /></label
            ><label class="full-row"
              ><span>评审意见 *</span
              ><textarea v-model="evaluation.comment" required></textarea>
            </label>
          </div>
          <div class="form-actions">
            <button class="primary" :disabled="saving">保存评审</button>
          </div>
        </form>
        <form
          v-if="
            selected.status !== 'PENDING_APPROVAL' &&
            selected.status !== 'AWARDED'
          "
          class="quote-form"
          @submit.prevent="requestAward"
        >
          <div class="section-title"><h3>提交定标</h3></div>
          <div class="form-grid">
            <label
              ><span>中标报价 *</span
              ><select v-model="award.quoteId" required>
                <option :value="null">请选择</option>
                <option
                  v-for="quote in quotes.filter(
                    (item) => item.evaluationScore !== null,
                  )"
                  :key="quote.id"
                  :value="quote.id"
                >
                  {{ quote.supplierName }} · {{ quote.amount }} ·
                  {{ quote.evaluationScore }}分
                </option>
              </select></label
            ><label class="full-row"
              ><span>定标原因 *</span
              ><textarea v-model="award.reason" required></textarea>
            </label>
          </div>
          <div class="form-actions">
            <button class="primary" :disabled="saving">提交定标审批</button>
          </div>
        </form>
        <div v-if="selected.status === 'PENDING_APPROVAL'" class="quote-form">
          <div class="section-title"><h3>定标审批</h3></div>
          <textarea
            v-model="award.comment"
            class="questionnaire"
            placeholder="审批意见"
          ></textarea>
          <div class="form-actions">
            <button
              class="secondary"
              :disabled="saving"
              @click="approve(false)"
            >
              驳回</button
            ><button class="primary" :disabled="saving" @click="approve(true)">
              审批通过
            </button>
          </div>
        </div>
      </section>
    </div>
    <BusinessCreateModal
      v-if="showCreate"
      module="sourcing"
      mode="admin"
      @close="showCreate = false"
      @saved="created"
  /></AppShell>
</template>
