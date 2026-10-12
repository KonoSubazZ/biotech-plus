<script setup lang="ts">
import { computed } from 'vue';
import SalesBarChart from './sales-bar-chart.vue';
import {
  MONTH_REPORT_COUNT,
  MONTH_SAMPLE_COUNT,
  REPORT_TYPE_RANK,
  SAMPLE_TYPE_RANK,
  formatNumber
} from './home-static-data';

defineOptions({
  name: 'SalesDimensionBody'
});

interface Props {
  /** 维度：报告 / 样本 */
  dimension: 'report' | 'sample';
}

const props = defineProps<Props>();

const chartData = computed(() => (props.dimension === 'report' ? MONTH_REPORT_COUNT : MONTH_SAMPLE_COUNT));
const chartLabel = computed(() => (props.dimension === 'report' ? '报告数量' : '样本数量'));
const rankingTitle = computed(() => (props.dimension === 'report' ? '报告类型排行' : '样本类型排行'));
const rankingRows = computed(() => (props.dimension === 'report' ? REPORT_TYPE_RANK : SAMPLE_TYPE_RANK));
</script>

<template>
  <div class="sales-body">
    <div class="sales-chart-wrap">
      <SalesBarChart :data="chartData" :label="chartLabel" />
    </div>
    <div class="sales-ranking">
      <h4>{{ rankingTitle }}</h4>
      <ol>
        <li v-for="(row, index) in rankingRows" :key="row.title">
          <span class="ranking-index" :class="{ active: index < 3 }">{{ index + 1 }}</span>
          <span class="ranking-title">{{ row.title }}</span>
          <span>{{ formatNumber(row.total) }}</span>
        </li>
      </ol>
    </div>
  </div>
</template>

<style scoped>
.sales-body {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(260px, 1fr);
  gap: 24px;
}

.sales-chart-wrap {
  padding: 16px 0 32px 32px;
}

.sales-ranking {
  padding: 16px 32px 32px;
}

.sales-ranking h4 {
  margin: 0 0 25px;
  font-size: 14px;
  font-weight: 500;
}

.sales-ranking ol {
  margin: 0;
  padding: 0;
  list-style: none;
}

.sales-ranking li {
  display: flex;
  align-items: center;
  margin-top: 16px;
  font-size: 14px;
}

.ranking-index {
  width: 20px;
  height: 20px;
  margin-right: 16px;
  border-radius: 50%;
  background: #f0f0f0;
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
  text-align: center;
}

.ranking-index.active {
  background: #314659;
  color: #fff;
}

.ranking-title {
  flex: 1;
  min-width: 0;
  margin-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .sales-body {
    grid-template-columns: 1fr;
  }

  .sales-chart-wrap {
    padding: 12px;
  }

  .sales-ranking {
    padding: 0 16px 24px;
  }
}
</style>
