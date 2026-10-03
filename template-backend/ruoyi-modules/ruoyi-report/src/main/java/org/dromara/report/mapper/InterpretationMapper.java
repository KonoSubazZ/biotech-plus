package org.dromara.report.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.report.domain.bo.InterpretationFileQueryBo;
import org.dromara.report.domain.bo.InterpretationQueryBo;
import org.dromara.report.domain.bo.InterpretationVariantQueryBo;
import org.dromara.report.domain.vo.AnalysisSnapshotVo;
import org.dromara.report.domain.vo.InterpretationFileContentVo;
import org.dromara.report.domain.vo.InterpretationFileVo;
import org.dromara.report.domain.vo.InterpretationRowVo;

import java.util.List;
import java.util.Map;

/**
 * 报告解读列表 / 批次快照 Mapper（跨表查询，SQL 见 resources/mapper/report/InterpretationMapper.xml）
 *
 * @author <你的名字>
 */
public interface InterpretationMapper {

    /**
     * 分页查询「报告解读」列表：analysis_data ⟕ 该批次最新一份 analysis_report。
     *
     * @param page       分页参数（由 MyBatis-Plus 分页插件接管）
     * @param query      查询条件（样本编号 / 产品 / 报告状态）
     * @param beginTime  分析日期起（yyyyMMdd，可空）
     * @param endTime    分析日期止（yyyyMMdd，可空）
     * @return 分页结果
     */
    Page<InterpretationRowVo> selectInterpretationPage(@Param("page") Page<InterpretationRowVo> page,
                                                      @Param("query") InterpretationQueryBo query,
                                                      @Param("beginTime") String beginTime,
                                                      @Param("endTime") String endTime);

    /**
     * 取一个分析批次的样本/产品快照，「进入解读」建报告记录时用。
     *
     * @param analysisId 分析数据ID
     * @return 快照；批次不存在时返回 null
     */
    AnalysisSnapshotVo selectAnalysisSnapshot(@Param("analysisId") Long analysisId);

    /**
     * 分页查询某个分析批次的集群文件（data_file_status）；不返回 file_text 大字段。
     *
     * @param page       分页参数
     * @param analysisId 分析数据ID
     * @param query      查询条件（文件名 / 文件类型 / 状态）
     * @return 分页结果
     */
    Page<InterpretationFileVo> selectFilePage(@Param("page") Page<InterpretationFileVo> page,
                                              @Param("analysisId") Long analysisId,
                                              @Param("query") InterpretationFileQueryBo query);

    /**
     * 取单个文件的文本内容（data_file_status.file_text），超长按 maxLength 截断。
     *
     * @param fileId    文件ID
     * @param analysisId 分析数据ID（校验文件确实属于该批次）
     * @param maxLength 最大返回字符数
     * @return 文件内容；文件不存在或不属于该批次时返回 null
     */
    InterpretationFileContentVo selectFileContent(@Param("fileId") Long fileId,
                                                 @Param("analysisId") Long analysisId,
                                                 @Param("maxLength") int maxLength);

    /**
     * 分页查询某分析批次的位点（按 sourceType 决定查哪张明细表，SQL 见 XML 的 choose 分支）。
     *
     * @param page       分页参数
     * @param analysisId 分析数据ID
     * @param query      查询条件（sourceType 必填 / gene / isReported）
     * @return 分页结果
     */
    Page<Map<String, Object>> selectVariantPage(@Param("page") Page<Map<String, Object>> page,
                                               @Param("analysisId") Long analysisId,
                                               @Param("query") InterpretationVariantQueryBo query);

    /**
     * 切换位点「入报告」状态；UPDATE 带 analysisId 归属校验，返回 0 表示位点不存在或不属于该批次。
     *
     * @param sourceId          位点主键
     * @param sourceType        位点类型
     * @param analysisId        分析数据ID
     * @param isReported        目标状态 0/1
     * @param filteredRationale 过滤理由（可空）
     * @param operatorId        操作人（reviewed_by / update_by）
     * @return 受影响行数
     */
    /**
     * 预览用：本批次所有「报出」的体细胞位点（SNP/Indel + CNV + Fusion 的并集）。
     * <p>
     * 四张表列不同，统一成预览需要的列：sourceType/sourceId/gene/variant/oriVariant/mutationTypeRaw/frequency/depth。
     *
     * @param analysisId 分析数据ID
     * @return 位点列表
     */
    List<Map<String, Object>> selectReportedSomaticVariants(@Param("analysisId") Long analysisId);

    /**
     * 预览用：报告 + 分析行（模板/产品/癌种/产品编码）
     *
     * @param reportId 报告ID
     * @return 行（reportId / analysisId / subbarcode / product / productId / template / templateId /
     *         disease / cancerType / templateCode / moduleCode）；无则 null
     */
    Map<String, Object> selectReportRow(@Param("reportId") Long reportId);

    /**
     * 预览用：本批次所有「报出」的胚系位点（file_CR_ALL）。
     *
     * @param analysisId 分析数据ID
     * @return 位点列表（含 zygosity / clinicalSignificanceClnsig）
     */
    List<Map<String, Object>> selectReportedGermlineVariants(@Param("analysisId") Long analysisId);

    /**
     * 产品启用的基因（圣域规则按 product_gene 输出）
     *
     * @param productId 产品配置ID
     * @return 基因符号列表
     */
    List<String> selectProductGeneSymbols(@Param("productId") Long productId);

    /**
     * 按 match_key 取体细胞匹配历史（命中即复用冻结结果）
     *
     * @param matchKey 规范化条件 SHA-256
     * @return 历史行（含 match_result）；无则 null
     */
    Map<String, Object> selectSomaticHistory(@Param("matchKey") String matchKey);

    /**
     * 按 match_key 取胚系匹配历史
     *
     * @param matchKey 规范化条件 SHA-256
     * @return 历史行（含 match_result / clinical_significance）；无则 null
     */
    Map<String, Object> selectGermlineHistory(@Param("matchKey") String matchKey);

    /**
     * 写入首条体细胞匹配历史（INSERT IGNORE，靠 uk(match_key) 收敛并发）
     *
     * @param history 历史字段
     * @return 影响行数（0 = 已存在，说明并发下别人先写了）
     */
    int insertSomaticHistory(@Param("h") Map<String, Object> history);

    /**
     * 写入首条胚系匹配历史（INSERT IGNORE）
     *
     * @param history 历史字段（含 clinical_significance）
     * @return 影响行数
     */
    int insertGermlineHistory(@Param("h") Map<String, Object> history);

    /**
     * 用新规则结果覆盖已有历史（仅用于兼容旧格式冻结结果：老结构无法解析时按最新规则重算后覆盖同一条）
     *
     * @param matchKey       匹配键
     * @param matchStatus    匹配状态
     * @param variationClass 位点分级
     * @param matchResult    冻结结果 JSON
     * @param germline       是否胚系（决定写哪张表）
     * @return 影响行数
     */
    int refreshHistory(@Param("matchKey") String matchKey,
                       @Param("matchStatus") String matchStatus,
                       @Param("variationClass") String variationClass,
                       @Param("matchResult") String matchResult,
                       @Param("germline") boolean germline);

    /**
     * 胚系临床意义继承：同客户/产品/癌种/性别/位点/合子状态下最近一条（**故意忽略人工父级 parent_mutation_id**）
     *
     * @param q 查询条件
     * @return clinical_significance；无则 null
     */
    Integer selectInheritedGermlineSignificance(@Param("q") Map<String, Object> q);

    /**
     * 保存人工确认的胚系临床意义（要求该位点已通过预览建立历史记录）
     *
     * @param matchKey             规范化匹配键
     * @param clinicalSignificance 1~5
     * @return 影响行数（0 = 还没建立历史）
     */
    int updateGermlineSignificance(@Param("matchKey") String matchKey,
                                  @Param("clinicalSignificance") Integer clinicalSignificance,
                                  @Param("matchResult") String matchResult,
                                  @Param("matchStatus") String matchStatus,
                                  @Param("variationClass") String variationClass);

    /**
     * 取单个 CR_ALL 位点（保存临床意义前要重算 match_key）
     *
     * @param sourceId   位点ID
     * @param analysisId 分析数据ID（归属校验）
     * @return gene/variant/oriVariant/zygosity；不存在时 null
     */
    Map<String, Object> selectGermlineVariant(@Param("sourceId") Long sourceId,
                                             @Param("analysisId") Long analysisId);

    /**
     * 改靶：更新胚系位点的人工父级（带分析批次归属校验）
     *
     * @param sourceId         位点ID
     * @param analysisId       分析数据ID
     * @param parentMutationId 人工父级；null 表示取消改靶
     * @return 影响行数
     */
    int updateGermlineParentMutation(@Param("sourceId") Long sourceId,
                                    @Param("analysisId") Long analysisId,
                                    @Param("parentMutationId") Long parentMutationId);

    int updateVariantReportStatus(@Param("sourceId") Long sourceId,
                                 @Param("sourceType") String sourceType,
                                 @Param("analysisId") Long analysisId,
                                 @Param("isReported") Integer isReported,
                                 @Param("filteredRationale") String filteredRationale,
                                 @Param("operatorId") Long operatorId);
}
