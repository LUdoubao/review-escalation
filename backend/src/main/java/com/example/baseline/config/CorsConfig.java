package com.example.baseline.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 开发态跨域配置，允许 Vite dev server 直连后端时读取 /api 下的响应。
 *
 * <p>领域：本地联调基础设施；关系：由 Spring MVC 在启动时收集，与具体控制器解耦。
 * 场景：默认走 Vite 代理（同源，不触发 CORS）；仅在把前端 baseURL 改为后端绝对地址时生效。
 * 生命周期：配置类为单例，映射在容器启动阶段注册，运行期不修改。</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /** Vite 默认开发端口；与 frontend/vite.config.js 的 server.port 保持一致，否则放宽的源会失效。 */
    private static final String DEV_ORIGIN = "http://localhost:5173";

    /**
     * 仅对 /api 前缀放行指定开发源与只读方法，避免把整个应用暴露为可跨域访问。
     *
     * @param registry Spring MVC 提供的跨域映射注册器，由容器传入
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 只放行 GET/OPTIONS：基线没有写操作，缩小方法集合可防止后续新增写接口时被默默放行。
        registry.addMapping("/api/**")
                .allowedOrigins(DEV_ORIGIN)
                .allowedMethods("GET", "OPTIONS");
    }
}
