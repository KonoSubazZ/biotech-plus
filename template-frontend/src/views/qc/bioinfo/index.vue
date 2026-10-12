<script setup lang="ts">
import BioinfoQcPage from '../modules/bioinfo-qc-page.vue';

// 生信质控：宽表（一行一个样本），数据由上游接口灌入；页面提供列表/修改/删除（新增不做，由接口负责）。
// 注意：模板必须保持「单根元素」—— 多根(fragment)会让全局布局的 <Transition mode="out-in"> 卡死，
// 表现为切换页面后内容区整块空白（实测踩过），所以说明写在 script 里，不要塞进模板。
defineOptions({
  name: 'QcBioinfo'
});
</script>

<template>
  <BioinfoQcPage />
</template>
