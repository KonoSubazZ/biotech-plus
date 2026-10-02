<script setup lang="ts">
import { toRaw } from 'vue';
import { jsonClone } from '@sa/utils';
import { $t } from '@/locales';

const model = defineModel<Api.System.TenantSearchParams>('model', { required: true });
const emit = defineEmits<{ search: [] }>();
const defaults = jsonClone(toRaw(model.value));

function reset() {
  Object.assign(model.value, jsonClone(defaults));
  emit('search');
}
</script>

<template>
  <NCard :bordered="false" size="small" class="card-wrapper">
    <NForm :model="model" label-placement="left" :label-width="80" novalidate>
      <NGrid responsive="screen" item-responsive>
        <NFormItemGi span="24 s:12 m:6" :label="$t('page.system.tenant.tenantId')" class="pr-16px">
          <NInput v-model:value="model.tenantId" clearable @keyup.enter="emit('search')" />
        </NFormItemGi>
        <NFormItemGi span="24 s:12 m:6" :label="$t('page.system.tenant.tenantName')" class="pr-16px">
          <NInput v-model:value="model.tenantName" clearable @keyup.enter="emit('search')" />
        </NFormItemGi>
        <NFormItemGi span="24 s:12 m:6" :label="$t('page.system.tenant.status')" class="pr-16px">
          <DictSelect v-model:value="model.status" dict-code="sys_normal_disable" clearable />
        </NFormItemGi>
        <NFormItemGi span="24 s:12 m:6">
          <NSpace class="w-full" justify="end">
            <NButton @click="reset">{{ $t('common.reset') }}</NButton>
            <NButton type="primary" ghost @click="emit('search')">{{ $t('common.search') }}</NButton>
          </NSpace>
        </NFormItemGi>
      </NGrid>
    </NForm>
  </NCard>
</template>
