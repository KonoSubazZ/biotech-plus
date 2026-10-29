package org.dromara.backup.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.backup.domain.bo.BackupDbBinlogBo;
import org.dromara.backup.domain.bo.BackupDbFullBo;
import org.dromara.backup.domain.bo.BackupDbPruneBo;
import org.dromara.backup.service.IBackupDbService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据库备份上报（脚本专用）
 * <p>
 * 调用方是宿主上的 backupctl（Python），带 X-Api-Token 以 sys_backup 身份执行，
 * 角色只授 backup:db:report 一个点位。没有「触发备份 / 改策略 / 删记录」接口。
 *
 * @author liushangzhi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/backup/db")
public class BackupDbReportController extends BaseController {

    private final IBackupDbService backupDbService;

    /**
     * 上报一份全量备份（按库 + 日期 + 类型幂等，重跑覆盖）
     *
     * @param bo 上报内容
     * @return 结果
     */
    @SaCheckPermission("backup:db:report")
    @Log(title = "数据库备份上报-全量", businessType = BusinessType.INSERT)
    @PostMapping("/full")
    public R<Void> reportFull(@Validated @RequestBody BackupDbFullBo bo) {
        return toAjax(backupDbService.reportFull(bo));
    }

    /**
     * 上报一份 binlog 增量归档（按实例 + 日期幂等）
     *
     * @param bo 上报内容
     * @return 结果
     */
    @SaCheckPermission("backup:db:report")
    @Log(title = "数据库备份上报-增量", businessType = BusinessType.INSERT)
    @PostMapping("/binlog")
    public R<Void> reportBinlog(@Validated @RequestBody BackupDbBinlogBo bo) {
        return toAjax(backupDbService.reportBinlog(bo));
    }

    /**
     * 标记某份全量已在本地清理（不删记录、不删 OSS）
     *
     * @param bo 库名 + 日期 + 类型
     * @return 结果
     */
    @SaCheckPermission("backup:db:report")
    @Log(title = "数据库备份上报-本地清理", businessType = BusinessType.UPDATE)
    @PostMapping("/prune")
    public R<Void> markPruned(@Validated @RequestBody BackupDbPruneBo bo) {
        return toAjax(backupDbService.markPruned(bo));
    }
}
