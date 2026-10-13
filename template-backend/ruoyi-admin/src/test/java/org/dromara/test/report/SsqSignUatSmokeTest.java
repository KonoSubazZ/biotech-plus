package org.dromara.test.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.report.config.SsqSignProperties;
import org.dromara.report.service.impl.SsqSignServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 上上签 UAT 真连冒烟（默认跳过；会真送签、真发通知短信，跑完请到上上签后台作废测试合同）。
 * <p>
 * 本地样张（实测含所需关键字，见计划 §0）：
 * <ul>
 *   <li>报告交付：/home/liushangzhi/project/reports/report/pdf/20261013/TKHS260125414-1ASOP-CTP-009-NG0101-T001组织样本基因检测报告5-20261013001737-34616085.pdf</li>
 *   <li>报告延迟 / 质控不合格：/home/liushangzhi/project/reports/notify/pdf/20261012/qc-failed-notify-S2026007-20261012181208.pdf</li>
 * </ul>
 * 运行：mvn -Plocal -DskipTests=false -Dtest=SsqSignUatSmokeTest -Dssq.uat.smoke=true -pl ruoyi-admin -am test
 *
 * @author <你的名字>
 */
@Tag("local")
@EnabledIfSystemProperty(named = "ssq.uat.smoke", matches = "true")
@DisplayName("上上签 UAT 真连冒烟")
class SsqSignUatSmokeTest {

    private static final String REPORT_PDF =
        "/home/liushangzhi/project/reports/report/pdf/20261013/"
            + "TKHS260125414-1ASOP-CTP-009-NG0101-T001组织样本基因检测报告5-20261013001737-34616085.pdf";

    private static final String NOTIFY_PDF =
        "/home/liushangzhi/project/reports/notify/pdf/20261012/qc-failed-notify-S2026007-20261012181208.pdf";

    @Test
    @DisplayName("报告交付：真送签拿到合同号")
    void reportDelivery() {
        String contractId = service().sendReportDelivery(REPORT_PDF);
        Assertions.assertFalse(contractId.isBlank(), "合同号不应为空");
        System.out.println("[UAT] 报告交付 contractId=" + contractId);
    }

    @Test
    @Disabled("2026-10-13 口径：本次联调只跑「报告交付 + 质控不合格」两个场景；报告延迟样张本地也还没有（见文档 §未确认项）")
    @DisplayName("报告延迟：真送签拿到合同号（用质控通知样张，关键字一致）")
    void reportDelay() {
        String contractId = service().sendReportDelay(NOTIFY_PDF);
        Assertions.assertFalse(contractId.isBlank());
        System.out.println("[UAT] 报告延迟 contractId=" + contractId);
    }

    @Test
    @DisplayName("质控不合格：真送签拿到合同号")
    void qcUnqualified() {
        String contractId = service().sendQcUnqualified(NOTIFY_PDF);
        Assertions.assertFalse(contractId.isBlank());
        System.out.println("[UAT] 质控不合格 contractId=" + contractId);
    }

    /** 直接构造（不经 Spring）：值从此处显式给 = application-dev.yml 里那套 UAT 配置
     *  （properties 现在没有 Java 默认值，测试里也必须显式设置，避免「测试跑通了但配置没生效」的假象） */
    private SsqSignServiceImpl service() {
        SsqSignProperties properties = new SsqSignProperties();
        properties.setBaseUrl("https://ipaas-uat.novogene.com");
        properties.setClientId("onco-pharma-platform");
        properties.setClientSecret("K7mP2qR9xT4wZ8nB");
        properties.setTemplateId("4197207713522448386");
        return new SsqSignServiceImpl(properties, new ObjectMapper());
    }
}
