<script setup lang="tsx">
import { ref, watch } from 'vue';
import { NButton } from 'naive-ui';
import { fetchGetInterpretationFileList } from '@/service/api/report/interpretation';
import { useNaiveForm } from '@/hooks/common/form';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { $t } from '@/locales';
import { handleCopy } from '@/utils/copy';

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
      { key: 'fileId', title: '文件ID', align: 'center', width: 100 },
      {
        key: 'filePath',
        title: '文件路径',
        align: 'left',
        minWidth: 460,
        // 绝对路径给一键复制：路径很长，手选容易漏字符
        render: row => (
          <div class="flex items-center gap-6px">
            <span class="truncate" title={row.filePath ?? ''}>
              {row.filePath ?? '-'}
            </span>
            <NButton
              text
              type="primary"
              size="tiny"
              disabled={!row.filePath}
              onClick={() => handleCopy(row.filePath ?? '')}
            >
              复制
            </NButton>
          </div>
        )
      },
      { key: 'analysisDate', title: '分析日期', align: 'center', minWidth: 110 },
      { key: 'mutNum', title: '文件内容数', align: 'center', minWidth: 110 },
      { key: 'updateTime', title: '更新时间', align: 'center', minWidth: 170 },
      // 操作列先占位留着（按需求暂不放按钮）
      { key: 'operate', title: $t('common.operate'), align: 'center', width: 110, fixed: 'right' }
    ]
  });

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
  </div>
</template>

<style scoped></style>
