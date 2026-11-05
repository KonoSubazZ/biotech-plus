package org.dromara.qc.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.qc.domain.bo.SampleNotifyBo;
import org.dromara.qc.domain.bo.SampleNotifyQueryBo;
import org.dromara.qc.domain.vo.SampleNotifyPrefillVo;
import org.dromara.qc.domain.vo.SampleNotifyResultVo;
import org.dromara.qc.domain.vo.SampleNotifyVo;

import java.io.File;

/**
 * 样本通知台账 服务层（生成侧：预填 / 生成 / 列表 / 详情 / 下载）
 *
 * @author <你的名字>
 */
public interface ISampleNotifyService {

    /**
     * 处理弹窗预填：表头按样本编号回填 cms_sample 字段 + 默认值 + 上次生成内容
     * <p>
     * delay / sample 不查质控表；wet_lab / bioinfo 取对应质控记录的分析时间与不达标原因。
     *
     * @param subbarcode 样本编号
     * @param notifyType 通知类型（delay / sample / wet_lab / bioinfo）
     * @return 预填数据
     */
    SampleNotifyPrefillVo prefill(String subbarcode, String notifyType);

    /**
     * 保存并生成通知文档：表单 → JSON → docxtpl 渲染 DOCX → 转 PDF → 落台账
     * <p>
     * 同一天同一样本同一类型：只有「未签名且未提交」的旧记录会被覆盖；已送签/已签署/已提交/已发送直接拒绝。
     *
     * @param bo 表单
     * @return 生成结果
     */
    SampleNotifyResultVo generate(SampleNotifyBo bo);

    /**
     * 异常处理分页：只返回「已签名且已提交」的记录（pending / sent / failed）
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<SampleNotifyVo> selectPageList(SampleNotifyQueryBo bo, PageQuery pageQuery);

    /**
     * 通知详情
     *
     * @param id 通知记录 id
     * @return 详情
     */
    SampleNotifyVo queryById(Long id);

    /** 下载 DOCX */
    File loadDocx(Long id);

    /** 下载 PDF */
    File loadPdf(Long id);

    /** 下载签名后 PDF */
    File loadSignedPdf(Long id);
}
