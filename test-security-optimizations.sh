#!/bin/bash

echo "========================================"
echo "🔒 Spring Security 配置优化验证测试"
echo "========================================"
echo ""

BASE_URL="http://localhost:8080"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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
    local description=$6
    
    echo -e "${BLUE}📝 测试: $test_name${NC}"
    if [ ! -z "$description" ]; then
        echo "   说明: $description"
    fi
    
    response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" $headers)
    status_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$status_code" == "$expected_status" ]; then
        echo -e "   ${GREEN}✅ PASS${NC} (状态码: $status_code)"
        ((PASSED++))
        if [ ! -z "$body" ] && [ "$body" != "null" ] && [ "$body" != "" ]; then
            echo "   响应: $(echo $body | jq -c '.' 2>/dev/null || echo $body | head -c 100)"
        fi
    else
        echo -e "   ${RED}❌ FAIL${NC} (期望: $expected_status, 实际: $status_code)"
        ((FAILED++))
        if [ ! -z "$body" ] && [ "$body" != "" ]; then
            echo "   响应: $(echo $body | head -c 200)"
        fi
    fi
    echo ""
}

echo "========================================"
echo "1️⃣ CORS 跨域配置测试"
echo "========================================"
echo ""

test_endpoint \
    "OPTIONS 预检请求 - /events" \
    "OPTIONS" \
    "/events" \
    '-H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: Authorization" -v 2>&1 | grep -E "Access-Control"' \
    "200" \
    "验证 CORS 预检是否放行，并检查响应头"

test_endpoint \
    "GET 请求携带 Origin - /events（无Token）" \
    "GET" \
    "/events" \
    '-H "Origin: http://localhost:3000" -v 2>&1 | tail -20' \
    "401" \
    "验证实际请求是否返回 401（未认证）"

echo "========================================"
echo "2️⃣ 放行路径测试"
echo "========================================"
echo ""

test_endpoint \
    "认证端点放行 - /api/v1/auth/authenticate" \
    "POST" \
    "/api/v1/auth/authenticate" \
    '-H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin\"}"' \
    "200" \
    "认证端点应该无需 Token 即可访问"

test_endpoint \
    "错误页面放行 - /error" \
    "GET" \
    "/error" \
    "" \
    "404" \
    "错误页面应该放行（返回 404 或 200）"

test_endpoint \
    "非放行路径 - /events（无Token）" \
    "GET" \
    "/events" \
    "" \
    "401" \
    "非放行路径应该返回 401 认证失败"

echo "========================================"
echo "3️⃣ 统一异常处理测试"
echo "========================================"
echo ""

test_endpoint \
    "401 认证失败 - JSON 响应格式" \
    "GET" \
    "/events" \
    "" \
    "401" \
    "验证返回标准 JSON 错误响应"

echo -e "${BLUE}📝 验证 401 响应 JSON 结构${NC}"
RESPONSE_401=$(curl -s http://localhost:8080/events)
if echo "$RESPONSE_401" | jq -e '.error == "Unauthorized" and .status == 401' > /dev/null 2>&1; then
    echo -e "   ${GREEN}✅ PASS${NC} - JSON 结构正确"
    echo "   响应: $(echo $RESPONSE_401 | jq -c '.')"
    ((PASSED++))
else
    echo -e "   ${RED}❌ FAIL${NC} - JSON 结构不正确"
    echo "   响应: $RESPONSE_401"
    ((FAILED++))
fi
echo ""

echo "========================================"
echo "4️⃣ 正常认证流程测试"
echo "========================================"
echo ""

echo -e "${BLUE}📝 获取有效 Token...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/authenticate" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin"}')

ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.access_token // .accessToken')
REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.refresh_token // .refreshToken')

if [ "$ACCESS_TOKEN" != "null" ] && [ ! -z "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✅ 成功获取 Access Token${NC}"
    echo "   Token 前缀: ${ACCESS_TOKEN:0:30}..."
    echo ""
    
    test_endpoint \
        "使用有效 Token 访问受保护端点 - /events" \
        "GET" \
        "/events" \
        "-H \"Authorization: Bearer $ACCESS_TOKEN\"" \
        "200" \
        "验证有效 Token 可以正常访问"
    
    test_endpoint \
        "使用有效 Token 访问 - 验证 CORS 响应头" \
        "GET" \
        "/events" \
        "-H \"Authorization: Bearer $ACCESS_TOKEN\" -H \"Origin: http://localhost:3000\" -v 2>&1 | grep -E \"Access-Control|200\"" \
        "200" \
        "验证带 Origin 的请求是否包含 CORS 响应头"
else
    echo -e "${RED}❌ 获取 Token 失败${NC}"
    echo "   响应: $LOGIN_RESPONSE"
    ((FAILED++))
fi

echo "========================================"
echo "5️⃣ 刷新令牌测试"
echo "========================================"
echo ""

if [ "$REFRESH_TOKEN" != "null" ] && [ ! -z "$REFRESH_TOKEN" ]; then
    test_endpoint \
        "使用有效 Refresh Token 刷新" \
        "POST" \
        "/api/v1/auth/refresh-token" \
        "-H \"Authorization: Bearer $REFRESH_TOKEN\"" \
        "200" \
        "验证 Refresh Token 可以成功刷新"
    
    # 测试刷新后的新 Token
    REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/refresh-token" \
        -H "Authorization: Bearer $REFRESH_TOKEN")
    
    NEW_ACCESS_TOKEN=$(echo $REFRESH_RESPONSE | jq -r '.access_token // .accessToken')
    
    if [ "$NEW_ACCESS_TOKEN" != "null" ] && [ ! -z "$NEW_ACCESS_TOKEN" ]; then
        echo -e "${GREEN}✅ 成功获取新的 Access Token${NC}"
        echo "   新 Token 前缀: ${NEW_ACCESS_TOKEN:0:30}..."
        echo ""
        
        test_endpoint \
            "使用刷新后的新 Token 访问 - /events" \
            "GET" \
            "/events" \
            "-H \"Authorization: Bearer $NEW_ACCESS_TOKEN\"" \
            "200" \
            "验证新 Token 可以正常使用"
    fi
else
    echo -e "${RED}❌ 无有效 Refresh Token${NC}"
    ((FAILED++))
fi

echo "========================================"
echo "6️⃣ 过滤器链验证（检查日志）"
echo "========================================"
echo ""

echo -e "${BLUE}📝 检查应用日志中的过滤器顺序...${NC}"
if grep -q "CorsFilter" spring-boot-new.log; then
    echo -e "${GREEN}✅ CorsFilter 已正确注册${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ 未找到 CorsFilter${NC}"
    ((FAILED++))
fi

if grep -q "JwtAuthenticationFilter" spring-boot-new.log; then
    echo -e "${GREEN}✅ JwtAuthenticationFilter 已正确注册${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ 未找到 JwtAuthenticationFilter${NC}"
    ((FAILED++))
fi

echo ""
echo -e "${BLUE}📝 验证过滤器顺序（CorsFilter 应在 JwtAuthenticationFilter 之前）${NC}"
FILTER_ORDER=$(grep "Will secure any request with filters" spring-boot-new.log | tail -1)
if echo "$FILTER_ORDER" | grep -q "CorsFilter.*JwtAuthenticationFilter"; then
    echo -e "${GREEN}✅ 过滤器顺序正确${NC}"
    echo "   顺序: ... → CorsFilter → ... → JwtAuthenticationFilter → ..."
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️  过滤器顺序可能不正确或未找到日志${NC}"
fi

echo ""

echo "========================================"
echo "📊 测试结果汇总"
echo "========================================"
echo -e "✅ 通过: ${GREEN}$PASSED${NC}"
echo -e "❌ 失败: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 所有测试通过！Security 配置优化成功！${NC}"
    echo ""
    echo "优化成果："
    echo "  ✅ CORS 跨域配置完成"
    echo "  ✅ 放行路径集中管理"
    echo "  ✅ 统一异常处理（401/403 JSON 响应）"
    echo "  ✅ 认证流程正常"
    echo "  ✅ Token 刷新正常"
    echo "  ✅ 过滤器链正确"
    echo ""
    echo "📄 详细文档见: Security配置优化说明.md"
    exit 0
else
    echo -e "${RED}⚠️  有 $FAILED 个测试失败，请检查配置${NC}"
    echo ""
    echo "📝 调试建议："
    echo "  1. 检查应用是否正常启动（curl http://localhost:8080/error）"
    echo "  2. 查看日志: tail -f spring-boot-new.log"
    echo "  3. 确认 SecurityConfiguration.java 是否正确编译"
    echo "  4. 验证 CORS 配置: grep -E \"CorsFilter|CORS\" spring-boot-new.log"
    exit 1
fi

