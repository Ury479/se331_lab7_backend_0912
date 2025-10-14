# JWT 认证模块测试报告

## 测试日期
2025-10-10

## 系统配置

### 服务器配置
- **端口**: 8080
- **数据库**: MySQL 8.0 (端口 3307)
- **认证方式**: JWT (JSON Web Token)

### JWT 配置
- **Secret Key**: 已配置 (application.yml)
- **Access Token 过期时间**: 24 小时 (86400000 ms)
- **Refresh Token 过期时间**: 7 天 (604800000 ms)

### 数据库配置
- **主数据库**: MySQL (用户认证、业务数据)
- **文件存储**: Firebase Storage (已配置)
- **数据库连接**: jdbc:mysql://localhost:3307/selabdb

## 测试账户

### 预配置用户
1. **管理员账户**
   - 用户名: admin
   - 密码: admin
   - 角色: ROLE_USER, ROLE_ADMIN
   
2. **普通用户**
   - 用户名: user
   - 密码: user
   - 角色: ROLE_USER

3. **禁用用户**
   - 用户名: disableUser
   - 密码: disableUser
   - 角色: ROLE_USER
   - 状态: 已禁用

## 测试结果

### 1. 登录测试 ✅

#### 测试 1.1: Admin 账户登录
```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

**结果**: ✅ 成功
- 返回 access_token
- 返回 refresh_token
- HTTP Status: 200

#### 测试 1.2: User 账户登录
```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user"}'
```

**结果**: ✅ 成功
- 返回 access_token
- 返回 refresh_token
- HTTP Status: 200

### 2. Token 验证测试 ✅

#### 测试 2.1: 使用有效 Token 访问受保护接口
```bash
curl -X GET http://localhost:8080/events \
  -H "Authorization: Bearer [valid_token]"
```

**结果**: ✅ 成功
- 能够正常访问受保护的接口
- 返回数据（空数组 []，因为数据库中暂无数据）
- HTTP Status: 200

#### 测试 2.2: 不带 Token 访问受保护接口
```bash
curl -X GET http://localhost:8080/events
```

**结果**: ✅ 按预期被拒绝
- HTTP Status: 403 Forbidden
- 无法访问受保护的接口

### 3. Refresh Token 测试 ✅

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Authorization: Bearer [refresh_token]"
```

**结果**: ✅ 成功
- 成功使用 refresh token 获取新的 access token
- 返回新的 access_token
- 返回相同的 refresh_token
- HTTP Status: 200

### 4. Logout 测试 ✅

#### 测试 4.1: 执行 Logout
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer [access_token]"
```

**结果**: ✅ 成功
- HTTP Status: 200
- Token 被标记为已撤销

#### 测试 4.2: 使用已撤销的 Token 访问接口
```bash
curl -X GET http://localhost:8080/events \
  -H "Authorization: Bearer [revoked_token]"
```

**结果**: ✅ 按预期被拒绝
- HTTP Status: 403 Forbidden
- 已撤销的 token 无法访问受保护的接口

## API 端点

### 认证相关 (无需 Token)
- `POST /api/v1/auth/register` - 用户注册
- `POST /api/v1/auth/authenticate` - 用户登录
- `POST /api/v1/auth/refresh-token` - 刷新 token

### 登出 (需要 Token)
- `POST /api/v1/auth/logout` - 用户登出

### 受保护的端点 (需要 Token)
- `GET /events` - 获取事件列表
- `GET /organizers` - 获取组织者列表
- 其他业务接口...

## 安全配置

### SecurityConfiguration 设置
- ✅ CSRF 已禁用 (适用于 REST API)
- ✅ Session 管理: STATELESS (无状态)
- ✅ JWT 过滤器已启用
- ✅ 认证路径 `/api/v1/auth/**` 允许匿名访问
- ✅ OPTIONS 请求允许匿名访问（CORS 预检）
- ✅ 其他所有请求需要认证

### Token 验证流程
1. 客户端在请求头中携带 `Authorization: Bearer [token]`
2. JwtAuthenticationFilter 拦截请求
3. 提取并验证 token
4. 检查 token 是否在数据库中且未被撤销
5. 如果验证通过，设置 Spring Security Context
6. 允许访问受保护的资源

### Token 撤销机制
- ✅ 登录时撤销用户的所有旧 token
- ✅ Logout 时将当前 token 标记为已撤销和已过期
- ✅ 数据库中存储所有有效 token
- ✅ 验证时检查 token 是否被撤销

## 改进建议

### 已实现的安全特性 ✅
- JWT 认证和授权
- Token 自动刷新机制
- Token 撤销机制
- 密码加密 (BCrypt)
- 无状态会话管理

### 可选的增强功能
1. 添加角色基础的访问控制 (ROLE_ADMIN vs ROLE_USER)
2. 添加 IP 白名单
3. 添加登录失败次数限制
4. 添加 Token 黑名单机制（Redis）
5. 添加审计日志

## 结论

✅ **JWT 认证模块已成功恢复并正常工作**

- 所有核心功能测试通过
- Access Token 生效正常
- Token 验证机制工作正常
- Token 撤销机制有效
- 安全配置符合最佳实践

## 使用示例

### 完整的认证流程示例

1. **登录获取 Token**
```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

2. **使用 Token 访问 API**
```bash
export TOKEN="your_access_token_here"
curl -X GET http://localhost:8080/events \
  -H "Authorization: Bearer $TOKEN"
```

3. **刷新 Token**
```bash
export REFRESH_TOKEN="your_refresh_token_here"
curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Authorization: Bearer $REFRESH_TOKEN"
```

4. **登出**
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

