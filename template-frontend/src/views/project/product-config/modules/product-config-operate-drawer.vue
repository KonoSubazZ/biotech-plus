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

/** panel类型：取值同生信质检判定口径（DNA / RNA / DNA_RNA / WES / HRD …），允许自定义 */
const panelTypeOptions = ['DNA', 'RNA', 'DNA_RNA', 'WES', 'HRD', 'DNA_HRD'].map(item => ({
  label: item,
  value: item
}));

function createDefaultModel(): Api.Project.ProductConfigForm {
  return {
    productCode: '',
    tradeName: null,
    productBio: '',
    productName: '',
    relatedDiseases: null,
    testType: null,
    controlType: null,
    productVersion: null,
    sequencingPlatform: null,
    geneCount: null,
    reportCycleDays: null,
    maintainLab: null,
    relatedProcess: null,
    probeType: null,
    panelType: null,
    status: 'active',
    remark: null
  };
}

const rules = {
  productName: createRequiredRule('产品名称不能为空'),
  productBio: createRequiredRule('生信编码不能为空'),
  productCode: createRequiredRule('产品编码不能为空')
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
          <NFormItem label="产品编码" path="productCode">
            <NInput v-model:value="model.productCode" placeholder="请输入产品编码" />
          </NFormItem>
          <NFormItem label="商品名" path="tradeName">
            <NInput v-model:value="model.tradeName" placeholder="请输入商品名" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="产品名称" path="productName">
            <NInput v-model:value="model.productName" placeholder="请输入系统用产品名称" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="生信编码" path="productBio">
            <NInput v-model:value="model.productBio" placeholder="生信编码，须与上传目录第 2 层 product 逐字一致" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="适应症" path="relatedDiseases">
            <NInput v-model:value="model.relatedDiseases" placeholder="请输入适应症，多个可用、分隔" />
          </NFormItem>
          <NFormItem label="样本类型" path="testType">
            <NInput v-model:value="model.testType" placeholder="如：组织 / 血液" />
          </NFormItem>
          <NFormItem label="对照" path="controlType">
            <NInput v-model:value="model.controlType" placeholder="如：血液 / PBMC / 无" />
          </NFormItem>
          <NFormItem label="版本" path="productVersion">
            <NInput v-model:value="model.productVersion" placeholder="如：LDT / IVD" />
          </NFormItem>
          <NFormItem label="测序平台" path="sequencingPlatform">
            <NInput v-model:value="model.sequencingPlatform" placeholder="如：surf5000 Dx" />
          </NFormItem>
          <NFormItem label="基因数/DNA" path="geneCount">
            <NInputNumber v-model:value="model.geneCount" class="w-full" :min="0" :precision="0" placeholder="如：1238" />
          </NFormItem>
          <NFormItem label="报告周期(天)" path="reportCycleDays">
            <NInputNumber v-model:value="model.reportCycleDays" class="w-full" placeholder="请输入报告周期" />
          </NFormItem>
          <NFormItem label="维护实验室" path="maintainLab">
            <NInput v-model:value="model.maintainLab" placeholder="如：天津实验室" />
          </NFormItem>
          <NFormItem label="关联工序" path="relatedProcess">
            <NInput v-model:value="model.relatedProcess" placeholder="如：全流程" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="探针类型" path="probeType">
            <NInput v-model:value="model.probeType" placeholder="如：正常1238探针 / 正常1238探针+UMI" />
          </NFormItem>
          <NFormItem label="panel类型" path="panelType">
            <NSelect
              v-model:value="model.panelType"
              :options="panelTypeOptions"
              filterable
              tag
              clearable
              placeholder="请选择或输入panel类型"
            />
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
