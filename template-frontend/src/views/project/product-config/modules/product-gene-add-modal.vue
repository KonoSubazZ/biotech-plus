<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { NButton, NModal, NTabPane, NTabs, NUpload } from 'naive-ui';
import type { UploadCustomRequestOptions } from 'naive-ui';
import {
  fetchImportProductGene,
  fetchImportProductGeneByExcel,
  fetchSearchNcbiGene
} from '@/service/api/project/product-gene';

defineOptions({
  name: 'ProductGeneAddModal'
});

interface Props {
  productId: number | null;
  productLabel?: string;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'submitted'): void;
}

const emit = defineEmits<Emits>();

const visible = defineModel<boolean>('visible', { default: false });

type TabKey = 'search' | 'text' | 'excel';

const tab = ref<TabKey>('search');
const submitting = ref(false);

/** 导入结果（成功后展示） */
const result = ref<Api.Project.ProductGeneImportResult | null>(null);

// ---------------------------------------------------------------- 方式 1：搜索基因库

const searching = ref(false);
const geneOptions = ref<{ label: string; value: number }[]>([]);
const selectedGenes = ref<number[]>([]);
const geneCache = ref<Map<number, Api.Project.NcbiGene>>(new Map());

async function handleSearch(keyword: string) {
  if (!keyword || keyword.trim().length === 0) {
    geneOptions.value = [];
    return;
  }
  searching.value = true;
  try {
    const { data, error } = await fetchSearchNcbiGene(keyword.trim(), 30);
    if (error || !data) {
      return;
    }
    data.forEach(item => geneCache.value.set(item.geneId, item));
    // 同一个 symbol 可能有多个 gene_id，标签里带上 gene_id 与描述后缀，便于分辨
    geneOptions.value = data.map(item => ({
      label: `${item.geneSymbol}（id=${item.geneId}）${item.description ? ` ${item.description.slice(0, 30)}` : ''}`,
      value: item.geneId
    }));
  } finally {
    searching.value = false;
  }
}

// ---------------------------------------------------------------- 方式 2：粘贴列表

const textInput = ref('');

const parsedSymbols = computed(() => {
  const raw = textInput.value.split(/[\r\n,;\t]+/);
  const seen = new Set<string>();
  const out: string[] = [];
  raw.forEach(item => {
    const value = item.replace(/\uFEFF/g, '').trim();
    if (value && !seen.has(value)) {
      seen.add(value);
      out.push(value);
    }
  });
  return out;
});

// ---------------------------------------------------------------- 方式 3：Excel 上传

const uploading = ref(false);

async function handleUpload({ file, onFinish, onError }: UploadCustomRequestOptions) {
  if (props.productId === null) {
    window.$message?.error('缺少产品信息');
    onError();
    return;
  }
  const raw = file.file;
  if (!raw) {
    onError();
    return;
  }
  uploading.value = true;
  try {
    const { data, error } = await fetchImportProductGeneByExcel(props.productId, raw);
    if (error) {
      onError();
      return;
    }
    result.value = data;
    if (data && data.addedCount > 0) {
      window.$message?.success(`新增 ${data.addedCount} 个基因`);
      emit('submitted');
    }
    onFinish();
  } finally {
    uploading.value = false;
  }
}

// ---------------------------------------------------------------- 提交

async function handleSubmit() {
  if (props.productId === null) {
    window.$message?.error('缺少产品信息');
    return;
  }

  let payload: Api.Project.ProductGeneImport;

  if (tab.value === 'search') {
    if (selectedGenes.value.length === 0) {
      window.$message?.warning('请先搜索并选择基因');
      return;
    }
    payload = {
      productId: props.productId,
      genes: selectedGenes.value.map(geneId => ({
        geneId,
        geneSymbol: geneCache.value.get(geneId)?.geneSymbol ?? ''
      }))
    };
  } else if (tab.value === 'text') {
    if (parsedSymbols.value.length === 0) {
      window.$message?.warning('请先粘贴基因符号列表');
      return;
    }
    payload = { productId: props.productId, symbols: parsedSymbols.value };
  } else {
    window.$message?.info('Excel 方式请直接选择文件上传');
    return;
  }

  submitting.value = true;
  try {
    const { data, error } = await fetchImportProductGene(payload);
    if (error) {
      return;
    }
    result.value = data;
    if (data && data.addedCount > 0) {
      window.$message?.success(`新增 ${data.addedCount} 个基因`);
      emit('submitted');
    }
  } finally {
    submitting.value = false;
  }
}

function resetState() {
  tab.value = 'search';
  result.value = null;
  selectedGenes.value = [];
  geneOptions.value = [];
  textInput.value = '';
}

watch(visible, value => {
  if (value) {
    resetState();
  }
});
</script>

<template>
  <NModal
    v-model:show="visible"
    preset="card"
    :title="`添加基因${productLabel ? `：${productLabel}` : ''}`"
    class="w-780px max-w-92%"
  >
    <NTabs v-model:value="tab" type="line" animated>
      <NTabPane name="search" tab="从基因库搜索">
        <NSelect
          v-model:value="selectedGenes"
          multiple
          filterable
          remote
          clearable
          :options="geneOptions"
          :loading="searching"
          placeholder="输入基因符号搜索，如 BRCA1"
          :consistent-menu-width="false"
          @search="handleSearch"
        />
        <div class="mt-8px text-12px text-#999">
          已选 {{ selectedGenes.length }} 个；同一个符号可能有多个 gene_id，选项里已标出 ID 便于分辨
        </div>
      </NTabPane>

      <NTabPane name="text" tab="粘贴列表">
        <NInput
          v-model:value="textInput"
          type="textarea"
          :rows="10"
          placeholder="每行一个基因符号，也支持逗号/分号/制表符分隔，例如：&#10;BRCA1&#10;TP53&#10;EGFR"
        />
        <div class="mt-8px text-12px text-#999">识别到 {{ parsedSymbols.length }} 个（已去重、去空行）</div>
      </NTabPane>

      <NTabPane name="excel" tab="上传 Excel">
        <NUpload
          :max="1"
          accept=".xlsx,.xls"
          :custom-request="handleUpload"
          :show-file-list="false"
          :disabled="uploading"
        >
          <NButton type="primary" :loading="uploading">选择 Excel 文件</NButton>
        </NUpload>
        <div class="mt-12px text-12px text-#999">
          表格第一列放 gene_symbol（可以有表头，会自动跳过）。上传后立即导入并显示结果。
        </div>
      </NTabPane>
    </NTabs>

    <!-- 导入结果 -->
    <div v-if="result" class="mt-16px rounded-8px bg-#f5f7fa px-16px py-12px">
      <div class="text-14px">
        新增 <b class="text-#18a058">{{ result.addedCount }}</b> 条 · 跳过（已存在）
        <b>{{ result.skippedCount }}</b> 条
      </div>
      <div v-if="result.unmatched.length" class="mt-8px text-13px">
        <span class="text-#d03050">未匹配 {{ result.unmatched.length }} 个（未入库）：</span>
        <span>{{ result.unmatched.join('、') }}</span>
      </div>
      <div v-if="result.ambiguous.length" class="mt-8px text-13px">
        <span class="text-#f0a020">有歧义 {{ result.ambiguous.length }} 个（已按 gene_id 最小的关联，请核对）：</span>
        <span>{{ result.ambiguous.join('、') }}</span>
      </div>
    </div>

    <template #footer>
      <NSpace justify="end" :size="16">
        <NButton @click="visible = false">关闭</NButton>
        <NButton
          v-if="tab !== 'excel'"
          type="primary"
          :loading="submitting"
          :disabled="tab === 'search' ? selectedGenes.length === 0 : parsedSymbols.length === 0"
          @click="handleSubmit"
        >
          确定导入
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped></style>
