<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchUpdateWetLabQc } from '@/service/api/qc/wet-lab-qc';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';
import { EMPTY_WET_LAB_QC_FORM, WET_LAB_QC_EDIT_KEYS, WET_LAB_QC_VALUE_FIELDS } from './wet-lab-qc-form-model';

defineOptions({
  name: 'WetLabQcOperateDrawer'
});

interface Props {
  /** 行数据（修改时传入） */
  rowData?: Api.Qc.WetLabQc | null;
}

const props = defineProps<Props>();

const visible = defineModel<boolean>('visible', { required: true });

const emit = defineEmits<{
  (e: 'submitted'): void;
}>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = ref<Api.Qc.WetLabQcForm>(jsonClone(EMPTY_WET_LAB_QC_FORM));

const title = computed(() => $t('common.edit'));

/** 打开抽屉时把行数据填进表单（清空 + 按字段清单逐个赋值） */
function resetModel() {
  const row = props.rowData;
  model.value = jsonClone(EMPTY_WET_LAB_QC_FORM);
  if (!row) return;
  model.value.id = row.id;
  model.value.subbarcode = row.subbarcode;
  const source = row as unknown as Record<string, unknown>;
  WET_LAB_QC_EDIT_KEYS.forEach(key => {
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
  const { error } = await fetchUpdateWetLabQc(model.value);
  if (error) return;
  window.$message?.success($t('common.updateSuccess'));
  visible.value = false;
  emit('submitted');
}
</script>

<template>
  <NDrawer v-model:show="visible" :width="560" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" label-placement="left" :label-width="150">
        <NAlert type="info" :bordered="false" class="mb-16px">
          样本编号是接口匹配样本的键，不可修改；保存后按「质控标准」里的阈值重新判定 status。
        </NAlert>
        <NFormItem label="样本条码">
          <NInput :value="model.subbarcode ?? ''" disabled />
        </NFormItem>
        <!-- 一个质控项一行：值全是字符串（阈值/等级值都能原样存），统一 NInput -->
        <NFormItem v-for="field in WET_LAB_QC_VALUE_FIELDS" :key="field.key" :label="`${field.label}（${field.param}）`">
          <NInput v-model:value="model[field.key]" clearable placeholder="留空表示不修改该列" />
        </NFormItem>
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
