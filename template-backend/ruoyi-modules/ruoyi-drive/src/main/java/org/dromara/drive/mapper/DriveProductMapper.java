package org.dromara.drive.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * product_config 只读查询（驱动侧校验产品编码并回填 product_id）。
 * <p>
 * 跨模块只读不引依赖（与 report 模块对 product_config 的做法一致），SQL 只在本文件里出现一行。
 *
 * @author <你的名字>
 */
public interface DriveProductMapper {

    /**
     * 按生信产品编码取产品 ID。
     *
     * @param productBio 生信产品编码（= 上传目录第 2 层）
     * @return product_config.id；不存在返回 null
     */
    Long selectProductIdByBio(@Param("productBio") String productBio);
}
