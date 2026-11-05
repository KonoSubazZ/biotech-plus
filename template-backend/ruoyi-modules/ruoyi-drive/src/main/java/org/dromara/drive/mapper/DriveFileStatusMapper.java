package org.dromara.drive.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * analysis_file_status 写侧 Mapper。
 * <p>
 * 驱动口径（用户 2026-10-21）：
 * <ul>
 *   <li>同批次重新驱动：先把该文件当前 del_flag='0' 的行置 '1'，再插一条新的 del_flag='0'（新 file_id）；</li>
 *   <li>新批次驱动：批次下没有旧行，直接插。</li>
 * </ul>
 * 样本级状态按"该批次所有 del_flag='0' 的文件都 Loaded"重算（与旧 Python 驱动一致）。
 *
 * @author <你的名字>
 */
public interface DriveFileStatusMapper {

    /**
     * 插入一条文件驱动记录（status=Pending，file_id 自增）。
     *
     * @param row 列名见 XML
     * @return 影响行数
     */
    int insertFile(@Param("row") Map<String, Object> row);

    /**
     * 取刚插入的那条记录 ID（按 analysis_id + file_name 取最新一条未删除行）。
     *
     * @param analysisId 分析批次 ID
     * @param fileName   文件名
     * @return file_id，没有则 null
     */
    Long selectLatestActiveFileId(@Param("analysisId") Long analysisId,
                                  @Param("fileName") String fileName);

    /**
     * 取该文件当前的未删除行（重新驱动前用于校验归属与取路径）。
     *
     * @param analysisId 分析批次 ID
     * @param fileId     文件 ID
     * @return 行（file_id/file_path/file_name/file_type/status）
     */
    Map<String, Object> selectActiveFile(@Param("analysisId") Long analysisId,
                                         @Param("fileId") Long fileId);

    /**
     * 同批次重新驱动：把该文件当前未删除的行置为已删除（保留历史）。
     *
     * @param analysisId 分析批次 ID
     * @param fileName   文件名
     * @param updateBy   更新人
     * @return 影响行数
     */
    int deactivateActiveFile(@Param("analysisId") Long analysisId,
                             @Param("fileName") String fileName,
                             @Param("updateBy") Long updateBy);

    /**
     * 标记文件驱动成功。
     *
     * @param fileId   文件 ID
     * @param mutNum   明细行数
     * @param fileText 文件内容（可截断）
     * @param updateBy 更新人
     * @return 影响行数
     */
    int markLoaded(@Param("fileId") Long fileId,
                   @Param("mutNum") Integer mutNum,
                   @Param("fileText") String fileText,
                   @Param("updateBy") Long updateBy);

    /**
     * 标记文件驱动失败。
     *
     * @param fileId   文件 ID
     * @param message  失败原因
     * @param updateBy 更新人
     * @return 影响行数
     */
    int markError(@Param("fileId") Long fileId,
                  @Param("message") String message,
                  @Param("updateBy") Long updateBy);

    /**
     * 该批次下所有未删除文件的当前状态。
     *
     * @param analysisId 分析批次 ID
     * @return 每行含 fileId / fileName / fileType / status / mutNum / message
     */
    List<Map<String, Object>> selectActiveFiles(@Param("analysisId") Long analysisId);

    /**
     * 该批次下未删除文件总数。
     *
     * @param analysisId 分析批次 ID
     * @return 文件数
     */
    int countActiveFiles(@Param("analysisId") Long analysisId);

    /**
     * 该批次下未删除且已加载的文件数。
     *
     * @param analysisId 分析批次 ID
     * @return 文件数
     */
    int countLoadedFiles(@Param("analysisId") Long analysisId);
}
