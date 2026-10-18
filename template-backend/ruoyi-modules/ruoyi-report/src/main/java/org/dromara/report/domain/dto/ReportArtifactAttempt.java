package org.dromara.report.domain.dto;

import java.nio.file.Path;

/** 一轮生成的文件工作区；业务版本与生成轮次分别保存。 */
public record ReportArtifactAttempt(Path home, Path staging, Path backup, String fileName, int generationNo) {
    public Path current() {
        return home.resolve("current");
    }

    public Path staged(String stage, String extension) {
        return staging.resolve(stage).resolve(fileName + extension);
    }

    public Path published(String stage, String extension) {
        return current().resolve(stage).resolve(fileName + extension);
    }
}
