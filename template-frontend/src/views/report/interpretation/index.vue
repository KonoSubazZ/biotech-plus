<script setup lang="tsx">
import { computed, ref } from 'vue';
import { NButton, NTag } from 'naive-ui';
import { useRoute } from 'vue-router';
import { fetchEnterInterpretation, fetchGetInterpretationList } from '@/service/api/report/interpretation';
import { useAppStore } from '@/store/modules/app';
import { useRouterPush } from '@/hooks/common/router';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { $t } from '@/locales';
import InterpretationSearch from './modules/interpretation-search.vue';

defineOptions({
  name: 'ReportInterpretation'
});

const appStore = useAppStore();
const route = useRoute();
const { routerPushByKey } = useRouterPush();

/**
 * 状态配色契约（本仓统一）：绿=正常/完成、黄=进行中/待处理、红=非正常（驳回/失败）、灰只做未知值兜底。
 * 状态 → 颜色集中在这张表里，别散到各个三元表达式。
 */
const REPORT_STATUS_META: Record<string, { label: string; type: 'success' | 'warning' | 'error' }> = {
  INTERPRETING: { label: '解读中', type: 'warning' },
  PENDING_REVIEW: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已审核', type: 'success' },
  REJECTED: { label: '已驳回', type: 'error' },
  SENT: { label: '已发送', type: 'success' }
};

const DRIVE_STATUS_META: Record<string, { label: string; type: 'success' | 'warning' | 'error' }> = {
  DRIVING: { label: '驱动中', type: 'warning' },
  LOADED: { label: '已完成', type: 'success' },
  PARTIAL: { label: '部分成功', type: 'error' },
  FAILED: { label: '失败', type: 'error' }
};

/** 进入解读请求中（防止连点） */
const entering = ref(false);

/** 当前是否在解读详情（Page 2 落地真实详情页，这里先按 query 占位） */
const currentReportId = computed(() => {
  const raw = route.query.reportId;
  const value = Array.isArray(raw) ? raw[0] : raw;
  return value ? Number(value) : null;
});

const currentAnalysisId = computed(() => {
  const raw = route.query.analysisId;
  const value = Array.isArray(raw) ? raw[0] : raw;
  return value ? Number(value) : null;
});

const searchParams = ref<Api.Report.InterpretationSearchParams>({
  pageNum: 1,
  pageSize: 10,
  subbarcode: null,
  product: null,
  reportStatus: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetInterpretationList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { key: 'subbarcode', title: '样本编号', align: 'center', minWidth: 150 },
      { key: 'barcode', title: '患者编号', align: 'center', minWidth: 140 },
      { key: 'product', title: '产品', align: 'center', minWidth: 160 },
      { key: 'analysisDate', title: '分析日期', align: 'center', minWidth: 110 },
      {
        key: 'driveStatus',
        title: '驱动状态',
        align: 'center',
        minWidth: 110,
        render: row => {
          // 未知值兜底：灰底 + 原样显示，别静默按「已完成」渲染
          const meta = DRIVE_STATUS_META[row.driveStatus ?? ''] ?? {
            label: row.driveStatus ?? '-',
            type: 'default' as const
          };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      {
        key: 'reportStatus',
        title: '报告状态',
        align: 'center',
        minWidth: 110,
        render: row => {
          const meta = REPORT_STATUS_META[row.reportStatus ?? ''] ?? {
            label: row.reportStatus ? row.reportStatus : '未解读',
            type: 'default' as const
          };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      { key: 'analysisDisease', title: '解读癌种', align: 'center', minWidth: 140 },
      { key: 'template', title: '报告模板', align: 'center', minWidth: 150 },
      {
        key: 'generated',
        title: '报告产出',
        align: 'center',
        minWidth: 170,
        render: row => {
          if (!row.reportGeneratedBy && !row.reportGeneratedAt) {
            return <span>-</span>;
          }
          return (
            <div class="flex-col items-center">
              <span>{row.reportGeneratedBy ?? '-'}</span>
              <span class="text-12px op-60">{row.reportGeneratedAt ?? ''}</span>
            </div>
          );
        }
      },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 100,
        fixed: 'right',
        render: row => (
          // 带文字的按钮（不用纯图标）：第一次交付要让用户一眼看到入口
          <NButton text type="primary" loading={entering.value} onClick={() => handleEnter(row.analysisId)}>
            解读
          </NButton>
        )
      }
    ]
  });

/** 进入解读：创建/复用报告记录 → 带上 reportId / analysisId 回到本页（Page 2 会在这里渲染详情） */
async function handleEnter(analysisId: number) {
  if (entering.value) {
    return;
  }
  entering.value = true;
  try {
    const { data: report, error } = await fetchEnterInterpretation({ analysisId });
    if (error) {
      return;
    }
    window.$message?.success(`已进入解读：报告 #${report.reportId}`);
    await routerPushByKey('report_interpretation', {
      query: { reportId: String(report.reportId), analysisId: String(report.analysisId) }
    });
  } finally {
    entering.value = false;
  }
}

/** 返回列表（清掉 query） */
async function backToList() {
  await routerPushByKey('report_interpretation');
  await getData();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <!-- 解读详情占位：Page 2 用真实详情页（6 个 Tab）替换这一段 -->
    <template v-if="currentReportId">
      <NCard :bordered="false" size="small" class="card-wrapper">
        <NSpace align="center" justify="space-between">
          <NSpace align="center">
            <span class="text-16px font-medium">报告解读 #{{ currentReportId }}</span>
            <NTag type="info">分析批次 {{ currentAnalysisId }}</NTag>
          </NSpace>
          <NButton @click="backToList">返回列表</NButton>
        </NSpace>
        <NDivider class="my-12px!" />
        <NEmpty description="详情页建设中（Page 2：顶部摘要 + LIMS 信息 / 集群对接 / 筛选位点 / 报告预览 / 审核报告 / 报告发送 六个 Tab）" />
      </NCard>
    </template>

    <template v-else>
      <InterpretationSearch v-model:model="searchParams" @search="getDataByPage" />

      <NCard title="报告解读" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
        <template #header-extra>
          <TableHeaderOperation
            v-model:columns="columnChecks"
            :disabled-delete="true"
            :loading="loading"
            :show-add="false"
            :show-delete="false"
            :show-export="false"
            @refresh="getData"
          />
        </template>

        <NDataTable
          :columns="columns"
          :data="data"
          :flex-height="!appStore.isMobile"
          :loading="loading"
          :pagination="mobilePagination"
          :row-key="row => `${row.analysisId}-${row.reportId ?? 'none'}`"
          :scroll-x="scrollX"
          remote
          size="small"
          class="sm:h-full"
        />
      </NCard>
    </template>
  </div>
</template>

<style scoped></style>
