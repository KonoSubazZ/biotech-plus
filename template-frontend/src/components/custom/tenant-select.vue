<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchTenantOptions } from '@/service/api/system/tenant';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';

defineProps<{ disabled?: boolean }>();
const value = defineModel<string | null>('value', { default: null });
const authStore = useAuthStore();
const isAdmin = computed(() => String(authStore.userInfo.user?.userId) === '1');
const loading = ref(false);
const options = ref<{ label: string; value: string }[]>([]);

async function loadOptions(show = true) {
  if (!show || loading.value) return;
  loading.value = true;
  try {
    const { data, error } = await fetchTenantOptions();
    if (!error) {
      options.value = data.map(tenant => ({
        label: `${tenant.tenantName} (${tenant.tenantId})`,
        value: tenant.tenantId
      }));
      if (!isAdmin.value) value.value = authStore.userInfo.user?.tenantId ?? null;
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => loadOptions());
</script>

<template>
  <NSelect
    v-model:value="value"
    :options="options"
    :loading="loading"
    :disabled="disabled || !isAdmin"
    :placeholder="$t('page.system.tenant.selectTenant')"
    filterable
    @update:show="loadOptions"
  />
</template>
