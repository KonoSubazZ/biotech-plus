<script setup lang="tsx">
import { ref } from 'vue';
import { NButton, NDivider, NTag } from 'naive-ui';
import { fetchBatchDeleteReportTemplate, fetchGetReportTemplateList } from '@/service/api/report/template';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import ReportTemplateOperateDrawer from './modules/template-operate-drawer.vue';
import ReportTemplateSearch from './modules/template-search.vue';

defineOptions({
  name: 'ReportTemplateList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

const searchParams = ref<Api.Report.ReportTemplateSearchParams>({
  pageNum: 1,
  pageSize: 10,
  templateCode: null,
  templateName: null,
  reportType: null,
  status: null,
  params: {}
});

/**
 * 状态标签配色契约：绿=正常/启用，黄=警告，红=非正常（含停用）；
 * 未知值兜底成灰并原样显示，别静默按正常渲染。
 */
const STATUS_META: Record<string, { label: string; type: 'success' | 'error' }> = {
  ENABLED: { label: '启用', type: 'success' },
  DISABLED: { label: '停用', type: 'error' }
};

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetReportTemplateList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      { key: 'templateCode', title: '模板编码', align: 'center', minWidth: 160 },
      { key: 'templateName', title: '模板名称', align: 'left', minWidth: 160 },
      { key: 'templateVersion', title: '版本', align: 'center', width: 80, render: row => row.templateVersion ?? '-' },
      { key: 'reportType', title: '报告类型', align: 'center', minWidth: 120, render: row => row.reportType ?? '-' },
      {
        key: 'productNames',
        title: '关联产品',
        align: 'left',
        minWidth: 200,
        ellipsis: { tooltip: true },
        render: row => (row.productNames?.length ? row.productNames.join('、') : '-')
      },
      {
        key: 'moduleCode',
        title: '输出范围（module_code）',
        align: 'left',
        width: 320,
        ellipsis: { tooltip: true },
        render: row => row.moduleCode ?? '（空：只输出公共字段）'
      },
      {
        key: 'templatePath',
        title: '模板文件路径',
        align: 'left',
        minWidth: 280,
        ellipsis: { tooltip: true },
        render: row => row.templatePath ?? '-'
      },
      {
        key: 'status',
        title: '状态',
        align: 'center',
        width: 90,
        render: row => {
          const meta = STATUS_META[row.status] ?? { label: row.status, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      { key: 'createTime', title: '创建时间', align: 'center', minWidth: 160, render: row => row.createTime ?? '-' },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 120,
        fixed: 'right',
        render: row => {
          const divider = () => {
            if (!hasAuth('report:template:edit') || !hasAuth('report:template:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('report:template:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => handleEdit(row.templateId)}
              />
            );
          };

          const deleteBtn = () => {
            if (!hasAuth('report:template:remove')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.templateId)}
              />
            );
          };

          return (
            <div class="flex-center gap-8px">
              {editBtn()}
              {divider()}
              {deleteBtn()}
            </div>
          );
        }
      }
    ]
  });

const { drawerVisible, operateType, editingData, handleAdd, handleEdit, checkedRowKeys, onBatchDeleted, onDeleted } =
  useTableOperate(data, 'templateId', getData);

/** 批量删除：与单条删除共用同一个批量接口 */
async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteReportTemplate(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(templateId: CommonType.IdType) {
  const { error } = await fetchBatchDeleteReportTemplate([templateId]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ReportTemplateSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="报告模板配置" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('report:template:add')"
          :show-delete="hasAuth('report:template:remove')"
          :show-export="false"
          @add="handleAdd"
          @delete="handleBatchDelete"
          @refresh="getData"
        />
      </template>

      <NDataTable
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="data"
        :flex-height="!appStore.isMobile"
        :loading="loading"
        :pagination="mobilePagination"
        :row-key="row => row.templateId"
        :scroll-x="scrollX"
        remote
        size="small"
        class="sm:h-full"
      />
    </NCard>

    <ReportTemplateOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />
  </div>
</template>

<style scoped></style>
