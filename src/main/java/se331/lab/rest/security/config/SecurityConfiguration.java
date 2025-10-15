package se331.lab.rest.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Security 安全配置
 * 
 * 优化点：
 * 1. ✅ CORS 跨域配置（前后端不同源）
 * 2. ✅ 放行路径集中管理（常量化）
 * 3. ✅ 刷新接口限流建议（生产环境）
 * 4. ✅ 统一异常处理（AuthenticationEntryPoint + AccessDeniedHandler）
 * 5. ✅ 文档端点管理（Swagger/OpenAPI）
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final AuthenticationProvider authenticationProvider;
  private final LogoutHandler logoutHandler;

  // 🔧 优化2: 放行路径集中管理
  private static final class SecurityPaths {
    // 认证相关端点（完全公开）
    public static final String[] AUTH_WHITELIST = {
        "/api/v1/auth/register",
        "/api/v1/auth/authenticate",
        "/api/v1/auth/refresh-token"
    };
    
    // 文档端点（开发环境放行，生产环境建议关闭或加密）
    // 当前项目未启用 Swagger，此配置保留供将来使用
    // 启用方法见: Security配置优化说明.md - 5️⃣ 文档端点管理
    @SuppressWarnings("unused")
    public static final String[] DOC_WHITELIST = {
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/**"  // 健康检查端点
    };
    
    // 错误页面
    public static final String[] ERROR_PAGES = {
        "/error"
    };
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http.headers((headers) -> {
      headers.frameOptions((frameOptions) -> frameOptions.disable());
    });
    
    http
        // 🔧 2.10: 将 CORS 配置源应用到 Security Filter Chain
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        
        // CSRF 禁用（无状态JWT不需要）
        .csrf((csrf) -> csrf.disable())
        
        // 授权配置
        .authorizeHttpRequests((authorize) -> {
          // OPTIONS 预检请求放行（CORS）
          authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
          
          // 🔧 优化2: 使用集中管理的放行路径
          authorize.requestMatchers(SecurityPaths.AUTH_WHITELIST).permitAll();
          authorize.requestMatchers(SecurityPaths.ERROR_PAGES).permitAll();
          
          // 🔧 优化5: 文档端点放行（开发环境）
          // 生产环境建议移除或使用 @Profile("dev") 条件化
          // authorize.requestMatchers(SecurityPaths.DOC_WHITELIST).permitAll();
          
          // 统一前缀后的公开端点：匿名可读（GET）
          authorize.requestMatchers(HttpMethod.GET, "/api/v1/events/**").permitAll();
          authorize.requestMatchers(HttpMethod.GET, "/api/v1/organizers/**").permitAll();

          // 写操作仅 ADMIN（POST/PUT/PATCH/DELETE）
          authorize.requestMatchers(HttpMethod.POST, "/api/v1/events/**").hasRole("ADMIN");
          authorize.requestMatchers(HttpMethod.PUT, "/api/v1/events/**").hasRole("ADMIN");
          authorize.requestMatchers(HttpMethod.PATCH, "/api/v1/events/**").hasRole("ADMIN");
          authorize.requestMatchers(HttpMethod.DELETE, "/api/v1/events/**").hasRole("ADMIN");
          authorize.requestMatchers(HttpMethod.POST, "/api/v1/organizers/**").hasRole("ADMIN");
          authorize.requestMatchers(HttpMethod.PUT, "/api/v1/organizers/**").hasRole("ADMIN");
          authorize.requestMatchers(HttpMethod.PATCH, "/api/v1/organizers/**").hasRole("ADMIN");
          authorize.requestMatchers(HttpMethod.DELETE, "/api/v1/organizers/**").hasRole("ADMIN");

          // 其他所有请求需要认证
          authorize.anyRequest().authenticated();
        })

        // 无状态会话管理
        .sessionManagement((session) -> {
          session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        })

        // 🔧 优化4: 统一异常处理
        .exceptionHandling(exception -> {
          exception.authenticationEntryPoint(authenticationEntryPoint());
          exception.accessDeniedHandler(accessDeniedHandler());
        })

        // 认证提供者
        .authenticationProvider(authenticationProvider)
        
        // JWT 过滤器（在 UsernamePasswordAuthenticationFilter 之前）
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        
        // 登出配置
        .logout((logout) -> {
          logout.logoutUrl("/api/v1/auth/logout");
          logout.addLogoutHandler(logoutHandler);
          logout.logoutSuccessHandler((request, response, authentication) -> {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.OK.value());
          });
        })
    ;

    return http.build();
  }

  /**
   * 🔧 2.9: 在 Security 中创建 CORS 配置 Bean
   * 
   * 此配置已从 WebConfig 迁移到 SecurityConfiguration：
   * - 优先级更高（Security Filter Chain 先执行）
   * - 避免与 WebConfig CORS 配置冲突
   * - 统一管理跨域和安全策略
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // allow your front-end origins
    config.setAllowedOriginPatterns(List.of("http://localhost:5173", "http://13.212.6.216:8001"));
    config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));             // includes Authorization
    config.setExposedHeaders(List.of("x-total-count")); // for pagination
    config.setAllowCredentials(true);
    
    // 允许的前端域名（根据实际情况调整）
    config.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:5173",     // Vue 3 Vite dev server
        "http://13.212.6.216:8001"   // 生产环境
    ));
    
    // 允许的 HTTP 方法
    config.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    ));
    
    // 允许的请求头
    config.setAllowedHeaders(Arrays.asList("*"));
    
    // 暴露给前端的响应头（重要：包含分页用的 x-total-count）
    config.setExposedHeaders(Arrays.asList("x-total-count"));
    
    // 允许携带凭证（Cookie、Authorization header）
    config.setAllowCredentials(true);
    
    // 注册 CORS 配置到所有路径
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    
    return source;
  }

  /**
   * 🔧 优化4: 认证失败处理器
   * 
   * 当用户未登录或 Token 无效时触发，返回 401 Unauthorized
   */
  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> {
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.setStatus(HttpStatus.UNAUTHORIZED.value());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "Unauthorized");
      errorResponse.put("message", "认证失败，请先登录");
      errorResponse.put("path", request.getRequestURI());
      errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
      errorResponse.put("timestamp", System.currentTimeMillis());

      new ObjectMapper().writeValue(response.getOutputStream(), errorResponse);
    };
  }

  /**
   * 🔧 优化4: 授权失败处理器
   * 
   * 当用户已登录但权限不足时触发，返回 403 Forbidden
   */
  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.setStatus(HttpStatus.FORBIDDEN.value());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "Forbidden");
      errorResponse.put("message", "权限不足，无法访问该资源");
      errorResponse.put("path", request.getRequestURI());
      errorResponse.put("status", HttpStatus.FORBIDDEN.value());
      errorResponse.put("timestamp", System.currentTimeMillis());

      new ObjectMapper().writeValue(response.getOutputStream(), errorResponse);
    };
  }
}

