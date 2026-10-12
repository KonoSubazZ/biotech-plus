<script setup lang="tsx">
import { computed, ref } from 'vue';
import { NDivider, NEllipsis, NModal, NTag } from 'naive-ui';
import { fetchBatchDeleteWetLabQc, fetchGetWetLabQcList } from '@/service/api/qc/wet-lab-qc';
import { useAppStore } from '@/store/modules/app';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import QcNotifyModal from './qc-notify-modal.vue';
import WetLabQcOperateDrawer from './wet-lab-qc-operate-drawer.vue';
import WetLabQcSearch from './wet-lab-qc-search.vue';

defineOptions({
  name: 'WetLabQcPage'
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
 * 数据列：[行字段, 表头（中文名，与 qc_standard.qc_item 一致）, 对应列名 = qc_param, 默认是否显示]。
 * <p>
 * 湿实验是**宽表：一行一个样本，每个质控项一列**（形态对齐生信质控 bioinfo_qc）。
 * 列名就是质控标准里的 qc_param（英文原始字段名）—— 新增质控项要同时加表列 + 加标准 + 这里加一行。
 * 宽表列展示契约：固定宽 + 缩略，超出截断；悬停看完整值；点单元格只看这一个字段。
 * 默认只显示 5 个常用项，保证一屏放得下、不出横向滚动条；三个「对照」项在右上角「列设置」里随时打开。
 */
const DATA_COLUMNS: [string, string, string, boolean][] = [
  ['pathoTumorPurity', '肿瘤纯度', 'patho_tumor_purity', true],
  ['dnaTotalNg', 'DNA总量（ng）', 'dna_total_ng', true],
  ['dnaDegradation', '检测结果判断', 'dna_degradation', true],
  ['preLibTotalNg', '预文库总量（ng）', 'pre_lib_total_ng', true],
  ['finalLibConc', '终文库浓度（ng/uL）', 'final_lib_conc', true],
  ['dnaTotalNgNormal', 'DNA总量（ng）对照', 'dna_total_ng_normal', false],
  ['preLibTotalNgNormal', '预文库总量（ng）对照', 'pre_lib_total_ng_normal', false],
  ['finalLibConcNormal', '终文库浓度（ng/uL）对照', 'final_lib_conc_normal', false]
];

const detailVisible = ref(false);
const detailField = ref<{ label: string; value: string } | null>(null);

const searchParams = ref<Api.Qc.WetLabQcSearchParams>({
  pageNum: 1,
  pageSize: 10,
  subbarcode: null,
  status: null,
  params: {}
});

function cellText(row: Api.Qc.WetLabQc, key: string): string {
  const value = (row as unknown as Record<string, unknown>)[key];
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return String(value);
}

/**
 * 单元格统一渲染：缩略 + 悬停提示（NEllipsis，与表头同一套 tooltip） + 悬停变蓝 + 点击看单字段。
 */
function renderCell(row: Api.Qc.WetLabQc, key: string, label: string) {
  const value = cellText(row, key);
  return (
    <div class="cursor-pointer hover:text-primary" onClick={() => openCell(row, key, label)}>
      <NEllipsis tooltip>{value}</NEllipsis>
    </div>
  );
}

/** 点单元格 = 看**这一个字段**的完整值（不是整行） */
function openCell(row: Api.Qc.WetLabQc, key: string, label: string) {
  detailField.value = { label, value: cellText(row, key) };
  detailVisible.value = true;
}

const detailTitle = computed(() => detailField.value?.label ?? '');
const detailValue = computed(() => detailField.value?.value ?? '-');

const { columns, columnChecks, data, getData, getDataByPage, loading, mobilePagination, scrollX } =
  useNaivePaginatedTable({
    api: () => fetchGetWetLabQcList(searchParams.value),
    transform: response => defaultTransform(response),
    onPaginationParamsChange: params => {
      searchParams.value.pageNum = params.page;
      searchParams.value.pageSize = params.pageSize;
    },
    columns: () => [
      { type: 'selection', align: 'center', width: 48 },
      {
        key: 'subbarcode',
        title: '样本条码',
        align: 'center',
        width: 170,
        fixed: 'left',
        // 列级 ellipsis 管「表头」的缩略 + 悬停提示；单元格的提示由 renderCell 里的 NEllipsis 负责
        ellipsis: { tooltip: true },
        render: row => renderCell(row, 'subbarcode', '样本条码')
      },
      {
        key: 'status',
        title: '质控状态',
        align: 'center',
        width: 96,
        fixed: 'left',
        render: row => {
          const meta = STATUS_META[row.status ?? ''] ?? { label: row.status, type: 'default' as const };
          return <NTag type={meta.type}>{meta.label}</NTag>;
        }
      },
      ...DATA_COLUMNS.map(([key, title, param]) => ({
        key,
        title,
        align: 'center' as const,
        width: 140,
        ellipsis: { tooltip: true },
        render: (row: Api.Qc.WetLabQc) => renderCell(row, key, `${title}（${param}）`)
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
 * 处理：只在「不合格」的行上出现，打开「样本质控不合格通知」弹窗（内容与通知文档一致，生成 DOCX 并留一条记录）。
 * 湿实验与生信同一套逻辑，靠 qcCategory 区分：预填取湿实验质控记录、原因按湿实验标准算、落库记 wet_lab。
 */
function fixBtn(row: Api.Qc.WetLabQc) {
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

function editBtn(row: Api.Qc.WetLabQc) {
  if (!hasAuth('qc:wetLabQc:edit')) {
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

function deleteBtn(row: Api.Qc.WetLabQc) {
  if (!hasAuth('qc:wetLabQc:remove')) {
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

const notifyVisible = ref(false);
const notifySubbarcode = ref('');

/** 打开处理弹窗（通知单按样本编号预填） */
function openNotify(row: Api.Qc.WetLabQc) {
  notifySubbarcode.value = row.subbarcode ?? '';
  notifyVisible.value = true;
}

const { drawerVisible, editingData, checkedRowKeys, onBatchDeleted, onDeleted, handleEdit } =
  useTableOperate(data, 'id', getData);

/** 默认关掉的长尾列（「列设置」里仍能打开）—— 见 DATA_COLUMNS 的第四个字段 */
const HIDDEN_BY_DEFAULT = new Set(DATA_COLUMNS.filter(([, , , visible]) => !visible).map(([key]) => key));

// hooks 里所有列的 checked 默认是 true（「列设置」管的是它），这里换成新数组触发列重算，
// 用户仍可在右上角「列设置」里把 3 个「对照」列打开
columnChecks.value = columnChecks.value.map(item =>
  HIDDEN_BY_DEFAULT.has(String(item.key)) ? { ...item, checked: false } : item
);

/** 批量删除：与单条删除共用同一个批量接口，避免两边各写一套 */
async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteWetLabQc(checkedRowKeys.value);
  if (error) return;
  onBatchDeleted();
}

/** 单条删除：传一个 id 的数组 */
async function handleDelete(id: CommonType.IdType) {
  const { error } = await fetchBatchDeleteWetLabQc([id]);
  if (error) return;
  onDeleted();
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <WetLabQcSearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="湿实验质控" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <!-- 数据由接口灌入（POST /qc/wetLabQc/receive）：不提供新增，保留修改（人工修正质控值）/ 删除 / 列设置 -->
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          :show-add="false"
          :show-delete="hasAuth('qc:wetLabQc:remove')"
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

    <WetLabQcOperateDrawer v-model:visible="drawerVisible" :row-data="editingData" @submitted="getData" />

    <!-- 处理：样本质控不合格通知（与生信同一套，qcCategory=wet_lab） -->
    <QcNotifyModal
      v-model:visible="notifyVisible"
      qc-category="wet_lab"
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
