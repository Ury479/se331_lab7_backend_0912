package com.example.demo331bacnkend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置
 * 
 * 🔧 CORS 配置已迁移到 SecurityConfiguration
 * 
 * 原因：
 * 1. Spring Security 的 CORS 配置优先级更高
 * 2. SecurityConfiguration 已配置 corsConfigurationSource()
 * 3. 避免重复配置和潜在冲突
 * 
 * 如需额外的 MVC 配置，可在此添加：
 * - 拦截器（Interceptors）
 * - 资源处理器（ResourceHandlers）
 * - 视图解析器（ViewResolvers）
 * - 消息转换器（MessageConverters）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    // CORS 配置已移至 SecurityConfiguration.corsConfigurationSource()
    // 如有其他 MVC 配置需求，可在此添加
    
    /*
     * 示例：添加拦截器
     * 
     * @Override
     * public void addInterceptors(InterceptorRegistry registry) {
     *     registry.addInterceptor(new CustomInterceptor())
     *             .addPathPatterns("/api/**")
     *             .excludePathPatterns("/api/v1/auth/**");
     * }
     */
    
    /*
     * 示例：静态资源处理
     * 
     * @Override
     * public void addResourceHandlers(ResourceHandlerRegistry registry) {
     *     registry.addResourceHandler("/uploads/**")
     *             .addResourceLocations("file:./uploads/");
     * }
     */
}
