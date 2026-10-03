<script setup lang="tsx">
import { ref, watch } from 'vue';
import { NTag } from 'naive-ui';
import {
  fetchGetInterpretationFileContent,
  fetchGetInterpretationFileList
} from '@/service/api/report/interpretation';
import { useNaiveForm } from '@/hooks/common/form';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { $t } from '@/locales';
import { handleCopy } from '@/utils/copy';
import { FILE_STATUS_META, statusMeta } from './interpretation-status';

defineOptions({
  name: 'InterpretationTabFiles'
});

interface Props {
  /** 分析数据ID：文件范围锁定在这个批次内 */
  analysisId: number;
}

const props = defineProps<Props>();

const { formRef, validate, restoreValidation } = useNaiveForm();

/** 状态筛选项（data_file_status.status） */
const FILE_STATUS_OPTIONS = [
  { label: '已加载', value: 'Loaded' },
  { label: '处理中', value: 'Pending' },
  { label: '失败', value: 'Error' }
];

// analysisId 必须在**初始化时**就带上：useNaivePaginatedTable 在 setup 阶段立刻发一次请求，
// 若这时还是 0，后端会以「分析数据不存在」报错并把红色提示弹在这个页面上（实测踩到）。
const searchParams = ref<Api.Report.InterpretationFileSearchParams>({
  analysisId: props.analysisId ?? 0,
  fileName: null,
  fileType: null,
  status: null,
  pageNum: 1,
  pageSize: 10,
  params: {}
});

const { columns, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetInterpretationFileList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { key: 'fileId', title: '文件ID', align: 'center', width: 84 },
      {
        key: 'filePath',
        title: '文件路径',
        align: 'left',
        width: 360,
        // 固定宽度 + 不展示全：容器按宽度截断（多出部分省略号），完整路径放 title；
        // 点击字段本身就是复制（不再单独放「复制」按钮）
        render: row => (
          <div
            class="cursor-pointer truncate hover:text-primary"
            title={row.filePath ?? ''}
            onClick={() => handleCopy(row.filePath ?? '')}
          >
            {row.filePath ?? '-'}
          </div>
        )
      },
      { key: 'analysisDate', title: '分析日期', align: 'center', width: 110 },
      { key: 'fileType', title: '文件类型', align: 'center', width: 120 },
      {
        key: 'fileContent',
        title: '文件内容',
        align: 'center',
        width: 100,
        // 点这一格查看文件正文（操作列按需求仍留空）
        render: row => (
          <NButton text type="primary" onClick={() => handleView(row.fileId, row.fileName ?? '')}>
            查看
          </NButton>
        )
      },
      {
        key: 'status',
        title: '状态',
        align: 'center',
        width: 100,
        render: row => {
          // 未知值兜底：灰 + 原样显示
          const meta = statusMeta(FILE_STATUS_META, row.status);
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      { key: 'updateTime', title: '更新时间', align: 'center', width: 170 },
      {
        key: 'message',
        title: '失败原因',
        align: 'left',
        width: 220,
        // 失败原因可能很长（最长 2000 字），列里只给缩略 + 悬停看全文
        ellipsis: { tooltip: true },
        render: row => row.message ?? '-'
      },
      // 操作列先占位留着（按需求暂不放按钮）
      { key: 'operate', title: $t('common.operate'), align: 'center', width: 100, fixed: 'right' }
    ]
  });

/** 文件内容弹窗 */
const contentVisible = ref(false);
const contentLoading = ref(false);
const content = ref<Api.Report.InterpretationFileContent | null>(null);

async function handleView(fileId: number, fileName: string) {
  contentVisible.value = true;
  contentLoading.value = true;
  content.value = { fileId, fileName, fileType: null, fileText: null, truncated: null, textLength: null };
  try {
    const { data: detail, error } = await fetchGetInterpretationFileContent({
      fileId,
      analysisId: props.analysisId
    });
    if (!error) {
      content.value = detail;
    }
  } finally {
    contentLoading.value = false;
  }
}

async function search() {
  await validate();
  getDataByPage();
}

async function reset() {
  await restoreValidation();
  searchParams.value.fileType = null;
  searchParams.value.status = null;
  getDataByPage();
}

/** analysisId 变了（切报告）才重新拉，避免详情页其它 state 变化引发多余请求 */
watch(
  () => props.analysisId,
  value => {
    if (value && value !== searchParams.value.analysisId) {
      searchParams.value.analysisId = value;
      getData();
    }
  }
);
</script>

<template>
  <div class="flex-col-stretch gap-12px">
    <NForm ref="formRef" :model="searchParams" label-placement="left" :label-width="80">
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="文件类型" path="fileType">
          <NInput v-model:value="searchParams.fileType" placeholder="如 SNP / Indel / CNV / Fusion" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="状态" path="status">
          <NSelect
            v-model:value="searchParams.status"
            :options="FILE_STATUS_OPTIONS"
            placeholder="请选择状态"
            filterable
            clearable
          />
        </NFormItem>
        <NFormItem class="ml-auto" :show-feedback="false">
          <NSpace :size="16">
            <NButton @click="reset">
              <template #icon>
                <icon-ic-round-refresh class="text-icon" />
              </template>
              {{ $t('common.reset') }}
            </NButton>
            <NButton type="primary" ghost @click="search">
              <template #icon>
                <icon-ic-round-search class="text-icon" />
              </template>
              {{ $t('common.search') }}
            </NButton>
          </NSpace>
        </NFormItem>
      </div>
    </NForm>

    <NDataTable
      :columns="columns"
      :data="data"
      :loading="loading"
      :pagination="mobilePagination"
      :row-key="row => row.fileId"
      :scroll-x="scrollX"
      remote
      size="small"
    />

    <NModal
      v-model:show="contentVisible"
      preset="card"
      :title="`文件内容：${content?.fileName ?? ''}`"
      class="w-90vw max-w-1400px"
      :bordered="false"
    >
      <NSpin :show="contentLoading">
        <NAlert v-if="content?.truncated" type="warning" :bordered="false" class="mb-12px">
          内容过长（共 {{ content.textLength }} 字符），已截断显示前 200000 字符。
        </NAlert>
        <pre class="max-h-70vh overflow-auto whitespace-pre-wrap break-all rounded bg-#f5f7fa p-12px text-12px">{{
          content?.fileText ?? '（空内容）'
        }}</pre>
      </NSpin>
    </NModal>
  </div>
</template>

<style scoped></style>
