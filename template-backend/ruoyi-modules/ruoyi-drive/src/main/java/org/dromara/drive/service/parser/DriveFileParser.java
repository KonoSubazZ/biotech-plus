package org.dromara.drive.service.parser;

import org.dromara.drive.utils.DriveFileType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 一种（或几种）文件类型的解析器。
 * <p>
 * 实现类由 Spring 注入到 {@link DriveParserRegistry}，按 {@link #fileTypes()} 建索引；
 * 新增文件类型 = 新增一个实现类，不改注册表、不改 Controller、不写中心 switch。
 *
 * @author <你的名字>
 */
public interface DriveFileParser {

    /**
     * 本解析器负责的文件类型（SNP/Indel 共用一个解析器，所以返回列表）。
     *
     * @return 文件类型
     */
    List<DriveFileType> fileTypes();

    /**
     * 解析文件。
     *
     * @param file   文件绝对路径
     * @param fileId 已创建的 analysis_file_status.file_id（明细表外键）
     * @return 解析结果
     * @throws IOException 读文件失败
     */
    DriveParseResult parse(Path file, long fileId) throws IOException;
}
