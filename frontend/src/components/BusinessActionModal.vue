<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ -->
<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import http from "../api/http";

const props = defineProps({ module: String, mode: String, row: Object });
const emit = defineEmits(["close", "saved"]);
const loading = ref(true),
  saving = ref(false),
  error = ref(""),
  detail = ref(null);
const form = reactive({
  appointmentAt: "",
  carrierName: "",
  trackingNo: "",
  discrepancyReason: "",
  rootCause: "",
  interimAction: "",
  permanentAction: "",
  evidenceFileKey: "",
  approved: true,
  comment: "",
  verificationResult: "",
  confirm: true,
  reason: "",
  invoiceNo: "",
  amount: null,
  invoiceDate: new Date().toISOString().slice(0, 10),
  attachmentKey: "",
  contractTitle: "",
  contractAmount: null,
  contractCurrency: "CNY",
  effectiveFrom: "",
  effectiveTo: "",
  changeReason: "",
  orderChangeReason: "",
  wholeConfirmedDate: "",
  wholeRemark: "",
});
const canSubmit = computed(() => {
  if (props.module === "contracts")
    return props.mode === "admin" && props.row.status === "ACTIVE";
  if (props.module === "quality") {
    if (props.mode === "portal")
      return ["OPEN", "ANALYZING", "RETURNED"].includes(props.row.status);
    return ["ACTION_SUBMITTED", "VERIFYING"].includes(props.row.status);
  }
  if (props.module === "settlement")
    return (
      props.mode === "portal" &&
      ["SENT", "DISPUTED", "SUPPLIER_CONFIRMED"].includes(props.row.status)
    );
  return true;
});

async function load() {
  try {
    if (props.module === "orders")
      detail.value = (
        await http.get(
          `/${props.mode === "portal" ? "portal" : "admin"}/orders/${props.row.id}`,
        )
      ).data.data;
    if (props.module === "delivery")
      detail.value = (await http.get(`/asns/${props.row.id}`)).data.data;
    if (props.module === "quality")
      detail.value = (
        await http.get(`/quality-issues/${props.row.id}`)
      ).data.data;
    if (props.module === "settlement")
      detail.value = (
        await http.get(`/reconciliations/${props.row.id}`)
      ).data.data;
    if (props.module === "contracts") {
      detail.value = (await http.get(`/contracts/${props.row.id}`)).data.data;
      Object.assign(form, {
        contractTitle: detail.value.contract.title,
        contractAmount: detail.value.contract.amount,
        contractCurrency: detail.value.contract.currency,
        effectiveFrom: detail.value.contract.effectiveFrom,
        effectiveTo: detail.value.contract.effectiveTo,
        attachmentKey: detail.value.contract.attachmentKey || "",
      });
    }
  } catch (exception) {
    error.value = exception.response?.data?.message || "业务详情加载失败";
  } finally {
    loading.value = false;
  }
}

async function confirmOrder() {
  saving.value = true;
  error.value = "";
  try {
    await http.post(`/portal/orders/${props.row.id}/confirm`, {
      confirmedDate: form.wholeConfirmedDate || null,
      remark: form.wholeRemark || "",
    });
    await load();
    emit("saved", false);
  } catch (exception) {
    error.value = exception.response?.data?.message || "整单确认失败";
  } finally {
    saving.value = false;
  }
}

async function confirmLine(line) {
  saving.value = true;
  error.value = "";
  try {
    await http.post(`/portal/orders/${props.row.id}/lines/${line.id}/confirm`, {
      confirmedQuantity: Number(line.inputQuantity ?? line.orderedQuantity),
      confirmedDate: line.inputDate || line.requestedDate,
      remark: line.inputRemark || "",
    });
    await load();
    emit("saved", false);
  } catch (exception) {
    error.value = exception.response?.data?.message || "订单行确认失败";
  } finally {
    saving.value = false;
  }
}

async function save() {
  saving.value = true;
  error.value = "";
  try {
    if (props.module === "orders" && props.mode === "admin") {
      await http.post(`/admin/orders/${props.row.id}/versions`, {
        deliveryDate: null,
        reason: form.orderChangeReason,
        lines: detail.value.lines.map((line) => ({
          lineId: line.id,
          quantity: Number(line.inputQuantity ?? line.orderedQuantity),
          unitPrice: Number(line.inputPrice ?? line.unitPrice),
          requestedDate: line.inputDate || line.requestedDate,
        })),
      });
    } else if (props.module === "contracts") {
      await http.post(`/contracts/${props.row.id}/versions`, {
        title: form.contractTitle,
        amount: Number(form.contractAmount),
        currency: form.contractCurrency,
        effectiveFrom: form.effectiveFrom,
        effectiveTo: form.effectiveTo,
        attachmentKey: form.attachmentKey || null,
        changeReason: form.changeReason,
      });
    } else if (props.module === "delivery" && props.mode === "portal") {
      await http.post(`/portal/asns/${props.row.id}/ship`, {
        carrierName: form.carrierName,
        trackingNo: form.trackingNo,
      });
    } else if (props.module === "delivery" && props.row.status === "DRAFT") {
      await http.post(`/asns/${props.row.id}/appointment`, {
        appointmentAt: form.appointmentAt,
      });
    } else if (props.module === "delivery") {
      await http.post(`/admin/asns/${props.row.id}/receive`, {
        discrepancyReason: form.discrepancyReason,
        lines: detail.value.lines.map((line) => ({
          asnLineId: line.id,
          receivedQuantity: Number(line.inputReceived ?? line.shippedQuantity),
          discrepancyReason: line.inputReason || "",
        })),
      });
    } else if (props.module === "quality" && props.mode === "portal") {
      if (props.row.status === "OPEN")
        await http.post(`/portal/quality-issues/${props.row.id}/start`);
      else
        await http.post(`/portal/quality-issues/${props.row.id}/actions`, {
          rootCause: form.rootCause,
          interimAction: form.interimAction,
          permanentAction: form.permanentAction,
          evidenceFileKey: form.evidenceFileKey || null,
        });
    } else if (
      props.module === "quality" &&
      props.row.status === "ACTION_SUBMITTED"
    ) {
      await http.post(`/admin/quality-issues/${props.row.id}/review`, {
        approved: form.approved,
        comment: form.comment,
      });
    } else if (props.module === "quality") {
      await http.post(`/admin/quality-issues/${props.row.id}/close`, {
        verificationResult: form.verificationResult,
      });
    } else if (
      props.module === "settlement" &&
      ["SENT", "DISPUTED"].includes(props.row.status)
    ) {
      await http.post(`/portal/reconciliations/${props.row.id}/action`, {
        confirm: form.confirm,
        reason: form.confirm ? null : form.reason,
      });
    } else if (props.module === "settlement") {
      await http.post(`/portal/reconciliations/${props.row.id}/invoices`, {
        invoiceNo: form.invoiceNo,
        amount: Number(form.amount),
        invoiceDate: form.invoiceDate,
        attachmentKey: form.attachmentKey || null,
      });
    }
    emit("saved", true);
  } catch (exception) {
    error.value = exception.response?.data?.message || "业务操作失败";
  } finally {
    saving.value = false;
  }
}
onMounted(load);
</script>

<template>
  <div class="modal-mask" @click.self="emit('close')">
    <section class="modal modal-large">
      <div class="modal-head">
        <div>
          <h2>业务处理</h2>
          <p>
            {{
              row.orderNo ||
              row.asnNo ||
              row.issueNo ||
              row.statementNo ||
              row.contractNo
            }}
            ·
            {{ row.status }}
          </p>
        </div>
        <button type="button" @click="emit('close')">×</button>
      </div>
      <div v-if="error" class="inline-error">{{ error }}</div>
      <div v-if="loading" class="state-box compact">
        <span class="loader"></span>
      </div>
      <template v-else-if="module === 'orders'"
        ><div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>行号</th>
                <th>物料</th>
                <th>订单数量</th>
                <th>{{ mode === "portal" ? "确认数量" : "变更数量" }}</th>
                <th v-if="mode === 'admin'">变更单价</th>
                <th>{{ mode === "portal" ? "确认交期" : "要求交期" }}</th>
                <th v-if="mode === 'portal'">备注</th>
                <th v-if="mode === 'portal'">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="line in detail.lines" :key="line.id">
                <td>{{ line.lineNo }}</td>
                <td>{{ line.materialCode }} · {{ line.materialName }}</td>
                <td>{{ line.orderedQuantity }} {{ line.unit }}</td>
                <td>
                  <input
                    v-model.number="line.inputQuantity"
                    type="number"
                    min="0.0001"
                    :max="line.orderedQuantity"
                    :placeholder="line.orderedQuantity"
                  />
                </td>
                <td v-if="mode === 'admin'">
                  <input
                    v-model.number="line.inputPrice"
                    type="number"
                    min="0"
                    step="0.0001"
                    :placeholder="line.unitPrice"
                  />
                </td>
                <td>
                  <input
                    v-model="line.inputDate"
                    type="date"
                    :placeholder="line.requestedDate"
                  />
                </td>
                <td v-if="mode === 'portal'">
                  <input v-model="line.inputRemark" placeholder="备注" />
                </td>
                <td v-if="mode === 'portal'">
                  <button
                    v-if="line.confirmStatus !== 'CONFIRMED'"
                    class="link-button"
                    :disabled="saving"
                    @click="confirmLine(line)"
                  >
                    确认
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="mode === 'portal'" class="form-grid modal-section">
          <label
            ><span>整单统一确认交期</span
            ><input v-model="form.wholeConfirmedDate" type="date"
          /></label>
          <label
            ><span>整单备注</span><input v-model="form.wholeRemark"
          /></label>
        </div>
        <div v-else class="form-grid modal-section">
          <label class="full-row"
            ><span>订单变更原因 *</span
            ><textarea v-model="form.orderChangeReason" required></textarea>
          </label>
        </div>
        <div class="modal-actions">
          <button class="secondary" @click="emit('close')">关闭</button>
          <button
            v-if="mode === 'portal'"
            class="primary"
            :disabled="saving"
            @click="confirmOrder"
          >
            整单确认
          </button>
          <button
            v-else
            class="primary"
            :disabled="saving || !form.orderChangeReason"
            @click="save"
          >
            创建变更版本
          </button>
        </div></template
      >
      <form v-else @submit.prevent="save">
        <div v-if="module === 'quality' && detail?.issue" class="field-help">
          <b>{{ detail.issue.issueNo }} · {{ detail.issue.title }}</b>
          <p>
            当前状态 {{ detail.issue.status }}，历史操作
            {{ detail.history?.length || 0 }} 条。
          </p>
          <ul v-if="detail.history?.length">
            <li v-for="item in detail.history" :key="item.id">
              {{ item.occurredAt?.replace("T", " ") }} {{ item.actor }}：{{
                item.action
              }}
            </li>
          </ul>
        </div>
        <div
          v-if="module === 'settlement' && detail?.lines?.length"
          class="table-scroll modal-section"
        >
          <table>
            <thead>
              <tr>
                <th>订单</th>
                <th>行</th>
                <th>物料</th>
                <th>订单数量</th>
                <th>实收数量</th>
                <th>结算金额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="line in detail.lines" :key="line.id">
                <td>{{ line.orderNo }}</td>
                <td>{{ line.lineNo }}</td>
                <td>{{ line.materialCode }}</td>
                <td>{{ line.orderedQuantity }}</td>
                <td>{{ line.receivedQuantity }}</td>
                <td>{{ line.settlementAmount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="module === 'contracts'" class="form-grid">
          <label class="full-row"
            ><span>合同名称 *</span
            ><input
              v-model="form.contractTitle"
              :disabled="!canSubmit"
              required
          /></label>
          <label
            ><span>合同金额 *</span
            ><input
              v-model.number="form.contractAmount"
              type="number"
              min="0"
              step="0.01"
              :disabled="!canSubmit"
              required
          /></label>
          <label
            ><span>币种 *</span
            ><input
              v-model="form.contractCurrency"
              :disabled="!canSubmit"
              required
          /></label>
          <label
            ><span>生效日期 *</span
            ><input
              v-model="form.effectiveFrom"
              type="date"
              :disabled="!canSubmit"
              required
          /></label>
          <label
            ><span>结束日期 *</span
            ><input
              v-model="form.effectiveTo"
              type="date"
              :disabled="!canSubmit"
              required
          /></label>
          <label class="full-row"
            ><span>附件文件键</span
            ><input v-model="form.attachmentKey" :disabled="!canSubmit"
          /></label>
          <label class="full-row"
            ><span>版本变更原因 *</span
            ><textarea
              v-model="form.changeReason"
              :disabled="!canSubmit"
              required
            ></textarea>
          </label>
          <div
            v-if="detail.versionHistory?.length"
            class="full-row table-scroll"
          >
            <table>
              <thead>
                <tr>
                  <th>版本</th>
                  <th>状态</th>
                  <th>金额</th>
                  <th>有效期</th>
                  <th>变更原因</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in detail.versionHistory" :key="item.id">
                  <td>V{{ item.version }}</td>
                  <td>{{ item.status }}</td>
                  <td>{{ item.amount }} {{ item.currency }}</td>
                  <td>{{ item.effectiveFrom }} 至 {{ item.effectiveTo }}</td>
                  <td>{{ item.changeReason }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div
          v-else-if="module === 'delivery' && mode === 'portal'"
          class="form-grid"
        >
          <label
            ><span>承运商 *</span
            ><input v-model="form.carrierName" required /></label
          ><label
            ><span>运单号 *</span><input v-model="form.trackingNo" required
          /></label>
        </div>
        <div
          v-else-if="module === 'delivery' && row.status === 'DRAFT'"
          class="form-grid"
        >
          <label
            ><span>送货预约时间 *</span
            ><input v-model="form.appointmentAt" type="datetime-local" required
          /></label>
        </div>
        <div v-else-if="module === 'delivery'" class="form-grid">
          <label class="full-row"
            ><span>整体差异说明</span
            ><textarea v-model="form.discrepancyReason"></textarea>
          </label>
          <fieldset class="full-row">
            <legend>实收数量</legend>
            <div v-for="line in detail.lines" :key="line.id" class="line-grid">
              <span>{{ line.materialCode }}</span
              ><span>发货 {{ line.shippedQuantity }}</span
              ><input
                v-model.number="line.inputReceived"
                type="number"
                min="0"
                :max="line.shippedQuantity"
                :placeholder="line.shippedQuantity"
              /><input v-model="line.inputReason" placeholder="差异原因" />
            </div>
          </fieldset>
        </div>
        <div
          v-else-if="
            module === 'quality' &&
            mode === 'portal' &&
            ['ANALYZING', 'RETURNED'].includes(row.status)
          "
          class="form-grid"
        >
          <label class="full-row"
            ><span>根本原因 *</span
            ><textarea v-model="form.rootCause" required></textarea></label
          ><label class="full-row"
            ><span>临时措施 *</span
            ><textarea v-model="form.interimAction" required></textarea></label
          ><label class="full-row"
            ><span>永久措施 *</span
            ><textarea
              v-model="form.permanentAction"
              required
            ></textarea></label
          ><label class="full-row"
            ><span>证据文件键</span><input v-model="form.evidenceFileKey"
          /></label>
        </div>
        <div
          v-else-if="
            module === 'quality' && mode === 'portal' && row.status === 'OPEN'
          "
          class="field-help"
        >
          确认后将开始原因分析和 8D 整改。
        </div>
        <div
          v-else-if="module === 'quality' && row.status === 'ACTION_SUBMITTED'"
          class="form-grid"
        >
          <label
            ><span>审核结果 *</span
            ><select v-model="form.approved">
              <option :value="true">接受并进入验证</option>
              <option :value="false">退回整改</option>
            </select></label
          ><label class="full-row"
            ><span>审核意见 *</span
            ><textarea v-model="form.comment" required></textarea>
          </label>
        </div>
        <div v-else-if="module === 'quality' && canSubmit" class="form-grid">
          <label class="full-row"
            ><span>验证结论 *</span
            ><textarea v-model="form.verificationResult" required></textarea>
          </label>
        </div>
        <div
          v-else-if="
            module === 'settlement' &&
            mode === 'portal' &&
            ['SENT', 'DISPUTED'].includes(row.status)
          "
          class="form-grid"
        >
          <label
            ><span>处理结果 *</span
            ><select v-model="form.confirm">
              <option :value="true">确认对账</option>
              <option :value="false">提出异议</option>
            </select></label
          ><label v-if="!form.confirm" class="full-row"
            ><span>异议原因 *</span
            ><textarea v-model="form.reason" required></textarea>
          </label>
        </div>
        <div
          v-else-if="module === 'settlement' && mode === 'portal'"
          class="form-grid"
        >
          <label
            ><span>发票号 *</span
            ><input v-model="form.invoiceNo" required /></label
          ><label
            ><span>发票金额 *</span
            ><input
              v-model.number="form.amount"
              type="number"
              min="0.01"
              step="0.01"
              required /></label
          ><label
            ><span>开票日期 *</span
            ><input v-model="form.invoiceDate" type="date" required /></label
          ><label
            ><span>附件文件键</span><input v-model="form.attachmentKey"
          /></label>
        </div>
        <div class="modal-actions">
          <button type="button" class="secondary" @click="emit('close')">
            取消</button
          ><button v-if="canSubmit" class="primary" :disabled="saving">
            {{ saving ? "处理中…" : "确认提交" }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
