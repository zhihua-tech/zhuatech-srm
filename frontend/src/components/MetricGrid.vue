<script setup>
defineProps({ items: { type: Array, default: () => [] } });
</script>
<template>
  <div class="metric-grid">
    <div v-for="(m, index) in items" :key="m.label || m[0]" class="metric">
      <div class="metric-top">
        <span>{{ m.label || m[0] }}</span
        ><i :class="['blue', 'green', 'warn', 'red'][index % 4]"></i>
      </div>
      <strong>{{ m.value ?? m[1] }}{{ m.unit || "" }}</strong
      ><small>{{ m.note || m[2] || "实时业务数据" }}</small>
    </div>
  </div>
</template>
