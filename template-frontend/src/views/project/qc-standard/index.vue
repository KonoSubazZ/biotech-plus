<script setup lang="tsx">
import { onMounted, ref } from 'vue';
import { NDivider, NTag } from 'naive-ui';
import { fetchBatchDeleteQcStandard, fetchGetQcStandardList } from '@/service/api/qc/qc-standard';
import { fetchGetActiveProductConfigList } from '@/service/api/project/product-config';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import QcStandardOperateDrawer from './modules/qc-standard-operate-drawer.vue';
import QcStandardSearch from './modules/qc-standard-search.vue';

defineOptions({
  name: 'QcStandardList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

/** 质控类别：与后端约定一致（wet_lab / bioinfo）。用查表代替三元，便于扩展新类别 */
const CATEGORY_META: Record<string, { label: string; type: 'success' | 'info' | 'default' }> = {
  wet_lab: { label: '湿实验', type: 'success' },
  bioinfo: { label: '生信', type: 'info' }
};

/** 状态 */
const STATUS_META: Record<string, { label: string; type: 'success' | 'default' }> = {
  active: { label: '启用', type: 'success' },
  inactive: { label: '停用', type: 'default' }
};

const searchParams = ref<Api.Qc.QcStandardSearchParams>({
  pageNum: 1,
  pageSize: 10,
  productId: null,
  qcItem: null,
  qcCategory: null,
  status: null,
  params: {}
});

/** 产品 id → 展示名（列表里的「关联产品」列不能显示裸 id） */
const productNameMap = ref<Record<number, string>>({});

async function loadProductMap() {
  const { data, error } = await fetchGetActiveProductConfigList();
  if (error || !data) {
    return;
  }
  productNameMap.value = Object.fromEntries(data.map(item => [item.id, `${item.name}（${item.code}）`]));
}

onMounted(loadProductMap);

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetQcStandardList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      {
        key: 'productId',
        title: '关联产品',
        align: 'center',
        minWidth: 180,
        render: row => productNameMap.value[row.productId] ?? row.productId
      },
      { key: 'qcItem', title: '质控项目', align: 'center', minWidth: 160 },
      {
        key: 'qcCategory',
        title: '质控类别',
        align: 'center',
        width: 110,
        render: row => {
          const meta = CATEGORY_META[row.qcCategory] ?? { label: row.qcCategory, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      {
        key: 'range',
        title: '合格范围',
        align: 'center',
        minWidth: 160,
        render: row => {
          const min = row.minValue ?? '-∞';
          const max = row.maxValue ?? '+∞';
          const unit = row.unit ? ` ${row.unit}` : '';
          return `${min} ~ ${max}${unit}`;
        }
      },
      {
        key: 'status',
        title: '状态',
        align: 'center',
        width: 100,
        render: row => {
          const meta = STATUS_META[row.status] ?? { label: row.status, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      { key: 'remark', title: '备注', align: 'center', minWidth: 140 },
      { key: 'createTime', title: '创建时间', align: 'center', minWidth: 160 },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 140,
        render: row => {
          // 每个按钮单独判权限；两个都没有时连分隔线都不渲染
          const divider = () => {
            if (!hasAuth('qc:qcStandard:edit') || !hasAuth('qc:qcStandard:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('qc:qcStandard:edit')) {
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
            if (!hasAuth('qc:qcStandard:remove')) {
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
  const { error } = await fetchBatchDeleteQcStandard(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteQcStandard([id]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <QcStandardSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="质控标准" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('qc:qcStandard:add')"
          :show-delete="hasAuth('qc:qcStandard:remove')"
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

    <QcStandardOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />
  </div>
</template>

<style scoped></style>
