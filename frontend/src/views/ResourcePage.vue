<script setup>
import { computed, onMounted, ref, watch } from "vue";
import AppShell from "../components/AppShell.vue";
import StatusPill from "../components/StatusPill.vue";
import BusinessCreateModal from "../components/BusinessCreateModal.vue";
import BusinessActionModal from "../components/BusinessActionModal.vue";
import http from "../api/http";
const props = defineProps({ module: String });
const loading = ref(true),
  error = ref(""),
  rows = ref([]),
  pageInfo = ref(null),
  uploading = ref(false),
  fileInput = ref(null),
  showCreate = ref(false),
  actionBusy = ref(null),
  actionRow = ref(null),
  auditActor = ref(""),
  auditModule = ref(""),
  auditFrom = ref(""),
  auditTo = ref("");
const configs = {
  sourcing: {
    title: "寻源询报价",
    description: "管理 RFQ/RFP、定向邀请、澄清、报价比较和定标结果。",
    admin: "/admin/sourcing-events",
    portal: "/portal/sourcing-events",
    columns: [
      ["eventNo", "项目编号"],
      ["title", "项目名称"],
      ["type", "类型"],
      ["category", "品类"],
      ["deadline", "报价截止"],
      ["invited", "邀请"],
      ["responded", "响应"],
      ["status", "状态"],
    ],
  },
  contracts: {
    title: "合同管理",
    description: "维护合同台账、审批版本、附件和到期提醒。",
    admin: "/contracts",
    portal: "/contracts",
    columns: [
      ["contractNo", "合同编号"],
      ["version", "版本"],
      ["supplierName", "供应商"],
      ["title", "合同名称"],
      ["amount", "金额"],
      ["currency", "币种"],
      ["effectiveTo", "到期日期"],
      ["executionAmount", "履约金额"],
      ["executionRate", "履约率%"],
      ["fulfillmentStatus", "履约状态"],
      ["status", "状态"],
    ],
  },
  orders: {
    title: "采购订单",
    description: "跟踪订单明细、供应商确认、交期反馈、版本及异常。",
    admin: "/admin/orders",
    portal: "/portal/orders/list",
    columns: [
      ["orderNo", "订单编号"],
      ["supplierName", "供应商"],
      ["itemSummary", "物料摘要"],
      ["amount", "金额"],
      ["currency", "币种"],
      ["deliveryDate", "交付日期"],
      ["version", "版本"],
      ["status", "状态"],
    ],
  },
  quotas: {
    title: "配额管理",
    description: "维护采购组织、工厂、物料和供应商维度的比例或数量配额。",
    admin: "/admin/quotas",
    columns: [
      ["id", "方案ID"],
      ["procurementOrganization", "采购组织"],
      ["factoryCode", "工厂"],
      ["materialCode", "物料编码"],
      ["materialName", "物料名称"],
      ["quotaType", "配额类型"],
      ["version", "版本"],
      ["status", "状态"],
    ],
  },
  delivery: {
    title: "交付协同",
    description: "管理 ASN、送货预约、发货、到货与收货差异。",
    admin: "/asns",
    portal: "/asns",
    columns: [
      ["asnNo", "ASN编号"],
      ["orderNo", "采购订单"],
      ["supplierName", "供应商"],
      ["plannedArrivalAt", "计划到货"],
      ["appointmentAt", "预约时间"],
      ["trackingNo", "运单号"],
      ["status", "状态"],
    ],
  },
  quality: {
    title: "质量协同",
    description: "闭环处理 NCR、原因分析、8D 整改、退回、验证和关闭。",
    admin: "/quality-issues",
    portal: "/quality-issues",
    columns: [
      ["issueNo", "问题编号"],
      ["supplierName", "供应商"],
      ["type", "类型"],
      ["title", "问题标题"],
      ["severity", "严重度"],
      ["dueDate", "截止日期"],
      ["status", "状态"],
    ],
  },
  settlement: {
    title: "对账与结算",
    description: "生成基础对账、处理差异并登记发票信息。",
    admin: "/reconciliations",
    portal: "/reconciliations",
    columns: [
      ["statementNo", "对账单号"],
      ["supplierName", "供应商"],
      ["periodFrom", "期间开始"],
      ["periodTo", "期间结束"],
      ["orderAmount", "订单金额"],
      ["settlementAmount", "结算金额"],
      ["status", "状态"],
    ],
  },
  performance: {
    title: "绩效与风险",
    description: "查询质量、交付、成本、服务评分及规则型风险整改。",
    admin: "/performance",
    portal: "/performance",
    columns: [
      ["supplierName", "供应商"],
      ["periodFrom", "期间开始"],
      ["periodTo", "期间结束"],
      ["qualityScore", "质量"],
      ["deliveryScore", "交付"],
      ["totalScore", "综合得分"],
      ["grade", "等级"],
      ["correctiveActionRequired", "需整改"],
    ],
  },
  inbox: {
    title: "消息与待办",
    description: "集中处理业务通知、审批提醒和异常待办。",
    admin: "/inbox",
    portal: "/inbox",
    columns: [
      ["kind", "类型"],
      ["title", "标题"],
      ["content", "内容"],
      ["businessType", "业务"],
      ["dueAt", "截止时间"],
      ["status", "状态"],
    ],
  },
  audit: {
    title: "审计日志",
    description: "查询关键业务操作人、时间及修改前后内容。",
    admin: "/admin/audit-logs",
    columns: [
      ["occurredAt", "操作时间"],
      ["actor", "操作人"],
      ["module", "模块"],
      ["action", "动作"],
      ["objectType", "对象类型"],
      ["objectId", "对象ID"],
      ["clientIp", "客户端IP"],
    ],
  },
  profile: {
    title: "企业资料",
    description: "查看企业基础资料、联系人、资质证照及变更记录。",
    portal: "/portal/profile",
    columns: [
      ["code", "供应商编码"],
      ["name", "企业名称"],
      ["registrationNo", "信用代码"],
      ["supplyScope", "供货范围"],
      ["contactName", "联系人"],
      ["contactPhone", "联系电话"],
      ["status", "状态"],
    ],
  },
};
const mode = computed(() =>
    location.pathname.startsWith("/portal") ? "portal" : "admin",
  ),
  config = computed(() => configs[props.module] || configs.orders);
const creatable = computed(
  () =>
    (mode.value === "admin" &&
      [
        "sourcing",
        "contracts",
        "orders",
        "quotas",
        "quality",
        "settlement",
        "performance",
      ].includes(props.module)) ||
    (mode.value === "portal" && props.module === "delivery"),
);
function normalize(data) {
  if (Array.isArray(data)) return data;
  if (data?.content) return data.content;
  if (data?.supplier) return [data.supplier];
  return data ? [data] : [];
}
function value(row, key) {
  const v = row?.[key];
  if (v === null || v === undefined || v === "") return "—";
  if (typeof v === "boolean") return v ? "是" : "否";
  if (typeof v === "object") return JSON.stringify(v);
  return String(v).replace("T", " ").slice(0, 19);
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const endpoint = config.value[mode.value];
    if (!endpoint) throw new Error("当前角色没有该模块权限");
    const response = await http.get(endpoint, {
      params:
        props.module === "audit"
          ? {
              actor: auditActor.value || undefined,
              module: auditModule.value || undefined,
              fromTime: auditFrom.value || undefined,
              toTime: auditTo.value || undefined,
            }
          : undefined,
    });
    const data = response.data.data;
    rows.value = normalize(data);
    pageInfo.value = data?.content ? data : null;
  } catch (e) {
    error.value =
      e.response?.status === 403
        ? "当前账号没有查看该模块的权限"
        : e.response?.data?.message || e.message || "数据加载失败";
  } finally {
    loading.value = false;
  }
}
async function uploadOrder(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  uploading.value = true;
  error.value = "";
  try {
    const form = new FormData();
    form.append("file", file);
    await http.post("/admin/orders/import", form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    await load();
  } catch (e) {
    const data = e.response?.data?.data;
    const errors = Array.isArray(data) ? data : data?.errors;
    error.value = errors?.length
      ? errors
          .map((x) => `第${x.rowNumber}行 ${x.field}：${x.message}`)
          .join("；")
      : e.response?.data?.message || "导入失败";
  } finally {
    uploading.value = false;
    event.target.value = "";
  }
}
async function downloadOrderTemplate() {
  try {
    const response = await http.get("/admin/orders/import-template", {
      responseType: "blob",
    });
    const url = URL.createObjectURL(response.data),
      a = document.createElement("a");
    a.href = url;
    a.download = "purchase-order-import-template.xlsx";
    a.click();
    URL.revokeObjectURL(url);
  } catch {
    error.value = "订单导入模板下载失败";
  }
}
async function exportQuota() {
  try {
    const response = await http.get("/admin/quotas/export", {
      responseType: "blob",
    });
    const url = URL.createObjectURL(response.data),
      a = document.createElement("a");
    a.href = url;
    a.download = "quota-execution.xlsx";
    a.click();
    URL.revokeObjectURL(url);
  } catch (e) {
    error.value = "配额导出失败，请确认账号具有导出权限";
  }
}
async function exportQuality() {
  try {
    const response = await http.get("/admin/quality-issues/export", {
      responseType: "blob",
    });
    const url = URL.createObjectURL(response.data),
      a = document.createElement("a");
    a.href = url;
    a.download = "quality-issues.xlsx";
    a.click();
    URL.revokeObjectURL(url);
  } catch {
    error.value = "质量问题导出失败，请确认账号具有导出权限";
  }
}
function actionLabel(row) {
  if (props.module === "quotas" && row.status === "DRAFT") return "启用";
  if (props.module === "contracts")
    return mode.value === "portal"
      ? "查看详情"
      : {
          DRAFT: "提交审批",
          PENDING_APPROVAL: "审批通过",
          APPROVED: "生效",
          ACTIVE: "创建新版本",
          SUPERSEDED: "查看版本",
          EXPIRED: "查看版本",
          TERMINATED: "查看版本",
        }[row.status];
  if (props.module === "inbox" && row.status !== "COMPLETED") return "完成";
  if (
    mode.value === "portal" &&
    props.module === "orders" &&
    ["PENDING_CONFIRMATION", "PENDING_ACK", "PARTIALLY_CONFIRMED"].includes(
      row.status,
    )
  )
    return "确认明细";
  if (
    mode.value === "admin" &&
    props.module === "orders" &&
    !["CANCELLED", "COMPLETED"].includes(row.status)
  )
    return "查看/变更";
  if (
    props.module === "delivery" &&
    mode.value === "portal" &&
    ["DRAFT", "APPOINTED"].includes(row.status)
  )
    return "登记发运";
  if (
    props.module === "delivery" &&
    mode.value === "admin" &&
    row.status === "DRAFT"
  )
    return "送货预约";
  if (
    props.module === "delivery" &&
    mode.value === "admin" &&
    ["SHIPPED", "PARTIALLY_RECEIVED"].includes(row.status)
  )
    return "收货";
  if (
    props.module === "quality" &&
    mode.value === "portal" &&
    ["OPEN", "ANALYZING", "RETURNED"].includes(row.status)
  )
    return row.status === "OPEN" ? "开始分析" : "提交整改";
  if (
    props.module === "quality" &&
    mode.value === "admin" &&
    row.status === "ACTION_SUBMITTED"
  )
    return "审核整改";
  if (
    props.module === "quality" &&
    mode.value === "admin" &&
    row.status === "VERIFYING"
  )
    return "验证关闭";
  if (props.module === "quality") return "查看历史";
  if (
    mode.value === "portal" &&
    props.module === "settlement" &&
    ["SENT", "DISPUTED"].includes(row.status)
  )
    return "对账处理";
  if (
    mode.value === "portal" &&
    props.module === "settlement" &&
    row.status === "SUPPLIER_CONFIRMED"
  )
    return "登记发票";
  if (props.module === "settlement") return "查看明细";
  return "";
}
function needsModal(row) {
  return (
    ["orders", "delivery", "quality", "settlement"].includes(props.module) ||
    (props.module === "contracts" &&
      (mode.value === "portal" ||
        ["ACTIVE", "SUPERSEDED", "EXPIRED", "TERMINATED"].includes(row.status)))
  );
}
async function act(row) {
  if (needsModal(row)) {
    actionRow.value = row;
    return;
  }
  actionBusy.value = row.id;
  error.value = "";
  try {
    if (props.module === "quotas")
      await http.post(`/admin/quotas/${row.id}/activate`);
    else if (props.module === "contracts") {
      const action = {
        DRAFT: "submit",
        PENDING_APPROVAL: "approve",
        APPROVED: "activate",
      }[row.status];
      await http.post(`/contracts/${row.id}/${action}`);
    } else if (props.module === "inbox")
      await http.post(`/inbox/${row.id}/complete`);
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || "业务操作失败";
  } finally {
    actionBusy.value = null;
  }
}
async function saved() {
  showCreate.value = false;
  await load();
}
async function actionSaved(close) {
  if (close) actionRow.value = null;
  await load();
}
watch(() => props.module, load);
onMounted(load);
</script>
<template>
  <AppShell :mode="mode"
    ><div class="page-head">
      <div>
        <span class="breadcrumb"
          >{{ mode === "portal" ? "供应商门户" : "采购管理" }} /
          {{ config.title }}</span
        >
        <h1>{{ config.title }}</h1>
        <p>{{ config.description }}</p>
      </div>
      <div class="head-actions">
        <input
          v-if="module === 'orders' && mode === 'admin'"
          ref="fileInput"
          type="file"
          accept=".xlsx"
          hidden
          @change="uploadOrder"
        /><button
          v-if="module === 'orders' && mode === 'admin'"
          class="secondary"
          @click="downloadOrderTemplate"
        >
          下载导入模板</button
        ><button
          v-if="module === 'orders' && mode === 'admin'"
          class="secondary"
          :disabled="uploading"
          @click="fileInput.click()"
        >
          {{ uploading ? "正在校验导入…" : "导入订单 Excel" }}</button
        ><button
          v-if="module === 'quotas'"
          class="secondary"
          @click="exportQuota"
        >
          导出配额执行</button
        ><button
          v-if="module === 'quality' && mode === 'admin'"
          class="secondary"
          @click="exportQuality"
        >
          导出质量问题</button
        ><button class="secondary" @click="load">刷新</button
        ><button v-if="creatable" class="primary" @click="showCreate = true">
          新建
        </button>
      </div>
    </div>
    <form
      v-if="module === 'audit'"
      class="panel filter-bar"
      @submit.prevent="load"
    >
      <label
        ><span>操作人</span><input v-model="auditActor" placeholder="用户名"
      /></label>
      <label
        ><span>业务模块</span
        ><input v-model="auditModule" placeholder="如 ORDER"
      /></label>
      <label
        ><span>开始时间</span><input v-model="auditFrom" type="datetime-local"
      /></label>
      <label
        ><span>结束时间</span><input v-model="auditTo" type="datetime-local"
      /></label>
      <button class="primary">查询</button>
    </form>
    <div v-if="error" class="state-box error-state">
      <b>无法完成请求</b>
      <p>{{ error }}</p>
      <button class="secondary" @click="load">重试</button>
    </div>
    <section v-else class="panel list-panel">
      <div class="table-summary">
        <span>共 {{ pageInfo?.totalElements ?? rows.length }} 条记录</span
        ><small>数据来自业务系统实时接口</small>
      </div>
      <div v-if="loading" class="state-box">
        <span class="loader"></span>
        <p>正在加载业务数据…</p>
      </div>
      <div v-else-if="!rows.length" class="state-box">
        <b>暂无业务数据</b>
        <p>当前筛选范围内没有记录。</p>
      </div>
      <div v-else class="table-scroll">
        <table>
          <thead>
            <tr>
              <th v-for="column in config.columns" :key="column[0]">
                {{ column[1] }}
              </th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in rows" :key="row.id || index">
              <td v-for="column in config.columns" :key="column[0]">
                <StatusPill
                  v-if="
                    column[0] === 'status' ||
                    column[0] === 'severity' ||
                    column[0] === 'grade'
                  "
                  :text="value(row, column[0])"
                /><b v-else-if="column === config.columns[0]">{{
                  value(row, column[0])
                }}</b
                ><span v-else>{{ value(row, column[0]) }}</span>
              </td>
              <td>
                <button
                  v-if="actionLabel(row)"
                  class="link-button"
                  :disabled="actionBusy === row.id"
                  @click="act(row)"
                >
                  {{
                    actionBusy === row.id ? "处理中…" : actionLabel(row)
                  }}</button
                ><span v-else>—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
    <BusinessCreateModal
      v-if="showCreate"
      :module="module"
      :mode="mode"
      @close="showCreate = false"
      @saved="saved" /><BusinessActionModal
      v-if="actionRow"
      :module="module"
      :mode="mode"
      :row="actionRow"
      @close="actionRow = null"
      @saved="actionSaved"
  /></AppShell>
</template>
