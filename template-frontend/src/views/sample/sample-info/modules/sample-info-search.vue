<script setup lang="ts">
import { ref, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'SampleInfoSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Report.CmsSampleSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 日期范围 = 送检日期 send_date */
const dateRangeSend = ref<[string, string] | null>(null);

function onDateRangeSendUpdate(value: [string, string] | null) {
  model.value.params = {
    ...model.value.params,
    beginTime: value?.[0],
    endTime: value?.[1]
  };
}

function resetModel() {
  dateRangeSend.value = null;
  Object.assign(model.value, defaultModel);
}

async function reset() {
  await restoreValidation();
  resetModel();
  emit('search');
}

async function search() {
  await validate();
  emit('search');
}
</script>

<template>
  <SearchCollapse name="sample-info-search">
    <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本编号" path="subbarcode">
          <NInput v-model:value="model.subbarcode" clearable placeholder="请输入诺禾编号" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="订单编号" path="orderNo">
          <NInput v-model:value="model.orderNo" clearable placeholder="请输入正式订单编号" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="客户名称" path="customerName">
          <NInput v-model:value="model.customerName" clearable placeholder="请输入正式订单客户名称" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="产品名称" path="productName">
          <NInput v-model:value="model.productName" clearable placeholder="请输入产品名称" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本类型" path="sampleType">
          <NInput v-model:value="model.sampleType" clearable placeholder="请输入样本类型" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="送检日期" path="params">
          <NDatePicker
            v-model:formatted-value="dateRangeSend"
            type="daterange"
            value-format="yyyy-MM-dd"
            clearable
            class="w-full"
            @update:formatted-value="onDateRangeSendUpdate"
          />
        </NFormItem>
        <NFormItem class="ml-auto" :show-feedback="false">
          <NSpace :size="16">
            <NButton @click="reset">{{ $t('common.reset') }}</NButton>
            <NButton type="primary" @click="search">{{ $t('common.search') }}</NButton>
          </NSpace>
        </NFormItem>
      </div>
    </NForm>
  </SearchCollapse>
</template>

<style scoped></style>
