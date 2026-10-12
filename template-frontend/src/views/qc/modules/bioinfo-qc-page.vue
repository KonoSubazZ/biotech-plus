<script setup lang="tsx">
import { computed, ref } from 'vue';
import { NButton, NDivider, NEllipsis, NModal, NTag } from 'naive-ui';
import { fetchBatchDeleteBioinfoQc, fetchGetBioinfoQcList } from '@/service/api/qc/bioinfo-qc';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import QcNotifyModal from './qc-notify-modal.vue';
import BioinfoQcOperateDrawer from './bioinfo-qc-operate-drawer.vue';
import BioinfoQcSearch from './bioinfo-qc-search.vue';

defineOptions({
  name: 'BioinfoQcPage'
});

const appStore = useAppStore();
const { hasAuth } = useAuth();

/**
 * 状态：与后端一致（passed / failed / pending），配色契约见 UX-CONTRACT ——
 * 绿=合格、红=不合格、黄=待判定。
 */
const STATUS_META: Record<string, { label: string; type: 'warning' | 'success' | 'error' }> = {
  pending: { label: '待判定', type: 'warning' },
  passed: { label: '合格', type: 'success' },
  failed: { label: '不合格', type: 'error' }
};

/**
 * 数据列：[行字段, 表头, 默认是否显示]。
 * <p>
 * 表头暂时直接用数据字段名（字段还没有中文名，确认后只改这一处）。
 * 宽表列展示契约：固定宽 + 缩略，超出截断；鼠标悬停看完整值；点单元格看整行详情。
 * 默认只显示「样本编号 + 状态 + 文库编号 + 6 个关键指标」，保证一屏放下、不出横向滚动条；
 * 其余列（含 bam、target_size、coverage_target_1000x 等）在右上角「列设置」里随时打开。
 */
const DATA_COLUMNS: [string, string, boolean][] = [
  ['libId', 'lib_id', true],
  ['bam', 'bam', false],
  ['gender', 'gender', false],
  ['paired', 'paired', false],
  ['rawGc', 'raw_gc', false],
  ['hotPos', 'hot_pos', false],
  ['rawQ20', 'raw_q20', false],
  ['rawQ30', 'raw_q30', false],
  ['pipeline', 'pipeline', false],
  ['rawBase', 'raw_base', true],
  ['sshPath', 'ssh_path', false],
  ['duplicate', 'duplicate', false],
  ['peakSize', 'peak_size', true],
  ['qcResult', 'qc_result', false],
  ['sqi16', 'sqi16', false],
  ['bioResult', 'bio_result', false],
  ['peakRange', 'peak_range', false],
  ['concordance', 'concordance', false],
  ['chrall', 'chrall', false],
  ['librarySize', 'library_size', false],
  ['bamRealign', 'bam_realign', false],
  ['targetSize', 'target_size', false],
  ['totalReads', 'total_reads', false],
  ['mappingRate', 'mapping_rate', true],
  ['averageDepth', 'average_depth', true],
  ['coverageTarget', 'coverage_target', true],
  ['mappedReads', 'mapped_reads', false],
  ['duplicateTarget', 'duplicate_target', false],
  ['contaminationLevel', 'contamination_level', false],
  ['coverageTarget50x', 'coverage_target_50x', false],
  ['probeCaptureRatio', 'probe_capture_ratio', true],
  ['coverageTarget500x', 'coverage_target_500x', false],
  ['coverageTarget1000x', 'coverage_target_1000x', false],
  ['coverageUniformity02xadOnTarget', 'coverage_uniformity_02xad_on_target', false],
  ['beforeDuplicate', 'before_duplicate', false]
];

const detailVisible = ref(false);
const detailField = ref<{ label: string; value: string } | null>(null);

const searchParams = ref<Api.Qc.BioinfoQcSearchParams>({
  pageNum: 1,
  pageSize: 10,
  subbarcode: null,
  libId: null,
  status: null,
  params: {}
});

function cellText(row: Api.Qc.BioinfoQc, key: string): string {
  const value = (row as unknown as Record<string, unknown>)[key];
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return String(value);
}

/**
 * 单元格统一渲染：缩略 + 悬停提示 + 可点。
 * <p>
 * 缩略与悬停提示用 NEllipsis（`tooltip` 开自带提示，内容就是这段文本 → 与表头同一套 tooltip 样式）；
 * 只有内容放不下时才出提示（NEllipsis 自带溢出判定）。外层 div 负责点击 → 看这一个字段的完整值，
 * 悬停变蓝（hover:text-primary）提示「可点」。
 */
function renderCell(row: Api.Qc.BioinfoQc, key: string, label: string) {
  const value = cellText(row, key);
  return (
    <div class="cursor-pointer hover:text-primary" onClick={() => openCell(row, key, label)}>
      <NEllipsis tooltip>{value}</NEllipsis>
    </div>
  );
}

/**
 * 点单元格 = 看**这一个字段**的完整值（不是整行）。
 * <p>
 * 悬停看缩略提示由表格自带 ellipsis 负责（与表头同一套 tooltip 样式），点开这里看全值。
 */
function openCell(row: Api.Qc.BioinfoQc, key: string, label: string) {
  detailField.value = { label, value: cellText(row, key) };
  detailVisible.value = true;
}

const detailTitle = computed(() => detailField.value?.label ?? '');
const detailValue = computed(() => detailField.value?.value ?? '-');

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetBioinfoQcList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      {
        key: 'subbarcode',
        title: 'subbarcode',
        align: 'center',
        width: 150,
        fixed: 'left',
        // 列级 ellipsis 管「表头」的缩略 + 悬停提示；单元格的提示由 renderCell 里的 NEllipsis 负责
        ellipsis: { tooltip: true },
        render: row => renderCell(row, 'subbarcode', 'subbarcode')
      },
      {
        key: 'status',
        title: 'status',
        align: 'center',
        width: 90,
        fixed: 'left',
        render: row => {
          const meta = STATUS_META[row.status ?? ''] ?? { label: row.status, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      ...DATA_COLUMNS.map(([key, title]) => ({
        key,
        title,
        align: 'center' as const,
        width: key === 'libId' || key === 'bam' ? 150 : 104,
        ellipsis: { tooltip: true },
        render: (row: Api.Qc.BioinfoQc) => renderCell(row, key, title)
      })),
      {
        key: 'operate',
        title: $t('common.operate'),
        align: 'center',
        width: 120,
        fixed: 'right',
        render: row => {
          // 顺序固定「修改 | 删除 | 处理」：按条件出现的「处理」放最后，
          // 这样不同行的 修改/删除 永远在同一列（对齐），不会因为多一个按钮就整体错位
          const buttons = [editBtn(row), deleteBtn(row), fixBtn(row)].filter(item => item !== null);
          // 间隙收窄：3 个按钮 + 两根分隔线也要能被 120px 的操作列包住，不然整组会被挤得偏左
          return <div class="flex-center gap-4px">{buttons.map(joinItem)}</div>;
        }
      }
    ]
  });

/** 按钮之间插分隔线（按实际渲染出的按钮个数算，避免出现多余的分隔线） */
function joinItem(item: unknown, index: number) {
  if (index === 0) {
    return item;
  }
  return (
    <>
      <NDivider vertical />
      {item}
    </>
  );
}

/**
 * 处理：只在「不合格」的行上出现。处理逻辑待定（后续接不合格样本的处理流程），
 * 先按占位交付：点一下给提示，不做任何写入。
 */
function fixBtn(row: Api.Qc.BioinfoQc) {
  // 占位（invisible 保留宽度）→ 修改/删除 在所有行都对齐
  const placeholder = (
    <ButtonIcon
      text
      class="invisible"
      type="warning"
      icon="material-symbols:handyman-outline"
      tooltipContent="处理"
    />
  );
  if (row.status !== 'failed' || !hasAuth('qc:notify:query')) {
    return placeholder;
  }
  // 处理 = 打开「样本质控不合格通知」弹窗（内容与通知文档一致，可编辑后生成 DOCX）
  return (
    <ButtonIcon
      text
      type="warning"
      icon="material-symbols:handyman-outline"
      tooltipContent="处理"
      onClick={() => openNotify(row)}
    />
  );
}

const notifyVisible = ref(false);
const notifySubbarcode = ref('');

/** 打开处理弹窗（通知单按样本编号预填） */
function openNotify(row: Api.Qc.BioinfoQc) {
  notifySubbarcode.value = row.subbarcode ?? '';
  notifyVisible.value = true;
}

function editBtn(row: Api.Qc.BioinfoQc) {
  if (!hasAuth('qc:bioinfo:edit')) {
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
}

function deleteBtn(row: Api.Qc.BioinfoQc) {
  if (!hasAuth('qc:bioinfo:remove')) {
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
}

const { drawerVisible, editingData, checkedRowKeys, onBatchDeleted, onDeleted, handleEdit } =
  useTableOperate(data, 'id', getData);

/** 默认关掉的长尾列（「列设置」里仍能打开）—— 见 DATA_COLUMNS 的第三个字段 */
const HIDDEN_BY_DEFAULT = new Set(DATA_COLUMNS.filter(([, , visible]) => !visible).map(([key]) => key));

// hooks 里所有列的 checked 默认是 true（「列设置」管的是它），宽表默认只留常用列：
// 这里换成新数组触发列重算，用户仍可在右上角「列设置」里把其余列打开
columnChecks.value = columnChecks.value.map(item =>
  HIDDEN_BY_DEFAULT.has(String(item.key)) ? { ...item, checked: false } : item
);

/** 批量删除：与单条删除共用同一个批量接口 */
async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteBioinfoQc(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteBioinfoQc([id]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <BioinfoQcSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="生信质控" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <!-- 数据由接口灌入：不提供新增；删除保留（行内 + 批量），导出不做 -->
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="false"
          :show-delete="hasAuth('qc:bioinfo:remove')"
          :show-export="false"
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

    <BioinfoQcOperateDrawer v-model:visible="drawerVisible" :row-data="editingData" @submitted="getData" />

    <!-- 处理：样本质控不合格通知（内容与文档一致，生成 DOCX 并留一条记录） -->
    <QcNotifyModal
      v-model:visible="notifyVisible"
      qc-category="bioinfo"
      :subbarcode="notifySubbarcode"
      @generated="getData"
    />

    <!-- 点单元格看**这一个字段**的完整值（列里放不下的内容在这里看全；不复制、不跳转） -->
    <NModal v-model:show="detailVisible" preset="card" :title="detailTitle" class="w-40%">
      <div class="break-all">{{ detailValue }}</div>
    </NModal>
  </div>
</template>

<style scoped></style>
