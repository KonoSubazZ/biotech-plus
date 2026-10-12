package org.dromara.web.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定 token 的接口调用配置
 * <p>
 * 场景：外部系统（生信流程、脚本）往本系统推数据，不想走「登录换 access_token」那套流程。
 * 调用方只要在请求头带约定好的固定 token，服务端就按 {@link #account} 的身份执行
 * （该账号自己创建、自己授权，权限完全由它的角色决定）。
 * <p>
 * 配置示例见 ruoyi-admin/src/main/resources/application.yml 的 api.token 段。
 *
 * @author <你的名字>
 */
@Data
@ConfigurationProperties(prefix = "api.token")
public class ApiTokenProperties {

    /** 开关：false 或没配 token 时整个机制不生效 */
    private Boolean enabled = false;

    /** 调用方携带的固定 token（请求头 X-Api-Token），不入库、不进前端 */
    private String value;

    /** 以哪个登录账号的身份执行；账号不存在或停用时按未登录处理（接口返回 401/403），不放行 */
    private String account;

    /** 生效路径（Ant 风格），一个模块一条；别的模块要接入就往这里加 */
    private List<String> paths = new ArrayList<>();

    /** 调用方没带 clientid 请求头时使用的客户端编号（对应 sys_client.client_key） */
    private String clientId;

}
