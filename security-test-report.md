# Spring Security 测试报告

## 1.4 重启后端服务器测试结果

### 问题：前端无法访问数据，HTTP 返回码是什么？

**回答：HTTP 401 Unauthorized**

### 测试详情：

#### 测试 1: GET /events
```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Basic realm="Realm"
```

#### 测试 2: GET /organizers
```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Basic realm="Realm"
```

## 1.5 使用 REST 客户端调用后端事件接口

### 问题：它们返回什么？

**回答：所有端点都返回 HTTP 401 Unauthorized**

这是因为 Spring Security 已配置为要求所有请求都需要身份验证：

```java
// SecurityConfiguration.java 第 34 行
authorize.anyRequest().authenticated();
```

### 原因分析：

1. **Spring Security 已启用**：所有请求都需要身份验证
2. **没有提供认证令牌**：请求中没有 JWT token 或 Basic Auth 凭证
3. **返回 401**：Spring Security 自动拒绝未经身份验证的请求

### 如何访问数据：

要访问这些端点，需要：
1. 先调用认证端点获取 JWT token
2. 在后续请求的 Header 中包含该 token
3. 或者修改 SecurityConfiguration 允许某些端点公开访问

### Security 配置位置：
- `/src/main/java/se331/lab/rest/security/config/SecurityConfiguration.java`
- JWT 配置：`/src/main/resources/application.yml`
