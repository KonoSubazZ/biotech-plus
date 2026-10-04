<script setup lang="tsx">
import { ref } from 'vue';
import { NTag } from 'naive-ui';
import { fetchGetDevLogList } from '@/service/api/compliance/dev-log';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import DevLogOperateDrawer from './modules/dev-log-operate-drawer.vue';
import DevLogSearch from './modules/dev-log-search.vue';

defineOptions({
  name: 'ComplianceDevLogList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

/**
 * 分类：feature 功能新增 / fix 缺陷修复 / change 变更调整。
 * 分类不是状态，用中性的 info，别借用 success/warning/error（会看着像「通过」）；
 * 同一维度内保持同一种中性色。用查表代替三元，便于扩展新分类。
 */
const CATEGORY_META: Record<string, { label: string; type: 'info' | 'default' }> = {
  feature: { label: '功能新增', type: 'info' },
  fix: { label: '缺陷修复', type: 'info' },
  change: { label: '变更调整', type: 'info' }
};

const searchParams = ref<Api.Compliance.DevLogSearchParams>({
  pageNum: 1,
  pageSize: 10,
  title: null,
  category: null,
  developer: null,
  params: {}
});

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetDevLogList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { key: 'title', title: '记录标题', align: 'left', minWidth: 220 },
      {
        key: 'category',
        title: '分类',
        align: 'center',
        width: 120,
        render: row => {
          const meta = CATEGORY_META[row.category] ?? { label: row.category, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      { key: 'developer', title: '开发人员', align: 'center', width: 120 },
      { key: 'logDate', title: '记录日期', align: 'center', width: 130 },
      { key: 'createTime', title: '创建时间', align: 'center', minWidth: 160 },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 90,
        render: row => {
          // append-only：只留「编辑」，不提供删除（合规上开发记录不可删改）
          if (!hasAuth('compliance:devLog:edit')) return null;
          return (
            <ButtonIcon
              text
              type="primary"
              icon="material-symbols:drive-file-rename-outline-outline"
              tooltipContent={$t('common.edit')}
              onClick={() => handleEdit(row.id)}
            />
          );
        }
      }
    ]
  });

const { drawerVisible, operateType, editingData, handleAdd, handleEdit } = useTableOperate(data, 'id', getData);
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <DevLogSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="开发记录" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <!-- append-only：去掉勾选列与批量删除，只保留「新增 / 刷新」 -->
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :loading="loading"
          :show-add="hasAuth('compliance:devLog:add')"
          :show-delete="false"
          :show-export="false"
          @add="handleAdd"
          @refresh="getData"
        />
      </template>

      <NDataTable
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

    <DevLogOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />
  </div>
</template>

<style scoped></style>
