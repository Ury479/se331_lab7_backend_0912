# 🔐 认证请求处理代码分析

## 📋 图中操作分析
- **请求方式**: POST
- **请求URL**: http://localhost:8080/api/v1/auth/authenticate
- **请求体**: `{"username":"admin", "password":"admin"}`
- **返回状态**: 403 (图中) → 200 OK (修复后)

---

## 📂 处理此请求的源代码

### 1️⃣ **控制器层 - AuthenticationController.java**
**位置**: `/src/main/java/se331/lab/rest/security/auth/AuthenticationController.java`

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService service;

  @PostMapping("/authenticate")  // 处理 /api/v1/auth/authenticate 请求
  public ResponseEntity<AuthenticationResponse> authenticate(
      @RequestBody AuthenticationRequest request
  ) {
    AuthenticationResponse result = service.authenticate(request);
    return ResponseEntity.ok(result);
  }
}
```

**职责**: 
- 接收来自客户端的认证请求
- 调用 AuthenticationService 处理认证逻辑
- 返回包含 JWT token 的响应

---

### 2️⃣ **服务层 - AuthenticationService.java**
**位置**: `/src/main/java/se331/lab/rest/security/auth/AuthenticationService.java`

```java
@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final UserRepository repository;
  private final TokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    // 1. 验证用户名和密码
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getUsername(),
            request.getPassword()
        )
    );
    
    // 2. 从数据库查询用户
    User user = repository.findByUsername(request.getUsername())
        .orElseThrow();

    // 3. 生成 JWT access token
    String jwtToken = jwtService.generateToken(user);
    
    // 4. 生成 refresh token
    String refreshToken = jwtService.generateRefreshToken(user);
    
    // 5. 保存 token 到数据库
    saveUserToken(user, jwtToken);
    
    // 6. 返回响应
    return AuthenticationResponse.builder()
        .accessToken(jwtToken)
        .refreshToken(refreshToken)
        .build();
  }

  private void saveUserToken(User user, String jwtToken) {
    Token token = Token.builder()
        .user(user)
        .token(jwtToken)
        .tokenType(TokenType.BEARER)
        .expired(false)
        .revoked(false)
        .build();
    tokenRepository.save(token);
  }
}
```

**职责**:
- 使用 Spring Security 的 AuthenticationManager 验证凭证
- 从数据库查询用户信息
- 生成 JWT access token 和 refresh token
- 保存 token 到数据库
- 构建并返回认证响应

---

### 3️⃣ **请求/响应模型**

**AuthenticationRequest.java**:
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
  private String username;
  private String password;
}
```

**AuthenticationResponse.java**:
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
  private String accessToken;
  private String refreshToken;
}
```

---

### 4️⃣ **安全配置 - SecurityConfiguration.java**
**位置**: `/src/main/java/se331/lab/rest/security/config/SecurityConfiguration.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf((csrf) -> csrf.disable())
        .authorizeHttpRequests((authorize) -> {
          // ✅ 允许认证端点公开访问（修复后）
          authorize.requestMatchers("/api/v1/auth/**").permitAll();
          authorize.anyRequest().authenticated();
        })
        .sessionManagement((session) -> {
          session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        })
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
```

**关键配置**:
- CSRF 保护已禁用
- `/api/v1/auth/**` 端点允许未认证访问
- 使用无状态会话管理（JWT）
- 添加 JWT 认证过滤器

---

## 🔄 完整请求处理流程

```
1. 客户端发送 POST 请求
   ↓
2. SecurityFilterChain 检查权限
   - /api/v1/auth/** → permitAll() ✅
   ↓
3. AuthenticationController 接收请求
   - @PostMapping("/authenticate")
   ↓
4. AuthenticationService.authenticate()
   - 验证用户名/密码
   - 查询用户信息
   - 生成 JWT tokens
   - 保存 token
   ↓
5. 返回 AuthenticationResponse
   {
     "access_token": "eyJhbGci...",
     "refresh_token": "eyJhbGci..."
   }
   ↓
6. 客户端收到 200 OK 响应
```

---

## 🎯 实际测试结果

### 请求:
```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

### 响应:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTc2MDAxODk5NCwiZXhwIjoxNzYwMTA1Mzk0fQ.GjcEfijbQ-m9PJ20NZQewA1ePsRLxr3rJcW_NhiOJ1Q",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTc2MDAxODk5NCwiZXhwIjoxNzYwNjIzNzk0fQ.xY--KYDmOnYhu73J190VLx1T6XgbrCa5P5_flrPPhew"
}
```

**HTTP 状态码**: 200 OK ✅

---

## 📝 关键代码位置总结

| 组件 | 文件路径 | 行号 |
|------|---------|------|
| **控制器** | `se331/lab/rest/security/auth/AuthenticationController.java` | 27-33 |
| **服务** | `se331/lab/rest/security/auth/AuthenticationService.java` | 52-70 |
| **Security配置** | `se331/lab/rest/security/config/SecurityConfiguration.java` | 30-35 |
| **请求模型** | `se331/lab/rest/security/auth/AuthenticationRequest.java` | - |
| **响应模型** | `se331/lab/rest/security/auth/AuthenticationResponse.java` | - |

---

## 🔧 修复记录

**问题**: 图中显示 403 Forbidden

**原因**: Security 配置要求所有请求都需要认证，包括认证端点本身

**解决方案**: 
在 `SecurityConfiguration.java` 中添加：
```java
authorize.requestMatchers("/api/v1/auth/**").permitAll();
```

**结果**: 认证端点可以公开访问，成功返回 JWT tokens
