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

const model = defineModel<Api.Report.SampleInfoSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 日期范围 = 委托日期 enter_date（源表列 ENTERDATE；库里既有 yyyy-MM-dd 也有带时分秒的写法） */
const dateRangeEnter = ref<[string, string] | null>(null);

function onDateRangeEnterUpdate(value: [string, string] | null) {
  model.value.params = {
    ...model.value.params,
    beginTime: value?.[0],
    endTime: value?.[1]
  };
}

function resetModel() {
  dateRangeEnter.value = null;
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
  <NCard :bordered="false" size="small" class="card-wrapper">
    <NCollapse>
      <NCollapseItem :title="$t('common.search')" name="sample-info-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <NGrid responsive="screen" item-responsive>
            <NFormItemGi span="24 s:12 m:8" label="委托日期" path="enterDate" class="pr-24px">
              <NDatePicker
                v-model:formatted-value="dateRangeEnter"
                type="daterange"
                value-format="yyyy-MM-dd"
                clearable
                @update:formatted-value="onDateRangeEnterUpdate"
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="样本编号" path="barcode" class="pr-24px">
              <NInput v-model:value="model.barcode" placeholder="请输入样本编号（BARCODE）" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="姓名" path="patientName" class="pr-24px">
              <NInput v-model:value="model.patientName" placeholder="请输入患者姓名（PATIENTNAME）" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="客户" path="customerName" class="pr-24px">
              <NInput v-model:value="model.customerName" placeholder="客户名称 / 客户（两个字段一起搜）" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="录单癌种" path="cancerType" class="pr-24px">
              <NInput v-model:value="model.cancerType" placeholder="请输入录单癌种（CANCERTYPE）" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="录单产品" path="erpTestName" class="pr-24px">
              <NInput v-model:value="model.erpTestName" placeholder="请输入录单产品（ERPTESTNAME）" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" class="pr-24px">
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
      </NCollapseItem>
    </NCollapse>
  </NCard>
</template>

<style scoped></style>
