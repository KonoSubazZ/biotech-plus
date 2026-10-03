<script setup lang="ts">
import { NAlert, NDescriptions, NDescriptionsItem, NEmpty } from 'naive-ui';

defineOptions({
  name: 'InterpretationTabLims'
});

interface Props {
  /** 后端 InterpretationLimsVo：字段已按设计书 5.2 从 sample_file 映射好 */
  lims: Api.Report.InterpretationLims;
}

const props = defineProps<Props>();

/** 空值统一显示为 /（只在展示层替换，后端保持 null） */
function show(value: string | null | undefined) {
  return value === null || value === undefined || value === '' ? '/' : value;
}
</script>

<template>
  <div class="flex-col-stretch gap-16px">
    <NAlert v-if="props.lims.found === false" type="error" :bordered="false" title="未找到样本信息">
      按样本编号
      <b>{{ props.lims.barcode }}</b>
      在样本信息（sample_file.barcode）里没有查到记录 —— LIMS 信息缺失，无法继续解读。
    </NAlert>

    <template v-else>
      <NCard title="患者与疾病" :bordered="false" size="small">
        <NDescriptions bordered :column="3" label-placement="left" size="small">
          <NDescriptionsItem label="患者编号">{{ show(props.lims.patientId) }}</NDescriptionsItem>
          <NDescriptionsItem label="患者姓名">{{ show(props.lims.patientName) }}</NDescriptionsItem>
          <NDescriptionsItem label="性别">{{ show(props.lims.gender) }}</NDescriptionsItem>
          <NDescriptionsItem label="出生日期">{{ show(props.lims.birthday) }}</NDescriptionsItem>
          <NDescriptionsItem label="年龄">{{ show(props.lims.age) }}</NDescriptionsItem>
          <NDescriptionsItem label="送检医生">{{ show(props.lims.doctorName) }}</NDescriptionsItem>
          <NDescriptionsItem label="录单癌种">{{ show(props.lims.cancerType) }}</NDescriptionsItem>
          <NDescriptionsItem label="病理类型">{{ show(props.lims.pathologicalType) }}</NDescriptionsItem>
          <NDescriptionsItem label="临床分期">{{ show(props.lims.clinicalStage) }}</NDescriptionsItem>
          <NDescriptionsItem label="临床备注" :span="3">{{ show(props.lims.clinicalRemark) }}</NDescriptionsItem>
        </NDescriptions>
      </NCard>

      <NCard title="样本与送检" :bordered="false" size="small">
        <NDescriptions bordered :column="3" label-placement="left" size="small">
          <NDescriptionsItem label="医院/送检单位" :span="2">{{ show(props.lims.hospitalName) }}</NDescriptionsItem>
          <NDescriptionsItem label="实验室">{{ show(props.lims.laboratoryName) }}</NDescriptionsItem>
          <NDescriptionsItem label="样本编号">{{ show(props.lims.barcode) }}</NDescriptionsItem>
          <NDescriptionsItem label="样本类型">{{ show(props.lims.specimenType) }}</NDescriptionsItem>
          <NDescriptionsItem label="样本量">{{ show(props.lims.specimenQuantity) }}</NDescriptionsItem>
          <NDescriptionsItem label="样本来源">{{ show(props.lims.sampleSource) }}</NDescriptionsItem>
          <NDescriptionsItem label="取材部位">{{ show(props.lims.fromOrgan) }}</NDescriptionsItem>
          <NDescriptionsItem label="采样日期">{{ show(props.lims.sampleCollectedAt) }}</NDescriptionsItem>
          <NDescriptionsItem label="收样日期">{{ show(props.lims.sampleReceivedAt) }}</NDescriptionsItem>
          <NDescriptionsItem label="委托日期">{{ show(props.lims.commissionedAt) }}</NDescriptionsItem>
          <NDescriptionsItem label="样本备注" :span="3">{{ show(props.lims.sampleRemark) }}</NDescriptionsItem>
        </NDescriptions>
      </NCard>

      <NCard title="产品与联系人" :bordered="false" size="small">
        <NDescriptions bordered :column="2" label-placement="left" size="small">
          <NDescriptionsItem label="录单产品">{{ show(props.lims.testingProgram) }}</NDescriptionsItem>
          <NDescriptionsItem label="报告接收人">{{ show(props.lims.reportReceiver) }}</NDescriptionsItem>
          <NDescriptionsItem label="送检邮箱">{{ show(props.lims.emailAddress) }}</NDescriptionsItem>
          <NDescriptionsItem label="患者信息邮箱">{{ show(props.lims.patientInfoEmail) }}</NDescriptionsItem>
          <NDescriptionsItem label="接诊医生邮箱" :span="2">{{ show(props.lims.doctorEmail) }}</NDescriptionsItem>
        </NDescriptions>
      </NCard>

      <NEmpty v-if="!props.lims.patientName" description="LIMS 关键字段为空" />
    </template>
  </div>
</template>

<style scoped></style>
