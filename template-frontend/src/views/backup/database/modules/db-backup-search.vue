<script setup lang="ts">
import { computed, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

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

/** 备份范围就在配置里（backup.yml 的 mysql.databases）；单选用 NSelect（UX-CONTRACT.md） */
const dbOptions = [
  { label: 'ry-vue', value: 'ry-vue' },
  { label: 'nkb', value: 'nkb' }
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
  <!-- 统一用公共组件 SearchCollapse（= NCard + NCollapse + 「搜索栏默认展开」通用设置），不要自己拼 NCard/NCollapse -->
  <SearchCollapse name="db-backup-search">
    <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
      <!-- 搜索项排布用 flex-wrap（不用 NGrid）：按钮组 ml-auto 贴当前行最右，换行后仍贴右 -->
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="库名" path="dbName">
          <NSelect v-model:value="model.dbName" :options="dbOptions" clearable filterable placeholder="请选择库名" />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="备份日期" path="beginDate">
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
