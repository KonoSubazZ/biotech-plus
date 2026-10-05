<script setup lang="tsx">
import { computed, ref } from 'vue';
import dayjs from 'dayjs';
import { NTag } from 'naive-ui';
import { useAppStore } from '@/store/modules/app';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { useDownload } from '@/hooks/business/download';
import { fetchGetVariantHistoryList } from '@/service/api/report/variant-history';
import { $t } from '@/locales';
import VariantHistorySearch from './modules/variant-history-search.vue';

defineOptions({
  name: 'ReportVariantHistory'
});

/** 用药信息 8 个分组字段（与 en7 drugGroups 的键一一对应，顺序即展示顺序） */
type DrugField =
  | 'drugsA'
  | 'drugsB'
  | 'drugsC'
  | 'drugsD'
  | 'resistantDrugsA'
  | 'resistantDrugsB'
  | 'resistantDrugsC'
  | 'resistantDrugsD';

interface DrugGroupItem {
  label: string;
  drugs: string;
  count: number;
}

const DRUG_GROUPS: { field: DrugField; label: string }[] = [
  { field: 'drugsA', label: '获益A级' },
  { field: 'drugsB', label: '获益B级' },
  { field: 'drugsC', label: '获益C级' },
  { field: 'drugsD', label: '获益D级' },
  { field: 'resistantDrugsA', label: '耐药A级' },
  { field: 'resistantDrugsB', label: '耐药B级' },
  { field: 'resistantDrugsC', label: '耐药C级' },
  { field: 'resistantDrugsD', label: '耐药D级' }
];

const appStore = useAppStore();
const { download } = useDownload();

/** 位点类型（source_type）→ 展示标签，与「筛选位点」页的 SOURCE_TYPE_OPTIONS 同口径 */
const SITE_TYPE_LABELS: Record<string, string> = {
  SNP_INDEL: 'SNP/Indel',
  CNV: 'CNV',
  FUSION: 'Fusion',
  CR_ALL: 'CR_ALL'
};

/** 位点类型展示标签（认不出的原样显示，不吞） */
function siteTypeLabel(sourceType: string | null) {
  if (!sourceType) {
    return '-';
  }
  return SITE_TYPE_LABELS[sourceType.toUpperCase()] ?? sourceType;
}

/** 用药信息弹窗：点表格单元格打开，显示该位点各分组的完整药名 */
const drugDetailVisible = ref(false);
const drugDetailRow = ref<Api.Report.VariantHistoryRow | null>(null);
const drugDetailGroups = ref<DrugGroupItem[]>([]);

const drugDetailTitle = computed(() => {
  const row = drugDetailRow.value;
  return row ? `用药信息：${row.gene ?? '-'} ${row.variant ?? '-'}` : '用药信息';
});

/** 取该行有值的分组（空分组不返回 —— 页面「没有就不展示」靠它） */
function drugGroupsOf(row: Api.Report.VariantHistoryRow): DrugGroupItem[] {
  return DRUG_GROUPS.map(group => {
    const drugs = row[group.field];
    return { label: group.label, drugs: drugs ?? '', count: drugs ? drugs.split(';').length : 0 };
  }).filter(group => group.count > 0);
}

function openDrugDetail(row: Api.Report.VariantHistoryRow) {
  drugDetailRow.value = row;
  drugDetailGroups.value = drugGroupsOf(row);
  drugDetailVisible.value = true;
}

/**
 * 用药信息列：缩略展示 —— 每个有值的分组一行，蓝色 tag「获益C级 19个」+ 药名（单行截断，悬停看全量）。
 * 点单元格打开弹窗看完整药名（原始 drug_info 只在库里，接口不吐，也不展示）。
 */
function renderDrugGroups(row: Api.Report.VariantHistoryRow) {
  const groups = drugGroupsOf(row);
  if (!groups.length) {
    return <span>-</span>;
  }
  return (
    <div class="cursor-pointer flex-col gap-4px" onClick={() => openDrugDetail(row)}>
      {groups.map(group => (
        <div key={group.label} class="flex items-center gap-6px">
          <NTag size="small" type="info" class="shrink-0">{`${group.label} ${group.count}个`}</NTag>
          <span class="truncate text-12px op-80" title={group.drugs}>
            {group.drugs}
          </span>
        </div>
      ))}
    </div>
  );
}

const searchParams = ref<Api.Report.VariantHistorySearchParams>({
  pageNum: 1,
  pageSize: 10,
  subbarcode: null,
  disease: null,
  product: null,
  client: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetVariantHistoryList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { key: 'subbarcode', title: '样本编号', align: 'center', minWidth: 150 },
      { key: 'client', title: '客户', align: 'center', minWidth: 160, ellipsis: { tooltip: true } },
      { key: 'gene', title: '基因', align: 'center', minWidth: 110 },
      { key: 'oriVariant', title: '原始位点', align: 'center', minWidth: 260, ellipsis: { tooltip: true } },
      { key: 'disease', title: '解读癌种', align: 'center', minWidth: 130 },
      { key: 'product', title: '产品', align: 'center', minWidth: 170, ellipsis: { tooltip: true } },
      { key: 'variant', title: '变异', align: 'center', minWidth: 160, ellipsis: { tooltip: true } },
      { key: 'abundance', title: '丰度', align: 'center', minWidth: 110, render: row => row.abundance ?? '-' },
      {
        key: 'siteType',
        title: '位点类型',
        align: 'center',
        minWidth: 110,
        render: row => siteTypeLabel(row.sourceType)
      },
      { key: 'variantType', title: '突变类型', align: 'center', minWidth: 130, render: row => row.variantType ?? '-' },
      { key: 'level', title: '位点等级', align: 'center', minWidth: 110, render: row => row.level ?? '-' },
      { key: 'drugGroups', title: '用药信息', minWidth: 320, render: row => renderDrugGroups(row) },
      { key: 'analyzer', title: '解读人', align: 'center', minWidth: 110, render: row => row.analyzer ?? '-' },
      {
        key: 'reportGeneratedAt',
        title: '报告时间',
        align: 'center',
        minWidth: 170,
        render: row => row.reportGeneratedAt ?? '-'
      }
    ]
  });

/** 导出 xlsx：查询条件与列表一致（后端同一套 where，不分页导出全部筛选结果） */
function handleExport() {
  download(
    '/report/variantHistory/export',
    { ...searchParams.value },
    `历史检出_${dayjs().format('YYYYMMDDHHmmss')}.xlsx`
  );
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <VariantHistorySearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="历史检出" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="true"
          :loading="loading"
          :show-add="false"
          :show-delete="false"
          :show-export="true"
          @refresh="getData"
          @export="handleExport"
        />
      </template>

      <NDataTable
        :columns="columns"
        :data="data"
        :flex-height="!appStore.isMobile"
        :loading="loading"
        :pagination="mobilePagination"
        :row-key="row => row.id"
        :scroll-x="scrollX"
        remote
        size="small"
        class="sm:h-full"
      />
    </NCard>

    <!-- 用药信息详情：点表格「用药信息」格打开，显示每个分组的完整药名 -->
    <NModal
      v-model:show="drugDetailVisible"
      preset="card"
      :title="drugDetailTitle"
      :bordered="false"
      class="w-70vw max-w-900px"
    >
      <div v-if="drugDetailRow" class="flex-col-stretch gap-12px">
        <NDescriptions :column="3" label-placement="left" size="small">
          <NDescriptionsItem label="样本编号">{{ drugDetailRow.subbarcode ?? '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="客户">{{ drugDetailRow.client ?? '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="解读癌种">{{ drugDetailRow.disease ?? '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="原始位点">{{ drugDetailRow.oriVariant ?? '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="丰度">{{ drugDetailRow.abundance ?? '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="位点类型">{{ siteTypeLabel(drugDetailRow.sourceType) }}</NDescriptionsItem>
          <NDescriptionsItem label="突变类型">{{ drugDetailRow.variantType ?? '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="位点等级">{{ drugDetailRow.level ?? '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="报告时间">{{ drugDetailRow.reportGeneratedAt ?? '-' }}</NDescriptionsItem>
        </NDescriptions>

        <NDivider class="my-4px!" />

        <div v-if="!drugDetailGroups.length" class="op-60">该位点没有用药信息</div>
        <div v-for="group in drugDetailGroups" :key="group.label" class="flex-col gap-6px">
          <div class="flex items-center gap-8px">
            <NTag size="small" type="info">{{ group.label }}</NTag>
            <span class="text-12px op-60">{{ group.count }} 个药物</span>
          </div>
          <div class="rounded bg-#f5f7fa p-10px text-13px leading-22px break-all">{{ group.drugs }}</div>
        </div>
      </div>
    </NModal>
  </div>
</template>

<style scoped></style>
