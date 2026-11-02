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
  <SearchCollapse name="sample-info-search" class="table-search">
    <NForm ref="formRef" :model="model" label-placement="left" :label-width="80">
      <NGrid responsive="screen" item-responsive>
        <NFormItemGi span="24 s:12 m:6" label="诺禾编号" path="subbarcode" class="pr-24px">
          <NInput v-model:value="model.subbarcode" clearable placeholder="请输入诺禾编号" />
        </NFormItemGi>
        <NFormItemGi span="24 s:12 m:6" label="订单编号" path="orderNo" class="pr-24px">
          <NInput v-model:value="model.orderNo" clearable placeholder="请输入正式订单编号" />
        </NFormItemGi>
        <NFormItemGi span="24 s:12 m:6" label="客户名称" path="customerName" class="pr-24px">
          <NInput v-model:value="model.customerName" clearable placeholder="请输入正式订单客户名称" />
        </NFormItemGi>
        <NFormItemGi span="24 s:12 m:6" label="产品名称" path="productName" class="pr-24px">
          <NInput v-model:value="model.productName" clearable placeholder="请输入产品名称" />
        </NFormItemGi>
        <NFormItemGi span="24 s:12 m:6" label="样本类型" path="sampleType" class="pr-24px">
          <NInput v-model:value="model.sampleType" clearable placeholder="请输入样本类型" />
        </NFormItemGi>
        <NFormItemGi span="24 s:12 m:6" label="送检日期" path="params" class="pr-24px">
          <NDatePicker
            v-model:formatted-value="dateRangeSend"
            type="daterange"
            value-format="yyyy-MM-dd"
            clearable
            class="w-full"
            @update:formatted-value="onDateRangeSendUpdate"
          />
        </NFormItemGi>
        <NFormItemGi span="24 s:12 m:12" class="pr-24px">
          <NSpace class="w-full" justify="end">
            <NButton @click="reset">
              <template #icon>
                <icon-ic-round-refresh class="text-icon" />
              </template>
              {{ $t('common.reset') }}
            </NButton>
            <NButton type="primary" ghost @click="search">
              <template #icon>
                <icon-ic-round-search class="text-icon" />
              </template>
              {{ $t('common.search') }}
            </NButton>
          </NSpace>
        </NFormItemGi>
      </NGrid>
    </NForm>
  </SearchCollapse>
</template>

<style scoped></style>
