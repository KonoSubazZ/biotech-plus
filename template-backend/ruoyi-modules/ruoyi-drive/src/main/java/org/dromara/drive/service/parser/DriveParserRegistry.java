package org.dromara.drive.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.drive.utils.DriveFileType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 解析器注册表：Spring 注入全部 {@link DriveFileParser}，按文件类型建索引。
 * <p>
 * 重复注册（两个解析器声明同一类型）直接启动失败 —— 配置错误应该在启动期暴露，而不是驱动到一半才发现。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
public class DriveParserRegistry {

    private final Map<DriveFileType, DriveFileParser> parsers = new EnumMap<>(DriveFileType.class);

    public DriveParserRegistry(List<DriveFileParser> parserList) {
        for (DriveFileParser parser : parserList) {
            for (DriveFileType type : parser.fileTypes()) {
                DriveFileParser existing = parsers.put(type, parser);
                if (existing != null) {
                    throw new IllegalStateException("文件类型被重复注册：" + type.code());
                }
            }
        }
        log.info("驱动解析器已注册 {} 个文件类型：{}", parsers.size(), parsers.keySet());
    }

    /**
     * 取某文件类型的解析器。
     *
     * @param type 文件类型
     * @return 解析器
     */
    public DriveFileParser required(DriveFileType type) {
        DriveFileParser parser = parsers.get(type);
        if (parser == null) {
            throw new ServiceException("文件类型未实现解析器：" + type.code());
        }
        return parser;
    }
}
