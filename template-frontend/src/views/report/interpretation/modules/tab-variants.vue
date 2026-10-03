<script setup lang="tsx">
import { computed, ref, watch } from 'vue';
import { NButton, NSwitch } from 'naive-ui';
import {
  fetchGetInterpretationVariantList,
  fetchUpdateVariantReportStatus
} from '@/service/api/report/interpretation';
import { useAuth } from '@/hooks/business/auth';
import { useNaiveForm } from '@/hooks/common/form';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { $t } from '@/locales';

defineOptions({
  name: 'InterpretationTabVariants'
});

interface Props {
  /** 分析数据ID：位点范围锁定在这个批次内 */
  analysisId: number;
}

const props = defineProps<Props>();

const { hasAuth } = useAuth();
const { formRef, validate, restoreValidation } = useNaiveForm();

/** 四类位点（与后端 history_*.source_type 口径一致） */
const SOURCE_TYPE_OPTIONS = [
  { label: 'SNP/Indel', value: 'SNP_INDEL' },
  { label: 'CNV', value: 'CNV' },
  { label: 'Fusion', value: 'FUSION' },
  { label: 'CR_ALL', value: 'CR_ALL' }
];

const REPORTED_OPTIONS = [
  { label: '入报告', value: 1 },
  { label: '不入报告', value: 0 }
];

/** 当前位点类型（切换后列会整体换一套） */
const sourceType = ref('SNP_INDEL');

// analysisId 必须在初始化时就带上（useNaivePaginatedTable 在 setup 阶段立刻发一次请求）
const searchParams = ref<Api.Report.InterpretationVariantSearchParams>({
  analysisId: props.analysisId ?? 0,
  sourceType: sourceType.value,
  gene: null,
  isReported: null,
  pageNum: 1,
  pageSize: 10,
  params: {}
});

const { columns, data, getData, getDataByPage, loading, mobilePagination, reloadColumns, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetInterpretationVariantList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => buildColumns()
  });

/** 正在切换状态的位点（行级 loading，避免连点） */
const switchingId = ref<number | null>(null);

/** 「入报告」开关：写共享的 file_*.is_reported；成功后刷新列表，失败也刷新（保证 UI 与库一致） */
async function handleToggle(row: Api.Report.InterpretationVariant, next: boolean) {
  switchingId.value = row.sourceId;
  try {
    const { error } = await fetchUpdateVariantReportStatus({
      analysisId: props.analysisId,
      sourceType: sourceType.value,
      sourceId: row.sourceId,
      isReported: next ? 1 : 0
    });
    if (!error) {
      window.$message?.success(next ? '已加入报告' : '已移出报告');
    }
    await getData();
  } finally {
    switchingId.value = null;
  }
}

/** 状态列：入报告开关（无编辑权限时禁用） */
function reportedColumn(width = 110) {
  return {
    key: 'isReported',
    title: '入报告',
    align: 'center' as const,
    width,
    fixed: 'right' as const,
    render: (row: Api.Report.InterpretationVariant) => (
      <NSwitch
        value={row.isReported === 1}
        size="small"
        loading={switchingId.value === row.sourceId}
        disabled={!hasAuth('report:interpretation:edit')}
        onUpdateValue={(value: boolean) => handleToggle(row, value)}
      />
    )
  };
}

/** 按位点类型给出各自的列（四张表明细字段不同，列也就不一样） */
function buildColumns(): NaiveUI.TableColumn<Api.Report.InterpretationVariant>[] {
  if (sourceType.value === 'CNV') return buildCnvColumns();
  if (sourceType.value === 'FUSION') return buildFusionColumns();
  if (sourceType.value === 'CR_ALL') return buildCrAllColumns();
  return buildSnpIndelColumns();
}

/** SNP / Indel（file_Somatic_SNV_Indel） */
function buildSnpIndelColumns(): NaiveUI.TableColumn<Api.Report.InterpretationVariant>[] {
  return [
    { key: 'gene', title: '基因', align: 'center', width: 110 },
    { key: 'variant', title: '突变', align: 'center', width: 120 },
    { key: 'oriVariant', title: '原始描述', align: 'left', minWidth: 280, ellipsis: { tooltip: true } },
    { key: 'mutationType', title: '突变类型', align: 'center', width: 170 },
    { key: 'exon', title: '外显子', align: 'center', width: 100 },
    { key: 'mutFreq', title: '突变丰度(%)', align: 'center', width: 130 },
    {
      key: 'depth',
      title: '深度(突变/总)',
      align: 'center',
      width: 140,
      render: (row: Api.Report.InterpretationVariant) => `${row.mutDepth ?? '-'}/${row.totalDepth ?? '-'}`
    },
    reportedColumn()
  ];
}

/** CNV（file_CNV） */
function buildCnvColumns(): NaiveUI.TableColumn<Api.Report.InterpretationVariant>[] {
  return [
    { key: 'gene', title: '基因', align: 'center', width: 110 },
    { key: 'variant', title: '变异', align: 'center', width: 120 },
    { key: 'copyNum', title: '拷贝数', align: 'center', width: 100 },
    { key: 'chromosome', title: '染色体', align: 'center', width: 100 },
    { key: 'position', title: '位置', align: 'center', width: 130 },
    { key: 'oriVariant', title: '原始描述', align: 'left', minWidth: 260, ellipsis: { tooltip: true } },
    reportedColumn()
  ];
}

/** Fusion（file_Fusion） */
function buildFusionColumns(): NaiveUI.TableColumn<Api.Report.InterpretationVariant>[] {
  return [
    { key: 'gene1', title: "5' 基因", align: 'center', width: 120 },
    { key: 'gene2', title: "3' 基因", align: 'center', width: 120 },
    { key: 'variant', title: '融合', align: 'center', width: 160 },
    { key: 'tag', title: '类型', align: 'center', width: 90 },
    { key: 'fusionReads', title: '融合 reads', align: 'center', width: 110 },
    { key: 'mutFreq', title: '频率', align: 'center', width: 90 },
    { key: 'mutDepth', title: '深度', align: 'center', width: 90 },
    { key: 'checkResult', title: '检测结果', align: 'center', width: 110 },
    reportedColumn()
  ];
}

/** CR_ALL（file_CR_ALL，胚系） */
function buildCrAllColumns(): NaiveUI.TableColumn<Api.Report.InterpretationVariant>[] {
  return [
    { key: 'gene', title: '基因', align: 'center', width: 110 },
    { key: 'variant', title: '突变', align: 'center', width: 120 },
    { key: 'transcript', title: '转录本', align: 'center', width: 150 },
    { key: 'exon', title: '外显子', align: 'center', width: 100 },
    { key: 'chgvs', title: 'cHGVS', align: 'center', width: 160, ellipsis: { tooltip: true } },
    { key: 'phgvs', title: 'pHGVS', align: 'center', width: 140, ellipsis: { tooltip: true } },
    { key: 'homHet', title: '合子', align: 'center', width: 90 },
    { key: 'clinicalSignificance', title: '临床意义', align: 'left', width: 180, ellipsis: { tooltip: true } },
    { key: 'totalDepth', title: '深度', align: 'center', width: 90 },
    reportedColumn()
  ];
}

/** 位点类型切换：换一整套列（reloadColumns 重建列设置）+ 重新取数 */
async function onSourceTypeChange(value: string) {
  sourceType.value = value;
  searchParams.value.sourceType = value;
  searchParams.value.pageNum = 1;
  reloadColumns();
  await getDataByPage();
}

async function search() {
  await validate();
  getDataByPage();
}

async function reset() {
  await restoreValidation();
  searchParams.value.gene = null;
  searchParams.value.isReported = null;
  getDataByPage();
}

watch(
  () => props.analysisId,
  value => {
    if (value && value !== searchParams.value.analysisId) {
      searchParams.value.analysisId = value;
      getData();
    }
  }
);
</script>

<template>
  <div class="flex-col-stretch gap-12px">
    <NSpace align="center" justify="space-between">
      <NRadioGroup :value="sourceType" size="small" @update:value="onSourceTypeChange">
        <NRadioButton v-for="item in SOURCE_TYPE_OPTIONS" :key="item.value" :value="item.value">
          {{ item.label }}
        </NRadioButton>
      </NRadioGroup>
      <span class="text-12px op-60">共享状态：同一分析批次的位点「入报告」状态不按报告隔离</span>
    </NSpace>

    <NForm ref="formRef" :model="searchParams" label-placement="left" :label-width="90">
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="基因" path="gene">
          <NInput v-model:value="searchParams.gene" placeholder="请输入基因，如 KIT" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="入报告" path="isReported">
          <NSelect
            v-model:value="searchParams.isReported"
            :options="REPORTED_OPTIONS"
            placeholder="不限"
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

    <NDataTable
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="mobilePagination"
      :row-key="row => row.sourceId"
      :scroll-x="scrollX"
      remote
      size="small"
    />
  </div>
</template>

<style scoped></style>
