<script setup lang="ts">
import { ref, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'VariantHistorySearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Report.VariantHistorySearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 报告时间 = 报告生成时间（report_generated_at），范围查询走 params.beginTime / params.endTime */
const dateRangeReport = ref<[string, string] | null>(null);

function onDateRangeReportUpdate(value: [string, string] | null) {
  model.value.params = {
    ...model.value.params,
    beginTime: value?.[0],
    endTime: value?.[1]
  };
}

function resetModel() {
  dateRangeReport.value = null;
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
  <SearchCollapse name="variant-history-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <!--
            搜索项排布用 flex-wrap（不用 NGrid）：按钮组 ml-auto 自动贴在本行最右边；
            一行放不下时整组换到下一行，ml-auto 仍让它贴最右。
          -->
          <div class="flex flex-wrap items-start">
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="报告时间" path="reportGeneratedAt">
              <NDatePicker
                v-model:formatted-value="dateRangeReport"
                class="w-full"
                type="daterange"
                value-format="yyyy-MM-dd"
                clearable
                @update:formatted-value="onDateRangeReportUpdate"
              />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本编号" path="subbarcode">
              <NInput v-model:value="model.subbarcode" placeholder="请输入样本编号" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="解读癌种" path="disease">
              <NInput v-model:value="model.disease" placeholder="请输入解读癌种" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="产品" path="product">
              <NInput v-model:value="model.product" placeholder="请输入产品名称" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="项目编号" path="projectNo">
              <NInput v-model:value="model.projectNo" placeholder="请输入项目编号（如 NG0101）" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="客户" path="client">
              <NInput v-model:value="model.client" placeholder="请输入客户名称" clearable />
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
  </SearchCollapse>
</template>

<style scoped></style>
