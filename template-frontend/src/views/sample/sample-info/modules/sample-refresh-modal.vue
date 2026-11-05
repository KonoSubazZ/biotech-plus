<script setup lang="tsx">
import { computed } from 'vue';
import type { DataTableColumns } from 'naive-ui';

defineOptions({
  name: 'SampleRefreshModal'
});

/** 弹窗里展示的一行：同订单多样本时用 subbarcode 区分 */
interface RefreshRow {
  subbarcode: string;
  label: string;
  oldValue: string | null;
  newValue: string | null;
}

interface Props {
  /** 本次所有变更（当前样本 + 同订单其他样本） */
  rows: RefreshRow[];
  /** 点击的那条样本编号（标题用） */
  sampleCode: string;
  /** 操作人与时间 */
  by: string;
  at: string;
  /** 是否显示「样本编号」列（同订单多样本时才有意义） */
  showCode: boolean;
}

const props = defineProps<Props>();

const visible = defineModel<boolean>('visible', { default: false });

const columns = computed<DataTableColumns<RefreshRow>>(() => {
  const base: DataTableColumns<RefreshRow> = [
    {
      title: '字段',
      key: 'label',
      width: 130
    },
    {
      title: '旧值',
      key: 'oldValue',
      ellipsis: { tooltip: true },
      render: row => row.oldValue ?? '—'
    },
    {
      title: '新值',
      key: 'newValue',
      ellipsis: { tooltip: true },
      render: row => row.newValue ?? '—'
    }
  ];
  if (!props.showCode) {
    return base;
  }
  return [{ title: '样本编号', key: 'subbarcode', width: 180, ellipsis: { tooltip: true } }, ...base];
});

/** 涉及到的样本数（标题里给出，便于知道同订单还有几条被刷了） */
const sampleCount = computed(() => new Set(props.rows.map(item => item.subbarcode)).size);
</script>

<template>
  <NModal
    v-model:show="visible"
    preset="card"
    :title="showCode ? 'CMS 数据有更新（含同订单样本）' : `CMS 数据有更新（${sampleCode}）`"
    class="w-820px max-w-94%"
    :mask-closable="false"
  >
    <div class="mb-12px text-13px text-#666">
      本次共 {{ rows.length }} 项变更<span v-if="showCode">（涉及 {{ sampleCount }} 条样本）</span> · 操作人
      {{ by }} · {{ at }}
    </div>
    <NDataTable size="small" :columns="columns" :data="rows" :max-height="360" />
    <template #footer>
      <div class="flex justify-end">
        <NButton type="primary" @click="visible = false">知道了</NButton>
      </div>
    </template>
  </NModal>
</template>
