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

defineOptions({
  name: 'InterpretationTabFiles'
});

interface Props {
  /** 分析数据ID：文件范围锁定在这个批次内 */
  analysisId: number;
}

const props = defineProps<Props>();

const { formRef, validate, restoreValidation } = useNaiveForm();

/** data_file_status.status：绿=已加载 / 黄=处理中 / 红=失败 */
const FILE_STATUS_META: Record<string, { label: string; type: 'success' | 'warning' | 'error' }> = {
  Loaded: { label: '已加载', type: 'success' },
  Pending: { label: '处理中', type: 'warning' },
  Error: { label: '失败', type: 'error' }
};

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
      { key: 'fileType', title: '文件类型', align: 'center', minWidth: 120 },
      { key: 'dataType', title: '数据类别', align: 'center', minWidth: 110 },
      { key: 'fileName', title: '文件名', align: 'left', minWidth: 300 },
      {
        key: 'status',
        title: '状态',
        align: 'center',
        minWidth: 100,
        render: row => {
          // 未知值兜底：灰 + 原样显示（别静默按「已加载」渲染）
          const meta = FILE_STATUS_META[row.status ?? ''] ?? {
            label: row.status ?? '-',
            type: 'default' as const
          };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      { key: 'mutNum', title: '变异数', align: 'center', minWidth: 90 },
      {
        key: 'textLength',
        title: '内容大小',
        align: 'center',
        minWidth: 100,
        render: row => (row.textLength ? `${row.textLength} 字符` : '-')
      },
      { key: 'analysisDate', title: '分析日期', align: 'center', minWidth: 110 },
      { key: 'updateTime', title: '更新时间', align: 'center', minWidth: 170 },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 110,
        fixed: 'right',
        render: row => (
          <NButton text type="primary" onClick={() => handleView(row.fileId, row.fileName ?? '')}>
            查看内容
          </NButton>
        )
      }
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
  searchParams.value.fileName = null;
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
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/4" label="文件名" path="fileName">
          <NInput v-model:value="searchParams.fileName" placeholder="请输入文件名" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/4" label="文件类型" path="fileType">
          <NInput v-model:value="searchParams.fileType" placeholder="如 SNP / Indel / CNV / Fusion" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/4" label="状态" path="status">
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
