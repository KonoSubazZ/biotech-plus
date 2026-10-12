<script setup lang="ts">
import { computed } from 'vue';
import { useAppStore } from '@/store/modules/app';
import AnalysisStatCard from './modules/analysis-stat-card.vue';
import SalesOverview from './modules/sales-overview.vue';
import SalesProportion from './modules/sales-proportion.vue';
import TopSearch from './modules/top-search.vue';
import { REPORT_DONE_PERCENT, STAT_CARDS } from './modules/home-static-data';

defineOptions({
  name: 'Home'
});

const appStore = useAppStore();

const gap = computed(() => (appStore.isMobile ? 16 : 24));
</script>

<template>
  <NSpace vertical :size="gap">
    <!-- ① 顶部 4 张统计卡：主数字 + 迷你图 / 进度条 + 底部两栏 -->
    <NGrid :x-gap="gap" :y-gap="gap" cols="1 s:2 l:4" responsive="screen">
      <NGi v-for="card in STAT_CARDS" :key="card.title">
        <AnalysisStatCard
          :title="card.title"
          :total="card.total"
          :footer-label="card.footerLabel"
          :footer-value="card.footerValue"
          :variant="card.variant"
          :data="card.data ?? []"
          :percentage="REPORT_DONE_PERCENT"
        />
      </NGi>
    </NGrid>

    <!-- ② 报告 / 样本维度：12 个月柱状图 + 类型排行 -->
    <SalesOverview />

    <!-- ③ 左：报告检索排行（两个指标 + 排行表）；右：报告 / 样本类型占比 -->
    <NGrid :x-gap="gap" :y-gap="gap" cols="1 l:2" responsive="screen">
      <NGi>
        <TopSearch />
      </NGi>
      <NGi>
        <SalesProportion />
      </NGi>
    </NGrid>
  </NSpace>
</template>

<style scoped></style>
