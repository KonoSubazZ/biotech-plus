import { request } from '@/service/request';

/**
 * 产品关联基因接口层
 * <p>
 * 一个产品一份 gene list：先建产品，之后在「详情」里逐个或批量补基因。
 * 三种添加方式共用 /import（genes 字段给 geneId，symbols 字段只给符号），
 * Excel 走 /importExcel 由后端解析第一列。
 */

/** 分页查询某产品的关联基因（详情列表） */
export function fetchGetProductGeneList(params: Api.Project.ProductGeneSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Project.ProductGene>>({
    url: '/project/productGene/list',
    method: 'get',
    params
  });
}

/** 某产品已关联的基因数 */
export function fetchGetProductGeneCount(productId: number) {
  return request<number>({
    url: '/project/productGene/count',
    method: 'get',
    params: { productId }
  });
}

/** 从基因库（nkb.ncbi_gene）搜索基因，供「添加基因」选择器使用 */
export function fetchSearchNcbiGene(keyword: string, limit = 20) {
  return request<Api.Project.NcbiGene[]>({
    url: '/project/productGene/searchGene',
    method: 'get',
    params: { keyword, limit }
  });
}

/** 批量导入（搜索勾选的 genes / 粘贴的 symbols） */
export function fetchImportProductGene(data: Api.Project.ProductGeneImport) {
  return request<Api.Project.ProductGeneImportResult>({
    url: '/project/productGene/import',
    method: 'post',
    data
  });
}

/** 上传 Excel 导入（表格第一列是 gene_symbol） */
export function fetchImportProductGeneByExcel(productId: number, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('productId', String(productId));
  return request<Api.Project.ProductGeneImportResult>({
    url: '/project/productGene/importExcel',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

/** 批量删除关联关系 */
export function fetchBatchDeleteProductGene(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/project/productGene/${ids.join(',')}`,
    method: 'delete'
  });
}
