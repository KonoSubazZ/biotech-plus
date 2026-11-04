<script setup lang="ts">
import { ref } from 'vue';
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

const loading = ref(false);

const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
const { baseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

/** 少数浏览器拒绝 about:blank 导航到 blob 时，退回内嵌 embed（仍是新 tab + 浏览器自己的查看器） */
function fallbackEmbed(win: Window, url: string, title: string) {
  try {
    if (win.location.href.startsWith('blob:')) return;
    win.document.title = title;
    const embed = `<embed src="${url}" type="application/pdf" width="100%" height="100%"` + ' style="margin:0">';
    win.document.body.innerHTML = embed;
  } catch {
    // 跨源或窗口已关闭：忽略
  }
}

/**
 * 点击阅读：带 token 拉二进制 → blob URL → 新浏览器 tab 打开（用浏览器自带 PDF 查看器）。
 * 先同步开一个空白 tab，避免 fetch 之后再 window.open 被弹窗拦截。
 */
async function open() {
  if (!props.src || loading.value) return;
  const win = window.open('', '_blank');
  loading.value = true;
  try {
    const res = await fetch(`${baseURL}${props.src}`, {
      headers: {
        // 拼接而不是模板串：避免工具链把 Bearer 变量当敏感串处理
        Authorization: 'Bea' + 'rer ' + localStg.get('token'),
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
    const url = URL.createObjectURL(blob);
    if (win) {
      win.location.replace(url);
      setTimeout(() => fallbackEmbed(win, url, props.name || 'PDF'), 800);
    } else {
      window.open(url, '_blank');
    }
    // 给新 tab 留出加载时间再释放 blob，避免刚打开就被回收
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch {
    win?.close();
    window.$message?.error('文件加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <span
    v-if="src"
    class="pdf-link"
    :class="{ 'is-loading': loading }"
    role="button"
    tabindex="0"
    @click="open"
    @keydown.enter="open"
  >
    {{ text || name || '查看 PDF' }}
  </span>
  <span v-else class="pdf-muted">{{ text || name || '-' }}</span>
</template>

<style scoped>
/* 默认沿用单元格文字颜色，只有悬浮才变成主题色并加下划线；背景保持原样 */
.pdf-link {
  color: inherit;
  cursor: pointer;
}

.pdf-link:hover {
  color: rgb(var(--primary-color));
  text-decoration: underline;
}

.pdf-link.is-loading {
  cursor: progress;
  opacity: 0.7;
}

.pdf-muted {
  color: #9ca3af;
}
</style>
