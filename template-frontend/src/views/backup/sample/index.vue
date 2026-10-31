<script setup lang="tsx">
import { onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NTag } from 'naive-ui';
import { fetchGetSampleList, fetchGetSampleProductOptions } from '@/service/api/backup/sample-backup';
import { useAppStore } from '@/store/modules/app';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { $t } from '@/locales';
import SampleBackupSearch from './modules/sample-backup-search.vue';
import SampleFilesDrawer from './modules/sample-files-drawer.vue';

// name 必须与路由名一致，否则 keep-alive 失效
defineOptions({
  name: 'BackupSampleList'
});

const appStore = useAppStore();

/**
 * 样本级状态由后端按 sample_id 聚合算好（跳过 4 视为已处理）：
 * 全部成功=2 / 部分成功=3 / 全部失败=4 / 待备份=0。
 */
const STATUS_META: Record<number, { label: string; type: 'default' | 'info' | 'success' | 'warning' | 'error' }> = {
  0: { label: '待备份', type: 'default' },
  1: { label: '备份中', type: 'info' },
  2: { label: '全部成功', type: 'success' },
  3: { label: '部分成功', type: 'warning' },
  4: { label: '全部失败', type: 'error' }
};

const searchParams = ref<Api.Backup.SampleSearchParams>({
  pageNum: 1,
  pageSize: 10,
  subbarcode: null,
  projectNo: null,
  orderCode: null,
  productBio: null,
  backupStatus: null,
  beginDate: null,
  endDate: null,
  params: {}
});

/** 产品下拉：取库里已出现过的 product_bio，避免前端写死产品清单 */
const productOptions = ref<{ label: string; value: string }[]>([]);

function fmtSize(bytes?: number | null) {
  if (!bytes) return '-';
  if (bytes >= 1073741824) return `${(bytes / 1073741824).toFixed(2)} GB`;
  return bytes >= 1048576 ? `${(bytes / 1048576).toFixed(1)} MB` : `${(bytes / 1024).toFixed(0)} KB`;
}

const {
  columns,
  columnChecks,
  data,
  getData,
  getDataByPage,
  loading,
  mobilePagination,
  scrollX
} = useNaivePaginatedTable({
  api: () => fetchGetSampleList(searchParams.value),
  transform: response => defaultTransform(response),
  onPaginationParamsChange: params => {
    searchParams.value.pageNum = params.page;
    searchParams.value.pageSize = params.pageSize;
  },
  columns: () => [
    { key: 'subbarcode', title: '样本编号', align: 'left', width: 155 },
    { key: 'projectNo', title: '项目编号', align: 'center', width: 85, render: row => row.projectNo || '-' },
    {
      key: 'orderCode',
      title: '订单批次',
      align: 'center',
      width: 130,
      ellipsis: { tooltip: true },
      render: row => row.orderCode || '-'
    },
    {
      key: 'productBio',
      title: '产品',
      align: 'center',
      width: 140,
      ellipsis: { tooltip: true },
      render: row => row.productBio || '-'
    },
    {
      key: 'researchCenter',
      title: '研究中心',
      align: 'left',
      width: 130,
      ellipsis: { tooltip: true },
      render: row => row.researchCenter || '-'
    },
    {
      key: 'files',
      title: '文件（已备/总）',
      align: 'right',
      width: 125,
      render: row => (
        <span class="whitespace-nowrap">
          {row.fileDone}/{row.fileTotal}
          {row.fileSkipped > 0 ? <span class="text-12px op-60"> (跳过{row.fileSkipped})</span> : null}
        </span>
      )
    },
    {
      key: 'bytes',
      title: '大小（已备/总）',
      align: 'right',
      width: 140,
      render: row => `${fmtSize(row.backedBytes)} / ${fmtSize(row.totalBytes)}`
    },
    {
      key: 'backupStatus',
      title: '备份状态',
      align: 'center',
      width: 95,
      render: row => {
        const meta = STATUS_META[row.backupStatus ?? 0] ?? STATUS_META[0];
        return <NTag type={meta.type}>{meta.label}</NTag>;
      }
    },
    {
      key: 'lastBackupAt',
      title: '最近备份时间',
      align: 'center',
      width: 150,
      render: row => <span class="whitespace-nowrap">{row.lastBackupAt || '-'}</span>
    },
    {
      key: 'operate',
      title: $t('common.operate'),
      align: 'center',
      width: 90,
      fixed: 'right',
      render: row => (
        <NButton text type="primary" onClick={() => openFiles(row)}>
          文件明细
        </NButton>
      )
    }
  ]
});

const filesVisible = ref(false);
const filesRow = ref<Api.Backup.SampleRow | null>(null);

function openFiles(row: Api.Backup.SampleRow) {
  filesRow.value = row;
  filesVisible.value = true;
}

/** 查询：回第一页 */
function queryNow() {
  searchParams.value.pageNum = 1;
  getDataByPage();
}

function handleRefresh() {
  getData();
}

onMounted(async () => {
  const { data: options, error } = await fetchGetSampleProductOptions();
  if (!error && options) {
    productOptions.value = options.map(item => ({ label: item, value: item }));
  }
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <SampleBackupSearch v-model:model="searchParams" :product-options="productOptions" @search="queryNow" />

    <NCard title="样本备份" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :loading="loading"
          :show-add="false"
          :show-delete="false"
          :show-export="false"
          @refresh="handleRefresh"
        />
      </template>

      <NDataTable
        :columns="columns"
        :data="data"
        :flex-height="!appStore.isMobile"
        :loading="loading"
        :pagination="mobilePagination"
        :row-key="row => row.sampleId"
        :scroll-x="scrollX"
        remote
        size="small"
        class="sm:h-full"
      />
    </NCard>

    <SampleFilesDrawer v-model:visible="filesVisible" :row="filesRow" />
  </div>
</template>

<style scoped></style>
