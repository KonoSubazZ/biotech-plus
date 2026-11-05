<script setup lang="ts">
import { toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ProjectConfigSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Project.ProjectConfigSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

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
  <SearchCollapse name="project-config-search">
    <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
      <!--
        搜索项排布用 flex-wrap（不用 NGrid）：按钮组 ml-auto 贴当前行最右，换行后仍贴右。
        与 report/*、qc/*、compliance/* 的搜索栏同一套（业务页定稿布局）。
      -->
      <div class="flex flex-wrap items-start">
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="项目编号" path="projectNo">
          <NInput v-model:value="model.projectNo" placeholder="请输入项目编号" clearable />
        </NFormItem>
        <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="客户名称" path="customer">
          <NInput v-model:value="model.customer" placeholder="请输入客户名称" clearable />
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
