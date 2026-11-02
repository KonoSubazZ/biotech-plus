/**
 * 生信质控「修改」抽屉的字段清单与空表单。
 * <p>
 * 单独一个文件的两个原因：字段有 34 个，写在抽屉里会让 createDefaultModel() 顶穿函数行数红线
 * （ai-rules 可读性：单函数 ≤ 50 行）；数值/文本分成两组，抽屉里才能用 v-model 直接绑（否则联合类型过不了 vue-tsc）。
 * 字段暂无中文名 —— 标签直接用数据字段名（snake_case）。
 * <p>
 * 控件选择按**后端下发的类型**分：decimal 字段后端序列化成字符串（避免精度丢失），所以走 NInput；
 * 只有 bigint 的四个计数字段（target_size/total_reads/mapped_reads/before_duplicate）走 NInputNumber。
 */

type FormField = keyof Api.Qc.BioinfoQcForm;

/** 值类型是 number|null 的字段（bigint，走 NInputNumber） */
type NumberField = {
  [K in FormField]: Api.Qc.BioinfoQcForm[K] extends number | null ? K : never;
}[FormField];

/** 其余可编辑字段（含 decimal 的字符串字段，走 NInput） */
type TextField = Exclude<FormField, NumberField | 'id' | 'subbarcode'>;

/** 计数字段（bigint） */
export const BIOINFO_QC_NUMBER_FIELDS: { key: NumberField; label: string }[] = [
  { key: 'targetSize', label: 'target_size' },
  { key: 'totalReads', label: 'total_reads' },
  { key: 'mappedReads', label: 'mapped_reads' },
  { key: 'beforeDuplicate', label: 'before_duplicate' },
];

/** 文本 / 小数（decimal 按字符串下发）字段 */
export const BIOINFO_QC_TEXT_FIELDS: { key: TextField; label: string }[] = [
  { key: 'bam', label: 'bam' },
  { key: 'gender', label: 'gender' },
  { key: 'libId', label: 'lib_id' },
  { key: 'paired', label: 'paired' },
  { key: 'rawGc', label: 'raw_gc' },
  { key: 'hotPos', label: 'hot_pos' },
  { key: 'rawQ20', label: 'raw_q20' },
  { key: 'rawQ30', label: 'raw_q30' },
  { key: 'pipeline', label: 'pipeline' },
  { key: 'rawBase', label: 'raw_base' },
  { key: 'sshPath', label: 'ssh_path' },
  { key: 'duplicate', label: 'duplicate' },
  { key: 'peakSize', label: 'peak_size' },
  { key: 'qcResult', label: 'qc_result' },
  { key: 'sqi16', label: 'sqi16' },
  { key: 'bioResult', label: 'bio_result' },
  { key: 'peakRange', label: 'peak_range' },
  { key: 'concordance', label: 'concordance' },
  { key: 'chrall', label: 'chrall' },
  { key: 'librarySize', label: 'library_size' },
  { key: 'bamRealign', label: 'bam_realign' },
  { key: 'mappingRate', label: 'mapping_rate' },
  { key: 'averageDepth', label: 'average_depth' },
  { key: 'coverageTarget', label: 'coverage_target' },
  { key: 'duplicateTarget', label: 'duplicate_target' },
  { key: 'contaminationLevel', label: 'contamination_level' },
  { key: 'coverageTarget50x', label: 'coverage_target_50x' },
  { key: 'probeCaptureRatio', label: 'probe_capture_ratio' },
  { key: 'coverageTarget500x', label: 'coverage_target_500x' },
  { key: 'coverageTarget1000x', label: 'coverage_target_1000x' },
  { key: 'coverageUniformity02xadOnTarget', label: 'coverage_uniformity_02xad_on_target' },
];

/** 全部可编辑字段名（重置表单时用） */
export const BIOINFO_QC_EDIT_KEYS: FormField[] = [
  ...BIOINFO_QC_NUMBER_FIELDS.map(field => field.key),
  ...BIOINFO_QC_TEXT_FIELDS.map(field => field.key)
];

/** 空表单：抽屉打开时用 jsonClone(EMPTY_BIOINFO_QC_FORM) 再填真实值（别直接改这个常量） */
export const EMPTY_BIOINFO_QC_FORM: Api.Qc.BioinfoQcForm = {
  id: 0,
  subbarcode: null,
  bam: null,
  gender: null,
  libId: null,
  paired: null,
  rawGc: null,
  hotPos: null,
  rawQ20: null,
  rawQ30: null,
  pipeline: null,
  rawBase: null,
  sshPath: null,
  duplicate: null,
  peakSize: null,
  qcResult: null,
  sqi16: null,
  bioResult: null,
  peakRange: null,
  concordance: null,
  chrall: null,
  librarySize: null,
  bamRealign: null,
  targetSize: null,
  totalReads: null,
  mappingRate: null,
  averageDepth: null,
  coverageTarget: null,
  mappedReads: null,
  duplicateTarget: null,
  contaminationLevel: null,
  coverageTarget50x: null,
  probeCaptureRatio: null,
  coverageTarget500x: null,
  coverageTarget1000x: null,
  coverageUniformity02xadOnTarget: null,
  beforeDuplicate: null,
};
