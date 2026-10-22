<script setup lang="ts">
import { onMounted, ref, toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { fetchGetTemplateProjectOptions } from '@/service/api/report/template';
import { useNaiveForm } from '@/hooks/common/form';
import { $t } from '@/locales';

defineOptions({
  name: 'ReportTemplateSearch'
});

interface Emits {
  (e: 'search'): void;
}

const emit = defineEmits<Emits>();

const { formRef, validate, restoreValidation } = useNaiveForm();

const model = defineModel<Api.Report.ReportTemplateSearchParams>('model', { required: true });

const defaultModel = jsonClone(toRaw(model.value));

/** 项目下拉（模板必须先选项目再选产品，搜索也按项目筛） */
const projectOptions = ref<{ label: string; value: number }[]>([]);

onMounted(async () => {
  const { data, error } = await fetchGetTemplateProjectOptions();
  if (error) return;
  projectOptions.value = (data ?? []).map(item => ({
    label: `${item.projectNo} ${item.projectName}`,
    value: item.id
  }));
});

/** 状态取值固定为 active 启用 / inactive 停用 */
const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'inactive' }
];

const reportTypeOptions = [
  { label: '体细胞（SOMATIC）', value: 'SOMATIC' },
  { label: '胚系（GERMLINE）', value: 'GERMLINE' },
  { label: '其他（OTHER）', value: 'OTHER' }
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
  <SearchCollapse name="project-template-config-search">
        <NForm ref="formRef" :model="model" label-placement="left" :label-width="90">
          <!--
            搜索项用 flex-wrap（不用 NGrid）：按钮组 ml-auto 自动贴本行最右；
            一行放不下时整组换到下一行，ml-auto 仍让它贴最右。
          -->
          <div class="flex flex-wrap items-start">
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="项目" path="projectId">
              <NSelect
                v-model:value="model.projectId"
                :options="projectOptions"
                placeholder="请选择项目（必选后再选产品）"
                clearable
                filterable
              />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="模板名称" path="templateName">
              <NInput v-model:value="model.templateName" placeholder="请输入模板名称" clearable />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="报告类型" path="reportType">
              <NSelect
                v-model:value="model.reportType"
                :options="reportTypeOptions"
                placeholder="请选择报告类型"
                clearable
                filterable
              />
            </NFormItem>
            <NFormItem class="w-full pr-24px sm:w-1/2 xl:w-1/3" label="状态" path="status">
              <NSelect v-model:value="model.status" :options="statusOptions" placeholder="请选择状态" clearable />
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
