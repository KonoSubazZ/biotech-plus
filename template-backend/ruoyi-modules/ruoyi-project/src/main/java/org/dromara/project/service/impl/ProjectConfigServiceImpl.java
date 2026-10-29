package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.ProjectConfig;
import org.dromara.project.domain.bo.ProjectConfigBo;
import org.dromara.project.domain.bo.ProjectCenterBo;
import org.dromara.project.domain.vo.CenterExcelRow;
import org.dromara.project.domain.vo.CenterImportResultVo;
import org.dromara.project.domain.vo.ProjectConfigVo;
import org.dromara.project.mapper.ProjectConfigMapper;
import org.dromara.project.service.IProjectConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目配置 业务层处理
 * <p>
 * 风格要点（对齐 template-backend/docs/backend-code-standard.md）：
 * 查询条件集中在 buildQueryWrapper 里「声明 → 基础条件 → 分支条件 → 排序」展开，
 * 不在方法体内堆链式调用；缺数据抛明确异常，不返回裸 null。
 *
 * @author levi
 */
@RequiredArgsConstructor
@Service
public class ProjectConfigServiceImpl implements IProjectConfigService {

    /** 单次导入行数上限：一个项目的研究中心是几十的量级，2000 是防御值，不是业务上限 */
    private static final int IMPORT_MAX_ROWS = 2000;

    private final ProjectConfigMapper baseMapper;

    @Override
    public TableDataInfo<ProjectConfigVo> selectPageList(ProjectConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProjectConfig> wrapper = buildQueryWrapper(bo);
        Page<ProjectConfigVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public ProjectConfigVo queryById(Long id) {
        ProjectConfigVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("项目配置不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(ProjectConfigBo bo) {
        validateProjectNoUnique(bo.getProjectNo(), null);
        ProjectConfig entity = MapstructUtils.convert(bo, ProjectConfig.class);

        // 项目行身份：parent_id=0，研究中心两列空串（唯一键里这两列一起构成「一项目一行」）
        entity.setParentId(0L);
        entity.setResearchCenterNo("");
        entity.setResearchCenterName("");

        // 唯一键 uk_project_config_project_no 不含 del_flag：同一个项目编号被软删过时，
        // 直接 insert 会撞唯一键报「系统错误」。这里恢复旧行并写入新值，
        // 用户视角就是「项目删掉之后还能重新配一遍」。
        Long deletedId = baseMapper.selectDeletedIdByProjectNo(bo.getProjectNo());
        if (deletedId != null) {
            entity.setId(deletedId);
            baseMapper.restoreById(deletedId);
            return baseMapper.updateById(entity) > 0;
        }
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(ProjectConfigBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改项目配置必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        ProjectConfigVo existing = queryById(bo.getId());
        assertProjectNoUnchanged(bo.getProjectNo(), existing.getProjectNo());
        ProjectConfig entity = MapstructUtils.convert(bo, ProjectConfig.class);
        // 项目编号建档后不可改：置 null 让 MyBatis-Plus 跳过该列（不写库、也不参与唯一键冲突）
        entity.setProjectNo(null);
        // 项目行身份不可通过项目编辑接口改变：parent_id 置 null 跳过；
        // 研究中心两列必须写空串而不是 null —— 它们是 NOT NULL 且实体上是 @TableField(updateStrategy = ALWAYS)，
        // 置 null 会真的把 NULL 写进库，直接报「Column 'research_center_no' cannot be null」。
        entity.setParentId(null);
        entity.setResearchCenterNo("");
        entity.setResearchCenterName("");
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的项目配置");
        }
        // 删项目连带软删它下面的研究中心，避免留下无主的中心行
        baseMapper.delete(Wrappers.<ProjectConfig>lambdaQuery().in(ProjectConfig::getParentId, ids));
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    @Override
    public List<ProjectConfigVo> selectCentersByParentId(Long parentId) {
        if (parentId == null) {
            throw new ServiceException("缺少项目主键");
        }
        LambdaQueryWrapper<ProjectConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectConfig::getParentId, parentId);
        wrapper.orderByAsc(ProjectConfig::getId);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertCenterByBo(ProjectCenterBo bo) {
        if (bo.getParentId() == null) {
            throw new ServiceException("新增研究中心必须指定所属项目");
        }
        ProjectConfigVo parent = queryById(bo.getParentId());
        if (parent.getParentId() != null && parent.getParentId() != 0L) {
            throw new ServiceException("只能给项目挂研究中心，不能挂在研究中心下");
        }
        String centerNo = StringUtils.trim(bo.getResearchCenterNo());
        String centerName = StringUtils.trim(bo.getResearchCenterName());
        assertResearchCenterUnique(parent.getProjectNo(), centerNo, centerName, null);

        ProjectConfig entity = MapstructUtils.convert(bo, ProjectConfig.class);
        entity.setParentId(parent.getId());
        entity.setProjectNo(parent.getProjectNo());
        // 项目级字段只活在项目行上，中心行一律置空（合同三项同理）
        entity.setProjectName("");
        entity.setCustomer("");
        entity.setContractName(null);
        entity.setContractCode(null);
        entity.setStudyPlanNo(null);
        entity.setResearchCenterNo(centerNo);
        entity.setResearchCenterName(centerName);

        // 唯一键不含 del_flag：同编号同名中心被软删过，直接 insert 会撞键报「系统错误」——
        // 与项目行同一套处理：恢复旧行再写新值。
        Long deletedId = baseMapper.selectDeletedCenterId(parent.getProjectNo(), centerNo, centerName);
        if (deletedId != null) {
            entity.setId(deletedId);
            baseMapper.restoreById(deletedId);
            return baseMapper.updateById(entity) > 0;
        }
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCenterByBo(ProjectCenterBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改研究中心必须提供主键");
        }
        ProjectConfigVo existing = queryById(bo.getId());
        if (existing.getParentId() == null || existing.getParentId() == 0L) {
            throw new ServiceException("该记录是项目行，不能走研究中心修改接口");
        }
        String centerNo = StringUtils.trim(bo.getResearchCenterNo());
        String centerName = StringUtils.trim(bo.getResearchCenterName());
        assertResearchCenterUnique(existing.getProjectNo(), centerNo, centerName, existing.getId());

        ProjectConfig entity = MapstructUtils.convert(bo, ProjectConfig.class);
        // 归属关系不可改：父节点、项目编号、项目级字段一律不写库
        entity.setParentId(null);
        entity.setProjectNo(null);
        entity.setProjectName(null);
        entity.setCustomer(null);
        entity.setContractName(null);
        entity.setContractCode(null);
        entity.setStudyPlanNo(null);
        entity.setResearchCenterNo(centerNo);
        entity.setResearchCenterName(centerName);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCentersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的研究中心");
        }
        long projectRows = baseMapper.selectCount(Wrappers.<ProjectConfig>lambdaQuery()
            .in(ProjectConfig::getId, ids)
            .eq(ProjectConfig::getParentId, 0L));
        if (projectRows > 0) {
            throw new ServiceException("所选记录里混入了项目行，请分开删除");
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    @Override
    public List<CenterExcelRow> exportCenters(Long parentId) {
        List<ProjectConfigVo> centers = selectCentersByParentId(parentId);
        List<CenterExcelRow> rows = new ArrayList<>(centers.size());
        for (ProjectConfigVo center : centers) {
            CenterExcelRow row = new CenterExcelRow();
            row.setResearchCenterNo(center.getResearchCenterNo());
            row.setResearchCenterName(center.getResearchCenterName());
            row.setReceiveEmail(center.getReceiveEmail());
            row.setRemark(center.getRemark());
            rows.add(row);
        }
        return rows;
    }

    /**
     * 批量导入研究中心：先全量校验，任一行不通过就整体不导入。
     * <p>
     * 两阶段写法是刻意的 —— 校验阶段只读库、不写库，错误一次性全部返回；
     * 只有零错误才进入写库阶段，并且整体包在一个事务里（写库阶段理论上不该失败，
     * 事务是兜底：异常时不会留下半批数据）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CenterImportResultVo importCenters(Long parentId, MultipartFile file) {
        ProjectConfigVo project = requireImportProject(parentId, file);
        ImportBatch batch = new ImportBatch(project, readCenterRows(file));
        loadExistingCenters(batch);

        // ---------- 第一阶段：全量校验（只读，不写库） ----------
        for (int i = 0; i < batch.rows.size(); i++) {
            // Excel 第 1 行是表头，所以数据行号从 2 开始
            validateCenterRow(batch.rows.get(i), i + 2, batch);
        }
        batch.result.setFailed(batch.result.getErrors().size());
        if (!batch.result.getErrors().isEmpty()) {
            // 整体不导入：committed 保持 false，inserted / updated 都是 0
            return batch.result;
        }

        // ---------- 第二阶段：写库（整体一个事务） ----------
        for (CenterExcelRow row : batch.rows) {
            writeCenterRow(batch, row);
        }
        batch.result.setCommitted(true);
        return batch.result;
    }

    /** 导入前置校验：主键、文件、以及「导入目标必须是项目行」 */
    private ProjectConfigVo requireImportProject(Long parentId, MultipartFile file) {
        if (parentId == null) {
            throw new ServiceException("缺少项目主键");
        }
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要导入的 Excel 文件");
        }
        ProjectConfigVo project = queryById(parentId);
        if (project.getParentId() != null && project.getParentId() != 0L) {
            throw new ServiceException("只能给项目导入研究中心，不能导入到研究中心下");
        }
        return project;
    }

    /** 现有中心（未删）按编号归类：同一编号下有多条，说明历史数据编号重了，导入无法按编号匹配 */
    private void loadExistingCenters(ImportBatch batch) {
        List<ProjectConfig> existing = baseMapper.selectList(Wrappers.<ProjectConfig>lambdaQuery()
            .eq(ProjectConfig::getParentId, batch.project.getId())
            .orderByAsc(ProjectConfig::getId));
        for (ProjectConfig center : existing) {
            String centerNo = StringUtils.trim(center.getResearchCenterNo());
            batch.centersByNo.computeIfAbsent(centerNo, key -> new ArrayList<>()).add(center);
        }
    }

    /** 写一行：编号能对上就改这一行，对不上就新增（同键的软删行先恢复再写值，避免撞唯一键） */
    private void writeCenterRow(ImportBatch batch, CenterExcelRow row) {
        String centerNo = StringUtils.trim(row.getResearchCenterNo());
        String centerName = StringUtils.trim(row.getResearchCenterName());
        String receiveEmail = StringUtils.trim(row.getReceiveEmail());
        String remark = StringUtils.trim(row.getRemark());

        List<ProjectConfig> matched = batch.centersByNo.get(centerNo);
        if (matched != null && matched.size() == 1) {
            // 只动名称/邮箱/备注，归属与项目级字段保持原样
            ProjectConfig center = matched.get(0);
            center.setResearchCenterName(centerName);
            center.setReceiveEmail(receiveEmail);
            center.setRemark(remark);
            baseMapper.updateById(center);
            batch.result.setUpdated(batch.result.getUpdated() + 1);
            return;
        }
        Long deletedId = baseMapper.selectDeletedCenterId(batch.project.getProjectNo(), centerNo, centerName);
        if (deletedId != null) {
            baseMapper.restoreById(deletedId);
            ProjectConfig restored = baseMapper.selectById(deletedId);
            restored.setResearchCenterName(centerName);
            restored.setReceiveEmail(receiveEmail);
            restored.setRemark(remark);
            baseMapper.updateById(restored);
            batch.result.setInserted(batch.result.getInserted() + 1);
            return;
        }
        baseMapper.insert(buildCenterEntity(batch.project, centerNo, centerName, receiveEmail, remark));
        batch.result.setInserted(batch.result.getInserted() + 1);
    }

    /**
     * 一次导入的上下文：把「项目行 + 数据行 + 现有中心索引 + 结果」打包，
     * 免得校验方法一路传 8 个参数。
     */
    private static final class ImportBatch {

        private final ProjectConfigVo project;

        private final List<CenterExcelRow> rows;

        /** 未删中心按编号归类（同一编号正常只有 1 条） */
        private final Map<String, List<ProjectConfig>> centersByNo = new LinkedHashMap<>();

        /** 编号 → 它在文件里第一次出现的 Excel 行号，用于「同文件内编号重复」提示 */
        private final Map<String, Integer> firstRowOfNo = new HashMap<>();

        private final CenterImportResultVo result = new CenterImportResultVo();

        private ImportBatch(ProjectConfigVo project, List<CenterExcelRow> rows) {
            this.project = project;
            this.rows = rows;
            this.result.setTotal(rows.size());
        }
    }

    /** 读 Excel 并去掉整行空白的行（用户在模板末尾多敲的空行不算数据） */
    private List<CenterExcelRow> readCenterRows(MultipartFile file) {
        List<CenterExcelRow> rows;
        try (InputStream inputStream = file.getInputStream()) {
            rows = ExcelUtil.importExcel(inputStream, CenterExcelRow.class);
        } catch (IOException e) {
            throw new ServiceException("Excel 读取失败：" + e.getMessage());
        } catch (RuntimeException e) {
            throw new ServiceException("Excel 解析失败：" + e.getMessage());
        }
        if (rows == null) {
            rows = new ArrayList<>();
        }
        rows.removeIf(row -> row == null
            || (StringUtils.isBlank(row.getResearchCenterNo())
            && StringUtils.isBlank(row.getResearchCenterName())
            && StringUtils.isBlank(row.getReceiveEmail())
            && StringUtils.isBlank(row.getRemark())));
        if (rows.isEmpty()) {
            throw new ServiceException("Excel 里没有数据行（第 1 行是表头，请从第 2 行开始填）");
        }
        if (rows.size() > IMPORT_MAX_ROWS) {
            throw new ServiceException("单次最多导入 " + IMPORT_MAX_ROWS + " 行，请拆成多份再导");
        }
        return rows;
    }

    /**
     * 单行校验：只往 result 里记错误，不抛异常 —— 这样一次能把所有问题行都列出来，
     * 用户一次改完再导，而不是改一条传一次。
     */
    private void validateCenterRow(CenterExcelRow row, int excelRowNo, ImportBatch batch) {
        String centerNo = StringUtils.trim(row.getResearchCenterNo());
        String centerName = StringUtils.trim(row.getResearchCenterName());
        String receiveEmail = StringUtils.trim(row.getReceiveEmail());
        String remark = StringUtils.trim(row.getRemark());

        String fieldError = checkCenterFields(excelRowNo, centerNo, centerName, receiveEmail, remark);
        if (!fieldError.isEmpty()) {
            batch.result.addError(fieldError);
            return;
        }
        Integer firstRow = batch.firstRowOfNo.putIfAbsent(centerNo, excelRowNo);
        if (firstRow != null) {
            batch.result.addError("第 " + excelRowNo + " 行：研究中心编号 " + centerNo + " 与第 " + firstRow + " 行重复");
            return;
        }
        String conflictError = checkCenterConflict(batch, centerNo, centerName);
        if (!conflictError.isEmpty()) {
            batch.result.addError("第 " + excelRowNo + " 行：" + conflictError);
        }
    }

    /** 必填与长度校验：返回第一条错误，空串 = 通过（同一行只报一条，避免刷屏） */
    private String checkCenterFields(int excelRowNo, String centerNo, String centerName,
                                     String receiveEmail, String remark) {
        String prefix = "第 " + excelRowNo + " 行：";
        if (StringUtils.isBlank(centerNo)) {
            return prefix + "研究中心编号为空";
        }
        if (centerNo.length() > 100) {
            return prefix + centerNo + " 研究中心编号超过 100 字";
        }
        if (StringUtils.isBlank(centerName)) {
            return prefix + centerNo + " 研究中心名称为空";
        }
        if (centerName.length() > 200) {
            return prefix + centerNo + " 研究中心名称超过 200 字";
        }
        if (StringUtils.isBlank(receiveEmail)) {
            return prefix + centerNo + " 报告接收邮箱为空";
        }
        if (receiveEmail.length() > 500) {
            return prefix + centerNo + " 报告接收邮箱超过 500 字";
        }
        if (remark != null && remark.length() > 500) {
            return prefix + centerNo + " 备注超过 500 字";
        }
        return "";
    }

    /** 与库里现有中心/已删中心的冲突校验（唯一键是「项目 + 编号 + 名称」成对） */
    private String checkCenterConflict(ImportBatch batch, String centerNo, String centerName) {
        List<ProjectConfig> matched = batch.centersByNo.get(centerNo);
        if (matched != null && matched.size() > 1) {
            return "编号 " + centerNo + " 在该项目下已有多条研究中心，无法按编号匹配，请先在列表里整理编号";
        }
        if (matched == null) {
            return "";
        }
        // 改名时若 (编号, 新名称) 这个组合的正是一条已删除的中心，唯一键仍被它占着 → 提前说清
        Long deletedTwinId = baseMapper.selectDeletedCenterId(batch.project.getProjectNo(), centerNo, centerName);
        if (deletedTwinId != null) {
            return "编号 " + centerNo + " / " + centerName + " 与一条已删除的中心重复，请先在列表里恢复它或换个名称";
        }
        return "";
    }

    /** 导入新增时组装中心行：项目级字段一律留空，只写中心自己的字段 */
    private ProjectConfig buildCenterEntity(ProjectConfigVo project, String centerNo, String centerName,
                                            String receiveEmail, String remark) {
        ProjectConfig entity = new ProjectConfig();
        entity.setParentId(project.getId());
        entity.setProjectNo(project.getProjectNo());
        entity.setProjectName("");
        entity.setCustomer("");
        entity.setContractName(null);
        entity.setContractCode(null);
        entity.setStudyPlanNo(null);
        entity.setResearchCenterNo(centerNo);
        entity.setResearchCenterName(centerName);
        entity.setReceiveEmail(receiveEmail);
        entity.setProjectNotifyEmail("");
        entity.setRemark(remark);
        return entity;
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 分支条件 → 排序。
     * 搜索栏只有「项目编号」「客户名称」两项，其余条件不在这里凭空发散。
     */
    private LambdaQueryWrapper<ProjectConfig> buildQueryWrapper(ProjectConfigBo bo) {
        LambdaQueryWrapper<ProjectConfig> wrapper = Wrappers.lambdaQuery();

        // 列表只列项目行（parent_id=0）；研究中心行走展开区单独接口
        wrapper.eq(ProjectConfig::getParentId, 0L);
        wrapper.like(StringUtils.isNotBlank(bo.getProjectNo()), ProjectConfig::getProjectNo, bo.getProjectNo());
        wrapper.like(StringUtils.isNotBlank(bo.getCustomer()), ProjectConfig::getCustomer, bo.getCustomer());

        wrapper.orderByDesc(ProjectConfig::getId);
        return wrapper;
    }

    /**
     * 项目编号建档后不可修改：入参给了不同值就明确拒绝，别让「改了但没生效」变成隐性行为。
     * 入参为空视为「不打算改」，直接放行。
     */
    private void assertProjectNoUnchanged(String requested, String current) {
        if (StringUtils.isBlank(requested) || StringUtils.equals(requested, current)) {
            return;
        }
        throw new ServiceException("项目编号建档后不可修改：" + current, 400);
    }

    /**
     * 校验项目编号唯一（新增时用；同一租户下一个项目只能一条配置）。
     * 排除自身，避免「改其它字段」时误判冲突。
     */
    private void validateProjectNoUnique(String projectNo, Long excludeId) {
        if (StringUtils.isBlank(projectNo)) {
            return;
        }
        LambdaQueryWrapper<ProjectConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectConfig::getProjectNo, projectNo);
        wrapper.eq(ProjectConfig::getParentId, 0L);
        wrapper.ne(excludeId != null, ProjectConfig::getId, excludeId);
        if (baseMapper.exists(wrapper)) {
            throw new ServiceException("项目编号已存在：" + projectNo, 409);
        }
    }

    /**
     * 同一项目下（研究中心编号, 研究中心名称）唯一 —— 与唯一键 uk_project_config_node 对齐，先给友好提示。
     */
    private void assertResearchCenterUnique(String projectNo, String centerNo, String centerName, Long excludeId) {
        LambdaQueryWrapper<ProjectConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ProjectConfig::getProjectNo, projectNo);
        wrapper.eq(ProjectConfig::getResearchCenterNo, centerNo);
        wrapper.eq(ProjectConfig::getResearchCenterName, centerName);
        wrapper.gt(ProjectConfig::getParentId, 0L);
        wrapper.ne(excludeId != null, ProjectConfig::getId, excludeId);
        if (baseMapper.exists(wrapper)) {
            throw new ServiceException("该项目下研究中心已存在：" + centerNo + " / " + centerName, 409);
        }
    }
}
