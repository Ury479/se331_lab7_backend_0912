# Spring Security 配置优化 - 快速参考

## 🎯 优化总览

| 优化项 | 状态 | 文件 | 说明 |
|--------|------|------|------|
| **CORS 跨域** | ✅ | SecurityConfiguration.java | Security 层 + MVC 层协同 |
| **路径管理** | ✅ | SecurityConfiguration.java | 集中常量化管理 |
| **异常处理** | ✅ | SecurityConfiguration.java | 401/403 统一 JSON |
| **限流建议** | ✅ | Security配置优化说明.md | 3 种方案可选 |
| **文档端点** | ✅ | SecurityConfiguration.java | 预留配置 |

---

## 📋 核心配置

### 1. CORS 配置

```java
// SecurityConfiguration.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(Arrays.asList("*"));  // 开发环境
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setAllowedHeaders(Arrays.asList("*"));
    config.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

**⚠️ 生产环境必改**：
```java
// ❌ 开发环境
config.setAllowedOriginPatterns(Arrays.asList("*"));

// ✅ 生产环境
config.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
```

---

### 2. 放行路径

```java
private static final class SecurityPaths {
    // 认证端点
    public static final String[] AUTH_WHITELIST = {
        "/api/v1/auth/register",
        "/api/v1/auth/authenticate",
        "/api/v1/auth/refresh-token"
    };
    
    // 错误页面
    public static final String[] ERROR_PAGES = { "/error" };
    
    // 文档端点（可选）
    @SuppressWarnings("unused")
    public static final String[] DOC_WHITELIST = {
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/actuator/**"
    };
}

// 使用
authorize.requestMatchers(SecurityPaths.AUTH_WHITELIST).permitAll();
```

---

### 3. 统一异常处理

#### 401 - 认证失败
```java
@Bean
public AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Unauthorized");
        error.put("message", "认证失败，请先登录");
        error.put("path", request.getRequestURI());
        error.put("status", 401);
        error.put("timestamp", System.currentTimeMillis());
        
        new ObjectMapper().writeValue(response.getOutputStream(), error);
    };
}
```

**响应示例**：
```json
{
  "error": "Unauthorized",
  "message": "认证失败，请先登录",
  "path": "/events",
  "status": 401,
  "timestamp": 1760346402178
}
```

#### 403 - 权限不足
```java
@Bean
public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
        // 同上，status: 403, error: "Forbidden", message: "权限不足"
    };
}
```

---

### 4. 过滤器顺序

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // ✅ CORS
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authorize -> {
            authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            authorize.requestMatchers(SecurityPaths.AUTH_WHITELIST).permitAll();
            authorize.anyRequest().authenticated();
        })
        .exceptionHandling(exception -> {
            exception.authenticationEntryPoint(authenticationEntryPoint());    // ✅ 401
            exception.accessDeniedHandler(accessDeniedHandler());              // ✅ 403
        })
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
```

**实际过滤器链**：
```
... → CorsFilter → LogoutFilter → JwtAuthenticationFilter → ...
```

---

## 🧪 测试验证

### 快速测试
```bash
# 运行完整测试
./test-security-simple.sh

# ✅ 期望结果
# ✅ 通过: 9
# ❌ 失败: 0
# 🎉 所有测试通过！
```

### 手动测试

#### 1. CORS 预检
```bash
curl -X OPTIONS http://localhost:8080/events \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -v

# ✅ 期望：200 + CORS 响应头
```

#### 2. 401 认证失败
```bash
curl http://localhost:8080/events

# ✅ 期望：
{
  "error": "Unauthorized",
  "message": "认证失败，请先登录",
  "status": 401
}
```

#### 3. 登录获取 Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# ✅ 期望：
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ..."
}
```

#### 4. 使用 Token 访问
```bash
curl http://localhost:8080/events \
  -H "Authorization: Bearer <access_token>"

# ✅ 期望：200 + 事件列表
```

#### 5. 刷新 Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Authorization: Bearer <refresh_token>"

# ✅ 期望：200 + 新 Token
```

---

## 🚨 生产环境配置

### 必须修改

1. **CORS 域名限制**
```java
// ✅ 改为具体域名
configuration.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
```

2. **实施限流**（选择一种）
- ✅ Bucket4j（单体应用）
- ✅ Redis（分布式）
- ✅ Gateway（微服务）

3. **移除/保护文档端点**
```java
// ❌ 生产环境不要放行
// authorize.requestMatchers(SecurityPaths.DOC_WHITELIST).permitAll();

// ✅ 或添加认证
authorize.requestMatchers("/swagger-ui/**").hasRole("ADMIN");
```

### 建议优化

1. **错误信息脱敏**
```java
// ✅ 生产环境使用通用错误
errorResponse.put("message", "认证失败");  // 不暴露详情
```

2. **日志级别**
```yaml
# application-prod.yml
logging:
  level:
    org.springframework.security: WARN
    se331.lab.rest.security: INFO
```

3. **监控告警**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
```

---

## 📂 相关文档

| 文档 | 内容 | 适用场景 |
|------|------|----------|
| **Security配置优化说明.md** | 详细优化指南 | 深入学习 |
| **Security配置优化完成总结.md** | 优化成果总结 | 快速回顾 |
| **JWT过滤器优化说明.md** | 过滤器优化 | JWT 相关 |
| **认证优化说明.md** | 认证服务优化 | Token 刷新 |
| **Firebase存储优化说明.md** | 存储服务优化 | 文件上传 |
| **本文档** | 快速参考 | 日常使用 |

---

## 🔧 常见问题

### Q1: CORS 预检返回 403？
**A:** 确保 `http.cors()` 在 `SecurityFilterChain` 中正确配置

### Q2: 401 返回 HTML 而不是 JSON？
**A:** 确保 `authenticationEntryPoint()` 已配置并应用

### Q3: Token 刷新失败？
**A:** 检查 `AuthenticationService.refreshToken()` 是否正确实现

### Q4: 过滤器顺序不对？
**A:** 查看启动日志：`grep "Will secure any request" spring-boot-new.log`

### Q5: 如何启用 Swagger？
**A:** 见 `Security配置优化说明.md` - 5️⃣ 文档端点管理

---

## ✅ 验证清单

### 开发环境
- [x] CORS 配置（允许所有来源）
- [x] 认证端点放行
- [x] 统一 JSON 错误响应
- [x] Token 认证正常
- [x] Token 刷新正常
- [x] 过滤器链正确

### 生产环境（必须）
- [ ] CORS 限制具体域名
- [ ] 实施限流保护
- [ ] 移除/保护文档端点
- [ ] 错误信息脱敏
- [ ] 日志级别调整
- [ ] 监控告警配置

---

## 🎉 优化成果

✅ **CORS 跨域** - Security + MVC 双层配置  
✅ **路径管理** - 集中常量化，易于维护  
✅ **异常处理** - 401/403 统一 JSON 响应  
✅ **限流建议** - 提供 3 种完整方案  
✅ **文档端点** - 预留配置 + 启用指南  

**测试结果**：✅ 9/9 通过

---

**📞 需要帮助？**
- 详细文档：`Security配置优化说明.md`
- 测试脚本：`./test-security-simple.sh`
- 验证日志：`tail -f spring-boot-new.log`

