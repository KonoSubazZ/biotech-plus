package org.dromara.qc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.qc.domain.BioinfoQc;
import org.dromara.qc.domain.WetLabQc;
import org.dromara.qc.mapper.BioinfoQcMapper;
import org.dromara.qc.mapper.WetLabQcMapper;
import org.dromara.qc.utils.NotifyConstants;
import org.springframework.stereotype.Component;

/**
 * 把通知状态回写到质控行（wet_lab_qc / bioinfo_qc 的 notify_status，只更新这一列）。
 * <p>
 * 真实状态以 sample_notify.notify_status 为准，质控行只是镜像（页面暂不展示）。
 *
 * @author <你的名字>
 */
@Component
@RequiredArgsConstructor
class NotifyQcStatusWriter {

    private final WetLabQcMapper wetLabQcMapper;
    private final BioinfoQcMapper bioinfoQcMapper;

    void write(String subbarcode, String notifyType, String status) {
        if (NotifyConstants.Type.WET_LAB.equals(notifyType)) {
            wetLabQcMapper.update(null, Wrappers.<WetLabQc>lambdaUpdate()
                .set(WetLabQc::getNotifyStatus, status)
                .eq(WetLabQc::getSubbarcode, subbarcode));
            return;
        }
        if (NotifyConstants.Type.BIOINFO.equals(notifyType)) {
            bioinfoQcMapper.update(null, Wrappers.<BioinfoQc>lambdaUpdate()
                .set(BioinfoQc::getNotifyStatus, status)
                .eq(BioinfoQc::getSubbarcode, subbarcode));
        }
    }
}
