<script setup>
import { onMounted, reactive, ref } from "vue";
import http from "../api/http";
const props = defineProps({ module: String, mode: String });
const emit = defineEmits(["close", "saved"]);
const saving = ref(false),
  error = ref(""),
  suppliers = ref([]),
  orders = ref([]),
  orderDetail = ref(null);
const today = new Date().toISOString().slice(0, 10),
  future = (days) =>
    new Date(Date.now() + days * 86400000).toISOString().slice(0, 10);
const form = reactive({
  eventNo: "",
  type: "RFQ",
  title: "",
  categoryName: "",
  requirementDescription: "",
  deadline: "",
  budget: null,
  supplierIds: [],
  items: [
    {
      materialCode: "",
      materialName: "",
      specification: "",
      quantity: null,
      unit: "件",
      requiredDate: future(30),
    },
  ],
  contractNo: "",
  supplierId: "",
  amount: null,
  currency: "CNY",
  effectiveFrom: today,
  effectiveTo: future(365),
  changeReason: "首次创建",
  orderNo: "",
  supplierCode: "",
  procurementOrganization: "",
  factoryCode: "",
  lines: [
    {
      lineNo: 10,
      materialCode: "",
      materialName: "",
      specification: "",
      quantity: null,
      unitPrice: null,
      unit: "件",
      requestedDate: future(30),
    },
  ],
  materialCode: "",
  materialName: "",
  quotaType: "QUANTITY",
  totalQuantity: null,
  validFrom: today,
  validTo: future(90),
  allocations: [{ supplierId: "", value: null, note: "" }],
  issueNo: "",
  issueType: "NCR",
  severity: "MAJOR",
  dueDate: future(14),
  statementNo: "",
  periodFrom: today,
  periodTo: future(30),
  qualityScore: 80,
  deliveryScore: 80,
  costScore: 80,
  serviceScore: 80,
  asnNo: "",
  asnOrderId: "",
  plannedArrivalAt: "",
  asnLines: [],
});
onMounted(async () => {
  try {
    if (props.mode === "portal" && props.module === "delivery")
      orders.value = (await http.get("/portal/orders/list")).data.data || [];
    else
      suppliers.value =
        (await http.get("/admin/suppliers/page?size=100")).data.data.content ||
        [];
  } catch {}
});
async function chooseAsnOrder() {
  if (!form.asnOrderId) {
    orderDetail.value = null;
    form.asnLines = [];
    return;
  }
  try {
    orderDetail.value = (
      await http.get(`/portal/orders/${form.asnOrderId}`)
    ).data.data;
    form.asnLines = orderDetail.value.lines.map((line) => ({
      orderLineId: line.id,
      materialCode: line.materialCode,
      materialName: line.materialName,
      remaining:
        Number(line.orderedQuantity) - Number(line.receivedQuantity || 0),
      shippedQuantity:
        Number(line.orderedQuantity) - Number(line.receivedQuantity || 0),
    }));
  } catch (e) {
    error.value = e.response?.data?.message || "订单明细加载失败";
  }
}
function addItem() {
  form.items.push({
    materialCode: "",
    materialName: "",
    specification: "",
    quantity: null,
    unit: "件",
    requiredDate: future(30),
  });
}
function addLine() {
  form.lines.push({
    lineNo: (form.lines.length + 1) * 10,
    materialCode: "",
    materialName: "",
    specification: "",
    quantity: null,
    unitPrice: null,
    unit: "件",
    requestedDate: future(30),
  });
}
function addAllocation() {
  form.allocations.push({ supplierId: "", value: null, note: "" });
}
function endpointAndPayload() {
  switch (props.module) {
    case "sourcing":
      return [
        "/admin/sourcing-events",
        {
          eventNo: form.eventNo,
          type: form.type,
          title: form.title,
          categoryName: form.categoryName,
          requirementDescription: form.requirementDescription,
          deadline: form.deadline,
          budget: form.budget,
          items: form.items,
          supplierIds: form.supplierIds.map(Number),
          publish: true,
        },
      ];
    case "contracts":
      return [
        "/contracts",
        {
          contractNo: form.contractNo,
          supplierId: Number(form.supplierId),
          title: form.title,
          amount: form.amount,
          currency: form.currency,
          effectiveFrom: form.effectiveFrom,
          effectiveTo: form.effectiveTo,
          attachmentKey: null,
          changeReason: form.changeReason,
        },
      ];
    case "orders":
      return [
        "/admin/orders",
        {
          orderNo: form.orderNo,
          supplierCode: form.supplierCode,
          procurementOrganization: form.procurementOrganization,
          factoryCode: form.factoryCode,
          currency: form.currency,
          lines: form.lines,
        },
      ];
    case "quotas":
      return [
        "/admin/quotas",
        {
          procurementOrganization: form.procurementOrganization,
          factoryCode: form.factoryCode,
          materialCode: form.materialCode,
          materialName: form.materialName,
          quotaType: form.quotaType,
          totalQuantity: form.totalQuantity,
          validFrom: form.validFrom,
          validTo: form.validTo,
          allocations: form.allocations.map((x) => ({
            ...x,
            supplierId: Number(x.supplierId),
          })),
          changeReason: form.changeReason,
        },
      ];
    case "quality":
      return [
        "/admin/quality-issues",
        {
          issueNo: form.issueNo,
          supplierId: Number(form.supplierId),
          type: form.issueType,
          title: form.title,
          severity: form.severity,
          dueDate: form.dueDate,
        },
      ];
    case "settlement":
      return [
        "/admin/reconciliations",
        {
          statementNo: form.statementNo,
          supplierId: Number(form.supplierId),
          periodFrom: form.periodFrom,
          periodTo: form.periodTo,
        },
      ];
    case "performance":
      return [
        "/admin/performance",
        {
          supplierId: Number(form.supplierId),
          periodFrom: form.periodFrom,
          periodTo: form.periodTo,
          qualityScore: form.qualityScore,
          deliveryScore: form.deliveryScore,
          costScore: form.costScore,
          serviceScore: form.serviceScore,
        },
      ];
    case "delivery":
      return [
        "/portal/asns",
        {
          asnNo: form.asnNo,
          orderId: Number(form.asnOrderId),
          plannedArrivalAt: form.plannedArrivalAt,
          lines: form.asnLines.map((line) => ({
            orderLineId: line.orderLineId,
            shippedQuantity: Number(line.shippedQuantity),
          })),
        },
      ];
    default:
      throw new Error("当前模块不支持新建");
  }
}
async function save() {
  saving.value = true;
  error.value = "";
  try {
    const [endpoint, payload] = endpointAndPayload();
    await http.post(endpoint, payload);
    emit("saved");
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "保存失败";
  } finally {
    saving.value = false;
  }
}
</script>
<template>
  <div class="modal-mask" @click.self="emit('close')">
    <form class="modal modal-large" @submit.prevent="save">
      <div class="modal-head">
        <div>
          <h2>新建业务数据</h2>
          <p>带 * 的字段为必填项，保存前请核对组织和供应商范围。</p>
        </div>
        <button type="button" @click="emit('close')">×</button>
      </div>
      <div v-if="error" class="inline-error">{{ error }}</div>
      <div v-if="module === 'sourcing'" class="form-grid">
        <label
          ><span>项目编号 *</span
          ><input v-model="form.eventNo" required /></label
        ><label
          ><span>类型 *</span
          ><select v-model="form.type">
            <option>RFQ</option>
            <option>RFP</option>
          </select></label
        ><label class="full-row"
          ><span>项目名称 *</span><input v-model="form.title" required /></label
        ><label
          ><span>品类 *</span
          ><input v-model="form.categoryName" required /></label
        ><label
          ><span>截止时间 *</span
          ><input
            v-model="form.deadline"
            type="datetime-local"
            required /></label
        ><label
          ><span>预算金额 *</span
          ><input
            v-model.number="form.budget"
            type="number"
            min="0"
            required /></label
        ><label class="full-row"
          ><span>邀请供应商 *</span
          ><select v-model="form.supplierIds" multiple required>
            <option v-for="s in suppliers" :key="s.id" :value="s.id">
              {{ s.code }} · {{ s.name }}
            </option></select
          ><small>按住 Ctrl / Command 可多选</small></label
        ><label class="full-row"
          ><span>需求说明</span
          ><textarea v-model="form.requirementDescription"></textarea>
        </label>
        <fieldset class="full-row">
          <legend>物料明细</legend>
          <div
            v-for="(line, index) in form.items"
            :key="index"
            class="line-grid"
          >
            <input
              v-model="line.materialCode"
              placeholder="物料编码"
              required
            /><input
              v-model="line.materialName"
              placeholder="物料名称"
              required
            /><input v-model="line.specification" placeholder="规格" /><input
              v-model.number="line.quantity"
              type="number"
              min="0.0001"
              step="0.0001"
              placeholder="数量"
              required
            /><input v-model="line.unit" placeholder="单位" required /><input
              v-model="line.requiredDate"
              type="date"
              required
            />
          </div>
          <button type="button" class="link-button" @click="addItem">
            + 添加物料
          </button>
        </fieldset>
      </div>
      <div v-else-if="module === 'contracts'" class="form-grid">
        <label
          ><span>合同编号 *</span
          ><input v-model="form.contractNo" required /></label
        ><label
          ><span>供应商 *</span
          ><select v-model="form.supplierId" required>
            <option value="">请选择</option>
            <option v-for="s in suppliers" :key="s.id" :value="s.id">
              {{ s.code }} · {{ s.name }}
            </option>
          </select></label
        ><label class="full-row"
          ><span>合同名称 *</span><input v-model="form.title" required /></label
        ><label
          ><span>合同金额 *</span
          ><input
            v-model.number="form.amount"
            type="number"
            min="0"
            required /></label
        ><label
          ><span>币种 *</span><input v-model="form.currency" required /></label
        ><label
          ><span>生效日期 *</span
          ><input v-model="form.effectiveFrom" type="date" required /></label
        ><label
          ><span>到期日期 *</span
          ><input v-model="form.effectiveTo" type="date" required /></label
        ><label class="full-row"
          ><span>版本原因 *</span
          ><textarea v-model="form.changeReason" required></textarea>
        </label>
      </div>
      <div v-else-if="module === 'orders'" class="form-grid">
        <label
          ><span>订单编号 *</span
          ><input v-model="form.orderNo" required /></label
        ><label
          ><span>供应商 *</span
          ><select v-model="form.supplierCode" required>
            <option value="">请选择</option>
            <option v-for="s in suppliers" :key="s.id" :value="s.code">
              {{ s.code }} · {{ s.name }}
            </option>
          </select></label
        ><label
          ><span>采购组织 *</span
          ><input v-model="form.procurementOrganization" required /></label
        ><label
          ><span>工厂 *</span
          ><input v-model="form.factoryCode" required /></label
        ><label
          ><span>币种 *</span><input v-model="form.currency" required
        /></label>
        <fieldset class="full-row">
          <legend>订单明细</legend>
          <div
            v-for="(line, index) in form.lines"
            :key="index"
            class="line-grid"
          >
            <input
              v-model.number="line.lineNo"
              type="number"
              placeholder="行号"
              required
            /><input
              v-model="line.materialCode"
              placeholder="物料编码"
              required
            /><input
              v-model="line.materialName"
              placeholder="物料名称"
              required
            /><input
              v-model.number="line.quantity"
              type="number"
              min="0.0001"
              step="0.0001"
              placeholder="数量"
              required
            /><input
              v-model.number="line.unitPrice"
              type="number"
              min="0"
              step="0.01"
              placeholder="单价"
              required
            /><input v-model="line.requestedDate" type="date" required />
          </div>
          <button type="button" class="link-button" @click="addLine">
            + 添加订单行
          </button>
        </fieldset>
      </div>
      <div v-else-if="module === 'quotas'" class="form-grid">
        <label
          ><span>采购组织 *</span
          ><input v-model="form.procurementOrganization" required /></label
        ><label
          ><span>工厂 *</span
          ><input v-model="form.factoryCode" required /></label
        ><label
          ><span>物料编码 *</span
          ><input v-model="form.materialCode" required /></label
        ><label
          ><span>物料名称 *</span
          ><input v-model="form.materialName" required /></label
        ><label
          ><span>配额类型 *</span
          ><select v-model="form.quotaType">
            <option value="QUANTITY">数量配额</option>
            <option value="RATIO">比例配额</option>
          </select></label
        ><label
          ><span>总数量 *</span
          ><input
            v-model.number="form.totalQuantity"
            type="number"
            min="0.0001"
            required /></label
        ><label
          ><span>有效期开始 *</span
          ><input v-model="form.validFrom" type="date" required /></label
        ><label
          ><span>有效期结束 *</span
          ><input v-model="form.validTo" type="date" required /></label
        ><label class="full-row"
          ><span>变更原因 *</span><input v-model="form.changeReason" required
        /></label>
        <fieldset class="full-row">
          <legend>供应商分配</legend>
          <div
            v-for="(line, index) in form.allocations"
            :key="index"
            class="allocation-grid"
          >
            <select v-model="line.supplierId" required>
              <option value="">选择供应商</option>
              <option v-for="s in suppliers" :key="s.id" :value="s.id">
                {{ s.code }} · {{ s.name }}
              </option></select
            ><input
              v-model.number="line.value"
              type="number"
              min="0.0001"
              step="0.0001"
              :placeholder="form.quotaType === 'RATIO' ? '比例' : '数量'"
              required
            /><input v-model="line.note" placeholder="说明" />
          </div>
          <button type="button" class="link-button" @click="addAllocation">
            + 添加供应商
          </button>
        </fieldset>
      </div>
      <div v-else-if="module === 'quality'" class="form-grid">
        <label
          ><span>问题编号 *</span
          ><input v-model="form.issueNo" required /></label
        ><label
          ><span>供应商 *</span
          ><select v-model="form.supplierId" required>
            <option value="">请选择</option>
            <option v-for="s in suppliers" :key="s.id" :value="s.id">
              {{ s.code }} · {{ s.name }}
            </option>
          </select></label
        ><label
          ><span>类型 *</span
          ><select v-model="form.issueType">
            <option>NCR</option>
            <option>CAR</option>
          </select></label
        ><label
          ><span>严重度 *</span
          ><select v-model="form.severity">
            <option>MINOR</option>
            <option>MAJOR</option>
            <option>CRITICAL</option>
          </select></label
        ><label class="full-row"
          ><span>问题标题 *</span><input v-model="form.title" required /></label
        ><label
          ><span>整改截止日期 *</span
          ><input v-model="form.dueDate" type="date" required
        /></label>
      </div>
      <div v-else-if="module === 'settlement'" class="form-grid">
        <label
          ><span>对账单号 *</span
          ><input v-model="form.statementNo" required /></label
        ><label
          ><span>供应商 *</span
          ><select v-model="form.supplierId" required>
            <option value="">请选择</option>
            <option v-for="s in suppliers" :key="s.id" :value="s.id">
              {{ s.code }} · {{ s.name }}
            </option>
          </select></label
        ><label
          ><span>期间开始 *</span
          ><input v-model="form.periodFrom" type="date" required /></label
        ><label
          ><span>期间结束 *</span
          ><input v-model="form.periodTo" type="date" required
        /></label>
      </div>
      <div v-else-if="module === 'performance'" class="form-grid">
        <label
          ><span>供应商 *</span
          ><select v-model="form.supplierId" required>
            <option value="">请选择</option>
            <option v-for="s in suppliers" :key="s.id" :value="s.id">
              {{ s.code }} · {{ s.name }}
            </option>
          </select></label
        ><label
          ><span>期间开始 *</span
          ><input v-model="form.periodFrom" type="date" required /></label
        ><label
          ><span>期间结束 *</span
          ><input v-model="form.periodTo" type="date" required /></label
        ><label
          ><span>质量得分 *</span
          ><input
            v-model.number="form.qualityScore"
            type="number"
            min="0"
            max="100"
            required /></label
        ><label
          ><span>交付得分 *</span
          ><input
            v-model.number="form.deliveryScore"
            type="number"
            min="0"
            max="100"
            required /></label
        ><label
          ><span>成本得分 *</span
          ><input
            v-model.number="form.costScore"
            type="number"
            min="0"
            max="100"
            required /></label
        ><label
          ><span>服务得分 *</span
          ><input
            v-model.number="form.serviceScore"
            type="number"
            min="0"
            max="100"
            required
        /></label>
      </div>
      <div v-else-if="module === 'delivery'" class="form-grid">
        <label
          ><span>ASN 编号 *</span><input v-model="form.asnNo" required
        /></label>
        <label
          ><span>采购订单 *</span
          ><select v-model="form.asnOrderId" required @change="chooseAsnOrder">
            <option value="">请选择</option>
            <option v-for="order in orders" :key="order.id" :value="order.id">
              {{ order.orderNo }} · {{ order.itemSummary }}
            </option>
          </select></label
        >
        <label class="full-row"
          ><span>计划到货时间 *</span
          ><input
            v-model="form.plannedArrivalAt"
            type="datetime-local"
            required
        /></label>
        <fieldset v-if="form.asnLines.length" class="full-row">
          <legend>发货明细</legend>
          <div
            v-for="line in form.asnLines"
            :key="line.orderLineId"
            class="line-grid"
          >
            <span>{{ line.materialCode }}</span
            ><span>{{ line.materialName }}</span
            ><span>可发 {{ line.remaining }}</span
            ><input
              v-model.number="line.shippedQuantity"
              type="number"
              min="0.0001"
              :max="line.remaining"
              step="0.0001"
              placeholder="本次发货数量"
              required
            />
          </div>
        </fieldset>
      </div>
      <div class="modal-actions">
        <button type="button" class="secondary" @click="emit('close')">
          取消</button
        ><button class="primary" :disabled="saving">
          {{ saving ? "正在保存…" : "保存" }}
        </button>
      </div>
    </form>
  </div>
</template>
