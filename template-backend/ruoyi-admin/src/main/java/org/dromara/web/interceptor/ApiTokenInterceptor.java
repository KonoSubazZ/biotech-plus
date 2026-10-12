package org.dromara.web.interceptor;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.satoken.utils.TenantContext;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysClientService;
import org.dromara.web.config.properties.ApiTokenProperties;
import org.dromara.web.service.SysLoginService;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 固定 token 的接口调用鉴权
 * <p>
 * 生效条件（三个都满足才动手）：开关打开 + 请求头 {@code X-Api-Token} 等于配置的固定 token +
 * 请求路径命中配置的路径列表。命中后按配置账号建立「本次请求」的登录态，后续与正常登录完全一致 ——
 * 权限看 @SaCheckPermission、create_by 记该账号、租户由业务代码按数据归属解析。
 * 不满足条件时原样放行，由 Sa-Token 拦截器按未登录处理。
 * <p>
 * 必须是拦截器而不是过滤器：Sa-Token 登录事件（UserActionListener 记在线用户/登录日志）要读当前请求，
 * 而请求上下文由 DispatcherServlet 绑定 —— 过滤器阶段还拿不到，实测会 NPE。
 * 注册时用最高优先级，保证在 Sa-Token 的拦截器之前建立身份。
 *
 * @author <你的名字>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiTokenInterceptor implements HandlerInterceptor {

    /** 调用方携带固定 token 的请求头 */
    public static final String HEADER = "X-Api-Token";

    /**
     * 内部接口会话有效期（秒）
     * <p>
     * 机器调用每次都会建一个会话，10 分钟足够覆盖一次请求，避免在 Redis 里长期堆积 token。
     */
    private static final long TOKEN_TIMEOUT_SECONDS = 600L;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiTokenProperties properties;
    private final SysUserMapper userMapper;
    private final SysLoginService loginService;
    private final ISysClientService clientService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        authenticate(request);
        return true;
    }

    /**
     * 命中固定 token 的调用才建立身份；任何一步不满足都直接放行（不伪装、不放行越权）
     */
    private void authenticate(HttpServletRequest request) {
        if (!isTokenCall(request)) {
            return;
        }
        if (LoginHelper.isLogin()) {
            // 已带正常登录态（管理员在页面/接口工具里直接调）—— 不覆盖真实身份
            return;
        }
        String account = StringUtils.trim(properties.getAccount());
        if (StringUtils.isBlank(account)) {
            // 没配账号：不建立身份，交回 Sa-Token 按未登录处理
            return;
        }
        // 账号与客户端查询都必须静默（可能属于任意药企，且此刻还没有登录态）
        SysUserVo user = TenantContext.withoutTenant(() -> userMapper.selectVoOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, account)));
        if (user == null || SystemConstants.DISABLE.equals(user.getStatus())) {
            // 账号不存在/停用：拒绝建立身份，fail closed
            log.warn("固定 token 调用：账号 {} 不存在或已停用，按未登录处理", account);
            return;
        }
        String clientId = clientIdOf(request);
        SysClientVo client = StringUtils.isBlank(clientId) ? null : clientService.queryByClientId(clientId);
        if (client == null) {
            // 没有可用客户端：同样不建立身份
            log.warn("固定 token 调用：客户端 {} 不存在或已停用，按未登录处理", clientId);
            return;
        }
        establishIdentity(account, user, client, request);
    }

    /**
     * 以该账号建立「本次请求」的登录态
     * <p>
     * 全程与租户作用域无关：此刻还没登录 —— buildLoginUser 查角色/权限、LoginHelper.login 触发登录事件
     * （记在线用户 + 回写登录时间）都会被租户拦截器拦下并抛「缺少租户身份」；
     * 正常登录流程也是整段包在 withoutTenant 里的（见 PasswordAuthStrategy）。
     */
    private void establishIdentity(String account, SysUserVo user, SysClientVo client, HttpServletRequest request) {
        TenantContext.withoutTenant(() -> {
            LoginUser loginUser = loginService.buildLoginUser(user);
            loginUser.setClientKey(client.getClientKey());
            loginUser.setDeviceType(client.getDeviceType());

            SaLoginParameter model = new SaLoginParameter();
            model.setDeviceType(client.getDeviceType());
            model.setTimeout(TOKEN_TIMEOUT_SECONDS);
            model.setActiveTimeout(TOKEN_TIMEOUT_SECONDS);
            model.setIsLastingCookie(false);
            model.setIsWriteHeader(false);
            model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
            LoginHelper.login(loginUser, model);
        });

        log.info("固定 token 调用已建立身份：account={} clientId={} {} {}",
            account, client.getClientId(), request.getMethod(), request.getRequestURI());
    }

    /** 开关打开 + 请求头 token 一致 + 路径在配置列表里 */
    private boolean isTokenCall(HttpServletRequest request) {
        if (!Boolean.TRUE.equals(properties.getEnabled()) || StringUtils.isBlank(properties.getValue())) {
            return false;
        }
        if (!properties.getValue().equals(request.getHeader(HEADER))) {
            return false;
        }
        return matchesConfiguredPath(request.getRequestURI());
    }

    private boolean matchesConfiguredPath(String uri) {
        List<String> paths = properties.getPaths();
        if (paths == null || paths.isEmpty()) {
            return false;
        }
        for (String pattern : paths) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private String clientIdOf(HttpServletRequest request) {
        String headerClientId = request.getHeader(LoginHelper.CLIENT_KEY);
        if (StringUtils.isNotBlank(headerClientId)) {
            return headerClientId;
        }
        return properties.getClientId();
    }
}
