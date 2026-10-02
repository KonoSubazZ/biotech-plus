<script setup lang="tsx">
import { ref } from 'vue';
import { NDivider } from 'naive-ui';
import { fetchBatchDeleteProductConfig, fetchGetProductConfigList } from '@/service/api/biotech/product-config';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import ProductConfigOperateDrawer from './modules/product-config-operate-drawer.vue';
import ProductConfigSearch from './modules/product-config-search.vue';

// name 必须与路由名一致，否则 keep-alive 失效
defineOptions({
  name: 'ProductConfigList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

const searchParams = ref<Api.Biotech.ProductConfigSearchParams>({
  pageNum: 1,
  pageSize: 10,
  name: null,
  code: null,
  status: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetProductConfigList(searchParams.value),
    // RuoYi 的分页响应是顶层 rows/total，需要用 defaultTransform 适配成 hook 期望的结构
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      { key: 'name', title: '产品名称', align: 'center', minWidth: 160 },
      { key: 'code', title: '产品编码', align: 'center', minWidth: 140 },
      { key: 'testType', title: '检测类型', align: 'center', minWidth: 120 },
      { key: 'reportCycleDays', title: '报告周期(天)', align: 'center', width: 120 },
      { key: 'status', title: '状态', align: 'center', width: 100 },
      { key: 'createTime', title: '创建时间', align: 'center', minWidth: 160 },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 140,
        render: row => {
          // 每个按钮单独判权限；两个都没有时连分隔线都不渲染
          const divider = () => {
            if (!hasAuth('biotech:productConfig:edit') || !hasAuth('biotech:productConfig:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('biotech:productConfig:edit')) {
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
            if (!hasAuth('biotech:productConfig:remove')) {
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
  const { error } = await fetchBatchDeleteProductConfig(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteProductConfig([id]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ProductConfigSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="产品配置" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('biotech:productConfig:add')"
          :show-delete="hasAuth('biotech:productConfig:remove')"
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

    <ProductConfigOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />
  </div>
</template>

<style scoped></style>
