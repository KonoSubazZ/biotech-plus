<script setup lang="ts">
defineOptions({
  name: 'SampleInfoFormOrder'
});

/** 表单分组：录单与商务（34 个字段）。model 由父级抽屉传入，直接改属性即可。 */
const model = defineModel<Api.Report.SampleInfoForm>('model', { required: true });
</script>

<template>
  <div class="mb-4px w-full text-14px font-500">录单与商务</div>
  <div class="grid grid-cols-1 gap-16px md:grid-cols-2">
      <NFormItem label="客户名称" path="customDesc">
        <NInput v-model:value="model.customDesc" placeholder="请输入客户名称" />
      </NFormItem>
      <NFormItem label="客户" path="customerName">
        <NInput v-model:value="model.customerName" placeholder="请输入客户" />
      </NFormItem>
      <NFormItem label="委托日期" path="enterDate">
        <NInput v-model:value="model.enterDate" placeholder="请输入委托日期" />
      </NFormItem>
      <NFormItem label="ERP销售" path="erpSalerName">
        <NInput v-model:value="model.erpSalerName" placeholder="请输入ERP销售" />
      </NFormItem>
      <NFormItem label="录单产品" path="erpTestName">
        <NInput v-model:value="model.erpTestName" placeholder="请输入录单产品" />
      </NFormItem>
      <NFormItem label="加急编号" path="fastCode">
        <NInput v-model:value="model.fastCode" placeholder="请输入加急编号" />
      </NFormItem>
      <NFormItem label="经理邮箱" path="managerEmail">
        <NInput v-model:value="model.managerEmail" placeholder="请输入经理邮箱" />
      </NFormItem>
      <NFormItem label="订单编号" path="orderCode">
        <NInput v-model:value="model.orderCode" placeholder="请输入订单编号" />
      </NFormItem>
      <NFormItem label="收款金额" path="payAmount">
        <NInput v-model:value="model.payAmount" placeholder="请输入收款金额" />
      </NFormItem>
      <NFormItem label="项目经理邮箱" path="pmEmail">
        <NInput v-model:value="model.pmEmail" placeholder="请输入项目经理邮箱" />
      </NFormItem>
      <NFormItem label="录单人" path="recorder">
        <NInput v-model:value="model.recorder" placeholder="请输入录单人" />
      </NFormItem>
      <NFormItem label="录单人编码" path="recorderCode">
        <NInput v-model:value="model.recorderCode" placeholder="请输入录单人编码" />
      </NFormItem>
      <NFormItem label="报告接收人" path="reportReceiver">
        <NInput v-model:value="model.reportReceiver" placeholder="请输入报告接收人" />
      </NFormItem>
      <NFormItem label="销售邮箱(ERP)" path="salerEmail">
        <NInput v-model:value="model.salerEmail" placeholder="请输入销售邮箱(ERP)" />
      </NFormItem>
      <NFormItem label="支持邮箱" path="supportEmail">
        <NInput v-model:value="model.supportEmail" placeholder="请输入支持邮箱" />
      </NFormItem>
      <NFormItem label="合同名称" path="contractName">
        <NInput v-model:value="model.contractName" placeholder="请输入合同名称" />
      </NFormItem>
      <NFormItem label="合同编号" path="contractsNo">
        <NInput v-model:value="model.contractsNo" placeholder="请输入合同编号" />
      </NFormItem>
      <NFormItem label="单位名称" path="corpDesc">
        <NInput v-model:value="model.corpDesc" placeholder="请输入单位名称" />
      </NFormItem>
      <NFormItem label="单位编号" path="corpNo">
        <NInput v-model:value="model.corpNo" placeholder="请输入单位编号" />
      </NFormItem>
      <NFormItem label="客户类型" path="customerType">
        <NInput v-model:value="model.customerType" placeholder="请输入客户类型" />
      </NFormItem>
      <NFormItem label="运营负责人编码" path="operateManagerCode">
        <NInput v-model:value="model.operateManagerCode" placeholder="请输入运营负责人编码" />
      </NFormItem>
      <NFormItem label="运营负责人" path="operateManagerDesc">
        <NInput v-model:value="model.operateManagerDesc" placeholder="请输入运营负责人" />
      </NFormItem>
      <NFormItem label="运营负责人邮箱" path="operateManagerEmail">
        <NInput v-model:value="model.operateManagerEmail" placeholder="请输入运营负责人邮箱" />
      </NFormItem>
      <NFormItem label="订单金额" path="orderMoney">
        <NInput v-model:value="model.orderMoney" placeholder="请输入订单金额" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="其它附件" path="otherAttachments">
        <NInput v-model:value="model.otherAttachments" type="textarea" placeholder="请输入其它附件" />
      </NFormItem>
      <NFormItem label="收款完成日期" path="payFinishDate">
        <NInput v-model:value="model.payFinishDate" placeholder="请输入收款完成日期" />
      </NFormItem>
      <NFormItem label="录单单位" path="recorderDesc">
        <NInput v-model:value="model.recorderDesc" placeholder="请输入录单单位" />
      </NFormItem>
      <NFormItem label="销售" path="salesMan">
        <NInput v-model:value="model.salesMan" placeholder="请输入销售" />
      </NFormItem>
      <NFormItem label="销售编码" path="salesManCode">
        <NInput v-model:value="model.salesManCode" placeholder="请输入销售编码" />
      </NFormItem>
      <NFormItem label="销售邮箱" path="salesManEmail">
        <NInput v-model:value="model.salesManEmail" placeholder="请输入销售邮箱" />
      </NFormItem>
      <NFormItem label="流水号" path="serialNumber">
        <NInput v-model:value="model.serialNumber" placeholder="请输入流水号" />
      </NFormItem>
      <NFormItem label="结算渠道" path="checkoutLogic">
        <NInput v-model:value="model.checkoutLogic" placeholder="请输入结算渠道" />
      </NFormItem>
      <NFormItem label="账期(天)" path="paymentDate">
        <NInput v-model:value="model.paymentDate" placeholder="请输入账期(天)" />
      </NFormItem>
      <NFormItem label="签约时间" path="signTime">
        <NInput v-model:value="model.signTime" placeholder="请输入签约时间" />
      </NFormItem>
  </div>
</template>

<style scoped></style>
