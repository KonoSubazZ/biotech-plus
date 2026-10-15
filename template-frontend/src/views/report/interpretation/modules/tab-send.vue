<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { NAlert, NButton, NCheckbox, NForm, NFormItem, NInput, NSelect, NSpace, NSpin, NTag } from 'naive-ui';
import { fetchGetMailPreview, fetchSendReport } from '@/service/api/report/interpretation';

defineOptions({
  name: 'InterpretationTabSend'
});

interface Props {
  analysisId: number;
  reportId: number;
  /** 报告当前状态（详情页 context 给的；SENT = 已发送，表单锁定） */
  reportStatus?: string | null;
}

const props = defineProps<Props>();

interface Emits {
  /** 发送成功后通知详情页刷新报告头（状态变「已发送」） */
  (e: 'changed'): void;
}

const emit = defineEmits<Emits>();

/** 已发送：表单锁定，不再允许改内容或重发 */
const sent = computed(() => props.reportStatus === 'SENT');

const loading = ref(false);
const sending = ref(false);
const preview = ref<Api.Report.InterpretationMailPreview | null>(null);

/** 发件邮箱：来自「邮件配置」列表，默认选中「默认邮箱」；每条带自己那套标题/正文 */
const senderId = ref<number | null>(null);

/** 收件人 / 抄送：多选 + 可手输（候选来自 LIMS 邮箱，允许新增与删除） */
const to = ref<string[]>([]);
const cc = ref<string[]>([]);
const subject = ref('');
const content = ref('');
const attachReport = ref(true);

/** 候选邮箱 → NSelect 选项（label 与 value 都是邮箱本身） */
const toOptions = computed(() => (preview.value?.toCandidates ?? []).map(address => ({ label: address, value: address })));
const ccOptions = computed(() => (preview.value?.ccCandidates ?? []).map(address => ({ label: address, value: address })));

/** 发件邮箱下拉项；enabled=0 的置灰不可选（业务判定在后端，这里只按它给的值置灰） */
const senderOptions = computed(() =>
  (preview.value?.senderOptions ?? []).map(option => ({
    label: `${option.fromName ?? ''} <${option.fromEmail ?? ''}>${option.isDefault === '1' ? '（默认）' : ''}`,
    value: option.configId,
    disabled: option.enabled !== '1'
  }))
);

/** 当前选中的发件邮箱（用来判断能不能发） */
const selectedSender = computed(() =>
  (preview.value?.senderOptions ?? []).find(option => option.configId === senderId.value) ?? null
);

/** 切换发件邮箱：直接换成这条邮箱自己那套标题/正文（后端已按本报告变量渲染好） */
function applySenderTemplates(id: number | null) {
  const option = (preview.value?.senderOptions ?? []).find(item => item.configId === id);
  if (!option) {
    return;
  }
  subject.value = option.subject ?? '';
  content.value = option.content ?? '';
}

async function load() {
  if (!props.reportId || !props.analysisId) {
    return;
  }
  loading.value = true;
  try {
    const { data, error } = await fetchGetMailPreview({ reportId: props.reportId, analysisId: props.analysisId });
    if (!error && data) {
      preview.value = data;
      senderId.value = data.defaultConfigId ?? null;
      to.value = [...(data.toCandidates ?? [])];
      cc.value = [...(data.ccCandidates ?? [])];
      subject.value = data.subject ?? '';
      content.value = data.content ?? '';
    }
  } finally {
    loading.value = false;
  }
}

/** 可发送 = 后端判定可发 + 选中的发件邮箱是启用状态 + 还没发过（页签权限已保证有发送权） */
const canSubmit = computed(
  () =>
    preview.value?.canSend === true &&
    selectedSender.value?.enabled === '1' &&
    !sent.value
);

async function handleSend() {
  if (!to.value.length) {
    window.$message?.error('请填写收件人');
    return;
  }
  sending.value = true;
  try {
    const { error } = await fetchSendReport({
      analysisId: props.analysisId,
      reportId: props.reportId,
      configId: senderId.value ?? undefined,
      to: to.value.join(','),
      cc: cc.value.join(','),
      subject: subject.value,
      content: content.value,
      attachReport: attachReport.value
    });
    if (!error) {
      window.$message?.success('报告已发送');
      emit('changed');
    }
  } finally {
    sending.value = false;
  }
}

watch(() => [props.reportId, props.analysisId], load, { immediate: true });
</script>

<template>
  <NSpin :show="loading">
    <div class="flex-col-stretch gap-12px">
      <NAlert v-if="sent" type="success" :bordered="false">该报告已发送，表单已锁定。</NAlert>
      <NAlert
        v-else-if="preview && preview.canSend === false"
        type="warning"
        :bordered="false"
        title="暂时不能发送"
      >
        {{ preview.blockedReason ?? '请先完善前置条件' }}
      </NAlert>

      <NForm label-placement="left" :label-width="80" :disabled="sent">
        <NFormItem label="发件邮箱">
          <NSelect
            v-model:value="senderId"
            :options="senderOptions"
            :disabled="sent"
            placeholder="从「邮件配置」里选择发件邮箱"
            @update:value="(value: number) => applySenderTemplates(value)"
          />
        </NFormItem>

        <NFormItem label="收件人">
          <NSelect
            v-model:value="to"
            :options="toOptions"
            multiple
            filterable
            tag
            clearable
            placeholder="从 LIMS 邮箱候选中选择，或直接输入邮箱后回车"
          />
        </NFormItem>

        <NFormItem label="抄送">
          <NSelect
            v-model:value="cc"
            :options="ccOptions"
            multiple
            filterable
            tag
            clearable
            placeholder="可留空；输入邮箱后回车可新增"
          />
        </NFormItem>

        <NFormItem label="标题">
          <NInput v-model:value="subject" placeholder="邮件标题" />
        </NFormItem>

        <NFormItem label="内容">
          <NInput
            v-model:value="content"
            type="textarea"
            :autosize="{ minRows: 6, maxRows: 14 }"
            placeholder="邮件正文，支持 HTML 片段"
          />
        </NFormItem>

        <NFormItem label="附件">
          <NSpace align="center" :size="12">
            <NTag v-if="preview?.attachmentName" type="info">{{ preview.attachmentName }}</NTag>
            <span v-else class="op-60">报告文件尚未生成</span>
            <NCheckbox v-model:checked="attachReport" :disabled="!preview?.attachmentName">
              附带报告文件
            </NCheckbox>
          </NSpace>
        </NFormItem>
      </NForm>

      <NSpace>
        <NButton type="primary" :loading="sending" :disabled="!canSubmit" @click="handleSend">发送报告</NButton>
        <NButton :disabled="sending" @click="load">刷新</NButton>
      </NSpace>
    </div>
  </NSpin>
</template>

<style scoped></style>
