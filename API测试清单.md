# API 测试完整清单

## 📋 测试内容总览

基于当前项目实现的功能，API 测试应该覆盖以下方面：

1. **功能测试** - API 是否按预期工作
2. **安全测试** - 认证和授权是否有效
3. **数据验证测试** - 输入输出是否正确
4. **错误处理测试** - 异常情况是否正确处理
5. **性能测试** - 响应时间是否合理
6. **集成测试** - 各模块是否正确协作

---

## 🔐 1. 认证模块测试

### 1.1 用户注册 API

**端点**: `POST /api/v1/auth/register`

#### ✅ 正常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 1.1.1**: 成功注册新用户

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "John",
    "lastname": "Doe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ 返回字段包含:
  - `access_token` (非空字符串)
  - `refresh_token` (非空字符串)
  - `user` (对象)
    - `id` (整数)
    - `username` (字符串)
    - `email` (字符串)
    - `roles` (数组，至少包含 "ROLE_USER")
  - `organizer` (可能为 null)

</details>

#### ❌ 异常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 1.1.2**: 注册重复邮箱

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "Admin",
    "lastname": "User",
    "email": "admin@admin.com",
    "password": "password123"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `400 Bad Request` 或 `409 Conflict`
- ✅ 错误信息: "Email already exists" 或类似提示

---

**测试用例 1.1.3**: 缺少必填字段

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@test.com"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `400 Bad Request`
- ✅ 错误信息指出缺少的字段

---

**测试用例 1.1.4**: 无效邮箱格式

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "Test",
    "lastname": "User",
    "email": "invalid-email",
    "password": "password123"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `400 Bad Request`
- ✅ 错误信息: "Invalid email format"

---

**测试用例 1.1.5**: 密码过于简单

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "Test",
    "lastname": "User",
    "email": "test@example.com",
    "password": "123"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `400 Bad Request`
- ✅ 错误信息: "Password too weak" 或类似提示

</details>

---

### 1.2 用户登录 API

**端点**: `POST /api/v1/auth/authenticate`

#### ✅ 正常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 1.2.1**: 使用正确的用户名和密码登录

```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ 返回字段:
  ```json
  {
    "access_token": "eyJhbGciOiJIUzI1NiJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "firstname": "admin",
      "lastname": "admin",
      "email": "admin@admin.com",
      "roles": ["ROLE_USER", "ROLE_ADMIN"]
    },
    "organizer": {
      "id": 1,
      "organizationName": "CAMT",
      "address": "239 Huay Kaew Rd, Suthep, Muang, Chiang Mai",
      "phone": null,
      "website": null,
      "image": null
    }
  }
  ```

---

**测试用例 1.2.2**: 普通用户登录

```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "user"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ `user.roles` 包含 `["ROLE_USER"]`
- ✅ 返回关联的 `organizer` 信息

</details>

#### ❌ 异常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 1.2.3**: 错误的密码

```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "wrongpassword"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`
- ✅ 错误信息: "Invalid credentials" 或 "认证失败"

---

**测试用例 1.2.4**: 不存在的用户名

```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "nonexistent",
    "password": "password"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`
- ✅ 不应泄露用户是否存在（统一返回 "Invalid credentials"）

---

**测试用例 1.2.5**: 缺少必填字段

```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `400 Bad Request`

---

**测试用例 1.2.6**: 禁用的用户登录

```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "disableUser",
    "password": "disableUser"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized` 或 `403 Forbidden`
- ✅ 错误信息: "Account disabled"

</details>

---

### 1.3 Token 刷新 API

**端点**: `POST /api/v1/auth/refresh-token`

#### ✅ 正常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 1.3.1**: 使用有效的 Refresh Token 刷新

```bash
# 1. 先登录获取 refresh_token
REFRESH_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.refresh_token')

# 2. 使用 refresh_token 刷新
curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Authorization: Bearer $REFRESH_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ 返回新的 `access_token` 和 `refresh_token`
- ✅ 旧的 `refresh_token` 被撤销（实现 Token 旋转）

</details>

#### ❌ 异常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 1.3.2**: 使用过期的 Refresh Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Authorization: Bearer expired.refresh.token"
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`
- ✅ 错误信息: "Token expired"

---

**测试用例 1.3.3**: 使用 Access Token 而非 Refresh Token

```bash
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.access_token')

curl -X POST http://localhost:8080/api/v1/auth/refresh-token \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized` 或 `400 Bad Request`
- ✅ 错误信息: "Invalid token type"

---

**测试用例 1.3.4**: 缺少 Authorization Header

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh-token
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`

</details>

---

### 1.4 用户登出 API

**端点**: `POST /api/v1/auth/logout`

#### ✅ 正常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 1.4.1**: 成功登出

```bash
# 1. 登录获取 token
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.access_token')

# 2. 登出
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ 用户的所有 token 被撤销

---

**测试用例 1.4.2**: 登出后 Token 不可用

```bash
# 登出后再次使用 token
curl http://localhost:8080/events \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`

</details>

---

## 🔒 2. 受保护资源测试

### 2.1 获取事件列表 API

**端点**: `GET /events`

#### ✅ 正常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 2.1.1**: 使用有效 Token 获取事件列表

```bash
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.access_token')

curl http://localhost:8080/events \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ 返回事件数组
- ✅ 每个事件包含必要字段（id, title, description, etc.）

---

**测试用例 2.1.2**: 分页测试

```bash
curl "http://localhost:8080/events?page=0&size=2" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ 返回 2 条数据
- ✅ 响应头包含 `x-total-count`

---

**测试用例 2.1.3**: 搜索测试

```bash
curl "http://localhost:8080/events?title=Exam" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ 返回包含 "Exam" 的事件

</details>

#### ❌ 异常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 2.1.4**: 未提供 Token

```bash
curl http://localhost:8080/events
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`
- ✅ JSON 错误响应:
  ```json
  {
    "error": "Unauthorized",
    "message": "认证失败，请先登录",
    "status": 401,
    "timestamp": 1760517547000
  }
  ```

---

**测试用例 2.1.5**: 使用无效的 Token

```bash
curl http://localhost:8080/events \
  -H "Authorization: Bearer invalid.token.here"
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`

---

**测试用例 2.1.6**: 使用过期的 Token

```bash
curl http://localhost:8080/events \
  -H "Authorization: Bearer expired.token.here"
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`
- ✅ 错误信息: "Token expired"

</details>

---

### 2.2 获取单个事件 API

**端点**: `GET /events/{id}`

#### ✅ 正常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 2.2.1**: 获取存在的事件

```bash
curl http://localhost:8080/events/1 \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ 返回事件详情

</details>

#### ❌ 异常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 2.2.2**: 获取不存在的事件

```bash
curl http://localhost:8080/events/999999 \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `404 Not Found`

---

**测试用例 2.2.3**: 无效的 ID 格式

```bash
curl http://localhost:8080/events/abc \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `400 Bad Request`

</details>

---

### 2.3 获取组织者列表 API

**端点**: `GET /organizers`

#### ✅ 正常场景测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 2.3.1**: 获取所有组织者

```bash
curl http://localhost:8080/organizers \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ 返回组织者数组
- ✅ 每个组织者包含: id, organizationName, address, etc.

---

**测试用例 2.3.2**: 分页测试

```bash
curl "http://localhost:8080/organizers?page=0&size=3" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ 返回 3 条数据
- ✅ 响应头包含 `x-total-count`

</details>

---

## 🌐 3. CORS 测试

### 3.1 预检请求测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 3.1.1**: OPTIONS 预检请求

```bash
curl -X OPTIONS http://localhost:8080/events \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization" \
  -v
```

**期望结果**:
- ✅ HTTP 状态码: `200 OK`
- ✅ 响应头包含:
  ```
  Access-Control-Allow-Origin: http://localhost:5173
  Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
  Access-Control-Allow-Headers: *
  Access-Control-Allow-Credentials: true
  Access-Control-Expose-Headers: x-total-count
  ```

---

**测试用例 3.1.2**: 不允许的来源

```bash
curl -X OPTIONS http://localhost:8080/events \
  -H "Origin: http://malicious-site.com" \
  -v
```

**期望结果**:
- ✅ 没有 `Access-Control-Allow-Origin` 头，或者明确拒绝

</details>

---

## 🔐 4. 安全测试

### 4.1 SQL 注入测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 4.1.1**: 登录时尝试 SQL 注入

```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin\" OR \"1\"=\"1",
    "password": "anything"
  }'
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`
- ✅ 登录失败（使用参数化查询应该防止 SQL 注入）

---

**测试用例 4.1.2**: 搜索时尝试 SQL 注入

```bash
curl "http://localhost:8080/events?title='; DROP TABLE events; --" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ 不应该执行 SQL 命令
- ✅ 返回空结果或错误，但数据库完整

</details>

---

### 4.2 XSS 测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 4.2.1**: 注册时插入 XSS 脚本

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "<script>alert(\"XSS\")</script>",
    "lastname": "User",
    "email": "xss@test.com",
    "password": "password123"
  }'
```

**期望结果**:
- ✅ 数据被转义或拒绝
- ✅ 响应中不包含可执行脚本

</details>

---

### 4.3 暴力破解防护测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 4.3.1**: 连续失败登录尝试

```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/v1/auth/authenticate \
    -H "Content-Type: application/json" \
    -d '{
      "username": "admin",
      "password": "wrongpassword'$i'"
    }'
  echo "Attempt $i"
  sleep 1
done
```

**期望结果**:
- ✅ 应该实现速率限制或账户锁定
- ✅ 多次失败后返回 `429 Too Many Requests` 或锁定账户

</details>

---

### 4.4 Token 安全测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 4.4.1**: Token 篡改测试

```bash
# 修改 Token 的某些字符
TAMPERED_TOKEN="eyJhbGciOiJIUzI1NiJ9.TAMPERED.DATA"

curl http://localhost:8080/events \
  -H "Authorization: Bearer $TAMPERED_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `401 Unauthorized`
- ✅ Token 签名验证失败

---

**测试用例 4.4.2**: 使用其他用户的 Token

```bash
# 用普通用户的 token 访问管理员资源
USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user"}' | jq -r '.access_token')

# 尝试访问需要 ADMIN 权限的资源（如果有的话）
curl -X DELETE http://localhost:8080/events/1 \
  -H "Authorization: Bearer $USER_TOKEN"
```

**期望结果**:
- ✅ HTTP 状态码: `403 Forbidden`
- ✅ 错误信息: "权限不足"

</details>

---

## ⚡ 5. 性能测试

### 5.1 响应时间测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 5.1.1**: 登录响应时间

```bash
time curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' \
  -w "\nTime: %{time_total}s\n"
```

**期望结果**:
- ✅ 响应时间 < 1 秒

---

**测试用例 5.1.2**: 获取列表响应时间

```bash
curl http://localhost:8080/events \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -w "\nTime: %{time_total}s\n"
```

**期望结果**:
- ✅ 响应时间 < 500ms

</details>

---

### 5.2 并发测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 5.2.1**: 并发登录请求

```bash
# 使用 Apache Bench (ab) 或类似工具
ab -n 100 -c 10 -p login.json -T application/json \
  http://localhost:8080/api/v1/auth/authenticate
```

**期望结果**:
- ✅ 所有请求成功处理
- ✅ 无数据库连接池耗尽
- ✅ 无内存溢出

</details>

---

## 📊 6. 数据验证测试

### 6.1 响应格式验证

<details>
<summary>点击展开测试用例</summary>

**测试用例 6.1.1**: JSON 格式验证

```bash
curl -s http://localhost:8080/events \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

**期望结果**:
- ✅ 返回有效的 JSON
- ✅ 符合预期的数据结构

---

**测试用例 6.1.2**: Content-Type 验证

```bash
curl -I http://localhost:8080/events \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**期望结果**:
- ✅ `Content-Type: application/json`

</details>

---

### 6.2 数据完整性验证

<details>
<summary>点击展开测试用例</summary>

**测试用例 6.2.1**: 用户角色正确返回

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq '.user.roles'
```

**期望结果**:
- ✅ 返回 `["ROLE_USER", "ROLE_ADMIN"]`

---

**测试用例 6.2.2**: 组织者信息正确返回

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq '.organizer'
```

**期望结果**:
- ✅ 返回组织者对象
- ✅ 包含 id, organizationName, address 等字段
- ✅ **无循环引用**（避免堆栈溢出）

</details>

---

## 🧪 7. 边界测试

### 7.1 超大请求测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 7.1.1**: 超长字符串

```bash
LONG_STRING=$(python3 -c "print('A' * 10000)")

curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{
    \"firstname\": \"$LONG_STRING\",
    \"lastname\": \"User\",
    \"email\": \"test@test.com\",
    \"password\": \"password123\"
  }"
```

**期望结果**:
- ✅ HTTP 状态码: `400 Bad Request`
- ✅ 错误信息: "Field too long"

</details>

---

### 7.2 特殊字符测试

<details>
<summary>点击展开测试用例</summary>

**测试用例 7.2.1**: Unicode 字符

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "测试",
    "lastname": "用户",
    "email": "test@test.com",
    "password": "password123"
  }'
```

**期望结果**:
- ✅ 正确处理 Unicode 字符
- ✅ 数据库正确存储和检索

</details>

---

## 📝 8. 测试脚本示例

### 完整的集成测试脚本

创建文件: `test-api.sh`

```bash
#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"
PASSED=0
FAILED=0

# 测试函数
test_case() {
    local name="$1"
    local expected_status="$2"
    local actual_status="$3"
    
    if [ "$expected_status" -eq "$actual_status" ]; then
        echo -e "${GREEN}✅ PASS${NC}: $name"
        ((PASSED++))
    else
        echo -e "${RED}❌ FAIL${NC}: $name (Expected: $expected_status, Got: $actual_status)"
        ((FAILED++))
    fi
}

echo "=========================================="
echo "  API 测试开始"
echo "=========================================="

# 1. 测试登录
echo -e "\n${YELLOW}[1] 测试用户登录${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

test_case "Admin 登录" 200 "$HTTP_CODE"

ACCESS_TOKEN=$(echo "$BODY" | jq -r '.access_token')

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✅ Access Token 获取成功${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ Access Token 获取失败${NC}"
    ((FAILED++))
fi

# 2. 测试错误密码
echo -e "\n${YELLOW}[2] 测试错误密码${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrongpassword"}')

test_case "错误密码登录" 401 "$HTTP_CODE"

# 3. 测试未授权访问
echo -e "\n${YELLOW}[3] 测试未授权访问${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/events)
test_case "无 Token 访问受保护资源" 401 "$HTTP_CODE"

# 4. 测试授权访问
echo -e "\n${YELLOW}[4] 测试授权访问${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/events \
  -H "Authorization: Bearer $ACCESS_TOKEN")
test_case "有效 Token 访问受保护资源" 200 "$HTTP_CODE"

# 5. 测试 CORS
echo -e "\n${YELLOW}[5] 测试 CORS${NC}"
CORS_HEADERS=$(curl -s -I -X OPTIONS $BASE_URL/events \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" | grep -i "access-control")

if echo "$CORS_HEADERS" | grep -q "Access-Control-Allow-Origin"; then
    echo -e "${GREEN}✅ CORS 配置正确${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ CORS 配置错误${NC}"
    ((FAILED++))
fi

# 测试总结
echo ""
echo "=========================================="
echo "  测试总结"
echo "=========================================="
echo -e "${GREEN}通过: $PASSED${NC}"
echo -e "${RED}失败: $FAILED${NC}"
echo "总计: $((PASSED + FAILED))"

if [ $FAILED -eq 0 ]; then
    echo -e "\n${GREEN}🎉 所有测试通过！${NC}"
    exit 0
else
    echo -e "\n${RED}⚠️  部分测试失败${NC}"
    exit 1
fi
```

**使用方法**:
```bash
chmod +x test-api.sh
./test-api.sh
```

---

## 📋 测试检查清单

使用此清单确保所有重要测试都已覆盖：

### 认证功能
- [ ] 成功注册
- [ ] 重复邮箱注册失败
- [ ] 成功登录（admin）
- [ ] 成功登录（普通用户）
- [ ] 错误密码登录失败
- [ ] 不存在用户登录失败
- [ ] 禁用用户登录失败
- [ ] Token 刷新成功
- [ ] 过期 Token 刷新失败
- [ ] 成功登出
- [ ] 登出后 Token 失效

### 受保护资源
- [ ] 有效 Token 访问成功
- [ ] 无 Token 访问失败
- [ ] 无效 Token 访问失败
- [ ] 过期 Token 访问失败
- [ ] 权限不足访问失败

### CORS
- [ ] OPTIONS 预检请求成功
- [ ] 允许的来源可访问
- [ ] 不允许的来源被拒绝
- [ ] 暴露的响应头正确

### 安全
- [ ] SQL 注入防护
- [ ] XSS 防护
- [ ] Token 篡改检测
- [ ] CSRF 防护（如启用）

### 数据验证
- [ ] 返回用户信息正确
- [ ] 返回组织者信息正确
- [ ] 无循环引用（避免堆栈溢出）
- [ ] 角色信息正确
- [ ] JSON 格式正确

### 性能
- [ ] 登录响应时间 < 1s
- [ ] 列表查询响应时间 < 500ms
- [ ] 并发请求处理正常

---

## 🛠️ 推荐的测试工具

1. **Postman / Insomnia** - API 手动测试
2. **cURL** - 命令行测试
3. **JMeter / Gatling** - 性能和负载测试
4. **Newman** - Postman 集合自动化
5. **REST Assured** - Java API 测试框架
6. **Spring Test** - Spring Boot 集成测试

---

## 🎯 总结

完整的 API 测试应该包括：

1. ✅ **功能测试** - 确保 API 按预期工作
2. ✅ **安全测试** - 防止常见的安全漏洞
3. ✅ **性能测试** - 确保响应时间合理
4. ✅ **边界测试** - 处理极端情况
5. ✅ **集成测试** - 验证各模块协作
6. ✅ **回归测试** - 确保新功能不破坏旧功能

**测试覆盖率目标**: 至少 80%

祝测试顺利！🚀

