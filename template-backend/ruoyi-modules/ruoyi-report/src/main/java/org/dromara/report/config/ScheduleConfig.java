package org.dromara.report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启 Spring 定时任务（项目里原先没有 @EnableScheduling）。
 * <p>
 * 目前只有 {@code CmsSampleSyncJob} 用到；后续要加定时任务直接加 {@code @Scheduled} 方法即可。
 *
 * @author <你的名字>
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
