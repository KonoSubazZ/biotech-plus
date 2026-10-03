<script setup lang="ts">
import QcRecordPage from '../modules/qc-record-page.vue';

// 湿实验质控：与生信质控共用同一个页面组件，仅 qcCategory 不同（取 wet_lab / bioinfo）。
// 注意：模板必须保持「单根元素」—— 多根(fragment)会让全局布局的 <Transition mode="out-in"> 卡死，
// 表现为切换两个页面后内容区整块空白（实测踩过），所以说明写在 script 里，不要塞进模板。
defineOptions({
  name: 'QcWetLab'
});
</script>

<template>
  <QcRecordPage qc-category="wet_lab" title="湿实验质控记录" />
</template>
