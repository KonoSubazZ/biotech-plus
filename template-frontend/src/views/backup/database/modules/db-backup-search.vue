<script setup lang="ts">
import { computed, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';

defineOptions({
  name: 'DbBackupSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Backup.DbBackupSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 备份范围就在配置里（backup.yml 的 mysql.databases），这里给常用两项，可搜索 */
const dbOptions = [
  { label: 'ry-vue', value: 'ry-vue' },
  { label: 'nkb', value: 'nkb' }
];

/** NDatePicker 的 daterange 用 [start, end] 毫秒时间戳 */
const range = computed({
  get() {
    if (!model.value.beginDate || !model.value.endDate) return null;
    return [new Date(model.value.beginDate).getTime(), new Date(model.value.endDate).getTime()] as [number, number];
  },
  set(value: [number, number] | null) {
    model.value.beginDate = value ? fmt(value[0]) : null;
    model.value.endDate = value ? fmt(value[1]) : null;
  }
});

function fmt(ms: number) {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

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
    <NForm ref="formRef" :model="model" label-placement="left" :label-width="80">
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/4" label="库名" path="dbName">
          <NSelect
            v-model:value="model.dbName"
            :options="dbOptions"
            clearable
            filterable
            placeholder="全部库"
          />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="日期范围" path="beginDate">
          <NDatePicker v-model:value="range" type="daterange" clearable class="w-full" />
        </NFormItem>
        <div class="ml-auto flex gap-8px">
          <NButton @click="reset">
            <template #icon>
              <SvgIcon icon="ic:round-refresh" />
            </template>
            重置
          </NButton>
          <NButton type="primary" @click="search">
            <template #icon>
              <SvgIcon icon="ic:round-search" />
            </template>
            查询
          </NButton>
        </div>
      </div>
    </NForm>
  </NCard>
</template>

<style scoped></style>
