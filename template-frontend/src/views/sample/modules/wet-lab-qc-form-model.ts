/**
 * 湿实验质控「修改」抽屉的字段清单与空表单。
 * <p>
 * 单独一个文件的原因与生信质控（bioinfo-qc-form-model.ts）一致：字段清单只写一处，
 * 抽屉里用 v-for 渲染，避免在抽屉里堆一长串 NFormItem。
 * 值全是字符串（后端列就是 varchar：阈值/等级值都能原样存），所以一律 NInput。
 * 标签用中文名（与 qc_standard.qc_item 一致），括号里带上列名 = qc_param。
 */

type FormField = keyof Api.Qc.WetLabQcForm;

/** 可编辑的质控项字段（除 id / subbarcode） */
type ValueField = Exclude<FormField, 'id' | 'subbarcode'>;

/** 质控项：中文名 + 行字段 + 对应列名（= qc_standard.qc_param） */
export const WET_LAB_QC_VALUE_FIELDS: { key: ValueField; label: string; param: string }[] = [
  { key: 'pathoTumorPurity', label: '肿瘤纯度', param: 'patho_tumor_purity' },
  { key: 'dnaTotalNg', label: 'DNA总量（ng）', param: 'dna_total_ng' },
  { key: 'dnaDegradation', label: '检测结果判断', param: 'dna_degradation' },
  { key: 'preLibTotalNg', label: '预文库总量（ng）', param: 'pre_lib_total_ng' },
  { key: 'finalLibConc', label: '终文库浓度（ng/uL）', param: 'final_lib_conc' },
  { key: 'dnaTotalNgNormal', label: 'DNA总量（ng）对照', param: 'dna_total_ng_normal' },
  { key: 'preLibTotalNgNormal', label: '预文库总量（ng）对照', param: 'pre_lib_total_ng_normal' },
  { key: 'finalLibConcNormal', label: '终文库浓度（ng/uL）对照', param: 'final_lib_conc_normal' }
];

/** 全部可编辑字段名（重置表单时用） */
export const WET_LAB_QC_EDIT_KEYS: FormField[] = WET_LAB_QC_VALUE_FIELDS.map(field => field.key);

/** 空表单：抽屉打开时用 jsonClone(EMPTY_WET_LAB_QC_FORM) 再填真实值（别直接改这个常量） */
export const EMPTY_WET_LAB_QC_FORM: Api.Qc.WetLabQcForm = {
  id: 0,
  subbarcode: null,
  pathoTumorPurity: null,
  dnaTotalNg: null,
  dnaDegradation: null,
  preLibTotalNg: null,
  finalLibConc: null,
  dnaTotalNgNormal: null,
  preLibTotalNgNormal: null,
  finalLibConcNormal: null
};
