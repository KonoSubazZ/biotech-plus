<script setup lang="ts">
import { computed, watch } from 'vue';
import { useEcharts } from '@/hooks/common/echarts';
import { useThemeStore } from '@/store/modules/theme';
import { MONTH_LABELS } from './home-static-data';

defineOptions({
  name: 'SalesBarChart'
});

interface Props {
  /** 各月数值 */
  data: number[];
  /** 系列名（报告数量 / 样本数量） */
  label: string;
}

const props = defineProps<Props>();

const themeStore = useThemeStore();
const primary = computed(() => themeStore.themeColors.primary);

const { domRef, updateOptions } = useEcharts(() => ({
  grid: { top: 24, right: 16, bottom: 28, left: 48 },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}<br />数量：{c}' },
  xAxis: {
    type: 'category',
    data: MONTH_LABELS,
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#e5e6eb' } },
    axisLabel: { color: '#00000073' }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f2f3f5' } },
    axisLabel: { color: '#00000073' }
  },
  series: [
    {
      name: props.label,
      type: 'bar',
      barWidth: 14,
      itemStyle: { color: themeStore.themeColors.primary, borderRadius: [3, 3, 0, 0] },
      data: props.data
    }
  ]
}));

/** 维度切换（数据/系列名变了）时刷新图表 */
watch(
  () => [props.data, props.label, primary.value] as const,
  () => {
    updateOptions(opts => {
      opts.series[0].name = props.label;
      opts.series[0].data = [...props.data];
      opts.series[0].itemStyle = { color: primary.value, borderRadius: [3, 3, 0, 0] };
      return opts;
    });
  }
);
</script>

<template>
  <div ref="domRef" class="sales-chart"></div>
</template>

<style scoped>
.sales-chart {
  width: 100%;
  height: 300px;
}
</style>
