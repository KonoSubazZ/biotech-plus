package org.dromara.audit.service.impl;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.audit.config.CanalCdcProperties;
import org.dromara.audit.domain.CdcChangeLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * canal 订阅线程：把每条行变更写成一行日志。
 * <p>
 * 专属 logger CDC_CHANGE（见 logback-plus.xml），additivity=false，不污染 sys-info.log。
 * 只在 audit.cdc.enabled=true 时装配；异常断连 5 秒后重连。
 *
 * @author liushangzhi
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "audit.cdc", name = "enabled", havingValue = "true")
public class CanalCdcConsumer implements SmartLifecycle {

    /** 变更日志专用 logger，对应 logback-plus.xml 的 CDC_CHANGE */
    private static final Logger CDC_LOG = LoggerFactory.getLogger("CDC_CHANGE");

    private final CanalCdcProperties properties;

    private final CdcActorNameResolver actorNameResolver;

    private volatile boolean running;

    private Thread worker;

    @Override
    public void start() {
        running = true;
        worker = new Thread(this::runLoop, "canal-cdc-consumer");
        worker.setDaemon(true);
        worker.start();
        log.info("CDC 变更日志已启动：{}:{} destination={}", properties.getHost(), properties.getPort(),
            properties.getDestination());
    }

    private void runLoop() {
        CanalConnector connector = null;
        while (running) {
            try {
                if (connector == null) {
                    connector = connect();
                }
                Message message = connector.getWithoutAck(properties.getBatchSize(),
                    properties.getPollTimeoutMs(), TimeUnit.MILLISECONDS);
                long batchId = message.getId();
                if (batchId == -1 || message.getEntries().isEmpty()) {
                    continue;
                }
                int written = writeLogs(message.getEntries());
                connector.ack(batchId);
                if (written > 0) {
                    log.debug("CDC 批次 {} 输出 {} 行", batchId, written);
                }
            } catch (Exception e) {
                log.error("CDC 订阅异常，5 秒后重连：{}", e.getMessage(), e);
                close(connector);
                connector = null;
                sleepQuietly();
            }
        }
        close(connector);
    }

    private int writeLogs(List<CanalEntry.Entry> entries) {
        int written = 0;
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }
            for (CdcChangeLine line : CdcRowDecoder.decode(entry, properties.getMaxValueLength())) {
                line.setActorName(actorNameResolver.resolve(line.getActorId()));
                CDC_LOG.info(line.toLogLine());
                written++;
            }
        }
        return written;
    }

    private CanalConnector connect() {
        CanalConnector connector = CanalConnectors.newSingleConnector(
            new InetSocketAddress(properties.getHost(), properties.getPort()),
            properties.getDestination(), properties.getUsername(), properties.getPassword());
        connector.connect();
        connector.subscribe(properties.getFilter());
        log.info("已连接 canal-server，订阅 filter={}", properties.getFilter());
        return connector;
    }

    private void close(CanalConnector connector) {
        if (connector == null) {
            return;
        }
        try {
            connector.disconnect();
        } catch (Exception e) {
            log.warn("关闭 canal 连接失败：{}", e.getMessage());
        }
    }

    private void sleepQuietly() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    @Override
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
