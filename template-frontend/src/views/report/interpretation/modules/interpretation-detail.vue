<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { NTag } from 'naive-ui';
import { useRoute, useRouter } from 'vue-router';
import { fetchGetInterpretationContext } from '@/service/api/report/interpretation';
import { useAuth } from '@/hooks/business/auth';
import { INTERPRETATION_TABS } from './interpretation-tabs';
import { REPORT_STATUS_META, statusMeta } from './interpretation-status';
import InterpretationTabFiles from './tab-files.vue';
import InterpretationTabLims from './tab-lims.vue';
import InterpretationTabPreview from './tab-preview.vue';
import InterpretationTabReview from './tab-review.vue';
import InterpretationTabSend from './tab-send.vue';
import InterpretationTabVariants from './tab-variants.vue';

defineOptions({
  name: 'InterpretationDetail'
});

interface Props {
  /** 报告ID（来自列表页「解读」跳转的 query；**为空 = 该批次还没有报告记录**，
   *  只放行「集群对接」页签，用于驱动异常样本按文件重新驱动） */
  reportId?: number | null;
  /** 分析数据ID */
  analysisId: number;
  /** 直达页签（路由 query.tab；空或越权时落到第一个有权限的页签） */
  initialTab?: string;
  /** 只读查看（已完成报告的「查看解读」）：所有写操作按钮禁用，只允许浏览与下载 */
  readonly?: boolean;
}

const props = withDefaults(defineProps<Props>(), { initialTab: '', reportId: null, readonly: false });

interface Emits {
  (e: 'back'): void;
}

const emit = defineEmits<Emits>();

const router = useRouter();
const route = useRoute();
const { hasAuth } = useAuth();

const loading = ref(false);
const context = ref<Api.Report.InterpretationContext | null>(null);

/** 没有报告记录的模式（驱动异常样本：还没进入解读，只有文件可以看/重驱） */
const noReport = computed(() => props.reportId === null || props.reportId === undefined);

/** 只渲染有权限的页签：一个页签都没有时整个页签栏不出现（只给提示）；
 *  没有报告记录时只有「集群对接」有意义（其它页签都依赖报告行） */
const visibleTabs = computed(() =>
  INTERPRETATION_TABS.filter(tab => hasAuth(tab.perm) && (!noReport.value || tab.name === 'files'))
);

function tabVisible(name: string) {
  return visibleTabs.value.some(tab => tab.name === name);
}

/** 当前页签（空 = 没有可用页签） */
const activeTab = ref('');

/** 页签收敛：URL 指定的页签有权限就用它，否则落到第一个有权限的页签 */
function syncActiveTab() {
  activeTab.value = tabVisible(props.initialTab) ? props.initialTab : (visibleTabs.value[0]?.name ?? '');
}

watch(visibleTabs, syncActiveTab, { immediate: true });
watch(() => props.initialTab, syncActiveTab);

/** 切页签：写回 URL，刷新 / 复制链接都能停在同一页签 */
function handleTabChange(name: string) {
  activeTab.value = name;
  if (route.query.tab !== name) {
    router.replace({ query: { ...route.query, tab: name } });
  }
}

const reportStatus = computed(() =>
  statusMeta(REPORT_STATUS_META, context.value?.report.reportStatus ?? null, '未解读')
);

async function loadContext() {
  // 没有报告记录时不查上下文（上下文接口要 reportId），页面显示「未建报告」提示
  if (noReport.value || !props.analysisId) {
    context.value = null;
    return;
  }
  loading.value = true;
  try {
    const { data, error } = await fetchGetInterpretationContext({
      // 走到这里说明有报告记录（noReport 在前面已 return）
      reportId: props.reportId as number,
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
            <span class="text-16px font-medium">
              {{ noReport ? '集群对接（尚未进入解读）' : `报告解读 #${props.reportId}` }}
            </span>
            <NTag v-if="noReport" type="warning">未建报告</NTag>
            <NTag v-else :type="reportStatus.type">{{ reportStatus.label }}</NTag>
            <NTag v-if="props.readonly" type="default">只读查看</NTag>
            <NTag v-if="context?.report.reportVersion" type="info">V{{ context.report.reportVersion }}</NTag>
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
          <NDescriptionsItem label="产品">{{ context.report.productBio ?? '/' }}</NDescriptionsItem>
          <NDescriptionsItem label="解读癌种">{{ context.report.analysisDisease ?? '/' }}</NDescriptionsItem>
        </NDescriptions>
      </NSpin>
    </NCard>

    <NAlert
      v-if="noReport"
      type="warning"
      :bordered="false"
      title="该批次还没有报告记录"
    >
      驱动成功的样本在「样本流转 › 生信看点」点处理时会自动创建报告记录；此处可先按文件重新驱动
      （每个文件行右侧「重新驱动」，全部文件都变为「已加载」后样本驱动状态即为成功）。
    </NAlert>
    <NAlert
      v-else-if="context && context.errors.length"
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
      <NAlert v-if="!visibleTabs.length" type="warning" :bordered="false" title="无页签权限">
        当前账号没有该报告任一页签的权限（LIMS 信息 / 集群对接 / 筛选位点 / 报告预览 / 审核报告 / 报告发送），
        请联系管理员分配。
      </NAlert>

      <NTabs v-else :value="activeTab" type="line" animated @update:value="handleTabChange">
        <NTabPane
          v-for="tab in visibleTabs"
          :key="tab.name"
          :name="tab.name"
          :tab="tab.label"
          :class="{ 'readonly-pane': props.readonly }"
        >
          <template v-if="tab.name === 'lims'">
            <InterpretationTabLims v-if="context" :lims="context.lims" />
          </template>
          <template v-else-if="tab.name === 'files'">
            <InterpretationTabFiles :analysis-id="props.analysisId" @changed="loadContext" />
          </template>
          <template v-else-if="tab.name === 'variants'">
            <InterpretationTabVariants
              :analysis-id="props.analysisId"
              :report-id="props.reportId ?? 0"
              :report-status="context?.report.reportStatus ?? null"
              @changed="loadContext"
            />
          </template>
          <template v-else-if="tab.name === 'preview'">
            <InterpretationTabPreview
              :analysis-id="props.analysisId"
              :report-id="props.reportId ?? 0"
              :template-id="context?.report.templateId"
              :report-version="context?.report.reportVersion"
              :report-status="context?.report.reportStatus ?? null"
              @changed="loadContext"
            />
          </template>
          <template v-else-if="tab.name === 'review'">
            <InterpretationTabReview
              :analysis-id="props.analysisId"
              :report-id="props.reportId ?? 0"
              @changed="loadContext"
            />
          </template>
          <template v-else-if="tab.name === 'send'">
            <InterpretationTabSend
              :analysis-id="props.analysisId"
              :report-id="props.reportId ?? 0"
              :report-status="context?.report.reportStatus ?? null"
              @changed="loadContext"
            />
          </template>
        </NTabPane>
      </NTabs>
    </NCard>
  </div>
</template>

<style scoped>
/* 只读查看：所有写操作控件不可点（后端状态机在 62 状态也已拒绝生成/提交/审核/发送） */
.readonly-pane :deep(button),
.readonly-pane :deep(input),
.readonly-pane :deep(textarea),
.readonly-pane :deep(.n-select),
.readonly-pane :deep(.n-switch),
.readonly-pane :deep(.n-checkbox) {
  pointer-events: none;
  opacity: 0.6;
}
</style>
