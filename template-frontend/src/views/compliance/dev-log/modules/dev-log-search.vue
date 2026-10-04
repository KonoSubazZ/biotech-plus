<script setup lang="ts">
import { toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'DevLogSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Compliance.DevLogSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 分类（单选用 NSelect，UX-CONTRACT.md） */
const categoryOptions = [
  { label: '功能新增', value: 'feature' },
  { label: '缺陷修复', value: 'fix' },
  { label: '变更调整', value: 'change' }
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
      <NCollapseItem :title="$t('common.search')" name="dev-log-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="80">
          <NGrid responsive="screen" item-responsive>
            <NFormItemGi span="24 s:12 m:8" label="记录标题" path="title" class="pr-24px">
              <NInput v-model:value="model.title" placeholder="请输入记录标题" clearable />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="分类" path="category" class="pr-24px">
              <NSelect v-model:value="model.category" :options="categoryOptions" clearable placeholder="请选择分类" />
            </NFormItemGi>
            <NFormItemGi span="24 s:12 m:8" label="开发人员" path="developer" class="pr-24px">
              <NInput v-model:value="model.developer" placeholder="请输入开发人员" clearable />
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
