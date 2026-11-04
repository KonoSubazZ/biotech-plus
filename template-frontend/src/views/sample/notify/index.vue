<script setup lang="tsx">
import { ref } from 'vue';
import { NButton, NDivider, NTag } from 'naive-ui';
import { fetchGetNotifyPage } from '@/service/api/qc/qc-notify';
import { useDownload } from '@/hooks/business/download';
import { defaultTransform, useNaivePaginatedTable } from '@/hooks/common/table';
import { $t } from '@/locales';
import NotifySearch from './modules/notify-search.vue';
import NotifySendModal from './modules/notify-send-modal.vue';
import NotifyDetailModal from './modules/notify-detail-modal.vue';

defineOptions({
  name: 'SampleNotifyList'
});

/** 通知类型标签 */
const TYPE_META: Record<string, { label: string; type: 'default' | 'info' | 'success' | 'warning' | 'error' }> = {
  delay: { label: '延期通知', type: 'warning' },
  sample: { label: '样本不合格', type: 'error' },
  wet_lab: { label: '湿实验不合格', type: 'error' },
  bioinfo: { label: '干实验不合格', type: 'error' }
};

/** 状态标签：待处理=黄、发送成功=绿、发送失败=红 */
const STATUS_META: Record<string, { label: string; type: 'default' | 'info' | 'success' | 'warning' | 'error' }> = {
  generated: { label: '已生成（待提交）', type: 'default' },
  pending: { label: '待处理', type: 'warning' },
  sent: { label: '发送成功', type: 'success' },
  failed: { label: '发送失败', type: 'error' }
};

const searchParams = ref<Api.Qc.NotifyQuery>({
  pageNum: 1,
  pageSize: 10,
  notifyType: null,
  notifyStatus: null,
  notifyDateStart: null,
  notifyDateEnd: null,
  subbarcode: null,
  projectNo: null,
  keyword: null
});

const { zip } = useDownload();

const { columns, data, getData, getDataByPage, loading, mobilePagination, scrollX } = useNaivePaginatedTable({
  api: () => fetchGetNotifyPage(searchParams.value),
  transform: response => defaultTransform(response),
  onPaginationParamsChange: params => {
    searchParams.value.pageNum = params.page ?? 1;
    searchParams.value.pageSize = params.pageSize ?? 10;
  },
  columns: () => [
    {
      key: 'notifyType',
      title: '通知类型',
      align: 'center',
      width: 110,
      render: row => {
        const meta = TYPE_META[row.notifyType] ?? { label: row.notifyType, type: 'default' as const };
        return (
          <NTag type={meta.type} size="small" bordered={false}>
            {meta.label}
          </NTag>
        );
      }
    },
    {
      key: 'notifyStatus',
      title: '状态',
      align: 'center',
      width: 130,
      render: row => {
        const meta = STATUS_META[row.notifyStatus] ?? { label: row.notifyStatus, type: 'default' as const };
        const tag = (
          <NTag type={meta.type} size="small" bordered={false}>
            {meta.label}
          </NTag>
        );
        return row.sendError ? <span title={row.sendError}>{tag}</span> : tag;
      }
    },
    { key: 'subbarcode', title: '样本编号', align: 'center', width: 150, ellipsis: { tooltip: true } },
    { key: 'sampleName', title: '样本名称', align: 'center', width: 110, ellipsis: { tooltip: true } },
    { key: 'participantNo', title: '试验参与者编号', align: 'center', width: 140, ellipsis: { tooltip: true } },
    { key: 'projectNo', title: '项目编号', align: 'center', width: 110, ellipsis: { tooltip: true } },
    { key: 'notifyDate', title: '通知日期', align: 'center', width: 100 },
    { key: 'studyNo', title: '研究方案编号', align: 'center', width: 140, ellipsis: { tooltip: true } },
    {
      key: 'filePath',
      title: '文件路径',
      align: 'center',
      width: 190,
      ellipsis: { tooltip: true },
      render: row => {
        const name = row.filePath ? row.filePath.split('/').pop() : '';
        if (!name) return '-';
        // 有签名件优先读签名件，其次读转化后的 PDF；只有 DOCX 时不可阅读
        const src = row.signedPdfPath
          ? `/qc/notify/${row.id}/signed-pdf`
          : row.pdfPath
            ? `/qc/notify/${row.id}/pdf`
            : '';
        return <PdfPreview src={src} name={name} />;
      }
    },
    { key: 'handlerName', title: '处理人', align: 'center', width: 90 },
    { key: 'handlerTime', title: '处理时间', align: 'center', width: 150 },
    { key: 'senderName', title: '发送人', align: 'center', width: 90 },
    { key: 'senderTime', title: '发送时间', align: 'center', width: 150 },
    {
      key: 'operate',
      title: $t('common.operate'),
      align: 'center',
      width: 190,
      fixed: 'right',
      render: row => (
        <div class="flex-center gap-8px">
          <NButton text type="primary" onClick={() => openDetail(row)}>
            查看
          </NButton>
          <NDivider vertical />
          <NButton text type="primary" disabled={!row.filePath} onClick={() => download(row)}>
            下载
          </NButton>
          <NDivider vertical />
          <NButton
            text
            type="primary"
            disabled={row.notifyStatus !== 'pending' && row.notifyStatus !== 'failed'}
            onClick={() => openSend(row)}
          >
            发送
          </NButton>
        </div>
      )
    }
  ]
});

const sendVisible = ref(false);
const detailVisible = ref(false);
const currentId = ref<number | null>(null);

function openSend(row: Api.Qc.NotifyRow) {
  currentId.value = row.id;
  sendVisible.value = true;
}

function openDetail(row: Api.Qc.NotifyRow) {
  currentId.value = row.id;
  detailVisible.value = true;
}

function download(row: Api.Qc.NotifyRow) {
  if (!row.filePath) return;
  const name = row.filePath.split('/').pop() ?? `${row.subbarcode}.pdf`;
  zip(`/qc/notify/${row.id}/signed-pdf`, name);
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <NotifySearch v-model:model="searchParams" @search="getDataByPage" />

    <NCard title="异常处理" :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <template #header-extra>
        <NButton size="small" ghost type="primary" :loading="loading" @click="getData">
          <template #icon>
            <icon-material-symbols-refresh-rounded class="text-icon" />
          </template>
          刷新
        </NButton>
      </template>

      <NDataTable
        :columns="columns"
        :data="data"
        :flex-height="true"
        :loading="loading"
        :pagination="mobilePagination"
        :row-key="row => row.id"
        :scroll-x="scrollX"
        remote
        size="small"
        class="sm:h-full"
      />
    </NCard>

    <NotifySendModal v-model:visible="sendVisible" :notify-id="currentId" @sent="getData" />
    <NotifyDetailModal v-model:visible="detailVisible" :notify-id="currentId" />
  </div>
</template>
