package org.dromara.report.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * 制品权限：渲染器/子进程以容器 root 写盘，默认 0600、属主 root，宿主上直接打开/拷贝会 Permission denied。
 * 报告与通知都要给人看，落盘后统一放开为 0644（失败只记 warn，不影响生成）。
 *
 * @author <你的名字>
 */
@Slf4j
public final class ArtifactPermissions {

    private ArtifactPermissions() {
    }

    /** 放开为 rw-r--r--；不支持 POSIX 或失败只记日志 */
    public static void relax(Path path) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r--r--"));
        } catch (IOException | UnsupportedOperationException e) {
            log.warn("制品权限放开失败（不影响生成）：{}，{}", path, e.getMessage());
        }
    }
}
