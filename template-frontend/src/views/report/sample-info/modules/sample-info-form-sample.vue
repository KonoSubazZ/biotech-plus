<script setup lang="ts">
defineOptions({
  name: 'SampleInfoFormSample'
});

/** 表单分组：样本与收样（28 个字段）。model 由父级抽屉传入，直接改属性即可。 */
const model = defineModel<Api.Report.SampleInfoForm>('model', { required: true });
</script>

<template>
  <div class="mb-4px w-full text-14px font-500">样本与收样</div>
  <div class="grid grid-cols-1 gap-16px md:grid-cols-2">
      <NFormItem label="样本编号" path="barcode">
        <NInput v-model:value="model.barcode" placeholder="请输入样本编号" />
      </NFormItem>
      <NFormItem label="收样日期" path="collectDate">
        <NInput v-model:value="model.collectDate" placeholder="请输入收样日期" />
      </NFormItem>
      <NFormItem label="取材部位" path="fromOrgan">
        <NInput v-model:value="model.fromOrgan" placeholder="请输入取材部位" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="基因检测结果" path="geneResult">
        <NInput v-model:value="model.geneResult" type="textarea" placeholder="请输入基因检测结果" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="基因型" path="geneType">
        <NInput v-model:value="model.geneType" type="textarea" placeholder="请输入基因型" />
      </NFormItem>
      <NFormItem label="取材日期" path="getSpecDate">
        <NInput v-model:value="model.getSpecDate" placeholder="请输入取材日期" />
      </NFormItem>
      <NFormItem label="文库编号" path="libraryName">
        <NInput v-model:value="model.libraryName" placeholder="请输入文库编号" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="样本备注" path="sampleRemark">
        <NInput v-model:value="model.sampleRemark" type="textarea" placeholder="请输入样本备注" />
      </NFormItem>
      <NFormItem label="样本来源" path="sampleSource">
        <NInput v-model:value="model.sampleSource" placeholder="请输入样本来源" />
      </NFormItem>
      <NFormItem label="采样日期" path="sampleTime">
        <NInput v-model:value="model.sampleTime" placeholder="请输入采样日期" />
      </NFormItem>
      <NFormItem label="样本类型" path="sampleType">
        <NInput v-model:value="model.sampleType" placeholder="请输入样本类型" />
      </NFormItem>
      <NFormItem label="寄出日期" path="sendDate">
        <NInput v-model:value="model.sendDate" placeholder="请输入寄出日期" />
      </NFormItem>
      <NFormItem label="标本编号" path="specimenNo">
        <NInput v-model:value="model.specimenNo" placeholder="请输入标本编号" />
      </NFormItem>
      <NFormItem label="样本数量" path="specimenNum">
        <NInput v-model:value="model.specimenNum" placeholder="请输入样本数量" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="检测方法" path="detectionMethod">
        <NInput v-model:value="model.detectionMethod" type="textarea" placeholder="请输入检测方法" />
      </NFormItem>
      <NFormItem label="检测时间" path="detectionTime">
        <NInput v-model:value="model.detectionTime" placeholder="请输入检测时间" />
      </NFormItem>
      <NFormItem label="快递公司" path="expressName">
        <NInput v-model:value="model.expressName" placeholder="请输入快递公司" />
      </NFormItem>
      <NFormItem label="快递单号" path="expressNo">
        <NInput v-model:value="model.expressNo" placeholder="请输入快递单号" />
      </NFormItem>
      <NFormItem label="实验室" path="laboratoryName">
        <NInput v-model:value="model.laboratoryName" placeholder="请输入实验室" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="送样地址" path="sampleAddress">
        <NInput v-model:value="model.sampleAddress" type="textarea" placeholder="请输入送样地址" />
      </NFormItem>
      <NFormItem label="送样联系人" path="sampleContactDesc">
        <NInput v-model:value="model.sampleContactDesc" placeholder="请输入送样联系人" />
      </NFormItem>
      <NFormItem label="送样联系电话" path="sampleContactPhone">
        <NInput v-model:value="model.sampleContactPhone" placeholder="请输入送样联系电话" />
      </NFormItem>
      <NFormItem label="数量单位" path="sampleNumUnit">
        <NInput v-model:value="model.sampleNumUnit" placeholder="请输入数量单位" />
      </NFormItem>
      <NFormItem label="产品编码" path="sampleProductCode">
        <NInput v-model:value="model.sampleProductCode" placeholder="请输入产品编码" />
      </NFormItem>
      <NFormItem label="送检机构" path="testingInstitution">
        <NInput v-model:value="model.testingInstitution" placeholder="请输入送检机构" />
      </NFormItem>
      <NFormItem label="到样日期" path="dyDate">
        <NInput v-model:value="model.dyDate" placeholder="请输入到样日期" />
      </NFormItem>
      <NFormItem label="样本属性" path="sampleAttribute">
        <NInput v-model:value="model.sampleAttribute" placeholder="请输入样本属性" />
      </NFormItem>
      <NFormItem label="冻结状态" path="block">
        <NInput v-model:value="model.block" placeholder="请输入冻结状态" />
      </NFormItem>
  </div>
</template>

<style scoped></style>
