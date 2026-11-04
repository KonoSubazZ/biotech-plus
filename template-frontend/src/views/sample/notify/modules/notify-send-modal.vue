<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { fetchGetNotifyMailPreview, fetchSendNotify } from '@/service/api/qc/qc-notify';

defineOptions({
  name: 'NotifySendModal'
});

interface Props {
  /** 通知记录 id */
  notifyId: number | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'sent'): void;
}>();

const visible = defineModel<boolean>('visible', { required: true });

const loading = ref(false);
const sending = ref(false);
const preview = ref<Api.Qc.NotifyMailPreview | null>(null);

/** 发件邮箱 / 收件人 / 抄送 / 标题 / 正文（页面可改） */
const senderId = ref<number | null>(null);
const to = ref<string[]>([]);
const cc = ref<string[]>([]);
const subject = ref('');
const content = ref('');

const toOptions = computed(() => (preview.value?.toCandidates ?? []).map(address => ({ label: address, value: address })));
const ccOptions = computed(() => (preview.value?.ccCandidates ?? []).map(address => ({ label: address, value: address })));

const senderOptions = computed(() =>
  (preview.value?.senderOptions ?? []).map(option => ({
    label: `${option.fromName ?? ''} <${option.fromEmail ?? ''}>${option.isDefault === '1' ? '（默认）' : ''}`,
    value: option.configId,
    disabled: option.enabled !== '1'
  }))
);

const selectedSender = computed(
  () => (preview.value?.senderOptions ?? []).find(option => option.configId === senderId.value) ?? null
);

/** 切换发件邮箱：标题/正文沿用同一套（通知文案不随邮箱变） */
function applySenderTemplate(id: number | null) {
  const option = (preview.value?.senderOptions ?? []).find(item => item.configId === id);
  if (!option) return;
  subject.value = option.subject ?? subject.value;
  content.value = option.content ?? content.value;
}

async function load() {
  if (!props.notifyId) return;
  loading.value = true;
  try {
    const { data, error } = await fetchGetNotifyMailPreview(props.notifyId);
    if (error || !data) return;
    preview.value = data;
    senderId.value = data.defaultConfigId ?? null;
    to.value = [...(data.toCandidates ?? [])];
    cc.value = [...(data.ccCandidates ?? [])];
    subject.value = data.subject ?? '';
    content.value = data.content ?? '';
  } finally {
    loading.value = false;
  }
}

watch(visible, value => {
  if (value) load();
});

const canSubmit = computed(
  () => preview.value?.canSend === true && selectedSender.value?.enabled === '1' && to.value.length > 0
);

async function handleSend() {
  if (!props.notifyId) return;
  if (!to.value.length) {
    window.$message?.error('请填写收件人');
    return;
  }
  sending.value = true;
  try {
    const { error } = await fetchSendNotify(props.notifyId, {
      configId: senderId.value ?? undefined,
      to: to.value.join(','),
      cc: cc.value.length ? cc.value.join(',') : null,
      subject: subject.value,
      content: content.value
    });
    if (error) return;
    window.$message?.success('已发送');
    emit('sent');
    visible.value = false;
  } finally {
    sending.value = false;
  }
}
</script>

<template>
  <NModal
    v-model:show="visible"
    preset="card"
    title="发送通知邮件"
    class="max-w-90% w-760px"
    :mask-closable="false"
  >
    <NSpin :show="loading">
      <NAlert v-if="preview?.blockedReason" type="error" :bordered="false" class="mb-12px">
        {{ preview.blockedReason }}
      </NAlert>

      <NForm label-placement="left" :label-width="90" :show-feedback="false">
        <NFormItem label="发件邮箱">
          <NSelect
            v-model:value="senderId"
            :options="senderOptions"
            placeholder="请选择发件邮箱"
            @update:value="applySenderTemplate"
          />
        </NFormItem>
        <NFormItem label="收件人">
          <NSelect
            v-model:value="to"
            :options="toOptions"
            multiple
            filterable
            tag
            placeholder="样本报告接收邮箱 + 研究中心收件邮箱"
          />
        </NFormItem>
        <NFormItem label="抄送">
          <NSelect
            v-model:value="cc"
            :options="ccOptions"
            multiple
            filterable
            tag
            placeholder="项目配置的抄送邮箱（可空）"
          />
        </NFormItem>
        <NFormItem label="标题">
          <NInput v-model:value="subject" placeholder="邮件标题" />
        </NFormItem>
        <NFormItem label="正文">
          <NInput v-model:value="content" type="textarea" :autosize="{ minRows: 8, maxRows: 16 }" />
        </NFormItem>
        <NFormItem label="附件">
          <NInput :value="preview?.attachmentName ?? ''" disabled placeholder="签名后的通知单 PDF" />
        </NFormItem>
      </NForm>

      <div class="text-12px text-gray-400">附件取签名件；发送成功置「发送成功」，失败置「发送失败」并记录原因，可重发。</div>
    </NSpin>

    <template #footer>
      <NSpace :size="16" justify="end">
        <NButton @click="visible = false">{{ $t('common.cancel') }}</NButton>
        <NButton type="primary" :disabled="!canSubmit" :loading="sending" @click="handleSend">发送</NButton>
      </NSpace>
    </template>
  </NModal>
</template>
