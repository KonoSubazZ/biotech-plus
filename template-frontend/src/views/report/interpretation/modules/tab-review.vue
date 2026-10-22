<script setup lang="tsx">
import { computed, h, ref, watch } from 'vue';
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NDescriptions,
  NDescriptionsItem,
  NDivider,
  NInput,
  NModal,
  NSpace,
  NSpin,
  NTag
} from 'naive-ui';
import {
  fetchApproveReview,
  fetchBuildInterpretationPreview,
  fetchGetInterpretationContext,
  fetchGetInterpretationFileList,
  fetchGetReportFileUrl,
  fetchRejectReview
} from '@/service/api/report/interpretation';
import { useAuthStore } from '@/store/modules/auth';
import { useDownload } from '@/hooks/business/download';
import { gradeCountTags } from './grade-count';
import { REPORT_STATUS_META, statusMeta } from './interpretation-status';

defineOptions({
  name: 'InterpretationTabReview'
});

interface Props {
  analysisId: number;
  reportId: number;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'changed'): void;
}

const emit = defineEmits<Emits>();

const authStore = useAuthStore();
const { zip } = useDownload();

const context = ref<Api.Report.InterpretationContext | null>(null);
const preview = ref<Api.Report.InterpretationPreview | null>(null);
/** 预览来源：artifact = 报告已生成（成品文件在）；realtime = 只组装过、还没生成 */
const previewSource = ref<'artifact' | 'realtime' | ''>('');
/** 集群文件个数（与 Tab② 集群对接同源：analysis_file_status 该批次条数） */
const fileCount = ref<number | null>(null);
const loading = ref(false);
const acting = ref(false);
const rejectVisible = ref(false);
const rejectReason = ref('');

const report = computed(() => context.value?.report ?? null);
/** 报告 JSON 的报告信息 / 样本信息（都是宽松 Map，这里按需取值） */
const reportInfo = computed(() => (preview.value?.reportInfo ?? {}) as Record<string, unknown>);
const sampleInfo = computed(() => (preview.value?.sampleInfo ?? {}) as Record<string, unknown>);

/** 状态展示：中文标签 + 配色（未知值原样显示，不静默按正常色渲染） */
const statusTag = computed(() => statusMeta(REPORT_STATUS_META, report.value?.reportStatus ?? null));

const hasReportFile = computed(() => previewSource.value === 'artifact');
const reportName = computed(() => text(reportInfo.value.reportName));

const currentUserId = computed(() => String(authStore.userInfo.user?.userId ?? ''));

const isProducer = computed(() => {
  const producer = report.value?.reportGeneratedBy;
  return producer != null && String(producer) === currentUserId.value;
});

/** 允许审核的状态：40 报告待审核 / 41 报告审核中（与后端 REVIEWABLE_STATUS 一致），且非产出人本人 */
const REVIEWABLE_STATUS = [40, 41];
const isAssignedReviewer = computed(() => {
  const reviewerId = report.value?.reportReviewerId ?? report.value?.reportReviewClaimedBy;
  return reviewerId === null || reviewerId === undefined || String(reviewerId) === currentUserId.value;
});

/** 只有「待审核 / 审核中」且非产出人本人才能审核（服务端也会再校验一次） */
const canReview = computed(
  () => REVIEWABLE_STATUS.includes(Number(report.value?.reportStatus)) && !isProducer.value
    && isAssignedReviewer.value
);

/** 不能审核时的原因提示；为空表示可审核 */
const reviewHint = computed(() => {
  const status = report.value?.reportStatus;
  if (!status) {
    return '';
  }
  if (!REVIEWABLE_STATUS.includes(Number(status))) {
    return `当前状态「${statusTag.value.label}」不可审核：只有「报告待审核 / 报告审核中」的报告能通过/驳回（请在「报告预览」先提交解读）。`;
  }
  if (isProducer.value) {
    return '产出人不能审核自己的报告。';
  }
  return '';
});

/* ---------------- 位点解读信息（全部来自报告 JSON 制品） ---------------- */

const somaticTags = computed(() => gradeCountTags(preview.value?.somaticVariants, 'somatic'));
const germlineTags = computed(() => gradeCountTags(preview.value?.germlineVariants, 'germline'));
const somaticSummary = computed(() => preview.value?.somaticVariants?.summary);
const germlineSummary = computed(() => preview.value?.germlineVariants?.summary);
const somaticRows = computed(() => preview.value?.somaticVariants?.items ?? []);
const germlineRows = computed(() => preview.value?.germlineVariants?.items ?? []);

/** 取值兜底：空显示 '/' */
function text(value: unknown): string {
  return value == null || value === '' ? '/' : String(value);
}

/** 位点表行 key */
const variantRowKey = (row: Api.Report.PreviewVariant) => row.sourceId;

/** 匹配状态标签 */
function matchTag(status: string | null) {
  if (status === 'MATCHED') {
    return { label: '已匹配', type: 'success' as const };
  }
  if (status === 'NOT_MATCHED') {
    return { label: '未匹配', type: 'warning' as const };
  }
  return status ? { label: status, type: 'warning' as const } : { label: '/', type: 'default' as const };
}

/** 获益用药标签：获益药带证据等级（药名·等级），耐药药单独一列。
 *  同药同等级只出一个 tag（不同证据癌种合并，悬停 title 里给癌种），避免一格里十几个重复 tag。 */
function drugTagItems(row: Api.Report.PreviewVariant, resistant: boolean) {
  const merged = new Map<string, { key: string; label: string; diseases: string[]; resistant: boolean }>();
  for (const drug of row.drugMatch ?? []) {
    if ((drug.relation === 'RESISTANT') !== resistant) {
      continue;
    }
    const key = `${drug.drugName ?? ''}·${drug.levelName ?? ''}`;
    const hit = merged.get(key);
    if (hit) {
      if (drug.disease && !hit.diseases.includes(drug.disease)) {
        hit.diseases.push(drug.disease);
      }
      continue;
    }
    merged.set(key, {
      key: `${row.sourceId}-${key}`,
      label: drug.levelName ? `${drug.drugName}·${drug.levelName}` : text(drug.drugName),
      diseases: drug.disease ? [drug.disease] : [],
      resistant
    });
  }
  return [...merged.values()];
}

/** 获益用药单元格：药名 tag 串（证据等级做后缀，悬停显示证据癌种） */
function renderDrugTags(resistant: boolean) {
  return (row: Api.Report.PreviewVariant) => {
    const items = drugTagItems(row, resistant);
    if (!items.length) {
      return '/';
    }
    return h(
      NSpace,
      { size: 4 },
      {
        default: () =>
          items.map(item =>
            h(
              NTag,
              {
                key: item.key,
                size: 'small',
                type: resistant ? 'error' : 'info',
                title: item.diseases.length ? `证据癌种：${item.diseases.join('、')}` : undefined
              },
              { default: () => item.label }
            )
          )
      }
    );
  };
}

/** 相关突变（命中节点 + 一层父级；空则用改靶父级名兜底） */
function relatedText(row: Api.Report.PreviewVariant): string {
  if (row.relatedMutations?.length) {
    return row.relatedMutations.join('、');
  }
  if (row.parentMutationNames?.length) {
    return row.parentMutationNames.filter(Boolean).join('、');
  }
  return '/';
}

/** 体细胞位点表（选项 A：列到「获益用药 + 证据等级」，药名用 tag 串） */
const somaticColumns = computed(() => [
  { title: '基因', key: 'gene', width: 90, render: (row: Api.Report.PreviewVariant) => text(row.gene) },
  { title: '位点', key: 'variant', width: 150, render: (row: Api.Report.PreviewVariant) => text(row.variant) },
  { title: '原始位点', key: 'oriVariant', width: 150, render: (row: Api.Report.PreviewVariant) => text(row.oriVariant) },
  { title: '突变类型', key: 'mutationType', width: 100, render: (row: Api.Report.PreviewVariant) => text(row.mutationType) },
  { title: '丰度', key: 'abundance', width: 100, render: (row: Api.Report.PreviewVariant) => text(row.abundanceText ?? row.frequency) },
  { title: '深度', key: 'depth', width: 80, render: (row: Api.Report.PreviewVariant) => text(row.depth) },
  { title: '位点等级', key: 'variationClass', width: 90, render: (row: Api.Report.PreviewVariant) => text(row.variationClass) },
  {
    title: '匹配状态',
    key: 'matchStatus',
    width: 95,
    render: (row: Api.Report.PreviewVariant) => {
      const tag = matchTag(row.matchStatus);
      return h(NTag, { size: 'small', type: tag.type }, { default: () => tag.label });
    }
  },
  { title: '获益用药', key: 'drugs', minWidth: 200, render: renderDrugTags(false) },
  { title: '耐药用药', key: 'resistantDrugs', minWidth: 140, render: renderDrugTags(true) },
  { title: '相关突变', key: 'related', minWidth: 180, render: (row: Api.Report.PreviewVariant) => relatedText(row) },
  { title: '命中节点', key: 'matchedNode', minWidth: 130, render: (row: Api.Report.PreviewVariant) => text(row.matchedNode) }
]);

/** 胚系位点表（临床意义五级 + 获益用药） */
const germlineColumns = computed(() => [
  { title: '基因', key: 'gene', width: 90, render: (row: Api.Report.PreviewVariant) => text(row.gene) },
  { title: '位点', key: 'variant', width: 160, render: (row: Api.Report.PreviewVariant) => text(row.variant) },
  { title: '原始位点', key: 'oriVariant', width: 160, render: (row: Api.Report.PreviewVariant) => text(row.oriVariant) },
  { title: '突变类型', key: 'mutationType', width: 100, render: (row: Api.Report.PreviewVariant) => text(row.mutationType) },
  { title: '合子', key: 'zygosity', width: 80, render: (row: Api.Report.PreviewVariant) => text(row.zygosity) },
  {
    title: '临床意义',
    key: 'clinicalSignificanceLabel',
    width: 120,
    render: (row: Api.Report.PreviewVariant) => text(row.clinicalSignificanceLabel)
  },
  {
    title: '匹配状态',
    key: 'matchStatus',
    width: 95,
    render: (row: Api.Report.PreviewVariant) => {
      const tag = matchTag(row.matchStatus);
      return h(NTag, { size: 'small', type: tag.type }, { default: () => tag.label });
    }
  },
  { title: '获益用药', key: 'drugs', minWidth: 200, render: renderDrugTags(false) },
  { title: '耐药用药', key: 'resistantDrugs', minWidth: 140, render: renderDrugTags(true) },
  { title: '相关突变', key: 'related', minWidth: 180, render: (row: Api.Report.PreviewVariant) => relatedText(row) }
]);

/* ---------------- 加载 ---------------- */

async function loadAll() {
  if (!props.analysisId || !props.reportId) {
    return;
  }
  loading.value = true;
  try {
    const [ctx, prev, files] = await Promise.all([
      fetchGetInterpretationContext({ reportId: props.reportId, analysisId: props.analysisId }),
      fetchBuildInterpretationPreview({ analysisId: props.analysisId, reportId: props.reportId }),
      fetchGetInterpretationFileList({ analysisId: props.analysisId, pageNum: 1, pageSize: 1 })
    ]);
    if (!ctx.error) {
      context.value = ctx.data;
    }
    if (!prev.error) {
      preview.value = prev.data?.data ?? null;
      previewSource.value = prev.data?.source ?? '';
    }
    if (!files.error) {
      fileCount.value = files.data?.total ?? null;
    }
  } finally {
    loading.value = false;
  }
}

/** 下载已生成的报告文件（接口要带令牌，走后端 download-filename 头给的文件名） */
function handleDownload() {
  zip(fetchGetReportFileUrl(props.reportId, props.analysisId), '');
}

async function handleApprove() {
  acting.value = true;
  try {
    const { error } = await fetchApproveReview({ analysisId: props.analysisId, reportId: props.reportId });
    if (!error) {
      window.$message?.success('已审核通过');
      await loadAll();
      emit('changed');
    }
  } finally {
    acting.value = false;
  }
}

function openReject() {
  rejectReason.value = '';
  rejectVisible.value = true;
}

async function confirmReject() {
  const reason = rejectReason.value.trim();
  if (!reason) {
    window.$message?.warning('请填写驳回原因');
    return;
  }
  acting.value = true;
  try {
    const { error } = await fetchRejectReview({
      analysisId: props.analysisId,
      reportId: props.reportId,
      reason
    });
    if (!error) {
      window.$message?.success('已驳回');
      rejectVisible.value = false;
      await loadAll();
      emit('changed');
    }
  } finally {
    acting.value = false;
  }
}

watch(() => [props.analysisId, props.reportId], loadAll, { immediate: true });
</script>

<template>
  <NSpin :show="loading">
    <div class="flex-col-stretch gap-12px">
      <!-- ① 报告基础信息 -->
      <NCard :bordered="false" size="small" class="card-wrapper">
        <template #header>
          <div class="flex items-center gap-8px">
            <span>报告基础信息</span>
            <NTag v-if="report" size="small" :type="statusTag.type">{{ statusTag.label }}</NTag>
          </div>
        </template>

        <NDescriptions :column="3" label-placement="left" size="small" bordered>
          <NDescriptionsItem label="报告ID">{{ text(report?.reportId) }}</NDescriptionsItem>
          <NDescriptionsItem label="样本编号">{{ text(report?.subbarcode ?? sampleInfo.sampleCode) }}</NDescriptionsItem>
          <NDescriptionsItem label="患者编号">{{ text(sampleInfo.participantNumber) }}</NDescriptionsItem>
          <NDescriptionsItem label="产品">{{ text(report?.productBio) }}</NDescriptionsItem>
          <NDescriptionsItem label="解读癌种">{{ text(report?.analysisDisease ?? sampleInfo.disease) }}</NDescriptionsItem>
          <NDescriptionsItem label="模板">{{ text(report?.template) }}</NDescriptionsItem>
          <NDescriptionsItem label="报告日期">{{ text(reportInfo.reportDate) }}</NDescriptionsItem>
          <NDescriptionsItem label="解读人">{{ text(reportInfo.analyzer) }}</NDescriptionsItem>
          <NDescriptionsItem label="标本类型">{{ text(reportInfo.specimentType ?? sampleInfo.sampleType) }}</NDescriptionsItem>
          <NDescriptionsItem label="研究中心">{{ text(sampleInfo.researchCenterName) }}</NDescriptionsItem>
          <NDescriptionsItem label="性别">{{ text(sampleInfo.gender) }}</NDescriptionsItem>
          <NDescriptionsItem label="出生年份">{{ text(sampleInfo.birthYear) }}</NDescriptionsItem>
          <NDescriptionsItem label="收样日期">{{ text(sampleInfo.receivedDate) }}</NDescriptionsItem>
          <NDescriptionsItem label="采样日期">{{ text(sampleInfo.tissueCollectionDate) }}</NDescriptionsItem>
          <NDescriptionsItem label="集群文件个数">{{ fileCount ?? '/' }}</NDescriptionsItem>
          <NDescriptionsItem label="报告产出人">{{ text(report?.reportGeneratedBy) }}</NDescriptionsItem>
          <NDescriptionsItem label="生成时间">{{ text(report?.reportGeneratedAt) }}</NDescriptionsItem>
          <NDescriptionsItem label="审核人">{{ text(report?.reportCheckedBy) }}</NDescriptionsItem>
          <NDescriptionsItem label="审核时间">{{ text(report?.reportCheckedAt) }}</NDescriptionsItem>
          <NDescriptionsItem label="报告名" :span="2">{{ reportName }}</NDescriptionsItem>
        </NDescriptions>

        <NAlert v-if="report?.comment" type="warning" :bordered="false" class="mt-12px">
          上次备注/驳回原因：{{ report.comment }}
        </NAlert>
      </NCard>

      <!-- ② 位点解读信息（全部来自报告 JSON 制品） -->
      <NCard :bordered="false" size="small" class="card-wrapper">
        <template #header>
          <div class="flex items-center gap-8px">
            <span>位点解读信息</span>
            <NTag size="small" :type="previewSource === 'artifact' ? 'success' : 'warning'">
              {{ previewSource === 'artifact' ? '来自已生成报告' : '实时组装（未生成）' }}
            </NTag>
            <NTag size="small" type="info">schema {{ preview?.schemaVersion ?? '-' }}</NTag>
          </div>
        </template>

        <NAlert v-if="preview?.warnings?.length" type="warning" :bordered="false" class="mb-12px">
          <div v-for="item in preview.warnings" :key="item">{{ item }}</div>
        </NAlert>

        <!-- 体细胞变异解析 -->
        <div class="mb-8px flex flex-wrap items-center gap-x-12px gap-y-8px">
          <span class="font-medium">体细胞变异解析</span>
          <NTag v-for="tag in somaticTags" :key="tag.key" size="small">{{ tag.label }}</NTag>
          <span class="text-12px op-60">
            报出 {{ somaticSummary?.reportedCount ?? 0 }} · 已匹配 {{ somaticSummary?.matchedCount ?? 0 }} ·
            无证据 {{ somaticSummary?.unmatchedCount ?? 0 }}
          </span>
        </div>
        <NDataTable
          :columns="somaticColumns"
          :data="somaticRows"
          :row-key="variantRowKey"
          :scroll-x="1500"
          size="small"
        />
        <NDivider class="my-16px!" />

        <!-- 肿瘤遗传风险（胚系） -->
        <div class="mb-8px flex flex-wrap items-center gap-x-12px gap-y-8px">
          <span class="font-medium">肿瘤遗传风险（胚系）</span>
          <NTag v-for="tag in germlineTags" :key="tag.key" size="small">{{ tag.label }}</NTag>
          <span class="text-12px op-60">
            报出 {{ germlineSummary?.reportedCount ?? 0 }} · 已匹配 {{ germlineSummary?.matchedCount ?? 0 }} ·
            无证据 {{ germlineSummary?.unmatchedCount ?? 0 }}
          </span>
        </div>
        <NDataTable
          :columns="germlineColumns"
          :data="germlineRows"
          :row-key="variantRowKey"
          :scroll-x="1350"
          size="small"
        />
      </NCard>

      <!-- ③ 报告文件 -->
      <NCard :bordered="false" size="small" class="card-wrapper">
        <template #header>
          <div class="flex items-center gap-8px">
            <span>报告文件</span>
            <NTag size="small" :type="hasReportFile ? 'success' : 'warning'">
              {{ hasReportFile ? '已生成' : '尚未生成' }}
            </NTag>
          </div>
        </template>
        <NSpace align="center">
          <NButton size="small" type="primary" ghost :disabled="!hasReportFile" @click="handleDownload">
            下载报告文件
          </NButton>
          <span class="text-12px op-60">{{ hasReportFile ? reportName : '还没生成成品文件，请先在「报告预览」生成报告。' }}</span>
        </NSpace>
      </NCard>

      <!-- ④ 审核 -->
      <NCard :bordered="false" size="small" class="card-wrapper">
        <template #header>
          <div class="flex items-center gap-8px">
            <span>审核</span>
            <NTag v-if="report" size="small" :type="statusTag.type">{{ statusTag.label }}</NTag>
          </div>
        </template>
        <NSpace v-if="canReview" justify="end" class="mb-8px">
          <NButton type="error" ghost :loading="acting" @click="openReject">驳回</NButton>
          <NButton type="primary" :loading="acting" @click="handleApprove">审核通过</NButton>
        </NSpace>
        <NTag v-if="reviewHint" type="warning">{{ reviewHint }}</NTag>
        <span v-else class="text-12px op-60">核对位点与用药无误后，点「审核通过」；有问题点「驳回」并填写原因。</span>
      </NCard>
    </div>

    <NModal v-model:show="rejectVisible" preset="card" title="驳回报告" class="w-50vw max-w-560px" :bordered="false">
      <NInput v-model:value="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因（必填）" />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="rejectVisible = false">取消</NButton>
          <NButton type="error" :loading="acting" @click="confirmReject">确认驳回</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpin>
</template>

<style scoped></style>
