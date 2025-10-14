package se331.lab.rest.security.auth;



import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se331.lab.rest.security.config.JwtService;
import se331.lab.rest.security.token.Token;
import se331.lab.rest.security.token.TokenRepository;
import se331.lab.rest.security.token.TokenType;
import se331.lab.rest.security.user.Role;
import se331.lab.rest.security.user.User;
import se331.lab.rest.security.user.UserRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//JWT 验证核心代码
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final UserRepository repository;
  private final TokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  @Transactional
  public AuthenticationResponse register(RegisterRequest request) {
    // 🔧 修复1: username vs email 一致性 - 统一使用 email 作为 username
    User user = User.builder()
            .username(request.getEmail())  // ✅ 设置 username = email
            .firstname(request.getFirstname())
            .lastname(request.getLastname())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .roles(List.of(Role.ROLE_USER))
            .build();
    var savedUser = repository.save(user);
    var jwtToken = jwtService.generateToken(user);
    var refreshToken = jwtService.generateRefreshToken(user);
    
    // 保存 access token 和 refresh token
    saveUserToken(savedUser, jwtToken, TokenType.ACCESS);
    saveUserToken(savedUser, refreshToken, TokenType.REFRESH);
    
    log.info("User registered successfully: {}", savedUser.getUsername());
    return AuthenticationResponse.builder()
        .accessToken(jwtToken)
            .refreshToken(refreshToken)
        .build();
  }

  @Transactional
  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
            )
    );
    User user = repository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found: " + request.getUsername()));

    String jwtToken = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    
    // 🔧 修复4: 事务一致性 - 撤销旧token和保存新token在同一事务中
    revokeAllUserTokens(user);
    saveUserToken(user, jwtToken, TokenType.ACCESS);
    saveUserToken(user, refreshToken, TokenType.REFRESH);
    
    log.info("User authenticated successfully: {}", user.getUsername());
    return AuthenticationResponse.builder()
            .accessToken(jwtToken)
            .refreshToken(refreshToken)
            .build();
  }

  // 🔧 修复7: 持久化 access token 和 refresh token，支持撤销
  private void saveUserToken(User user, String jwtToken, TokenType tokenType) {
    Token token = Token.builder()
            .user(user)
            .token(jwtToken)
            .tokenType(tokenType)
            .expired(false)
            .revoked(false)
            .build();
    tokenRepository.save(token);
    log.debug("Saved {} token for user: {}", tokenType, user.getUsername());
  }

  private void revokeAllUserTokens(User user) {
    List<Token> validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
    if (validUserTokens.isEmpty())
      return;
    validUserTokens.forEach(token -> {
      token.setExpired(true);
      token.setRevoked(true);
    });
    tokenRepository.saveAll(validUserTokens);
  }

  @Transactional
  public void refreshToken(
          HttpServletRequest request,
          HttpServletResponse response
  ) throws IOException {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    
    // 🔧 修复3: 响应与错误码 - 返回明确的错误响应
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      sendErrorResponse(response, HttpStatus.BAD_REQUEST, "Missing or invalid Authorization header");
      return;
    }
    
    final String refreshToken = authHeader.substring(7);
    final String username;
    
    try {
      username = jwtService.extractUsername(refreshToken);
    } catch (Exception e) {
      log.error("Failed to extract username from refresh token", e);
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid refresh token");
      return;
    }
    
    if (username == null) {
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid refresh token");
      return;
    }
    
    // 🔧 修复1: 使用 findByUsername 保持一致性
    User user = this.repository.findByUsername(username)
            .orElse(null);
    
    if (user == null) {
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "User not found");
      return;
    }
    
    // 🔧 修复7: 验证 refresh token 是否在数据库中且有效
    boolean isRefreshTokenValid = tokenRepository.findByToken(refreshToken)
            .map(t -> t.getTokenType() == TokenType.REFRESH && !t.isExpired() && !t.isRevoked())
            .orElse(false);
    
    if (!jwtService.isTokenValid(refreshToken, user) || !isRefreshTokenValid) {
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
      return;
    }
    
    // 🔧 修复2: 刷新令牌旋转 - 生成新的 access token 和 refresh token
    String newAccessToken = jwtService.generateToken(user);
    String newRefreshToken = jwtService.generateRefreshToken(user);
    
    // 🔧 修复4 & 7: 事务一致性 - 撤销旧token + 保存新token
    revokeAllUserTokens(user);  // 撤销所有旧的 token
    saveUserToken(user, newAccessToken, TokenType.ACCESS);
    saveUserToken(user, newRefreshToken, TokenType.REFRESH);
    
    log.info("Token refreshed successfully for user: {}", username);
    
    // 🔧 修复6: 设置正确的 Content-Type
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpStatus.OK.value());
    
    AuthenticationResponse authResponse = AuthenticationResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)  // 返回新的 refresh token
            .build();
    
    new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
  }
  
  // 🔧 修复3: 统一错误响应格式
  private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(status.value());
    
    Map<String, Object> errorBody = new HashMap<>();
    errorBody.put("error", status.getReasonPhrase());
    errorBody.put("message", message);
    errorBody.put("status", status.value());
    errorBody.put("timestamp", System.currentTimeMillis());
    
    new ObjectMapper().writeValue(response.getOutputStream(), errorBody);
    log.warn("Refresh token error: {} - {}", status, message);
  }
}
