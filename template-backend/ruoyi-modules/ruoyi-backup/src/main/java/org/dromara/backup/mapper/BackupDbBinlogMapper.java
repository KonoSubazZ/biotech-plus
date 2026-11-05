package org.dromara.backup.mapper;

import org.dromara.backup.domain.BackupDbBinlog;
import org.dromara.backup.domain.vo.BackupDbBinlogVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * binlog 增量归档 数据层
 * <p>
 * 唯一键 (instance, backup_date)：同一天只允许一条，重跑覆盖。
 *
 * @author liushangzhi
 */
public interface BackupDbBinlogMapper extends BaseMapperPlus<BackupDbBinlog, BackupDbBinlogVo> {

    /**
     * 按唯一键 upsert 一条增量记录
     *
     * @param entity 记录
     * @return 影响行数（1 = 插入，2 = 更新）
     */
    int upsert(BackupDbBinlog entity);
}
