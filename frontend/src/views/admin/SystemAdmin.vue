<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ -->
<script setup>
import { onMounted, reactive, ref } from "vue";
import AppShell from "../../components/AppShell.vue";
import StatusPill from "../../components/StatusPill.vue";
import http from "../../api/http";

const organizations = ref([]),
  users = ref([]),
  suppliers = ref([]),
  error = ref(""),
  success = ref(""),
  saving = ref(false);
const role = ref("BUYER"),
  permissionText = ref(""),
  usingDefaults = ref(true);
const organization = reactive({ code: "", name: "" });
const user = reactive({
  username: "",
  password: "",
  fullName: "",
  role: "BUYER",
  supplierId: null,
});
const roles = ["ADMIN", "BUYER", "QUALITY", "FINANCE", "AUDITOR", "SUPPLIER"];
async function load() {
  error.value = "";
  try {
    const [orgs, accounts, vendors] = await Promise.all([
      http.get("/admin/system/organizations"),
      http.get("/admin/system/users"),
      http.get("/admin/suppliers/page?size=100"),
    ]);
    organizations.value = orgs.data.data || [];
    users.value = accounts.data.data || [];
    suppliers.value = vendors.data.data.content || [];
    await loadPermissions();
  } catch (e) {
    error.value = e.response?.data?.message || "系统配置加载失败";
  }
}
async function loadPermissions() {
  const data = (await http.get(`/admin/system/roles/${role.value}/permissions`))
    .data.data;
  usingDefaults.value = data.usingDefaults;
  permissionText.value = [...data.permissions].sort().join("\n");
}
async function createOrganization() {
  saving.value = true;
  try {
    await http.post("/admin/system/organizations", organization);
    Object.assign(organization, { code: "", name: "" });
    success.value = "组织已创建。";
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || "组织创建失败";
  } finally {
    saving.value = false;
  }
}
async function createUser() {
  saving.value = true;
  try {
    await http.post("/admin/system/users", {
      ...user,
      supplierId: user.role === "SUPPLIER" ? Number(user.supplierId) : null,
    });
    Object.assign(user, {
      username: "",
      password: "",
      fullName: "",
      role: "BUYER",
      supplierId: null,
    });
    success.value = "账号已创建。";
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || "账号创建失败";
  } finally {
    saving.value = false;
  }
}
async function toggleUser(item) {
  try {
    await http.patch(
      `/admin/system/users/${item.id}/enabled?enabled=${!item.enabled}`,
    );
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || "账号状态更新失败";
  }
}
async function savePermissions() {
  const permissions = [
    ...new Set(
      permissionText.value
        .split(/[\s,]+/)
        .map((value) => value.trim())
        .filter(Boolean),
    ),
  ];
  try {
    await http.put(`/admin/system/roles/${role.value}/permissions`, {
      permissions,
    });
    success.value = "角色权限已替换并记录审计。";
    await loadPermissions();
  } catch (e) {
    error.value = e.response?.data?.message || "角色权限保存失败";
  }
}
onMounted(load);
</script>
<template>
  <AppShell mode="admin"
    ><div class="page-head">
      <div>
        <span class="breadcrumb">系统管理 / 权限配置</span>
        <h1>组织、账号与角色权限</h1>
        <p>系统管理员维护组织、后端权限和账号启停，变更均记录审计。</p>
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
    <div class="dashboard-grid">
      <section class="panel profile-section">
        <div class="section-title">
          <h2>组织</h2>
          <span>{{ organizations.length }} 个</span>
        </div>
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>编码</th>
                <th>名称</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in organizations" :key="item.id">
                <td>
                  <b>{{ item.code }}</b>
                </td>
                <td>{{ item.name }}</td>
                <td>
                  <StatusPill :text="item.enabled ? 'ENABLED' : 'DISABLED'" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <form class="inline-form" @submit.prevent="createOrganization">
          <input
            v-model="organization.code"
            placeholder="组织编码"
            required
          /><input
            v-model="organization.name"
            placeholder="组织名称"
            required
          /><button class="secondary" :disabled="saving">创建</button>
        </form>
      </section>
      <section class="panel profile-section">
        <div class="section-title">
          <h2>角色权限</h2>
          <span>{{ usingDefaults ? "使用默认矩阵" : "已自定义" }}</span>
        </div>
        <div class="form-grid">
          <label
            ><span>角色</span
            ><select v-model="role" @change="loadPermissions">
              <option v-for="item in roles" :key="item">{{ item }}</option>
            </select></label
          ><label class="full-row"
            ><span>权限编码（换行或逗号分隔）</span
            ><textarea
              v-model="permissionText"
              class="questionnaire"
            ></textarea>
          </label>
        </div>
        <div class="form-actions">
          <button class="primary" @click="savePermissions">保存权限</button>
        </div>
      </section>
    </div>
    <section class="panel profile-section">
      <div class="section-title">
        <h2>账号</h2>
        <span>{{ users.length }} 个</span>
      </div>
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>用户名</th>
              <th>姓名</th>
              <th>角色</th>
              <th>供应商</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in users" :key="item.id">
              <td>
                <b>{{ item.username }}</b>
              </td>
              <td>{{ item.fullName }}</td>
              <td>{{ item.role }}</td>
              <td>{{ item.supplierId || "—" }}</td>
              <td>
                <StatusPill :text="item.enabled ? 'ENABLED' : 'DISABLED'" />
              </td>
              <td>
                <button class="link-button" @click="toggleUser(item)">
                  {{ item.enabled ? "停用" : "启用" }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <form class="inline-form" @submit.prevent="createUser">
        <input v-model="user.username" placeholder="用户名" required /><input
          v-model="user.fullName"
          placeholder="姓名"
          required
        /><input
          v-model="user.password"
          type="password"
          placeholder="初始密码（至少 12 位字母+数字）"
          required
        /><select v-model="user.role">
          <option v-for="item in roles" :key="item">{{ item }}</option></select
        ><select
          v-if="user.role === 'SUPPLIER'"
          v-model="user.supplierId"
          required
        >
          <option :value="null">选择供应商</option>
          <option v-for="item in suppliers" :key="item.id" :value="item.id">
            {{ item.code }} · {{ item.name }}
          </option></select
        ><button class="secondary" :disabled="saving">创建账号</button>
      </form>
    </section></AppShell
  >
</template>
