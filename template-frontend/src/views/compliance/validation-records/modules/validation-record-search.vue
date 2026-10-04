<script setup lang="ts">
import { toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ValidationRecordSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Compliance.ValidationRecordSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 验证类型（单选用 NSelect，UX-CONTRACT.md） */
const typeOptions = [
  { label: 'IQ 安装确认', value: 'IQ' },
  { label: 'OQ 运行确认', value: 'OQ' },
  { label: 'PQ 性能确认', value: 'PQ' }
];

/** 结果 */
const resultOptions = [
  { label: '通过', value: 'passed' },
  { label: '未通过', value: 'failed' },
  { label: '不适用', value: 'na' }
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
      <NCollapseItem :title="$t('common.search')" name="validation-record-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <!--
            搜索栏布局契约（仓库 crud skill §「搜索栏布局契约」）：
            搜索项用 flex-wrap + 每项固定百分比宽；按钮组 ml-auto 自动贴当前行最右；
            一行放不下整组换行后仍贴右。不要用 NGrid + 最后一格放按钮（只会在自己格子内靠右，
            字段数不是列数整数倍时会悬在行中间）。
          -->
          <div class="flex flex-wrap items-start">
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="验证类型" path="validationType">
              <NSelect
                v-model:value="model.validationType"
                :options="typeOptions"
                clearable
                placeholder="请选择验证类型"
              />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="验证标题" path="title">
              <NInput v-model:value="model.title" placeholder="请输入验证标题" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="执行人" path="executedBy">
              <NInput v-model:value="model.executedBy" placeholder="请输入执行人" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="结果" path="result">
              <NSelect v-model:value="model.result" :options="resultOptions" clearable placeholder="请选择结果" />
            </NFormItem>
            <NFormItem class="ml-auto" :show-feedback="false">
              <NSpace :size="16">
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
            </NFormItem>
          </div>
        </NForm>
      </NCollapseItem>
    </NCollapse>
  </NCard>
</template>

<style scoped></style>
