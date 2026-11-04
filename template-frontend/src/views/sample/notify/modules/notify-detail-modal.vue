<script setup lang="tsx">
import { computed, ref, watch } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { NTag } from 'naive-ui';
import { fetchGetNotifyDetail, fetchGetNotifyMailLogs } from '@/service/api/qc/qc-notify';

defineOptions({
  name: 'NotifyDetailModal'
});

interface Props {
  notifyId: number | null;
}

const props = defineProps<Props>();

const visible = defineModel<boolean>('visible', { required: true });

const loading = ref(false);
const detail = ref<Api.Qc.NotifyRow | null>(null);
const mailLogs = ref<Api.Qc.DeliveryEmailLog[]>([]);

const TYPE_LABEL: Record<string, string> = {
  delay: '延期通知',
  sample: '样本不合格',
  wet_lab: '湿实验不合格',
  bioinfo: '干实验不合格'
};
const STATUS_LABEL: Record<string, string> = {
  generated: '已生成（待提交）',
  pending: '待处理',
  sent: '发送成功',
  failed: '发送失败'
};
const SIGN_LABEL: Record<string, string> = {
  none: '未送签',
  submitted: '已送签',
  signed: '已签署'
};
const MAIL_STATUS: Record<string, { label: string; type: 'success' | 'warning' | 'error' }> = {
  SUCCESS: { label: '发送成功', type: 'success' },
  SENDING: { label: '发送中', type: 'warning' },
  FAILED: { label: '发送失败', type: 'error' }
};

const mailColumns = computed<DataTableColumns<Api.Qc.DeliveryEmailLog>>(() => [
  { key: 'requestedAt', title: '发送时间', width: 150, align: 'center' },
  {
    key: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    render: row => {
      const meta = MAIL_STATUS[row.status] ?? { label: row.status, type: 'warning' as const };
      return <NTag type={meta.type} size="small" bordered={false}>{meta.label}</NTag>;
    }
  },
  { key: 'recipients', title: '收件人', ellipsis: { tooltip: true } },
  { key: 'cc', title: '抄送', ellipsis: { tooltip: true } },
  { key: 'attachmentName', title: '附件', ellipsis: { tooltip: true } },
  { key: 'triggeredBy', title: '操作人', width: 100, align: 'center' },
  { key: 'failureReason', title: '失败原因', ellipsis: { tooltip: true } }
]);

async function load() {
  if (!props.notifyId) return;
  loading.value = true;
  try {
    const { data, error } = await fetchGetNotifyDetail(props.notifyId);
    if (!error) detail.value = data;
    const logs = await fetchGetNotifyMailLogs(props.notifyId);
    mailLogs.value = logs.error ? [] : (logs.data ?? []);
  } finally {
    loading.value = false;
  }
}

/** 只展示文件名，不把绝对路径铺在页面上 */
function fileName(path: string | null | undefined) {
  if (!path) return '';
  return path.split('/').pop() ?? '';
}

watch(visible, value => {
  if (value) load();
});
</script>

<template>
  <NModal v-model:show="visible" preset="card" title="通知详情" class="max-w-92% w-900px">
    <NSpin :show="loading">
      <NDescriptions
        v-if="detail"
        :column="2"
        bordered
        size="small"
        label-placement="left"
        :label-style="{ width: '130px' }"
      >
        <NDescriptionsItem label="通知类型">{{ TYPE_LABEL[detail.notifyType] ?? detail.notifyType }}</NDescriptionsItem>
        <NDescriptionsItem label="状态">{{ STATUS_LABEL[detail.notifyStatus] ?? detail.notifyStatus }}</NDescriptionsItem>
        <NDescriptionsItem label="签署状态">{{ SIGN_LABEL[detail.signStatus] ?? detail.signStatus }}</NDescriptionsItem>
        <NDescriptionsItem label="通知单号">{{ detail.notifyNo ?? '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="通知日期">{{ detail.notifyDate }}</NDescriptionsItem>
        <NDescriptionsItem label="样本编号">{{ detail.subbarcode }}</NDescriptionsItem>
        <NDescriptionsItem label="样本名称">{{ detail.sampleName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="研究方案编号">{{ detail.schemeNo || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="项目编号">{{ detail.projectNo || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="试验参与者编号">{{ detail.subjectNo || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="检测产品">{{ detail.productName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="客户">{{ detail.customerName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="样本类型">{{ detail.sampleType || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="处理人">{{ detail.handlerName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="处理时间">{{ detail.handlerTime || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="上上签合同号">{{ detail.signContractId || '-' }}</NDescriptionsItem>
      </NDescriptions>

      <div v-if="detail" class="mt-16px">
        <div class="mb-8px flex items-center gap-8px text-14px font-500">
          <span class="h-14px w-3px rounded-2px bg-primary"></span>
          附件
        </div>
        <NDescriptions :column="2" bordered size="small" label-placement="left" :label-style="{ width: '130px' }">
          <NDescriptionsItem label="通知单 PDF">
            <PdfPreview
              v-if="detail.pdfPath"
              :src="`/qc/notify/${detail.id}/pdf`"
              :name="fileName(detail.pdfPath)"
            />
            <span v-else>-</span>
          </NDescriptionsItem>
          <NDescriptionsItem label="签名件 PDF">
            <PdfPreview
              v-if="detail.signedPdfPath"
              :src="`/qc/notify/${detail.id}/signed-pdf`"
              :name="fileName(detail.signedPdfPath)"
            />
            <span v-else>-</span>
          </NDescriptionsItem>
        </NDescriptions>
      </div>

      <div v-if="detail" class="mt-16px">
        <div class="mb-8px flex items-center gap-8px text-14px font-500">
          <span class="h-14px w-3px rounded-2px bg-primary"></span>
          发送记录
          <span class="text-12px color-#8f959e font-400">{{ mailLogs.length }} 条</span>
        </div>
        <NDataTable
          :columns="mailColumns"
          :data="mailLogs"
          :pagination="false"
          :bordered="false"
          size="small"
          :max-height="260"
        />
      </div>
    </NSpin>

    <template #footer>
      <NSpace justify="end">
        <NButton @click="visible = false">关闭</NButton>
      </NSpace>
    </template>
  </NModal>
</template>
