package org.dromara.report.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.ReportMailConfig;
import org.dromara.report.domain.bo.ReportMailConfigBo;
import org.dromara.report.domain.bo.ReportMailConfigQueryBo;
import org.dromara.report.domain.vo.ReportMailConfigVo;
import org.dromara.report.domain.vo.ReportMailSenderOptionVo;

import java.util.List;
import org.dromara.report.utils.ReportMailDefaults;

/**
 * 报告邮件配置 服务层
 * <p>
 * 一个租户可以配**多条**发件邮箱（如不同业务线各一个），其中最多一条是「默认邮箱」，
 * 报告发送页默认选中它、也可以改成别的（见 {@link #senderOptions()}）。
 * 恒定不变式：只要有配置，就一定有且只有一条默认（新建第一条自动成为默认；
 * 删掉默认/取消默认后会自动把最早的一条补成默认）。
 *
 * @author <你的名字>
 */
public interface IReportMailConfigService {

    /**
     * 分页查询配置列表（password 不回显，只给 passwordSet）。
     *
     * @param bo        查询条件（发件邮箱模糊）
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<ReportMailConfigVo> selectPageList(ReportMailConfigQueryBo bo, PageQuery pageQuery);

    /**
     * 按 id 查一条（编辑回显用；password 不回显）。
     *
     * @param id 配置ID
     * @return 配置
     */
    ReportMailConfigVo queryById(Long id);

    /**
     * 新增 / 修改一条配置（password 空串 = 不修改已保存的值）。
     *
     * @param bo 请求对象
     */
    void save(ReportMailConfigBo bo);

    /**
     * 删除一条配置（逻辑删；删掉的若是默认，会自动把最早的一条补成默认）。
     *
     * @param id 配置ID
     */
    void remove(Long id);

    /**
     * 把某条配置设为「默认邮箱」（同租户其它行会被清成非默认）。
     *
     * @param id 配置ID
     */
    void changeDefault(Long id);

    /**
     * 发信时取配置实体（**含密码**）：传 id 用指定那条，不传用「默认邮箱」。
     * <p>
     * 未配置 / 未填账号密码都会抛业务异常（400），由页面直接提示。
     *
     * @param configId 配置ID，可空
     * @return 配置实体（空列已用 {@link ReportMailDefaults} 补齐）
     */
    ReportMailConfig loadForSend(Long configId);

    /**
     * 发件邮箱候选（报告发送页下拉用；默认那条排最前，不含密码/SMTP）。
     *
     * @return 候选列表
     */
    List<ReportMailSenderOptionVo> senderOptions();
}
