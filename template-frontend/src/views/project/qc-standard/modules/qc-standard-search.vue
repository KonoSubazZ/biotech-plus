<script setup lang="ts">
import { onMounted, ref, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchGetActiveProductConfigList } from '@/service/api/project/product-config';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'QcStandardSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Qc.QcStandardSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 关联产品下拉（只取启用中的产品） */
const productOptions = ref<{ label: string; value: number }[]>([]);

async function loadProductOptions() {
  const { data, error } = await fetchGetActiveProductConfigList();
  if (error || !data) {
    return;
  }
  productOptions.value = data.map(item => ({ label: `${item.productName}（${item.productCode}）`, value: item.id }));
}

onMounted(loadProductOptions);

/** 质控类别（单选用 NSelect，UX-CONTRACT.md） */
const categoryOptions = [
  { label: '湿实验', value: 'wet_lab' },
  { label: '生信', value: 'bioinfo' }
];

/** 状态 */
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
  <SearchCollapse name="qc-standard-search">
    <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
      <!--
        搜索项排布用 flex-wrap（不用 NGrid）：按钮组 ml-auto 贴当前行最右，换行后仍贴右。
        与 report/*、qc/*、compliance/* 的搜索栏同一套（业务页定稿布局）。
      -->
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="关联产品" path="productId">
          <NSelect
            v-model:value="model.productId"
            :options="productOptions"
            filterable
            clearable
            placeholder="请选择产品"
          />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="质控项目" path="qcItem">
          <NInput v-model:value="model.qcItem" placeholder="请输入质控项目名称" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="质控类别" path="qcCategory">
          <NSelect v-model:value="model.qcCategory" :options="categoryOptions" clearable placeholder="请选择质控类别" />
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
