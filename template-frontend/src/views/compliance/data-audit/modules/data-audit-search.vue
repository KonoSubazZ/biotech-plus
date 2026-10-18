<script setup lang="ts">
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({ name: 'DataAuditSearch' });
defineProps<{ pending: boolean }>();
const emit = defineEmits<{ search: []; reset: [] }>();
const model = defineModel<Api.Audit.SearchParams>('model', { required: true });
const { formRef, validate, restoreValidation } = useNaiveForm();
const scopeOptions = [
  { label: '样本相关', value: 'SAMPLE' },
  { label: '非样本对象', value: 'NON_SAMPLE' }
];

async function search() {
  await validate();
  emit('search');
}

async function reset() {
  await restoreValidation();
  Object.assign(model.value, { subbarcode: null, analysisId: null, auditScope: null, action: null, actorName: null });
  emit('reset');
}
</script>

<template>
  <NCard :bordered="false" size="small" class="card-wrapper">
    <NForm
      ref="formRef"
      :model="model"
      :disabled="pending"
      label-placement="left"
      :label-width="90"
      @submit.prevent="search"
    >
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本编号" path="subbarcode">
          <NInput v-model:value="model.subbarcode" placeholder="完整样本编号" clearable @keyup.enter="search" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="分析数据ID" path="analysisId">
          <NInput v-model:value="model.analysisId" placeholder="可选，筛选数据批次" clearable @keyup.enter="search" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="对象范围" path="auditScope">
          <NSelect v-model:value="model.auditScope" :options="scopeOptions" clearable placeholder="全部对象" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="操作" path="action">
          <NInput v-model:value="model.action" placeholder="搜索业务操作" clearable @keyup.enter="search" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="操作人" path="actorName">
          <NInput v-model:value="model.actorName" placeholder="搜索操作人" clearable @keyup.enter="search" />
        </NFormItem>
        <NFormItem class="ml-auto" :show-feedback="false">
          <NSpace :size="16">
            <NButton @click="reset">{{ $t('common.reset') }}</NButton>
            <NButton type="primary" ghost @click="search">{{ $t('common.search') }}</NButton>
          </NSpace>
        </NFormItem>
      </div>
    </NForm>
  </NCard>
</template>
