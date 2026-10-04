<script setup lang="tsx">
import { ref, watch } from 'vue';
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

/** 搜索条件：报出 = 是(1) / 否(0) */
const REPORTED_OPTIONS = [
  { label: '是', value: 1 },
  { label: '否', value: 0 }
];

/**
 * 列配置（key = 数据库列名，顺序 = 展示顺序）
 * <p>
 * 规则：**原本已经在展示的列放最前（保留中文标题）**，其后是明细表「除公共字段外的其余列」，
 * 标题先用数据库列名；最后一列固定是「报出」。
 * mut_depth/total_depth（SNP/Indel）与 start（CNV）已包含在前面已有的复合/等价列里，不再重复列。
 */
interface ColumnSpec {
  key: string;
  title: string;
  width?: number;
}

const LONG_TEXT_KEYS = new Set([
  'ori_variant',
  'aa_change_known_gene',
  'cosmic91',
  'clinvar',
  'clinvar_result',
  'database_info',
  'cosmic_info',
  'db_info',
  'sclip1_info',
  'sclip2_info',
  'interpro_domain',
  'omim_phenotypes',
  'hgmd_disease',
  'clinical_significance_clinvar',
  'clinical_significance_enigma',
  'classification_lovd'
]);

const COLUMN_SPEC: Record<string, ColumnSpec[]> = {
  SNP_INDEL: [
    { key: 'gene', title: '基因', width: 110 },
    { key: 'variant', title: '突变', width: 120 },
    { key: 'ori_variant', title: '原始描述' },
    { key: 'exonic_func_known_gene', title: '突变类型', width: 170 },
    { key: 'exon', title: '外显子', width: 100 },
    { key: 'mut_freq', title: '突变丰度(%)', width: 130 },
    { key: 'id', title: 'id', width: 90 },
    { key: 'file_id', title: 'file_id', width: 90 },
    { key: 'chr', title: 'chr', width: 80 },
    { key: 'start', title: 'start', width: 110 },
    { key: 'end', title: 'end', width: 110 },
    { key: 'ref', title: 'ref', width: 80 },
    { key: 'alt', title: 'alt', width: 80 },
    { key: 'hom_het', title: 'hom_het', width: 100 },
    { key: 'mut_depth', title: 'mut_depth', width: 110 },
    { key: 'total_depth', title: 'total_depth', width: 110 },
    { key: 'func_known_gene', title: 'func_known_gene' },
    { key: 'gene_known_gene', title: 'gene_known_gene' },
    { key: 'aa_change_known_gene', title: 'aa_change_known_gene' },
    { key: 'esp6500si_all', title: 'esp6500si_all' },
    { key: 'c1000g2012apr_all', title: 'c1000g2012apr_all' },
    { key: 'dbsnp_rs', title: 'dbsnp_rs' },
    { key: 'cosmic91', title: 'cosmic91' },
    { key: 'clinvar', title: 'clinvar' },
    { key: 'check_depth', title: 'check_depth', width: 110 },
    { key: 'check_result', title: 'check_result', width: 110 },
    { key: 'clinvar_result', title: 'clinvar_result' },
    { key: 'sp_tag', title: 'sp_tag' },
    { key: 'final_check', title: 'final_check' },
    { key: 'database_info', title: 'database_info' },
    { key: 'transcript', title: 'transcript', width: 150 },
    { key: 'chgvs', title: 'chgvs', width: 150 },
    { key: 'phgvs', title: 'phgvs', width: 130 },
    { key: 'parent_mutation_id', title: 'parent_mutation_id', width: 150 },
    { key: 'filtered_rationale', title: 'filtered_rationale' },
    { key: 'reviewed_by', title: 'reviewed_by', width: 110 },
    { key: 'reviewed_at', title: 'reviewed_at', width: 170 }
  ],
  CNV: [
    { key: 'gene', title: '基因', width: 110 },
    { key: 'variant', title: '变异', width: 120 },
    { key: 'copy_num', title: '拷贝数', width: 100 },
    { key: 'chr', title: '染色体', width: 100 },
    { key: 'start', title: '位置', width: 130 },
    { key: 'ori_variant', title: '原始描述' },
    { key: 'id', title: 'id', width: 90 },
    { key: 'file_id', title: 'file_id', width: 90 },
    { key: 'end', title: 'end', width: 130 },
    { key: 'parent_mutation_id', title: 'parent_mutation_id', width: 150 },
    { key: 'filtered_rationale', title: 'filtered_rationale' },
    { key: 'reviewed_by', title: 'reviewed_by', width: 110 },
    { key: 'reviewed_at', title: 'reviewed_at', width: 170 }
  ],
  FUSION: [
    { key: 'gene1', title: "5' 基因", width: 120 },
    { key: 'gene2', title: "3' 基因", width: 120 },
    { key: 'variant', title: '融合', width: 160 },
    { key: 'tag', title: '类型', width: 90 },
    { key: 'fusion_reads', title: '融合 reads', width: 110 },
    { key: 'freq', title: '频率', width: 90 },
    { key: 'depth', title: '深度', width: 90 },
    { key: 'check_result', title: '检测结果', width: 110 },
    { key: 'id', title: 'id', width: 90 },
    { key: 'file_id', title: 'file_id', width: 90 },
    { key: 'chromosome1', title: 'chromosome1', width: 110 },
    { key: 'softclip1', title: 'softclip1', width: 100 },
    { key: 'sclip1_info', title: 'sclip1_info' },
    { key: 'chromosome2', title: 'chromosome2', width: 110 },
    { key: 'softclip2', title: 'softclip2', width: 100 },
    { key: 'sclip2_info', title: 'sclip2_info' },
    { key: 'driver_gene', title: 'driver_gene', width: 120 },
    { key: 'cosmic_info', title: 'cosmic_info' },
    { key: 'db_info', title: 'db_info' },
    { key: 'stream', title: 'stream', width: 100 },
    { key: 'sup_reads_hq', title: 'sup_reads_hq', width: 120 },
    { key: 'sup_reads_uniq', title: 'sup_reads_uniq', width: 130 },
    { key: 'transcript', title: 'transcript', width: 150 },
    { key: 'sarcoma_subtypes', title: 'sarcoma_subtypes' },
    { key: 'evidence_level', title: 'evidence_level', width: 120 },
    { key: 'gene', title: 'gene', width: 110 },
    { key: 'bp1', title: 'bp1', width: 100 },
    { key: 'bp2', title: 'bp2', width: 100 },
    { key: 'ori_variant', title: 'ori_variant' },
    { key: 'parent_mutation_id', title: 'parent_mutation_id', width: 150 },
    { key: 'filtered_rationale', title: 'filtered_rationale' },
    { key: 'reviewed_by', title: 'reviewed_by', width: 110 },
    { key: 'reviewed_at', title: 'reviewed_at', width: 170 }
  ],
  CR_ALL: [
    { key: 'gene', title: '基因', width: 110 },
    { key: 'variant', title: '突变', width: 120 },
    { key: 'transcript_id', title: '转录本', width: 150 },
    { key: 'exon', title: '外显子', width: 100 },
    { key: 'chgvs', title: 'cHGVS', width: 160 },
    { key: 'phgvs', title: 'pHGVS', width: 140 },
    { key: 'zygosity', title: '合子', width: 90 },
    { key: 'clnsig', title: '临床意义', width: 180 },
    { key: 'depth', title: '深度', width: 90 },
    { key: 'id', title: 'id', width: 90 },
    { key: 'file_id', title: 'file_id', width: 90 },
    { key: 'chr', title: 'chr', width: 80 },
    { key: 'pos', title: 'pos', width: 110 },
    { key: 'exonic_func', title: 'exonic_func', width: 150 },
    { key: 'c1000g2015aug_all', title: 'c1000g2015aug_all' },
    { key: 'exac_eas', title: 'exac_eas', width: 110 },
    { key: 'clinvar_id', title: 'clinvar_id', width: 120 },
    { key: 'sift_pred', title: 'sift_pred', width: 110 },
    { key: 'polyphen2_hdiv_pred', title: 'polyphen2_hdiv_pred', width: 160 },
    { key: 'mutation_taster_pred', title: 'mutation_taster_pred', width: 170 },
    { key: 'revel', title: 'revel', width: 90 },
    { key: 'gnomad_genome_all', title: 'gnomad_genome_all', width: 150 },
    { key: 'interpro_domain', title: 'interpro_domain' },
    { key: 'omim_phenotypes', title: 'omim_phenotypes' },
    { key: 'omim_id', title: 'omim_id', width: 110 },
    { key: 'hgmd_tag', title: 'hgmd_tag', width: 110 },
    { key: 'hgmd_disease', title: 'hgmd_disease' },
    { key: 'hgmd_pmid', title: 'hgmd_pmid', width: 110 },
    { key: 'classification_lovd', title: 'classification_lovd' },
    { key: 'clinical_significance_clinvar', title: 'clinical_significance_clinvar' },
    { key: 'source', title: 'source', width: 100 },
    { key: 'clinical_significance_enigma', title: 'clinical_significance_enigma' },
    { key: 'ori_variant', title: 'ori_variant' },
    { key: 'parent_mutation_id', title: 'parent_mutation_id', width: 150 },
    { key: 'filtered_rationale', title: 'filtered_rationale' },
    { key: 'reviewed_by', title: 'reviewed_by', width: 110 },
    { key: 'reviewed_at', title: 'reviewed_at', width: 170 }
  ]
};

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
const switchingId = ref<unknown>(null);

/** 取值统一转成可显示的字符串：null/undefined/空 → '-' */
function cellText(row: Record<string, unknown>, key: string) {
  const value = row[key];
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return String(value);
}

/** 报出状态：is_reported 是 tinyint(1)，JDBC 可能给布尔(true)也可能给 0/1 */
function reported(row: Record<string, unknown>) {
  const value = row.is_reported;
  return value === true || value === 1 || value === '1';
}

/** 「报出」开关：写共享的 file_*.is_reported；成功后刷新列表，失败也刷新（保证 UI 与库一致） */
async function handleToggle(row: Record<string, unknown>, next: boolean) {
  switchingId.value = row.id;
  try {
    const { error } = await fetchUpdateVariantReportStatus({
      analysisId: props.analysisId,
      sourceType: sourceType.value,
      sourceId: Number(row.id),
      isReported: next ? 1 : 0
    });
    if (!error) {
      window.$message?.success(next ? '已设置报出' : '已设置不报出');
    }
    await getData();
  } finally {
    switchingId.value = null;
  }
}

/** 报出列（固定在右侧；无编辑权限时禁用） */
function reportedColumn() {
  return {
    key: 'is_reported',
    title: '报出',
    align: 'center' as const,
    width: 100,
    fixed: 'right' as const,
    render: (row: Record<string, unknown>) => (
      <NSwitch
        value={reported(row)}
        size="small"
        loading={switchingId.value === row.id}
        disabled={!hasAuth('report:interpretation:edit')}
        onUpdateValue={(value: boolean) => handleToggle(row, value)}
      />
    )
  };
}

/** 按位点类型生成列：配置里的字段顺序 → 报出列固定在最后 */
function buildColumns(): NaiveUI.TableColumn<Api.Report.InterpretationVariant>[] {
  const specs = COLUMN_SPEC[sourceType.value] ?? [];

  const dataColumns = specs.map(spec => ({
    key: spec.key,
    title: spec.title,
    align: 'center' as const,
    width: spec.width ?? (LONG_TEXT_KEYS.has(spec.key) ? 220 : 140),
    // 长文本一律缩略 + 悬停看全文
    ellipsis: { tooltip: true },
    render: (row: Record<string, unknown>) => cellText(row, spec.key)
  }));

  return [...dataColumns, reportedColumn()] as NaiveUI.TableColumn<Api.Report.InterpretationVariant>[];
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
      <span class="text-12px op-60">共享状态：同一分析批次的位点「报出」状态不按报告隔离</span>
    </NSpace>

    <NForm ref="formRef" :model="searchParams" label-placement="left" :label-width="90">
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="基因" path="gene">
          <NInput v-model:value="searchParams.gene" placeholder="请输入基因，如 KIT" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="报出" path="isReported">
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
      :row-key="row => row.id"
      :scroll-x="scrollX"
      remote
      size="small"
    />
  </div>
</template>

<style scoped></style>
