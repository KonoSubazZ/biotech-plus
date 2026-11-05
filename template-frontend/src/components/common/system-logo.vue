<script lang="ts" setup>
import { computed } from 'vue';
import logoDark from '@/assets/imgs/novogene-logo-white.png';
import logoLight from '@/assets/imgs/novogene-logo.png';
import markDark from '@/assets/imgs/novogene-mark-white.png';
import markLight from '@/assets/imgs/novogene-mark.png';

defineOptions({ name: 'SystemLogo' });

interface Props {
  /** mark: 方形标记；full: 横版品牌 logo */
  variant?: 'mark' | 'full';
  /** 显示高度（px） */
  size?: number;
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'mark',
  size: 28
});

const imgStyle = computed(() => ({ height: `${props.size}px` }));
const lightSrc = computed(() => (props.variant === 'full' ? logoLight : markLight));
const darkSrc = computed(() => (props.variant === 'full' ? logoDark : markDark));
</script>

<template>
  <img
    :src="lightSrc"
    :style="imgStyle"
    alt="Novogene 诺禾致源"
    class="w-auto select-none dark:hidden"
  />
  <img
    :src="darkSrc"
    :style="imgStyle"
    alt="Novogene 诺禾致源"
    class="w-auto select-none hidden dark:block"
  />
</template>

<style scoped></style>
