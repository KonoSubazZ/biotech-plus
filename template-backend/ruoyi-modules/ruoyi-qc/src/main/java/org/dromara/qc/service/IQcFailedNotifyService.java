package org.dromara.qc.service;

import org.dromara.qc.domain.bo.QcFailedNotifyBo;
import org.dromara.qc.domain.vo.QcFailedNotifyPrefillVo;
import org.dromara.qc.domain.vo.QcFailedNotifyResultVo;

import java.io.File;

/**
 * 样本质控不合格通知 服务层
 *
 * @author <你的名字>
 */
public interface IQcFailedNotifyService {
    /**
     * 处理弹窗预填：表头按样本编号回填 sample_file 字段 + 默认值 + 上次生成内容
     * <p>
     * 按 {@code qcCategory} 取对应质控表（wet_lab → wet_lab_qc / bioinfo → bioinfo_qc）的
     * 质控记录 id、分析时间与不达标原因；为空按 bioinfo 处理（老调用方兼容）。
     *
     * @param subbarcode 样本编号
     * @param qcCategory 质控类型（wet_lab 湿实验 / bioinfo 生信）
     * @return 预填数据
     */
    QcFailedNotifyPrefillVo prefill(String subbarcode, String qcCategory);

    /**
     * 保存并生成通知文档：表单 → JSON → docxtpl 渲染 DOCX → 落一条生成记录（流水）
     *
     * @param bo 表单
     * @return 生成结果（记录 id、通知单号、JSON/DOCX 路径与渲染信息）
     */
    QcFailedNotifyResultVo generate(QcFailedNotifyBo bo);

    /**
     * 取已生成的通知文档（按生成记录 id）
     * <p>
     * 只读记录里登记的路径，且必须落在制品根目录内（与报告模块同一口径），不猜路径。
     *
     * @param id 生成记录 id
     * @return DOCX 文件
     */
    File loadDocx(Long id);

    /**
     * 取已生成通知单的 PDF（按生成记录 id）
     * <p>
     * 口径与 {@link #loadDocx(Long)} 一致：只读记录里登记的路径、必须落在制品根目录内。
     * 转换失败时记录里 pdf_path 为空，这里按「还没生成 PDF」报错。
     *
     * @param id 生成记录 id
     * @return PDF 文件
     */
    File loadPdf(Long id);
}
