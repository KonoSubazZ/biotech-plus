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

/** en7 药物等级分组键与中文标签（1-4 获益 A-D，5-8 耐药 A-D） */
const GROUP_KEYS = [
  'drugsA',
  'drugsB',
  'drugsC',
  'drugsD',
  'resistantDrugsA',
  'resistantDrugsB',
  'resistantDrugsC',
  'resistantDrugsD'
];

const GROUP_LABELS: Record<string, string> = {
  drugsA: '获益A级',
  drugsB: '获益B级',
  drugsC: '获益C级',
  drugsD: '获益D级',
  resistantDrugsA: '耐药A级',
  resistantDrugsB: '耐药B级',
  resistantDrugsC: '耐药C级',
  resistantDrugsD: '耐药D级'
};

/** 五级临床意义（固定口径，见设计书 4.8） */
const SIGNIFICANCE_OPTIONS = [
  { label: '1 致病', value: 1 },
  { label: '2 可能致病', value: 2 },
  { label: '3 未知临床意义', value: 3 },
  { label: '4 可能良性', value: 4 },
  { label: '5 良性', value: 5 }
];

const editing = ref(false);
const preview = ref<Api.Report.InterpretationPreview | null>(null);
const jsonVisible = ref(false);
/** 正在保存的行（避免连点） */
const savingId = ref<number | null>(null);
/** 详情弹窗（原来表格展开行里的内容挪进来） */
const detailRow = ref<Api.Report.PreviewVariant | null>(null);
const detailVisible = ref(false);
/** 改靶弹窗（胚系：人工父级ID；体细胞暂为占位） */
const targetRow = ref<Api.Report.PreviewVariant | null>(null);
const targetVisible = ref(false);
const targetValue = ref<number | null>(null);

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

/** 位点表前四列（基因 / 变异 / 类型 / 原始位点） */
function baseColumns(): NaiveUI.TableColumn<Api.Report.PreviewVariant>[] {
  return [
    { key: 'gene', title: '基因', align: 'center', width: 110, render: row => row.gene ?? '-' },
    { key: 'variant', title: '变异', align: 'center', width: 150, render: row => row.variant ?? '-' },
    // 类型：体系|变异类别|核酸类型（核酸类型只有融合有，由后端 typeText 组装）
    { key: 'mutationType', title: '类型', align: 'center', width: 150, render: row => row.typeText ?? '-' },
    {
      key: 'oriVariant',
      title: '原始位点',
      align: 'left',
      minWidth: 220,
      ellipsis: { tooltip: true },
      render: row => row.oriVariant ?? '-'
    }
  ];
}

/** 变异类型列：VEP ExonicFunc 中文（错义突变/移码突变…，后端 variantTypeText）；插在「原始位点」之后 */
function variantTypeColumn(): NaiveUI.TableColumn<Api.Report.PreviewVariant> {
  return {
    key: 'variantType',
    title: '变异类型',
    align: 'center',
    width: 130,
    render: row => row.variantTypeText ?? '-'
  };
}

/** 丰度/reads 列：DNA → 45.47%；RNA 融合 → reads 数（无单位）；扩增/缺失 → 拷贝数 */
function abundanceColumn(): NaiveUI.TableColumn<Api.Report.PreviewVariant> {
  return { key: 'frequency', title: '丰度/reads', align: 'center', width: 120, render: row => row.abundanceText ?? '-' };
}

/** 体细胞列 = 公共列 + 变异类型 + 丰度/reads + 位点等级 + 操作 */
function buildSomaticColumns(): NaiveUI.TableColumn<Api.Report.PreviewVariant>[] {
  return [...baseColumns(), variantTypeColumn(), abundanceColumn(), variationLevelColumn(), operateColumn()];
}

/** 胚系列 = 公共列 + 合子/临床意义/文件判定 + 操作 */
function buildGermlineColumns(): NaiveUI.TableColumn<Api.Report.PreviewVariant>[] {
  return [
    ...baseColumns(),
    abundanceColumn(),
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
    variationLevelColumn(),
    operateColumn()
  ];
}

/** 位点等级列：I / II / III（来自匹配结果的 variationClass，去掉「类」字） */
function variationLevelColumn(): NaiveUI.TableColumn<Api.Report.PreviewVariant> {
  return {
    key: 'variationLevel',
    title: '位点等级',
    align: 'center',
    width: 100,
    render: row => {
      const short = row.variationClass ? row.variationClass.replace('类', '') : null;
      // 等级是「类别」不是「状态」：按配色契约用中性色，不借用绿/黄/红
      return short ? <NTag size="small">{short}</NTag> : <>-</>;
    }
  };
}

/**
 * 操作列：详情（弹窗展示原来的展开内容）/ 匹配(占位) / 改靶
 * <p>
 * 匹配、体细胞改靶先占位（提示待实现）；胚系改靶沿用已实现的接口，点开弹窗填人工父级ID。
 */
function operateColumn(): NaiveUI.TableColumn<Api.Report.PreviewVariant> {
  return {
    key: 'operate',
    title: '操作',
    align: 'center',
    width: 190,
    render: row => (
      <div class="flex items-center justify-center gap-10px">
        <NButton text type="primary" size="small" onClick={() => openDetail(row)}>
          详情
        </NButton>
        <NButton text type="primary" size="small" onClick={() => showTodo('匹配')}>
          匹配
        </NButton>
        <NButton
          text
          type="primary"
          size="small"
          onClick={() => (row.sourceType === 'CR_ALL' ? openTarget(row) : showTodo('改靶'))}
        >
          改靶
        </NButton>
      </div>
    )
  };
}

/** 详情弹窗：位点 + 命中信息 + 证据明细（原来的展开行内容） */
function openDetail(row: Api.Report.PreviewVariant) {
  detailRow.value = row;
  detailVisible.value = true;
}

/** 改靶弹窗（胚系：人工父级节点ID） */
function openTarget(row: Api.Report.PreviewVariant) {
  targetRow.value = row;
  targetValue.value = row.parentMutationId ?? null;
  targetVisible.value = true;
}

async function confirmTarget() {
  if (!targetRow.value) {
    return;
  }
  await saveTarget(targetRow.value, targetValue.value);
  targetVisible.value = false;
}

/** 尚未实现的操作统一提示（匹配 / 体细胞改靶） */
function showTodo(name: string) {
  window.$message?.info(`${name}功能待实现`);
}

/** 详情用：取该位点非空的等级分组 key（模板里 v-for 用） */
function groupItemsOf(row: Api.Report.PreviewVariant | null): string[] {
  const groups = row?.drugGroups ?? {};
  return GROUP_KEYS.filter(key => groups[key]);
}

/**
 * 详情用：关联突变 tag = 命中节点自身 + 一层父级，**每个都带基因前缀**（如 `KIT V559D`）。
 * <p>
 * 末尾补一条 `基因 位点`：精确命中时它和第一条节点相同（节点名就是位点名）→ 去重不重复；
 * 未收录（节点列表为空）或兜底命中父级节点（如 Inactive Mutation）时它才有意义。
 */
function relatedMutationTags(row: Api.Report.PreviewVariant | null): string[] {
  const gene = row?.gene ?? '';
  const tags = (row?.relatedMutations ?? []).map(name => [gene, name].filter(Boolean).join(' '));
  const locus = [row?.gene, row?.variant].filter(Boolean).join(' ');
  if (locus && !tags.includes(locus)) {
    tags.push(locus);
  }
  return tags;
}

/** 详情用：节点 tag 文本（关联突变 / 证据行命中节点共用，带基因前缀） */
function nodeTagText(gene: string | null | undefined, nodeName: string | null | undefined): string | null {
  const text = [gene, nodeName].filter(Boolean).join(' ');
  return text || null;
}

/**
 * 详情用：药物信息 tag（获益 / 耐药分开，各自按去重药名计数，只出有数据的等级）
 *
 * @returns [{ key, label, resistant }]，label 形如 `获益D级(4)` / `耐药A级(1)`
 */
function drugLevelTags(row: Api.Report.PreviewVariant | null) {
  const buckets = new Map<string, Set<string>>();
  for (const drug of row?.drugMatch ?? []) {
    if (!drug.levelName) {
      continue;
    }
    const key = `${drug.relation === 'RESISTANT' ? 'R' : 'B'}${drug.levelName}`;
    const names = buckets.get(key) ?? new Set<string>();
    if (drug.drugName) {
      names.add(drug.drugName);
    }
    buckets.set(key, names);
  }
  const tags: { key: string; label: string; resistant: boolean }[] = [];
  for (const prefix of ['R', 'B']) {
    for (const level of ['A', 'B', 'C', 'D']) {
      const count = buckets.get(`${prefix}${level}`)?.size ?? 0;
      if (count === 0) {
        continue;
      }
      tags.push({
        key: `${prefix}${level}`,
        label: `${prefix === 'R' ? '耐药' : '获益'}${level}级(${count})`,
        resistant: prefix === 'R'
      });
    }
  }
  return tags;
}

/**
 * 详情用：突变说明正文（对齐 en7 `getVarDrugNote` 的拼装）。
 * <p>
 * en7 是 `mutDesc + variantDescription`：HGVS 译文 + 位点说明，
 * 其中位点说明取 NKB `gene_variant_description`，**NKB 没有时才用四段兜底文案**（`description`）。
 */
function mutationTexts(row: Api.Report.PreviewVariant | null): string[] {
  return [row?.mutationExplanation, row?.variantDescription || row?.description].filter(Boolean) as string[];
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

    <!-- 详情：标签行（关联突变 / 药物信息 / 位点分级 / 证据条数）+ 说明 + 证据明细全量 -->
    <NModal
      v-model:show="detailVisible"
      preset="card"
      :title="detailRow ? `位点详情：${detailRow.gene ?? '-'} ${detailRow.variant ?? '-'}` : '位点详情'"
      class="w-80vw max-w-1000px"
      :bordered="false"
    >
      <div v-if="detailRow" class="max-h-70vh overflow-auto">
        <div class="mb-10px flex flex-wrap items-center gap-x-16px gap-y-8px text-13px">
          <div class="flex flex-wrap items-center gap-6px">
            <span class="op-60">关联突变</span>
            <NTag
              v-for="name in relatedMutationTags(detailRow)"
              :key="name"
              size="small"
              type="info"
              :bordered="false"
            >
              {{ name }}
            </NTag>
          </div>
          <div v-if="drugLevelTags(detailRow).length" class="flex flex-wrap items-center gap-6px">
            <span class="op-60">药物信息</span>
            <NTag
              v-for="tag in drugLevelTags(detailRow)"
              :key="tag.key"
              size="small"
              :type="tag.resistant ? 'error' : 'success'"
            >
              {{ tag.label }}
            </NTag>
          </div>
          <div class="flex flex-wrap items-center gap-6px">
            <span class="op-60">位点分级</span>
            <NTag size="small">{{ detailRow.variationClass ?? '-' }}</NTag>
          </div>
          <span v-if="detailRow.drugMatch?.length" class="op-60">证据 {{ detailRow.drugMatch.length }} 条</span>
        </div>

        <!-- 三段说明：基因说明 / 信号通路说明 / 突变说明（en7 拼装：HGVS 译文 + 位点说明，NKB 优先、四段兜底） -->
        <div
          v-if="detailRow.geneDescription || detailRow.pathwayDescription || mutationTexts(detailRow).length"
          class="mb-10px flex-col gap-6px text-12px"
        >
          <div v-if="detailRow.geneDescription" class="rounded bg-#f5f7fa p-10px">
            <span class="font-medium">基因说明</span>
            <div class="mt-4px leading-20px op-80">{{ detailRow.geneDescription }}</div>
          </div>
          <div v-if="detailRow.pathwayDescription" class="rounded bg-#f5f7fa p-10px">
            <span class="font-medium">信号通路说明</span>
            <div class="mt-4px leading-20px op-80">{{ detailRow.pathwayDescription }}</div>
          </div>
          <div v-if="mutationTexts(detailRow).length" class="rounded bg-#f5f7fa p-10px">
            <span class="font-medium">突变说明</span>
            <div
              v-for="(text, index) in mutationTexts(detailRow)"
              :key="index"
              class="mt-4px leading-20px op-80"
            >
              {{ text }}
            </div>
          </div>
        </div>

        <div v-if="groupItemsOf(detailRow).length" class="mb-10px flex flex-wrap items-center gap-8px text-12px">
          <span v-for="key in groupItemsOf(detailRow)" :key="key" class="rounded bg-#f5f7fa px-8px py-2px">
            {{ GROUP_LABELS[key] }}：{{ detailRow.drugGroups?.[key] }}
          </span>
        </div>

        <div v-if="!detailRow.drugMatch?.length" class="text-13px op-60">
          该位点在当前癌种范围（本癌种 + 祖先 + 子孙）内没有可用药物证据
        </div>
        <div v-else class="flex-col gap-8px">
          <div v-for="(drug, index) in detailRow.drugMatch" :key="index" class="rounded bg-#f5f7fa p-10px">
            <div class="mb-4px flex flex-wrap items-center gap-8px">
              <NTag size="small" :type="drug.relation === 'RESISTANT' ? 'error' : 'success'">
                {{ drug.levelName ?? '-' }} 级
              </NTag>
              <span class="font-medium">{{ drug.drugName ?? '-' }}</span>
              <span class="text-12px op-60">{{ drug.disease ?? '-' }}</span>
              <NTag v-if="drug.evidencePhase" size="small">{{ drug.evidencePhase }}</NTag>
              <NTag v-if="drug.relationship" size="small" type="info">{{ drug.relationship }}</NTag>
              <NTag v-if="drug.fromOtherCancer" size="small" type="warning">其他癌种获批</NTag>
              <NTag
                v-if="nodeTagText(detailRow.gene, drug.nodeName)"
                size="small"
                type="info"
                :bordered="false"
              >
                {{ nodeTagText(detailRow.gene, drug.nodeName) }}
              </NTag>
            </div>
            <div class="text-12px leading-20px op-80">{{ drug.annotation ?? drug.comment ?? '-' }}</div>
          </div>
        </div>
      </div>
    </NModal>

    <!-- 改靶（胚系）：人工父级节点ID -->
    <NModal
      v-model:show="targetVisible"
      preset="card"
      title="改靶（人工父级节点）"
      class="w-40vw max-w-520px"
      :bordered="false"
    >
      <NForm label-placement="left" :label-width="110">
        <NFormItem label="人工父级ID">
          <NInputNumber
            v-model:value="targetValue"
            class="w-full"
            placeholder="填 NKB gene_variant_id（留空=取消改靶）"
            :disabled="!canEdit"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="targetVisible = false">取消</NButton>
          <NButton
            type="primary"
            :disabled="!canEdit"
            :loading="savingId === targetRow?.sourceId"
            @click="confirmTarget"
          >
            保存
          </NButton>
        </NSpace>
      </template>
    </NModal>

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
