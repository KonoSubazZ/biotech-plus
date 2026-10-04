package org.dromara.compliance.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.compliance.domain.bo.ValidationRecordBo;
import org.dromara.compliance.domain.vo.ValidationRecordVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 3Q 验证记录 业务层
 * <p>
 * append-only：不提供删除方法（合规上验证记录不可删改，只能新增/更正）。
 *
 * @author liushangzhi
 */
public interface IValidationRecordService {

    /**
     * 分页查询验证记录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<ValidationRecordVo> selectPageList(ValidationRecordBo bo, PageQuery pageQuery);

    /**
     * 按主键查询详情
     *
     * @param id 主键
     * @return 详情；不存在时抛 ServiceException
     */
    ValidationRecordVo queryById(Long id);

    /**
     * 新增
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean insertByBo(ValidationRecordBo bo);

    /**
     * 修改
     *
     * @param bo 业务对象
     * @return 是否成功
     */
    Boolean updateByBo(ValidationRecordBo bo);

    /**
     * 保存上传的「验证文档」附件
     * <p>
     * 附件落在服务器固定目录（配置 compliance.validation-record.upload-dir），**同名文件直接覆盖**；
     * 库里只记原始文件名，不存路径。
     *
     * @param file 上传文件
     * @return 保存后的文件名（原文件名）
     */
    String uploadFile(MultipartFile file);

    /**
     * 下载某条记录关联的「验证文档」
     *
     * @param id       记录主键
     * @param response 响应，直接写入文件流
     * @throws IOException 写响应流失败
     */
    void downloadFile(Long id, HttpServletResponse response) throws IOException;
}
