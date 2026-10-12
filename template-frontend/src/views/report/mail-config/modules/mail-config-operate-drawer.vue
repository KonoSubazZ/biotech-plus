<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import {
  fetchAddReportMailConfig,
  fetchGetReportMailConfig,
  fetchGetReportMailVariables,
  fetchUpdateReportMailConfig
} from '@/service/api/report/mail-config';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';

defineOptions({
  name: 'ReportMailConfigOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据（只有 id 一定在，其余字段从详情接口回填） */
  rowData?: Api.Report.ReportMailConfig | null;
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
const loading = ref(false);

/** 空的表单模型：服务器/端口/SSL/昵称留空 → 后端用代码默认值兜底 */
const EMPTY_FORM: Api.Report.ReportMailConfigForm = {
  id: undefined,
  enabled: '1',
  isDefault: '0',
  smtpHost: '',
  smtpPort: 465,
  sslEnable: '1',
  fromEmail: '',
  fromName: '',
  password: '',
  defaultSubject: '',
  defaultContent: ''
};

const model = ref<Api.Report.ReportMailConfigForm>(jsonClone(EMPTY_FORM));

/** 该行是否已设置密码（决定密码框的占位提示） */
const passwordSet = ref(false);

/** 可用变量 + 样例值（都由后端给，前端不自己维护清单） */
const mailVars = ref<string[]>([]);
const mailSample = ref<Record<string, string>>({});

const title = computed(() => (props.operateType === 'add' ? '新增发件邮箱' : '编辑发件邮箱'));

const rules = {
  fromEmail: [
    createRequiredRule('发件邮箱不能为空'),
    {
      type: 'email' as const,
      message: '发件邮箱格式不正确',
      trigger: ['blur', 'input']
    }
  ]
};

/** 试算：把 {{x}} 换成样例值；未知变量原样显示（仅示意，真正校验在后端保存时） */
function tryout(text: string | undefined) {
  if (!text) {
    return '';
  }
  return text.replace(/\{\{\s*([A-Za-z0-9_]+)\s*\}\}/g, (all, key: string) =>
    key in mailSample.value ? mailSample.value[key] : all
  );
}

const smtpSummary = computed(() =>
  model.value.smtpHost ? `${model.value.smtpHost}:${model.value.smtpPort ?? ''}` : '后端默认 smtp.exmail.qq.com:465'
);

/** 打开抽屉时：add 用空模型，edit 拉详情回填（密码永远留空 = 不修改） */
async function init() {
  restoreValidation();
  loading.value = true;
  try {
    const variables = await fetchGetReportMailVariables();
    if (!variables.error && variables.data) {
      mailVars.value = variables.data.variables ?? [];
      mailSample.value = variables.data.sample ?? {};
    }
    if (props.operateType === 'add') {
      model.value = jsonClone(EMPTY_FORM);
      passwordSet.value = false;
      return;
    }
    const id = props.rowData?.id;
    if (!id) {
      return;
    }
    const { data, error } = await fetchGetReportMailConfig(id);
    if (error || !data) {
      return;
    }
    model.value = {
      id: data.id ?? undefined,
      enabled: data.enabled ?? '0',
      isDefault: data.isDefault ?? '0',
      smtpHost: data.smtpHost ?? '',
      smtpPort: data.smtpPort ?? 465,
      sslEnable: data.sslEnable ?? '1',
      fromEmail: data.fromEmail ?? '',
      fromName: data.fromName ?? '',
      password: '',
      defaultSubject: data.defaultSubject ?? '',
      defaultContent: data.defaultContent ?? ''
    };
    passwordSet.value = data.passwordSet === true;
  } finally {
    loading.value = false;
  }
}

watch(visible, value => {
  if (value) {
    init();
  }
});

async function handleSubmit() {
  await validate();
  saving.value = true;
  try {
    const isAdd = props.operateType === 'add';
    const request = isAdd ? fetchAddReportMailConfig : fetchUpdateReportMailConfig;
    const { error } = await request({ ...model.value });
    if (error) {
      return;
    }
    window.$message?.success(isAdd ? '发件邮箱已新增' : '发件邮箱已保存');
    visible.value = false;
    emit('submitted');
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="760" class="max-w-92%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NSpin :show="loading">
        <NForm ref="formRef" :model="model" :rules="rules" label-placement="left" :label-width="120">
          <NFormItem label="发件邮箱" path="fromEmail">
            <NInput v-model:value="model.fromEmail" placeholder="如 novomedicine-report@novogene.com" clearable />
          </NFormItem>

          <NFormItem label="发件人昵称">
            <NInput v-model:value="model.fromName" placeholder="留空用默认：诺禾致源报告" clearable />
          </NFormItem>

          <NFormItem label="邮箱密码">
            <NInput
              v-model:value="model.password"
              type="password"
              show-password-on="click"
              :placeholder="passwordSet ? '已设置，留空表示不修改' : '请输入邮箱密码 / 客户端授权码'"
            />
          </NFormItem>

          <NFormItem label="启用">
            <NSpace align="center" :size="12">
              <NSwitch
                :value="model.enabled === '1'"
                @update:value="(value: boolean) => (model.enabled = value ? '1' : '0')"
              />
              <span class="text-12px op-60">停用后报告发送页会把它置灰，不能选来发信</span>
            </NSpace>
          </NFormItem>

          <NFormItem label="默认邮箱">
            <NSpace align="center" :size="12">
              <NSwitch
                :value="model.isDefault === '1'"
                @update:value="(value: boolean) => (model.isDefault = value ? '1' : '0')"
              />
              <span class="text-12px op-60">报告发送页默认选中的就是它（一个租户只能有一个默认）</span>
            </NSpace>
          </NFormItem>

          <NDivider class="my-8px!">SMTP（留空用后端默认 smtp.exmail.qq.com:465 + SSL）</NDivider>

          <NFormItem label="SMTP 服务器">
            <NInput v-model:value="model.smtpHost" placeholder="smtp.exmail.qq.com" clearable />
          </NFormItem>

          <NFormItem label="SMTP 端口">
            <NInputNumber v-model:value="model.smtpPort" class="w-full" :min="1" :max="65535" />
          </NFormItem>

          <NFormItem label="SSL 加密">
            <NSwitch
              :value="model.sslEnable === '1'"
              @update:value="(value: boolean) => (model.sslEnable = value ? '1' : '0')"
            />
          </NFormItem>

          <NDivider class="my-8px!">默认标题 / 正文</NDivider>

          <NFormItem label="默认标题">
            <NInput
              v-model:value="model.defaultSubject"
              placeholder="请查收诺禾致源的检测报告，姓名：{{patientName}}-{{subbarcode}}"
            />
          </NFormItem>

          <div class="mb-12px pl-120px text-12px leading-20px op-60">
            可用变量：{{ mailVars.length ? mailVars.join('、') : '加载中…' }}
            <br />
            当前连接：{{ smtpSummary }}
            <br />
            标题试算：{{ tryout(model.defaultSubject) || '（留空 = 用系统默认标题）' }}
          </div>

          <NFormItem label="默认正文">
            <NInput
              v-model:value="model.defaultContent"
              type="textarea"
              :autosize="{ minRows: 5, maxRows: 12 }"
              placeholder="支持 HTML 片段，换行可用 <br>；留空 = 用系统默认正文"
            />
          </NFormItem>

          <div class="mb-8px pl-120px text-12px leading-20px op-60">
            正文试算：{{ tryout(model.defaultContent) || '（留空 = 用系统默认正文）' }}
          </div>
        </NForm>
      </NSpin>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="visible = false">取消</NButton>
          <NButton type="primary" :loading="saving" @click="handleSubmit">保存</NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
