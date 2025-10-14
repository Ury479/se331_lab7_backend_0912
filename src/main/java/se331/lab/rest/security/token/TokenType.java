package se331.lab.rest.security.token;

public enum TokenType {
  BEARER,      // 保留兼容性
  ACCESS,      // Access Token
  REFRESH      // Refresh Token
}
