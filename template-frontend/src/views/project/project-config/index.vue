<script setup lang="tsx">
import { ref } from 'vue';
import { NButton, NDivider } from 'naive-ui';
import { fetchBatchDeleteProjectConfig, fetchGetProjectConfigList } from '@/service/api/project/project-config';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import ProjectConfigOperateDrawer from './modules/project-config-operate-drawer.vue';
import ProjectConfigSearch from './modules/project-config-search.vue';

// name 必须与路由名一致，否则 keep-alive 失效
defineOptions({
  name: 'ProjectConfigList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

const searchParams = ref<Api.Project.ProjectConfigSearchParams>({
  pageNum: 1,
  pageSize: 10,
  projectNo: null,
  customer: null,
  params: {}
});

/** 「姓名 + 时间」两行显示（上行姓名、下行时间）；两样都没有则留空，与 report/sample-flow 同一套写法 */
function renderNameTime(name?: string | null, at?: string | null) {
  if (!name && !at) {
    return null;
  }
  return (
    <div class="flex-col items-center">
      <span>{name ?? '-'}</span>
      <span class="text-12px op-60">{at ?? ''}</span>
    </div>
  );
}

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetProjectConfigList(searchParams.value),
    // RuoYi 的分页响应是顶层 rows/total，需要用 defaultTransform 适配成 hook 期望的结构
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      { key: 'projectNo', title: '项目编号', align: 'center', width: 140 },
      { key: 'projectName', title: '项目名称', align: 'center', minWidth: 160 },
      { key: 'customer', title: '客户名称', align: 'center', minWidth: 150 },
      { key: 'receiveEmail', title: '收件邮箱', align: 'center', minWidth: 190 },
      { key: 'ccEmail', title: '抄送邮箱', align: 'center', minWidth: 160 },
      { key: 'createByName', title: '创建人', align: 'center', width: 150, render: row => renderNameTime(row.createByName, row.createTime) },
      { key: 'updateByName', title: '更新人', align: 'center', width: 150, render: row => renderNameTime(row.updateByName, row.updateTime) },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 100,
        // 操作列固定在右侧：横向滚动时「修改 / 删除」始终可见（与 report/*、qc/* 同一套）
        fixed: 'right',
        render: row => {
          const editBtn = () => {
            if (!hasAuth('project:projectConfig:edit')) {
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
            if (!hasAuth('project:projectConfig:remove')) {
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
              <NDivider vertical />
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
  const { error } = await fetchBatchDeleteProjectConfig(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteProjectConfig([id]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ProjectConfigSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="项目配置" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="hasAuth('project:projectConfig:add')"
          :show-delete="hasAuth('project:projectConfig:remove')"
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

    <ProjectConfigOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />
  </div>
</template>

<style scoped></style>
