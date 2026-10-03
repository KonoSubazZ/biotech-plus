<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { UploadFileInfo } from 'naive-ui';
import { getToken } from '@/store/modules/auth/shared';
import { useDownload } from '@/hooks/business/download';
import { getServiceBaseURL } from '@/utils/service';
import type FileUpload from '@/components/custom/file-upload.vue';
import { $t } from '@/locales';

defineOptions({
  name: 'SampleInfoImportModal'
});

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const { download } = useDownload();

const { baseURL } = getServiceBaseURL(import.meta.env);

const headers: Record<string, string> = {
  Authorization: `Bearer ${getToken()}`,
  clientid: import.meta.env.VITE_APP_CLIENT_ID!
};

const uploadRef = ref<typeof FileUpload>();

const visible = defineModel<boolean>('visible', { default: false });

const fileList = ref<UploadFileInfo[]>([]);
const importing = ref(false);
const result = ref<Api.Report.SampleInfoImportResult | null>(null);

/** 有导入成功的行就算成功；全失败（只有 errors）按错误提示 */
const succeeded = computed(() => {
  const data = result.value;
  return data !== null && data.inserted + data.updated > 0;
});

const resultSummary = computed(() => {
  const data = result.value;
  if (!data) return '';
  const lines = [`共 ${data.total} 行：新增 ${data.inserted} 行，更新 ${data.updated} 行，失败 ${data.failed} 行`];
  if (data.errors.length > 0) {
    lines.push('失败明细：');
    lines.push(...data.errors);
  }
  return lines.join('\n');
});

function closeModal() {
  visible.value = false;
  if (succeeded.value) {
    emit('submitted');
  }
}

function handleSubmit() {
  if (fileList.value.length === 0) {
    window.$message?.warning('请先选择要导入的 Excel 文件');
    return;
  }
  fileList.value.forEach(item => {
    item.status = 'pending';
  });
  importing.value = true;
  uploadRef.value?.submit();
}

function isErrorState(xhr: XMLHttpRequest) {
  const response = JSON.parse(xhr?.responseText);
  return response.code !== 200;
}

function handleFinish(options: { file: UploadFileInfo; event?: ProgressEvent }) {
  const { file, event } = options;
  importing.value = false;
  // @ts-expect-error Ignore type errors
  const response = JSON.parse(event?.target?.responseText);
  result.value = response.data as Api.Report.SampleInfoImportResult;
  window.$message?.success($t('common.importSuccess'));
  return file;
}

function handleError(options: { file: UploadFileInfo; event?: ProgressEvent }) {
  const { event } = options;
  importing.value = false;
  // @ts-expect-error Ignore type errors
  const response = JSON.parse(event?.target?.responseText);
  window.$message?.error(response.msg || $t('common.importFail'));
}

/** 导入模板：后端按 SampleInfoExcelRow 的表头生成只有表头的 xlsx */
function handleDownloadTemplate() {
  download('/report/sampleInfo/importTemplate', {}, `样本信息_${$t('common.importTemplate')}_${Date.now()}.xlsx`);
}

watch(visible, () => {
  if (visible.value) {
    fileList.value = [];
    result.value = null;
    importing.value = false;
  }
});
</script>

<template>
  <NModal
    v-model:show="visible"
    :title="`${$t('common.import')}样本信息`"
    preset="card"
    :bordered="false"
    display-directive="show"
    class="max-w-90% w-680px"
    @close="closeModal"
  >
    <NUpload
      ref="uploadRef"
      v-model:file-list="fileList"
      :action="`${baseURL}/report/sampleInfo/importData`"
      :headers="headers"
      :max="1"
      :file-size="50"
      accept=".xls,.xlsx"
      :multiple="false"
      directory-dnd
      :default-upload="false"
      list-type="text"
      :is-error-state="isErrorState"
      @finish="handleFinish"
      @error="handleError"
    >
      <NUploadDragger>
        <div class="mb-12px flex-center">
          <SvgIcon icon="material-symbols:unarchive-outline" class="text-58px color-#d8d8db dark:color-#a1a1a2" />
        </div>
        <NText class="text-16px">{{ $t('common.importTip') }}</NText>
        <NP depth="3" class="mt-8px text-center">
          {{ $t('common.importSize') }}
          <b class="text-red-500">50MB</b>
          {{ $t('common.importFormat') }}
          <b class="text-red-500">xls/xlsx</b>
          {{ $t('common.importEnd') }}
        </NP>
      </NUploadDragger>
    </NUpload>

    <NAlert class="mt-16px" type="info" :bordered="false">
      <div class="text-13px">
        列顺序与导入模板一致（样本编号、条码、患者编号、患者姓名、性别、出生日期、年龄、患者电话、医院、接收日期、样本类型、
        样本数量、检测方案、疾病类型、客户、委托日期、产品名称、备注）。
        <br />
        按「样本编号」判断：不存在则新增，已存在则用文件里的内容覆盖；单行出错只跳过该行，其余照常导入。
      </div>
    </NAlert>

    <NAlert
      v-if="result"
      class="mt-12px"
      :title="$t('common.importResult')"
      :type="succeeded ? 'success' : 'error'"
      :bordered="false"
    >
      <NScrollbar class="max-h-200px">
        <span class="whitespace-pre-line">{{ resultSummary }}</span>
      </NScrollbar>
    </NAlert>

    <template #footer>
      <NSpace justify="end" :size="16">
        <NButton @click="handleDownloadTemplate">{{ $t('common.downloadTemplate') }}</NButton>
        <NButton type="primary" :loading="importing" @click="handleSubmit">{{ $t('common.import') }}</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped></style>
