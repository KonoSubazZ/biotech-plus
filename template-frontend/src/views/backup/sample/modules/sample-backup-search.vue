<script setup lang="ts">
import { computed, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'SampleBackupSearch'
});

interface Props {
  /** 产品下拉（后端按已有数据 distinct 出来，前端不写死清单） */
  productOptions?: { label: string; value: string }[];
}

const props = withDefaults(defineProps<Props>(), { productOptions: () => [] });

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Backup.SampleSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 样本级状态（与后端聚合口径一致） */
const statusOptions = [
  { label: '待备份', value: 0 },
  { label: '全部成功', value: 2 },
  { label: '部分成功', value: 3 },
  { label: '全部失败', value: 4 }
];

/** 日期范围用 yyyy-MM-dd 字符串直接落进查询条件（不用时间戳，避免时区把边界日算错） */
const dateRange = computed({
  get: (): [string, string] | null =>
    model.value.beginDate && model.value.endDate ? [model.value.beginDate, model.value.endDate] : null,
  set: (value: [string, string] | null) => {
    model.value.beginDate = value?.[0] ?? null;
    model.value.endDate = value?.[1] ?? null;
  }
});

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
  <!-- 统一用公共组件 SearchCollapse，不要自己拼 NCard/NCollapse -->
  <SearchCollapse name="sample-backup-search">
    <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本编号" path="subbarcode">
          <NInput v-model:value="model.subbarcode" clearable placeholder="请输入样本编号" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="项目编号" path="projectNo">
          <NInput v-model:value="model.projectNo" clearable placeholder="请输入项目编号" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="订单批次" path="orderCode">
          <NInput v-model:value="model.orderCode" clearable placeholder="请输入订单批次编号" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="产品" path="productBio">
          <NSelect
            v-model:value="model.productBio"
            :options="props.productOptions"
            clearable
            filterable
            placeholder="请选择产品"
          />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="备份状态" path="backupStatus">
          <NSelect v-model:value="model.backupStatus" :options="statusOptions" clearable placeholder="请选择状态" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="创建日期" path="beginDate">
          <NDatePicker
            v-model:formatted-value="dateRange"
            class="w-full"
            clearable
            placeholder="请选择日期范围"
            type="daterange"
            value-format="yyyy-MM-dd"
          />
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
