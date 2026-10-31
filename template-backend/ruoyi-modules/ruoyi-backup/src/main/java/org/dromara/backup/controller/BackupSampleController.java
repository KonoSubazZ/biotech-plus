package org.dromara.backup.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.backup.domain.bo.BackupSampleQueryBo;
import org.dromara.backup.domain.vo.BackupSampleFileVo;
import org.dromara.backup.domain.vo.BackupSampleRowVo;
import org.dromara.backup.service.IBackupSampleService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 样本备份查询（页面只读）
 * <p>
 * 一个样本一行（按 sample_id 聚合），点开看文件明细。没有写接口。
 *
 * @author liushangzhi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/backup/sample")
public class BackupSampleController extends BaseController {

    private final IBackupSampleService backupSampleService;

    /**
     * 样本备份列表（样本维度聚合）
     *
     * @param bo        样本编号/项目/订单/产品/状态/日期
     * @param pageQuery 分页
     * @return 分页列表
     */
    @SaCheckPermission("backup:sample:list")
    @GetMapping("/list")
    public TableDataInfo<BackupSampleRowVo> list(BackupSampleQueryBo bo, PageQuery pageQuery) {
        return backupSampleService.selectSamplePage(bo, pageQuery);
    }

    /**
     * 某个样本的文件明细
     *
     * @param sampleId sample_file.id
     * @return 文件列表
     */
    @SaCheckPermission("backup:sample:query")
    @GetMapping("/{sampleId}/files")
    public R<List<BackupSampleFileVo>> files(@PathVariable Long sampleId) {
        return R.ok(backupSampleService.selectFiles(sampleId));
    }

    /**
     * 产品下拉选项（已有数据里出现过的产品编码）
     *
     * @return 产品编码列表
     */
    @SaCheckPermission("backup:sample:query")
    @GetMapping("/product/options")
    public R<List<String>> productOptions() {
        return R.ok(backupSampleService.productOptions());
    }
}
