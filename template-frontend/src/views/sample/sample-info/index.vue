<script setup lang="tsx">
import { computed, ref } from 'vue';
import { NButton, NDivider } from 'naive-ui';
import {
  fetchBatchDeleteCmsSample,
  fetchGetCmsSampleList,
  fetchRefreshCmsSample
} from '@/service/api/report/cms-sample';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import { DEFAULT_VISIBLE_COLUMNS, CMS_SAMPLE_FIELDS } from './modules/cms-sample-fields';
import SampleInfoImportModal from './modules/sample-info-import-modal.vue';
import SampleRefreshModal from './modules/sample-refresh-modal.vue';
import SampleInfoOperateDrawer from './modules/sample-info-operate-drawer.vue';
import SampleInfoSearch from './modules/sample-info-search.vue';

defineOptions({
  name: 'SampleInfoList'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

const importModalVisible = ref(false);

const searchParams = ref<Api.Report.CmsSampleSearchParams>({
  pageNum: 1,
  pageSize: 10,
  subbarcode: null,
  orderNo: null,
  customerName: null,
  productName: null,
  sampleType: null,
  params: {}
});

/** 表头文案覆盖：列表里用短名，抽屉/列设置里仍用完整业务名 */
const SHORT_TITLES: Record<string, string> = {
  customerName: '客户名称',
  projectNo: '项目编号',
  contractNo: '合同编号',
  schemeNo: '方案编号'
};

/** 默认 15 列的宽度预算：合计控制在 1600 内，1900 视口不用横向滚动 */
const COLUMN_WIDTHS: Record<string, number> = {
  subbarcode: 170,
  sampleName: 90,
  productName: 100,
  birthYear: 80,
  sex: 50,
  sampleType: 100,
  diseaseType: 150,
  sampleAttribute: 80,
  collectionSite: 85,
  primaryTumorSite: 85,
  customerName: 80,
  projectNo: 75,
  researchCenter: 105,
  collectionDate: 100,
  remark: 95
};

/** 列表列 = 45 个设计字段；默认显示 15 个关键列，其余通过「列设置」打开 */
const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetCmsSampleList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      ...CMS_SAMPLE_FIELDS.map(field => ({
        key: field.key,
        title: SHORT_TITLES[field.key] ?? field.label,
        align: 'center' as const,
        width: COLUMN_WIDTHS[field.key] ?? (field.long ? 220 : 140),
        ellipsis: field.key === 'subbarcode' ? false : { tooltip: true },
        className: field.key === 'subbarcode' ? 'barcode-cell' : undefined
      })),
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 150,
        fixed: 'right',
        render: row => {
          // 每个按钮单独判权限；两个都没有时连分隔线都不渲染
          const divider = () => {
            if (!hasAuth('report:sampleInfo:edit')) {
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
                onClick={() => openEdit(row.id)}
              />
            );
          };

          const refreshBtn = () => {
            if (!hasAuth('report:sampleInfo:edit')) {
              return null;
            }
            return (
              <ButtonIcon
                text
                type="primary"
                icon="material-symbols:refresh-rounded"
                tooltipContent="从 CMS 刷新"
                loading={refreshingId.value === row.id}
                onClick={() => handleRefresh(row)}
              />
            );
          };

          return (
            <div class="flex-center gap-8px">
              <NButton text type="primary" onClick={() => openDetail(row)}>
                查看
              </NButton>
              {divider()}
              {editBtn()}
              {divider()}
              {refreshBtn()}
            </div>
          );
        }
      }
    ]
  });

const { drawerVisible, operateType, editingData, handleAdd, handleEdit, checkedRowKeys, onBatchDeleted } =
  useTableOperate(data, 'id', getData);

/**
 * 默认只勾选 9 个关键列。
 * 说明：useNaivePaginatedTable 的 getColumnVisible 只影响「列设置里的 visible 标记」，
 * 真正决定表格列的是 columnChecks[].checked（getColumns 只过滤 checked），
 * 所以这里在 setup 阶段把非默认列的 checked 置 false。
 */
const DEFAULT_COLUMN_SET = new Set(DEFAULT_VISIBLE_COLUMNS);
columnChecks.value.forEach(item => {
  if (item.key === '__selection__' || item.key === 'operate') return;
  item.checked = DEFAULT_COLUMN_SET.has(item.key);
});

// 列顺序按 DEFAULT_VISIBLE_COLUMNS 排（否则会按表结构顺序，诺禾编号被订单编号挤到后面）
const columnRank = new Map(DEFAULT_VISIBLE_COLUMNS.map((key, index) => [key, index + 1]));
const originalRank = new Map(columnChecks.value.map((item, index) => [item.key, index]));
const FIXED_COLUMN_RANK: Record<string, number> = { __selection__: 0, operate: 999 };
columnChecks.value.sort((a, b) => {
  const rankOf = (key: string) => FIXED_COLUMN_RANK[key] ?? columnRank.get(key) ?? 100;
  return rankOf(a.key) - rankOf(b.key) || (originalRank.get(a.key) ?? 0) - (originalRank.get(b.key) ?? 0);
});

/** 抽屉形态：查看 / 新增 / 编辑 */
const drawerMode = ref<'view' | 'add' | 'edit'>('add');

function openDetail(row: Api.Report.CmsSample) {
  drawerMode.value = 'view';
  editingData.value = row;
  drawerVisible.value = true;
}

function openAdd() {
  drawerMode.value = 'add';
  handleAdd();
}

function openEdit(id: CommonType.IdType) {
  drawerMode.value = 'edit';
  handleEdit(id);
}

/** 批量删除：与单条删除共用同一个批量接口 */
async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteCmsSample(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 行内「从 CMS 刷新」：有变更弹窗列字段级差异，无变更给轻提示 */
const refreshingId = ref<number | null>(null);
const refreshVisible = ref(false);
const refreshResult = ref<Api.Report.CmsSampleRefresh | null>(null);

/** 弹窗行：当前样本 + 同订单其他样本（双样本联动）的变更平铺，带样本编号 */
interface RefreshRow {
  subbarcode: string;
  label: string;
  oldValue: string | null;
  newValue: string | null;
}

const refreshRows = computed<RefreshRow[]>(() => {
  const result = refreshResult.value;
  if (!result) return [];
  const rows: RefreshRow[] = [];
  const push = (item: Api.Report.CmsSampleRefresh, code: string) => {
    for (const change of item.changes ?? []) {
      if (change.field === '__insert__') continue;
      rows.push({ subbarcode: code, label: change.label, oldValue: change.oldValue, newValue: change.newValue });
    }
  };
  push(result, result.sample?.subbarcode ?? '');
  for (const sibling of result.siblings ?? []) {
    push(sibling, sibling.sample?.subbarcode ?? '');
  }
  return rows;
});

/** 变更是否涉及别的样本（双样本联动时弹窗多一列「样本编号」，标题也不再用单条编号） */
const refreshShowCode = computed(() => {
  const primaryCode = refreshResult.value?.sample?.subbarcode ?? '';
  return refreshRows.value.some(row => row.subbarcode !== primaryCode)
    || new Set(refreshRows.value.map(row => row.subbarcode)).size > 1;
});

async function handleRefresh(row: Api.Report.CmsSample) {
  if (!row.subbarcode) {
    window.$message?.error('该行没有诺禾编号，无法刷新');
    return;
  }
  refreshingId.value = row.id ?? null;
  // 按编号刷新：列表可能是旧快照（行已删除/重建，id 变了），库里没有就按 CMS 补回来
  const { data, error } = await fetchRefreshCmsSample(row.subbarcode);
  refreshingId.value = null;
  if (error) return;
  // 双样本：同订单其他样本也一起刷了，changed 取并集（主样本没变、兄弟变了也要弹）
  const allChanges = [data, ...(data.siblings ?? [])].flatMap(item => item.changes ?? []);
  if (allChanges.length === 0) {
    window.$message?.success('与 CMS 数据一致，无需更新');
    return;
  }
  const inserted = allChanges.some(item => item.field === '__insert__');
  const fieldChanges = allChanges.filter(item => item.field !== '__insert__');
  refreshResult.value = data;
  refreshVisible.value = fieldChanges.length > 0;
  getData();
  if (inserted) {
    window.$message?.success('该样本已不存在，已按 CMS 数据重新同步');
  }
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
          @add="openAdd"
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
      :mode="drawerMode"
      :operate-type="operateType"
      :row-data="editingData"
      @submitted="getData"
      @edit="drawerMode = 'edit'"
    />

    <SampleInfoImportModal v-model:visible="importModalVisible" @submitted="getData" />

    <SampleRefreshModal
      v-model:visible="refreshVisible"
      :rows="refreshRows"
      :show-code="refreshShowCode"
      :sample-code="refreshResult?.sample?.subbarcode ?? ''"
      :by="refreshResult?.by ?? ''"
      :at="refreshResult?.at ?? ''"
    />
  </div>
</template>

<style scoped>
/* 诺禾编号固定单行展示，不换行、不省略 */
:deep(.barcode-cell) {
  white-space: nowrap;
}
</style>
