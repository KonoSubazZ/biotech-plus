<script setup lang="ts">
import { computed, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'NotifySearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Qc.NotifyQuery>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

const typeOptions = [
  { label: '延期通知', value: 'delay' },
  { label: '样本不合格', value: 'sample' },
  { label: '湿实验不合格', value: 'wet_lab' },
  { label: '干实验不合格', value: 'bioinfo' }
];

const statusOptions = [
  { label: '待处理', value: 'pending' },
  { label: '发送成功', value: 'sent' },
  { label: '发送失败', value: 'failed' }
];

/** 日期区间：后端按 yyyyMMdd 字符串闭区间比较 */
const dateRange = computed<[string, string] | null>({
  get: () =>
    model.value.notifyDateStart && model.value.notifyDateEnd
      ? ([model.value.notifyDateStart, model.value.notifyDateEnd] as [string, string])
      : null,
  set: (value: [string, string] | null) => {
    model.value.notifyDateStart = value?.[0] ?? null;
    model.value.notifyDateEnd = value?.[1] ?? null;
  }
});

function resetModel() {
  Object.assign(model.value, jsonClone(defaultModel));
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
  <SearchCollapse name="sample-notify-search">
    <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
      <!-- 搜索项排布用 flex-wrap（不用 NGrid）：按钮组 ml-auto 贴当前行最右，换行后仍贴右 -->
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="通知类型" path="notifyType">
          <NSelect v-model:value="model.notifyType" :options="typeOptions" placeholder="全部" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="状态" path="notifyStatus">
          <NSelect v-model:value="model.notifyStatus" :options="statusOptions" placeholder="全部" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="通知日期" path="notifyDateStart">
          <NDatePicker
            v-model:formatted-value="dateRange"
            type="daterange"
            value-format="yyyyMMdd"
            clearable
            class="w-full"
          />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本编号" path="subbarcode">
          <NInput v-model:value="model.subbarcode" placeholder="请输入样本编号" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="项目编号" path="projectNo">
          <NInput v-model:value="model.projectNo" placeholder="请输入项目编号" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="关键字" path="keyword">
          <NInput v-model:value="model.keyword" placeholder="样本编号 / 样本名称 / 参与者编号" clearable />
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
  </SearchCollapse>
</template>

<style scoped></style>
