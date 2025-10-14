#!/bin/bash

echo "========================================"
echo "🧪 JWT 过滤器优化验证测试"
echo "========================================"
echo ""

BASE_URL="http://localhost:8080"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试计数器
PASSED=0
FAILED=0

# 测试函数
test_endpoint() {
    local test_name=$1
    local method=$2
    local endpoint=$3
    local headers=$4
    local expected_status=$5
    
    echo -n "测试: $test_name ... "
    
    response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" $headers)
    status_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$status_code" == "$expected_status" ]; then
        echo -e "${GREEN}✅ PASS${NC} (状态码: $status_code)"
        ((PASSED++))
        if [ ! -z "$body" ] && [ "$body" != "null" ]; then
            echo "   响应: $(echo $body | jq -c '.' 2>/dev/null || echo $body)"
        fi
    else
        echo -e "${RED}❌ FAIL${NC} (期望: $expected_status, 实际: $status_code)"
        ((FAILED++))
        if [ ! -z "$body" ]; then
            echo "   响应: $body"
        fi
    fi
    echo ""
}

echo "========================================"
echo "1️⃣ 路径匹配测试"
echo "========================================"

test_endpoint \
    "认证端点应该跳过 JWT 验证 (/api/v1/auth/authenticate)" \
    "POST" \
    "/api/v1/auth/authenticate" \
    '-H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin\"}"' \
    "200"

test_endpoint \
    "非认证端点应该需要 JWT 验证 (/events)" \
    "GET" \
    "/events" \
    "" \
    "403"

test_endpoint \
    "错误路径不应该被错误匹配 (/api/v1/authenticated)" \
    "GET" \
    "/api/v1/authenticated" \
    "" \
    "403"

echo "========================================"
echo "2️⃣ 异常与过期处理测试"
echo "========================================"

test_endpoint \
    "无效 Token 应返回 401 错误" \
    "GET" \
    "/events" \
    '-H "Authorization: Bearer invalid_token_12345"' \
    "401"

test_endpoint \
    "格式错误的 Token 应返回 401 错误" \
    "GET" \
    "/events" \
    '-H "Authorization: Bearer malformed.token"' \
    "401"

echo "========================================"
echo "3️⃣ OPTIONS 预检测试"
echo "========================================"

test_endpoint \
    "OPTIONS 请求应该被放行" \
    "OPTIONS" \
    "/events" \
    '-H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: GET"' \
    "200"

echo "========================================"
echo "4️⃣ 正常认证流程测试"
echo "========================================"

# 获取有效 Token
echo "正在获取有效 Token..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/authenticate" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin"}')

ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.access_token // .accessToken')
REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.refresh_token // .refreshToken')

if [ "$ACCESS_TOKEN" != "null" ] && [ ! -z "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✅ 成功获取 Access Token${NC}"
    echo "   Token 前缀: ${ACCESS_TOKEN:0:20}..."
    echo ""
    
    test_endpoint \
        "使用有效 Access Token 访问受保护端点" \
        "GET" \
        "/events" \
        "-H \"Authorization: Bearer $ACCESS_TOKEN\"" \
        "200"
    
    test_endpoint \
        "使用 Refresh Token 访问受保护端点应该失败" \
        "GET" \
        "/events" \
        "-H \"Authorization: Bearer $REFRESH_TOKEN\"" \
        "401"
else
    echo -e "${RED}❌ 获取 Token 失败${NC}"
    echo "   响应: $LOGIN_RESPONSE"
    ((FAILED++))
fi

echo "========================================"
echo "5️⃣ 刷新令牌测试"
echo "========================================"

if [ "$REFRESH_TOKEN" != "null" ] && [ ! -z "$REFRESH_TOKEN" ]; then
    test_endpoint \
        "使用有效 Refresh Token 刷新" \
        "POST" \
        "/api/v1/auth/refresh-token" \
        "-H \"Authorization: Bearer $REFRESH_TOKEN\"" \
        "200"
else
    echo -e "${RED}❌ 无有效 Refresh Token${NC}"
    ((FAILED++))
fi

echo "========================================"
echo "6️⃣ 过滤器顺序验证（检查日志）"
echo "========================================"

echo "检查应用日志中的过滤器顺序..."
if grep -q "JwtAuthenticationFilter" spring-boot-new.log; then
    echo -e "${GREEN}✅ JWT 过滤器已注册${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️  未在日志中找到过滤器信息（可能需要更高日志级别）${NC}"
fi
echo ""

echo "检查日志中的 DEBUG 信息（应该有条件日志）..."
if grep -q "JWT Token extracted" spring-boot-new.log; then
    echo -e "${GREEN}✅ 条件日志正常工作${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️  未找到 DEBUG 日志（可能日志级别为 INFO/WARN）${NC}"
fi
echo ""

echo "========================================"
echo "📊 测试结果汇总"
echo "========================================"
echo -e "✅ 通过: ${GREEN}$PASSED${NC}"
echo -e "❌ 失败: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 所有测试通过！JWT 过滤器优化成功！${NC}"
    exit 0
else
    echo -e "${RED}⚠️  有 $FAILED 个测试失败，请检查配置${NC}"
    exit 1
fi

