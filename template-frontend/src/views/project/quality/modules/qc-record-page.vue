<script setup lang="tsx">
import { ref } from 'vue';
import { NDivider, NTag } from 'naive-ui';
import { fetchBatchDeleteQcRecord, fetchGetQcRecordList } from '@/service/api/qc/qc-record';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import QcRecordOperateDrawer from './qc-record-operate-drawer.vue';
import QcRecordSearch from './qc-record-search.vue';

interface Props {
  /** 质控类别：wet_lab 湿实验 / bioinfo 生信 —— 两个菜单共用本组件，靠它区分 */
  qcCategory: string;
  /** 卡片标题（如「湿实验质控记录」） */
  title: string;
}

const props = defineProps<Props>();

defineOptions({
  name: 'QcRecordPage'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

/** 人工确认状态：与后端约定一致（pending / passed / failed） */
const STATUS_META: Record<string, { label: string; type: 'warning' | 'success' | 'error' }> = {
  pending: { label: '待确认', type: 'warning' },
  passed: { label: '通过', type: 'success' },
  failed: { label: '未通过', type: 'error' }
};

const searchParams = ref<Api.Qc.QcRecordSearchParams>({
  pageNum: 1,
  pageSize: 10,
  subbarcode: null,
  qcItem: null,
  status: null,
  qcCategory: props.qcCategory,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetQcRecordList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      { key: 'subbarcode', title: '样本条码', align: 'center', minWidth: 160 },
      { key: 'qcItem', title: '质控项目', align: 'center', minWidth: 160 },
      { key: 'qcResult', title: '质控结果', align: 'center', minWidth: 120 },
      {
        key: 'status',
        title: '人工状态',
        align: 'center',
        width: 110,
        render: row => {
          const meta = STATUS_META[row.status] ?? { label: row.status, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      { key: 'operator', title: '操作员', align: 'center', minWidth: 100 },
      { key: 'testedAt', title: '检测时间', align: 'center', minWidth: 160 },
      { key: 'remark', title: '备注', align: 'center', minWidth: 140 },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 140,
        render: row => {
          // 每个按钮单独判权限；两个都没有时连分隔线都不渲染
          const divider = () => {
            if (!hasAuth('qc:qcRecord:edit') || !hasAuth('qc:qcRecord:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('qc:qcRecord:edit')) {
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
            if (!hasAuth('qc:qcRecord:remove')) {
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
  const { error } = await fetchBatchDeleteQcRecord(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteQcRecord([id]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <QcRecordSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard :title="props.title" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('qc:qcRecord:add')"
          :show-delete="hasAuth('qc:qcRecord:remove')"
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

    <QcRecordOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      :qc-category="props.qcCategory"
      @submitted="getData"
    />
  </div>
</template>

<style scoped></style>
