<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateQcStandard, fetchUpdateQcStandard } from '@/service/api/qc/qc-standard';
import { fetchGetActiveProductConfigList } from '@/service/api/project/product-config';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'QcStandardOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据 */
  rowData?: Api.Qc.QcStandard | null;
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

const title = computed(() => (props.operateType === 'add' ? '新增质控标准' : '编辑质控标准'));

/** 质控类别（单选用 NSelect） */
const categoryOptions = [
  { label: '湿实验', value: 'wet_lab' },
  { label: '生信', value: 'bioinfo' }
];

/** 状态 */
const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'inactive' }
];

const model = ref<Api.Qc.QcStandardForm>(createDefaultModel());

/** 关联产品下拉：只取启用中的产品（设计文档 product-config.md §3「前端下拉」） */
const productOptions = ref<{ label: string; value: number }[]>([]);

async function loadProductOptions() {
  const { data, error } = await fetchGetActiveProductConfigList();
  if (error || !data) {
    return;
  }
  productOptions.value = data.map(item => ({ label: `${item.name}（${item.code}）`, value: item.id }));
}

function createDefaultModel(): Api.Qc.QcStandardForm {
  return {
    productId: null,
    qcItem: '',
    qcCategory: 'wet_lab',
    minValue: null,
    maxValue: null,
    unit: null,
    status: 'active',
    remark: null
  };
}

const rules = {
  productId: createRequiredRule('关联产品不能为空'),
  qcItem: createRequiredRule('质控项目不能为空'),
  qcCategory: createRequiredRule('质控类别不能为空')
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
      const { error } = await fetchCreateQcStandard(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.operateType === 'edit') {
      const { error } = await fetchUpdateQcStandard(model.value);
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
    loadProductOptions();
  }
});
</script>

<template>
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="600" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules">
        <div class="grid grid-cols-1 gap-16px md:grid-cols-2">
          <NFormItem class="md:col-span-2" label="关联产品" path="productId">
            <NSelect
              v-model:value="model.productId"
              :options="productOptions"
              filterable
              placeholder="请选择产品（数据来自「项目管理 → 产品配置」）"
            />
          </NFormItem>
          <NFormItem label="质控项目" path="qcItem">
            <NInput v-model:value="model.qcItem" placeholder="请输入质控项目名称" />
          </NFormItem>
          <NFormItem label="质控类别" path="qcCategory">
            <NSelect v-model:value="model.qcCategory" :options="categoryOptions" placeholder="请选择质控类别" />
          </NFormItem>
          <NFormItem label="合格下限" path="minValue">
            <NInput v-model:value="model.minValue" placeholder="留空表示不设下限（可写 <0.5）" />
          </NFormItem>
          <NFormItem label="合格上限" path="maxValue">
            <NInput v-model:value="model.maxValue" placeholder="留空表示不设上限" />
          </NFormItem>
          <NFormItem label="单位" path="unit">
            <NInput v-model:value="model.unit" placeholder="如 %、X" />
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
