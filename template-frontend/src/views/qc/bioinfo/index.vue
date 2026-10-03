<script setup lang="ts">
import QcRecordPage from '../modules/qc-record-page.vue';

defineOptions({
  name: 'QcBioinfo'
});
</script>

<template>
  <!-- 生信质控：与湿实验质控共用同一页面组件，仅 qcCategory 不同 -->
  <QcRecordPage qc-category="bioinfo" title="生信质控记录" />
</template>
