<script setup lang="ts">
import { toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ProductConfigSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Project.ProductConfigSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 状态下拉：单选用 NSelect（UX-CONTRACT.md） */
const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'inactive' }
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
  <SearchCollapse name="product-config-search">
    <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
      <!--
        搜索项排布用 flex-wrap（不用 NGrid）：按钮组 ml-auto 贴当前行最右，换行后仍贴右。
        与 report/*、qc/*、compliance/* 的搜索栏同一套（业务页定稿布局）。
      -->
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="产品名称" path="productName">
          <NInput v-model:value="model.productName" placeholder="请输入产品名称" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="产品编码" path="productCode">
          <NInput v-model:value="model.productCode" placeholder="请输入产品编码" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="生信编码" path="productBio">
          <NInput v-model:value="model.productBio" placeholder="请输入生信编码" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="状态" path="status">
          <NSelect v-model:value="model.status" :options="statusOptions" clearable placeholder="请选择状态" />
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
