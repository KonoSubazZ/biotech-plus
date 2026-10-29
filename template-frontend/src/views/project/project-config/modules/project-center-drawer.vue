<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { fetchCreateProjectCenter, fetchUpdateProjectCenter } from '@/service/api/project/project-config';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ProjectCenterDrawer'
});

interface Props {
  /** 所属项目行 */
  project: Api.Project.ProjectConfig;
  /** 编辑时的研究中心行；为空表示新增 */
  rowData?: Api.Project.ProjectConfig | null;
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

const isEdit = computed(() => Boolean(props.rowData?.id));

const title = computed(() => (isEdit.value ? '编辑研究中心' : '新增研究中心'));

const model = ref<Api.Project.ProjectCenterForm>(createDefaultModel());

function createDefaultModel(): Api.Project.ProjectCenterForm {
  return {
    parentId: props.project.id,
    researchCenterNo: '',
    researchCenterName: '',
    receiveEmail: '',
    ccEmail: null,
    remark: null
  };
}

const rules: Record<string, App.Global.FormRule> = {
  researchCenterNo: createRequiredRule('研究中心编号不能为空'),
  researchCenterName: createRequiredRule('研究中心名称不能为空'),
  receiveEmail: createRequiredRule('收件邮箱不能为空')
};

function handleUpdateModelWhenEdit() {
  model.value = createDefaultModel();

  if (isEdit.value && props.rowData) {
    model.value.id = props.rowData.id;
    model.value.researchCenterNo = props.rowData.researchCenterNo ?? '';
    model.value.researchCenterName = props.rowData.researchCenterName ?? '';
    model.value.receiveEmail = props.rowData.receiveEmail ?? '';
    model.value.ccEmail = props.rowData.ccEmail ?? null;
    model.value.remark = props.rowData.remark ?? null;
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
    if (isEdit.value) {
      const { error } = await fetchUpdateProjectCenter(model.value);
      if (error) return;
      window.$message?.success($t('common.updateSuccess'));
    } else {
      const { error } = await fetchCreateProjectCenter(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
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
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="520" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules">
        <div class="grid grid-cols-1 gap-16px md:grid-cols-2">
          <NFormItem class="md:col-span-2" label="所属项目">
            <NInput :value="`${project.projectNo} / ${project.projectName}`" disabled />
          </NFormItem>
          <NFormItem label="研究中心编号" path="researchCenterNo">
            <NInput v-model:value="model.researchCenterNo" placeholder="如 01；与名称成对，录入后不要随意改" />
          </NFormItem>
          <NFormItem label="研究中心名称" path="researchCenterName">
            <NInput v-model:value="model.researchCenterName" placeholder="与样本上的送检机构保持一致" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="报告接收邮箱" path="receiveEmail">
            <NInput v-model:value="model.receiveEmail" placeholder="多个邮箱用分号分隔，如 a@x.com;b@y.com" />
          </NFormItem>
          <!-- 抄送邮箱已隐藏（2026-10-29）：字段与历史数据保留，页面不再维护 -->
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
