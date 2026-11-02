<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateCmsSample, fetchGetCmsSample, fetchUpdateCmsSample } from '@/service/api/report/cms-sample';
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
const { createRequiredRule } = useFormRules();

const saving = ref(false);
const loadingDetail = ref(false);

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

  if (props.mode === 'add') return;

  if (props.mode === 'view') {
    if (props.rowData) Object.assign(model.value, jsonClone(props.rowData));
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
