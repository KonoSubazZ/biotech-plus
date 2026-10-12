<script setup lang="ts">
import { computed } from 'vue';
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';

defineOptions({
  name: 'SearchCollapse'
});

interface Props {
  /** Collapse item name, unique in the page */
  name: string;
}

const props = defineProps<Props>();

const themeStore = useThemeStore();

/**
 * Search panel expanded state
 *
 * It is only read when the collapse is mounted, so the change of the setting takes effect on the next page
 * visit. Controlled by `theme.general.search.expanded` in the theme drawer.
 */
const defaultExpandedNames = computed<string[] | null>(() => (themeStore.search.expanded ? [props.name] : null));
</script>

<template>
  <NCard :bordered="false" size="small" class="card-wrapper">
    <NCollapse :default-expanded-names="defaultExpandedNames">
      <NCollapseItem :title="$t('common.search')" :name="name">
        <slot />
      </NCollapseItem>
    </NCollapse>
  </NCard>
</template>

<style scoped></style>
