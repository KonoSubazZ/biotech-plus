package org.dromara.backup.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.backup.domain.bo.BackupDbQueryBo;
import org.dromara.backup.domain.vo.BackupDbBinlogVo;
import org.dromara.backup.domain.vo.BackupDbFullVo;
import org.dromara.backup.domain.vo.BackupDbSummaryVo;
import org.dromara.backup.service.IBackupDbService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据库备份查询（页面只读）
 * <p>
 * 页面不写入、不触发备份：策略在宿主机 backup.yml，执行过程写日志文件。
 *
 * @author liushangzhi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/backup/db")
public class BackupDbController extends BaseController {

    private final IBackupDbService backupDbService;

    /**
     * 全量备份列表
     *
     * @param bo        库名 / 日期区间
     * @param pageQuery 分页
     * @return 分页列表
     */
    @SaCheckPermission("backup:db:list")
    @GetMapping("/full/list")
    public TableDataInfo<BackupDbFullVo> fullList(BackupDbQueryBo bo, PageQuery pageQuery) {
        return backupDbService.selectFullPage(bo, pageQuery);
    }

    /**
     * binlog 增量列表（每条带现算的 chainState）
     *
     * @param bo        日期区间
     * @param pageQuery 分页
     * @return 分页列表
     */
    @SaCheckPermission("backup:db:list")
    @GetMapping("/binlog/list")
    public TableDataInfo<BackupDbBinlogVo> binlogList(BackupDbQueryBo bo, PageQuery pageQuery) {
        return backupDbService.selectBinlogPage(bo, pageQuery);
    }

    /**
     * 汇总（四张卡片 + 页面头部）
     *
     * @return 汇总数据
     */
    @SaCheckPermission("backup:db:query")
    @GetMapping("/summary")
    public R<BackupDbSummaryVo> summary() {
        return R.ok(backupDbService.summary());
    }
}
