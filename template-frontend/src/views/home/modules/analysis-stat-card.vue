<script setup lang="ts">
import { computed } from 'vue';
import { useThemeStore } from '@/store/modules/theme';
import MiniChart from './mini-chart.vue';

defineOptions({
  name: 'AnalysisStatCard'
});

interface Props {
  /** 卡片标题 */
  title: string;
  /** 主数字 */
  total: string;
  /** 底部左侧文案 */
  footerLabel?: string;
  /** 底部右侧文案 */
  footerValue?: string;
  /** 卡内容形态：折线 / 面积 / 柱状 / 进度条 */
  variant?: 'trend' | 'area' | 'bar' | 'progress';
  /** 迷你图数据 */
  data?: number[];
  /** 进度条百分比（variant=progress 时用） */
  percentage?: number;
}

const themeStore = useThemeStore();

const props = withDefaults(defineProps<Props>(), {
  footerLabel: '',
  footerValue: '',
  variant: 'trend',
  data: () => [],
  percentage: 78
});

/** trend 形态用折线画；progress 形态走 NProgress，这里给个不会用到的值 */
const chartType = computed<'area' | 'bar' | 'line'>(() => {
  if (props.variant === 'trend') {
    return 'line';
  }
  if (props.variant === 'progress') {
    return 'area';
  }
  return props.variant;
});
</script>

<template>
  <NCard :bordered="false" class="analysis-stat-card card-wrapper">
    <div class="stat-title">
      <span>{{ title }}</span>
      <NPopover trigger="hover">
        <template #trigger>
          <icon-ant-design-info-circle-outlined class="cursor-pointer text-14px op-45" />
        </template>
        指标说明
      </NPopover>
    </div>
    <div class="stat-total">{{ total }}</div>
    <div class="stat-content">
      <NProgress
        v-if="variant === 'progress'"
        type="line"
        :percentage="percentage"
        :show-indicator="false"
        :height="8"
        :color="themeStore.themeColors.primary"
      />
      <MiniChart v-else :data="data" :type="chartType" :color="themeStore.themeColors.primary" />
    </div>
    <div class="stat-footer">
      <span>{{ footerLabel }}</span>
      <span>{{ footerValue }}</span>
    </div>
  </NCard>
</template>

<style scoped>
/* 样式对齐 biotech 首页的统计卡（同一套 antd token 色） */
.analysis-stat-card {
  min-height: 182px;
}

.analysis-stat-card :deep(.n-card__content) {
  padding: 20px 24px 8px;
}

.stat-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #00000073;
  font-size: 14px;
}

.stat-total {
  height: 40px;
  margin-top: 4px;
  color: #000000e0;
  font-size: 30px;
  line-height: 40px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 46px;
  overflow: hidden;
  color: #000000a6;
  font-size: 14px;
}

.stat-content :deep(.n-progress) {
  width: 100%;
}

.stat-footer {
  display: flex;
  gap: 8px;
  height: 38px;
  padding-top: 9px;
  border-top: 1px solid #0505050f;
  color: #000000a6;
  font-size: 14px;
}
</style>
