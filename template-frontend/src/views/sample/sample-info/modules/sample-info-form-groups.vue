<script setup lang="ts">
import { computed } from 'vue';
import { CMS_SAMPLE_GROUPS } from './cms-sample-fields';

defineOptions({
  name: 'SampleInfoFormGroups'
});

interface Props {
  /** view = 只读详情（NDescriptions）；add/edit = 可编辑表单 */
  mode: 'view' | 'add' | 'edit';
}

const props = defineProps<Props>();

const model = defineModel<Api.Report.CmsSampleForm>('model', { required: true });

const readonly = computed(() => props.mode === 'view');

/** NInput 的值类型含 number，统一收敛成字符串；null 保持 null */
function setField(key: keyof Api.Report.CmsSampleForm, value: string | number | null) {
  const target = model.value as Record<string, string | null>;
  target[key] = value === null ? null : String(value);
}

/** 读取字段值：统一成字符串给输入框，空值给空串 */
function getField(key: keyof Api.Report.CmsSampleForm) {
  const value = model.value[key];
  return value === null || value === undefined ? '' : String(value);
}
</script>

<template>
  <div v-for="group in CMS_SAMPLE_GROUPS" :key="group.key" class="mb-20px">
    <div class="mb-8px flex items-center gap-8px text-14px font-500">
      <span class="h-14px w-3px rounded-2px bg-primary"></span>
      {{ group.name }}
      <span class="text-12px color-#8f959e font-400">{{ group.fields.length }} 项</span>
    </div>

    <NDescriptions v-if="readonly" :column="3" bordered size="small" label-placement="top">
      <NDescriptionsItem
        v-for="field in group.fields"
        :key="field.key"
        :label="field.label"
        :span="field.long ? 3 : 1"
      >
        {{ model[field.key] || '-' }}
      </NDescriptionsItem>
    </NDescriptions>

    <div v-else class="grid grid-cols-1 gap-14px md:grid-cols-2 xl:grid-cols-3">
      <NFormItem
        v-for="field in group.fields"
        :key="field.key"
        :label="field.label"
        :path="field.key"
        :class="field.long ? 'md:col-span-2 xl:col-span-3' : ''"
        label-placement="top"
        :show-feedback="false"
      >
        <NInput
          :value="getField(field.key)"
          :type="field.long ? 'textarea' : 'text'"
          :autosize="field.long ? { minRows: 2, maxRows: 4 } : undefined"
          :placeholder="`请输入${field.label}`"
          @update:value="value => setField(field.key, value)"
        />
      </NFormItem>
    </div>
  </div>
</template>

<style scoped></style>
