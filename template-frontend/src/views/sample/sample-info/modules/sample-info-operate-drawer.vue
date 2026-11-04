<script setup lang="tsx">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import type { DataTableColumns } from 'naive-ui';
import {
  fetchCreateCmsSample,
  fetchGetCmsSample,
  fetchGetCmsSampleChangeLog,
  fetchUpdateCmsSample
} from '@/service/api/report/cms-sample';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';
import { createEmptyCmsSampleForm } from './cms-sample-fields';
import SampleInfoFormGroups from './sample-info-form-groups.vue';

defineOptions({
  name: 'SampleInfoOperateDrawer'
});

interface Props {
  /** view = 只读详情；add / edit = 表单 */
  mode: 'view' | 'add' | 'edit';
  /** 操作类型：add / edit（由 useTableOperate 提供） */
  operateType: NaiveUI.TableOperateType;
  /** 查看/编辑时的行数据 */
  rowData?: Api.Report.CmsSample | null;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
  (e: 'edit'): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', {
  default: false
});

const { formRef, validate, restoreValidation } = useNaiveForm();

/** 最近一次 CMS 同步摘要（只读展示：谁 / 何时 / 几项变更），来自列表行的 syncLog */
const syncSummary = computed(() => {
  const raw = props.rowData?.syncLog;
  if (!raw) return '';
  try {
    const list = JSON.parse(raw) as Api.Report.CmsFieldChange[];
    if (!Array.isArray(list) || list.length === 0) return '';
    const last = list[list.length - 1];
    return `${last.by} · ${last.at} · ${list.length} 项变更`;
  } catch {
    return '';
  }
});
const { createRequiredRule } = useFormRules();

const saving = ref(false);
const loadingDetail = ref(false);

/** 历史修改信息（从CMS更新 + 编辑保存）；只在查看模式展示 */
interface HistoryRow extends Api.Report.CmsSampleChangeLog {
  sourceLabel: string;
  oldText: string;
  newText: string;
}

const HISTORY_SOURCE_LABEL: Record<string, string> = {
  refresh: '从CMS更新',
  edit: '编辑'
};

const history = ref<HistoryRow[]>([]);
const loadingHistory = ref(false);

const historyColumns: DataTableColumns<HistoryRow> = [
  { key: 'operateTime', title: '操作时间', width: 150, align: 'center' },
  { key: 'sourceLabel', title: '操作类型', width: 100, align: 'center' },
  {
    key: 'label',
    title: '字段',
    width: 130,
    align: 'center',
    ellipsis: { tooltip: true },
    render: row => row.label || row.field
  },
  { key: 'oldText', title: '旧值', ellipsis: { tooltip: true } },
  { key: 'newText', title: '新值', ellipsis: { tooltip: true } },
  { key: 'operator', title: '操作人', width: 100, align: 'center' }
];

async function loadHistory() {
  history.value = [];
  if (!props.rowData?.id) return;
  loadingHistory.value = true;
  try {
    const { data, error } = await fetchGetCmsSampleChangeLog(props.rowData.id);
    if (error || !data) return;
    history.value = data.map(row => ({
      ...row,
      sourceLabel: HISTORY_SOURCE_LABEL[row.source] ?? row.source,
      oldText: row.oldValue ?? '—',
      newText: row.newValue ?? '—'
    }));
  } finally {
    loadingHistory.value = false;
  }
}

const title = computed(() => {
  if (props.mode === 'view') return '查看样本信息';
  return props.mode === 'add' ? '新增样本信息' : '编辑样本信息';
});

const model = ref<Api.Report.CmsSampleForm>(createEmptyCmsSampleForm());

const rules = {
  subbarcode: createRequiredRule('诺禾编号不能为空')
};

/** 打开抽屉时准备数据：查看直接用行数据；编辑重新拉详情，避免用列表里的旧值覆盖 */
async function prepareModel() {
  model.value = createEmptyCmsSampleForm();
  history.value = [];

  if (props.mode === 'add') return;

  if (props.mode === 'view') {
    if (props.rowData) Object.assign(model.value, jsonClone(props.rowData));
    loadHistory();
    return;
  }

  if (!props.rowData?.id) return;
  loadingDetail.value = true;
  try {
    const { data, error } = await fetchGetCmsSample(props.rowData.id);
    if (!error && data) Object.assign(model.value, jsonClone(data));
  } finally {
    loadingDetail.value = false;
  }
}

function closeDrawer() {
  visible.value = false;
}

async function handleSubmit() {
  await validate();

  // pending 时禁止重复提交与关闭；失败时保留抽屉与输入（UX-CONTRACT.md）
  saving.value = true;
  try {
    if (props.mode === 'add') {
      const { error } = await fetchCreateCmsSample(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.mode === 'edit') {
      const { error } = await fetchUpdateCmsSample(model.value);
      if (error) return;
      window.$message?.success($t('common.updateSuccess'));
    }

    closeDrawer();
    emit('submitted');
  } finally {
    saving.value = false;
  }
}

watch(visible, () => {
  if (visible.value) {
    prepareModel();
    restoreValidation();
  }
});
</script>

<template>
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="1000" class="max-w-94%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NSpin :show="loadingDetail">
        <NForm ref="formRef" :model="model" :rules="rules" label-placement="top">
          <SampleInfoFormGroups v-model:model="model" :mode="mode" />
        </NForm>
        <div v-if="mode === 'view' && syncSummary" class="mt-12px text-12px text-#999">
          最近同步：{{ syncSummary }}
        </div>

        <!-- 历史修改信息：数据来自行内从CMS更新 + 抽屉内编辑保存，时间倒序 -->
        <div v-if="mode === 'view'" class="mt-16px">
          <div class="mb-8px flex items-center justify-between">
            <span class="text-14px font-600">历史修改信息</span>
            <span class="text-12px text-#999">共 {{ history.length }} 条</span>
          </div>
          <NDataTable
            :columns="historyColumns"
            :data="history"
            :loading="loadingHistory"
            :pagination="false"
            :max-height="320"
            :bordered="false"
            size="small"
          />
        </div>
      </NSpin>
      <template #footer>
        <NSpace :size="16">
          <NButton :disabled="saving" @click="closeDrawer">{{ mode === 'view' ? '关闭' : $t('common.cancel') }}</NButton>
          <NButton v-if="mode === 'view'" type="primary" @click="emit('edit')">编辑</NButton>
          <NButton v-else type="primary" :loading="saving" @click="handleSubmit">{{ $t('common.confirm') }}</NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
