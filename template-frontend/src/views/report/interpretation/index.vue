<script setup lang="tsx">
import { computed, ref } from 'vue';
import { NButton, NTag } from 'naive-ui';
import { useRoute } from 'vue-router';
import { fetchEnterInterpretation, fetchGetInterpretationList } from '@/service/api/report/interpretation';
import { useAppStore } from '@/store/modules/app';
import { useRouterPush } from '@/hooks/common/router';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { $t } from '@/locales';
import InterpretationDetail from './modules/interpretation-detail.vue';
import InterpretationSearch from './modules/interpretation-search.vue';
import { DRIVE_STATUS_META, REPORT_STATUS_META, statusMeta } from './modules/interpretation-status';

defineOptions({
  name: 'ReportInterpretation'
});

const appStore = useAppStore();
const route = useRoute();
const { routerPushByKey } = useRouterPush();

/** 进入解读请求中（防止连点） */
const entering = ref(false);

/** 详情页参数来自 query（菜单路由是动态生成的，详情用同一路由 + query，避免多挂一个隐藏菜单） */
const currentReportId = computed(() => queryNumber('reportId'));
const currentAnalysisId = computed(() => queryNumber('analysisId'));

function queryNumber(key: string) {
  const raw = route.query[key];
  const value = Array.isArray(raw) ? raw[0] : raw;
  return value ? Number(value) : 0;
}

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
          const meta = statusMeta(DRIVE_STATUS_META, row.driveStatus);
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      {
        key: 'reportStatus',
        title: '报告状态',
        align: 'center',
        minWidth: 110,
        render: row => {
          const meta = statusMeta(REPORT_STATUS_META, row.reportStatus, '未解读');
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

/** 进入解读：创建/复用报告记录 → 带上 reportId / analysisId 回本页，由详情组件接管渲染 */
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

/** 返回列表（清掉 query 并刷新列表，保证状态列是最新的） */
async function backToList() {
  await routerPushByKey('report_interpretation');
  await getData();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <InterpretationDetail
      v-if="currentReportId"
      :report-id="currentReportId"
      :analysis-id="currentAnalysisId"
      @back="backToList"
    />

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
