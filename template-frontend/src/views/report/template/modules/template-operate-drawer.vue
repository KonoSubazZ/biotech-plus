<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import {
  fetchCreateReportTemplate,
  fetchGetProductOptions,
  fetchUpdateReportTemplate
} from '@/service/api/report/template';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ReportTemplateOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据 */
  rowData?: Api.Report.ReportTemplate | null;
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

const productOptions = ref<Api.Report.ProductOption[]>([]);

const productSelectOptions = computed(() =>
  productOptions.value.map(item => ({
    label: `${item.name}（${item.code}）`,
    value: item.id
  }))
);

/** 空的表单模型：moduleCode 留空 = 只输出公共字段 */
const EMPTY_FORM: Api.Report.ReportTemplateForm = {
  templateId: null,
  templateCode: '',
  templateName: '',
  templateVersion: 'v1',
  customerCode: '',
  reportType: 'SOMATIC',
  moduleCode: '',
  templatePath: '',
  templateSha256: '',
  status: 'ENABLED',
  productIds: []
};

const model = ref<Api.Report.ReportTemplateForm>(jsonClone(EMPTY_FORM));

const rules = {
  templateCode: createRequiredRule('模板编码不能为空'),
  templateName: createRequiredRule('模板名称不能为空'),
  reportType: createRequiredRule('报告类型不能为空'),
  templatePath: createRequiredRule('模板文件路径不能为空'),
  status: createRequiredRule('状态不能为空')
};

const title = computed(() => (props.operateType === 'add' ? '新增报告模板' : '编辑报告模板'));

/** 已注册的个性化模块编码（占位提示用；以交付说明为准） */
const MODULE_CODE_HINT = 'SHENGYU_SOMATIC_VARIANTS_V1;SHENGYU_GERMLINE_VARIANTS_V1;SHENGYU_QC_V1';

function handleUpdateModelWhenEdit() {
  model.value = jsonClone(EMPTY_FORM);
  if (props.operateType === 'edit' && props.rowData) {
    const row = jsonClone(props.rowData);
    model.value = {
      ...jsonClone(EMPTY_FORM),
      templateId: row.templateId,
      templateCode: row.templateCode ?? '',
      templateName: row.templateName ?? '',
      templateVersion: row.templateVersion ?? 'v1',
      customerCode: row.customerCode ?? '',
      reportType: row.reportType ?? 'SOMATIC',
      moduleCode: row.moduleCode ?? '',
      templatePath: row.templatePath ?? '',
      templateSha256: row.templateSha256 ?? '',
      status: row.status ?? 'ENABLED',
      productIds: row.productIds ?? []
    };
  }
}

async function loadProductOptions() {
  if (productOptions.value.length) return;
  const { data: options, error } = await fetchGetProductOptions();
  if (error) return;
  productOptions.value = options ?? [];
}

function closeDrawer() {
  visible.value = false;
}

async function handleSubmit() {
  await validate();

  saving.value = true;
  try {
    if (props.operateType === 'add') {
      const { error } = await fetchCreateReportTemplate(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.operateType === 'edit') {
      const { error } = await fetchUpdateReportTemplate(model.value);
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
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="720" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules" label-placement="left" :label-width="130">
        <NFormItem label="模板编码" path="templateCode">
          <NInput v-model:value="model.templateCode" placeholder="稳定编码，如 pharma-shengyu（同租户唯一）" clearable />
        </NFormItem>
        <NFormItem label="模板名称" path="templateName">
          <NInput v-model:value="model.templateName" placeholder="如：圣域 1238 报告" clearable />
        </NFormItem>
        <NFormItem label="模板版本" path="templateVersion">
          <NInput v-model:value="model.templateVersion" placeholder="如 v1" clearable />
        </NFormItem>
        <NFormItem label="报告类型" path="reportType">
          <NSelect
            v-model:value="model.reportType"
            :options="[
              { label: '体细胞（SOMATIC）', value: 'SOMATIC' },
              { label: '胚系（GERMLINE）', value: 'GERMLINE' },
              { label: '其他（OTHER）', value: 'OTHER' }
            ]"
            filterable
          />
        </NFormItem>
        <NFormItem label="客户编码" path="customerCode">
          <NInput v-model:value="model.customerCode" placeholder="可选；留空表示不限客户" clearable />
        </NFormItem>
        <NFormItem label="关联产品" path="productIds">
          <NSelect
            v-model:value="model.productIds"
            :options="productSelectOptions"
            multiple
            filterable
            clearable
            placeholder="选择该模板可用的产品（可多选）"
          />
        </NFormItem>
        <NFormItem label="输出范围" path="moduleCode">
          <NInput
            v-model:value="model.moduleCode"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            :placeholder="`分号分隔的有序个性化模块编码，顺序即执行顺序；留空 = 只输出公共字段。已注册：${MODULE_CODE_HINT}`"
          />
        </NFormItem>
        <NFormItem label="模板文件路径" path="templatePath">
          <NInput
            v-model:value="model.templatePath"
            placeholder="DOCX 实体文件路径，如 report-templates/pharma-shengyu/v1/template.docx（本轮只登记路径，不渲染）"
          />
        </NFormItem>
        <NFormItem label="模板校验值" path="templateSha256">
          <NInput v-model:value="model.templateSha256" placeholder="可选：模板文件 SHA-256" clearable />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NSelect
            v-model:value="model.status"
            :options="[
              { label: '启用', value: 'ENABLED' },
              { label: '停用', value: 'DISABLED' }
            ]"
          />
        </NFormItem>
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
