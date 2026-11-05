<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchUpdateBioinfoQc } from '@/service/api/qc/bioinfo-qc';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';
import {
  BIOINFO_QC_EDIT_KEYS,
  BIOINFO_QC_NUMBER_FIELDS,
  BIOINFO_QC_TEXT_FIELDS,
  EMPTY_BIOINFO_QC_FORM
} from './bioinfo-qc-form-model';

defineOptions({
  name: 'BioinfoQcOperateDrawer'
});

interface Props {
  /** 行数据（修改时传入） */
  rowData?: Api.Qc.BioinfoQc | null;
}

const props = defineProps<Props>();

const visible = defineModel<boolean>('visible', { required: true });

const emit = defineEmits<{
  (e: 'submitted'): void;
}>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = ref<Api.Qc.BioinfoQcForm>(jsonClone(EMPTY_BIOINFO_QC_FORM));

const title = computed(() => $t('common.edit'));

/** 打开抽屉时把行数据填进表单（字段多，清空 + 逐个赋值比写 34 个 if 好维护） */
function resetModel() {
  const row = props.rowData;
  model.value = jsonClone(EMPTY_BIOINFO_QC_FORM);
  if (!row) return;
  model.value.id = row.id;
  model.value.subbarcode = row.subbarcode;
  const source = row as unknown as Record<string, unknown>;
  BIOINFO_QC_EDIT_KEYS.forEach(key => {
    model.value[key] = (source[key] ?? null) as never;
  });
}

watch(visible, value => {
  if (value) {
    resetModel();
    restoreValidation();
  }
});

async function handleSubmit() {
  await validate();
  const { error } = await fetchUpdateBioinfoQc(model.value);
  if (error) return;
  window.$message?.success($t('common.updateSuccess'));
  visible.value = false;
  emit('submitted');
}
</script>

<template>
  <NDrawer v-model:show="visible" :width="640" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" label-placement="left" :label-width="130">
        <NAlert type="info" :bordered="false" class="mb-16px">
          样本编号是接口匹配样本的键，不可修改；保存后按「质控标准」里的阈值重新判定 status。
        </NAlert>
        <NFormItem label="subbarcode">
          <NInput :value="model.subbarcode ?? ''" disabled />
        </NFormItem>
        <NGrid :cols="2" :x-gap="16">
          <NFormItemGi v-for="field in BIOINFO_QC_NUMBER_FIELDS" :key="field.key" :label="field.label">
            <NInputNumber v-model:value="model[field.key]" :show-button="false" class="w-full" />
          </NFormItemGi>
          <NFormItemGi v-for="field in BIOINFO_QC_TEXT_FIELDS" :key="field.key" :label="field.label">
            <NInput v-model:value="model[field.key]" />
          </NFormItemGi>
        </NGrid>
      </NForm>

      <template #footer>
        <NSpace :size="16" justify="end">
          <NButton @click="visible = false">{{ $t('common.cancel') }}</NButton>
          <NButton type="primary" @click="handleSubmit">{{ $t('common.confirm') }}</NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
