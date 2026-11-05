package org.dromara.report.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.report.config.CmsApiProperties;
import org.dromara.report.service.ICmsSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每天 01:00 拉前一天的 CMS 订单样本并 upsert 到 cms_sample。
 * <p>
 * 用 Spring {@code @Scheduled}（当前后端单容器部署，不引 SnailJob 服务）；
 * {@code cms.api.sync-enabled=false} 时整个 bean 不注册。
 * 未配置 cms.api.base-url 时只打 WARN 跳过，不抛异常（prod 留空即"静默不跑"，避免启动失败）。
 * 失败不重试：第二天会再拉一次，且行内「刷新」可人工补。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cms.api", name = "sync-enabled", havingValue = "true", matchIfMissing = true)
public class CmsSampleSyncJob {

    private final ICmsSyncService cmsSyncService;
    private final CmsApiProperties props;

    /** 每天 01:00 拉前一天；cron 可用 CMS_SYNC_CRON 覆盖 */
    @Scheduled(cron = "${cms.api.sync-cron:0 0 1 * * ?}")
    public void syncYesterday() {
        if (props.getBaseUrl() == null || props.getBaseUrl().isBlank()) {
            log.warn("跳过 CMS 每日同步：未配置 cms.api.base-url");
            return;
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try {
            log.info("CMS 每日同步开始 {}", yesterday);
            log.info("CMS 每日同步结束 {}", cmsSyncService.syncRange(yesterday, yesterday, "SYSTEM"));
        } catch (Exception e) {
            log.error("CMS 每日同步失败 {}", yesterday, e);
        }
    }
}
