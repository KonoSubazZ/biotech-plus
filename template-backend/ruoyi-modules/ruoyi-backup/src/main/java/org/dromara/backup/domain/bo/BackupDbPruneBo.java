package org.dromara.backup.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

/**
 * 本地清理标记上报对象
 * <p>
 * 只标记「本地已按保留策略清理」（local_pruned=1），不删记录、不删 OSS —— 否则页面会误判成丢备份。
 *
 * @author liushangzhi
 */
@Data
public class BackupDbPruneBo {

    /** 库名 */
    @NotBlank(message = "库名不能为空")
    @Size(max = 64, message = "库名长度不能超过 64")
    private String dbName;

    /** 备份日期（yyyy-MM-dd） */
    @NotNull(message = "备份日期不能为空")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private Date backupDate;

    /** 备份类型：weekly / manual */
    private String backupType;
}
