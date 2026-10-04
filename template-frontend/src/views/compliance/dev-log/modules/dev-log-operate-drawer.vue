<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { UploadFileInfo } from 'naive-ui';
import { jsonClone } from '@sa/utils';
import { fetchCreateDevLog, fetchUpdateDevLog } from '@/service/api/compliance/dev-log';
import { getToken } from '@/store/modules/auth/shared';
import { getServiceBaseURL } from '@/utils/service';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'DevLogOperateDrawer'
});

interface Props {
  /** 操作类型：add / edit */
  operateType: NaiveUI.TableOperateType;
  /** 编辑时的行数据 */
  rowData?: Api.Compliance.DevLog | null;
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

const title = computed(() => (props.operateType === 'add' ? '新增开发记录' : '编辑开发记录'));

/** 分类（单选用 NSelect） */
const categoryOptions = [
  { label: '功能新增', value: 'feature' },
  { label: '缺陷修复', value: 'fix' },
  { label: '变更调整', value: 'change' }
];

const model = ref<Api.Compliance.DevLogForm>(createDefaultModel());

// ---------------------------------------------------------------- 记录文档上传
const { baseURL } = getServiceBaseURL(import.meta.env);
// 附件走本模块自己的上传接口（存服务器固定目录），不用 OSS
const uploadAction = `${baseURL}/compliance/devLog/upload`;
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
    window.$message?.error(response.msg || '记录文档上传失败');
    return file;
  }
  model.value.fileName = response.data.fileName;
  window.$message?.success('记录文档上传成功');
  return file;
}

function handleUploadError(options: { file: UploadFileInfo; event?: ProgressEvent }) {
  const { event } = options;
  // @ts-expect-error Ignore type errors
  const responseText = event?.target?.responseText;
  const msg = JSON.parse(responseText).msg;
  window.$message?.error(msg || '记录文档上传失败');
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

function createDefaultModel(): Api.Compliance.DevLogForm {
  return {
    title: '',
    category: 'feature',
    version: null,
    content: null,
    fileName: null,
    developer: null,
    logDate: null
  };
}

const rules = {
  title: createRequiredRule('记录标题不能为空'),
  category: createRequiredRule('分类不能为空')
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
      const { error } = await fetchCreateDevLog(model.value);
      if (error) return;
      window.$message?.success($t('common.addSuccess'));
    }

    if (props.operateType === 'edit') {
      const { error } = await fetchUpdateDevLog(model.value);
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
          <NFormItem class="md:col-span-2" label="记录标题" path="title">
            <NInput v-model:value="model.title" placeholder="请输入记录标题" />
          </NFormItem>
          <NFormItem label="分类" path="category">
            <NSelect v-model:value="model.category" :options="categoryOptions" placeholder="请选择分类" />
          </NFormItem>
          <NFormItem label="版本" path="version">
            <NInput v-model:value="model.version" placeholder="如 v1.0" />
          </NFormItem>
          <NFormItem label="开发人员" path="developer">
            <NInput v-model:value="model.developer" placeholder="请输入开发人员" />
          </NFormItem>
          <NFormItem label="记录日期" path="logDate">
            <NDatePicker
              v-model:formatted-value="model.logDate"
              type="date"
              value-format="yyyy-MM-dd"
              clearable
              placeholder="请选择记录日期"
            />
          </NFormItem>
          <NFormItem class="md:col-span-2" label="记录文档" path="fileName">
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
                <NButton>{{ model.fileName ? '重新上传' : '上传记录文档' }}</NButton>
              </NUpload>
              <NText depth="3" class="mt-4px text-12px">同名文件直接替换；列表里只显示文件名，点击文件名可下载。</NText>
            </div>
          </NFormItem>
          <NFormItem class="md:col-span-2" label="详细内容" path="content">
            <NInput
              v-model:value="model.content"
              type="textarea"
              :autosize="{ minRows: 4, maxRows: 10 }"
              placeholder="请输入详细内容"
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
