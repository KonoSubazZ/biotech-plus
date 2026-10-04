<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { NTag } from 'naive-ui';
import { fetchGetInterpretationContext } from '@/service/api/report/interpretation';
import { REPORT_STATUS_META, statusMeta } from './interpretation-status';
import InterpretationTabFiles from './tab-files.vue';
import InterpretationTabLims from './tab-lims.vue';
import InterpretationTabPreview from './tab-preview.vue';
import InterpretationTabVariants from './tab-variants.vue';

defineOptions({
  name: 'InterpretationDetail'
});

interface Props {
  /** 报告ID（来自列表页「解读」跳转的 query） */
  reportId: number;
  /** 分析数据ID */
  analysisId: number;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'back'): void;
}

const emit = defineEmits<Emits>();

const loading = ref(false);
const context = ref<Api.Report.InterpretationContext | null>(null);

/** 详情页 Tab：① LIMS 信息已实现，其余 5 个由第 3~7 页逐个补齐 */
const TABS = [
  { name: 'lims', label: 'LIMS 信息' },
  { name: 'files', label: '集群对接' },
  { name: 'variants', label: '筛选位点' },
  { name: 'preview', label: '报告预览' },
  { name: 'review', label: '审核报告' },
  { name: 'send', label: '报告发送' }
];

const activeTab = ref('lims');

const reportStatus = computed(() => statusMeta(REPORT_STATUS_META, context.value?.report.status ?? null, '未解读'));

async function loadContext() {
  if (!props.reportId || !props.analysisId) {
    return;
  }
  loading.value = true;
  try {
    const { data, error } = await fetchGetInterpretationContext({
      reportId: props.reportId,
      analysisId: props.analysisId
    });
    if (!error) {
      context.value = data;
    }
  } finally {
    loading.value = false;
  }
}

watch(() => [props.reportId, props.analysisId], loadContext, { immediate: true });
</script>

<template>
  <div class="flex-col-stretch gap-16px">
    <NCard :bordered="false" size="small" class="card-wrapper">
      <NSpin :show="loading">
        <NSpace align="center" justify="space-between">
          <NSpace align="center" :size="12">
            <span class="text-16px font-medium">报告解读 #{{ props.reportId }}</span>
            <NTag :type="reportStatus.type">{{ reportStatus.label }}</NTag>
            <span class="text-13px op-60">分析批次 {{ props.analysisId }}</span>
          </NSpace>
          <NButton @click="emit('back')">
            <template #icon>
              <icon-ic-round-arrow-back class="text-icon" />
            </template>
            返回列表
          </NButton>
        </NSpace>

        <NDivider class="my-12px!" />

        <NDescriptions v-if="context" :column="4" label-placement="left" size="small">
          <NDescriptionsItem label="样本编号">{{ context.report.subbarcode ?? '/' }}</NDescriptionsItem>
          <NDescriptionsItem label="患者姓名">{{ context.lims.patientName ?? '/' }}</NDescriptionsItem>
          <NDescriptionsItem label="产品">{{ context.report.product ?? '/' }}</NDescriptionsItem>
          <NDescriptionsItem label="解读癌种">{{ context.report.analysisDisease ?? '/' }}</NDescriptionsItem>
        </NDescriptions>
      </NSpin>
    </NCard>

    <NAlert
      v-if="context && context.errors.length"
      type="error"
      :bordered="false"
      title="数据不完整，暂不能生成报告"
    >
      <ul class="m-0 pl-18px">
        <li v-for="item in context.errors" :key="item">{{ item }}</li>
      </ul>
    </NAlert>
    <NAlert v-else-if="context && context.warnings.length" type="warning" :bordered="false" title="非关键字段缺失">
      {{ context.warnings.join('；') }}
    </NAlert>

    <NCard :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <NTabs v-model:value="activeTab" type="line" animated>
        <NTabPane v-for="tab in TABS" :key="tab.name" :name="tab.name" :tab="tab.label">
          <template v-if="tab.name === 'lims'">
            <InterpretationTabLims v-if="context" :lims="context.lims" />
          </template>
          <template v-else-if="tab.name === 'files'">
            <InterpretationTabFiles :analysis-id="props.analysisId" />
          </template>
          <template v-else-if="tab.name === 'variants'">
            <InterpretationTabVariants :analysis-id="props.analysisId" />
          </template>
          <template v-else-if="tab.name === 'preview'">
            <InterpretationTabPreview :analysis-id="props.analysisId" :report-id="props.reportId" />
          </template>
          <template v-else>
            <NEmpty :description="`${tab.label}：建设中（后续页面按 Tab 逐个交付）`" />
          </template>
        </NTabPane>
      </NTabs>
    </NCard>
  </div>
</template>

<style scoped></style>
