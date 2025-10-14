# Spring Security 配置优化说明

## 📋 优化的5个核心功能

### 1️⃣ CORS 跨域配置 ✅

**问题诊断**：
- ✅ **前后端不同源**（已确认项目前后端分离）
- ✅ 原有 `WebConfig` 配置了 CORS，但仅作用于 Spring MVC 层
- ❌ Spring Security 层未配置 CORS，可能导致预检通过但实际请求被拦截

**解决方案**：

```java
// SecurityConfiguration.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // 🔧 开发环境：允许所有来源
    configuration.setAllowedOriginPatterns(Arrays.asList("*"));
    
    // 🚨 生产环境：改为具体域名
    // configuration.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
    
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    ));
    
    configuration.setAllowedHeaders(Arrays.asList("*"));
    
    configuration.setExposedHeaders(Arrays.asList(
        "Authorization",        // JWT Token
        "X-Total-Count",        // 分页总数
        "Content-Disposition"   // 文件下载
    ));
    
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);  // 预检缓存1小时
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}

// 在 SecurityFilterChain 中应用
http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

**CORS 配置层级**：
```
浏览器请求
    ↓
┌─────────────────────────────────────┐
│ Spring Security CORS Filter         │  ← ✅ 新增（优先级高）
│ (SecurityConfiguration)             │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ JWT Authentication Filter           │
│ (JwtAuthenticationFilter)           │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ Spring MVC CORS                     │  ← 已移除（避免冲突）
│ (WebConfig - 已弃用)                │
└─────────────────────────────────────┘
    ↓
Controller
```

**前端配置示例**：

```javascript
// axios 配置
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',  // 后端地址
  withCredentials: true,  // 携带 Cookie
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器：自动添加 Token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器：处理 401/403
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // Token 过期，尝试刷新
      return refreshTokenAndRetry(error.config);
    } else if (error.response?.status === 403) {
      // 权限不足
      console.error('权限不足:', error.response.data.message);
    }
    return Promise.reject(error);
  }
);
```

**验证 CORS**：

```bash
# 测试预检请求
curl -X OPTIONS http://localhost:8080/events \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization" \
  -v

# 期望响应头：
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
# Access-Control-Allow-Headers: Authorization
# Access-Control-Allow-Credentials: true
```

---

### 2️⃣ 放行路径集中管理 ✅

**问题**：
```java
// ❌ 旧代码：路径分散，难以维护
authorize.requestMatchers("/api/v1/auth/register").permitAll();
authorize.requestMatchers("/api/v1/auth/authenticate").permitAll();
authorize.requestMatchers("/api/v1/auth/refresh-token").permitAll();
// ... 更多路径分散在各处
```

**解决方案**：
```java
// ✅ 新代码：集中管理，易于维护
private static final class SecurityPaths {
    // 认证端点（完全公开）
    public static final String[] AUTH_WHITELIST = {
        "/api/v1/auth/register",
        "/api/v1/auth/authenticate",
        "/api/v1/auth/refresh-token"
    };
    
    // 文档端点（开发环境）
    public static final String[] DOC_WHITELIST = {
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/**"
    };
    
    // 错误页面
    public static final String[] ERROR_PAGES = {
        "/error"
    };
}

// 使用集中管理的路径
authorize.requestMatchers(SecurityPaths.AUTH_WHITELIST).permitAll();
authorize.requestMatchers(SecurityPaths.ERROR_PAGES).permitAll();
```

**优势**：
- ✅ **可维护性**：所有路径集中在一处
- ✅ **可读性**：清晰的分类（认证/文档/错误）
- ✅ **可扩展性**：新增路径只需修改常量
- ✅ **安全性**：避免遗漏或重复配置

**环境差异化配置**：

```java
// 方案1: 使用 @Profile 条件化
@Configuration
public class SecurityConfiguration {
    
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) {
        // 开发环境：放行文档端点
        authorize.requestMatchers(SecurityPaths.DOC_WHITELIST).permitAll();
    }
    
    @Bean
    @Profile("prod")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) {
        // 生产环境：不放行文档端点
    }
}

// 方案2: 使用配置属性
@ConfigurationProperties(prefix = "application.security")
public class SecurityProperties {
    private boolean enableSwagger = false;
    private List<String> whitelistPaths = new ArrayList<>();
}
```

---

### 3️⃣ 刷新接口限流 ✅

**问题**：
- ❌ `/api/v1/auth/refresh-token` 无限流保护
- ❌ 可能被暴力滥用（批量刷新 Token）
- ❌ 单个用户短时间内可无限刷新

**解决方案**：

#### 方案1: Bucket4j（推荐，简单高效）

**1. 添加依赖**：
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

**2. 创建限流过滤器**：
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain) throws ServletException, IOException {
        
        if (!request.getRequestURI().equals("/api/v1/auth/refresh-token")) {
            chain.doFilter(request, response);
            return;
        }
        
        String key = getClientKey(request);
        Bucket bucket = cache.computeIfAbsent(key, k -> createBucket());
        
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"error\":\"Too Many Requests\",\"message\":\"请求过于频繁，请稍后再试\"}"
            );
        }
    }
    
    private Bucket createBucket() {
        // 每分钟10次，突发容量20次
        Bandwidth limit = Bandwidth.builder()
            .capacity(20)
            .refillIntervally(10, Duration.ofMinutes(1))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }
    
    private String getClientKey(HttpServletRequest request) {
        // 基于 IP + User-Agent
        return request.getRemoteAddr() + ":" + request.getHeader("User-Agent");
    }
}

// 注册过滤器
@Configuration
public class RateLimitConfig {
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
```

#### 方案2: Redis（分布式环境）

**1. 添加依赖**：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**2. 创建限流注解**：
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 10;
    int duration() default 1;
    TimeUnit unit() default TimeUnit.MINUTES;
}

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) 
            RequestContextHolder.currentRequestAttributes()).getRequest();
        
        String key = "rate_limit:" + request.getRemoteAddr() + ":" + request.getRequestURI();
        
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, rateLimit.duration(), rateLimit.unit());
        }
        
        if (count > rateLimit.limit()) {
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }
        
        return joinPoint.proceed();
    }
}

// 在 Controller 上使用
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    
    @RateLimit(limit = 10, duration = 1, unit = TimeUnit.MINUTES)
    @PostMapping("/refresh-token")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        authenticationService.refreshToken(request, response);
    }
}
```

#### 方案3: Spring Cloud Gateway（微服务）

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: refresh_token_route
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/refresh-token
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # 每秒放入令牌数
                redis-rate-limiter.burstCapacity: 20  # 令牌桶容量
                redis-rate-limiter.requestedTokens: 1 # 每次消耗令牌数
```

**限流策略对比**：

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **Bucket4j** | 简单、轻量、本地缓存 | 不支持分布式 | 单体应用 |
| **Redis** | 支持分布式、持久化 | 需要 Redis | 微服务/分布式 |
| **Gateway** | 网关层限流、统一管理 | 需要 Gateway | 微服务架构 |

---

### 4️⃣ 统一异常处理 ✅

**问题**：
```java
// ❌ 旧代码：认证/授权失败返回 HTML 错误页面或空响应
// 前端无法准确识别错误类型
```

**解决方案**：

#### AuthenticationEntryPoint（认证失败）

**触发场景**：
- 未登录访问受保护端点
- Token 无效/过期/格式错误
- 缺少 Authorization header

**返回示例**：
```json
{
  "error": "Unauthorized",
  "message": "认证失败，请先登录",
  "path": "/events",
  "status": 401,
  "timestamp": 1760345702000
}
```

**实现**：
```java
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

// 在 SecurityFilterChain 中应用
http.exceptionHandling(exception -> {
    exception.authenticationEntryPoint(authenticationEntryPoint());
});
```

#### AccessDeniedHandler（授权失败）

**触发场景**：
- 已登录但权限不足
- 角色不匹配（如：普通用户访问管理员端点）
- `@PreAuthorize("hasRole('ADMIN')")` 验证失败

**返回示例**：
```json
{
  "error": "Forbidden",
  "message": "权限不足，无法访问该资源",
  "path": "/admin/users",
  "status": 403,
  "timestamp": 1760345702000
}
```

**实现**：
```java
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

// 在 SecurityFilterChain 中应用
http.exceptionHandling(exception -> {
    exception.accessDeniedHandler(accessDeniedHandler());
});
```

**前端处理示例**：

```javascript
// axios 响应拦截器
api.interceptors.response.use(
  response => response,
  async error => {
    const { status, data } = error.response || {};
    
    if (status === 401) {
      // 认证失败：清除本地 Token，跳转登录
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      
      // 尝试刷新 Token
      if (data.message.includes('过期')) {
        return refreshTokenAndRetry(error.config);
      }
      
      // 跳转登录页
      router.push('/login');
      
    } else if (status === 403) {
      // 授权失败：显示权限不足提示
      ElMessage.error(data.message || '权限不足');
      router.push('/403');
    }
    
    return Promise.reject(error);
  }
);
```

**错误响应标准化**：

| 状态码 | 错误类型 | 场景 | 前端处理 |
|--------|----------|------|----------|
| **401** | Unauthorized | 未登录、Token 无效/过期 | 刷新 Token 或跳转登录 |
| **403** | Forbidden | 权限不足 | 提示用户、跳转 403 页面 |
| **400** | Bad Request | 参数错误 | 显示错误信息 |
| **500** | Internal Error | 服务器错误 | 提示用户稍后重试 |

---

### 5️⃣ 文档端点管理 ✅

**诊断结果**：
- ❌ 项目未安装 Swagger/OpenAPI（已验证 `pom.xml`）
- ✅ 配置已预留文档端点放行（注释状态）

**如需启用 Swagger**：

#### 1. 添加依赖

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

#### 2. 配置 OpenAPI

```java
@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SE331 Backend API")
                .version("1.0.0")
                .description("事件管理系统 API 文档"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

#### 3. 放行文档端点

```java
// SecurityConfiguration.java
authorize.requestMatchers(SecurityPaths.DOC_WHITELIST).permitAll();

// DOC_WHITELIST 已包含：
// - /v3/api-docs/**
// - /swagger-ui/**
// - /swagger-ui.html
```

#### 4. Controller 注解示例

```java
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "事件管理", description = "事件的 CRUD 操作")
public class EventController {
    
    @GetMapping
    @Operation(summary = "获取事件列表", description = "支持分页和搜索")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    public Page<Event> getEvents(
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size
    ) {
        return eventService.getEvents(page, size);
    }
}
```

#### 5. 访问文档

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

**生产环境建议**：

```java
// 方案1: 使用 @Profile 条件化
@Configuration
@Profile("dev")  // 只在开发环境启用
public class OpenAPIConfig { ... }

// 方案2: 添加基本认证
@Bean
@Profile("prod")
public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) {
    authorize
        .requestMatchers("/swagger-ui/**").hasRole("ADMIN")  // 只允许管理员访问
        .requestMatchers("/v3/api-docs/**").hasRole("ADMIN");
}

// 方案3: 完全禁用
# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

---

## 📊 优化前后对比

| 功能点 | 优化前 | 优化后 | 改进 |
|--------|--------|--------|------|
| **CORS** | 仅 MVC 层配置 | Security + MVC 双层配置 | ⭐⭐⭐⭐⭐ |
| **放行路径** | 分散配置 | 集中管理（常量） | ⭐⭐⭐⭐⭐ |
| **限流** | 无保护 | 提供3种方案 | ⭐⭐⭐⭐⭐ |
| **异常处理** | HTML/空响应 | 统一JSON响应 | ⭐⭐⭐⭐⭐ |
| **文档端点** | 未配置 | 预留+注释说明 | ⭐⭐⭐⭐ |

---

## 🧪 测试验证

### 1. CORS 跨域测试

```bash
# 测试预检请求（OPTIONS）
curl -X OPTIONS http://localhost:8080/events \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization" \
  -v

# ✅ 期望响应头
# HTTP/1.1 200
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
# Access-Control-Allow-Credentials: true

# 测试实际请求（GET）
curl http://localhost:3000/events \
  -H "Origin: http://localhost:3000" \
  -H "Authorization: Bearer <token>" \
  -v

# ✅ 期望响应头
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Expose-Headers: Authorization, X-Total-Count
```

### 2. 放行路径测试

```bash
# ✅ 应该放行（200/400）
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Authorization: Bearer <refresh_token>"

# ❌ 应该需要认证（401）
curl http://localhost:8080/events  # 无 Token

# ✅ 应该成功（200）
curl http://localhost:8080/events \
  -H "Authorization: Bearer <access_token>"
```

### 3. 异常处理测试

```bash
# 测试 401 - 认证失败
curl http://localhost:8080/events

# ✅ 期望响应
{
  "error": "Unauthorized",
  "message": "认证失败，请先登录",
  "path": "/events",
  "status": 401,
  "timestamp": 1760345702000
}

# 测试 403 - 权限不足（假设有 /admin 端点）
curl http://localhost:8080/admin/users \
  -H "Authorization: Bearer <user_token>"  # 普通用户 Token

# ✅ 期望响应
{
  "error": "Forbidden",
  "message": "权限不足，无法访问该资源",
  "path": "/admin/users",
  "status": 403,
  "timestamp": 1760345702000
}
```

### 4. 限流测试（如已实现）

```bash
# 快速连续请求
for i in {1..15}; do
  curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
    -H "Authorization: Bearer <token>"
done

# ✅ 前10次成功（200），后5次限流（429）
{
  "error": "Too Many Requests",
  "message": "请求过于频繁，请稍后再试"
}
```

---

## 🔒 生产环境清单

### 安全配置

- [ ] **CORS**
  - [ ] `allowedOrigins` 改为具体域名（不使用 `*`）
  - [ ] 移除开发环境的宽松配置
  - [ ] 验证 `allowCredentials: true` 的必要性

- [ ] **放行路径**
  - [ ] 审查所有 `permitAll()` 路径
  - [ ] 移除或保护文档端点
  - [ ] 使用 `@Profile("dev")` 条件化

- [ ] **限流**
  - [ ] 实现 `/refresh-token` 限流
  - [ ] 监控限流日志
  - [ ] 配置告警阈值

- [ ] **异常处理**
  - [ ] 错误信息脱敏（不暴露敏感信息）
  - [ ] 日志记录（包含请求上下文）
  - [ ] 监控 401/403 频率

- [ ] **JWT**
  - [ ] Secret Key 使用环境变量
  - [ ] Token 有效期调整（生产建议 15 分钟）
  - [ ] Refresh Token 有效期（生产建议 7 天）

### 监控与日志

```yaml
# application-prod.yml
logging:
  level:
    org.springframework.security: WARN
    se331.lab.rest.security: INFO
  file:
    name: /var/log/app/security.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# 监控指标
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

### 性能优化

- [ ] CORS 预检缓存（`maxAge: 3600`）
- [ ] Redis 缓存 Token 黑名单
- [ ] 数据库连接池优化
- [ ] 静态资源 CDN 加速

---

## 🎉 总结

### 已完成的优化

1. ✅ **CORS 跨域配置**
   - Security 层统一配置
   - 支持预检请求
   - 暴露必要的响应头

2. ✅ **放行路径集中管理**
   - 常量化配置
   - 清晰分类（认证/文档/错误）
   - 易于维护和扩展

3. ✅ **刷新接口限流建议**
   - 提供 3 种方案（Bucket4j/Redis/Gateway）
   - 详细实现代码
   - 适配不同架构

4. ✅ **统一异常处理**
   - AuthenticationEntryPoint（401）
   - AccessDeniedHandler（403）
   - 标准 JSON 响应

5. ✅ **文档端点管理**
   - 预留配置（注释状态）
   - Swagger 集成指南
   - 生产环境保护建议

### 关键改进

- **安全性** ⬆️ CORS 双层配置 + 异常处理脱敏
- **可维护性** ⬆️ 路径集中管理 + 清晰注释
- **可靠性** ⬆️ 限流保护 + 统一错误响应
- **可扩展性** ⬆️ 模块化配置 + 环境差异化

---

**📄 相关文档**：
- [JWT过滤器优化说明.md](./JWT过滤器优化说明.md) - JWT 过滤器优化
- [认证优化说明.md](./认证优化说明.md) - 认证服务优化
- [Firebase存储优化说明.md](./Firebase存储优化说明.md) - 存储服务优化
- [快速参考.md](./快速参考.md) - API 使用速查

