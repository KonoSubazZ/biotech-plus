<script lang="ts" setup>
import { $t } from '@/locales';

defineOptions({
  name: 'GlobalLogo'
});

interface Props {
  /** 侧栏展开时显示系统名，收起时只留标记 */
  showTitle?: boolean;
}

withDefaults(defineProps<Props>(), {
  showTitle: true
});
</script>

<template>
  <RouterLink
    to="/"
    class="w-full flex-y-center nowrap-hidden"
    :class="showTitle ? 'px-16px' : 'justify-center'"
  >
    <SystemLogo :size="28" />
    <span v-show="showTitle" class="pl-10px text-16px text-primary font-600 dark:text-white">
      {{ $t('system.title') }}
    </span>
  </RouterLink>
</template>

<style scoped></style>
