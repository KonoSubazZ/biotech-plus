<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { fetchCreateTenant, fetchUpdateTenant } from '@/service/api/system/tenant';
import { useNaiveForm, useFormRules } from '@/hooks/common/form';
import { $t } from '@/locales';

const props = defineProps<{ operateType: NaiveUI.TableOperateType; rowData?: Api.System.Tenant | null }>();
const emit = defineEmits<{ submitted: [] }>();
const visible = defineModel<boolean>('visible', { default: false });
const { formRef, validate, restoreValidation } = useNaiveForm();
const { createRequiredRule } = useFormRules();
const saving = ref(false);
const model = ref<Api.System.TenantOperateParams>({ tenantId: '', tenantName: '', status: '0', remark: '' });
const title = computed(() =>
  $t(props.operateType === 'add' ? 'page.system.tenant.addTenant' : 'page.system.tenant.editTenant')
);
const rules = {
  tenantId: [
    createRequiredRule($t('page.system.tenant.identifierHint')),
    { pattern: /^[\w-]{1,20}$/, message: $t('page.system.tenant.identifierHint'), trigger: 'blur' }
  ],
  tenantName: createRequiredRule($t('page.system.tenant.form.tenantName.invalid')),
  status: createRequiredRule($t('page.system.tenant.form.status.invalid'))
};

watch(visible, show => {
  if (!show) return;
  model.value =
    props.operateType === 'edit' && props.rowData
      ? {
          id: props.rowData.id,
          tenantId: props.rowData.tenantId,
          tenantName: props.rowData.tenantName,
          status: props.rowData.status,
          remark: props.rowData.remark ?? ''
        }
      : { tenantId: '', tenantName: '', status: '0', remark: '' };
  restoreValidation();
});

async function save() {
  if (saving.value) return;
  model.value.tenantId = model.value.tenantId?.trim() ?? '';
  model.value.tenantName = model.value.tenantName?.trim() ?? '';
  await validate();
  saving.value = true;
  try {
    const { error } =
      props.operateType === 'add' ? await fetchCreateTenant(model.value) : await fetchUpdateTenant(model.value);
    if (error) return;
    window.$message?.success($t(props.operateType === 'add' ? 'common.addSuccess' : 'common.updateSuccess'));
    visible.value = false;
    emit('submitted');
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <NDrawer v-model:show="visible" :width="420" class="max-w-90%" :close-on-esc="!saving" :mask-closable="!saving">
    <NDrawerContent :title="title" :native-scrollbar="false" :closable="!saving">
      <NForm ref="formRef" :model="model" :rules="rules" novalidate>
        <NFormItem :label="$t('page.system.tenant.tenantId')" path="tenantId">
          <NInput v-model:value="model.tenantId" :disabled="operateType === 'edit'" :maxlength="20" />
        </NFormItem>
        <NText depth="3">{{ $t('page.system.tenant.identifierHint') }}</NText>
        <NFormItem class="mt-16px" :label="$t('page.system.tenant.tenantName')" path="tenantName">
          <NInput v-model:value="model.tenantName" :maxlength="100" />
        </NFormItem>
        <NFormItem :label="$t('page.system.tenant.status')" path="status">
          <DictRadio
            v-model:value="model.status"
            dict-code="sys_normal_disable"
            :disabled="model.tenantId === '000000'"
          />
        </NFormItem>
        <NFormItem :label="$t('page.system.user.remark')" path="remark">
          <NInput
            v-model:value="model.remark"
            type="textarea"
            :maxlength="500"
            :autosize="{ minRows: 3, maxRows: 6 }"
            class="resize-none"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace :size="16">
          <NButton :disabled="saving" @click="visible = false">{{ $t('common.cancel') }}</NButton>
          <NButton type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>
