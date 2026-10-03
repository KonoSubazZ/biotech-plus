<script setup lang="ts">
defineOptions({
  name: 'SampleInfoFormPatient'
});

/** 表单分组：患者与临床（33 个字段）。model 由父级抽屉传入，直接改属性即可。 */
const model = defineModel<Api.Report.SampleInfoForm>('model', { required: true });
</script>

<template>
  <div class="mb-4px w-full text-14px font-500">患者与临床</div>
  <div class="grid grid-cols-1 gap-16px md:grid-cols-2">
      <NFormItem label="年龄" path="age">
        <NInput v-model:value="model.age" placeholder="请输入年龄" />
      </NFormItem>
      <NFormItem label="床位" path="bed">
        <NInput v-model:value="model.bed" placeholder="请输入床位" />
      </NFormItem>
      <NFormItem label="出生日期" path="birthDay">
        <NInput v-model:value="model.birthDay" placeholder="请输入出生日期" />
      </NFormItem>
      <NFormItem label="BIRTHPLACE" path="birthplace">
        <NInput v-model:value="model.birthplace" placeholder="请输入BIRTHPLACE" />
      </NFormItem>
      <NFormItem label="录单癌种" path="cancerType">
        <NInput v-model:value="model.cancerType" placeholder="请输入录单癌种" />
      </NFormItem>
      <NFormItem label="临床备注" path="clinicalRemark">
        <NInput v-model:value="model.clinicalRemark" placeholder="请输入临床备注" />
      </NFormItem>
      <NFormItem label="临床分期" path="clinicalStages">
        <NInput v-model:value="model.clinicalStages" placeholder="请输入临床分期" />
      </NFormItem>
      <NFormItem label="科室编码" path="departmentCode">
        <NInput v-model:value="model.departmentCode" placeholder="请输入科室编码" />
      </NFormItem>
      <NFormItem label="医生姓名" path="doctorName">
        <NInput v-model:value="model.doctorName" placeholder="请输入医生姓名" />
      </NFormItem>
      <NFormItem label="邮箱" path="emailAddress">
        <NInput v-model:value="model.emailAddress" placeholder="请输入邮箱" />
      </NFormItem>
      <NFormItem label="病区" path="locationName">
        <NInput v-model:value="model.locationName" placeholder="请输入病区" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="邮寄地址" path="mailingAddress">
        <NInput v-model:value="model.mailingAddress" type="textarea" placeholder="请输入邮寄地址" />
      </NFormItem>
      <NFormItem label="门诊号" path="outpatient">
        <NInput v-model:value="model.outpatient" placeholder="请输入门诊号" />
      </NFormItem>
      <NFormItem label="病理类型" path="pathologicalType">
        <NInput v-model:value="model.pathologicalType" placeholder="请输入病理类型" />
      </NFormItem>
      <NFormItem label="病理号" path="pathologyNum">
        <NInput v-model:value="model.pathologyNum" placeholder="请输入病理号" />
      </NFormItem>
      <NFormItem label="姓名" path="patientName">
        <NInput v-model:value="model.patientName" placeholder="请输入姓名" />
      </NFormItem>
      <NFormItem label="患者电话" path="patientPhone">
        <NInput v-model:value="model.patientPhone" placeholder="请输入患者电话" />
      </NFormItem>
      <NFormItem label="患者编号" path="pcode">
        <NInput v-model:value="model.pcode" placeholder="请输入患者编号" />
      </NFormItem>
      <NFormItem label="报告接收人电话" path="receiverTelephone">
        <NInput v-model:value="model.receiverTelephone" placeholder="请输入报告接收人电话" />
      </NFormItem>
      <NFormItem label="房间" path="room">
        <NInput v-model:value="model.room" placeholder="请输入房间" />
      </NFormItem>
      <NFormItem label="性别" path="sex">
        <NInput v-model:value="model.sex" placeholder="请输入性别" />
      </NFormItem>
      <NFormItem label="单位" path="unit">
        <NInput v-model:value="model.unit" placeholder="请输入单位" />
      </NFormItem>
      <NFormItem label="接诊医生邮箱" path="admissionDoctorEmail">
        <NInput v-model:value="model.admissionDoctorEmail" placeholder="请输入接诊医生邮箱" />
      </NFormItem>
      <NFormItem label="接诊医生电话" path="admissionDoctorPhone">
        <NInput v-model:value="model.admissionDoctorPhone" placeholder="请输入接诊医生电话" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="就诊医院" path="admissionHospital">
        <NInput v-model:value="model.admissionHospital" type="textarea" placeholder="请输入就诊医院" />
      </NFormItem>
      <NFormItem label="癌种1" path="cancerType1">
        <NInput v-model:value="model.cancerType1" placeholder="请输入癌种1" />
      </NFormItem>
      <NFormItem label="癌种编码" path="cancerTypeCode">
        <NInput v-model:value="model.cancerTypeCode" placeholder="请输入癌种编码" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="病理报告" path="pathologyReport">
        <NInput v-model:value="model.pathologyReport" type="textarea" placeholder="请输入病理报告" />
      </NFormItem>
      <NFormItem label="患者信息邮箱" path="patientInfoEmail">
        <NInput v-model:value="model.patientInfoEmail" placeholder="请输入患者信息邮箱" />
      </NFormItem>
      <NFormItem label="患者是否肿瘤" path="patientInfoIsCancer">
        <NInput v-model:value="model.patientInfoIsCancer" placeholder="请输入患者是否肿瘤" />
      </NFormItem>
      <NFormItem label="具体癌种" path="specificCancer">
        <NInput v-model:value="model.specificCancer" placeholder="请输入具体癌种" />
      </NFormItem>
      <NFormItem label="科室名称" path="departmentDesc">
        <NInput v-model:value="model.departmentDesc" placeholder="请输入科室名称" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="异常备注" path="abnormalRemark">
        <NInput v-model:value="model.abnormalRemark" type="textarea" placeholder="请输入异常备注" />
      </NFormItem>
  </div>
</template>

<style scoped></style>
