<script setup lang="tsx">
import { computed, ref, watch } from 'vue';
import { NButton, NInputNumber, NSelect, NTag } from 'naive-ui';
import {
  fetchBuildInterpretationPreview,
  fetchUpdateGermlineSignificance,
  fetchUpdateGermlineTarget
} from '@/service/api/report/interpretation';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

defineOptions({
  name: 'InterpretationTabPreview'
});

interface Props {
  analysisId: number;
  reportId: number;
}

const props = defineProps<Props>();

const { hasAuth } = useAuth();

/** 五级临床意义（固定口径，见设计书 4.8） */
const SIGNIFICANCE_OPTIONS = [
  { label: '1 致病', value: 1 },
  { label: '2 可能致病', value: 2 },
  { label: '3 未知临床意义', value: 3 },
  { label: '4 可能良性', value: 4 },
  { label: '5 良性', value: 5 }
];

/** MATCHED 绿 / NOT_MATCHED 灰（未知值原样显示） */
function statusMeta(status: string | null | undefined) {
  if (status === 'MATCHED') {
    return { label: '已匹配', type: 'success' as const };
  }
  if (status === 'NOT_MATCHED') {
    return { label: '无证据', type: 'default' as const };
  }
  return { label: status ?? '-', type: 'default' as const };
}

const editing = ref(false);
const preview = ref<Api.Report.InterpretationPreview | null>(null);
const jsonVisible = ref(false);
/** 正在保存的行（避免连点） */
const savingId = ref<number | null>(null);
/** 改靶输入：位点ID → 父级ID */
const targetInput = ref<Record<number, number | null>>({});

const canEdit = computed(() => hasAuth('report:interpretation:edit'));

/** 顶部上下文一行（sampleInfo 是 Map，取值统一转字符串） */
const contextLine = computed(() => {
  const info = preview.value?.sampleInfo ?? {};
  const part = (key: string) => (info[key] === null || info[key] === undefined ? '-' : String(info[key]));
  return `样本 ${part('sampleCode')} · 癌种 ${part('disease')} · 性别 ${part('gender')}`;
});

/** JSON 原文（放 script 里，模板只负责渲染字符串） */
const previewJson = computed(() => JSON.stringify(preview.value ?? {}, null, 2));

async function loadPreview() {
  if (!props.analysisId || !props.reportId) {
    return;
  }
  editing.value = true;
  try {
    const { data, error } = await fetchBuildInterpretationPreview({
      analysisId: props.analysisId,
      reportId: props.reportId
    });
    if (!error) {
      preview.value = data;
    }
  } finally {
    editing.value = false;
  }
}

/** 保存临床意义后立即重新预览（设计书：改报出/临床意义/改靶后重新调用即可拿到新 JSON） */
async function saveSignificance(row: Api.Report.PreviewVariant, value: number) {
  savingId.value = row.sourceId;
  try {
    const { error } = await fetchUpdateGermlineSignificance({
      analysisId: props.analysisId,
      reportId: props.reportId,
      sourceId: row.sourceId,
      clinicalSignificance: value
    });
    if (!error) {
      window.$message?.success('临床意义已保存');
    }
    await loadPreview();
  } finally {
    savingId.value = null;
  }
}

async function saveTarget(row: Api.Report.PreviewVariant, parentMutationId: number | null) {
  savingId.value = row.sourceId;
  try {
    const { error } = await fetchUpdateGermlineTarget({
      analysisId: props.analysisId,
      sourceId: row.sourceId,
      parentMutationId
    });
    if (!error) {
      window.$message?.success(parentMutationId ? '已改靶' : '已取消改靶');
    }
    await loadPreview();
  } finally {
    savingId.value = null;
  }
}

/** 位点表公共前几列（基因/变异/类型/原始位点/丰度/深度） */
function baseColumns(): NaiveUI.TableColumn<Api.Report.PreviewVariant>[] {
  return [
    // Naive UI 的展开行不是表格 prop，而是**一个 type='expand' 的列**（实测踩到：只传 :render-expand 不会出触发器）
    { type: 'expand', renderExpand: renderEvidence, width: 46 },
    { key: 'gene', title: '基因', align: 'center', width: 110, render: row => row.gene ?? '-' },
    { key: 'variant', title: '变异', align: 'center', width: 150, render: row => row.variant ?? '-' },
    { key: 'mutationType', title: '类型', align: 'center', width: 90, render: row => row.mutationType ?? '-' },
    {
      key: 'oriVariant',
      title: '原始位点',
      align: 'left',
      minWidth: 220,
      ellipsis: { tooltip: true },
      render: row => row.oriVariant ?? '-'
    },
    { key: 'frequency', title: '丰度', align: 'center', width: 90, render: row => row.frequency ?? '-' },
    { key: 'depth', title: '深度', align: 'center', width: 110, render: row => row.depth ?? '-' }
  ];
}

/** 体细胞列 = 公共列 + 知识库匹配 */
function buildSomaticColumns(): NaiveUI.TableColumn<Api.Report.PreviewVariant>[] {
  return [...baseColumns(), matchStatusColumn()];
}

/** 胚系列 = 公共列 + 合子/临床意义/文件判定/改靶 + 知识库匹配 */
function buildGermlineColumns(): NaiveUI.TableColumn<Api.Report.PreviewVariant>[] {
  return [
    ...baseColumns(),
    { key: 'zygosity', title: '合子', align: 'center', width: 90, render: row => row.zygosity ?? '-' },
    {
      key: 'clinicalSignificance',
      title: '临床意义',
      align: 'center',
      width: 170,
      render: row => (
        <NSelect
          value={row.clinicalSignificance}
          options={SIGNIFICANCE_OPTIONS}
          size="small"
          disabled={!canEdit.value}
          loading={savingId.value === row.sourceId}
          onUpdateValue={(value: number) => saveSignificance(row, value)}
        />
      )
    },
    {
      key: 'sourceClnsig',
      title: '文件判定',
      align: 'center',
      width: 130,
      render: row => row.sourceClnsig ?? row.classificationLovd ?? '-'
    },
    targetColumn(),
    matchStatusColumn()
  ];
}

/** 知识库匹配状态列 */
function matchStatusColumn(): NaiveUI.TableColumn<Api.Report.PreviewVariant> {
  return {
    key: 'matchStatus',
    title: '知识库匹配',
    align: 'center',
    width: 120,
    render: row => {
      const meta = statusMeta(row.matchStatus);
      return <NTag type={meta.type}>{meta.label}</NTag>;
    }
  };
}

/** 改靶列（人工父级 + 保存/取消） */
function targetColumn(): NaiveUI.TableColumn<Api.Report.PreviewVariant> {
  return {
    key: 'target',
    title: '改靶',
    align: 'center',
    width: 230,
    render: row => (
      <div class="flex items-center justify-center gap-6px">
        <NInputNumber
          value={targetInput.value[row.sourceId] ?? row.parentMutationId}
          size="small"
          class="w-110px"
          placeholder="人工父级ID"
          disabled={!canEdit.value}
          onUpdateValue={(value: number | null) => {
            targetInput.value[row.sourceId] = value;
          }}
        />
        <NButton
          text
          type="primary"
          size="tiny"
          disabled={!canEdit.value}
          loading={savingId.value === row.sourceId}
          onClick={() => saveTarget(row, targetInput.value[row.sourceId] ?? null)}
        >
          保存
        </NButton>
        <NButton text size="tiny" disabled={!canEdit.value} onClick={() => saveTarget(row, null)}>
          取消
        </NButton>
      </div>
    )
  };
}

/** 展开行：证据列表（药物/癌种/等级/关系/说明） */
function renderEvidence(row: Api.Report.PreviewVariant) {
  if (!row.drugMatch || row.drugMatch.length === 0) {
    return (
      <div class="px-16px py-12px text-13px op-60">
        该位点在当前癌种（含父级癌种）下没有查到药物证据
        {row.variationClass ? `；变异分类：${row.variationClass}` : ''}
      </div>
    );
  }
  return (
    <div class="px-16px py-12px">
      <div class="mb-8px text-13px font-medium">
        药物证据 {row.drugMatch.length} 条
        {row.variationClass ? ` · 变异分类 ${row.variationClass}` : ''}
      </div>
      <div class="flex-col gap-8px">
        {row.drugMatch.map((drug, index) => (
          <div key={index} class="rounded bg-#f5f7fa p-10px">
            <div class="mb-4px flex items-center gap-8px">
              <span class="font-medium">{drug.drugName ?? '-'}</span>
              <span class="text-12px op-60">{drug.disease ?? '-'}</span>
              {drug.evidenceType ? <NTag size="small">{drug.evidenceType}</NTag> : null}
              {drug.evidenceRanking ? <NTag size="small" type="info">{drug.evidenceRanking}</NTag> : null}
              {drug.relationship ? <NTag size="small" type="warning">{drug.relationship}</NTag> : null}
            </div>
            <div class="text-12px leading-20px op-80">{drug.annotation ?? drug.comment ?? '-'}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

const sectionMeta = computed(() => [
  {
    key: 'somatic',
    title: '体细胞变异解析',
    hint: '报出的体细胞位点（SNP/Indel + CNV + Fusion），按癌种匹配 NKB 用药证据',
    section: preview.value?.somaticVariants,
    columns: buildSomaticColumns()
  },
  {
    key: 'germline',
    title: '肿瘤遗传风险',
    hint: '报出的胚系位点（CR_ALL），临床意义人工确认后按致病性分级匹配证据',
    section: preview.value?.germlineVariants,
    columns: buildGermlineColumns()
  }
]);

watch(() => [props.analysisId, props.reportId], loadPreview, { immediate: true });
</script>

<template>
  <div class="flex-col-stretch gap-12px">
    <NCard :bordered="false" size="small" class="card-wrapper">
      <NSpace align="center" justify="space-between">
        <NSpace align="center" :size="12">
          <span class="font-medium">报告预览</span>
          <NTag size="small" type="info">schema {{ preview?.schemaVersion ?? '-' }}</NTag>
          <span class="text-13px op-60">模板 {{ preview?.templateCode ?? '未绑定' }}</span>
          <span class="text-13px op-60">{{ contextLine }}</span>
        </NSpace>
        <NSpace :size="8">
          <NButton size="small" @click="jsonVisible = true">查看 JSON</NButton>
          <NButton size="small" type="primary" ghost :loading="editing" @click="loadPreview">重新预览</NButton>
        </NSpace>
      </NSpace>
      <NAlert v-if="preview?.warnings?.length" type="warning" :bordered="false" class="mt-12px">
        <div v-for="item in preview.warnings" :key="item">{{ item }}</div>
      </NAlert>
    </NCard>

    <NCard
      v-for="meta in sectionMeta"
      :key="meta.key"
      :bordered="false"
      size="small"
      class="card-wrapper"
    >
      <template #header>
        <div class="flex items-center gap-8px">
          <span>{{ meta.title }}</span>
          <NTag size="small">报出 {{ meta.section?.summary?.reportedCount ?? 0 }}</NTag>
          <NTag size="small" type="success">已匹配 {{ meta.section?.summary?.matchedCount ?? 0 }}</NTag>
          <NTag size="small">无证据 {{ meta.section?.summary?.unmatchedCount ?? 0 }}</NTag>
        </div>
      </template>
      <template #header-extra>
        <span class="text-12px op-60">{{ meta.hint }}</span>
      </template>

      <NDataTable
        :columns="meta.columns"
        :data="meta.section?.items ?? []"
        :loading="editing"
        :row-key="row => row.sourceId"
        :scroll-x="1200"
        size="small"
      />
    </NCard>

    <NModal
      v-model:show="jsonVisible"
      preset="card"
      title="预览 JSON（后端组装结果，对齐设计书 7.6 契约）"
      class="w-90vw max-w-1400px"
      :bordered="false"
    >
      <pre class="max-h-70vh overflow-auto whitespace-pre-wrap break-all rounded bg-#f5f7fa p-12px text-12px">{{
        previewJson
      }}</pre>
    </NModal>
  </div>
</template>

<style scoped></style>
