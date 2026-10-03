<script setup lang="ts">
import { ref, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'QcRecordSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Qc.QcRecordSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/**
 * 人工状态（单选用 NSelect，UX-CONTRACT.md）
 * <p>
 * 质控类别由所在页面固定（湿实验 / 生信），搜索区不提供切换，避免两个页面查出同一批数据。
 */
const statusOptions = [
  { label: '待确认', value: 'pending' },
  { label: '通过', value: 'passed' },
  { label: '未通过', value: 'failed' }
];

function resetModel() {
  Object.assign(model.value, defaultModel);
}

async function reset() {
  await restoreValidation();
  resetModel();
  emit('search');
}

async function search() {
  await validate();
  emit('search');
}
</script>

<template>
  <NCard :bordered="false" size="small" class="card-wrapper">
    <NCollapse>
      <NCollapseItem :title="$t('common.search')" name="qc-record-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="80">
          <NGrid responsive="screen" item-responsive>
            <NFormItemGi span="24 s:12 m:8" label="样本条码" path="subbarcode" class="pr-24px">
              <NInput v-model:value="model.subbarcode" placeholder="请输入样本条码" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="质控项目" path="qcItem" class="pr-24px">
              <NInput v-model:value="model.qcItem" placeholder="请输入质控项目名称" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="人工状态" path="status" class="pr-24px">
              <NSelect v-model:value="model.status" :options="statusOptions" clearable placeholder="请选择人工状态" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" class="pr-24px">
              <NSpace class="w-full" justify="end">
                <NButton @click="reset">
                  <template #icon>
                    <icon-ic-round-refresh class="text-icon" />
                  </template>
                  {{ $t('common.reset') }}
                </NButton>
                <NButton type="primary" ghost @click="search">
                  <template #icon>
                    <icon-ic-round-search class="text-icon" />
                  </template>
                  {{ $t('common.search') }}
                </NButton>
              </NSpace>
            </NFormItemGi>
          </NGrid>
        </NForm>
      </NCollapseItem>
    </NCollapse>
  </NCard>
</template>

<style scoped></style>
