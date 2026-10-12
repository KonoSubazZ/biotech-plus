package org.dromara.common.doc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * knife4j 中文界面（/doc.html）的静态资源映射
 * <p>
 * <b>为什么要自己映射</b>：本项目把 {@code spring.mvc.static-path-pattern} 设成了 {@code /static/**}
 * （默认静态资源只在 /static/ 下暴露），而 knife4j 的界面是纯静态资源（放在 knife4j-openapi3-ui 的
 * {@code META-INF/resources/} 下）。不映射的话 /doc.html 会走到「请求地址不存在」的 404 —— 实测踩过。
 * springdoc 自带的 /swagger-ui 不受影响，因为它自己注册了 /swagger-ui/** 的资源映射。
 * <p>
 * <b>为什么不用 knife4j 的 starter</b>：knife4j-openapi3-jakarta-spring-boot-starter 4.5.0 依赖
 * springdoc 2.6 时代的 {@code org.springdoc.core.configuration.SpringDocUIConfiguration}，
 * 与本项目的 springdoc 2.8.17（对应 Spring Boot 3.5）类名已变，加了会导致应用起不来（实测 22 次启动失败）。
 * 只引 knife4j-openapi3-ui（纯静态包，没有任何 Java 依赖）最稳：界面是 knife4j，文档内容仍由 springdoc 生成。
 *
 * @author <你的名字>
 */
@Configuration
public class Knife4jUiConfig implements WebMvcConfigurer {

    /**
     * /webjars/** 是目录型位置，直接映射即可；doc.html 在下面用转发处理
     * （doc.html 里的 js/css 引用都是相对路径 webjars/...，所以浏览器会请求 /webjars/**）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * doc.html 是「单个文件」，而资源处理器的位置必须是目录（映射文件本身会 NoResourceFound，
     * 实测 500「No static resource /doc.html」）—— 所以转发到默认静态路径 /static/** 下，
     * 那条路径由 {@code spring.mvc.static-path-pattern} 提供，资源位置天然包含 META-INF/resources。
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/doc.html").setViewName("forward:/static/doc.html");
    }
}
