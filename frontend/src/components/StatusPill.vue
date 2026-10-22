<script setup>
defineProps({ text: [String, Number, Boolean] });
const red = [
    "HIGH",
    "BLACKLISTED",
    "SUSPENDED",
    "ELIMINATED",
    "OVERDUE",
    "RETURNED",
    "REJECTED",
    "ABNORMAL",
    "待响应",
    "整改中",
    "严重",
    "高",
  ],
  green = [
    "ACTIVE",
    "APPROVED",
    "AWARDED",
    "COMPLETED",
    "CLOSED",
    "SUPPLIER_CONFIRMED",
    "RECEIVED",
    "合作中",
    "已完成",
    "已定标",
  ],
  blue = [
    "OPEN",
    "PUBLISHED",
    "EVALUATING",
    "PENDING_APPROVAL",
    "PENDING_CONFIRMATION",
    "SHIPPING",
    "IN_PROGRESS",
    "生产中",
    "报价中",
    "澄清中",
  ];
</script>
<template>
  <span
    class="status"
    :class="{
      'is-red': red.includes(String(text)),
      'is-green': green.includes(String(text)),
      'is-blue': blue.includes(String(text)),
    }"
    ><i></i>{{ text ?? "—" }}</span
  >
</template>
