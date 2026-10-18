<script setup lang="tsx">
import { ref } from 'vue';
import { NButton, NTag, NRadioGroup, NRadioButton } from 'naive-ui';
import { fetchGetDataAuditList } from '@/service/api/compliance/data-audit';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import DataAuditSearch from './modules/data-audit-search.vue';
import DataAuditDetailDrawer from './modules/data-audit-detail-drawer.vue';

defineOptions({ name: 'ComplianceDataAudit' });
const appStore = useAppStore();
const { hasAuth } = useAuth();
const mode = ref('changes');
const sampleQueried = ref(false);
const drawerVisible = ref(false);
const eventId = ref<CommonType.IdType | null>(null);
const outcomeLabels: Record<string, string> = {
  STARTED: '已发起', SUCCESS: '成功', FAILED: '失败', NO_CHANGE: '无变化'
};
const searchParams = ref<Api.Audit.SearchParams>({
  pageNum: 1, pageSize: 20, subbarcode: null, analysisId: null, auditScope: null,
  action: null, actorName: null, ascending: false, includeSampleCommon: true
});

const { columns, columnChecks, data, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetDataAuditList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page ?? 1;
      searchParams.value.pageSize = params.pageSize ?? 20;
    },
    columns: () => [
      { key: 'occurredAt', title: '操作时间', width: 180 },
      { key: 'action', title: '操作', minWidth: 180 },
      { key: 'subbarcode', title: '样本编号', minWidth: 160, render: row => row.subbarcode || '不适用' },
      { key: 'analysisId', title: '分析数据ID', width: 130, render: row => row.analysisId || '公共／不适用' },
      { key: 'reportId', title: '报告ID', width: 110, render: row => row.reportId || '-' },
      { key: 'actorName', title: '操作人', width: 130 },
      { key: 'changeCount', title: '变化记录数', width: 110 },
      {
        key: 'outcome', title: '结果', width: 100,
        render: row => <NTag type={row.outcome === 'FAILED' ? 'error' : 'default'}>
          {outcomeLabels[row.outcome] || row.outcome}
        </NTag>
      },
      {
        key: 'operate', title: '操作详情', width: 100,
        render: row => {
          if (!hasAuth('compliance:dataAudit:query')) return null;
          return <NButton text type="primary" onClick={() => openDetail(row.id)}>查看详情</NButton>;
        }
      }
    ]
  });

function openDetail(id: CommonType.IdType) {
  eventId.value = id;
  drawerVisible.value = true;
}

function search() {
  if (loading.value) return;
  if (mode.value === 'sample' && !searchParams.value.subbarcode?.trim()) {
    sampleQueried.value = false;
    window.$message?.warning('请输入完整样本编号后查询样本履历');
    return;
  }
  searchParams.value.auditScope = mode.value === 'sample' ? 'SAMPLE' : searchParams.value.auditScope;
  if (searchParams.value.subbarcode) searchParams.value.subbarcode = searchParams.value.subbarcode.trim();
  sampleQueried.value = mode.value === 'sample';
  getDataByPage();
}

function changeMode(value: string | number) {
  if (loading.value) return;
  mode.value = String(value);
  sampleQueried.value = false;
  searchParams.value.ascending = mode.value === 'sample';
  searchParams.value.auditScope = mode.value === 'sample' ? 'SAMPLE' : null;
  if (mode.value === 'sample' && !searchParams.value.subbarcode) return;
  search();
}

function reset() {
  sampleQueried.value = false;
  searchParams.value.auditScope = mode.value === 'sample' ? 'SAMPLE' : null;
  if (mode.value === 'changes') search();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <DataAuditSearch v-model:model="searchParams" :pending="loading" @search="search" @reset="reset" />
    <NCard
      title="数据审计"
      :bordered="false"
      size="small"
      class="card-wrapper sm:flex-1-hidden"
      content-class="flex-col-stretch sm:flex-1-hidden"
    >
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :loading="loading"
          :show-add="false"
          :show-delete="false"
          :show-export="false"
          @refresh="search"
        />
      </template>
      <NRadioGroup
        :value="mode"
        :disabled="loading"
        name="audit-view"
        aria-label="审计视图"
        class="mb-16px"
        @update:value="changeMode"
      >
        <NRadioButton value="changes">变更记录</NRadioButton>
        <NRadioButton value="sample">样本履历</NRadioButton>
      </NRadioGroup>
      <NAlert v-if="mode === 'sample'" type="info" :show-icon="false" class="mb-12px">
        输入完整样本编号，按操作时间从早到晚查看流转与解读记录；可按分析数据ID筛选，并包含样本公共事件。
      </NAlert>
      <NEmpty v-if="mode === 'sample' && !sampleQueried" class="py-24px" description="请输入完整样本编号后查询" />
      <NDataTable
        v-else
        :columns="columns"
        :data="data"
        :flex-height="!appStore.isMobile"
        :loading="loading"
        :pagination="mobilePagination"
        :row-key="row => row.id"
        :scroll-x="scrollX"
        size="small"
        class="sm:flex-1"
        remote
      />
    </NCard>
    <DataAuditDetailDrawer v-model:visible="drawerVisible" :event-id="eventId" />
  </div>
</template>
