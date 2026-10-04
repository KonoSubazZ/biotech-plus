<script setup lang="tsx">
import { ref } from 'vue';
import { NButton, NDivider, NTag } from 'naive-ui';
import { fetchBatchDeleteComplianceDoc, fetchGetComplianceDocList } from '@/service/api/compliance/compliance-doc';
import { useDownload } from '@/hooks/business/download';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import ComplianceDocOperateDrawer from './modules/compliance-doc-operate-drawer.vue';
import ComplianceDocSearch from './modules/compliance-doc-search.vue';

defineOptions({
  name: 'ComplianceDocsList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();
// 附件下载复用项目既有下载器（自带 token，GET 直取后端文件流）
const { zip: downloadFile } = useDownload();

/**
 * 文档类型：IQ/OQ/PQ/DEV_TEST。
 * 类型不是状态，用中性的 info，别借用 success/warning/error；
 * 同一维度内保持同一种中性色。
 */
const DOC_TYPE_META: Record<string, { label: string; type: 'info' | 'default' }> = {
  IQ: { label: 'IQ 安装确认', type: 'info' },
  OQ: { label: 'OQ 运行确认', type: 'info' },
  PQ: { label: 'PQ 性能确认', type: 'info' },
  DEV_TEST: { label: '开发测试', type: 'info' }
};

const searchParams = ref<Api.Compliance.ComplianceDocSearchParams>({
  pageNum: 1,
  pageSize: 10,
  docType: null,
  title: null,
  version: null,
  params: {}
});

/** 点文件名即下载：文件名由后端按 id 定位（同名附件只存一份） */
function handleDownloadDoc(row: Api.Compliance.ComplianceDoc) {
  if (!row.fileName) return;
  downloadFile(`/compliance/complianceDoc/download/${row.id}`, row.fileName);
}

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetComplianceDocList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      // 记录类：带勾选列，支持批量删除
      { type: 'selection', align: 'center', width: 48 },
      {
        key: 'index',
        title: '序号',
        align: 'center',
        width: 64,
        // 跨页连续：当前页第一行 = (页码 - 1) * 每页条数 + 1
        render: (_row, index) => {
          const pageNum = Number(searchParams.value.pageNum ?? 1);
          const pageSize = Number(searchParams.value.pageSize ?? 10);
          return (pageNum - 1) * pageSize + index + 1;
        }
      },
      {
        key: 'docType',
        title: '文档类型',
        align: 'center',
        width: 130,
        render: row => {
          const meta = DOC_TYPE_META[row.docType] ?? { label: row.docType, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      { key: 'title', title: '文档标题', align: 'left', minWidth: 220 },
      { key: 'version', title: '版本', align: 'center', width: 100 },
      {
        key: 'fileName',
        title: '文档文件',
        align: 'center',
        minWidth: 180,
        render: row => {
          if (!row.fileName) return '-';
          return (
            <NButton text type="primary" onClick={() => handleDownloadDoc(row)}>
              {row.fileName}
            </NButton>
          );
        }
      },
      {
        key: 'remark',
        title: '备注',
        align: 'left',
        minWidth: 160,
        ellipsis: { tooltip: true },
        render: row => row.remark ?? '-'
      },
      { key: 'createTime', title: '创建时间', align: 'center', minWidth: 160 },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 140,
        render: row => {
          // 每个按钮单独判权限；两个都没有时连分隔线都不渲染
          const divider = () => {
            if (!hasAuth('compliance:complianceDoc:edit') || !hasAuth('compliance:complianceDoc:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('compliance:complianceDoc:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:drive-file-rename-outline-outline"
                tooltipContent={$t('common.edit')}
                onClick={() => handleEdit(row.id)}
              />
            );
          };

          const deleteBtn = () => {
            if (!hasAuth('compliance:complianceDoc:remove')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="error"
                icon="material-symbols:delete-outline"
                tooltipContent={$t('common.delete')}
                popconfirmContent={$t('common.confirmDelete')}
                onPositiveClick={() => handleDelete(row.id)}
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

const {
  drawerVisible,
  operateType,
  editingData,
  handleAdd,
  handleEdit,
  checkedRowKeys,
  onBatchDeleted,
  onDeleted
} = useTableOperate(data, 'id', getData);

/** 批量删除：与单条删除共用同一个批量接口，避免两边各写一套 */
async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteComplianceDoc(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteComplianceDoc([id]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ComplianceDocSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="3Q 文档管理" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('compliance:complianceDoc:add')"
          :show-delete="hasAuth('compliance:complianceDoc:remove')"
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
        :row-key="row => row.id"
        :scroll-x="scrollX"
        remote
        size="small"
        class="sm:h-full"
      />
    </NCard>

    <ComplianceDocOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />
  </div>
</template>

<style scoped></style>
