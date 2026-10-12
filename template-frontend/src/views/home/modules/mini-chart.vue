<script setup lang="ts">
import { computed } from 'vue';

defineOptions({
  name: 'HomeMiniChart'
});

interface Props {
  /** 数据（按最大值归一） */
  data?: number[];
  /** 形态：面积 / 柱状 / 折线 */
  type?: 'area' | 'bar' | 'line';
  /** 主色 */
  color?: string;
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  type: 'area',
  color: '#1677ff'
});

/** SVG 画布（与 biotech 首页一致的比例） */
const VIEW_W = 100;
const VIEW_H = 46;

/** 渐变 id：模块内自增，避免多实例冲突 */
let seq = 0;
const gradientId = `home-mini-chart-${(seq += 1)}`;

/** 折线/面积的坐标点串 */
const points = computed(() => {
  if (!props.data.length) {
    return '';
  }
  const max = Math.max(...props.data) || 1;
  const step = props.data.length > 1 ? VIEW_W / (props.data.length - 1) : 0;
  return props.data
    .map((value, index) => `${(index * step).toFixed(2)},${(VIEW_H - (value / max) * VIEW_H).toFixed(2)}`)
    .join(' ');
});

/** 柱状图的柱子（宽度按数据个数均分） */
const bars = computed(() => {
  if (!props.data.length) {
    return [];
  }
  const max = Math.max(...props.data) || 1;
  const barWidth = VIEW_W / props.data.length;
  return props.data.map((value, index) => {
    const height = Math.max((value / max) * VIEW_H, 1);
    return { x: index * barWidth + barWidth * 0.2, y: VIEW_H - height, width: barWidth * 0.6, height };
  });
});
</script>

<template>
  <svg class="mini-chart" :viewBox="`0 0 ${VIEW_W} ${VIEW_H}`" preserveAspectRatio="none" aria-hidden="true">
    <defs v-if="type === 'area'">
      <linearGradient :id="gradientId" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" :stop-color="color" stop-opacity="0.35" />
        <stop offset="100%" :stop-color="color" stop-opacity="0" />
      </linearGradient>
    </defs>

    <template v-if="type === 'bar'">
      <rect
        v-for="(bar, index) in bars"
        :key="index"
        :x="bar.x"
        :y="bar.y"
        :width="bar.width"
        :height="bar.height"
        :fill="color"
        rx="1"
      />
    </template>
    <template v-else>
      <polygon v-if="type === 'area'" :points="`0,${VIEW_H} ${points} ${VIEW_W},${VIEW_H}`" :fill="`url(#${gradientId})`" />
      <polyline :points="points" fill="none" :stroke="color" stroke-width="1.5" vector-effect="non-scaling-stroke" />
    </template>
  </svg>
</template>

<style scoped>
.mini-chart {
  width: 100%;
  height: 46px;
  overflow: visible;
}
</style>
