package org.dromara.drive.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * analysis_data 写侧 Mapper（读侧在 ruoyi-report，本模块只负责写）。
 * <p>
 * 口径（用户 2026-10-22）：
 * <ul>
 *   <li>批次身份 = {@code analysis_batch_date}（上传目录名 YYYYMMDDHHMMSS），没有 batch_no；</li>
 *   <li>上传 → {@link #insertIfAbsent}：三件套已存在就不插（原子判重，服务层据此拒绝上传）；</li>
 *   <li>重新解读 → {@link #insertNewBatch}：三件套相同也允许新建 analysis_id。</li>
 * </ul>
 *
 * @author <你的名字>
 */
public interface DriveAnalysisMapper {

    /**
     * 上传建批次：三件套（batch + productBio + subbarcode）已存在则不插入。
     *
     * @param row analysisBatchDate / analysisDate / subbarcode / productBio / productId / driveStatus /
     *            operatorName / createBy
     * @return 1 = 已插入；0 = 该批次已驱动过
     */
    int insertIfAbsent(@Param("row") Map<String, Object> row);

    /**
     * 重新解读建批次：不判重，直接插入。
     *
     * @param row 同上
     * @return 影响行数
     */
    int insertNewBatch(@Param("row") Map<String, Object> row);

    /**
     * 取该三件套下最新的批次 ID。
     *
     * @param analysisBatchDate 批次目录名
     * @param productBio        产品编码
     * @param subbarcode        样本编号
     * @return analysis_id，没有则 null
     */
    Long selectLatestAnalysisId(@Param("analysisBatchDate") String analysisBatchDate,
                                @Param("productBio") String productBio,
                                @Param("subbarcode") String subbarcode);

    /**
     * 按 analysis_id 取批次归属信息（单文件重驱时拼上下文用）。
     *
     * @param analysisId 分析批次 ID
     * @return analysisBatchDate / analysisDate / productBio / subbarcode；不存在返回 null
     */
    Map<String, Object> selectBatch(@Param("analysisId") Long analysisId);

    /**
     * 校验批次存在。
     *
     * @param analysisId 分析批次 ID
     * @return 1 = 存在
     */
    int countById(@Param("analysisId") Long analysisId);

    /**
     * 更新驱动状态（驱动中）。
     *
     * @param analysisId  分析批次 ID
     * @param driveStatus 10/11/12
     * @param updateBy    更新人
     * @return 影响行数
     */
    int updateDriveStatus(@Param("analysisId") Long analysisId,
                          @Param("driveStatus") int driveStatus,
                          @Param("updateBy") Long updateBy);

    /**
     * 记录驱动执行时间、执行人与最终状态。
     *
     * @param analysisId  分析批次 ID
     * @param executedBy  执行人姓名
     * @param driveStatus 驱动的最终状态
     * @param updateBy    更新人
     * @return 影响行数
     */
    int finishDrive(@Param("analysisId") Long analysisId,
                    @Param("executedBy") String executedBy,
                    @Param("driveStatus") int driveStatus,
                    @Param("updateBy") Long updateBy);
}
