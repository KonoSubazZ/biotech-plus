<script setup lang="tsx">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { NButton, NSpace, NSwitch, NTag } from 'naive-ui';
import { fetchGetSampleFlowList, fetchHandleSampleFlow } from '@/service/api/report/sample-flow';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { $t } from '@/locales';
// 状态标签表与「报告解读」共用一份（同一批状态码，别在两边各抄一张表）
import { DRIVE_STATUS_META, REPORT_STATUS_META, statusMeta } from '../interpretation/modules/interpretation-status';
import { SAMPLE_FLOW_PROCESS_PERM, SAMPLE_FLOW_TABS } from './modules/sample-flow-tabs';

defineOptions({
  name: 'ReportSampleFlow'
});

/** 一次拉这么多条：样本流转按页签看，单页签数据量小，不分页（超上限时页面提示缩小范围） */
const PAGE_SIZE = 100;

/** 静默轮询间隔：页面在前台时每 30 秒对一次数据（10 人规模，轮询成本可忽略） */
const POLL_INTERVAL = 30 * 1000;

const appStore = useAppStore();
const { hasAuth } = useAuth();

/** 处理按钮：需要有「处理」权限（页签动作还要各自的页签权限，服务端会再校验一次） */
const canProcess = computed(() => hasAuth(SAMPLE_FLOW_PROCESS_PERM));

/** 只渲染有权限的页签 */
const visibleTabs = computed(() => SAMPLE_FLOW_TABS.filter(tab => hasAuth(tab.perm)));

const activeTab = ref('');

/** 页签收敛：当前页签没权限（或还没选）时落到第一个有权限的页签 */
watch(
  visibleTabs,
  tabs => {
    if (!tabs.some(tab => tab.name === activeTab.value)) {
      activeTab.value = tabs[0]?.name ?? '';
    }
  },
  { immediate: true }
);

/** 当前页签的「处理」说明（告诉用户点处理会发生什么） */
const activeHint = computed(() => SAMPLE_FLOW_TABS.find(tab => tab.name === activeTab.value)?.hint ?? '');

/** 查询条件：分析日期默认不限（样本流转要跨天看流转，默认当天常常空表） */
const searchParams = ref<Api.Report.SampleFlowSearchParams>({
  tab: '',
  analysisDate: null,
  subbarcode: null,
  product: null,
  showAll: false,
  pageNum: 1,
  pageSize: PAGE_SIZE
});

/** 处理请求中（防连点） */
const handling = ref(false);

/**
 * 本次查询参数：tab 一律取**当前页签**（不再依赖 searchParams.tab 的赋值时机）。
 * 之前 tab 存在 searchParams 上、由 watch 赋值，首次进页面时 hook 的首次请求早于赋值，
 * 出现过「页签是驱动异常但请求带着空 tab」→ 列表空白的问题。
 */
function currentQuery(): Api.Report.SampleFlowSearchParams {
  return { ...searchParams.value, tab: activeTab.value };
}

/** 上一次拿到数据的指纹（行集合 + 各行的状态/最新更新时间），用来判断"有没有变化" */
let lastFingerprint = '';

/**
 * 指纹 = 每一行的（分析批次 / 报告 / 报告状态 / 驱动状态 / 最新更新时间）拼起来。
 * 轮询时先算指纹再决定要不要替换表格：一样就什么都不做，避免整表闪动、丢滚动位置；
 * 不一样（含新增/消失行）才换数据。
 */
function flowFingerprint(rows: Api.Report.SampleFlowRow[]) {
  return rows
    .map(row =>
      [
        row.analysisId,
        row.reportId ?? '',
        row.reportStatus ?? '',
        row.driveStatus ?? '',
        row.latestUpdateTime ?? ''
      ].join(':')
    )
    .join('|');
}

const { columns, columnChecks, data, getData, loading, scrollX } = useNaivePaginatedTable({
  api: () => fetchGetSampleFlowList(currentQuery()),
  transform: response => {
    const result = defaultTransform(response);
    // 任何一次查询（首次/切页签/手动/条件变化）都顺手记指纹，供轮询对比
    lastFingerprint = flowFingerprint(result.data);
    return result;
  },
  columns: () => [
    { key: 'subbarcode', title: '样本编号', align: 'center', minWidth: 150 },
    { key: 'barcode', title: '患者编号', align: 'center', minWidth: 140, render: row => row.barcode ?? '-' },
    { key: 'patientName', title: '患者姓名', align: 'center', minWidth: 110, render: row => row.patientName ?? '-' },
    {
      key: 'customerName',
      title: '送检单位',
      align: 'center',
      minWidth: 160,
      ellipsis: { tooltip: true },
      render: row => row.customerName ?? '-'
    },
    { key: 'sampleType', title: '样本类型', align: 'center', minWidth: 110, render: row => row.sampleType ?? '-' },
    { key: 'product', title: '产品', align: 'center', minWidth: 180 },
    { key: 'erpTestName', title: '录单产品', align: 'center', minWidth: 160, ellipsis: { tooltip: true } },
    { key: 'analysisDate', title: '分析日期', align: 'center', minWidth: 110 },
    {
      key: 'driveStatus',
      title: '驱动状态',
      align: 'center',
      minWidth: 110,
      render: row =>
        row.driveStatus === null || row.driveStatus === undefined
          ? renderTag({ label: '-', type: 'default' })
          : renderTag(statusMeta(DRIVE_STATUS_META, String(row.driveStatus)))
    },
    { key: 'reportId', title: '报告ID', align: 'center', minWidth: 90, render: row => row.reportId ?? '-' },
    { key: 'reportStatus', title: '报告状态', align: 'center', minWidth: 150, render: row => renderReportStatus(row) },
    { key: 'reportName', title: '报告名', align: 'center', minWidth: 200, ellipsis: { tooltip: true } },
    {
      key: 'analysisDisease',
      title: '解读癌种',
      align: 'center',
      minWidth: 140,
      render: row => row.analysisDisease ?? '-'
    },
    { key: 'interpreter', title: '报告解读人', align: 'center', minWidth: 110, render: row => row.interpreterName ?? '-' },
    {
      key: 'reportGeneratedAt',
      title: '报告生成',
      align: 'center',
      minWidth: 130,
      render: row => renderPersonTime(row.reportGeneratedByName, row.reportGeneratedAt)
    },
    {
      key: 'reportCheckedAt',
      title: '报告复核',
      align: 'center',
      minWidth: 130,
      render: row => renderPersonTime(row.reportCheckedByName, row.reportCheckedAt)
    },
    {
      key: 'reportSentAt',
      title: '报告发送',
      align: 'center',
      minWidth: 130,
      render: row => renderPersonTime(row.reportSentByName, row.reportSentAt)
    },
    {
      key: 'operate',
      title: $t('common.operate'),
      align: 'center',
      width: 100,
      fixed: 'right',
      render: row =>
        canProcess.value ? (
          <NButton text type="primary" loading={handling.value} onClick={() => handle(row.analysisId)}>
            处理
          </NButton>
        ) : (
          <span>-</span>
        )
    }
  ]
});

/** 状态标签（未知值灰底原样显示，与全站配色契约一致） */
function renderTag(meta: { label: string; type: 'success' | 'warning' | 'error' | 'default' }) {
  return <NTag type={meta.type === 'default' ? 'default' : meta.type}>{meta.label}</NTag>;
}

/** 报告状态：审核失败回退的样本额外打一个标签（同一条记录的 revert_from 非空） */
function renderReportStatus(row: Api.Report.SampleFlowRow) {
  if (row.reportStatus === null || row.reportStatus === undefined) {
    return <span>-</span>;
  }
  const meta = statusMeta(REPORT_STATUS_META, String(row.reportStatus), '未解读');
  return (
    <div class="flex-center gap-4px">
      {renderTag(meta)}
      {row.revertFrom !== null && row.revertFrom !== undefined ? <NTag type="error">审核失败回退</NTag> : null}
    </div>
  );
}

/** 人 + 时间两行显示（没有就 '-'） */
function renderPersonTime(name: string | null, at: string | null) {
  if (!name && !at) {
    return <span>-</span>;
  }
  return (
    <div class="flex-col items-center">
      <span>{name ?? '-'}</span>
      <span class="text-12px op-60">{at ?? ''}</span>
    </div>
  );
}

/** 查询条件变化后立即重查（结果不分页，都从头拉） */
function queryNow() {
  searchParams.value.pageNum = 1;
  getData();
}

/** 「显示全部」切换：写回条件并立即重查 */
function handleShowAll(value: boolean) {
  searchParams.value.showAll = value;
  queryNow();
}

function resetQuery() {
  searchParams.value.analysisDate = null;
  searchParams.value.subbarcode = null;
  searchParams.value.product = null;
  searchParams.value.showAll = false;
  queryNow();
}

/** 页签切换：换 tab 后重新查询（每个页签的数据集不一样，查询参数由 currentQuery 带上当前 tab） */
function handleTabChange(name: string) {
  activeTab.value = name;
  queryNow();
}

/** 页签收敛完成后（首个有权限的页签）自动加载一次数据 */
watch(activeTab, name => {
  if (name) {
    queryNow();
  }
});

/**
 * 静默轮询：页面在前台才拉，数据没变就不动表格（对比行指纹）。
 * 用列表接口直接取数据、不走 useNaivePaginatedTable 的 getData —— 那样会置 loading 让表格闪。
 */
async function refreshQuietly() {
  if (document.visibilityState !== 'visible') {
    return;
  }
  const { data: response, error } = await fetchGetSampleFlowList(currentQuery());
  if (error) {
    return;
  }
  const rows = response?.rows ?? [];
  const fingerprint = flowFingerprint(rows);
  if (fingerprint === lastFingerprint) {
    return;
  }
  lastFingerprint = fingerprint;
  data.value = rows;
}

/** 窗口重新获得焦点（例如从 window.open 的详情页办完事切回来）立即对一次 */
function refreshOnFocus() {
  if (!handling.value) {
    refreshQuietly();
  }
}

let pollTimer: ReturnType<typeof setInterval> | undefined;

onMounted(() => {
  pollTimer = setInterval(refreshQuietly, POLL_INTERVAL);
  window.addEventListener('focus', refreshOnFocus);
  document.addEventListener('visibilitychange', refreshOnFocus);
});

onUnmounted(() => {
  clearInterval(pollTimer);
  window.removeEventListener('focus', refreshOnFocus);
  document.removeEventListener('visibilitychange', refreshOnFocus);
});

/**
 * 处理：服务端按页签决定「跳转」还是「认领」。
 * 跳转类用 window.open 开新标签页（保留当前列表，便于连续处理多个样本）；
 * 认领类只更新状态，提示后原地刷新（数据会自己流转到下一个页签）。
 */
async function handle(analysisId: number) {
  if (handling.value) {
    return;
  }
  handling.value = true;
  try {
    const { data: result, error } = await fetchHandleSampleFlow({ tab: activeTab.value, analysisId });
    if (error) {
      return;
    }
    if (result.targetTab && result.reportId) {
      const query = `reportId=${result.reportId}&analysisId=${result.analysisId}&tab=${result.targetTab}`;
      window.open(`${window.location.origin}/report/interpretation?${query}`, '_blank');
      return;
    }
    if (result.message) {
      window.$message?.success(result.message);
    }
    await getData();
  } finally {
    handling.value = false;
  }
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <NCard title="样本流转" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <!-- 查询控件整合进表格标题区（原来独立搜索栏占一整行）：都是小号控件，把行高让给数据 -->
      <template #header-extra>
        <div class="flex flex-wrap items-center justify-end gap-8px">
          <NInput
            v-model:value="searchParams.subbarcode"
            :style="{ width: '130px' }"
            clearable
            placeholder="样本编号"
            size="small"
            @keyup.enter="queryNow"
          />
          <NInput
            v-model:value="searchParams.product"
            :style="{ width: '130px' }"
            clearable
            placeholder="产品"
            size="small"
            @keyup.enter="queryNow"
          />
          <NDatePicker
            v-model:formatted-value="searchParams.analysisDate"
            :style="{ width: '136px' }"
            clearable
            placeholder="分析日期"
            size="small"
            type="date"
            value-format="yyyyMMdd"
            @update:formatted-value="queryNow"
          />
          <NSpace align="center" :size="4">
            <NSwitch :value="searchParams.showAll ?? false" size="small" @update:value="handleShowAll" />
            <span class="text-12px">{{ searchParams.showAll ? '显示全部' : '只看相关' }}</span>
          </NSpace>
          <NButton size="small" @click="resetQuery">{{ $t('common.reset') }}</NButton>
          <span class="text-12px op-60">共 {{ data.length }} 条</span>
          <TableHeaderOperation
            v-model:columns="columnChecks"
            :disabled-delete="true"
            :loading="loading"
            :show-add="false"
            :show-delete="false"
            :show-export="false"
            @refresh="getData"
          />
        </div>
      </template>

      <!-- 卡片内容定高 flex 列：页签与提示固定，表格自己滚。
           表格不再用 flex-height（否则行少时表格被撑满整块、横向滚动条被推到视口下方看不见），
           交给外层 overflow-auto：横向滚动条紧贴最后一行，竖直方向行多了就滚表体。 -->
      <div class="h-full min-h-0 flex-col-stretch gap-8px">
        <NTabs
          v-if="visibleTabs.length"
          :value="activeTab"
          class="shrink-0"
          type="line"
          animated
          @update:value="handleTabChange"
        >
          <NTabPane v-for="tab in visibleTabs" :key="tab.name" :name="tab.name" :tab="tab.label" />
        </NTabs>
        <NEmpty v-else class="shrink-0 py-40px" description="没有可访问的页签：请在角色里授予 report:flow:* 权限" />

        <NAlert v-if="activeHint" class="shrink-0" :bordered="false" type="default">{{ activeHint }}</NAlert>

        <div class="min-h-0 flex-1 overflow-auto">
          <NDataTable
            :columns="columns"
            :data="data"
            :flex-height="!appStore.isMobile"
            :loading="loading"
            :pagination="false"
            :row-key="row => `${row.analysisId}-${row.reportId ?? 'none'}`"
            :scroll-x="scrollX"
            remote
            size="small"
            class="sm:h-full"
          />
        </div>
      </div>
    </NCard>
  </div>
</template>

<style scoped></style>
