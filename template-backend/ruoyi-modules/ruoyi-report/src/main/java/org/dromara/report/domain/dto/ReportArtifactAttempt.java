package org.dromara.report.domain.dto;

import org.dromara.report.utils.ReportArtifactPaths;

import java.nio.file.Path;

/**
 * 一轮生成的文件工作区。
 * <p>
 * 生成阶段全部写在临时工作目录 work 里，成功后按 analysis_date 发布到
 * {@code <root>/report/<stage>/<analysisDate>/}；失败直接丢弃工作目录，目标目录不留半成品。
 *
 * @param root         制品根目录
 * @param analysisDate 分析日期（analysis_report.analysis_date，目录名）
 * @param fileName     本轮共用的文件名前缀（不含扩展名）
 * @param generationNo 内部生成轮次
 * @param work         临时工作目录
 */
public record ReportArtifactAttempt(String root, String analysisDate, String fileName, int generationNo, Path work) {

    /** 临时目录里的阶段文件：work/<stage>/<fileName><ext> */
    public Path staged(String stage, String extension) {
        return work.resolve(stage).resolve(fileName + extension);
    }

    /** 发布后的目标路径：<root>/report/<stage>/<analysisDate>/<fileName><ext> */
    public Path published(String stage, String extension) {
        return ReportArtifactPaths.reportDir(root, stage, analysisDate).resolve(fileName + extension);
    }

    /** 发布前旧文件暂存区（回滚用）：work/backup/<stage>/ */
    public Path backupDir(String stage) {
        return work.resolve("backup").resolve(stage);
    }
}
