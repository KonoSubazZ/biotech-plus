<script setup lang="tsx">
import { ref } from 'vue';
import { NButton, NDivider } from 'naive-ui';
import { fetchBatchDeleteSampleInfo, fetchGetSampleInfoList } from '@/service/api/report/sample-info';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import SampleInfoImportModal from './modules/sample-info-import-modal.vue';
import SampleInfoOperateDrawer from './modules/sample-info-operate-drawer.vue';
import SampleInfoSearch from './modules/sample-info-search.vue';

defineOptions({
  name: 'SampleInfoList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

const importModalVisible = ref(false);

const searchParams = ref<Api.Report.SampleInfoSearchParams>({
  pageNum: 1,
  pageSize: 10,
  subbarcode: null,
  personName: null,
  client: null,
  diseaseType: null,
  productName: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetSampleInfoList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      { key: 'subbarcode', title: '样本编号', align: 'center', minWidth: 150 },
      { key: 'barcode', title: '条码', align: 'center', minWidth: 130 },
      { key: 'personName', title: '姓名', align: 'center', minWidth: 100 },
      { key: 'gender', title: '性别', align: 'center', width: 70 },
      { key: 'age', title: '年龄', align: 'center', width: 70 },
      { key: 'patientPhone', title: '患者电话', align: 'center', minWidth: 130 },
      { key: 'hospital', title: '医院', align: 'center', minWidth: 150 },
      { key: 'specimenType', title: '样本类型', align: 'center', minWidth: 120 },
      { key: 'specimenQuantity', title: '样本数量', align: 'center', minWidth: 100 },
      { key: 'diseaseType', title: '录单癌种', align: 'center', minWidth: 120 },
      { key: 'client', title: '客户', align: 'center', minWidth: 140 },
      { key: 'productName', title: '录单产品', align: 'center', minWidth: 170 },
      { key: 'testingProgram', title: '检测方案', align: 'center', minWidth: 140 },
      { key: 'commissionDate', title: '委托日期', align: 'center', minWidth: 110 },
      { key: 'receivedDate', title: '接收日期', align: 'center', minWidth: 110 },
      { key: 'remark', title: '备注', align: 'center', minWidth: 140 },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 140,
        render: row => {
          // 每个按钮单独判权限；两个都没有时连分隔线都不渲染
          const divider = () => {
            if (!hasAuth('report:sampleInfo:edit') || !hasAuth('report:sampleInfo:remove')) {
              return null;
            }
            return <NDivider vertical />;
          };

          const editBtn = () => {
            if (!hasAuth('report:sampleInfo:edit')) {
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
            if (!hasAuth('report:sampleInfo:remove')) {
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
  const { error } = await fetchBatchDeleteSampleInfo(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteSampleInfo([id]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <SampleInfoSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="样本信息" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('report:sampleInfo:add')"
          :show-delete="hasAuth('report:sampleInfo:remove')"
          :show-export="false"
          @add="handleAdd"
          @delete="handleBatchDelete"
          @refresh="getData"
        >
          <template #prefix>
            <NButton
              v-if="hasAuth('report:sampleInfo:import')"
              size="small"
              ghost
              type="primary"
              @click="importModalVisible = true"
            >
              <template #icon>
                <icon-material-symbols-upload class="text-icon" />
              </template>
              导入
            </NButton>
          </template>
        </TableHeaderOperation>
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

    <SampleInfoOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />

    <SampleInfoImportModal v-model:visible="importModalVisible" @submitted="getData" />
  </div>
</template>

<style scoped></style>
