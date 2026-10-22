<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchCreateProjectConfig, fetchUpdateProjectConfig } from '@/service/api/project/project-config';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ProjectConfigOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据 */
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

const title = computed(() => {
  const titles: Record<NaiveUI.TableOperateType, string> = {
    add: '新增项目配置',
    edit: '编辑项目配置'
  };
  return titles[props.operateType];
});

const model = ref<Api.Project.ProjectConfigForm>(createDefaultModel());

function createDefaultModel(): Api.Project.ProjectConfigForm {
  return {
    projectNo: '',
    projectName: '',
    customer: '',
    receiveEmail: '',
    ccEmail: null,
    remark: null
  };
}

const rules = {
  projectNo: createRequiredRule('项目编号不能为空'),
  projectName: createRequiredRule('项目名称不能为空'),
  customer: createRequiredRule('客户名称不能为空'),
  receiveEmail: createRequiredRule('收件邮箱不能为空')
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
      const { error } = await fetchCreateProjectConfig(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.operateType === 'edit') {
      const { error } = await fetchUpdateProjectConfig(model.value);
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
          <NFormItem label="项目编号" path="projectNo">
            <!-- 项目编号是业务主键：建档后不可改（后端也会拒绝不同值），只允许编辑时只读展示 -->
            <NInput
              v-model:value="model.projectNo"
              :disabled="operateType === 'edit'"
              :placeholder="operateType === 'edit' ? '项目编号建档后不可修改' : '请输入项目编号'"
            />
          </NFormItem>
          <NFormItem label="项目名称" path="projectName">
            <NInput v-model:value="model.projectName" placeholder="请输入项目名称" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="客户名称" path="customer">
            <NInput v-model:value="model.customer" placeholder="请输入客户名称" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="收件邮箱" path="receiveEmail">
            <NInput v-model:value="model.receiveEmail" placeholder="多个邮箱用分号分隔，如 a@x.com;b@y.com" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="抄送邮箱" path="ccEmail">
            <NInput v-model:value="model.ccEmail" placeholder="可空；多个邮箱用分号分隔" />
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
