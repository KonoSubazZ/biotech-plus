package org.dromara.backup.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.backup.domain.BackupSampleFile;
import org.dromara.backup.domain.bo.BackupSampleQueryBo;
import org.dromara.backup.domain.vo.BackupSampleFileVo;
import org.dromara.backup.domain.vo.BackupSampleRowVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 样本备份明细 数据层
 * <p>
 * 表里只有文件级事实，列表页的「样本一行」是 GROUP BY sample_id 聚合出来的，
 * 所以两条查询写在 XML 里；其余走 BaseMapperPlus。
 *
 * @author liushangzhi
 */
public interface BackupSampleFileMapper extends BaseMapperPlus<BackupSampleFile, BackupSampleFileVo> {

    /**
     * 样本维度分页（按 sample_id 聚合文件计数/字节/状态）
     *
     * @param page 分页
     * @param bo   查询条件
     * @return 分页结果
     */
    Page<BackupSampleRowVo> selectSamplePage(@Param("page") Page<BackupSampleRowVo> page,
                                             @Param("bo") BackupSampleQueryBo bo);

    /**
     * 某个样本的文件明细
     *
     * @param sampleId sample_file.id
     * @return 文件列表
     */
    List<BackupSampleFileVo> selectFilesBySampleId(@Param("sampleId") Long sampleId);

    /**
     * 产品下拉选项（已有数据里出现过的 product_bio）
     *
     * @return 产品编码列表
     */
    List<String> selectProductOptions();
}
