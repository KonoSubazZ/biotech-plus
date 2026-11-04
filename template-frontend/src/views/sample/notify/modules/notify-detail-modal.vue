<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchGetNotifyDetail } from '@/service/api/qc/qc-notify';

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

async function load() {
  if (!props.notifyId) return;
  loading.value = true;
  try {
    const { data, error } = await fetchGetNotifyDetail(props.notifyId);
    if (!error) detail.value = data;
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
  <NModal v-model:show="visible" preset="card" title="通知详情" class="max-w-90% w-860px">
    <NSpin :show="loading">
      <NDescriptions v-if="detail" :column="2" bordered size="small" label-placement="left" :label-style="{ width: '130px' }">
        <NDescriptionsItem label="通知类型">{{ TYPE_LABEL[detail.notifyType] ?? detail.notifyType }}</NDescriptionsItem>
        <NDescriptionsItem label="状态">{{ STATUS_LABEL[detail.notifyStatus] ?? detail.notifyStatus }}</NDescriptionsItem>
        <NDescriptionsItem label="签署状态">{{ SIGN_LABEL[detail.signStatus] ?? detail.signStatus }}</NDescriptionsItem>
        <NDescriptionsItem label="通知单号">{{ detail.notifyNo ?? '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="通知日期">{{ detail.notifyDate }}</NDescriptionsItem>
        <NDescriptionsItem label="样本编号">{{ detail.subbarcode }}</NDescriptionsItem>
        <NDescriptionsItem label="样本名称">{{ detail.sampleName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="试验参与者编号">{{ detail.participantNo || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="项目编号">{{ detail.projectNo || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="研究方案编号">{{ detail.studyNo || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="客户">{{ detail.customerName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="检测产品">{{ detail.product || '-' }}</NDescriptionsItem>

        <template v-if="detail.notifyType === 'delay'">
          <NDescriptionsItem label="延迟原因" :span="2">{{ detail.delayReason || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="预计出具时间" :span="2">{{ detail.expectedReportDate || '-' }}</NDescriptionsItem>
        </template>
        <template v-else>
          <NDescriptionsItem label="不合格环节" :span="2">{{ detail.qcStage || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="不合格原因" :span="2">{{ detail.reason || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="是否终止检测" :span="2">{{ detail.terminateMeasure || '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="建议处理办法" :span="2">{{ detail.remedy || '-' }}</NDescriptionsItem>
        </template>

        <NDescriptionsItem label="反馈人">{{ detail.reporter || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="反馈时间">{{ detail.reportTime || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="复核人">{{ detail.reviewer || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="复核时间">{{ detail.reviewTime || '-' }}</NDescriptionsItem>

        <NDescriptionsItem label="处理人">{{ detail.handlerName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="处理时间">{{ detail.handlerTime || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="发送人">{{ detail.senderName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="发送时间">{{ detail.senderTime || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="实际收件人" :span="2">{{ detail.sendTo || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="实际抄送" :span="2">{{ detail.sendCc || '-' }}</NDescriptionsItem>
        <NDescriptionsItem v-if="detail.sendError" label="失败原因" :span="2">{{ detail.sendError }}</NDescriptionsItem>

        <NDescriptionsItem label="上上签合同号" :span="2">{{ detail.signContractId || '-' }}</NDescriptionsItem>

        <NDescriptionsItem label="通知单 PDF" :span="2">
          <PdfPreview
            v-if="detail.pdfPath"
            :src="`/qc/notify/${detail.id}/pdf`"
            :name="fileName(detail.pdfPath)"
          />
          <span v-else>-</span>
        </NDescriptionsItem>
        <NDescriptionsItem label="签名件 PDF" :span="2">
          <PdfPreview
            v-if="detail.signedPdfPath"
            :src="`/qc/notify/${detail.id}/signed-pdf`"
            :name="fileName(detail.signedPdfPath)"
          />
          <span v-else>-</span>
        </NDescriptionsItem>
        <NDescriptionsItem label="JSON / DOCX" :span="2">
          {{ fileName(detail.jsonPath) || '-' }} / {{ fileName(detail.rawDocxPath) || '-' }}
        </NDescriptionsItem>
      </NDescriptions>
    </NSpin>

    <template #footer>
      <NSpace justify="end">
        <NButton @click="visible = false">关闭</NButton>
      </NSpace>
    </template>
  </NModal>
</template>
