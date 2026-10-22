package org.dromara.drive.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 按样本驱动的入参（上传 / 重新解读）。
 * <p>
 * 目录由服务端拼接，不接受完整路径：
 * {@code <root>/<analysisBatchDate>/<productBio>/<subbarcode>/<subbarcode>T/10.Upload/}。
 * <p>
 * 两种类型（用户 2026-10-22 口径）：
 * <ul>
 *   <li>{@code UPLOAD}（默认）：生信上传后调用。三件套（batch + product + subbarcode）已有驱动记录 → <b>拒绝</b>；</li>
 *   <li>{@code REINTERPRET}：重新解读。同目录、不判重，直接新建一个 analysis_id（新批次）。</li>
 * </ul>
 *
 * @author <你的名字>
 */
@Data
public class DriveSampleBo {

    /** 上传批次目录名 YYYYMMDDHHMMSS（= 上传根目录第 1 层，批次身份） */
    @NotBlank(message = "批次目录（analysisBatchDate）不能为空")
    @Pattern(regexp = "\\d{14}", message = "批次目录必须是 14 位 YYYYMMDDHHMMSS")
    private String analysisBatchDate;

    /** 生信产品编码（= 上传目录第 2 层 = product_config.product_bio） */
    @NotBlank(message = "产品编码（productBio）不能为空")
    private String productBio;

    /** 样本编号（上传目录第 3 层，不含结尾 T） */
    @NotBlank(message = "样本编号（subbarcode）不能为空")
    private String subbarcode;

    /** 驱动类型：UPLOAD（默认，判重）/ REINTERPRET（重新解读，不判重） */
    private String type = TYPE_UPLOAD;

    /** 上传：三件套已驱动过就拒绝 */
    public static final String TYPE_UPLOAD = "UPLOAD";

    /** 重新解读：同三件套、新建 analysis_id */
    public static final String TYPE_REINTERPRET = "REINTERPRET";
}
