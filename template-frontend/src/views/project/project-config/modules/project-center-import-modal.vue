<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { UploadFileInfo } from 'naive-ui';
import { getToken } from '@/store/modules/auth/shared';
import { useDownload } from '@/hooks/business/download';
import { getServiceBaseURL } from '@/utils/service';
import { $t } from '@/locales';
import type FileUpload from '@/components/custom/file-upload.vue';

defineOptions({
  name: 'ProjectCenterImportModal'
});

interface Props {
  /** 所属项目行（导入/导出的目标） */
  project: Api.Project.ProjectConfig;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const { download } = useDownload();
const { baseURL } = getServiceBaseURL(import.meta.env);

const visible = defineModel<boolean>('visible', { default: false });

const uploadRef = ref<typeof FileUpload>();
const fileList = ref<UploadFileInfo[]>([]);
const importing = ref(false);
const result = ref<Api.Project.CenterImportResult | null>(null);

const headers: Record<string, string> = {
  Authorization: `Bearer ${getToken()}`,
  clientid: import.meta.env.VITE_APP_CLIENT_ID!
};

/** 只有「校验全过、真的写库」才算成功；有任一错误整体不导入 */
const succeeded = computed(() => Boolean(result.value?.committed));

const resultSummary = computed(() => {
  const data = result.value;
  if (!data) return '';
  if (data.committed) {
    return [`共 ${data.total} 行：新增 ${data.inserted} 条，修改 ${data.updated} 条，全部导入成功。`].join('\n');
  }
  return [
    `共 ${data.total} 行，其中 ${data.failed} 行校验不通过 —— 按「任一行不过就整体不导入」，本次一行都没有写入。`,
    '修改后重新上传即可：',
    ...data.errors
  ].join('\n');
});

function closeModal() {
  visible.value = false;
}

/** 导出当前研究中心清单（同一份文件就是导入模板；空项目导出只有表头） */
function handleExport() {
  download(
    `/project/projectConfig/${props.project.id}/center/export`,
    {},
    `研究中心_${props.project.projectNo}_${Date.now()}.xlsx`
  );
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
  result.value = response.data as Api.Project.CenterImportResult;
  if (succeeded.value) {
    window.$message?.success($t('common.importSuccess'));
    // 导入成功立即让后面的研究中心列表刷新（弹窗留着给用户看结果明细）
    emit('submitted');
  } else {
    window.$message?.error('校验没通过，未导入任何数据');
  }
  return file;
}

function handleError(options: { file: UploadFileInfo; event?: ProgressEvent }) {
  const { event } = options;
  importing.value = false;
  // @ts-expect-error Ignore type errors
  const response = JSON.parse(event?.target?.responseText);
  window.$message?.error(response.msg || $t('common.importFail'));
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
    :title="`批量导入研究中心 · ${project.projectNo} ${project.projectName}`"
    preset="card"
    :bordered="false"
    display-directive="show"
    class="max-w-90% w-720px"
    @close="closeModal"
  >
    <NAlert class="mb-16px" type="info" :bordered="false">
      <div class="text-13px leading-6">
        先点「导出当前研究中心数据」，在导出的表里改；新中心直接在最后一行往下追加。<br />
        导入按「<b>研究中心编号</b>」匹配：能对上就改这一行，对不上就新增；<b>不会删除任何中心</b>（删中心请在列表里点删除）。<br />
        <b>先校验后导入</b>：任何一行不通过（编号/名称/邮箱为空、同文件编号重复…），整批都不导入，按提示改完再传。
      </div>
    </NAlert>

    <NUpload
      ref="uploadRef"
      v-model:file-list="fileList"
      :action="`${baseURL}/project/projectConfig/${project.id}/center/importData`"
      :headers="headers"
      :max="1"
      :file-size="10"
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
          <b class="text-red-500">10MB</b>
          {{ $t('common.importFormat') }}
          <b class="text-red-500">xls/xlsx</b>
          {{ $t('common.importEnd') }}
        </NP>
      </NUploadDragger>
    </NUpload>

    <NAlert
      v-if="result"
      class="mt-12px"
      :title="succeeded ? $t('common.importResult') : '校验未通过'"
      :type="succeeded ? 'success' : 'error'"
      :bordered="false"
    >
      <NScrollbar class="max-h-240px">
        <span class="whitespace-pre-line">{{ resultSummary }}</span>
      </NScrollbar>
    </NAlert>

    <template #footer>
      <NSpace justify="end" :size="16">
        <NButton @click="handleExport">导出当前研究中心数据</NButton>
        <NButton type="primary" :loading="importing" @click="handleSubmit">{{ $t('common.import') }}</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped></style>
