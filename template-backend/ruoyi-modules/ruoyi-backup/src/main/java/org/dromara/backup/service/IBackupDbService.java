package org.dromara.backup.service;

import org.dromara.backup.domain.bo.BackupDbBinlogBo;
import org.dromara.backup.domain.bo.BackupDbFullBo;
import org.dromara.backup.domain.bo.BackupDbPruneBo;
import org.dromara.backup.domain.bo.BackupDbQueryBo;
import org.dromara.backup.domain.vo.BackupDbBinlogVo;
import org.dromara.backup.domain.vo.BackupDbFullVo;
import org.dromara.backup.domain.vo.BackupDbSummaryVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * 数据库备份 业务层
 * <p>
 * 写入只有「脚本上报」三个入口（全量 / 增量 / 本地清理标记），页面全部只读；
 * 服务端不判断策略对错（策略在宿主机 backup.yml，判断由脚本的 check 子命令做）。
 *
 * @author liushangzhi
 */
public interface IBackupDbService {

    /**
     * 上报一份全量备份记录（按实例 + 日期 + 类型幂等；一组库一个包）
     *
     * @param bo 上报内容
     * @return 是否成功
     */
    Boolean reportFull(BackupDbFullBo bo);

    /**
     * 上报一份 binlog 增量归档记录（按 instance + backup_date 幂等）
     *
     * @param bo 上报内容
     * @return 是否成功
     */
    Boolean reportBinlog(BackupDbBinlogBo bo);

    /**
     * 标记某份全量已在本地按保留策略清理（不删记录、不删 OSS）
     *
     * @param bo 库名 + 日期 + 类型
     * @return 是否命中记录
     */
    Boolean markPruned(BackupDbPruneBo bo);

    /**
     * 分页查询全量记录
     *
     * @param bo        查询条件
     * @param pageQuery 分页
     * @return 分页结果
     */
    TableDataInfo<BackupDbFullVo> selectFullPage(BackupDbQueryBo bo, PageQuery pageQuery);

    /**
     * 分页查询增量记录（每条带现算的 chainState）
     *
     * @param bo        查询条件
     * @param pageQuery 分页
     * @return 分页结果
     */
    TableDataInfo<BackupDbBinlogVo> selectBinlogPage(BackupDbQueryBo bo, PageQuery pageQuery);

    /**
     * 汇总（页面卡片）
     *
     * @return 汇总数据
     */
    BackupDbSummaryVo summary();
}
