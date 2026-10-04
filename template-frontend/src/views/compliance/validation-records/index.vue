<script setup lang="tsx">
import { ref } from 'vue';
import { NButton, NTag } from 'naive-ui';
import { fetchGetValidationRecordList } from '@/service/api/compliance/validation-record';
import { useDownload } from '@/hooks/business/download';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import ValidationRecordOperateDrawer from './modules/validation-record-operate-drawer.vue';
import ValidationRecordSearch from './modules/validation-record-search.vue';

defineOptions({
  name: 'ComplianceValidationRecordsList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();
// 附件下载复用项目既有下载器（自带 token，GET 直取后端文件流）
const { zip: downloadFile } = useDownload();

/**
 * 验证类型：IQ 安装确认 / OQ 运行确认 / PQ 性能确认。
 * 类型不是状态，用中性的 info，别借用 success/warning/error（会看着像「通过」）；
 * 同一维度内保持同一种中性色。
 */
const TYPE_META: Record<string, { label: string; type: 'info' | 'default' }> = {
  IQ: { label: 'IQ 安装确认', type: 'info' },
  OQ: { label: 'OQ 运行确认', type: 'info' },
  PQ: { label: 'PQ 性能确认', type: 'info' }
};

/**
 * 结果配色（仓库 crud skill §「状态标签配色契约」）：通过=绿、未通过=红，
 * 灰只留给未知值兜底；na（不适用）是合法取值、不是未知 → 用中性 info。
 */
const RESULT_META: Record<string, { label: string; type: 'success' | 'error' | 'info' }> = {
  passed: { label: '通过', type: 'success' },
  failed: { label: '未通过', type: 'error' },
  na: { label: '不适用', type: 'info' }
};

const searchParams = ref<Api.Compliance.ValidationRecordSearchParams>({
  pageNum: 1,
  pageSize: 10,
  validationType: null,
  title: null,
  executedBy: null,
  result: null,
  params: {}
});

/** 点文件名即下载：文件名由后端按 id 定位（同名附件只存一份） */
function handleDownloadDoc(row: Api.Compliance.ValidationRecord) {
  if (!row.fileName) return;
  downloadFile(`/compliance/validationRecord/download/${row.id}`, row.fileName);
}

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetValidationRecordList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
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
        key: 'validationType',
        title: '验证类型',
        align: 'center',
        width: 130,
        render: row => {
          const meta = TYPE_META[row.validationType] ?? { label: row.validationType, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      { key: 'title', title: '验证标题', align: 'left', minWidth: 220 },
      { key: 'version', title: '版本', align: 'center', width: 100, render: row => row.version ?? '-' },
      { key: 'executedBy', title: '执行人', align: 'center', width: 120, render: row => row.executedBy ?? '-' },
      { key: 'executedDate', title: '执行日期', align: 'center', width: 120, render: row => row.executedDate ?? '-' },
      {
        key: 'result',
        title: '结果',
        align: 'center',
        width: 100,
        render: row => {
          // 未知值兜底成灰并原样显示，别静默按「通过」渲染
          const meta = RESULT_META[row.result] ?? { label: row.result, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      {
        key: 'fileName',
        title: '验证文档',
        align: 'center',
        minWidth: 160,
        render: row => {
          if (!row.fileName) return '-';
          return (
            <NButton text type="primary" onClick={() => handleDownloadDoc(row)}>
              {row.fileName}
            </NButton>
          );
        }
      },
      { key: 'createTime', title: '创建时间', align: 'center', minWidth: 160 },
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 90,
        render: row => {
          // append-only：只留「编辑」，不提供删除（合规上验证记录不可删改）
          if (!hasAuth('compliance:validationRecord:edit')) return null;
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
    <ValidationRecordSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="3Q 验证记录" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <!-- append-only：去掉勾选列与批量删除，只保留「新增 / 刷新」 -->
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :loading="loading"
          :show-add="hasAuth('compliance:validationRecord:add')"
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

    <ValidationRecordOperateDrawer
      v-model:visible="drawerVisible"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
    />
  </div>
</template>

<style scoped></style>
