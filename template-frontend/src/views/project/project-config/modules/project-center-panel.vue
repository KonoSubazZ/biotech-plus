<script setup lang="tsx">
import { onMounted, ref } from 'vue';
import { NCard } from 'naive-ui';
import { fetchBatchDeleteProjectCenter, fetchGetProjectCenters } from '@/service/api/project/project-config';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import ProjectCenterDrawer from './project-center-drawer.vue';

defineOptions({
  name: 'ProjectCenterPanel'
});

interface Props {
  /** 所属项目行（表格展开行的数据） */
  project: Api.Project.ProjectConfig;
}

const props = defineProps<Props>();

const { hasAuth } = useAuth();

const rows = ref<Api.Project.ProjectConfig[]>([]);
const loading = ref(false);
const drawerVisible = ref(false);
const editingRow = ref<Api.Project.ProjectConfig | null>(null);

/**
 * 每次展开都现拉一次：中心是本页内嵌的子表，不做缓存，避免在别处改完这里还是旧数据。
 */
async function load() {
  loading.value = true;
  try {
    const { data, error } = await fetchGetProjectCenters(props.project.id);
    if (error) return;
    rows.value = data ?? [];
  } finally {
    loading.value = false;
  }
}

function handleAdd() {
  editingRow.value = null;
  drawerVisible.value = true;
}

function handleEdit(row: Api.Project.ProjectConfig) {
  editingRow.value = row;
  drawerVisible.value = true;
}

async function handleDelete(row: Api.Project.ProjectConfig) {
  const { error } = await fetchBatchDeleteProjectCenter([row.id]);
  if (error) return;
  window.$message?.success($t('common.deleteSuccess'));
  await load();
}

onMounted(load);

/** 「姓名 + 时间」两行显示（上行姓名、下行时间）；两样都没有则留空，与项目列表同一套写法 */
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

const columns = [
  { key: 'researchCenterNo', title: '研究中心编号', align: 'center' as const, width: 110 },
  { key: 'researchCenterName', title: '研究中心名称', align: 'center' as const, minWidth: 160 },
  // 邮箱多为「多个用分号分隔」，超长就省略 + 悬浮看全文
  { key: 'receiveEmail', title: '收件邮箱', align: 'center' as const, minWidth: 180, ellipsis: { tooltip: true } },
  { key: 'ccEmail', title: '抄送邮箱', align: 'center' as const, minWidth: 160, ellipsis: { tooltip: true } },
  { key: 'remark', title: '备注', align: 'center' as const, minWidth: 120 },
  {
    key: 'createByName',
    title: '创建人',
    align: 'center' as const,
    width: 130,
    render: (row: Api.Project.ProjectConfig) => renderNameTime(row.createByName, row.createTime)
  },
  {
    key: 'updateByName',
    title: '更新人',
    align: 'center' as const,
    width: 130,
    render: (row: Api.Project.ProjectConfig) => renderNameTime(row.updateByName, row.updateTime)
  },
  {
    key: 'operate',
    title: $t('common.operate'),
    align: 'center' as const,
    width: 100,
    render: (row: Api.Project.ProjectConfig) => (
      <div class="flex-center gap-8px">
        {hasAuth('project:projectConfig:edit') && (
          <ButtonIcon
            text
            type="primary"
            icon="material-symbols:drive-file-rename-outline-outline"
            tooltipContent={$t('common.edit')}
            onClick={() => handleEdit(row)}
          />
        )}
        {hasAuth('project:projectConfig:remove') && (
          <ButtonIcon
            text
            type="error"
            icon="material-symbols:delete-outline"
            tooltipContent={$t('common.delete')}
            popconfirmContent={$t('common.confirmDelete')}
            onPositiveClick={() => handleDelete(row)}
          />
        )}
      </div>
    )
  }
];
</script>

<template>
  <NCard size="small" :bordered="false" class="mt-4px">
    <template #header>
      <span class="text-13px op-70">研究中心（{{ rows.length }}）</span>
    </template>
    <template #header-extra>
      <!-- 与项目配置卡片的新增同一形态：ghost primary + 加号图标（子表不单独给刷新，跟着主表/提交后自动 re-load） -->
      <NButton v-if="hasAuth('project:projectConfig:add')" size="small" ghost type="primary" @click="handleAdd">
        <template #icon>
          <icon-material-symbols-add class="text-icon" />
        </template>
        新增研究中心
      </NButton>
    </template>

    <NDataTable :columns="columns" :data="rows" :loading="loading" :row-key="row => row.id" size="small" />

    <ProjectCenterDrawer
      v-model:visible="drawerVisible"
      :project="project"
      :row-data="editingRow"
      @submitted="load"
    />
  </NCard>
</template>

<style scoped></style>
