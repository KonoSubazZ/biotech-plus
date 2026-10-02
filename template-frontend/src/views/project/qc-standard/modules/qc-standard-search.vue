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
  productOptions.value = data.map(item => ({ label: `${item.name}（${item.code}）`, value: item.id }));
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
  <NCard :bordered="false" size="small" class="card-wrapper">
    <NCollapse>
      <NCollapseItem :title="$t('common.search')" name="qc-standard-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="80">
          <NGrid responsive="screen" item-responsive>
            <NFormItemGi span="24 s:12 m:8" label="关联产品" path="productId" class="pr-24px">
              <NSelect
                v-model:value="model.productId"
                :options="productOptions"
                filterable
                clearable
                placeholder="请选择产品"
              />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="质控项目" path="qcItem" class="pr-24px">
              <NInput v-model:value="model.qcItem" placeholder="请输入质控项目名称" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="质控类别" path="qcCategory" class="pr-24px">
              <NSelect v-model:value="model.qcCategory" :options="categoryOptions" clearable placeholder="请选择质控类别" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="状态" path="status" class="pr-24px">
              <NSelect v-model:value="model.status" :options="statusOptions" clearable placeholder="请选择状态" />
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
