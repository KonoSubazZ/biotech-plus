package org.dromara.backup.service;

import org.dromara.backup.domain.bo.BackupSampleQueryBo;
import org.dromara.backup.domain.vo.BackupSampleFileVo;
import org.dromara.backup.domain.vo.BackupSampleRowVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

/**
 * 样本备份查询（只读）
 *
 * @author liushangzhi
 */
public interface IBackupSampleService {

    /**
     * 样本维度分页（按 sample_id 聚合文件计数与状态）
     *
     * @param bo        查询条件
     * @param pageQuery 分页
     * @return 分页结果
     */
    TableDataInfo<BackupSampleRowVo> selectSamplePage(BackupSampleQueryBo bo, PageQuery pageQuery);

    /**
     * 某个样本的文件明细
     *
     * @param sampleId sample_file.id
     * @return 文件列表
     */
    List<BackupSampleFileVo> selectFiles(Long sampleId);

    /**
     * 产品下拉选项
     *
     * @return 产品编码列表
     */
    List<String> productOptions();
}
