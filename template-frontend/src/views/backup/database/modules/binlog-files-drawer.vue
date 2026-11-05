<script setup lang="tsx">
import { computed } from 'vue';

defineOptions({
  name: 'BinlogFilesDrawer'
});

interface Props {
  /** 当前增量记录 */
  row?: Api.Backup.DbBinlog | null;
}

const props = withDefaults(defineProps<Props>(), { row: null });
const visible = defineModel<boolean>('visible', { required: true });

/** files_json 由脚本上传时生成（含 md5），页面只展示 */
const files = computed<{ file: string; size?: number; md5?: string }[]>(() => {
  if (!props.row?.filesJson) return [];
  try {
    const parsed = JSON.parse(props.row.filesJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
});

function fmtSize(size?: number) {
  if (!size) return '-';
  return size >= 1048576 ? `${(size / 1048576).toFixed(1)} MB` : `${(size / 1024).toFixed(0)} KB`;
}
</script>

<template>
  <NDrawer v-model:show="visible" :width="720" placement="right">
    <NDrawerContent title="文件清单" closable>
      <NSpace vertical :size="12">
        <NDescriptions :column="2" label-placement="left" size="small" bordered>
          <NDescriptionsItem label="起点">
            {{ row ? `${row.startFile} @ ${row.startPos}` : '-' }}
          </NDescriptionsItem>
          <NDescriptionsItem label="终点">
            {{ row ? `${row.endFile} @ ${row.endPos}` : '-' }}
          </NDescriptionsItem>
          <NDescriptionsItem label="文件数">{{ row?.fileCount ?? '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="总大小">{{ fmtSize(row?.totalSize) }}</NDescriptionsItem>
        </NDescriptions>

        <NDataTable
          :columns="[
            { key: 'file', title: '文件', minWidth: 160 },
            { key: 'size', title: '大小', width: 110, render: (r: any) => fmtSize(r.size) },
            { key: 'md5', title: 'MD5（上传时计算）', minWidth: 260, ellipsis: { tooltip: true } }
          ]"
          :data="files"
          :row-key="(row: any) => row.file"
          size="small"
        />

      </NSpace>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
