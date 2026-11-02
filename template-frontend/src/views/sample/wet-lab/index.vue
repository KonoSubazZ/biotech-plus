<script setup lang="ts">
import WetLabQcPage from '../modules/wet-lab-qc-page.vue';

// 湿实验质控：一行 = 样本 × 质控项，数据由上游接口灌入（POST /qc/wetLabQc/receive），
// 质控项与阈值落库时从「质控标准」快照。页面只读 + 删除（新增/修改不做，由接口负责）。
// 注意：模板必须保持「单根元素」—— 多根(fragment)会让全局布局的 <Transition mode="out-in"> 卡死，
// 表现为切换两个页面后内容区整块空白（实测踩过），所以说明写在 script 里，不要塞进模板。
defineOptions({
  name: 'QcWetLab'
});
</script>

<template>
  <WetLabQcPage />
</template>
