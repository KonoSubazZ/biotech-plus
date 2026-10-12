<script setup lang="tsx">
import { NTag } from 'naive-ui';
import { useThemeStore } from '@/store/modules/theme';
import MiniChart from './mini-chart.vue';
import { SEARCH_RANK, SEARCH_SUMMARY, formatNumber } from './home-static-data';
import type { SearchRankItem } from './home-static-data';

defineOptions({
  name: 'TopSearch'
});

const columns: NaiveUI.TableColumn<SearchRankItem>[] = [
  { title: '排名', key: 'index', width: 72, render: row => row.index },
  { title: '检索维度', key: 'keyword', minWidth: 130, render: row => <a class="keyword-link">{row.keyword}</a> },
  {
    title: '查询次数',
    key: 'count',
    width: 90,
    sorter: (a, b) => a.count - b.count,
    render: row => formatNumber(row.count)
  },
  {
    title: '周涨幅',
    key: 'range',
    width: 108,
    sorter: (a, b) => a.range - b.range,
    render: row => (
      <NTag bordered={false} size="small" type={row.status === 'up' ? 'success' : 'error'}>
        {`${row.range}% ${row.status === 'up' ? '▲' : '▼'}`}
      </NTag>
    )
  }
];

const dropdownOptions = [
  { label: '操作一', key: '1' },
  { label: '操作二', key: '2' }
];
</script>

<template>
  <NCard :bordered="false" title="报告检索排行" class="analysis-detail-card card-wrapper">
    <template #header-extra>
      <NDropdown :options="dropdownOptions">
        <NButton quaternary circle>
          <template #icon>
            <icon-ant-design-ellipsis-outlined />
          </template>
        </NButton>
      </NDropdown>
    </template>

    <NGrid :x-gap="68" :y-gap="20" cols="1 s:2" responsive="screen" class="search-summary">
      <NGi>
        <div class="number-subtitle">
          <span>报告查询次数</span>
          <NPopover trigger="hover">
            <template #trigger>
              <icon-ant-design-info-circle-outlined class="cursor-pointer text-14px op-45" />
            </template>
            指标说明
          </NPopover>
        </div>
        <div class="number-line">
          <strong>{{ formatNumber(SEARCH_SUMMARY.count.value) }}</strong>
          <span>{{ SEARCH_SUMMARY.count.trend }}<i :class="SEARCH_SUMMARY.count.status" /></span>
        </div>
        <MiniChart :data="SEARCH_SUMMARY.count.data" type="line" color="#6294fa" />
      </NGi>
      <NGi>
        <div class="number-subtitle">
          <span>人均查看报告</span>
          <NPopover trigger="hover">
            <template #trigger>
              <icon-ant-design-info-circle-outlined class="cursor-pointer text-14px op-45" />
            </template>
            指标说明
          </NPopover>
        </div>
        <div class="number-line">
          <strong>{{ SEARCH_SUMMARY.avg.value }}</strong>
          <span>{{ SEARCH_SUMMARY.avg.trend }}<i :class="SEARCH_SUMMARY.avg.status" /></span>
        </div>
        <MiniChart :data="SEARCH_SUMMARY.avg.data" type="line" color="#6294fa" />
      </NGi>
    </NGrid>

    <NDataTable
      :columns="columns"
      :data="SEARCH_RANK"
      :row-key="row => row.index"
      :pagination="{ pageSize: 5, showSizePicker: false }"
      :scroll-x="450"
      size="small"
    />
  </NCard>
</template>

<style scoped>
/* 与 biotech 首页一致 */
.analysis-detail-card {
  height: 100%;
}

.analysis-detail-card :deep(.n-card__content) {
  display: flex;
  flex-direction: column;
}

.search-summary {
  margin-bottom: 18px;
}

.number-subtitle {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #00000073;
  font-size: 14px;
}

.number-line {
  display: flex;
  align-items: baseline;
  gap: 32px;
  margin: 8px 0 2px;
}

.number-line strong {
  font-size: 24px;
  font-weight: 400;
}

.number-line span {
  color: #00000073;
  font-size: 14px;
}

.number-line i {
  display: inline-block;
  width: 0;
  height: 0;
  margin-left: 4px;
  border-right: 4px solid transparent;
  border-left: 4px solid transparent;
  vertical-align: 2px;
}

.number-line .up {
  border-bottom: 7px solid #f5222d;
}

.number-line .down {
  border-top: 7px solid #52c41a;
}

.analysis-detail-card :deep(.keyword-link) {
  color: #1677ff;
}
</style>
