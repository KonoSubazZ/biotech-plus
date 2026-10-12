<script setup lang="ts">
import { computed, ref } from 'vue';
import dayjs from 'dayjs';
import SalesDimensionBody from './sales-dimension-body.vue';

defineOptions({
  name: 'SalesOverview'
});

/** 时间范围快捷项 */
const RANGE_OPTIONS = [
  { label: '今日', value: 'today', unit: 'day' },
  { label: '本周', value: 'week', unit: 'week' },
  { label: '本月', value: 'month', unit: 'month' },
  { label: '本年', value: 'year', unit: 'year' }
] as const;

const TABS = [
  { name: 'report', tab: '报告维度' },
  { name: 'sample', tab: '样本维度' }
];

/** 维度（报告 / 样本） */
const dimension = ref<'report' | 'sample'>('report');
/** 时间范围 */
const range = ref<string>('year');
const dateRange = ref<[number, number]>([dayjs().startOf('year').valueOf(), dayjs().endOf('year').valueOf()]);

const rangeButtons = computed(() =>
  RANGE_OPTIONS.map(item => ({ label: item.label, value: item.value, unit: item.unit }))
);

/** 切换时间范围：日期选择器跟着跳到对应区间（静态数据，只做联动） */
function changeRange(value: string) {
  range.value = value;
  const hit = RANGE_OPTIONS.find(item => item.value === value);
  const unit = hit?.unit ?? 'year';
  dateRange.value = [dayjs().startOf(unit).valueOf(), dayjs().endOf(unit).valueOf()];
}
</script>

<template>
  <NCard :bordered="false" class="sales-overview-card card-wrapper">
    <NTabs v-model:value="dimension" type="line" size="large" animated>
      <template #suffix>
        <div class="sales-filter">
          <NButton
            v-for="item in rangeButtons"
            :key="item.value"
            text
            :type="range === item.value ? 'primary' : 'default'"
            @click="changeRange(item.value)"
          >
            {{ item.label }}
          </NButton>
          <NDatePicker v-model:value="dateRange" type="daterange" clearable class="date-range" />
        </div>
      </template>

      <NTabPane v-for="item in TABS" :key="item.name" :name="item.name" :tab="item.tab">
        <SalesDimensionBody :dimension="item.name as 'report' | 'sample'" />
      </NTabPane>
    </NTabs>
  </NCard>
</template>

<style scoped>
/* 与 biotech 首页一致：卡片内容区不留白，tab 头 24px 缩进 */
.sales-overview-card :deep(.n-card__content) {
  padding: 0;
}

.sales-overview-card :deep(.n-tabs-nav) {
  padding: 0 24px;
}

.sales-overview-card :deep(.n-tabs-tab) {
  padding-top: 18px;
  padding-bottom: 16px;
}

.sales-overview-card :deep(.n-tab-pane) {
  padding: 0;
}

.sales-filter {
  display: flex;
  align-items: center;
  gap: 22px;
}

@media (max-width: 640px) {
  .sales-filter {
    display: none;
  }
}
</style>
