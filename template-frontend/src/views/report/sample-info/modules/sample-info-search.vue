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
          <!--
            搜索项排布用 flex-wrap（不用 NGrid）：按钮组 ml-auto 自动贴在本行最右边；
            一行放不下时整组换到下一行，ml-auto 仍让它贴最右。
            （NGrid 的最后一格只会在自己格子内靠右，字段数不是列数整数倍时会悬在行中间 —— 实测踩到）
          -->
          <div class="flex flex-wrap items-start">
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="委托日期" path="enterDate">
              <NDatePicker
                v-model:formatted-value="dateRangeEnter"
                type="daterange"
                value-format="yyyy-MM-dd"
                clearable
                @update:formatted-value="onDateRangeEnterUpdate"
              />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本编号" path="barcode">
              <NInput v-model:value="model.barcode" placeholder="请输入样本编号（BARCODE）" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="姓名" path="patientName">
              <NInput v-model:value="model.patientName" placeholder="请输入患者姓名（PATIENTNAME）" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="客户" path="customerName">
              <NInput v-model:value="model.customerName" placeholder="客户名称 / 客户（两个字段一起搜）" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="录单癌种" path="cancerType">
              <NInput v-model:value="model.cancerType" placeholder="请输入录单癌种（CANCERTYPE）" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="录单产品" path="erpTestName">
              <NInput v-model:value="model.erpTestName" placeholder="请输入录单产品（ERPTESTNAME）" clearable />
            </NFormItem>
            <NFormItem class="ml-auto" :show-feedback="false">
              <NSpace :size="16">
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
            </NFormItem>
          </div>
        </NForm>
      </NCollapseItem>
    </NCollapse>
  </NCard>
</template>

<style scoped></style>
