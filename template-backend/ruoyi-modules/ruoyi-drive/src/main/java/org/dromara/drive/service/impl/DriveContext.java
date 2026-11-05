package org.dromara.drive.service.impl;

import java.nio.file.Path;

/**
 * 一次样本驱动的上下文（路径 + 归属信息），避免在多层方法之间传 6~7 个散参数。
 *
 * @param analysisBatchDate 上传批次目录名 YYYYMMDDHHMMSS（= 上传根目录第 1 层，批次身份）
 * @param analysisDate      分析日期 YYYYMMDD（= analysisBatchDate 前 8 位，展示/搜索用）
 * @param productBio        生信产品编码（上传目录第 2 层，= product_config.product_bio）
 * @param subbarcode        样本编号（上传目录第 3 层，不含结尾 T）
 * @param uploadDir         该样本的 10.Upload 目录
 * @param productId         product_config.id（驱动时按 product_bio 回填，可为 null）
 * @author <你的名字>
 */
record DriveContext(String analysisBatchDate, String analysisDate, String productBio, String subbarcode,
                    Path uploadDir, Long productId) {
}
