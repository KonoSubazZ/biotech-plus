<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { NDatePicker, NForm, NFormItem, NSelect } from 'naive-ui';
import {
  fetchGetAnalyzerOptions,
  fetchGetDiseaseOptions,
  fetchGetInterpretationTemplateOptions
} from '@/service/api/report/interpretation';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({
  name: 'ReportGenerateParams'
});

/**
 * 报告生成参数小组件（对齐 report_en7 报告生成页 produceReport.jsp 的四个字段）
 *
 * 四个字段与默认值：
 *   模板     —— 报告产品绑定的候选模板（默认 = 产品默认模板；en7 也是按产品取模板）
 *   解读癌种 —— NKB 疾病分类下拉（默认 = 报告自带的解读癌种；en7 的「原发癌种」同此表）
 *   报告日期 —— 日期选择（默认 = 报告的分析日期；en7 默认当天，这里用分析日期以保证同输入同 JSON）
 *   解读人   —— 启用用户下拉（默认 = 当前登录用户；en7 是隐藏域取当前用户）
 *
 * 值变化时向上抛 change，由父组件重新拉预览 —— 该参数会真正参与组装（模板/癌种/报告日期/解读人）。
 * 报告已生成（props.locked）：预览读的是落盘的 JSON 制品，页面参数不再参与组装，
 * 四个控件置灰并回填成制品里的实际值，避免「改了以为报告就变了」。
 */
interface Props {
  analysisId: number;
  reportId: number;
  /** 报告自带的解读癌种（第一版预览 JSON 的 sampleInfo.disease），只在用户没动过该控件时用于回填默认值 */
  presetDisease?: string | null;
  /** 报告自带的报告日期（第一版预览 JSON 的 reportInfo.reportDate），同上只在没动过时回填 */
  presetReportDate?: string | null;
  /** 报告自带的解读人（第一版预览 JSON 的 reportInfo.analyzer），同上只在没动过时回填 */
  presetAnalyzer?: string | null;
  /** 已生成报告（预览来自 JSON 制品）：四个参数按生成时那份固定，控件置灰不可改 */
  locked?: boolean;
  templateLocked?: boolean;
  presetTemplateId?: number | null;
  busy?: boolean;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'change'): void;
}

const emit = defineEmits<Emits>();

const authStore = useAuthStore();

/** 当前选中的四个参数（父组件读它去请求预览） */
const templateId = ref<number | null>(null);
const disease = ref<string | null>(null);
const reportDate = ref<string | null>(null);
const analyzer = ref<string | null>(null);

/** 用户是否动过某个控件：动过就不再用报告自带值回填（避免手选被重新预览冲掉） */
const touched = ref({ disease: false, reportDate: false, analyzer: false, template: false });

const templateOptions = ref<{ label: string; value: number }[]>([]);
const diseaseOptions = ref<{ label: string; value: string }[]>([]);
const analyzerOptions = ref<{ label: string; value: string }[]>([]);

/** 父组件请求预览时要带的参数（没值就不传，后端各自走默认） */
const params = computed(() => ({
  templateId: templateId.value ?? undefined,
  disease: disease.value ?? undefined,
  reportDate: reportDate.value ?? undefined,
  analyzer: analyzer.value ?? undefined
}));

defineExpose({ params });

/** 模板：产品绑定的候选模板，默认选「产品默认模板」 */
async function loadTemplates() {
  const { data, error } = await fetchGetInterpretationTemplateOptions({
    analysisId: props.analysisId,
    reportId: props.reportId
  });
  if (error) {
    return;
  }
  const rows = data ?? [];
  templateOptions.value = rows.map(row => ({
    label: row.defaultTemplate ? `${row.templateName}（默认）` : row.templateName,
    value: row.templateId
  }));
  const preset = rows.find(row => row.templateId === props.presetTemplateId)
    ?? rows.find(row => row.defaultTemplate) ?? rows[0];
  templateId.value = preset ? preset.templateId : null;
}

/** 解读癌种：NKB 疾病分类 */
async function loadDiseases() {
  const { data, error } = await fetchGetDiseaseOptions();
  if (error) {
    return;
  }
  diseaseOptions.value = (data ?? []).map(row => ({ label: row.diseaseName, value: row.diseaseName }));
}

/** 解读人：启用用户，默认当前登录用户（按账号匹配选项） */
async function loadAnalyzers() {
  const { data, error } = await fetchGetAnalyzerOptions();
  if (error) {
    return;
  }
  const rows = data ?? [];
  analyzerOptions.value = rows.map(row => ({ label: `${row.nickName}（${row.userName}）`, value: row.userName }));
  // 已生成报告：解读人固定为生成时那位，不跟随当前登录用户
  if (props.presetAnalyzer && !touched.value.analyzer) {
    analyzer.value = props.presetAnalyzer;
    return;
  }
  // 当前登录用户：auth store 的 userInfo.user（账号 userName 与后端 sys_user 的 user_name 对应）
  const loginName = authStore.userInfo.user?.userName ?? null;
  const hit = rows.find(row => row.userName === loginName);
  analyzer.value = hit ? hit.userName : (loginName ?? null);
}

/** 报告自带的癌种/报告日期回填（只在用户没动过对应控件时） */
watch(
  () => props.presetDisease,
  value => {
    if (!props.locked && !touched.value.disease && value) {
      disease.value = value;
    }
  },
  { immediate: true }
);

watch(
  () => props.presetReportDate,
  value => {
    if (!props.locked && !touched.value.reportDate && value) {
      reportDate.value = value;
    }
  },
  { immediate: true }
);

watch(() => props.presetAnalyzer, value => {
  if (value && !touched.value.analyzer) {
    analyzer.value = value;
  }
});

watch(() => props.presetTemplateId, value => {
  if (value && (props.templateLocked || !touched.value.template)) {
    templateId.value = value;
  }
});

/** 已生成报告：四个参数一律回填成生成时那份（制品的实际值），页面上的改动不再生效 */
watch(
  () => [props.locked, props.presetDisease, props.presetReportDate, props.presetAnalyzer] as const,
  () => {
    if (!props.locked) {
      return;
    }
    if (props.presetDisease) {
      disease.value = props.presetDisease;
    }
    if (props.presetReportDate) {
      reportDate.value = props.presetReportDate;
    }
    if (props.presetAnalyzer) {
      analyzer.value = props.presetAnalyzer;
    }
  },
  { immediate: true }
);

onMounted(() => {
  loadTemplates();
  loadDiseases();
  loadAnalyzers();
});
</script>

<template>
  <!-- 报告生成参数：label + input（label-width 与模块内其它表单一致） -->
  <NForm label-placement="left" :label-width="90" :show-feedback="false">
    <div class="flex flex-wrap items-center">
      <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/4" label="模板">
        <NSelect
          v-model:value="templateId"
          size="small"
          :disabled="locked || templateLocked || busy"
          :options="templateOptions"
          :consistent-menu-width="false"
          placeholder="请选择模板"
          @update:value="() => { touched.template = true; emit('change'); }"
        />
      </NFormItem>
      <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/4" label="解读癌种">
        <NSelect
          v-model:value="disease"
          size="small"
          filterable
          clearable
          :disabled="locked || busy"
          :options="diseaseOptions"
          :consistent-menu-width="false"
          placeholder="请选择解读癌种"
          @update:value="
            () => {
              touched.disease = true;
              emit('change');
            }
          "
        />
      </NFormItem>
      <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/4" label="报告日期">
        <NDatePicker
          v-model:formatted-value="reportDate"
          class="w-full"
          size="small"
          type="date"
          value-format="yyyy-MM-dd"
          clearable
          :disabled="locked || busy"
          @update:formatted-value="
            () => {
              touched.reportDate = true;
              emit('change');
            }
          "
        />
      </NFormItem>
      <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/4" label="解读人">
        <NSelect
          v-model:value="analyzer"
          size="small"
          filterable
          :disabled="locked || busy"
          :options="analyzerOptions"
          :consistent-menu-width="false"
          placeholder="请选择解读人"
          @update:value="() => { touched.analyzer = true; emit('change'); }"
        />
      </NFormItem>
    </div>
  </NForm>
</template>

<style scoped></style>
