package org.dromara.compliance.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 3Q 文档管理「文档」上传结果
 * <p>
 * 单独用一个对象而不是 R&lt;String&gt;：R 里有 ok(String msg) 重载，
 * 传字符串会被当成 msg、data 反而为空，前端拿不到文件名。
 *
 * @author liushangzhi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceDocFileVo {

    /** 保存后的文件名（原文件名；同名文件已在服务器目录里被替换） */
    private String fileName;
}
