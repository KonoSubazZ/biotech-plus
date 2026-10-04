<script setup lang="ts">
import { ref, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'InterpretationSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Report.InterpretationSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 报告状态下拉（与后端 analysis_report.status 状态机一致） */
const REPORT_STATUS_OPTIONS = [
  { label: '解读中', value: 'INTERPRETING' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已审核', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已发送', value: 'SENT' }
];

/** 分析日期范围（analysis_date 是 varchar(8) 的 YYYYMMDD，选完转成 yyyyMMdd 再传） */
const dateRangeAnalysis = ref<[string, string] | null>(null);

function onDateRangeAnalysisUpdate(value: [string, string] | null) {
  model.value.params = {
    ...model.value.params,
    beginTime: value?.[0]?.replace(/-/g, '').slice(0, 8),
    endTime: value?.[1]?.replace(/-/g, '').slice(0, 8)
  };
}

function resetModel() {
  dateRangeAnalysis.value = null;
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
      <NCollapseItem :title="$t('common.search')" name="interpretation-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <!-- 搜索项排布用 flex-wrap（不用 NGrid）：按钮组 ml-auto 贴当前行最右，换行后仍贴右 -->
          <div class="flex flex-wrap items-start">
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="分析日期" path="analysisDate">
              <NDatePicker
                v-model:formatted-value="dateRangeAnalysis"
                type="daterange"
                value-format="yyyy-MM-dd"
                clearable
                @update:formatted-value="onDateRangeAnalysisUpdate"
              />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本编号" path="subbarcode">
              <NInput v-model:value="model.subbarcode" placeholder="请输入样本编号" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="产品" path="product">
              <NInput v-model:value="model.product" placeholder="请输入产品名称" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="报告状态" path="reportStatus">
              <NSelect
                v-model:value="model.reportStatus"
                :options="REPORT_STATUS_OPTIONS"
                placeholder="请选择报告状态"
                filterable
                clearable
              />
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
