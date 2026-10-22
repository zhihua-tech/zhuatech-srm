<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ -->
<script setup>
import { computed, onMounted, ref } from "vue";
import AppShell from "../../components/AppShell.vue";
import StatusPill from "../../components/StatusPill.vue";
import http from "../../api/http";

const loading = ref(true),
  saving = ref(false),
  error = ref(""),
  success = ref("");
const suppliers = ref([]),
  keyword = ref(""),
  showInvite = ref(false),
  selected = ref(null),
  detail = ref(null);
const approvalComment = ref(""),
  statusReason = ref(""),
  targetStatus = ref("ACTIVE");
const invite = ref({
  supplierName: "",
  email: "",
  contactName: "",
  validDays: 7,
});
const filtered = computed(() =>
  suppliers.value.filter(
    (item) =>
      !keyword.value ||
      item.name?.includes(keyword.value) ||
      item.code?.includes(keyword.value) ||
      item.registrationNo?.includes(keyword.value),
  ),
);

async function load() {
  loading.value = true;
  error.value = "";
  try {
    suppliers.value =
      (await http.get("/admin/suppliers/page?size=100")).data.data.content ||
      [];
  } catch (exception) {
    error.value = exception.response?.data?.message || "供应商数据加载失败";
  } finally {
    loading.value = false;
  }
}
async function openDetail(item) {
  selected.value = item;
  detail.value = null;
  error.value = "";
  try {
    detail.value = (await http.get(`/admin/suppliers/${item.id}`)).data.data;
  } catch (exception) {
    error.value = exception.response?.data?.message || "供应商详情加载失败";
  }
}
async function submitInvite() {
  saving.value = true;
  error.value = "";
  try {
    const result = (
      await http.post("/admin/supplier-invitations", invite.value)
    ).data.data;
    success.value = `邀请已创建，请通过安全渠道发送注册令牌：${result.token}`;
    showInvite.value = false;
    invite.value = {
      supplierName: "",
      email: "",
      contactName: "",
      validDays: 7,
    };
  } catch (exception) {
    error.value = exception.response?.data?.message || "邀请创建失败";
  } finally {
    saving.value = false;
  }
}
async function approve(action) {
  if (!approvalComment.value.trim()) {
    error.value = "请填写审批意见";
    return;
  }
  saving.value = true;
  try {
    await http.post(`/admin/onboarding/${detail.value.onboarding.id}/approve`, {
      action,
      comment: approvalComment.value,
    });
    success.value = "准入审批已处理。";
    approvalComment.value = "";
    await openDetail(selected.value);
    await load();
  } catch (exception) {
    error.value = exception.response?.data?.message || "准入审批失败";
  } finally {
    saving.value = false;
  }
}
async function changeStatus() {
  if (!statusReason.value.trim()) {
    error.value = "请填写状态变更原因";
    return;
  }
  saving.value = true;
  try {
    await http.patch(`/admin/suppliers/${selected.value.id}/status`, {
      status: targetStatus.value,
      reason: statusReason.value,
    });
    success.value = "供应商合作状态已更新并留痕。";
    statusReason.value = "";
    await openDetail(selected.value);
    await load();
  } catch (exception) {
    error.value = exception.response?.data?.message || "状态变更失败";
  } finally {
    saving.value = false;
  }
}
onMounted(load);
</script>

<template>
  <AppShell mode="admin">
    <div class="page-head">
      <div>
        <span class="breadcrumb">供应商管理 / 供应商库</span>
        <h1>供应商库</h1>
        <p>统一维护企业资料、资质、准入、合作状态和变更记录。</p>
      </div>
      <button class="primary" @click="showInvite = true">邀请供应商</button>
    </div>
    <div v-if="success" class="notice success-notice">
      <b>操作成功</b><span>{{ success }}</span
      ><button @click="success = ''">关闭</button>
    </div>
    <div v-if="error" class="notice error-notice">
      <b>操作失败</b><span>{{ error }}</span
      ><button @click="error = ''">关闭</button>
    </div>
    <section class="panel list-panel">
      <div class="toolbar">
        <div class="table-summary">
          <span>供应商 {{ suppliers.length }} 家</span>
        </div>
        <div class="filters">
          <input v-model="keyword" placeholder="名称、编码或信用代码" /><button
            class="secondary"
            @click="load"
          >
            刷新
          </button>
        </div>
      </div>
      <div v-if="loading" class="state-box">
        <span class="loader"></span>
        <p>正在加载供应商数据…</p>
      </div>
      <div v-else-if="!filtered.length" class="state-box">
        <b>暂无供应商</b>
        <p>可通过“邀请供应商”开始准入流程。</p>
      </div>
      <div v-else class="table-scroll">
        <table class="supplier-table">
          <thead>
            <tr>
              <th>供应商</th>
              <th>统一社会信用代码</th>
              <th>供货范围</th>
              <th>联系人</th>
              <th>准入完整度</th>
              <th>合作状态</th>
              <th>风险</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filtered" :key="item.id">
              <td>
                <div class="supplier-name">
                  <span>{{ item.name?.slice(0, 1) }}</span>
                  <div>
                    <b>{{ item.name }}</b
                    ><small>{{ item.code }}</small>
                  </div>
                </div>
              </td>
              <td>{{ item.registrationNo }}</td>
              <td>{{ item.supplyScope }}</td>
              <td>
                {{ item.contactName
                }}<small class="block">{{ item.contactPhone }}</small>
              </td>
              <td>{{ item.qualificationProgress }}%</td>
              <td><StatusPill :text="item.status" /></td>
              <td><StatusPill :text="item.riskLevel" /></td>
              <td>
                <button class="link-button" @click="openDetail(item)">
                  查看与审批
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="showInvite" class="modal-mask" @click.self="showInvite = false">
      <form class="modal" @submit.prevent="submitInvite">
        <div class="modal-head">
          <div>
            <h2>邀请供应商</h2>
            <p>创建限时注册邀请，供应商注册后进入准入流程。</p>
          </div>
          <button type="button" @click="showInvite = false">×</button>
        </div>
        <div class="form-grid">
          <label
            ><span>企业名称 *</span
            ><input
              v-model="invite.supplierName"
              required
              maxlength="120" /></label
          ><label
            ><span>联系人 *</span
            ><input
              v-model="invite.contactName"
              required
              maxlength="50" /></label
          ><label class="full-row"
            ><span>企业邮箱 *</span
            ><input v-model="invite.email" type="email" required /></label
          ><label
            ><span>有效天数 *</span
            ><input
              v-model.number="invite.validDays"
              type="number"
              min="1"
              max="30"
              required
          /></label>
        </div>
        <div class="modal-actions">
          <button type="button" class="secondary" @click="showInvite = false">
            取消</button
          ><button class="primary" :disabled="saving">
            {{ saving ? "正在创建…" : "创建邀请" }}
          </button>
        </div>
      </form>
    </div>

    <div v-if="selected" class="modal-mask" @click.self="selected = null">
      <section class="modal modal-large">
        <div class="modal-head">
          <div>
            <h2>{{ selected.name }}</h2>
            <p>{{ selected.code }} · {{ selected.registrationNo }}</p>
          </div>
          <button @click="selected = null">×</button>
        </div>
        <div v-if="!detail" class="state-box compact">
          <span class="loader"></span>
        </div>
        <template v-else
          ><div class="requirement">
            <div>
              <span>状态</span
              ><b><StatusPill :text="detail.supplier.status" /></b>
            </div>
            <div>
              <span>准入申请</span
              ><b>{{ detail.onboarding?.status || "尚未提交" }}</b>
            </div>
            <div>
              <span>资质数</span><b>{{ detail.certificates.length }}</b>
            </div>
            <div>
              <span>联系人</span><b>{{ detail.contacts.length }}</b>
            </div>
          </div>
          <div v-if="detail.onboarding" class="profile-section">
            <div class="section-title">
              <h3>准入审批</h3>
              <StatusPill :text="detail.onboarding.status" />
            </div>
            <textarea
              v-model="approvalComment"
              class="questionnaire"
              placeholder="填写审批意见"
            ></textarea>
            <div class="form-actions">
              <button
                class="secondary"
                :disabled="saving"
                @click="approve('RETURN')"
              >
                退回修改</button
              ><button
                v-if="detail.onboarding.status === 'SUBMITTED'"
                class="primary"
                :disabled="saving"
                @click="approve('FIRST_APPROVE')"
              >
                初审通过</button
              ><button
                v-if="detail.onboarding.status === 'FIRST_APPROVED'"
                class="primary"
                :disabled="saving"
                @click="approve('FINAL_APPROVE')"
              >
                终审通过
              </button>
            </div>
          </div>
          <div class="profile-section">
            <div class="section-title"><h3>合作状态</h3></div>
            <div class="inline-form">
              <select v-model="targetStatus">
                <option>ACTIVE</option>
                <option>RECTIFYING</option>
                <option>SUSPENDED</option>
                <option>ELIMINATED</option>
                <option>BLACKLISTED</option></select
              ><input
                v-model="statusReason"
                placeholder="状态变更原因"
              /><button
                class="secondary"
                :disabled="saving"
                @click="changeStatus"
              >
                更新状态
              </button>
            </div>
          </div>
          <div class="profile-section">
            <div class="section-title">
              <h3>变更记录</h3>
              <span>{{ detail.changeHistory.length }} 条</span>
            </div>
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>字段</th>
                    <th>原值</th>
                    <th>新值</th>
                    <th>原因</th>
                    <th>操作人</th>
                    <th>时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in detail.changeHistory" :key="item.id">
                    <td>{{ item.field }}</td>
                    <td>{{ item.beforeValue }}</td>
                    <td>{{ item.afterValue }}</td>
                    <td>{{ item.reason }}</td>
                    <td>{{ item.changedBy }}</td>
                    <td>
                      {{ item.changedAt?.replace("T", " ").slice(0, 19) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div></template
        >
      </section>
    </div>
  </AppShell>
</template>
