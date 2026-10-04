package org.dromara.compliance.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.compliance.domain.bo.ComplianceDocBo;
import org.dromara.compliance.domain.vo.ComplianceDocVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 3Q 文档管理 业务层
 * <p>
 * 记录类：提供批量删除（软删，@TableLogic），与 append-only 的开发记录/验证记录不同。
 *
 * @author liushangzhi
 */
public interface IComplianceDocService {

    /**
     * 分页查询文档列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<ComplianceDocVo> selectPageList(ComplianceDocBo bo, PageQuery pageQuery);

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    ComplianceDocVo queryById(Long id);

    /**
     * 新增
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(ComplianceDocBo bo);

    /**
     * 修改
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(ComplianceDocBo bo);

    /**
     * 批量删除（软删）
     *
     * @param ids 主键集合
     * @return 是否成功
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 保存上传的「文档」附件
     * <p>
     * 附件落在服务器固定目录（配置 compliance.compliance-doc.upload-dir），**同名文件直接覆盖**；
     * 库里只记原始文件名，不存路径。
     *
     * @param file 上传文件
     * @return 保存后的文件名（原文件名）
     */
    String uploadFile(MultipartFile file);

    /**
     * 下载某条记录关联的「文档」
     *
     * @param id       记录主键
     * @param response 响应，直接写入文件流
     * @throws IOException 写响应流失败
     */
    void downloadFile(Long id, HttpServletResponse response) throws IOException;
}
