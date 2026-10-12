<script setup lang="ts">
import { toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'WetLabQcSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Qc.WetLabQcSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/**
 * 状态（单选用 NSelect）：与后端 wet_lab_qc.status 一致，配色见 UX-CONTRACT——
 * 绿=合格 黄=待判定 红=不合格。
 */
const statusOptions = [
  { label: '待判定', value: 'pending' },
  { label: '合格', value: 'passed' },
  { label: '不合格', value: 'failed' }
];

/** 搜索只筛样本条码与质控状态（行里没有别的分类字段） */
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
      <NCollapseItem :title="$t('common.search')" name="wet-lab-qc-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <!-- 搜索项排布用 flex-wrap（不用 NGrid）：按钮组 ml-auto 贴当前行最右，换行后仍贴右 -->
          <div class="flex flex-wrap items-start">
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="样本条码" path="subbarcode">
              <NInput v-model:value="model.subbarcode" placeholder="请输入样本条码" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="质控状态" path="status">
              <NSelect
                v-model:value="model.status"
                :options="statusOptions"
                placeholder="请选择质控状态"
                clearable
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
      </NCollapseItem>
    </NCollapse>
  </NCard>
</template>

<style scoped></style>
