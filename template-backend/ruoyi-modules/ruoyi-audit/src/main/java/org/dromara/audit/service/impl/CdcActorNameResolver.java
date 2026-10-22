package org.dromara.audit.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作者姓名解析（只读 sys_user 昵称，带缓存）。
 * <p>
 * 用 JdbcTemplate 而不是 MyBatis：JdbcTemplate 不经租户拦截器，后台线程不需要租户上下文。
 *
 * @author liushangzhi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CdcActorNameResolver {

    private final JdbcTemplate jdbcTemplate;

    private final Map<Long, String> cache = new ConcurrentHashMap<>();

    /**
     * 取昵称；查不到返回 null（日志里保留 null，不编造名字）
     *
     * @param userId 用户ID
     * @return 昵称
     */
    public String resolve(Long userId) {
        if (userId == null) {
            return null;
        }
        String name = cache.computeIfAbsent(userId, key -> {
            try {
                return jdbcTemplate.queryForObject(
                    "SELECT nick_name FROM sys_user WHERE user_id = ?", String.class, key);
            } catch (Exception e) {
                log.debug("查询操作者昵称失败 userId={}：{}", key, e.getMessage());
                return "";
            }
        });
        // 日志字段约定：查不到昵称写 null，不编造名字
        return name == null || name.isEmpty() ? null : name;
    }
}
