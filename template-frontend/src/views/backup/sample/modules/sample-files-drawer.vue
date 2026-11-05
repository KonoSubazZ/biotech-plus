<script setup lang="tsx">
import { ref, watch } from 'vue';
import { NTag } from 'naive-ui';
import { fetchGetSampleFiles } from '@/service/api/backup/sample-backup';

defineOptions({
  name: 'SampleFilesDrawer'
});

interface Props {
  /** 当前样本（列表行） */
  row?: Api.Backup.SampleRow | null;
}

const props = withDefaults(defineProps<Props>(), { row: null });
const visible = defineModel<boolean>('visible', { required: true });

const files = ref<Api.Backup.SampleFile[]>([]);
const loading = ref(false);

/** 类别与状态的中文口径：与 backup_sample_file.file_category / backup_status 注释一致 */
const CATEGORY_META: Record<string, string> = {
  report_docx: '报告 DOCX',
  report_pdf: '报告 PDF',
  report_json: '报告 JSON',
  report_signed_pdf: '签署 PDF',
  notify_docx: '通知 DOCX',
  notify_pdf: '通知 PDF',
  notify_json: '通知 JSON',
  notify_signed_pdf: '通知签署 PDF',
  fastq: 'FASTQ',
  bam: 'BAM',
  vcf: 'VCF',
  other: '其他'
};

const FILE_STATUS_META: Record<number, { label: string; type: 'default' | 'info' | 'success' | 'warning' | 'error' }> =
  {
    0: { label: '待备份', type: 'default' },
    1: { label: '备份中', type: 'info' },
    2: { label: '成功', type: 'success' },
    3: { label: '失败', type: 'error' },
    4: { label: '跳过', type: 'warning' }
  };

function fmtSize(bytes?: number | null) {
  if (!bytes) return '-';
  if (bytes >= 1073741824) return `${(bytes / 1073741824).toFixed(2)} GB`;
  return bytes >= 1048576 ? `${(bytes / 1048576).toFixed(1)} MB` : `${(bytes / 1024).toFixed(0)} KB`;
}

async function loadFiles(sampleId: number) {
  loading.value = true;
  const { data, error } = await fetchGetSampleFiles(sampleId);
  loading.value = false;
  if (error) {
    files.value = [];
    return;
  }
  files.value = data ?? [];
}

/** 每次打开抽屉都重新拉一次明细（数据由脚本上报，列表可能是缓存的） */
watch(visible, value => {
  if (value && props.row) {
    loadFiles(props.row.sampleId);
  }
});

const columns = [
  {
    key: 'fileCategory',
    title: '类别',
    width: 110,
    render: (row: Api.Backup.SampleFile) => CATEGORY_META[row.fileCategory] ?? row.fileCategory
  },
  {
    key: 'fileName',
    title: '文件名',
    minWidth: 260,
    ellipsis: { tooltip: true },
    render: (row: Api.Backup.SampleFile) => row.fileName || '-'
  },
  {
    key: 'fileSize',
    title: '大小',
    width: 100,
    align: 'right' as const,
    render: (row: Api.Backup.SampleFile) => fmtSize(row.fileSize)
  },
  {
    key: 'backupStatus',
    title: '状态',
    width: 90,
    align: 'center' as const,
    render: (row: Api.Backup.SampleFile) => {
      const meta = FILE_STATUS_META[row.backupStatus ?? 0] ?? FILE_STATUS_META[0];
      return <NTag type={meta.type}>{meta.label}</NTag>;
    }
  },
  {
    key: 'ossPath',
    title: 'OSS 路径',
    minWidth: 240,
    ellipsis: { tooltip: true },
    render: (row: Api.Backup.SampleFile) => row.ossPath || '-'
  },
  {
    key: 'failReason',
    title: '失败原因',
    minWidth: 160,
    ellipsis: { tooltip: true },
    render: (row: Api.Backup.SampleFile) => row.failReason || '-'
  }
];
</script>

<template>
  <NDrawer v-model:show="visible" :width="960" placement="right">
    <NDrawerContent title="文件明细" closable>
      <NSpace vertical :size="12">
        <NDescriptions :column="3" label-placement="left" size="small" bordered>
          <NDescriptionsItem label="样本编号">{{ row?.subbarcode ?? '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="项目编号">{{ row?.projectNo || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="订单批次">{{ row?.orderCode || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="产品">{{ row?.productBio || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="研究中心">{{ row?.researchCenter || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="文件">
            {{ row ? `${row.fileDone}/${row.fileTotal}` : '-' }}
            <span v-if="row && row.fileSkipped > 0" class="text-12px op-60">（跳过 {{ row.fileSkipped }}）</span>
          </NDescriptionsItem>
        </NDescriptions>

        <NDataTable
          :columns="columns"
          :data="files"
          :loading="loading"
          :row-key="(item: Api.Backup.SampleFile) => item.id"
          :scroll-x="1000"
          size="small"
        />
      </NSpace>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
