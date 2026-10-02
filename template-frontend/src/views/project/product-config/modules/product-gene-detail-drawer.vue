<script setup lang="tsx">
import { computed, ref, watch } from 'vue';
import { NButton, NInput, NTag } from 'naive-ui';
import {
  fetchBatchDeleteProductGene,
  fetchGetProductGeneCount,
  fetchGetProductGeneList
} from '@/service/api/project/product-gene';
import { useAuth } from '@/hooks/business/auth';
import { defaultTransform, useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { $t } from '@/locales';
import ButtonIcon from '@/components/custom/button-icon.vue';
import ProductGeneAddModal from './product-gene-add-modal.vue';

defineOptions({
  name: 'ProductGeneDetailDrawer'
});

interface Props {
  /** 要查看的产品（从列表行传入） */
  product?: Api.Project.ProductConfig | null;
}

const props = defineProps<Props>();

const visible = defineModel<boolean>('visible', { default: false });

const { hasAuth } = useAuth();

const productId = computed(() => props.product?.id ?? null);
const productLabel = computed(() => {
  if (!props.product) return '';
  return `${props.product.name}（${props.product.code}）`;
});

/** 已关联的基因数（头部展示） */
const geneCount = ref(0);
const addModalVisible = ref(false);

const searchParams = ref<Api.Project.ProductGeneSearchParams>({
  productId: null,
  geneSymbol: null,
  pageNum: 1,
  pageSize: 20,
  params: {}
});

const { columns, data, getData, loading, mobilePagination } = useNaivePaginatedTable({
  api: () => fetchGetProductGeneList(searchParams.value),
  // RuoYi 的分页响应是顶层 rows/total，需要用 defaultTransform 适配
  transform: response => defaultTransform(response),
  onPaginationParamsChange: params => {
    searchParams.value.pageNum = params.page;
    searchParams.value.pageSize = params.pageSize;
  },
  columns: () => [
    { type: 'selection', align: 'center', width: 48 },
    { key: 'geneSymbol', title: '基因符号', align: 'center', minWidth: 140 },
    { key: 'geneId', title: '基因 ID', align: 'center', width: 120 },
    {
      key: 'remark',
      title: '备注',
      align: 'center',
      minWidth: 140,
      render: row => row.remark || '-'
    },
    { key: 'createTime', title: '添加时间', align: 'center', minWidth: 160 },
    {
      key: 'operate',
      title: $t('common.operate'),
      align: 'center',
      width: 80,
      render: row => {
        if (!hasAuth('project:productGene:remove')) {
          return null;
        }
        return (
          <ButtonIcon
            text
            type="error"
            icon="material-symbols:delete-outline"
            tooltipContent={$t('common.delete')}
            popconfirmContent={$t('common.confirmDelete')}
            onPositiveClick={() => handleDelete(row.id)}
          />
        );
      }
    }
  ]
});

// 勾选 / 批量删除 / 单条删除后回调由 useTableOperate 提供（不在 useNaivePaginatedTable 里）
const { checkedRowKeys, onBatchDeleted, onDeleted } = useTableOperate(data, 'id', getData);

/** 头部计数 + 列表一起刷新 */
async function reloadAll() {
  if (productId.value === null) {
    return;
  }
  const { data: count, error } = await fetchGetProductGeneCount(productId.value);
  if (!error) {
    geneCount.value = count ?? 0;
  }
  await getData();
}

async function handleSearch() {
  searchParams.value.pageNum = 1;
  await getData();
}

async function handleDelete(id: number) {
  const { error } = await fetchBatchDeleteProductGene([id]);
  if (error) {
    return;
  }
  window.$message?.success($t('common.deleteSuccess'));
  onDeleted();
  await reloadAll();
}

async function handleBatchDelete() {
  if (checkedRowKeys.value.length === 0) {
    window.$message?.warning('请先勾选要删除的基因');
    return;
  }
  const { error } = await fetchBatchDeleteProductGene(checkedRowKeys.value);
  if (error) {
    return;
  }
  window.$message?.success($t('common.deleteSuccess'));
  onBatchDeleted();
  await reloadAll();
}

function handleAddSuccess() {
  reloadAll();
}

watch(visible, value => {
  if (!value) {
    return;
  }
  searchParams.value.productId = productId.value;
  searchParams.value.pageNum = 1;
  reloadAll();
});
</script>

<template>
  <NDrawer v-model:show="visible" :trap-focus="false" display-directive="show" :width="900" class="max-w-92%">
    <NDrawerContent :title="`产品基因详情：${productLabel}`" :native-scrollbar="false" closable>
      <!-- 头部：产品信息 + 基因数 -->
      <div class="mb-16px flex flex-wrap items-center gap-12px rounded-8px bg-#f5f7fa px-16px py-12px">
        <NTag type="info" :bordered="false">已关联基因 {{ geneCount }} 个</NTag>
        <span v-if="props.product?.testType" class="text-14px text-#666">检测类型：{{ props.product.testType }}</span>
        <span v-if="props.product?.relatedDiseases" class="text-14px text-#666">
          相关疾病：{{ props.product.relatedDiseases }}
        </span>
      </div>

      <!-- 工具条 -->
      <div class="mb-12px flex flex-wrap items-center justify-between gap-12px">
        <div class="flex items-center gap-8px">
          <NInput
            v-model:value="searchParams.geneSymbol"
            placeholder="按基因符号搜索"
            clearable
            class="w-220px"
            @keyup.enter="handleSearch"
          />
          <NButton type="primary" @click="handleSearch">搜索</NButton>
        </div>
        <div class="flex items-center gap-8px">
          <NButton v-if="hasAuth('project:productGene:remove')" @click="handleBatchDelete">删除选中</NButton>
          <NButton
            v-if="hasAuth('project:productGene:add')"
            type="primary"
            @click="addModalVisible = true"
          >
            添加基因
          </NButton>
        </div>
      </div>

      <!-- 列表：服务端分页（remote）+ 分页器交给 NDataTable，与产品配置列表页保持同一写法 -->
      <NDataTable
        v-model:checked-row-keys="checkedRowKeys"
        remote
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="mobilePagination"
        :row-key="row => row.id"
        :scroll-x="700"
        size="small"
      />

      <ProductGeneAddModal
        v-model:visible="addModalVisible"
        :product-id="productId"
        :product-label="productLabel"
        @submitted="handleAddSuccess"
      />
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped></style>
