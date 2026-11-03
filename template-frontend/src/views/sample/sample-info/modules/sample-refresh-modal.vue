<script setup lang="tsx">
import type { DataTableColumns } from 'naive-ui';

defineOptions({
  name: 'SampleRefreshModal'
});

interface Props {
  /** 由行内刷新接口返回的字段变更清单 */
  changes: Api.Report.CmsFieldChange[];
  /** 样本编号（标题里展示） */
  sampleCode: string;
  /** 操作人与时间 */
  by: string;
  at: string;
}

defineProps<Props>();

const visible = defineModel<boolean>('visible', { default: false });

const columns: DataTableColumns<Api.Report.CmsFieldChange> = [
  { title: '字段', key: 'label', width: 140 },
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
</script>

<template>
  <NModal
    v-model:show="visible"
    preset="card"
    :title="`CMS 数据有更新（${sampleCode}）`"
    class="w-760px max-w-94%"
    :mask-closable="false"
  >
    <div class="mb-12px text-13px text-#666">
      本次共 {{ changes.length }} 项变更 · 操作人 {{ by }} · {{ at }}
    </div>
    <NDataTable size="small" :columns="columns" :data="changes" :max-height="340" />
    <template #footer>
      <div class="flex justify-end">
        <NButton type="primary" @click="visible = false">知道了</NButton>
      </div>
    </template>
  </NModal>
</template>
