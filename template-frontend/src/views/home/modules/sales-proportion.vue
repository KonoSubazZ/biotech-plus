<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useEcharts } from '@/hooks/common/echarts';
import { useThemeStore } from '@/store/modules/theme';
import { TYPE_PROPORTION, formatNumber } from './home-static-data';
import { primaryPalette } from './home-theme';

defineOptions({
  name: 'SalesProportion'
});

/** 维度：报告 / 样本 */
const dimension = ref<'report' | 'sample'>('report');

const themeStore = useThemeStore();
const primary = computed(() => themeStore.themeColors.primary);

const { domRef, updateOptions } = useEcharts(() => ({
  color: primaryPalette(themeStore.themeColors.primary, TYPE_PROPORTION.report.length),
  tooltip: { trigger: 'item', formatter: '{b}<br />数量：{c}（{d}%）' },
  legend: { show: false },
  series: [
    {
      name: '数量',
      type: 'pie',
      radius: ['46%', '68%'],
      center: ['50%', '53%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: {
        color: '#595959',
        // 与 biotech 一致：标签显示「类型: 千分位数量」（入参按回调实参收窄，不用 any）
        formatter: (params: unknown) => {
          const item = params as { name?: string; value?: number };
          return `${item.name ?? ''}: ${formatNumber(Number(item.value ?? 0))}`;
        }
      },
      labelLine: { length: 12, length2: 8 },
      data: TYPE_PROPORTION.report
    }
  ]
}));

/** 维度切换时换一份数据 */
/** 维度或主题色变化时刷新 */
watch(
  () => [dimension.value, primary.value] as const,
  ([value]) => {
    updateOptions(opts => {
      opts.series[0].data = TYPE_PROPORTION[value];
      opts.color = primaryPalette(primary.value, TYPE_PROPORTION[value].length);
      return opts;
    });
  }
);
</script>

<template>
  <NCard :bordered="false" title="报告 / 样本类型占比" class="analysis-detail-card card-wrapper">
    <template #header-extra>
      <NSpace :size="8">
        <NButton size="small" :type="dimension === 'report' ? 'primary' : 'default'" @click="dimension = 'report'">
          报告维度
        </NButton>
        <NButton size="small" :type="dimension === 'sample' ? 'primary' : 'default'" @click="dimension = 'sample'">
          样本维度
        </NButton>
      </NSpace>
    </template>

    <div class="sales-label">类型分布</div>
    <div ref="domRef" class="pie-chart"></div>
  </NCard>
</template>

<style scoped>
.analysis-detail-card {
  position: relative;
  height: 100%;
}

.sales-label {
  color: #000000a6;
  font-size: 14px;
}

.pie-chart {
  width: 100%;
  height: 300px;
}
</style>
