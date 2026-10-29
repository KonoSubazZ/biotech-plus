package org.dromara.backup.mapper;

import org.dromara.backup.domain.BackupDbFull;
import org.dromara.backup.domain.vo.BackupDbFullVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 全量备份记录 数据层
 * <p>
 * 唯一键即幂等键：(db_name, backup_date, backup_type)。脚本补跑 / 重跑只更新不插重复行，
 * 所以 upsert 写在 XML 里（ON DUPLICATE KEY UPDATE），其余查询走 BaseMapperPlus。
 *
 * @author liushangzhi
 */
public interface BackupDbFullMapper extends BaseMapperPlus<BackupDbFull, BackupDbFullVo> {

    /**
     * 按唯一键 upsert 一条全量记录
     *
     * @param entity 记录
     * @return 影响行数（1 = 插入，2 = 更新）
     */
    int upsert(BackupDbFull entity);
}
