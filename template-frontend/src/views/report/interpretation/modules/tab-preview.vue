<script setup lang="tsx">
import { computed, ref, watch } from 'vue';
import { NButton, NSelect, NTag } from 'naive-ui';
import {
  fetchBuildInterpretationPreview,
  fetchGenerateReport,
  fetchGetReportFileUrl,
  fetchParentCandidates,
  fetchSubmitReview,
  fetchUpdateGermlineSignificance,
  fetchUpdateVariantTarget
} from '@/service/api/report/interpretation';
import ReportGenerateParams from './report-generate-params.vue';
import { gradeCountTags } from './grade-count';
import { useAuth } from '@/hooks/business/auth';
import { useDownload } from '@/hooks/business/download';

defineOptions({
  name: 'InterpretationTabPreview'
});

interface Props {
  analysisId: number;
  reportId: number;
  /** 报告当前状态（详情页 context 给的，用来判断能不能提交审核；可空 = 还没加载出来） */
  reportStatus?: string | null;
}

const props = defineProps<Props>();

interface Emits {
  /** 状态被本页改过（如提交审核），通知详情页刷新报告头 */
  (e: 'changed'): void;
}

const emit = defineEmits<Emits>();

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
/** 改靶弹窗（人工父级节点，可多个；父级来自知识库） */
const targetRow = ref<Api.Report.PreviewVariant | null>(null);
const targetVisible = ref(false);
const targetIds = ref<number[]>([]);
/** 本次弹窗内已加载的知识库候选（供 NSelect 回显与选择） */
const candidateNodes = ref<Api.Report.NkbVariantNode[]>([]);
const candidateLoading = ref(false);

const canEdit = computed(() => hasAuth('report:interpretation:edit'));

/** 顶部上下文一行（sampleInfo 是 Map，取值统一转字符串） */
const contextLine = computed(() => {
  const info = preview.value?.sampleInfo ?? {};
  const part = (key: string) => (info[key] === null || info[key] === undefined ? '-' : String(info[key]));
  return `样本 ${part('sampleCode')} · 癌种 ${part('disease')} · 性别 ${part('gender')}`;
});

/** JSON 原文（放 script 里，模板只负责渲染字符串） */
const previewJson = computed(() => JSON.stringify(preview.value ?? {}, null, 2));

/** 预览数据来源：artifact = 后端读「已生成的 JSON 制品」，realtime = 实时重新匹配组装 */
const previewSource = ref<'artifact' | 'realtime' | ''>('');
const previewArtifactTime = ref<string | null>(null);

/** 报告生成参数小组件（模板 / 解读癌种 / 报告日期 / 解读人，各自带默认值） */
const generateParamsRef = ref<InstanceType<typeof ReportGenerateParams> | null>(null);

async function loadPreview() {
  if (!props.analysisId || !props.reportId) {
    return;
  }
  editing.value = true;
  try {
    const { data, error } = await fetchBuildInterpretationPreview({
      analysisId: props.analysisId,
      reportId: props.reportId,
      ...(generateParamsRef.value?.params ?? {})
    });
    if (!error) {
      preview.value = data?.data ?? null;
      previewSource.value = data?.source ?? '';
      previewArtifactTime.value = data?.artifactGeneratedAt ?? null;
    }
  } finally {
    editing.value = false;
  }
}

/** 出预览（生成参数由小组件提供，组件自己在挂载时加载候选并定默认值） */
async function initPreview() {
  await loadPreview();
}

const { zip } = useDownload();

/** 正在生成（按钮 loading） */
const generating = ref(false);

/** 报告文件是否已有：预览来源是已生成制品时说明成品文件在（下载按钮据此出现） */
const hasReportFile = computed(() => previewSource.value === 'artifact');

/**
 * 正式生成报告：把「报告生成参数」一起带给后端，后端写 JSON/DOCX 到固定输出目录并登记报告。
 * 成功后重新预览 —— 来源变成「已生成报告」，参数随之锁定，并出现下载入口。
 */
async function handleGenerate() {
  generating.value = true;
  try {
    const { data, error } = await fetchGenerateReport({
      analysisId: props.analysisId,
      reportId: props.reportId,
      ...(generateParamsRef.value?.params ?? {})
    });
    if (!error) {
      const fileName = data?.docxFileName as string | undefined;
      window.$message?.success(fileName ? `报告已生成：${fileName}` : '报告已生成');
      await loadPreview();
    }
  } finally {
    generating.value = false;
  }
}

/**
 * 下载已生成的报告文件：接口要带令牌，走通用下载 hooks。
 * 文件名不在这里拼（后端 download-filename 头给的真实文件名），传空串让 hooks 用响应头兜底。
 */
function handleDownload() {
  zip(fetchGetReportFileUrl(props.reportId, props.analysisId), '');
}

/** 允许提交审核的报告状态（与后端 InterpretationReviewService.SUBMITTABLE_STATUS 保持一致） */
const SUBMITTABLE_STATUS = ['INTERPRETING', 'REJECTED'];

/** 提交审核按钮：已生成报告（有成品文件）+ 状态允许提交时才出现 */
const canSubmitReview = computed(() => hasReportFile.value && SUBMITTABLE_STATUS.includes(props.reportStatus ?? ''));

const submitting = ref(false);

/**
 * 提交审核：后端在同一事务里冻结本次解读的位点匹配历史（history_*）并把报告置为待审核。
 * 参数与生成时同一套（预览页在已生成状态下是锁定回填的），否则冻出的历史与报告内容对不上。
 */
async function handleSubmitReview() {
  submitting.value = true;
  try {
    const { error } = await fetchSubmitReview({
      analysisId: props.analysisId,
      reportId: props.reportId,
      ...(generateParamsRef.value?.params ?? {})
    });
    if (!error) {
      window.$message?.success('已提交审核，请到「审核报告」处理');
      emit('changed');
    }
  } finally {
    submitting.value = false;
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

async function saveTarget(row: Api.Report.PreviewVariant, parentMutationIds: number[]): Promise<boolean> {
  savingId.value = row.sourceId;
  try {
    const { error } = await fetchUpdateVariantTarget({
      analysisId: props.analysisId,
      reportId: props.reportId,
      sourceType: row.sourceType ?? 'SNP_INDEL',
      sourceId: row.sourceId ?? 0,
      parentMutationIds
    });
    if (error) {
      return false;
    }
    window.$message?.success(parentMutationIds.length ? '已改靶' : '已取消改靶');
    await loadPreview();
    return true;
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

/** 合子列：CR_ALL 的 Zygosity（纯合/杂合）；只在胚系表用 */
function zygosityColumn(): NaiveUI.TableColumn<Api.Report.PreviewVariant> {
  return {
    key: 'zygosity',
    title: '纯合/杂合',
    align: 'center',
    width: 110,
    render: row => row.zygosity ?? '-'
  };
}

/** 胚系列 = 公共列 + 变异类型 + 纯合/杂合 + 临床意义 + 操作（丰度/reads、文件判定、位点等级不展示） */
function buildGermlineColumns(): NaiveUI.TableColumn<Api.Report.PreviewVariant>[] {
  return [...baseColumns(), variantTypeColumn(), zygosityColumn(), significanceColumn(), operateColumn()];
}

/** 临床意义列：五级人工确认（1 致病 ~ 5 良性），保存后重新预览 */
function significanceColumn(): NaiveUI.TableColumn<Api.Report.PreviewVariant> {
  return {
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
  };
}

/** 位点等级列：I / II / III（来自匹配结果的 variationClass，去掉「类」字）；只在体细胞表用 */
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
 * 操作列：详情（弹窗展示原来的展开内容）/ 匹配(占位) / 改靶（体细胞与胚系共用：选知识库父级）
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
        <NButton text type="primary" size="small" onClick={() => openTarget(row)}>
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

/**
 * 改靶弹窗：人工指定一个/多个知识库父级节点（远程搜索 NKB，可多选）
 * <p>
 * 父级由知识库查到（`/parent-candidates`）；留空 = 取消改靶。保存后重新预览，走新的匹配键。
 */
function openTarget(row: Api.Report.PreviewVariant) {
  targetRow.value = row;
  targetIds.value = [...(row.parentMutationIds ?? [])];
  targetVisible.value = true;
  searchCandidates(row.gene ?? '');
}

/** 远程搜索知识库父级候选（按位点基因预载，改关键词即重搜） */
async function searchCandidates(keyword: string) {
  const query = keyword.trim();
  candidateNodes.value = [];
  if (!query) {
    return;
  }
  candidateLoading.value = true;
  try {
    const { data, error } = await fetchParentCandidates(query);
    if (!error) {
      candidateNodes.value = data ?? [];
    }
  } finally {
    candidateLoading.value = false;
  }
}

/** 候选选项：已选中的父级必须出现在选项里（且带名字），否则 NSelect 回显不出来 */
const targetOptions = computed(() => {
  // 没有 mutationId 的知识库节点不能当改靶目标，直接不进选项：
  // 既避免把 null 塞进 NSelect 的 value（SelectMixedOption 的 value 只接受 string | number，
  // 实测 vue-tsc 会报 TS2322），也免得用户选到一个没有 id 的项
  const options = candidateNodes.value.flatMap(node =>
    node.mutationId === null
      ? []
      : [{ label: `${node.gene ?? '-'} ${node.variantName ?? '-'}`, value: node.mutationId }]
  );
  const ids = targetRow.value?.parentMutationIds ?? [];
  const names = targetRow.value?.parentMutationNames ?? [];
  const gene = targetRow.value?.gene ?? '';
  ids.forEach((id, index) => {
    if (!options.some(option => option.value === id)) {
      options.push({ label: [gene, names[index] ?? `节点 ${id}`].filter(Boolean).join(' '), value: id });
    }
  });
  return options;
});

async function confirmTarget() {
  if (!targetRow.value) {
    return;
  }
  // 保存失败（如「改靶后无药物信息」）时保留弹窗，便于调整父级
  if (await saveTarget(targetRow.value, targetIds.value)) {
    targetVisible.value = false;
  }
}

/** 尚未实现的操作统一提示（匹配） */
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
 * 证据「循证医学信息」：按分期取说明（对齐 en7 getVarDrugNote 的取数口径，后端已填好）
 * 指南推荐(23) → 指南说明；获批上市(24) → 批准说明；其余（含耐药）→ annotation_chinese
 * <p>
 * ⚠️ **不要兜底到 `comment`**：那是 NKB 内部维护备注（「新增 NCT… LSF 20210129」「修改证据等级为…」），
 * 报告里从不输出，en7 的取数 SQL 也从不 select 它。
 * <p>
 * 都没有时返回空串（模板里 `v-if` 直接不渲染说明行——en7 报告里这类证据只出现在等级药名串里）
 */
function drugDescription(drug: Api.Report.PreviewDrug): string {
  return drug.guidelineDescription ?? drug.approvalDescription ?? drug.annotation ?? '';
}

/** 该证据是否带说明（决定是否打「说明」tag、是否渲染说明行） */
function hasDescription(drug: Api.Report.PreviewDrug): boolean {
  return drugDescription(drug) !== '';
}

/** 该证据的注释是否挂着临床试验（决定是否打「临床试验」tag） */
function hasClinicalTrial(drug: Api.Report.PreviewDrug): boolean {
  return (detailRow.value?.trials ?? []).some(trial => trial.annotationId != null && trial.annotationId === drug.annotationId);
}

/**
 * 证据「关系」tag 的显示文案（按 NKB 关系字典 ID 归一，胚系/体细胞一致）：
 * 1 敏感性增加 / 4 有益的 → **敏感性增加**（两者同属正相关、在报告里都算"获益"类）
 * 6 抗药性 → **耐药性增加**
 * 其余取值原样显示（2 敏感性降低 / 3 未明 / 5 无益的，当前证据过滤不会出现）
 *
 * @param drug 证据行（后端已带 relationshipId）
 * @returns 展示文案；无关系时返回空串（模板 v-if 不会渲染该 tag）
 */
function relationshipTagText(drug: Api.Report.PreviewDrug): string {
  if (drug.relationshipId === 6) {
    return '耐药性增加';
  }
  if (drug.relationshipId === 1 || drug.relationshipId === 4) {
    return '敏感性增加';
  }
  return drug.relationship ?? '';
}

/** 临床试验表列（对齐实际报告「临床试验信息」表：ID / 名称 / 肿瘤类型 / 阶段 / 药物 / 地点） */
const TRIAL_COLUMNS: NaiveUI.TableColumn<Api.Report.PreviewTrial>[] = [
  { key: 'trialId', title: 'ID', width: 130 },
  { key: 'title', title: '临床试验名称', minWidth: 240, ellipsis: { tooltip: true } },
  { key: 'trialCondition', title: '肿瘤类型', width: 160, ellipsis: { tooltip: true } },
  { key: 'phaseText', title: '阶段', width: 80, align: 'center' },
  { key: 'drugName', title: '药物', width: 170, ellipsis: { tooltip: true } },
  { key: 'location', title: '地点', width: 150, ellipsis: { tooltip: true } }
];

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

/** 详情用：分级项标签 —— 胚系看「临床意义」（五级人工确认），体细胞看「位点分级」（I/II/III 类） */
function gradeLabel(row: Api.Report.PreviewVariant | null): string {
  return row?.sourceType === 'CR_ALL' ? '临床意义' : '位点分级';
}

/** 详情用：分级项取值（与 {@link gradeLabel} 配套） */
function gradeValue(row: Api.Report.PreviewVariant | null): string {
  return row?.sourceType === 'CR_ALL' ? row.clinicalSignificanceLabel ?? '-' : row?.variationClass ?? '-';
}

const sectionMeta = computed(() => [
  {
    key: 'somatic',
    title: '体细胞变异解析',
    hint: '报出的体细胞位点（SNP/Indel + CNV + Fusion），按癌种匹配 NKB 用药证据',
    section: preview.value?.somaticVariants,
    columns: buildSomaticColumns(),
    gradeTags: gradeCountTags(preview.value?.somaticVariants, 'somatic')
  },
  {
    key: 'germline',
    title: '肿瘤遗传风险',
    hint: '报出的胚系位点（CR_ALL），临床意义人工确认后按致病性分级匹配证据',
    section: preview.value?.germlineVariants,
    columns: buildGermlineColumns(),
    gradeTags: gradeCountTags(preview.value?.germlineVariants, 'germline')
  }
]);

/** 质控指标中文表头（模板专属字段；未知键原样显示） */
const QC_LABELS: Record<string, string> = {
  tumorCellContent: '肿瘤细胞含量',
  dnaTotal: 'DNA总量(ng)',
  dnaDegradation: 'DNA降解',
  preLibraryTotal: '建库前总量(ng)',
  sequencingDataVolume: '测序数据量',
  meanDepth: '平均深度',
  coverageUniformity: '覆盖均一性',
  targetRegionCoverage: '目标区域覆盖度',
  genomeAlignmentRate: '基因组比对率',
  baseQualityQ30Rate: 'Q30比例'
};

/** 取值兜底：空显示 '/' */
function qcText(value: unknown): string {
  return value == null || value === '' ? '/' : String(value);
}

/** 质控：样本 / 对照 两列并排（报告 JSON 的 qualityControl；圣域等模板才有） */
const qcRows = computed(() => {
  const qc = preview.value?.qualityControl ?? {};
  const sampleQc = (qc.sampleQc ?? {}) as Record<string, unknown>;
  const controlQc = (qc.controlQc ?? {}) as Record<string, unknown>;
  const keys = Object.keys(sampleQc).length ? Object.keys(sampleQc) : Object.keys(controlQc);
  return keys.map(key => ({
    key,
    label: QC_LABELS[key] ?? key,
    sample: qcText(sampleQc[key]),
    control: qcText(controlQc[key])
  }));
});

const qcColumns = [
  { title: '指标', key: 'label', width: 180 },
  { title: '样本', key: 'sample', minWidth: 140 },
  { title: '对照', key: 'control', minWidth: 140 }
];

const qcSpecimenType = computed(() => qcText(preview.value?.qualityControl?.specimenType));

watch(() => [props.analysisId, props.reportId], () => initPreview(), { immediate: true });
</script>

<template>
  <div class="flex-col-stretch gap-12px">
    <NCard :bordered="false" size="small" class="card-wrapper">
      <NSpace align="center" justify="space-between">
        <NSpace align="center" :size="12">
          <span class="font-medium">报告预览</span>
          <NTag size="small" type="info">schema {{ preview?.schemaVersion ?? '-' }}</NTag>
          <NTag v-if="previewSource === 'artifact'" size="small" type="success">
            已生成报告{{ previewArtifactTime ? `（${previewArtifactTime}）` : '' }}
          </NTag>
          <NTag v-else-if="previewSource === 'realtime'" size="small" type="warning">实时组装</NTag>
          <span class="text-13px op-60">{{ contextLine }}</span>
        </NSpace>
        <NSpace :size="8">
          <NButton size="small" @click="jsonVisible = true">查看 JSON</NButton>
          <NButton size="small" type="primary" ghost :loading="editing" @click="() => loadPreview()">重新预览</NButton>
          <NButton size="small" type="primary" :loading="generating" @click="handleGenerate">生成报告</NButton>
          <NButton v-if="hasReportFile" size="small" type="success" ghost @click="handleDownload">下载报告</NButton>
          <NButton
            v-if="canSubmitReview"
            size="small"
            type="info"
            ghost
            :loading="submitting"
            @click="handleSubmitReview"
          >
            提交审核
          </NButton>
        </NSpace>
      </NSpace>
      <NDivider class="my-12px!" />
      <!-- 报告生成参数（模板 / 解读癌种 / 报告日期 / 解读人）：label + input，与预览同卡片 -->
      <ReportGenerateParams
        ref="generateParamsRef"
        :analysis-id="props.analysisId"
        :report-id="props.reportId"
        :preset-disease="(preview?.sampleInfo?.disease as string | null) ?? null"
        :preset-report-date="(preview?.reportInfo?.reportDate as string | null) ?? null"
        :preset-analyzer="(preview?.reportInfo?.analyzer as string | null) ?? null"
        :locked="previewSource === 'artifact'"
        @change="() => loadPreview()"
      />
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
          <NTag v-for="tag in meta.gradeTags" :key="tag.key" size="small">{{ tag.label }}</NTag>
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

    <!-- 质控：报告 JSON 的 qualityControl（模板专属，圣域等模板才有） -->
    <NCard v-if="qcRows.length" :bordered="false" size="small" class="card-wrapper">
      <template #header>
        <div class="flex items-center gap-8px">
          <span>质控</span>
          <NTag size="small">{{ qcSpecimenType }}</NTag>
        </div>
      </template>
      <NDataTable :columns="qcColumns" :data="qcRows" :row-key="row => row.key" size="small" />
    </NCard>

    <!-- 详情：标签行（关联突变 / 药物信息 / 位点分级·临床意义 / 证据条数）+ 说明 + 证据明细全量 -->
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
            <span class="op-60">{{ gradeLabel(detailRow) }}</span>
            <NTag size="small">{{ gradeValue(detailRow) }}</NTag>
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
              <NTag v-if="relationshipTagText(drug)" size="small" type="info">{{ relationshipTagText(drug) }}</NTag>
              <NTag v-if="drug.fromOtherCancer" size="small" type="warning">其他癌种获批</NTag>
              <!-- 证据类型：说明为空但挂着招募中试验 → 临床试验（有说明的不打 tag，说明正文本身就是标识） -->
              <NTag v-if="hasClinicalTrial(drug)" size="small" type="warning" :bordered="false">临床试验</NTag>
              <NTag v-for="type in drug.guidelineTypes ?? []" :key="type" size="small" type="info">{{ type }}指南</NTag>
              <NTag v-if="drug.approvingAgency" size="small" type="success">{{ drug.approvingAgency }}</NTag>
              <NTag
                v-if="nodeTagText(detailRow.gene, drug.nodeName)"
                size="small"
                type="info"
                :bordered="false"
              >
                {{ nodeTagText(detailRow.gene, drug.nodeName) }}
              </NTag>
            </div>
            <!-- 说明为空就不渲染这一行（en7 报告里这类证据只出现在等级药名串里） -->
            <div v-if="hasDescription(drug)" class="whitespace-pre-wrap text-12px leading-20px op-80">
              {{ drugDescription(drug) }}
            </div>
          </div>
        </div>

        <!-- 临床试验证据（第二类证据）：ID / 名称 / 肿瘤类型 / 阶段 / 药物 / 地点 -->
        <div v-if="detailRow.trials?.length" class="mt-14px">
          <div class="mb-6px flex items-center gap-6px text-13px font-medium">
            <NTag size="small" type="warning" :bordered="false">临床试验</NTag>
            <span>临床试验信息（{{ detailRow.trials.length }} 条）</span>
          </div>
          <NDataTable
            :columns="TRIAL_COLUMNS"
            :data="detailRow.trials"
            :pagination="false"
            size="small"
            :bordered="false"
            :single-line="false"
          />
        </div>
      </div>
    </NModal>

    <!-- 改靶：人工指定一个/多个知识库父级节点（留空 = 取消改靶） -->
    <NModal
      v-model:show="targetVisible"
      preset="card"
      title="改靶（指定知识库父级）"
      class="w-50vw max-w-640px"
      :bordered="false"
    >
      <div class="mb-10px text-12px op-60">
        位点：{{ targetRow?.gene ?? '-' }} {{ targetRow?.variant ?? '-' }}
        · 父级从知识库选（可多个），留空 = 取消改靶
      </div>
      <NSelect
        v-model:value="targetIds"
        multiple
        filterable
        remote
        clearable
        :options="targetOptions"
        :loading="candidateLoading"
        :disabled="!canEdit"
        placeholder="输入基因或位点关键词搜索知识库节点"
        @search="searchCandidates"
      />
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
