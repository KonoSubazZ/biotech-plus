package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.report.domain.ReportMailConfig;
import org.dromara.report.domain.bo.ReportMailConfigBo;
import org.dromara.report.domain.bo.ReportMailConfigQueryBo;
import org.dromara.report.domain.vo.ReportMailConfigVo;
import org.dromara.report.domain.vo.ReportMailSenderOptionVo;
import org.dromara.report.mapper.ReportMailConfigMapper;
import org.dromara.report.service.IReportMailConfigService;
import org.dromara.report.utils.ReportMailDefaults;
import org.dromara.report.utils.ReportNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告邮件配置 服务实现
 * <p>
 * 一个租户多条发件邮箱，其中最多一条「默认」；三条口径写在这里（页面与发信都只有这一处）：
 * <ul>
 *   <li><b>空值兜底</b>：落库前用 {@link ReportMailDefaults} 把空列补成具体值，读取/发信都不用再判空；</li>
 *   <li><b>密码只进不出</b>：列表/详情只回 passwordSet，保存时密码为空串表示「不修改」；</li>
 *   <li><b>默认唯一</b>：新建第一条自动为默认；设默认会清掉别人的默认；删/取消后若一条默认都没有，
 *       自动把最早的一条补成默认（{@link #ensureDefault()}）。</li>
 * </ul>
 * 租户条件由租户拦截器自动附加（表未进共享白名单），所以这里查到的永远是本租户的配置。
 *
 * @author <你的名字>
 */
@Service
@RequiredArgsConstructor
public class ReportMailConfigServiceImpl implements IReportMailConfigService {

    private static final String YES = "1";

    private static final String NO = "0";

    private final ReportMailConfigMapper baseMapper;

    @Override
    public TableDataInfo<ReportMailConfigVo> selectPageList(ReportMailConfigQueryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReportMailConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(bo.getFromEmail()), ReportMailConfig::getFromEmail, bo.getFromEmail());
        orderByDefaultFirst(wrapper);
        Page<ReportMailConfigVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        fillPasswordSet(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public ReportMailConfigVo queryById(Long id) {
        ReportMailConfigVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("邮件配置不存在或已删除：id=" + id, 400);
        }
        ReportMailConfig entity = baseMapper.selectById(id);
        vo.setPasswordSet(entity != null && StringUtils.isNotBlank(entity.getPassword()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(ReportMailConfigBo bo) {
        // 标题/正文是 {{变量}} 模板：保存时就校验用法与变量白名单（与报告命名同一套校验，报错信息里带可用变量）
        ReportNameResolver.validate(bo.getDefaultSubject());
        ReportNameResolver.validate(bo.getDefaultContent());

        ReportMailConfig existing = bo.getId() == null ? null : baseMapper.selectById(bo.getId());
        if (bo.getId() != null && existing == null) {
            throw new ServiceException("邮件配置不存在或已删除：id=" + bo.getId(), 400);
        }
        ReportMailConfig entity = existing == null ? new ReportMailConfig() : existing;
        applyForm(entity, bo, existing);
        if (existing == null) {
            baseMapper.insert(entity);
        } else {
            baseMapper.updateById(entity);
        }
        ensureDefault();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        baseMapper.deleteById(id);
        ensureDefault();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeDefault(Long id) {
        if (baseMapper.selectById(id) == null) {
            throw new ServiceException("邮件配置不存在或已删除：id=" + id, 400);
        }
        clearDefault();
        baseMapper.update(null, Wrappers.<ReportMailConfig>lambdaUpdate()
            .eq(ReportMailConfig::getId, id)
            .set(ReportMailConfig::getIsDefault, YES));
    }

    @Override
    public ReportMailConfig loadForSend(Long configId) {
        ReportMailConfig config = configId == null ? firstByDefault() : baseMapper.selectById(configId);
        if (config == null) {
            throw new ServiceException(configId == null
                ? "邮件发送未配置，请先到「报告管理 › 邮件配置」新增一条发件邮箱"
                : "发件邮箱不存在或已删除：id=" + configId, 400);
        }
        if (StringUtils.isBlank(config.getFromEmail())) {
            throw new ServiceException("发件邮箱未填写完整，请到「报告管理 › 邮件配置」补填", 400);
        }
        if (StringUtils.isBlank(config.getPassword())) {
            throw new ServiceException("发件邮箱「" + config.getFromEmail() + "」未配置密码，请到「邮件配置」补填", 400);
        }
        applyDefaults(config);
        return config;
    }

    @Override
    public List<ReportMailSenderOptionVo> senderOptions() {
        LambdaQueryWrapper<ReportMailConfig> wrapper = Wrappers.lambdaQuery();
        orderByDefaultFirst(wrapper);
        return baseMapper.selectList(wrapper).stream().map(this::toOption).toList();
    }

    /** 列表：一次查回本页各行的密码是否存在，避免逐行查库 */
    private void fillPasswordSet(List<ReportMailConfigVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> ids = rows.stream().map(ReportMailConfigVo::getId).toList();
        List<ReportMailConfig> entities = baseMapper.selectList(Wrappers.<ReportMailConfig>lambdaQuery()
            .select(ReportMailConfig::getId, ReportMailConfig::getPassword)
            .in(ReportMailConfig::getId, ids));
        Map<Long, Boolean> hasPassword = new HashMap<>();
        entities.forEach(entity -> hasPassword.put(entity.getId(), StringUtils.isNotBlank(entity.getPassword())));
        rows.forEach(row -> row.setPasswordSet(hasPassword.getOrDefault(row.getId(), false)));
    }

    /** 表单 → 实体（同时把空列补成代码默认值，保证库里存的就是最终生效值） */
    private void applyForm(ReportMailConfig entity, ReportMailConfigBo bo, ReportMailConfig existing) {
        entity.setEnabled(textOr(bo.getEnabled(), NO));
        entity.setSmtpHost(textOr(bo.getSmtpHost(), ReportMailDefaults.SMTP_HOST));
        entity.setSmtpPort(bo.getSmtpPort() == null ? ReportMailDefaults.SMTP_PORT : bo.getSmtpPort());
        entity.setSslEnable(textOr(bo.getSslEnable(), ReportMailDefaults.SSL_ENABLE));
        entity.setFromEmail(StringUtils.trim(bo.getFromEmail()));
        entity.setFromName(textOr(bo.getFromName(), ReportMailDefaults.FROM_NAME));
        entity.setDefaultSubject(textOr(bo.getDefaultSubject(), ReportMailDefaults.SUBJECT));
        entity.setDefaultContent(textOr(bo.getDefaultContent(), ReportMailDefaults.CONTENT));
        // 密码为空串 = 不修改；新增时也允许为空（发信会被 loadForSend 拦下并提示去填）
        entity.setPassword(existing == null || StringUtils.isNotBlank(bo.getPassword())
            ? bo.getPassword() : existing.getPassword());
        entity.setIsDefault(YES.equals(bo.getIsDefault()) ? YES : NO);
    }

    /** 恒定不变式：只要还有配置，就保证有且只有一条默认（默认条不存在时把最早那条顶上） */
    private void ensureDefault() {
        Long count = baseMapper.selectCount(Wrappers.<ReportMailConfig>lambdaQuery());
        if (count == null || count == 0) {
            return;
        }
        Long defaultCount = baseMapper.selectCount(Wrappers.<ReportMailConfig>lambdaQuery()
            .eq(ReportMailConfig::getIsDefault, YES));
        if (defaultCount != null && defaultCount > 0) {
            return;
        }
        ReportMailConfig earliest = baseMapper.selectOne(
            Wrappers.<ReportMailConfig>lambdaQuery().orderByAsc(ReportMailConfig::getId), false);
        if (earliest != null) {
            baseMapper.update(null, Wrappers.<ReportMailConfig>lambdaUpdate()
                .eq(ReportMailConfig::getId, earliest.getId())
                .set(ReportMailConfig::getIsDefault, YES));
        }
    }

    /** 展示顺序：默认排最前，其余按 id 升序（分页稳定） */
    private void orderByDefaultFirst(LambdaQueryWrapper<ReportMailConfig> wrapper) {
        wrapper.orderByDesc(ReportMailConfig::getIsDefault);
        wrapper.orderByAsc(ReportMailConfig::getId);
    }

    /** 清掉本租户所有「默认」标记（设默认前调用） */
    private void clearDefault() {
        baseMapper.update(null, Wrappers.<ReportMailConfig>lambdaUpdate()
            .eq(ReportMailConfig::getIsDefault, YES)
            .set(ReportMailConfig::getIsDefault, NO));
    }

    /** 默认那条：优先 is_default=1，缺失时退回最早一条（不抛异常，由调用方判断空） */
    private ReportMailConfig firstByDefault() {
        ReportMailConfig config = baseMapper.selectOne(Wrappers.<ReportMailConfig>lambdaQuery()
            .eq(ReportMailConfig::getIsDefault, YES)
            .orderByAsc(ReportMailConfig::getId), false);
        if (config != null) {
            return config;
        }
        return baseMapper.selectOne(
            Wrappers.<ReportMailConfig>lambdaQuery().orderByAsc(ReportMailConfig::getId), false);
    }

    /** 发信前把可空的展示/连接字段补成代码默认值（与页面展示同一口径） */
    private void applyDefaults(ReportMailConfig config) {
        config.setSmtpHost(textOr(config.getSmtpHost(), ReportMailDefaults.SMTP_HOST));
        config.setSmtpPort(config.getSmtpPort() == null ? ReportMailDefaults.SMTP_PORT : config.getSmtpPort());
        config.setSslEnable(textOr(config.getSslEnable(), ReportMailDefaults.SSL_ENABLE));
        config.setFromName(textOr(config.getFromName(), ReportMailDefaults.FROM_NAME));
        config.setDefaultSubject(textOr(config.getDefaultSubject(), ReportMailDefaults.SUBJECT));
        config.setDefaultContent(textOr(config.getDefaultContent(), ReportMailDefaults.CONTENT));
    }

    private ReportMailSenderOptionVo toOption(ReportMailConfig config) {
        ReportMailSenderOptionVo option = new ReportMailSenderOptionVo();
        option.setConfigId(config.getId());
        option.setFromEmail(config.getFromEmail());
        option.setFromName(StringUtils.isBlank(config.getFromName())
            ? ReportMailDefaults.FROM_NAME : config.getFromName());
        option.setIsDefault(config.getIsDefault());
        option.setEnabled(textOr(config.getEnabled(), NO));
        return option;
    }

    /** 空串 / null 都当「没配」，返回默认值 */
    private static String textOr(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }
}
