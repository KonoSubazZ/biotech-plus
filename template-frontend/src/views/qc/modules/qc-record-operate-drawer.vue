<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateQcRecord, fetchUpdateQcRecord } from '@/service/api/qc/qc-record';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'QcRecordOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据 */
  rowData?: Api.Qc.QcRecord | null;
  /** 质控类别：由所在页面固定传入（wet_lab / bioinfo），表单里不编辑 */
  qcCategory: string;
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

const title = computed(() => (props.operateType === 'add' ? '新增质控记录' : '编辑质控记录'));

/** 人工状态（pending 待确认 / passed 通过 / failed 未通过） */
const statusOptions = [
  { label: '待确认', value: 'pending' },
  { label: '通过', value: 'passed' },
  { label: '未通过', value: 'failed' }
];

const model = ref<Api.Qc.QcRecordForm>(createDefaultModel());

function createDefaultModel(): Api.Qc.QcRecordForm {
  return {
    subbarcode: '',
    qcItem: '',
    qcResult: null,
    operator: null,
    testedAt: null,
    remark: null,
    status: 'pending',
    qcCategory: props.qcCategory
  };
}

const rules = {
  subbarcode: createRequiredRule('样本条码不能为空'),
  qcItem: createRequiredRule('质控项目不能为空'),
  status: createRequiredRule('人工状态不能为空')
};

function handleUpdateModelWhenEdit() {
  model.value = createDefaultModel();

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
      const { error } = await fetchCreateQcRecord(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.operateType === 'edit') {
      const { error } = await fetchUpdateQcRecord(model.value);
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
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="600" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules">
        <div class="grid grid-cols-1 gap-16px md:grid-cols-2">
          <NFormItem label="样本条码" path="subbarcode">
            <NInput v-model:value="model.subbarcode" placeholder="请输入样本条码" />
          </NFormItem>
          <NFormItem label="质控项目" path="qcItem">
            <NInput v-model:value="model.qcItem" placeholder="如 mapping_rate、average_depth" />
          </NFormItem>
          <NFormItem label="质控结果" path="qcResult">
            <NInput v-model:value="model.qcResult" placeholder="数值字符串，如 98.5" />
          </NFormItem>
          <NFormItem label="操作员" path="operator">
            <NInput v-model:value="model.operator" placeholder="请输入操作员" />
          </NFormItem>
          <NFormItem label="检测时间" path="testedAt">
            <NDatePicker
              v-model:formatted-value="model.testedAt"
              type="datetime"
              value-format="yyyy-MM-dd HH:mm:ss"
              clearable
            />
          </NFormItem>
          <NFormItem label="人工状态" path="status">
            <NSelect v-model:value="model.status" :options="statusOptions" placeholder="请选择人工状态" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="备注" path="remark">
            <NInput v-model:value="model.remark" type="textarea" placeholder="请输入备注" />
          </NFormItem>
        </div>
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
