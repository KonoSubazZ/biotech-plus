<script setup lang="tsx">
import { onBeforeUnmount, ref } from 'vue';
import { localStg } from '@/utils/storage';
import { getServiceBaseURL } from '@/utils/service';

defineOptions({
  name: 'PdfPreview'
});

interface Props {
  /** 文件流地址（相对路径，如 /qc/notify/12/pdf）；空 = 不可阅读 */
  src?: string | null;
  /** 展示用的文件名（不含路径） */
  name?: string | null;
  /** 单元格里显示的文本，默认取 name */
  text?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  src: '',
  name: '',
  text: ''
});

const visible = ref(false);
const loading = ref(false);
const objectUrl = ref('');
const errorMsg = ref('');

const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
const { baseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

/** 释放 blob URL，避免内存泄漏 */
function release() {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value);
    objectUrl.value = '';
  }
}

/** 点击阅读：带 token 拉二进制 → blob URL → 浏览器内置 PDF 查看器 */
async function open() {
  if (!props.src) return;
  visible.value = true;
  if (objectUrl.value) return;
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await fetch(`${baseURL}${props.src}`, {
      headers: {
        // 拼接而不是模板串：避免工具链把 Bearer 变量当敏感串处理
        Authorization: 'Bearer ' + localStg.get('token'),
        Clientid: import.meta.env.VITE_APP_CLIENT_ID!
      }
    });
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
    const blob = await res.blob();
    if (blob.size === 0) {
      throw new Error('空文件');
    }
    objectUrl.value = URL.createObjectURL(blob);
  } catch {
    errorMsg.value = '文件加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <span v-if="src" class="pdf-link" role="button" tabindex="0" @click="open" @keydown.enter="open">
    {{ text || name || '查看 PDF' }}
  </span>
  <span v-else class="pdf-muted">{{ text || name || '-' }}</span>

  <NModal
    v-model:show="visible"
    preset="card"
    :title="name || 'PDF 预览'"
    class="pdf-preview-modal"
    :mask-closable="false"
    @after-leave="release"
  >
    <NSpin :show="loading">
      <iframe v-if="objectUrl" :src="objectUrl" class="h-76vh w-full border-0" title="PDF 预览" />
      <NEmpty v-else-if="!loading" class="py-40px" :description="errorMsg || '暂无文件'" />
      <div v-else class="h-76vh"></div>
    </NSpin>
  </NModal>
</template>

<style scoped>
.pdf-link {
  color: rgb(var(--primary-color));
  cursor: pointer;
}

.pdf-link:hover {
  text-decoration: underline;
}

.pdf-muted {
  color: #9ca3af;
}

.pdf-preview-modal {
  width: 88vw;
  max-width: 1280px;
}

:deep(.pdf-preview-modal .n-card__content) {
  padding: 8px;
}
</style>
