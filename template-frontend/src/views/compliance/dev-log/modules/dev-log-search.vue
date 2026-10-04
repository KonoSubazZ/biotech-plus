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
          <!--
            搜索栏布局契约（仓库 crud skill §「搜索栏布局契约」）：
            搜索项用 flex-wrap + 每项固定百分比宽；按钮组 ml-auto 自动贴当前行最右；
            一行放不下整组换行后仍贴右。不要用 NGrid + 最后一格放按钮（只会在自己格子内靠右，
            字段数不是列数整数倍时会悬在行中间）。
          -->
          <div class="flex flex-wrap items-start">
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="记录标题" path="title">
              <NInput v-model:value="model.title" placeholder="请输入记录标题" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="分类" path="category">
              <NSelect v-model:value="model.category" :options="categoryOptions" clearable placeholder="请选择分类" />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="开发人员" path="developer">
              <NInput v-model:value="model.developer" placeholder="请输入开发人员" clearable />
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
