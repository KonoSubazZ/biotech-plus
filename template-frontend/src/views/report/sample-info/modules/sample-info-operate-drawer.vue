<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateSampleInfo, fetchUpdateSampleInfo } from '@/service/api/report/sample-info';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';
import { EMPTY_SAMPLE_INFO_FORM } from './sample-info-form-model';
import SampleInfoFormOrder from './sample-info-form-order.vue';
import SampleInfoFormPatient from './sample-info-form-patient.vue';
import SampleInfoFormSample from './sample-info-form-sample.vue';
import SampleInfoFormTreatment from './sample-info-form-treatment.vue';

defineOptions({
  name: 'SampleInfoOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据 */
  rowData?: Api.Report.SampleInfo | null;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', {
  default: false
});

const { formRef, validate, restoreValidation } = useNaiveForm();
const { createRequiredRule } = useFormRules();

const saving = ref(false);

const title = computed(() => (props.operateType === 'add' ? '新增样本信息' : '编辑样本信息'));

const model = ref<Api.Report.SampleInfoForm>(jsonClone(EMPTY_SAMPLE_INFO_FORM));

const rules = {
  barcode: createRequiredRule('样本编号不能为空')
};

function handleUpdateModelWhenEdit() {
  model.value = jsonClone(EMPTY_SAMPLE_INFO_FORM);

  if (props.operateType === 'edit' && props.rowData) {
    Object.assign(model.value, jsonClone(props.rowData));
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
    if (props.operateType === 'add') {
      const { error } = await fetchCreateSampleInfo(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.operateType === 'edit') {
      const { error } = await fetchUpdateSampleInfo(model.value);
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
    handleUpdateModelWhenEdit();
    restoreValidation();
  }
});
</script>

<template>
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="920" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules" label-placement="left" :label-width="120">
        <SampleInfoFormPatient v-model:model="model" />
        <SampleInfoFormSample v-model:model="model" />
        <SampleInfoFormOrder v-model:model="model" />
        <SampleInfoFormTreatment v-model:model="model" />
      </NForm>
      <template #footer>
        <NSpace :size="16">
          <NButton :disabled="saving" @click="closeDrawer">{{ $t('common.cancel') }}</NButton>
          <NButton type="primary" :loading="saving" @click="handleSubmit">{{ $t('common.confirm') }}</NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
