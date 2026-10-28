<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { useLoading } from '@sa/hooks';
import { fetchCreateUser, fetchGetUserInfo, fetchUpdateUser } from '@/service/api/system';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';

defineOptions({
  name: 'UserOperateDrawer'
});

interface Props {
  /** the type of operation */
  operateType: NaiveUI.TableOperateType;
  /** the edit row data */
  rowData?: Api.System.User | null;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', {
  default: false
});

const { loading, startLoading, endLoading } = useLoading();
const { formRef, validate, restoreValidation } = useNaiveForm();
const { createRequiredRule, patternRules } = useFormRules();
const authStore = useAuthStore();

const title = computed(() => {
  const titles: Record<NaiveUI.TableOperateType, string> = {
    add: $t('page.system.user.addUser'),
    edit: $t('page.system.user.editUser')
  };
  return titles[props.operateType];
});

type Model = Api.System.UserOperateParams;

const model = ref<Model>(createDefaultModel());

const roleOptions = ref<CommonType.Option<CommonType.IdType>[]>([]);

function createDefaultModel(): Model {
  return {
    // 单药企模式：页面不再展示租户，统一继承当前登录用户所在租户（超管即 000000）
    tenantId: authStore.userInfo.user?.tenantId ?? '000000',
    userName: '',
    nickName: '',
    email: '',
    phonenumber: '',
    sex: '0',
    password: '',
    status: '0',
    roleIds: [],
    remark: ''
  };
}

type RuleKey = Extract<
  keyof Model,
  'userName' | 'nickName' | 'password' | 'status' | 'phonenumber' | 'roleIds'
>;

const rules: Record<RuleKey, App.Global.FormRule[]> = {
  userName: [createRequiredRule($t('page.system.user.form.userName.required'))],
  nickName: [createRequiredRule($t('page.system.user.form.nickName.required'))],
  password: [{ ...patternRules.pwd, required: props.operateType === 'add' }],
  phonenumber: [patternRules.phone],
  status: [createRequiredRule($t('page.system.user.form.status.required'))],
  roleIds: [{ ...createRequiredRule('请选择角色'), type: 'array' }]
};

async function getUserInfo(id: CommonType.IdType = '') {
  startLoading();
  const { error, data } = await fetchGetUserInfo(id);
  if (!error) {
    model.value.roleIds = data.roleIds;
    roleOptions.value = data.roles.map(role => ({
      label: role.roleName,
      value: role.roleId
    }));
  }
  endLoading();
}

function handleUpdateModelWhenEdit() {
  model.value = createDefaultModel();

  if (props.operateType === 'add') {
    getUserInfo();
    return;
  }

  if (props.operateType === 'edit' && props.rowData) {
    Object.assign(model.value, jsonClone(props.rowData));
    model.value.password = '';
    getUserInfo(props.rowData.userId);
  }
}

function closeDrawer() {
  visible.value = false;
}

async function handleSubmit() {
  await validate();

  const { tenantId, userId, userName, nickName, email, phonenumber, sex, password, status, roleIds, remark } =
    model.value;

  // request
  if (props.operateType === 'add') {
    const { error } = await fetchCreateUser({
      tenantId,
      userName,
      password,
      nickName,
      email,
      phonenumber,
      sex,
      status,
      roleIds,
      remark
    });
    if (error) return;
    window.$message?.success($t('common.addSuccess'));
  }

  if (props.operateType === 'edit') {
    const { error } = await fetchUpdateUser({
      tenantId,
      userId,
      userName,
      nickName,
      email,
      phonenumber,
      sex,
      status,
      roleIds,
      remark
    });
    if (error) return;
    window.$message?.success($t('common.updateSuccess'));
  }

  closeDrawer();
  emit('submitted');
}

watch(visible, () => {
  if (visible.value) {
    handleUpdateModelWhenEdit();
    restoreValidation();
  }
});
</script>

<template>
  <NDrawer v-model:show="visible" display-directive="show" :width="800" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NSpin :show="loading">
        <NForm ref="formRef" :model="model" :rules="rules" novalidate>
          <NFormItem :label="$t('page.system.user.nickName')" path="nickName">
            <NInput v-model:value="model.nickName" :placeholder="$t('page.system.user.form.nickName.required')" />
          </NFormItem>
          <NFormItem :label="$t('page.system.user.phonenumber')" path="phonenumber">
            <NInput v-model:value="model.phonenumber" :placeholder="$t('page.system.user.form.phonenumber.required')" />
          </NFormItem>
          <NFormItem :label="$t('page.system.user.email')" path="email">
            <NInput v-model:value="model.email" :placeholder="$t('page.system.user.form.email.required')" />
          </NFormItem>
          <NFormItem v-if="operateType === 'add'" :label="$t('page.system.user.userName')" path="userName">
            <NInput v-model:value="model.userName" :placeholder="$t('page.system.user.form.userName.required')" />
          </NFormItem>
          <NFormItem v-if="operateType === 'add'" :label="$t('page.system.user.password')" path="password">
            <NInput
              v-model:value="model.password"
              type="password"
              show-password-on="click"
              :input-props="{ autocomplete: 'off' }"
              :placeholder="$t('page.system.user.form.password.required')"
            />
          </NFormItem>
          <NFormItem :label="$t('page.system.user.sex')" path="sex">
            <DictRadio
              v-model:value="model.sex"
              dict-code="sys_user_sex"
              :placeholder="$t('page.system.user.form.sex.required')"
            />
          </NFormItem>
          <NFormItem :label="$t('page.system.user.roleIds')" path="roleIds">
            <NSelect
              v-model:value="model.roleIds"
              :loading="loading"
              :options="roleOptions"
              multiple
              clearable
              placeholder="请选择角色"
            />
          </NFormItem>
          <NFormItem :label="$t('page.system.user.status')" path="status">
            <DictRadio v-model:value="model.status" dict-code="sys_normal_disable" />
          </NFormItem>
          <NFormItem :label="$t('page.system.user.remark')" path="remark">
            <NInput v-model:value="model.remark" :placeholder="$t('page.system.user.form.remark.required')" />
          </NFormItem>
        </NForm>
      </NSpin>
      <template #footer>
        <NSpace :size="16">
          <NButton @click="closeDrawer">{{ $t('common.cancel') }}</NButton>
          <NButton type="primary" @click="handleSubmit">{{ $t('common.save') }}</NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
