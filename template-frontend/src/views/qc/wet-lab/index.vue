<script setup lang="ts">
import QcRecordPage from '../modules/qc-record-page.vue';

defineOptions({
  name: 'QcWetLab'
});
</script>

<template>
  <!-- 湿实验质控：与生信质控共用同一页面组件，仅 qcCategory 不同 -->
  <QcRecordPage qc-category="wet_lab" title="湿实验质控记录" />
</template>
