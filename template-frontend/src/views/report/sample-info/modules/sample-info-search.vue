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

/** 日期范围（= 委托日期 commission_date；列在上游是 varchar(20) 的 yyyy-MM-dd，按字符串比较与日期先后一致） */
const dateRangeCommission = ref<[string, string] | null>(null);

function onDateRangeCommissionUpdate(value: [string, string] | null) {
  model.value.params = {
    ...model.value.params,
    beginTime: value?.[0],
    endTime: value?.[1]
  };
}

function resetModel() {
  dateRangeCommission.value = null;
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
            <NFormItemGi span="24 s:12 m:8" label="委托日期" path="commissionDate" class="pr-24px">
              <NDatePicker
                v-model:formatted-value="dateRangeCommission"
                type="daterange"
                value-format="yyyy-MM-dd"
                clearable
                @update:formatted-value="onDateRangeCommissionUpdate"
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="样本编号" path="subbarcode" class="pr-24px">
              <NInput v-model:value="model.subbarcode" placeholder="请输入样本编号" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="姓名" path="personName" class="pr-24px">
              <NInput v-model:value="model.personName" placeholder="请输入患者姓名" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="客户" path="client" class="pr-24px">
              <NInput v-model:value="model.client" placeholder="请输入客户" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="录单癌种" path="diseaseType" class="pr-24px">
              <NInput v-model:value="model.diseaseType" placeholder="请输入录单癌种" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="录单产品" path="productName" class="pr-24px">
              <NInput v-model:value="model.productName" placeholder="请输入录单产品" clearable />
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
