<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { UploadFileInfo } from 'naive-ui';
import { jsonClone } from '@sa/utils';
import {
  fetchCreateValidationRecord,
  fetchUpdateValidationRecord
} from '@/service/api/compliance/validation-record';
import { getToken } from '@/store/modules/auth/shared';
import { getServiceBaseURL } from '@/utils/service';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ValidationRecordOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据 */
  rowData?: Api.Compliance.ValidationRecord | null;
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

const title = computed(() => (props.operateType === 'add' ? '新增验证记录' : '编辑验证记录'));

/** 验证类型（单选用 NSelect） */
const typeOptions = [
  { label: 'IQ 安装确认', value: 'IQ' },
  { label: 'OQ 运行确认', value: 'OQ' },
  { label: 'PQ 性能确认', value: 'PQ' }
];

/** 结果 */
const resultOptions = [
  { label: '通过', value: 'passed' },
  { label: '未通过', value: 'failed' },
  { label: '不适用', value: 'na' }
];

const model = ref<Api.Compliance.ValidationRecordForm>(createDefaultModel());

// ---------------------------------------------------------------- 验证文档上传
const { baseURL } = getServiceBaseURL(import.meta.env);
// 附件走本模块自己的上传接口（存服务器固定目录），不用 OSS
const uploadAction = `${baseURL}/compliance/validationRecord/upload`;
const uploadHeaders: Record<string, string> = {
  Authorization: `Bearer ${getToken()}`,
  clientid: import.meta.env.VITE_APP_CLIENT_ID!
};

const uploadFileList = ref<UploadFileInfo[]>([]);

function isUploadErrorState(xhr: XMLHttpRequest) {
  const response = JSON.parse(xhr?.responseText);
  return response.code !== 200;
}

function handleUploadFinish(options: { file: UploadFileInfo; event?: ProgressEvent }) {
  const { file, event } = options;
  // @ts-expect-error Ignore type errors
  const responseText = event?.target?.responseText;
  const response = JSON.parse(responseText);
  if (response.code !== 200) {
    window.$message?.error(response.msg || '验证文档上传失败');
    return file;
  }
  model.value.fileName = response.data.fileName;
  window.$message?.success('验证文档上传成功');
  return file;
}

function handleUploadError(options: { file: UploadFileInfo; event?: ProgressEvent }) {
  const { event } = options;
  // @ts-expect-error Ignore type errors
  const responseText = event?.target?.responseText;
  const msg = JSON.parse(responseText).msg;
  window.$message?.error(msg || '验证文档上传失败');
}

/**
 * 移除只是取消本次选择：文件按「重名替换」策略留在服务器固定目录（没有删除接口），
 * 只把表单里的文件名清掉。
 */
function handleUploadRemove() {
  model.value.fileName = null;
  return true;
}

/** 打开抽屉时把已保存的文件名回显成一条已完成的列表项 */
function syncUploadFileList() {
  const fileName = model.value.fileName;
  const item: UploadFileInfo = { id: fileName ?? '', name: fileName ?? '', status: 'finished' };
  uploadFileList.value = fileName ? [item] : [];
}

function createDefaultModel(): Api.Compliance.ValidationRecordForm {
  return {
    validationType: 'IQ',
    title: '',
    version: null,
    executedBy: null,
    executedDate: null,
    result: 'passed',
    summary: null,
    fileName: null
  };
}

const rules = {
  validationType: createRequiredRule('验证类型不能为空'),
  title: createRequiredRule('验证标题不能为空'),
  result: createRequiredRule('结果不能为空')
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
      const { error } = await fetchCreateValidationRecord(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.operateType === 'edit') {
      const { error } = await fetchUpdateValidationRecord(model.value);
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
    syncUploadFileList();
  }
});
</script>

<template>
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="600" class="max-w-90%">
    <NDrawerContent :title="title" :native-scrollbar="false" closable>
      <NForm ref="formRef" :model="model" :rules="rules">
        <div class="grid grid-cols-1 gap-16px md:grid-cols-2">
          <NFormItem label="验证类型" path="validationType">
            <NSelect v-model:value="model.validationType" :options="typeOptions" placeholder="请选择验证类型" />
          </NFormItem>
          <NFormItem label="结果" path="result">
            <NSelect v-model:value="model.result" :options="resultOptions" placeholder="请选择结果" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="验证标题" path="title">
            <NInput v-model:value="model.title" placeholder="请输入验证标题" />
          </NFormItem>
          <NFormItem label="版本" path="version">
            <NInput v-model:value="model.version" placeholder="本次验证对应的方案/镜像版本，如 v2.3.0" />
          </NFormItem>
          <NFormItem label="执行人" path="executedBy">
            <NInput v-model:value="model.executedBy" placeholder="请输入执行人" />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="执行日期" path="executedDate">
            <NDatePicker
              v-model:formatted-value="model.executedDate"
              type="date"
              value-format="yyyy-MM-dd"
              clearable
              placeholder="请选择执行日期"
            />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="验证文档" path="fileName">
            <div class="w-full">
              <NUpload
                v-model:file-list="uploadFileList"
                :action="uploadAction"
                :headers="uploadHeaders"
                :max="1"
                :file-size="20"
                :multiple="false"
                list-type="text"
                :is-error-state="isUploadErrorState"
                @finish="handleUploadFinish"
                @error="handleUploadError"
                @remove="handleUploadRemove"
              >
                <NButton>{{ model.fileName ? '重新上传' : '上传验证文档' }}</NButton>
              </NUpload>
              <NText depth="3" class="mt-4px text-12px">
                上传本次验证的方案/测试记录/报告；同名文件直接替换，列表里点文件名可下载。
              </NText>
            </div>
          </NFormItem>
          <NFormItem class="md:col-span-2" label="验证摘要" path="summary">
            <NInput
              v-model:value="model.summary"
              type="textarea"
              :autosize="{ minRows: 4, maxRows: 10 }"
              placeholder="请输入验证摘要（做了哪些项、结论、有无偏差）"
            />
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
