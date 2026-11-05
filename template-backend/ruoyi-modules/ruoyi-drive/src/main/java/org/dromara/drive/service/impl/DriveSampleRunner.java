package org.dromara.drive.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.drive.config.DriveProperties;
import org.dromara.drive.domain.vo.DriveFileVo;
import org.dromara.drive.domain.vo.DriveSampleVo;
import org.dromara.drive.mapper.DriveAnalysisMapper;
import org.dromara.drive.mapper.DriveDetailMapper;
import org.dromara.drive.mapper.DriveFileStatusMapper;
import org.dromara.drive.mapper.DriveProductMapper;
import org.dromara.drive.service.parser.DriveParseResult;
import org.dromara.drive.service.parser.DriveParserRegistry;
import org.dromara.drive.utils.DriveFileType;
import org.dromara.drive.utils.DriveStatus;
import org.dromara.drive.utils.DriveTexts;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个样本的驱动执行器：定位上传目录 → 建/取分析批次 → 逐文件解析落库 → 汇总样本状态。
 * <p>
 * 口径（用户 2026-10-22 重构）：
 * <ul>
 *   <li>上传目录 = {@code <root>/<batch>/<productBio>/<subbarcode>/<subbarcode>T/10.Upload/}，
 *       批次身份就是 14 位目录名，不再有 batch_no；</li>
 *   <li>UPLOAD：三件套（batch + productBio + subbarcode）已驱动过 → 拒绝（INSERT…WHERE NOT EXISTS 原子判重）；</li>
 *   <li>REINTERPRET：同三件套、直接新建 analysis_id；</li>
 *   <li>文件级失败绝不中断其它文件：单个文件的任何异常都在 {@link #processFile} 内消化成 Error 行；</li>
 *   <li>样本级状态一定收尾：异常也会 finishDrive 置 10，不会停在 12（驱动中）；</li>
 *   <li>不写 tenant_id（库默认 000000），数据访问全程 withoutTenant。</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriveSampleRunner {

    /** 明细类别 → total_variant_count 里的顺序（与旧脚本一致：snp_del+fusion+cnv+cr_all） */
    private static final String[] VARIANT_ORDER = {"snp_del", "fusion", "cnv", "cr_all"};

    private final DriveProperties properties;
    private final DriveAnalysisMapper analysisMapper;
    private final DriveFileStatusMapper fileStatusMapper;
    private final DriveDetailMapper detailMapper;
    private final DriveProductMapper productMapper;
    private final DriveParserRegistry parserRegistry;

    /**
     * 驱动一个样本。
     *
     * @param analysisBatchDate 上传批次目录名 YYYYMMDDHHMMSS
     * @param productBio        生信产品编码（= 上传目录第 2 层）
     * @param subbarcode        样本编号（上传目录第 3 层）
     * @param rejectIfDriven    true = UPLOAD（三件套已驱动过就拒）；false = REINTERPRET（新建 analysis_id）
     * @return 样本驱动结果
     */
    public DriveSampleVo run(String analysisBatchDate, String productBio, String subbarcode,
                             boolean rejectIfDriven) {
        requirePathPart(productBio, "产品编码");
        requirePathPart(subbarcode, "样本编号");
        if (analysisBatchDate == null || !analysisBatchDate.matches("\\d{14}")) {
            throw new ServiceException("批次目录必须是 14 位 YYYYMMDDHHMMSS：" + analysisBatchDate);
        }
        Long productId = productMapper.selectProductIdByBio(productBio);
        if (productId == null) {
            throw new ServiceException("产品编码在 product_config 中不存在：" + productBio);
        }

        DriveContext context = new DriveContext(analysisBatchDate, analysisBatchDate.substring(0, 8),
            productBio, subbarcode, resolveUploadDir(analysisBatchDate, productBio, subbarcode), productId);

        DriveSampleVo vo = new DriveSampleVo();
        vo.setAnalysisBatchDate(analysisBatchDate);
        vo.setAnalysisDate(context.analysisDate());
        vo.setProductBio(productBio);
        vo.setSubbarcode(subbarcode);
        if (!Files.isDirectory(context.uploadDir())) {
            return failOnMissingDirectory(vo, context);
        }

        Long analysisId = rejectIfDriven ? createBatchIfAbsent(context) : createBatch(context);
        vo.setAnalysisId(analysisId);
        analysisMapper.updateDriveStatus(analysisId, DriveStatus.DRIVING, properties.getCreateBy());

        int driveStatus = DriveStatus.FAILED;
        try {
            driveStatus = driveAllFiles(vo, context, analysisId);
        } catch (RuntimeException e) {
            // 兜底：文件循环里理论上不会抛（逐文件已 catch），这里保证样本状态一定收尾
            log.error("样本驱动异常，按失败收尾：analysis_id={}，{}/{}/{}", analysisId,
                context.analysisBatchDate(), context.productBio(), context.subbarcode(), e);
            vo.setStatus("failed");
            vo.setMessage("驱动异常：" + messageOf(e));
        } finally {
            analysisMapper.finishDrive(analysisId, properties.getOperatorName(), driveStatus,
                properties.getCreateBy());
            vo.setDriveStatus(driveStatus);
        }
        return vo;
    }

    /**
     * 重新驱动单个文件（同一批次）：老行置删除 + 新行新 file_id。
     *
     * @param analysisId 分析批次 ID
     * @param fileId     文件 ID
     * @return 文件驱动结果
     */
    public DriveFileVo driveSingleFile(Long analysisId, Long fileId) {
        Map<String, Object> existing = fileStatusMapper.selectActiveFile(analysisId, fileId);
        if (existing == null) {
            throw new ServiceException("文件不存在或不属于该批次：fileId=" + fileId + "，analysisId=" + analysisId);
        }
        DriveFileType type = DriveFileType.byCode(String.valueOf(existing.get("fileType")));
        if (type == null) {
            throw new ServiceException("该文件类型尚未实现驱动的 Java 解析器：" + existing.get("fileType"));
        }
        Path file = Paths.get(String.valueOf(existing.get("filePath")));
        if (!Files.isRegularFile(file)) {
            throw new ServiceException("文件在磁盘上不存在，无法重新驱动：" + file);
        }
        Map<String, Object> batch = analysisMapper.selectBatch(analysisId);
        if (batch == null) {
            throw new ServiceException("分析批次不存在：analysisId=" + analysisId);
        }
        String batchDate = String.valueOf(batch.get("analysisBatchDate"));
        DriveContext context = new DriveContext(batchDate, String.valueOf(batch.get("analysisDate")),
            String.valueOf(batch.get("productBio")), String.valueOf(batch.get("subbarcode")),
            file.getParent(), null);

        analysisMapper.updateDriveStatus(analysisId, DriveStatus.DRIVING, properties.getCreateBy());
        DriveFileVo vo = new DriveFileVo();
        int driveStatus = DriveStatus.FAILED;
        try {
            vo = processFile(analysisId, type, file, context, true);
            driveStatus = recomputeSampleStatus(analysisId);
            vo.setDriveStatus(driveStatus);
        } finally {
            analysisMapper.finishDrive(analysisId, properties.getOperatorName(), driveStatus,
                properties.getCreateBy());
        }
        return vo;
    }

    // ------------------------------------------------------------------ 整样本

    /**
     * 逐文件驱动并汇总：缺文件/失败都计入，样本状态按"本次是否全部成功"判定。
     *
     * @return 样本级驱动状态（10/11）
     */
    private int driveAllFiles(DriveSampleVo vo, DriveContext context, Long analysisId) {
        Map<String, Integer> variantStats = new LinkedHashMap<>();
        for (String category : VARIANT_ORDER) {
            variantStats.put(category, 0);
        }
        List<String> loadedNames = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (DriveFileType type : DriveFileType.values()) {
            String fileName = type.fileNameOf(context.subbarcode());
            Path file = context.uploadDir().resolve(fileName);
            if (!Files.isRegularFile(file)) {
                missing.add(fileName);
                log.warn("缺少文件：{}（analysis_id={}）", file, analysisId);
                continue;
            }
            DriveFileVo fileVo = processFile(analysisId, type, file, context, false);
            vo.getFiles().add(fileVo);
            if ("Loaded".equals(fileVo.getStatus())) {
                loadedNames.add(fileName);
                addVariantRows(variantStats, type, fileVo.getMutNum());
            } else {
                failures.add(fileName + "：" + fileVo.getMessage());
            }
        }

        vo.setFileCount(loadedNames.size());
        vo.setFileNames(String.join(",", loadedNames));
        vo.setTotalVariantCount(variantStats.get("snp_del") + "+" + variantStats.get("fusion") + "+"
            + variantStats.get("cnv") + "+" + variantStats.get("cr_all"));
        int driveStatus = missing.isEmpty() && failures.isEmpty() ? DriveStatus.SUCCESS : DriveStatus.FAILED;
        assembleMessage(vo, driveStatus, failures, missing);
        return driveStatus;
    }

    private void addVariantRows(Map<String, Integer> variantStats, DriveFileType type, Integer mutNum) {
        if (type.variantCategory() == null) {
            return;
        }
        int rows = mutNum == null ? 0 : mutNum;
        variantStats.merge(type.variantCategory(), rows, Integer::sum);
    }

    // ------------------------------------------------------------------ 单文件处理

    /**
     * 处理一个文件：插 Pending 行 → 解析 → 批量写明细 → 置 Loaded/Error。
     * <p>
     * <b>本方法保证不向外抛异常</b>（除 JVM 级错误）：任何失败都落成 Error 行并返回，
     * 这样同一个样本里一个坏文件不会影响其它文件。
     */
    private DriveFileVo processFile(Long analysisId, DriveFileType type, Path file, DriveContext context,
                                    boolean deactivateOld) {
        String fileName = file.getFileName().toString();
        DriveFileVo vo = new DriveFileVo();
        vo.setFileName(fileName);
        vo.setFileType(type.code());
        vo.setStatus("Error");
        vo.setMutNum(0);
        Long fileId = null;
        try {
            if (deactivateOld) {
                fileStatusMapper.deactivateActiveFile(analysisId, fileName, properties.getCreateBy());
            }
            fileId = createPendingFileRow(analysisId, type, file, context);
            vo.setFileId(fileId);

            DriveParseResult result = parserRegistry.required(type).parse(file, fileId);
            insertDetails(type, fileId, result.getRows());
            fileStatusMapper.markLoaded(fileId, result.rowCount(),
                DriveTexts.truncate(result.getFileText(), properties.getFileTextMaxLength()),
                properties.getCreateBy());
            vo.setStatus("Loaded");
            vo.setMutNum(result.rowCount());
            vo.setMessage("处理成功");
            log.info("驱动文件成功：analysis_id={}，file={}，行数={}", analysisId, fileName, result.rowCount());
        } catch (IOException | RuntimeException e) {
            String message = DriveTexts.truncateMessage(messageOf(e));
            vo.setMessage(message);
            log.warn("文件处理失败（不影响其它文件）：analysis_id={}，file={}，原因={}", analysisId, fileName, message);
            if (fileId != null) {
                markFileFailed(type, fileId, message);
            }
        }
        return vo;
    }

    private String messageOf(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    /** 文件字节数（读不到就留空，不影响驱动） */
    private Long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            log.warn("读取文件大小失败：{}", file, e);
            return null;
        }
    }

    /**
     * 失败落库：先清掉本次可能已插入的半截明细，再把文件置 Error。
     * <p>
     * 这里的写入失败只记日志——不能让"清理失败"把整个样本的驱动打断。
     */
    private void markFileFailed(DriveFileType type, Long fileId, String message) {
        try {
            detailMapper.deleteByFileId(type.detailTable(), fileId);
        } catch (RuntimeException e) {
            log.error("失败文件明细清理异常：fileId={}，table={}", fileId, type.detailTable(), e);
        }
        try {
            fileStatusMapper.markError(fileId, message, properties.getCreateBy());
        } catch (RuntimeException e) {
            log.error("失败文件状态写入异常：fileId={}", fileId, e);
        }
    }

    private Long createPendingFileRow(Long analysisId, DriveFileType type, Path file, DriveContext context) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("filePath", file.toString());
        row.put("fileName", file.getFileName().toString());
        row.put("dataType", type.dataType());
        row.put("fileType", type.code());
        row.put("analysisDate", context.analysisDate());
        row.put("subbarcode", context.subbarcode());
        row.put("productBio", context.productBio());
        row.put("fileSize", sizeOf(file));
        row.put("status", "Pending");
        row.put("createBy", properties.getCreateBy());
        row.put("analysisId", analysisId);
        fileStatusMapper.insertFile(row);

        Long fileId = fileStatusMapper.selectLatestActiveFileId(analysisId, file.getFileName().toString());
        if (fileId == null) {
            throw new ServiceException("文件驱动记录创建失败：" + file.getFileName());
        }
        return fileId;
    }

    private void insertDetails(DriveFileType type, Long fileId, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : rows) {
            row.put("createBy", properties.getCreateBy());
        }
        int batchSize = Math.max(1, properties.getBatchInsertSize());
        for (int start = 0; start < rows.size(); start += batchSize) {
            List<Map<String, Object>> chunk = rows.subList(start, Math.min(start + batchSize, rows.size()));
            switch (type) {
                case SNP, INDEL -> detailMapper.insertSomaticSnvIndel(chunk);
                case CNV -> detailMapper.insertCnv(chunk);
                case FUSION -> detailMapper.insertFusion(chunk);
                case CR_ALL -> detailMapper.insertCrAll(chunk);
                case MSI -> detailMapper.insertMsi(chunk);
                case QC -> detailMapper.insertQc(chunk);
                case QC_CONTROL -> detailMapper.insertQcControl(chunk);
                case CHEM -> detailMapper.insertChem(chunk);
                case CHEMICAL_ALL -> detailMapper.insertChemical(chunk);
            }
        }
    }

    // ------------------------------------------------------------------ 批次

    /**
     * UPLOAD：三件套已驱动过就拒绝（一条 INSERT…WHERE NOT EXISTS 原子判重，并发下不会双插）。
     */
    private Long createBatchIfAbsent(DriveContext context) {
        int inserted = analysisMapper.insertIfAbsent(insertRow(context));
        if (inserted == 0) {
            throw new ServiceException("该批次已驱动过，拒绝重复上传：" + context.analysisBatchDate() + "/"
                + context.productBio() + "/" + context.subbarcode());
        }
        Long analysisId = analysisMapper.selectLatestAnalysisId(context.analysisBatchDate(),
            context.productBio(), context.subbarcode());
        if (analysisId == null) {
            throw new ServiceException("analysis_data 创建失败：" + context.productBio() + "/" + context.subbarcode());
        }
        log.info("新建分析批次（上传）：analysis_id={}，{}/{}/{}", analysisId, context.analysisBatchDate(),
            context.productBio(), context.subbarcode());
        return analysisId;
    }

    /**
     * REINTERPRET（重新解读）：同三件套、不判重，直接新建一个 analysis_id。
     */
    private Long createBatch(DriveContext context) {
        analysisMapper.insertNewBatch(insertRow(context));
        Long analysisId = analysisMapper.selectLatestAnalysisId(context.analysisBatchDate(),
            context.productBio(), context.subbarcode());
        if (analysisId == null) {
            throw new ServiceException("analysis_data 创建失败：" + context.productBio() + "/" + context.subbarcode());
        }
        log.info("新建分析批次（重新解读）：analysis_id={}，{}/{}/{}", analysisId, context.analysisBatchDate(),
            context.productBio(), context.subbarcode());
        return analysisId;
    }

    private Map<String, Object> insertRow(DriveContext context) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("analysisBatchDate", context.analysisBatchDate());
        row.put("analysisDate", context.analysisDate());
        row.put("subbarcode", context.subbarcode());
        row.put("productBio", context.productBio());
        row.put("productId", context.productId());
        row.put("driveStatus", DriveStatus.DRIVING);
        row.put("operatorName", properties.getOperatorName());
        row.put("createBy", properties.getCreateBy());
        return row;
    }

    /**
     * 样本状态按"该批次所有未删除文件都 Loaded"重算（单文件重驱后用；整样本驱动按本次结果判定）。
     */
    private int recomputeSampleStatus(Long analysisId) {
        int total = fileStatusMapper.countActiveFiles(analysisId);
        int loaded = fileStatusMapper.countLoadedFiles(analysisId);
        int status = total > 0 && loaded == total ? DriveStatus.SUCCESS : DriveStatus.FAILED;
        log.info("样本驱动状态重算：analysis_id={}，{}/{} 个文件已加载 → drive_status={}", analysisId, loaded, total,
            status);
        return status;
    }

    // ------------------------------------------------------------------ 辅助

    private DriveSampleVo failOnMissingDirectory(DriveSampleVo vo, DriveContext context) {
        vo.setStatus("failed");
        vo.setMessage("上传目录不存在：" + context.uploadDir());
        vo.setDriveStatus(DriveStatus.FAILED);
        vo.setFileCount(0);
        vo.setTotalVariantCount("0+0+0+0");
        log.warn("驱动失败（目录不存在）：{}", context.uploadDir());
        return vo;
    }

    private void assembleMessage(DriveSampleVo vo, int driveStatus, List<String> failures, List<String> missing) {
        if (driveStatus == DriveStatus.SUCCESS) {
            vo.setStatus("success");
            vo.setMessage("驱动完成：" + vo.getFileCount() + " 个文件全部成功");
            return;
        }
        vo.setStatus("failed");
        StringBuilder builder = new StringBuilder("驱动失败");
        if (!missing.isEmpty()) {
            builder.append("；缺少文件 ").append(String.join(",", missing));
        }
        if (!failures.isEmpty()) {
            builder.append("；失败文件 ").append(String.join(" | ", failures));
        }
        vo.setMessage(builder.toString());
    }

    /**
     * 上传目录：{@code <root>/<batch>/<productBio>/<subbarcode>/<subbarcode>T/10.Upload}。
     */
    private Path resolveUploadDir(String analysisBatchDate, String productBio, String subbarcode) {
        String rootDir = properties.getRootDir();
        if (rootDir == null || rootDir.isBlank()) {
            throw new ServiceException("未配置上传根目录（drive.root-dir），无法驱动");
        }
        return Paths.get(rootDir, analysisBatchDate, productBio, subbarcode, subbarcode + "T", "10.Upload");
    }

    /** 路径段校验：产品/样本编号来自请求，禁止带路径分隔符或 .. 逃逸上传根 */
    private void requirePathPart(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(label + "不能为空");
        }
        if (value.contains("/") || value.contains("\\") || ".".equals(value) || "..".equals(value)) {
            throw new ServiceException(label + "不合法：" + value);
        }
    }
}
