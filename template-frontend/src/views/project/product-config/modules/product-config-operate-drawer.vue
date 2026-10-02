<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateProductConfig, fetchUpdateProductConfig } from '@/service/api/project/product-config';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ProductConfigOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据 */
  rowData?: Api.Project.ProductConfig | null;
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

const title = computed(() => {
  const titles: Record<NaiveUI.TableOperateType, string> = {
    add: '新增产品配置',
    edit: '编辑产品配置'
  };
  return titles[props.operateType];
});

const model = ref<Api.Project.ProductConfigForm>(createDefaultModel());

/** 状态下拉（单选用 NSelect） */
const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'inactive' }
];

function createDefaultModel(): Api.Project.ProductConfigForm {
  return {
    name: '',
    code: '',
    testType: null,
    relatedDiseases: null,
    reportCycleDays: null,
    status: 'active',
    remark: null
  };
}

const rules = {
  name: createRequiredRule('产品名称不能为空'),
  code: createRequiredRule('产品编码不能为空')
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
      const { error } = await fetchCreateProductConfig(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.operateType === 'edit') {
      const { error } = await fetchUpdateProductConfig(model.value);
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
          <NFormItem label="产品名称" path="name">
            <NInput v-model:value="model.name" placeholder="请输入产品名称" />
          </NFormItem>
          <NFormItem label="产品编码" path="code">
            <NInput v-model:value="model.code" placeholder="请输入产品编码" />
          </NFormItem>
          <NFormItem label="检测类型" path="testType">
            <NInput v-model:value="model.testType" placeholder="请输入检测类型" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="相关疾病" path="relatedDiseases">
            <NInput v-model:value="model.relatedDiseases" placeholder="多个疾病可用、分隔" />
          </NFormItem>
          <NFormItem label="报告周期(天)" path="reportCycleDays">
            <NInputNumber v-model:value="model.reportCycleDays" class="w-full" placeholder="请输入报告周期" />
          </NFormItem>
          <NFormItem label="状态" path="status">
            <NSelect v-model:value="model.status" :options="statusOptions" placeholder="请选择状态" />
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
