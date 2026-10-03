<script setup lang="ts">
defineOptions({
  name: 'SampleInfoFormTreatment'
});

/** 表单分组：治疗史与家族史（27 个字段）。model 由父级抽屉传入，直接改属性即可。 */
const model = defineModel<Api.Report.SampleInfoForm>('model', { required: true });
</script>

<template>
  <div class="mb-4px w-full text-14px font-500">治疗史与家族史</div>
  <div class="grid grid-cols-1 gap-16px md:grid-cols-2">
      <NFormItem label="一级亲属患癌情况" path="familyFirst">
        <NInput v-model:value="model.familyFirst" placeholder="请输入一级亲属患癌情况" />
      </NFormItem>
      <NFormItem label="一级亲属年龄" path="familyFirstAge">
        <NInput v-model:value="model.familyFirstAge" placeholder="请输入一级亲属年龄" />
      </NFormItem>
      <NFormItem label="一级亲属癌种" path="familyFirstCancerType">
        <NInput v-model:value="model.familyFirstCancerType" placeholder="请输入一级亲属癌种" />
      </NFormItem>
      <NFormItem label="一级亲属确诊时间" path="familyFirstConfirmTime">
        <NInput v-model:value="model.familyFirstConfirmTime" placeholder="请输入一级亲属确诊时间" />
      </NFormItem>
      <NFormItem label="二级亲属患癌情况" path="familySecond">
        <NInput v-model:value="model.familySecond" placeholder="请输入二级亲属患癌情况" />
      </NFormItem>
      <NFormItem label="二级亲属年龄" path="familySecondAge">
        <NInput v-model:value="model.familySecondAge" placeholder="请输入二级亲属年龄" />
      </NFormItem>
      <NFormItem label="二级亲属癌种" path="familySecondCancerType">
        <NInput v-model:value="model.familySecondCancerType" placeholder="请输入二级亲属癌种" />
      </NFormItem>
      <NFormItem label="二级亲属确诊时间" path="familySecondConfirmTime">
        <NInput v-model:value="model.familySecondConfirmTime" placeholder="请输入二级亲属确诊时间" />
      </NFormItem>
      <NFormItem label="第一次治疗史" path="firstTreatment">
        <NInput v-model:value="model.firstTreatment" placeholder="请输入第一次治疗史" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第二次治疗史" path="secondTreatment">
        <NInput v-model:value="model.secondTreatment" type="textarea" placeholder="请输入第二次治疗史" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第三次治疗史" path="thirdTreatment">
        <NInput v-model:value="model.thirdTreatment" type="textarea" placeholder="请输入第三次治疗史" />
      </NFormItem>
      <NFormItem label="二级亲属患癌情况" path="familyKinshipTwoCancer">
        <NInput v-model:value="model.familyKinshipTwoCancer" placeholder="请输入二级亲属患癌情况" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第一次治疗用药方案" path="firstTreatmentDrugRegimen">
        <NInput v-model:value="model.firstTreatmentDrugRegimen" type="textarea" placeholder="请输入第一次治疗用药方案" />
      </NFormItem>
      <NFormItem label="第一次治疗时长" path="firstTreatmentDuration">
        <NInput v-model:value="model.firstTreatmentDuration" placeholder="请输入第一次治疗时长" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第一次治疗疗效" path="firstTreatmentEffect">
        <NInput v-model:value="model.firstTreatmentEffect" type="textarea" placeholder="请输入第一次治疗疗效" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第一次治疗方式" path="firstTreatmentMethod">
        <NInput v-model:value="model.firstTreatmentMethod" type="textarea" placeholder="请输入第一次治疗方式" />
      </NFormItem>
      <NFormItem label="第一次治疗时间" path="firstTreatmentTime">
        <NInput v-model:value="model.firstTreatmentTime" placeholder="请输入第一次治疗时间" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第二次治疗用药方案" path="secondTreatmentDrugRegimen">
        <NInput v-model:value="model.secondTreatmentDrugRegimen" type="textarea" placeholder="请输入第二次治疗用药方案" />
      </NFormItem>
      <NFormItem label="第二次治疗时长" path="secondTreatmentDuration">
        <NInput v-model:value="model.secondTreatmentDuration" placeholder="请输入第二次治疗时长" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第二次治疗疗效" path="secondTreatmentEffect">
        <NInput v-model:value="model.secondTreatmentEffect" type="textarea" placeholder="请输入第二次治疗疗效" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第二次治疗方式" path="secondTreatmentMethod">
        <NInput v-model:value="model.secondTreatmentMethod" type="textarea" placeholder="请输入第二次治疗方式" />
      </NFormItem>
      <NFormItem label="第二次治疗时间" path="secondTreatmentTime">
        <NInput v-model:value="model.secondTreatmentTime" placeholder="请输入第二次治疗时间" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第三次治疗用药方案" path="thirdTreatmentDrugRegimen">
        <NInput v-model:value="model.thirdTreatmentDrugRegimen" type="textarea" placeholder="请输入第三次治疗用药方案" />
      </NFormItem>
      <NFormItem label="第三次治疗时长" path="thirdTreatmentDuration">
        <NInput v-model:value="model.thirdTreatmentDuration" placeholder="请输入第三次治疗时长" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第三次治疗疗效" path="thirdTreatmentEffect">
        <NInput v-model:value="model.thirdTreatmentEffect" type="textarea" placeholder="请输入第三次治疗疗效" />
      </NFormItem>
      <NFormItem class="md:col-span-2" label="第三次治疗方式" path="thirdTreatmentMethod">
        <NInput v-model:value="model.thirdTreatmentMethod" type="textarea" placeholder="请输入第三次治疗方式" />
      </NFormItem>
      <NFormItem label="第三次治疗时间" path="thirdTreatmentTime">
        <NInput v-model:value="model.thirdTreatmentTime" placeholder="请输入第三次治疗时间" />
      </NFormItem>
  </div>
</template>

<style scoped></style>
