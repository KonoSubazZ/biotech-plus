<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchGenerateQcNotify, fetchGetQcNotifyPrefill, fetchRefreshQcNotifySign, fetchSignQcNotify } from '@/service/api/qc/qc-notify';
import { useDownload } from '@/hooks/business/download';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'QcNotifyModal'
});

interface Props {
  /** 样本编号（列表页点「处理」时传入） */
  subbarcode: string;
  /** 质控类型（wet_lab 湿实验 / bioinfo 生信）：预填取对应质控表的记录，生成时落库区分来源 */
  qcCategory?: string;
}

const props = withDefaults(defineProps<Props>(), {
  qcCategory: 'bioinfo'
});

const visible = defineModel<boolean>('visible', { required: true });

const emit = defineEmits<{
  (e: 'generated'): void;
}>();

/** 下载用仓库既有的 GET 下载（zip 是「GET + 二进制」那个口子；download 是 POST，别用） */
const { zip } = useDownload();
const { formRef, validate } = useNaiveForm();

/** 文档里「常见不合格原因参考」的固定文案（只展示，不进 JSON） */
const REASON_REFERENCE =
  '常见不合格原因参考：样本量不足、运输超温、样本溶血/凝血、肿瘤含量低、DNA浓度低、RNA降解、文库浓度低、' +
  'Q30偏低、数据量不足、污染、生信分析未通过等；样本类型错误：送检样本类型与检测要求完全不符；' +
  '样本严重损坏或泄漏：容器破损、样本泄漏、污染严重；运输条件严重偏离。';

/** 样本质控环节：文档里是「一行内空格分隔」→ 页面横排 */
const STAGE_OPTIONS = [
  { label: '样本接收', value: 'stageReceive' },
  { label: '病理质控', value: 'stagePathology' },
  { label: '核酸质控', value: 'stageNucleic' },
  { label: '文库质控', value: 'stageLibrary' },
  { label: '测序质控', value: 'stageSequencing' }
] as const;

/** 建议处理办法：文档里是「一行一项」→ 页面竖排 */
const REMEDY_OPTIONS = [
  { label: '重新采集符合要求的样本重新寄送', value: 'remedyResample' },
  { label: '更换肿瘤含量较高的蜡块重新切片后寄送', value: 'remedyWaxBlock' },
  { label: '选取组织面积较大或较多的切片重新寄送', value: 'remedyMoreSlices' },
  { label: '重新送样', value: 'remedyResend' }
] as const;

const EMPTY_FORM: Api.Qc.NotifyForm = {
  subbarcode: null,
  qcCategory: null,
  studyNo: null,
  projectNo: null,
  participantNo: null,
  center: null,
  receiveDate: null,
  sampleType: null,
  sampleCount: null,
  product: null,
  stageReceive: false,
  stagePathology: false,
  stageNucleic: false,
  stageLibrary: false,
  stageSequencing: false,
  reason: null,
  terminateYes: false,
  terminatePause: false,
  remedyResample: false,
  remedyWaxBlock: false,
  remedyMoreSlices: false,
  remedyResend: false,
  remedyOther: false,
  remedyOtherText: null,
  reporter: null,
  reportTime: null,
  reviewer: null,
  reviewTime: null
};

const model = ref<Api.Qc.NotifyForm>(jsonClone(EMPTY_FORM));
const prefill = ref<Api.Qc.NotifyPrefill | null>(null);
const loading = ref(false);
const generating = ref(false);
/** 最近一次生成的记录 id（生成成功或预填里带回），用于下载 */
const downloadId = ref<number | null>(null);
/** 该记录有没有 PDF：有则「下载生成通知」优先给 PDF，没有（转换失败/历史记录）退回 DOCX */
const pdfAvailable = ref(false);
/** 送签后拿到的合同号（用于「刷新签署状态」的可用性判断） */
const signContractId = ref<string | null>(null);
/** 已拿到签名件时，用记录 id 走既有下载口子 */
const signedPdfId = ref<string | null>(null);
const signing = ref(false);
const refreshing = ref(false);

/** 顶部提示：已生成过就报单号/时间/状态 */
const generatedHint = computed(() => {
  if (!prefill.value?.generated) return '';
  const status = prefill.value.notifyStatus ? `，状态：${prefill.value.notifyStatus}` : '';
  const signHint = signContractId.value ? `，合同号：${signContractId.value}` : '';
  return `该样本已生成过通知：${prefill.value.notifyNo ?? ''}（${prefill.value.lastGeneratedAt ?? ''}）${status}${signHint}；再点生成会新增一条记录`;
});

/** 环节多选：五个布尔位 → 选中项数组（页面好用 NCheckboxGroup，横排） */
const stages = computed<string[]>({
  get: () => STAGE_OPTIONS.filter(item => Boolean(model.value[item.value])).map(item => item.value),
  set: values => {
    STAGE_OPTIONS.forEach(item => {
      model.value[item.value] = values.includes(item.value);
    });
  }
});

function pickForm(source: Api.Qc.NotifyPrefill): Api.Qc.NotifyForm {
  const form = jsonClone(EMPTY_FORM);
  Object.keys(form).forEach(key => {
    const value = (source as unknown as Record<string, unknown>)[key];
    if (value !== undefined && value !== null) {
      (form as unknown as Record<string, unknown>)[key] = value;
    }
  });
  return form;
}

async function load() {
  loading.value = true;
  try {
    const { data, error } = await fetchGetQcNotifyPrefill(props.subbarcode, props.qcCategory);
    if (error) return;
    prefill.value = data;
    model.value = pickForm(data);
    // 质控类型以页面传入的为准（预填里回带的可能来自上次另一种类型的记录）
    model.value.qcCategory = props.qcCategory;
    downloadId.value = data.lastNotifyId ?? null;
    pdfAvailable.value = Boolean(data.lastPdfName);
    // 签署相关状态：优先用本条记录带的（刷新页面后按钮仍可用），没有则清空，避免换样本时串用上一条
    signContractId.value = data.signContractId ?? null;
    signedPdfId.value = data.signedPdfPath ? String(data.lastNotifyId) : null;
  } finally {
    loading.value = false;
  }
}

watch(visible, value => {
  if (value) {
    load();
  }
});

/** 生成通知（DOCX + 静默转 PDF，状态记为「已生成（文档）」）*/
async function handleGenerate() {
  await validate();
  generating.value = true;
  try {
    const { data, error } = await fetchGenerateQcNotify(model.value);
    if (error) return;
    downloadId.value = data.notifyId;
    pdfAvailable.value = Boolean(data.pdfName);
    window.$message?.success(`已生成通知：${data.pdfName ?? data.docxName}`);
    emit('generated');
    load();
  } finally {
    generating.value = false;
  }
}

/** 下载已生成的通知（GET + 二进制；文件名走后端 Download-Filename 头）。优先给 PDF，没有 PDF 退回 DOCX */
function handleDownload() {
  if (!downloadId.value) return;
  const fileBase = prefill.value?.notifyNo ?? props.subbarcode;
  if (pdfAvailable.value) {
    zip(`/qc/notify/${downloadId.value}/pdf`, `${fileBase}.pdf`);
    return;
  }
  zip(`/qc/notify/${downloadId.value}/docx`, `${fileBase}.docx`);
}

/** 发送通知（邮件）：预设无功能，等邮件通道接上后再实现 */
function handleSend() {
  window.$message?.info('发送通知（邮件）功能待实现：接上邮件通道后这里改成发送并置状态「已发送（邮件）」');
}

/** 送签（上上签）：前置是已生成通知且有 PDF；成功带回合同号 */
async function handleSign() {
  if (!downloadId.value) return;
  signing.value = true;
  try {
    const { data, error } = await fetchSignQcNotify(String(downloadId.value));
    if (error) return;
    signContractId.value = data?.contractId ?? null;
    window.$message?.success(`已送签，合同号：${data?.contractId ?? ''}`);
  } finally {
    signing.value = false;
  }
}

/** 刷新签署状态：签完则把签名件拉到 notify/signed/ 并置「已签署」 */
async function handleRefreshSign() {
  if (!downloadId.value) return;
  refreshing.value = true;
  try {
    const { data, error } = await fetchRefreshQcNotifySign(String(downloadId.value));
    if (error) return;
    signContractId.value = data?.contractId ?? signContractId.value;
    if (data?.signed) {
      signedPdfId.value = String(downloadId.value);
      window.$message?.success(`已签署，签名件：${data?.signedPdfName ?? ''}`);
    } else {
      window.$message?.info(`对方还没签完（网关状态：${data?.contractStatus ?? '未知'}）`);
    }
  } finally {
    refreshing.value = false;
  }
}

/** 下载签名件（GET + 二进制，与既有「下载生成通知」同款口子） */
function handleDownloadSigned() {
  if (!signedPdfId.value) return;
  zip(`/qc/notify/${signedPdfId.value}/signed-pdf`, `通知签名件-${signedPdfId.value}.pdf`);
}
</script>

<template>
  <NModal v-model:show="visible" preset="card" title="样本质控不合格通知" class="max-w-90% w-840px" :mask-closable="false">
    <NSpin :show="loading">
      <NAlert v-if="generatedHint" type="warning" :bordered="false" class="mb-12px">{{ generatedHint }}</NAlert>

      <!-- 与通知单文档同序、同排布：表头一行两个 → 环节 → 不合格原因（下方参考文案）→ 是否终止检测 → 建议处理办法 → 反馈与复核 -->
      <NForm ref="formRef" :model="model" label-placement="left" :label-width="140" :show-feedback="false">
        <NGrid :cols="2" :x-gap="16">
          <NFormItemGi label="研究方案编号">
            <NInput v-model:value="model.studyNo" placeholder="客户合同/研究方案上的方案编号，如 NGS-2026-011" />
          </NFormItemGi>
          <NFormItemGi label="诺禾项目编号">
            <NInput v-model:value="model.projectNo" placeholder="诺禾侧项目/订单编号，如 P20260101" />
          </NFormItemGi>
          <NFormItemGi label="试验参与者编号">
            <NInput v-model:value="model.participantNo" placeholder="受试者/患者编号，如 SUBJ-1011" />
          </NFormItemGi>
          <NFormItemGi label="诺禾样本编号">
            <NInput :value="model.subbarcode ?? ''" placeholder="来自样本信息（不可修改）" disabled />
          </NFormItemGi>
          <NFormItemGi label="研究中心名称">
            <NInput v-model:value="model.center" placeholder="送检医院/研究中心名称" />
          </NFormItemGi>
          <NFormItemGi label="样本接收日期">
            <NInput v-model:value="model.receiveDate" placeholder="格式 yyyy-MM-dd，如 2026-06-10" />
          </NFormItemGi>
          <NFormItemGi label="样本类型">
            <NInput v-model:value="model.sampleType" placeholder="如：石蜡切片 / 全血 / 组织" />
          </NFormItemGi>
          <NFormItemGi label="样本数量">
            <NInput v-model:value="model.sampleCount" placeholder="如：1（单位见样本信息）" />
          </NFormItemGi>
          <NFormItemGi label="检测产品">
            <NInput v-model:value="model.product" placeholder="录单产品名，如 novopm2_tis_1238" />
          </NFormItemGi>
          <NFormItemGi label="分析时间">
            <NInput :value="prefill?.analysisTime ?? ''" placeholder="质控数据入库时间（不可修改）" disabled />
          </NFormItemGi>
        </NGrid>

        <NDivider class="my-8px" />

        <NFormItem class="notify-section" label="样本质控环节">
          <NCheckboxGroup v-model:value="stages">
            <NSpace :size="8">
              <NCheckbox v-for="item in STAGE_OPTIONS" :key="item.value" :value="item.value" :label="item.label" />
            </NSpace>
          </NCheckboxGroup>
        </NFormItem>
        <NFormItem class="notify-section" label="样本质控不合格原因">
          <NInput
            v-model:value="model.reason"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="已按质控标准自动带出该样本的不达标项（如 raw_base 1.30（要求 ≥2）），可直接修改或补充其他原因"
          />
        </NFormItem>
        <div class="mb-12px pl-140px text-12px text-gray-400">{{ REASON_REFERENCE }}</div>

        <NDivider class="my-8px" />

        <NFormItem class="notify-section" label="是否终止检测">
          <!-- 文档里是竖着两项 -->
          <NSpace vertical :size="4">
            <NCheckbox v-model:checked="model.terminateYes" label="是" />
            <NCheckbox v-model:checked="model.terminatePause" label="暂停相关检测，待申办方确认后处理" />
          </NSpace>
        </NFormItem>

        <NDivider class="my-8px" />

        <NFormItem class="notify-section" label="建议处理办法">
          <!-- 文档里是一行一项 -->
          <NSpace vertical :size="4">
            <NCheckbox
              v-for="item in REMEDY_OPTIONS"
              :key="item.value"
              v-model:checked="model[item.value]"
              :label="item.label"
            />
            <div class="flex items-center gap-8px">
              <NCheckbox v-model:checked="model.remedyOther" class="shrink-0 whitespace-nowrap" label="其他" />
              <NInput
                v-model:value="model.remedyOtherText"
                class="remedy-other"
                placeholder="请填写具体处理办法，如：联系客户后重新采集寄送"
              />
            </div>
          </NSpace>
        </NFormItem>

        <NDivider class="my-8px" />

        <NGrid :cols="2" :x-gap="16">
          <NFormItemGi label="反馈人">
            <NInput v-model:value="model.reporter" placeholder="填写反馈人姓名" />
          </NFormItemGi>
          <NFormItemGi label="反馈时间">
            <NInput v-model:value="model.reportTime" placeholder="格式 yyyy-MM-dd HH:mm" />
          </NFormItemGi>
          <NFormItemGi label="复核人">
            <NInput v-model:value="model.reviewer" placeholder="填写复核人姓名" />
          </NFormItemGi>
          <NFormItemGi label="复核时间">
            <NInput v-model:value="model.reviewTime" placeholder="格式 yyyy-MM-dd HH:mm" />
          </NFormItemGi>
        </NGrid>
      </NForm>

      <div class="text-12px text-gray-400">不合格原因默认带出质控标准算出的不达标项、可由生信修改；本页签发的状态为「已生成（文档）」。</div>
    </NSpin>

    <template #footer>
      <NSpace :size="16" justify="end">
        <NButton @click="visible = false">{{ $t('common.cancel') }}</NButton>
        <NButton type="primary" :loading="generating" @click="handleGenerate">生成通知</NButton>
        <NButton :disabled="!downloadId" @click="handleDownload">下载生成通知</NButton>
        <NButton :disabled="!downloadId" @click="handleSend">发送通知</NButton>
        <NButton :disabled="!downloadId" :loading="signing" @click="handleSign">送签</NButton>
        <NButton :disabled="!signContractId" :loading="refreshing" @click="handleRefreshSign">刷新签署状态</NButton>
        <NButton :disabled="!signedPdfId" @click="handleDownloadSigned">下载签名件</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped>
/* 标签一律单行：「样本质控不合格原因」9 个字在 flex 布局里会被折行，128px 也塞不下 */
:deep(.n-form-item-label) {
  white-space: nowrap;
}

/* 行距：show-feedback=false 之后每行贴在一起，补一点上下呼吸（用户反馈「太紧凑」） */
:deep(.n-form-item) {
  margin-bottom: 14px;
}

/* 「其他」输入框定宽：unoCSS 的 w-* 类会被 Naive 的 .n-input{width:100%} 顶掉，改用 scoped 规则 */
:deep(.n-input.remedy-other) {
  width: 360px;
}

/* 分组标签强调：加粗 + 左侧主题色小竖条（表头字段保持普通，形成层级；颜色走主题令牌不写死） */
:deep(.n-form-item.notify-section > .n-form-item-label) {
  font-weight: 600;
}

/* 竖条挂在标签文本上（Naive 的 label 是 grid，挂在 label 上会落到文字右边） */
:deep(.n-form-item.notify-section > .n-form-item-label > .n-form-item-label__text)::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 14px;
  margin-right: 8px;
  border-radius: 2px;
  background-color: rgb(var(--primary-color));
  vertical-align: -2px;
}
</style>
