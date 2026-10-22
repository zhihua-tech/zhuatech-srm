<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ -->
<script setup>
import { onMounted, reactive, ref } from "vue";
import AppShell from "../../components/AppShell.vue";
import StatusPill from "../../components/StatusPill.vue";
import http from "../../api/http";

const loading = ref(true);
const saving = ref(false);
const error = ref("");
const success = ref("");
const detail = ref(null);
const questionnaire = reactive({
  capacity: "",
  qualitySystem: "",
  deliveryCapability: "",
  complianceStatement: "",
  serviceCapability: "",
});
const reason = ref("供应商自助更新");
const profile = reactive({
  supplierName: "",
  registrationNo: "",
  supplyScope: "",
  legalRepresentative: "",
  registeredAddress: "",
  contactName: "",
  contactPhone: "",
  contactEmail: "",
  bankAccountName: "",
  bankName: "",
  bankAccountNo: "",
});
const contact = reactive({
  name: "",
  positionName: "",
  phone: "",
  email: "",
  primaryContact: false,
});
const certificate = reactive({
  certificateType: "",
  certificateNo: "",
  validFrom: "",
  expiresOn: "",
  fileKey: "",
});

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const data = (await http.get("/portal/profile")).data.data;
    detail.value = data.detail;
    Object.assign(profile, data.profile);
    try {
      Object.assign(
        questionnaire,
        JSON.parse(data.detail.onboarding?.questionnaireJson || "{}"),
      );
    } catch {
      error.value = "历史准入问卷格式异常，请重新填写后保存。";
    }
  } catch (exception) {
    error.value = exception.response?.data?.message || "企业资料加载失败";
  } finally {
    loading.value = false;
  }
}

async function saveProfile() {
  saving.value = true;
  error.value = "";
  try {
    await http.put("/portal/profile", profile, {
      params: { reason: reason.value },
    });
    success.value = "企业资料已保存，变更内容已留痕。";
    await load();
  } catch (exception) {
    error.value = exception.response?.data?.message || "企业资料保存失败";
  } finally {
    saving.value = false;
  }
}

async function addContact() {
  try {
    await http.post("/portal/profile/contacts", contact);
    Object.assign(contact, {
      name: "",
      positionName: "",
      phone: "",
      email: "",
      primaryContact: false,
    });
    success.value = "联系人已添加。";
    await load();
  } catch (exception) {
    error.value = exception.response?.data?.message || "联系人添加失败";
  }
}

async function addCertificate() {
  try {
    await http.post("/portal/profile/certificates", certificate);
    Object.assign(certificate, {
      certificateType: "",
      certificateNo: "",
      validFrom: "",
      expiresOn: "",
      fileKey: "",
    });
    success.value = "资质证照已添加。";
    await load();
  } catch (exception) {
    error.value = exception.response?.data?.message || "资质证照添加失败";
  }
}

async function saveQuestionnaire(submit = false) {
  error.value = "";
  try {
    await http.put(
      "/portal/onboarding/questionnaire",
      JSON.stringify(questionnaire),
      {
        headers: { "Content-Type": "application/json" },
      },
    );
    if (submit) await http.post("/portal/onboarding/submit");
    success.value = submit ? "准入申请已提交审批。" : "准入问卷草稿已保存。";
    await load();
  } catch (exception) {
    error.value = exception.response?.data?.message || "准入问卷处理失败";
  }
}

onMounted(load);
</script>

<template>
  <AppShell mode="portal">
    <div class="page-head">
      <div>
        <span class="breadcrumb">供应商门户 / 企业资料</span>
        <h1>企业资料与准入</h1>
        <p>维护企业、联系人、银行和资质信息，并提交准入审批。</p>
      </div>
      <button class="secondary" @click="load">刷新</button>
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
      <p>正在加载企业资料…</p>
    </div>
    <template v-else-if="detail">
      <form class="panel profile-section" @submit.prevent="saveProfile">
        <div class="section-title">
          <h2>企业基本信息</h2>
          <StatusPill :text="detail.supplier.status" />
        </div>
        <div class="form-grid">
          <label
            ><span>企业名称 *</span
            ><input v-model="profile.supplierName" required
          /></label>
          <label
            ><span>统一社会信用代码 *</span
            ><input v-model="profile.registrationNo" required
          /></label>
          <label class="full-row"
            ><span>供货范围 *</span
            ><input v-model="profile.supplyScope" required
          /></label>
          <label
            ><span>法定代表人 *</span
            ><input v-model="profile.legalRepresentative" required
          /></label>
          <label
            ><span>联系人 *</span><input v-model="profile.contactName" required
          /></label>
          <label
            ><span>联系电话 *</span
            ><input v-model="profile.contactPhone" required
          /></label>
          <label
            ><span>联系邮箱 *</span
            ><input v-model="profile.contactEmail" type="email" required
          /></label>
          <label class="full-row"
            ><span>注册地址 *</span
            ><input v-model="profile.registeredAddress" required
          /></label>
          <label
            ><span>账户名 *</span
            ><input v-model="profile.bankAccountName" required
          /></label>
          <label
            ><span>开户行 *</span><input v-model="profile.bankName" required
          /></label>
          <label
            ><span>银行账号 *</span
            ><input v-model="profile.bankAccountNo" required
          /></label>
          <label
            ><span>变更原因 *</span><input v-model="reason" required
          /></label>
        </div>
        <div class="form-actions">
          <button class="primary" :disabled="saving">
            {{ saving ? "保存中…" : "保存企业资料" }}
          </button>
        </div>
      </form>

      <section class="panel profile-section">
        <div class="section-title">
          <h2>联系人</h2>
          <span>{{ detail.contacts.length }} 人</span>
        </div>
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>职位</th>
                <th>电话</th>
                <th>邮箱</th>
                <th>主联系人</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in detail.contacts" :key="item.id">
                <td>{{ item.name }}</td>
                <td>{{ item.positionName || "—" }}</td>
                <td>{{ item.phone || "—" }}</td>
                <td>{{ item.email || "—" }}</td>
                <td>{{ item.primaryContact ? "是" : "否" }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <form class="inline-form" @submit.prevent="addContact">
          <input v-model="contact.name" placeholder="姓名" required /><input
            v-model="contact.positionName"
            placeholder="职位"
          /><input v-model="contact.phone" placeholder="电话" /><input
            v-model="contact.email"
            type="email"
            placeholder="邮箱"
          /><label
            ><input
              v-model="contact.primaryContact"
              type="checkbox"
            />主联系人</label
          ><button class="secondary">添加</button>
        </form>
      </section>

      <section class="panel profile-section">
        <div class="section-title">
          <h2>资质证照</h2>
          <span>{{ detail.certificates.length }} 项</span>
        </div>
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>类型</th>
                <th>证书编号</th>
                <th>生效日</th>
                <th>到期日</th>
                <th>验证</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in detail.certificates" :key="item.id">
                <td>{{ item.type }}</td>
                <td>{{ item.no }}</td>
                <td>{{ item.validFrom }}</td>
                <td>{{ item.expiresOn }}</td>
                <td>{{ item.verified ? "已验证" : "待验证" }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <form class="inline-form" @submit.prevent="addCertificate">
          <input
            v-model="certificate.certificateType"
            placeholder="证照类型"
            required
          /><input
            v-model="certificate.certificateNo"
            placeholder="证照编号"
            required
          /><input v-model="certificate.validFrom" type="date" required /><input
            v-model="certificate.expiresOn"
            type="date"
            required
          /><button class="secondary">添加</button>
        </form>
      </section>

      <section class="panel profile-section">
        <div class="section-title">
          <h2>准入问卷</h2>
          <StatusPill :text="detail.onboarding?.status || 'DRAFT'" />
        </div>
        <p class="field-help">
          请如实填写产能、质量、交付、合规和服务能力，提交后进入采购方准入审批。
        </p>
        <div class="form-grid">
          <label
            ><span>生产或服务能力 *</span
            ><textarea v-model="questionnaire.capacity" required></textarea>
          </label>
          <label
            ><span>质量管理体系 *</span
            ><textarea
              v-model="questionnaire.qualitySystem"
              required
            ></textarea>
          </label>
          <label
            ><span>交付保障能力 *</span
            ><textarea
              v-model="questionnaire.deliveryCapability"
              required
            ></textarea>
          </label>
          <label
            ><span>合规声明 *</span
            ><textarea
              v-model="questionnaire.complianceStatement"
              required
            ></textarea>
          </label>
          <label class="full-row"
            ><span>售后与服务能力 *</span
            ><textarea
              v-model="questionnaire.serviceCapability"
              required
            ></textarea>
          </label>
        </div>
        <div class="form-actions">
          <button class="secondary" @click="saveQuestionnaire(false)">
            保存草稿</button
          ><button class="primary" @click="saveQuestionnaire(true)">
            提交准入审批
          </button>
        </div>
      </section>
    </template>
  </AppShell>
</template>
