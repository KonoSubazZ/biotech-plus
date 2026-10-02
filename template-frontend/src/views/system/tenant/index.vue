<script setup lang="tsx">
import { ref } from 'vue';
import { NDivider } from 'naive-ui';
import { fetchDeleteTenants, fetchGetTenantList } from '@/service/api/system/tenant';
import { useAppStore } from '@/store/modules/app';
import { useDict } from '@/hooks/business/dict';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import ButtonIcon from '@/components/custom/button-icon.vue';
import DictTag from '@/components/custom/dict-tag.vue';
import { $t } from '@/locales';
import TenantSearch from './modules/tenant-search.vue';
import TenantOperateDrawer from './modules/tenant-operate-drawer.vue';

defineOptions({ name: 'TenantList' });
useDict('sys_normal_disable');
const appStore = useAppStore();
const searchParams = ref<Api.System.TenantSearchParams>({ pageNum: 1, pageSize: 10, tenantId: null, tenantName: null, status: null, params: {} });
const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } = useNaivePaginatedTable({
  api: () => fetchGetTenantList(searchParams.value),
  transform: response => defaultTransform(response),
  onPaginationParamsChange: params => { searchParams.value.pageNum = params.page; searchParams.value.pageSize = params.pageSize; },
  columns: () => [
    { type: 'selection', width: 48, disabled: row => row.tenantId === '000000' },
    { key: 'tenantId', title: $t('page.system.tenant.tenantId'), width: 150 },
    { key: 'tenantName', title: $t('page.system.tenant.tenantName'), minWidth: 200, ellipsis: { tooltip: true } },
    { key: 'status', title: $t('page.system.tenant.status'), width: 100, render: row => <DictTag value={row.status} dictCode="sys_normal_disable" /> },
    { key: 'createTime', title: $t('page.system.tenant.createTime'), width: 180 },
    { key: 'remark', title: $t('page.system.user.remark'), minWidth: 160, ellipsis: { tooltip: true } },
    {
      key: 'operate', title: $t('common.operate'), width: 120, align: 'center',
      render: row => <div class="flex-center gap-8px">
        <ButtonIcon text type="primary" icon="material-symbols:drive-file-rename-outline-outline" tooltipContent={$t('common.edit')} onClick={() => edit(row.id)} />
        {row.tenantId !== '000000' && <>
          <NDivider vertical />
          <ButtonIcon text type="error" icon="material-symbols:delete-outline" tooltipContent={$t('common.delete')} popconfirmContent={`${$t('common.confirmDelete')} ${row.tenantName}`} onPositiveClick={() => remove([row.id])} />
        </>}
      </div>
    }
  ]
});
const { drawerVisible, operateType, editingData, handleAdd, handleEdit, checkedRowKeys, onBatchDeleted } = useTableOperate(data, 'id', getData);

function edit(id: CommonType.IdType) {
  handleEdit(id);
}

async function remove(ids: CommonType.IdType[]) {
  const { error } = await fetchDeleteTenants(ids);
  if (!error) onBatchDeleted();
}

function search() {
  checkedRowKeys.value = [];
  getDataByPage();
}

function submitted() {
  checkedRowKeys.value = [];
  if (operateType.value === 'add') getDataByPage();
  else getData();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <TenantSearch v-model:model="searchParams" @search="search" />
    <NCard :title="$t('page.system.tenant.title')" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-export="false"
          @add="handleAdd"
          @delete="remove(checkedRowKeys)"
          @refresh="search"
        />
      </template>
      <NDataTable
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="data"
        size="small"
        :flex-height="!appStore.isMobile"
        :scroll-x="scrollX"
        :loading="loading"
        remote
        :row-key="row => row.id"
        :pagination="mobilePagination"
        class="sm:h-full"
      />
      <TenantOperateDrawer
        v-model:visible="drawerVisible"
        :operate-type="operateType"
        :row-data="editingData"
        @submitted="submitted"
      />
    </NCard>
  </div>
</template>
