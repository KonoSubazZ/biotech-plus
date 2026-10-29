<script setup lang="tsx">
import { computed, ref } from 'vue';
import { NButton, NCard, NDataTable, NTab, NTabs, NTag } from 'naive-ui';
import { fetchGetDbBinlogList, fetchGetDbFullList } from '@/service/api/backup/db-backup';
import { useAppStore } from '@/store/modules/app';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { $t } from '@/locales';
import BinlogFilesDrawer from './modules/binlog-files-drawer.vue';
import DbBackupSearch from './modules/db-backup-search.vue';

// name 必须与路由名一致，否则 keep-alive 失效
defineOptions({
  name: 'BackupDatabaseList'
});

const appStore = useAppStore();

/** 两个数据集分两个页签（同一张卡片里切，不并排堆两张表） */
const activeTab = ref<'full' | 'binlog'>('full');

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
function renderTableCount(row: Api.Backup.DbFull) {
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
  columnChecks: fullColumnChecks,
  data: fullData,
  getData: getFullData,
  getDataByPage: getFullDataByPage,
  loading: fullLoading,
  mobilePagination: fullPagination,
  scrollX: fullScrollX
} = useNaivePaginatedTable({
  api: () => fetchGetDbFullList(searchParams.value),
  // RuoYi 的分页响应是顶层 rows/total，需要用 defaultTransform 适配成 hook 期望的结构
  transform: response => defaultTransform(response),
  onPaginationParamsChange: params => {
    searchParams.value.pageNum = params.page;
    searchParams.value.pageSize = params.pageSize;
  },
  columns: () => [
    { key: 'dbName', title: '库', align: 'center', width: 100 },
    {
      key: 'backupDate',
      title: '备份日期',
      align: 'center',
      width: 118,
      render: row => <span class="whitespace-nowrap">{row.backupDate}</span>
    },
    {
      key: 'backupType',
      title: '类型',
      align: 'center',
      width: 80,
      render: row => TYPE_META[row.backupType] ?? row.backupType
    },
    { key: 'tableCount', title: '表数(排除)', align: 'right', width: 120, render: row => renderTableCount(row) },
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
      width: 100,
      render: row => (row.localPruned === 1 ? <NTag type="warning">已清理</NTag> : <NTag type="success">在本地</NTag>)
    },
    {
      key: 'ossPath',
      title: 'OSS 路径',
      align: 'left',
      minWidth: 200,
      ellipsis: { tooltip: true },
      render: row => row.ossPath ?? '-'
    },
    { key: 'configHash', title: '配置版本', align: 'center', width: 100, render: row => cfgShort(row.configHash) }
  ]
});

const {
  columns: binlogColumns,
  columnChecks: binlogColumnChecks,
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
    {
      key: 'startFile',
      title: '起点 file @ pos',
      align: 'left',
      minWidth: 230,
      render: row => `${row.startFile} @ ${row.startPos}`
    },
    {
      key: 'endFile',
      title: '终点 file @ pos',
      align: 'left',
      minWidth: 230,
      render: row => `${row.endFile} @ ${row.endPos}`
    },
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
      title: $t('common.operate'),
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

/** toolbar 的列设置按钮跟当前页签走（两个表各自一套可见列配置） */
const columnChecks = computed<NaiveUI.TableColumnCheck[]>({
  get: () => (activeTab.value === 'full' ? fullColumnChecks.value : binlogColumnChecks.value),
  set: value => {
    if (activeTab.value === 'full') {
      fullColumnChecks.value = value;
    } else {
      binlogColumnChecks.value = value;
    }
  }
});

const activeLoading = computed(() => (activeTab.value === 'full' ? fullLoading.value : binlogLoading.value));

const filesVisible = ref(false);
const filesRow = ref<Api.Backup.DbBinlog | null>(null);

function openFiles(row: Api.Backup.DbBinlog) {
  filesRow.value = row;
  filesVisible.value = true;
}

/** 查询：两个表都回第一页（切页签看到的是同一组条件） */
function queryNow() {
  searchParams.value.pageNum = 1;
  binlogSearchParams.value.pageNum = 1;
  binlogSearchParams.value.dbName = searchParams.value.dbName;
  binlogSearchParams.value.beginDate = searchParams.value.beginDate;
  binlogSearchParams.value.endDate = searchParams.value.endDate;
  getFullDataByPage();
  getBinlogDataByPage();
}

function handleTabChange(name: 'full' | 'binlog') {
  activeTab.value = name;
  if (name === 'full') {
    getFullDataByPage();
  } else {
    getBinlogDataByPage();
  }
}

function handleRefresh() {
  if (activeTab.value === 'full') {
    getFullData();
  } else {
    getBinlogData();
  }
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <DbBackupSearch v-model:model="searchParams" @search="queryNow" />

    <NCard title="数据库备份" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :loading="activeLoading"
          :show-add="false"
          :show-delete="false"
          :show-export="false"
          @refresh="handleRefresh"
        />
      </template>

      <!-- 卡片内容定高 flex 列：页签固定、表格自己滚（与 report/sample-flow 的页签页同一写法） -->
      <div class="h-full min-h-0 flex-col-stretch gap-8px">
        <div class="flex shrink-0 items-center gap-8px">
          <NTabs :value="activeTab" class="min-w-0 flex-1" type="line" animated @update:value="handleTabChange">
            <NTab name="full">全量备份</NTab>
            <NTab name="binlog">binlog 增量</NTab>
          </NTabs>
        </div>

        <div class="min-h-0 flex-1 overflow-auto">
          <NDataTable
            v-if="activeTab === 'full'"
            :columns="fullColumns"
            :data="fullData"
            :flex-height="!appStore.isMobile"
            :loading="fullLoading"
            :pagination="fullPagination"
            :row-key="row => row.id"
            :scroll-x="fullScrollX"
            remote
            size="small"
            class="sm:h-full"
          />
          <NDataTable
            v-else
            :columns="binlogColumns"
            :data="binlogData"
            :flex-height="!appStore.isMobile"
            :loading="binlogLoading"
            :pagination="binlogPagination"
            :row-key="row => row.id"
            :scroll-x="binlogScrollX"
            remote
            size="small"
            class="sm:h-full"
          />
        </div>
      </div>
    </NCard>

    <BinlogFilesDrawer v-model:visible="filesVisible" :row="filesRow" />
  </div>
</template>

<style scoped></style>
