<script setup>
defineProps({ name: String, size: { type: Number, default: 18 } });
const icons = {
  home: "M3 11 12 3l9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z",
  supplier:
    "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75",
  search: "M3 6h18M5 6l1 14h12l1-14M9 10v6M15 10v6M8 3h8l1 3H7z",
  order: "M6 3h12l3 4v14H3V7zM3 7h18M9 11h6",
  truck:
    "M3 6h11v11H3zM14 10h4l3 4v3h-7zM7 21a2 2 0 1 0 0-4 2 2 0 0 0 0 4M18 21a2 2 0 1 0 0-4 2 2 0 0 0 0 4",
  quality: "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10M9 12l2 2 4-5",
  finance: "M4 5h16v14H4zM8 9h8M8 13h3",
  chart: "M4 19V9M10 19V5M16 19v-7M22 19H2",
  risk: "M12 3 2 21h20zM12 9v5M12 18h.01",
  settings:
    "M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.12 2.12-.06-.06a1.7 1.7 0 0 0-1.88-.34",
  bell: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4",
};
</script>
<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path :d="icons[name] || icons.home" />
  </svg>
</template>
