package org.dromara.backup.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.backup.domain.bo.BackupSampleQueryBo;
import org.dromara.backup.domain.vo.BackupSampleFileVo;
import org.dromara.backup.domain.vo.BackupSampleRowVo;
import org.dromara.backup.mapper.BackupSampleFileMapper;
import org.dromara.backup.service.IBackupSampleService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 样本备份查询实现
 * <p>
 * 只读：数据由宿主机脚本上报，页面不写库、不触发备份。
 *
 * @author liushangzhi
 */
@RequiredArgsConstructor
@Service
public class BackupSampleServiceImpl implements IBackupSampleService {

    private final BackupSampleFileMapper sampleMapper;

    @Override
    public TableDataInfo<BackupSampleRowVo> selectSamplePage(BackupSampleQueryBo bo, PageQuery pageQuery) {
        Page<BackupSampleRowVo> page = sampleMapper.selectSamplePage(pageQuery.build(), bo);
        return TableDataInfo.build(page);
    }

    @Override
    public List<BackupSampleFileVo> selectFiles(Long sampleId) {
        return sampleMapper.selectFilesBySampleId(sampleId);
    }

    @Override
    public List<String> productOptions() {
        return sampleMapper.selectProductOptions();
    }
}
