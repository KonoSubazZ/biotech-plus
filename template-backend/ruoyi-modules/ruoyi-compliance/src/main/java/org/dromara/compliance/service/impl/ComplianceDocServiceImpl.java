package org.dromara.compliance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.compliance.domain.ComplianceDoc;
import org.dromara.compliance.domain.bo.ComplianceDocBo;
import org.dromara.compliance.domain.vo.ComplianceDocVo;
import org.dromara.compliance.mapper.ComplianceDocMapper;
import org.dromara.compliance.service.IComplianceDocService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 3Q 文档管理 业务层处理
 * <p>
 * 风格要点（对齐 template-backend/docs/backend-code-standard.md）：
 * 查询条件集中在 buildQueryWrapper 里「声明 → 基础条件 → 分支条件 → 排序」展开，
 * 不在方法体内堆链式调用；缺数据抛明确异常，不返回裸 null。
 * <p>
 * 记录类：删除是软删（实体 @TableLogic），**不联动物理文件** ——
 * 附件按文件名存在同一个固定目录里，别的记录可能引用同名文件，删记录时不能连带删盘上文件。
 *
 * @author liushangzhi
 */
@RequiredArgsConstructor
@Service
public class ComplianceDocServiceImpl implements IComplianceDocService {

    /** 「文档」附件存放目录（服务器固定目录，见 application.yml 的 compliance.compliance-doc.upload-dir） */
    @Value("${compliance.compliance-doc.upload-dir}")
    private String uploadDir;

    private final ComplianceDocMapper baseMapper;

    @Override
    public TableDataInfo<ComplianceDocVo> selectPageList(ComplianceDocBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ComplianceDoc> wrapper = buildQueryWrapper(bo);
        Page<ComplianceDocVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public ComplianceDocVo queryById(Long id) {
        ComplianceDocVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("文档不存在：id=" + id);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(ComplianceDocBo bo) {
        ComplianceDoc entity = MapstructUtils.convert(bo, ComplianceDoc.class);
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(ComplianceDocBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("修改文档必须提供主键");
        }
        // 先确认记录存在，避免把「改不到」当成「改成功」
        queryById(bo.getId());
        ComplianceDoc entity = MapstructUtils.convert(bo, ComplianceDoc.class);
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的文档");
        }
        baseMapper.deleteByIds(ids);
        return Boolean.TRUE;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String fileName = resolveSafeFileName(file.getOriginalFilename());
        Path targetDir = uploadDirPath();
        try {
            Files.createDirectories(targetDir);
            // 直接按原文件名落盘：同名文件被覆盖 = 「重名替换」
            file.transferTo(targetDir.resolve(fileName));
        } catch (IOException e) {
            throw new ServiceException("文档保存失败：" + fileName + "（" + e.getMessage() + "）");
        }
        return fileName;
    }

    @Override
    public void downloadFile(Long id, HttpServletResponse response) throws IOException {
        ComplianceDocVo vo = queryById(id);
        String fileName = vo.getFileName();
        if (StringUtils.isBlank(fileName)) {
            throw new ServiceException("该记录没有上传文档");
        }
        Path targetDir = uploadDirPath();
        Path target = targetDir.resolve(fileName).normalize();
        // 兜底防路径穿越：必须仍在附件目录内
        if (!target.startsWith(targetDir) || !Files.isRegularFile(target)) {
            throw new ServiceException("文档不存在：" + fileName);
        }
        FileUtils.setAttachmentResponseHeader(response, fileName);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
        Files.copy(target, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 组装查询条件：声明 Wrapper → 基础条件 → 分支条件 → 排序。
     */
    private LambdaQueryWrapper<ComplianceDoc> buildQueryWrapper(ComplianceDocBo bo) {
        LambdaQueryWrapper<ComplianceDoc> wrapper = Wrappers.lambdaQuery();

        wrapper.like(StringUtils.isNotBlank(bo.getTitle()), ComplianceDoc::getTitle, bo.getTitle());
        wrapper.like(StringUtils.isNotBlank(bo.getVersion()), ComplianceDoc::getVersion, bo.getVersion());

        if (StringUtils.isNotBlank(bo.getDocType())) {
            wrapper.eq(ComplianceDoc::getDocType, bo.getDocType());
        }

        wrapper.orderByAsc(ComplianceDoc::getDocType);
        wrapper.orderByDesc(ComplianceDoc::getId);
        return wrapper;
    }

    /**
     * 附件目录的绝对规范路径。
     */
    private Path uploadDirPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 从原始文件名里取安全的名字：只保留最后一段，挡掉路径穿越、空名与目录占位名。
     */
    private String resolveSafeFileName(String originalName) {
        if (StringUtils.isBlank(originalName)) {
            throw new ServiceException("上传文件缺少文件名");
        }
        String normalized = originalName.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        String fileName = (slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized).trim();
        if (StringUtils.isBlank(fileName) || ".".equals(fileName) || "..".equals(fileName)) {
            throw new ServiceException("文件名不合法：" + originalName);
        }
        return fileName;
    }
}
