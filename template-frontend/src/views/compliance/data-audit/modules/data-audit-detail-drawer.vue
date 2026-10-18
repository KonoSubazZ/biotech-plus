<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchGetDataAuditDetail } from '@/service/api/compliance/data-audit';

defineOptions({ name: 'DataAuditDetailDrawer' });
const props = defineProps<{ eventId: CommonType.IdType | null }>();
const visible = defineModel<boolean>('visible', { default: false });
const detail = ref<Api.Audit.Detail | null>(null);
const loading = ref(false);
const failed = ref(false);
let requestSequence = 0;

async function load() {
  const sequence = ++requestSequence;
  if (!visible.value || props.eventId === null) return;
  loading.value = true;
  failed.value = false;
  const { data, error } = await fetchGetDataAuditDetail(props.eventId);
  if (sequence !== requestSequence) return;
  loading.value = false;
  if (error || !data) {
    failed.value = true;
    return;
  }
  detail.value = data;
}

watch([visible, () => props.eventId], () => {
  detail.value = null;
  load();
});

function pretty(value: string | null) {
  if (value === null) return '不存在';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}
</script>

<template>
  <NDrawer v-model:show="visible" :width="960" class="max-w-90vw">
    <NDrawerContent title="审计详情" closable>
      <NSpin :show="loading">
        <NResult v-if="failed" status="error" title="审计详情加载失败" description="请重试，或确认查看权限。">
          <template #footer><NButton @click="load">重新加载</NButton></template>
        </NResult>
        <template v-else-if="detail">
          <NDescriptions :column="2" bordered size="small">
            <NDescriptionsItem label="操作">{{ detail.event.action }}</NDescriptionsItem>
            <NDescriptionsItem label="操作时间">{{ detail.event.occurredAt }}</NDescriptionsItem>
            <NDescriptionsItem label="操作人">{{ detail.event.actorName }}</NDescriptionsItem>
            <NDescriptionsItem label="样本编号">{{ detail.event.subbarcode || '不适用' }}</NDescriptionsItem>
            <NDescriptionsItem label="分析数据ID">
              {{ detail.event.analysisId || '样本公共事件／不适用' }}
            </NDescriptionsItem>
            <NDescriptionsItem label="报告ID">{{ detail.event.reportId || '不适用' }}</NDescriptionsItem>
          </NDescriptions>
          <NEmpty
            v-if="!detail.changes.length"
            class="py-24px"
            description="本次操作无业务字段变化，或仅涉及不保存原值的凭证操作"
          />
          <NCollapse class="mt-16px">
            <NCollapseItem v-for="change in detail.changes" :key="change.id" :name="String(change.id)">
              <template #header>
                <div class="min-w-0 break-all">
                  {{ change.tableName }} · {{ change.changeType }} · {{ change.objectKey }}
                </div>
              </template>
              <NDescriptions :column="1" bordered size="small">
                <NDescriptionsItem label="变化字段">
                  <pre class="m-0 whitespace-pre-wrap break-all text-13px">{{ pretty(change.changedFieldsJson) }}</pre>
                </NDescriptionsItem>
                <NDescriptionsItem label="修改前">
                  <pre class="m-0 whitespace-pre-wrap break-all text-13px">{{ pretty(change.beforeJson) }}</pre>
                </NDescriptionsItem>
                <NDescriptionsItem label="修改后">
                  <pre class="m-0 whitespace-pre-wrap break-all text-13px">{{ pretty(change.afterJson) }}</pre>
                </NDescriptionsItem>
              </NDescriptions>
            </NCollapseItem>
          </NCollapse>
        </template>
      </NSpin>
    </NDrawerContent>
  </NDrawer>
</template>
