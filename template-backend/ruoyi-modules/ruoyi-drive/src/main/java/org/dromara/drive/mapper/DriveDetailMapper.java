package org.dromara.drive.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 明细表写侧 Mapper（9 张 file_* 表）。
 * <p>
 * 表名不允许自由拼接：{@link #deleteByFileId} 的表名只能由 {@code DriveFileType.detailTable()}
 * （枚举白名单）传入；每个 insert 的列名都写死在 XML 里，解析器只提供值。
 *
 * @author <你的名字>
 */
public interface DriveDetailMapper {

    /**
     * 删除某 file_id 的旧明细（重新驱动同一文件时，旧 file_id 的明细保留，这里只用于同 file_id 复用场景）。
     *
     * @param table  明细表名（枚举白名单）
     * @param fileId 文件 ID
     * @return 删除行数
     */
    int deleteByFileId(@Param("table") String table, @Param("fileId") Long fileId);

    /**
     * 体细胞 SNV / Indel 批量插入（file_Somatic_SNV_Indel）。
     *
     * @param rows 解析行
     * @return 插入行数
     */
    int insertSomaticSnvIndel(@Param("rows") List<Map<String, Object>> rows);

    /**
     * CNV 批量插入（file_CNV）。
     *
     * @param rows 解析行
     * @return 插入行数
     */
    int insertCnv(@Param("rows") List<Map<String, Object>> rows);

    /**
     * Fusion 批量插入（file_Fusion）。
     *
     * @param rows 解析行
     * @return 插入行数
     */
    int insertFusion(@Param("rows") List<Map<String, Object>> rows);

    /**
     * CR_ALL 批量插入（file_CR_ALL）。
     *
     * @param rows 解析行
     * @return 插入行数
     */
    int insertCrAll(@Param("rows") List<Map<String, Object>> rows);

    /**
     * MSI 批量插入（file_MSI）。
     *
     * @param rows 解析行
     * @return 插入行数
     */
    int insertMsi(@Param("rows") List<Map<String, Object>> rows);

    /**
     * 样本质控插入（file_qc，整文件一条）。
     *
     * @param rows 解析行
     * @return 插入行数
     */
    int insertQc(@Param("rows") List<Map<String, Object>> rows);

    /**
     * 对照质控插入（file_qc_control，整文件一条）。
     *
     * @param rows 解析行
     * @return 插入行数
     */
    int insertQcControl(@Param("rows") List<Map<String, Object>> rows);

    /**
     * 化疗位点批量插入（file_Chem）。
     *
     * @param rows 解析行
     * @return 插入行数
     */
    int insertChem(@Param("rows") List<Map<String, Object>> rows);

    /**
     * 化疗用药原始 JSON 插入（file_chemical，整文件一条）。
     *
     * @param rows 解析行
     * @return 插入行数
     */
    int insertChemical(@Param("rows") List<Map<String, Object>> rows);
}
