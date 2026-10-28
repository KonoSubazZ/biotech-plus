package org.dromara.project.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.project.domain.ProjectConfig;
import org.dromara.project.domain.vo.ProjectConfigVo;

/**
 * 项目配置 数据层
 * <p>
 * 单表增删改查、简单条件组合、排序、计数一律用继承来的 BaseMapperPlus 能力，
 * 不写自定义 SQL。只有复杂联表 / 子查询 / 聚合报表才在本接口声明方法，
 * 实现写在 resources/mapper/project/ProjectConfigMapper.xml。
 *
 * @author levi
 */
public interface ProjectConfigMapper extends BaseMapperPlus<ProjectConfig, ProjectConfigVo> {

    /**
     * 查被软删的、用了同一个项目编号的记录 id（专供「删了再配一次」恢复旧行）。
     * <p>
     * 故意不带 del_flag 过滤：MyBatis-Plus 的 @TableLogic 只作用于 Wrapper 生成的 SQL，
     * 自定义 SQL 能看见软删行，而项目编号唯一键不含 del_flag。
     *
     * @param projectNo 项目编号
     * @return 软删行 id；没有则 null
     */
    Long selectDeletedIdByProjectNo(@Param("projectNo") String projectNo);

    /**
     * 查被软删的、同项目同编号同名的研究中心行 id（专供「删了再加回来」恢复旧行）。
     * <p>
     * 与 {@link #selectDeletedIdByProjectNo} 同一套理由：唯一键不含 del_flag，软删行仍占着
     * (project_no, research_center_no, research_center_name)，直接 insert 会撞键。
     *
     * @param projectNo          项目编号
     * @param researchCenterNo   研究中心编号
     * @param researchCenterName 研究中心名称
     * @return 软删行 id；没有则 null
     */
    Long selectDeletedCenterId(@Param("projectNo") String projectNo,
                               @Param("researchCenterNo") String researchCenterNo,
                               @Param("researchCenterName") String researchCenterName);

    /**
     * 把软删行恢复成存在状态（updateById 不会带 del_flag，所以单列一条 SQL）。
     *
     * @param id 主键
     * @return 影响行数
     */
    int restoreById(@Param("id") Long id);
}
