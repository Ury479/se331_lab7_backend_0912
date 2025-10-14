# JWT 认证模块配置说明

## 📋 概述

本项目已成功配置并启用 JWT (JSON Web Token) 认证模块，用于保护 REST API 接口。

## 🔧 核心配置

### 1. 应用配置 (application.yml)

```yaml
server:
  port: 8080  # 应用端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3307/selabdb
    username: root
    password: cmu123wyh
  jpa:
    hibernate:
      ddl-auto: create  # 每次启动时重建数据库表

application:
  security:
    jwt:
      secret-key: [已配置 256位密钥]
      expiration: 86400000  # Access Token: 24小时
      refresh-token:
        expiration: 604800000  # Refresh Token: 7天
```

### 2. 数据库配置

#### MySQL 数据库
- **主机**: localhost:3307
- **数据库名**: selabdb
- **用户**: root
- **密码**: cmu123wyh
- **用途**: 用户认证、Token 存储、业务数据

#### Firebase Storage
- **用途**: 文件存储
- **配置文件**: se331lab10-firebase-adminsdk-fbsvc-d273d10d80.json
- **项目ID**: se331lab10
- **状态**: ✅ 已初始化成功

### 3. 安全配置

#### SecurityConfiguration 主要设置：
- ✅ JWT 认证已启用
- ✅ 无状态会话 (STATELESS)
- ✅ CSRF 禁用（REST API 模式）
- ✅ 自动 Token 撤销机制

#### 公开路径（无需认证）：
- `/api/v1/auth/**` - 认证相关接口
- `OPTIONS /**` - CORS 预检请求

#### 受保护路径（需要 Token）：
- 所有其他接口都需要有效的 JWT Token

## 👥 预配置用户

### 管理员账户
- **用户名**: `admin`
- **密码**: `admin`
- **角色**: `ROLE_USER`, `ROLE_ADMIN`
- **状态**: 启用

### 普通用户
- **用户名**: `user`
- **密码**: `user`
- **角色**: `ROLE_USER`
- **状态**: 启用

### 禁用用户
- **用户名**: `disableUser`
- **密码**: `disableUser`
- **角色**: `ROLE_USER`
- **状态**: 禁用

## 🔐 JWT Token 说明

### Access Token
- **用途**: 访问受保护的 API 接口
- **有效期**: 24 小时
- **格式**: `Bearer eyJhbGciOiJIUzI1NiJ9...`
- **存储位置**: 客户端（LocalStorage 或 Cookie）

### Refresh Token
- **用途**: 获取新的 Access Token
- **有效期**: 7 天
- **使用场景**: Access Token 过期时刷新

## 📡 API 接口

### 认证接口（公开）

#### 1. 用户登录
```bash
POST /api/v1/auth/authenticate
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}

# 响应
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### 2. 用户注册
```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "firstname": "张三",
  "lastname": "李四",
  "email": "zhangsan@example.com",
  "password": "password123"
}
```

#### 3. 刷新 Token
```bash
POST /api/v1/auth/refresh-token
Authorization: Bearer [refresh_token]

# 响应
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### 4. 用户登出
```bash
POST /api/v1/auth/logout
Authorization: Bearer [access_token]
```

### 受保护接口（需要 Token）

所有业务接口都需要在请求头中携带有效的 Access Token：

```bash
GET /events
Authorization: Bearer [access_token]

GET /organizers
Authorization: Bearer [access_token]
```

## 🔄 认证流程

### 1. 登录流程
```
客户端                              服务器
  |                                   |
  |------ POST /authenticate -------->|
  |  { username, password }           |
  |                                   |
  |<----- 200 OK ---------------------|
  |  { access_token, refresh_token }  |
  |                                   |
```

### 2. 访问受保护资源
```
客户端                              服务器
  |                                   |
  |------ GET /events --------------->|
  |  Authorization: Bearer [token]    |
  |                                   |
  |  [JWT Filter 验证 Token]          |
  |  [检查 Token 是否有效]            |
  |  [检查 Token 是否被撤销]          |
  |                                   |
  |<----- 200 OK ---------------------|
  |  [返回数据]                       |
  |                                   |
```

### 3. Token 刷新流程
```
客户端                              服务器
  |                                   |
  |------ POST /refresh-token ------->|
  |  Authorization: Bearer [refresh]  |
  |                                   |
  |  [验证 Refresh Token]             |
  |  [撤销旧的 Access Token]          |
  |  [生成新的 Access Token]          |
  |                                   |
  |<----- 200 OK ---------------------|
  |  { access_token, refresh_token }  |
  |                                   |
```

### 4. 登出流程
```
客户端                              服务器
  |                                   |
  |------ POST /logout --------------->|
  |  Authorization: Bearer [token]    |
  |                                   |
  |  [标记 Token 为已撤销]            |
  |  [标记 Token 为已过期]            |
  |  [保存到数据库]                   |
  |                                   |
  |<----- 200 OK ---------------------|
  |                                   |
```

## 🛡️ 安全特性

### 已实现的安全措施

1. **密码加密**
   - 使用 BCrypt 算法
   - 加密强度：默认 10 轮

2. **Token 签名**
   - 算法：HMAC-SHA256 (HS256)
   - 密钥：256位随机密钥

3. **Token 验证**
   - 验证签名完整性
   - 验证过期时间
   - 验证用户存在性
   - 检查 Token 撤销状态

4. **Token 撤销机制**
   - 登录时自动撤销旧 Token
   - 登出时撤销当前 Token
   - 数据库持久化撤销记录

5. **无状态会话**
   - 不使用 HTTP Session
   - 完全基于 Token 的认证

## 🚀 快速开始

### 1. 启动数据库
```bash
docker-compose up -d
```

### 2. 编译项目
```bash
./mvnw clean package -DskipTests
```

### 3. 运行应用
```bash
java -jar target/demo-331-bacnkend-0.0.1-SNAPSHOT.jar
```

### 4. 测试认证
```bash
# 登录
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# 保存返回的 access_token
export TOKEN="返回的access_token"

# 访问受保护接口
curl -X GET http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN"
```

## 🔍 故障排查

### 问题 1: 403 Forbidden
**原因**: 未提供 Token 或 Token 无效
**解决**: 确保在请求头中正确携带 `Authorization: Bearer [token]`

### 问题 2: Token 过期
**原因**: Access Token 超过 24 小时
**解决**: 使用 Refresh Token 获取新的 Access Token

### 问题 3: 401 Unauthorized
**原因**: 用户名或密码错误
**解决**: 检查登录凭证是否正确

### 问题 4: Token 被撤销
**原因**: 已经登出或重新登录
**解决**: 重新登录获取新的 Token

## 📊 Token 验证日志

应用会在以下情况记录日志：
- 用户登录成功
- Token 验证失败
- Token 被撤销
- 刷新 Token

查看日志：
```bash
tail -f spring-boot-new.log
```

## 🔄 自动刷新 Token 策略

### 推荐的客户端实现

```javascript
// 前端示例（JavaScript）
async function fetchWithAuth(url, options = {}) {
  // 1. 获取 access token
  let accessToken = localStorage.getItem('access_token');
  
  // 2. 添加 Authorization 头
  const headers = {
    ...options.headers,
    'Authorization': `Bearer ${accessToken}`
  };
  
  // 3. 发送请求
  let response = await fetch(url, { ...options, headers });
  
  // 4. 如果 401/403，尝试刷新 token
  if (response.status === 401 || response.status === 403) {
    const refreshToken = localStorage.getItem('refresh_token');
    const refreshResponse = await fetch('/api/v1/auth/refresh-token', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${refreshToken}`
      }
    });
    
    if (refreshResponse.ok) {
      const data = await refreshResponse.json();
      localStorage.setItem('access_token', data.access_token);
      
      // 5. 使用新 token 重试原请求
      headers['Authorization'] = `Bearer ${data.access_token}`;
      response = await fetch(url, { ...options, headers });
    }
  }
  
  return response;
}
```

## 📝 注意事项

1. ⚠️ **Secret Key 安全**
   - 生产环境应使用环境变量存储
   - 不要将密钥提交到代码仓库

2. ⚠️ **Token 存储**
   - 客户端应安全存储 Token
   - 考虑使用 HttpOnly Cookie

3. ⚠️ **HTTPS**
   - 生产环境务必使用 HTTPS
   - 防止 Token 被中间人攻击

4. ⚠️ **Token 过期时间**
   - 根据业务需求调整
   - 平衡安全性和用户体验

5. ⚠️ **数据库配置**
   - `ddl-auto: create` 会删除所有数据
   - 生产环境改为 `update` 或 `validate`

## ✅ 测试清单

- [x] 登录功能正常
- [x] Token 生成正常
- [x] Token 验证正常
- [x] 受保护接口拦截正常
- [x] Refresh Token 功能正常
- [x] Logout 功能正常
- [x] Token 撤销机制正常
- [x] 数据库连接正常
- [x] Firebase Storage 初始化正常

## 🎉 总结

JWT 认证模块已成功配置并通过所有测试。系统使用：
- ✅ MySQL 作为主数据库（用户、Token、业务数据）
- ✅ Firebase Storage 用于文件存储
- ✅ 端口 8080 提供服务
- ✅ 完整的 Token 生命周期管理
- ✅ 安全的密码加密和 Token 签名

详细测试结果请查看：`jwt-test-report.md`

