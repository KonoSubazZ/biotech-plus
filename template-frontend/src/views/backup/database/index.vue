<script setup lang="tsx">
import { onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NTag } from 'naive-ui';
import { fetchGetDbBackupSummary, fetchGetDbBinlogList, fetchGetDbFullList } from '@/service/api/backup/db-backup';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import DbBackupSearch from './modules/db-backup-search.vue';
import BinlogFilesDrawer from './modules/binlog-files-drawer.vue';

defineOptions({
  name: 'BackupDatabaseList'
});

/** 汇总（四张卡片）：全部后端现算，页面不落任何数据 */
const summary = ref<Api.Backup.DbSummary>({});

/** 类型：weekly 周定 / manual 手工补跑（配置 full.type 决定默认值） */
const TYPE_META: Record<string, string> = { weekly: '每周', manual: '手工' };

/**
 * 位点链：后端按「本条起点 == 上一条终点」现算，前端不重复实现比对逻辑。
 * linked 绿 / origin 中性 / broken 黄（本期不处理失败，断链只做提示）。
 */
const CHAIN_META: Record<string, { label: string; type: 'success' | 'default' | 'warning' }> = {
  linked: { label: '接上', type: 'success' },
  origin: { label: '链起点', type: 'default' },
  broken: { label: '未接上', type: 'warning' }
};

const searchParams = ref<Api.Backup.DbBackupSearchParams>({
  pageNum: 1,
  pageSize: 10,
  dbName: null,
  beginDate: null,
  endDate: null,
  params: {}
});

const binlogSearchParams = ref<Api.Backup.DbBackupSearchParams>({ ...searchParams.value });

function fmtSize(bytes?: number | null) {
  if (!bytes) return '-';
  if (bytes >= 1073741824) return `${(bytes / 1073741824).toFixed(2)} GB`;
  return bytes >= 1048576 ? `${(bytes / 1048576).toFixed(1)} MB` : `${(bytes / 1024).toFixed(0)} KB`;
}

function cfgShort(hash?: string | null) {
  return hash ? hash.slice(0, 8) : '-';
}

/** 表数：dump 内表数 / 库实际表数（括号里是排除数） */
function tableCell(row: Api.Backup.DbFull) {
  const total = row.dbTableTotal ?? 0;
  const dumped = row.tableCount ?? 0;
  const excluded = Math.max(total - dumped, 0);
  return (
    <span>
      {dumped}/{total}
      {excluded > 0 ? <span class="text-12px op-60"> (-{excluded})</span> : null}
    </span>
  );
}

const {
  columns: fullColumns,
  data: fullData,
  getData: getFullData,
  getDataByPage: getFullDataByPage,
  loading: fullLoading,
  mobilePagination: fullPagination,
  scrollX: fullScrollX
} = useNaivePaginatedTable({
  api: () => fetchGetDbFullList(searchParams.value),
  transform: response => defaultTransform(response),
  onPaginationParamsChange: params => {
    searchParams.value.pageNum = params.page;
    searchParams.value.pageSize = params.pageSize;
  },
  columns: () => [
    { key: 'dbName', title: '库', align: 'center', width: 100 },
    { key: 'backupDate', title: '备份日期', align: 'center', width: 118, render: row => <span class="whitespace-nowrap">{row.backupDate}</span> },
    {
      key: 'backupType',
      title: '类型',
      align: 'center',
      width: 80,
      render: row => TYPE_META[row.backupType] ?? row.backupType
    },
    { key: 'tableCount', title: '表数(排除)', align: 'right', width: 120, render: row => tableCell(row) },
    { key: 'localSize', title: '压缩包', align: 'right', width: 100, render: row => fmtSize(row.localSize) },
    {
      key: 'binlogPos',
      title: '基线位点',
      align: 'left',
      width: 200,
      render: row => (row.binlogFile ? `${row.binlogFile} @ ${row.binlogPos}` : '-')
    },
    {
      key: 'localMd5',
      title: '本地 MD5',
      align: 'center',
      width: 140,
      ellipsis: { tooltip: true },
      render: row => row.localMd5 ?? '-'
    },
    {
      key: 'localPruned',
      title: '本地',
      align: 'center',
      width: 110,
      render: row =>
        row.localPruned === 1 ? <NTag type="warning">已清理</NTag> : <NTag type="success">在本地</NTag>
    },
    {
      key: 'ossPath',
      title: 'OSS 路径',
      align: 'left',
      minWidth: 200,
      ellipsis: { tooltip: true },
      render: row => (row.ossPath ? row.ossPath : <span class="op-50">未上传</span>)
    },
    { key: 'configHash', title: '配置版本', align: 'center', width: 100, render: row => cfgShort(row.configHash) }
  ]
});

const {
  columns: binlogColumns,
  data: binlogData,
  getData: getBinlogData,
  getDataByPage: getBinlogDataByPage,
  loading: binlogLoading,
  mobilePagination: binlogPagination,
  scrollX: binlogScrollX
} = useNaivePaginatedTable({
  api: () => fetchGetDbBinlogList(binlogSearchParams.value),
  transform: response => defaultTransform(response),
  onPaginationParamsChange: params => {
    binlogSearchParams.value.pageNum = params.page;
    binlogSearchParams.value.pageSize = params.pageSize;
  },
  columns: () => [
    {
      key: 'backupDate',
      title: '归档日期',
      align: 'center',
      width: 118,
      render: row => <span class="whitespace-nowrap">{row.backupDate}</span>
    },
    { key: 'startFile', title: '起点 file @ pos', align: 'left', minWidth: 230, render: row => `${row.startFile} @ ${row.startPos}` },
    { key: 'endFile', title: '终点 file @ pos', align: 'left', minWidth: 230, render: row => `${row.endFile} @ ${row.endPos}` },
    { key: 'fileCount', title: '文件数', align: 'right', width: 90 },
    { key: 'totalSize', title: '大小', align: 'right', width: 100, render: row => fmtSize(row.totalSize) },
    {
      key: 'chainState',
      title: '链',
      align: 'center',
      width: 100,
      render: row => {
        const meta = CHAIN_META[row.chainState ?? 'origin'] ?? CHAIN_META.origin;
        return <NTag type={meta.type}>{meta.label}</NTag>;
      }
    },
    { key: 'configHash', title: '配置版本', align: 'center', width: 100, render: row => cfgShort(row.configHash) },
    {
      key: 'operate',
      title: '操作',
      align: 'center',
      width: 100,
      render: row => (
        <NButton text type="primary" onClick={() => openFiles(row)}>
          文件清单
        </NButton>
      )
    }
  ]
});

const filesVisible = ref(false);
const filesRow = ref<Api.Backup.DbBinlog | null>(null);

function openFiles(row: Api.Backup.DbBinlog) {
  filesRow.value = row;
  filesVisible.value = true;
}

async function loadSummary() {
  const { data } = await fetchGetDbBackupSummary();
  summary.value = data ?? {};
}

/** 搜索：两个表都回第一页，卡片一起刷新（卡片是聚合，条件变了必须重算） */
function handleSearch() {
  searchParams.value.pageNum = 1;
  binlogSearchParams.value.pageNum = 1;
  binlogSearchParams.value.dbName = searchParams.value.dbName;
  binlogSearchParams.value.beginDate = searchParams.value.beginDate;
  binlogSearchParams.value.endDate = searchParams.value.endDate;
  getFullDataByPage();
  getBinlogDataByPage();
  loadSummary();
}

function refreshAll() {
  getFullData();
  getBinlogData();
  loadSummary();
}

onMounted(() => {
  loadSummary();
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <NGrid responsive="screen" item-responsive :x-gap="16" :y-gap="16">
      <NGridItem span="24 s:12 m:12 l:6">
        <NCard :bordered="false" size="small" class="card-wrapper">
          <div class="text-12px op-60">最近一次全量（每周日）</div>
          <div class="mt-8px text-18px font-600">
            {{ summary.latestFullOk ?? 0 }}/{{ summary.latestFullTotal ?? 0 }} 库成功
          </div>
          <div class="mt-4px text-12px op-60">
            {{ summary.latestFullDate ?? '还没跑过' }}
            <span v-if="summary.latestFullSize"> · {{ fmtSize(summary.latestFullSize) }}</span>
          </div>
        </NCard>
      </NGridItem>
      <NGridItem span="24 s:12 m:12 l:6">
        <NCard :bordered="false" size="small" class="card-wrapper">
          <div class="text-12px op-60">最近一次增量（每日）</div>
          <div class="mt-8px text-18px font-600">{{ summary.lastBinlogDate ?? '还没跑过' }}</div>
          <div class="mt-4px text-12px op-60">
            <span v-if="summary.lastBinlogAt">{{ summary.lastBinlogAt }} 执行 · </span>
            <span v-if="summary.lastBinlogSize">{{ fmtSize(summary.lastBinlogSize) }}</span>
            <span v-if="summary.lastBinlogFileCount"> · {{ summary.lastBinlogFileCount }} 个 binlog 文件</span>
          </div>
        </NCard>
      </NGridItem>
      <NGridItem span="24 s:12 m:12 l:6">
        <NCard :bordered="false" size="small" class="card-wrapper">
          <div class="text-12px op-60">备份范围（backup.yml）</div>
          <div class="mt-8px text-18px font-600">{{ summary.dbCount ?? 0 }} 库 {{ summary.dbTableTotal ?? 0 }} 表</div>
          <div class="mt-4px text-12px op-60">
            <span v-if="summary.excludedTableCount">按配置排除 {{ summary.excludedTableCount }} 表</span>
            <span v-else>全部表都备（exclude_tables 为空）</span>
          </div>
        </NCard>
      </NGridItem>
      <NGridItem span="24 s:12 m:12 l:6">
        <NCard :bordered="false" size="small" class="card-wrapper">
          <div class="text-12px op-60">保留策略</div>
          <div class="mt-8px text-18px font-600">本地全量 {{ summary.localFullCopies ?? 0 }} 份</div>
          <div class="mt-4px text-12px op-60">
            增量本地 {{ summary.localBinlogDays ?? 0 }} 天
            <span v-if="summary.ossLatestAt"> · OSS 已上传（{{ summary.ossLatestAt }}）</span>
            <span v-else> · OSS 未上传</span>
          </div>
        </NCard>
      </NGridItem>
    </NGrid>

    <DbBackupSearch v-model:model="searchParams" @search="handleSearch" />

    <NCard :bordered="false" size="small" class="card-wrapper">
      <template #header>全量备份</template>
      <template #header-extra>
        <NButton size="small" @click="refreshAll">
          <template #icon>
            <SvgIcon icon="ic:round-refresh" />
          </template>
          刷新
        </NButton>
      </template>
      <NDataTable
        :columns="fullColumns"
        :data="fullData"
        :loading="fullLoading"
        :pagination="fullPagination"
        :row-key="row => row.id"
        :scroll-x="fullScrollX"
        remote
        size="small"
      />
    </NCard>

    <NCard :bordered="false" size="small" class="card-wrapper">
      <template #header>binlog 增量</template>
      <template #header-extra>
        <span class="text-12px op-60">起点必须等于上一条的终点 —— 这就是「链」</span>
      </template>
      <NDataTable
        :columns="binlogColumns"
        :data="binlogData"
        :loading="binlogLoading"
        :pagination="binlogPagination"
        :row-key="row => row.id"
        :scroll-x="binlogScrollX"
        remote
        size="small"
      />
    </NCard>

    <BinlogFilesDrawer v-model:visible="filesVisible" :row="filesRow" />
  </div>
</template>

<style scoped></style>
