<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
const username = ref(""),
  password = ref(""),
  busy = ref(false),
  error = ref(""),
  router = useRouter(),
  auth = useAuthStore();
async function login() {
  if (!username.value || !password.value) {
    error.value = "请输入账号和密码";
    return;
  }
  busy.value = true;
  error.value = "";
  try {
    const user = await auth.login(username.value.trim(), password.value);
    router.push(user.role === "SUPPLIER" ? "/portal" : "/admin");
  } catch (e) {
    error.value = e.response?.data?.message || "登录失败，请检查账号或网络";
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="login">
    <div class="login-story">
      <div class="brand light">
        <div class="brand-mark">ZH</div>
        <div>
          <b>知华 SRM</b
          ><small>ZHUATECH SUPPLIER RELATIONSHIP MANAGEMENT</small>
        </div>
      </div>
      <div class="story-copy">
        <span>采购与供应商协同</span>
        <h1>让供应商准入、寻源、订单、配额与交付在同一套流程中闭环。</h1>
        <p>采购管理端与供应商门户统一入口，关键操作全程留痕。</p>
      </div>
      <div class="story-capabilities">
        <span>供应商全生命周期</span><span>询报价与定标</span
        ><span>订单与配额协同</span><span>质量与结算闭环</span>
      </div>
      <small class="copyright">© 2026 上海如静知华信息科技有限公司</small>
    </div>
    <div class="login-panel">
      <form class="login-box" @submit.prevent="login">
        <span class="eyebrow">SECURE SIGN IN</span>
        <h2>登录 SRM 工作台</h2>
        <p>请使用管理员分配的企业账号登录。</p>
        <label for="username">账号</label
        ><input
          id="username"
          v-model="username"
          autocomplete="username"
          autofocus
        /><label for="password">密码</label
        ><input
          id="password"
          v-model="password"
          type="password"
          autocomplete="current-password"
        />
        <div class="login-options">
          <span>连续失败将触发账号临时锁定</span
          ><a href="https://www.zhuatech.cn/" target="_blank">技术支持</a>
        </div>
        <button class="primary full" type="submit" :disabled="busy">
          {{ busy ? "正在验证…" : "登录" }}</button
        ><span class="login-error" role="alert">{{ error }}</span
        ><small class="security-note"
          >系统不提供默认账号。首次部署请通过环境变量一次性创建管理员，并在登录后立即修改密码。</small
        >
      </form>
    </div>
  </div>
</template>
