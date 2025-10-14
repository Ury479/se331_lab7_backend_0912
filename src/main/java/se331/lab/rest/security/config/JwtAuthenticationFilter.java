package se331.lab.rest.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import se331.lab.rest.security.token.TokenRepository;
import se331.lab.rest.security.token.TokenType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器
 * 
 * 优化点：
 * 1. 使用 AntPathMatcher 精确匹配路径
 * 2. 完善异常处理，返回 401 便于前端处理
 * 3. OPTIONS 预检请求已在 SecurityConfiguration 放行
 * 4. 生产环境可调整日志级别
 * 5. 刷新令牌在 Service 层处理
 * 6. 过滤器顺序在 SecurityConfiguration 正确配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final TokenRepository tokenRepository;
  
  // 🔧 修复1: 使用 AntPathMatcher 精确匹配路径
  private final AntPathMatcher pathMatcher = new AntPathMatcher();
  
  // 定义需要跳过 JWT 验证的路径
  private static final List<String> EXCLUDED_PATHS = Arrays.asList(
      "/api/v1/auth/**",      // 所有认证相关端点
      "/error",               // 错误页面
      "/actuator/**"          // 健康检查端点（可选）
  );

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    
    // 🔧 2.7: Always let preflight (OPTIONS) through FIRST
    // CORS 预检请求必须最先放行，避免被 JWT 校验拦截
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }
    
    final String requestPath = request.getServletPath();
    
    // 🔧 修复1: 使用 AntPathMatcher 精确匹配认证端点
    if (shouldSkipFilter(requestPath)) {
      log.debug("Skipping JWT filter for path: {}", requestPath);
      filterChain.doFilter(request, response);
      return;
    }
    
    final String authHeader = request.getHeader("Authorization");
    
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.debug("No Bearer token found in Authorization header for path: {}", requestPath);
      filterChain.doFilter(request, response);
      return;
    }
    
    try {
      final String jwt = authHeader.substring(7);
      
      // 🔧 修复4: 日志安全 - 只在 DEBUG 级别打印 token 前缀
      if (log.isDebugEnabled()) {
        log.debug("JWT Token extracted: {}...", jwt.substring(0, Math.min(20, jwt.length())));
      }
      
      // 🔧 修复2: 异常处理 - 捕获 token 解析异常
      final String username;
      try {
        username = jwtService.extractUsername(jwt);
      } catch (Exception e) {
        log.warn("Failed to extract username from JWT: {}", e.getMessage());
        sendUnauthorizedError(response, "Invalid or expired token");
        return;
      }
      
      if (log.isDebugEnabled()) {
        log.debug("Username extracted from JWT: {}", username);
      }
      
      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        
        UserDetails userDetails;
        try {
          userDetails = this.userDetailsService.loadUserByUsername(username);
        } catch (Exception e) {
          log.warn("User not found: {}", username);
          sendUnauthorizedError(response, "User not found");
          return;
        }
        
        if (log.isDebugEnabled()) {
          log.debug("User loaded from database: {}, Authorities: {}", 
                    userDetails.getUsername(), userDetails.getAuthorities());
        }
        
        // 🔧 修复5: Token 类型验证 - 只接受 ACCESS 或 BEARER 类型
        boolean isTokenValid = tokenRepository.findByToken(jwt)
                .map(t -> (t.getTokenType() == TokenType.ACCESS || t.getTokenType() == TokenType.BEARER) 
                          && !t.isExpired() && !t.isRevoked())
                .orElse(false);
        
        if (log.isDebugEnabled()) {
          log.debug("Token validation - JWT valid: {}, DB token valid: {}", 
                    jwtService.isTokenValid(jwt, userDetails), isTokenValid);
        }
        
        // 🔧 修复2: 异常处理 - 捕获 token 验证异常
        boolean isJwtValid;
        try {
          isJwtValid = jwtService.isTokenValid(jwt, userDetails);
        } catch (Exception e) {
          log.warn("JWT validation failed: {}", e.getMessage());
          sendUnauthorizedError(response, "Token validation failed");
          return;
        }
        
        if (isJwtValid && isTokenValid) {
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
              userDetails,
              null,
              userDetails.getAuthorities()
          );
          authToken.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(request)
          );
          SecurityContextHolder.getContext().setAuthentication(authToken);
          
          if (log.isDebugEnabled()) {
            log.debug("SecurityContext authentication set successfully for user: {}", username);
          }
        } else {
          log.warn("Token validation failed for user: {} (JWT valid: {}, DB valid: {})", 
                   username, isJwtValid, isTokenValid);
          // 不返回错误，让请求继续，由 Spring Security 处理未授权访问
        }
      }
      
    } catch (Exception e) {
      // 🔧 修复2: 全局异常处理 - 捕获所有未预期的异常
      log.error("Unexpected error in JWT filter for path: {}", requestPath, e);
      sendUnauthorizedError(response, "Authentication failed");
      return;
    }
    
    filterChain.doFilter(request, response);
  }
  
  /**
   * 🔧 修复1: 使用 AntPathMatcher 判断路径是否应该跳过过滤器
   */
  private boolean shouldSkipFilter(String requestPath) {
    return EXCLUDED_PATHS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
  }
  
  /**
   * 🔧 修复2: 统一的 401 错误响应，便于前端触发刷新或跳转登录
   */
  private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    String jsonResponse = String.format(
        "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"status\":401,\"timestamp\":%d}",
        message,
        System.currentTimeMillis()
    );
    
    response.getWriter().write(jsonResponse);
    response.getWriter().flush();
  }
}
