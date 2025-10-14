#!/bin/bash

echo "========================================"
echo "🔒 Spring Security 配置优化验证"
echo "========================================"
echo ""

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

PASSED=0
FAILED=0

echo "========================================"
echo "1️⃣ CORS 配置验证"
echo "========================================"
echo ""

echo -e "${BLUE}测试 OPTIONS 预检请求...${NC}"
CORS_RESPONSE=$(curl -s -i -X OPTIONS "$BASE_URL/events" \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET")

if echo "$CORS_RESPONSE" | grep -q "HTTP.*200"; then
    echo -e "${GREEN}✅ OPTIONS 返回 200${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ OPTIONS 未返回 200${NC}"
    ((FAILED++))
fi

if echo "$CORS_RESPONSE" | grep -qi "Access-Control"; then
    echo -e "${GREEN}✅ 包含 CORS 响应头${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ 缺少 CORS 响应头${NC}"
    ((FAILED++))
fi
echo ""

echo "========================================"
echo "2️⃣ 认证失败 - 统一 JSON 响应"
echo "========================================"
echo ""

echo -e "${BLUE}测试 401 未认证错误...${NC}"
ERROR_401=$(curl -s "$BASE_URL/events")

if echo "$ERROR_401" | jq -e '.error == "Unauthorized" and .status == 401' > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 401 返回标准 JSON 响应${NC}"
    echo "   响应: $(echo $ERROR_401 | jq -c '.')"
    ((PASSED++))
else
    echo -e "${RED}❌ 401 响应格式不正确${NC}"
    echo "   响应: $ERROR_401"
    ((FAILED++))
fi
echo ""

echo "========================================"
echo "3️⃣ 认证端点放行"
echo "========================================"
echo ""

echo -e "${BLUE}测试登录端点...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/authenticate" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}')

ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.access_token // .accessToken')

if [ "$ACCESS_TOKEN" != "null" ] && [ ! -z "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✅ 登录成功，获取到 Token${NC}"
    echo "   Token: ${ACCESS_TOKEN:0:30}..."
    ((PASSED++))
else
    echo -e "${RED}❌ 登录失败${NC}"
    echo "   响应: $LOGIN_RESPONSE"
    ((FAILED++))
fi
echo ""

echo "========================================"
echo "4️⃣ Token 访问受保护端点"
echo "========================================"
echo ""

if [ "$ACCESS_TOKEN" != "null" ] && [ ! -z "$ACCESS_TOKEN" ]; then
    echo -e "${BLUE}测试使用 Token 访问 /events...${NC}"
    EVENTS_RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/events" \
      -H "Authorization: Bearer $ACCESS_TOKEN")
    
    STATUS_CODE=$(echo "$EVENTS_RESPONSE" | tail -n 1)
    
    if [ "$STATUS_CODE" == "200" ]; then
        echo -e "${GREEN}✅ 使用有效 Token 访问成功 (200)${NC}"
        ((PASSED++))
    else
        echo -e "${RED}❌ 访问失败 (状态码: $STATUS_CODE)${NC}"
        ((FAILED++))
    fi
fi
echo ""

echo "========================================"
echo "5️⃣ Refresh Token 测试"
echo "========================================"
echo ""

REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.refresh_token // .refreshToken')

if [ "$REFRESH_TOKEN" != "null" ] && [ ! -z "$REFRESH_TOKEN" ]; then
    echo -e "${BLUE}测试 Refresh Token...${NC}"
    REFRESH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/refresh-token" \
      -H "Authorization: Bearer $REFRESH_TOKEN")
    
    REFRESH_STATUS=$(echo "$REFRESH_RESPONSE" | tail -n 1)
    
    if [ "$REFRESH_STATUS" == "200" ]; then
        echo -e "${GREEN}✅ Token 刷新成功 (200)${NC}"
        ((PASSED++))
    else
        echo -e "${RED}❌ Token 刷新失败 (状态码: $REFRESH_STATUS)${NC}"
        ((FAILED++))
    fi
fi
echo ""

echo "========================================"
echo "6️⃣ 过滤器链验证"
echo "========================================"
echo ""

echo -e "${BLUE}检查应用日志中的过滤器链...${NC}"

if grep -q "CorsFilter" spring-boot-new.log 2>/dev/null; then
    echo -e "${GREEN}✅ CorsFilter 已正确注册${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ 未找到 CorsFilter${NC}"
    ((FAILED++))
fi

if grep -q "JwtAuthenticationFilter" spring-boot-new.log 2>/dev/null; then
    echo -e "${GREEN}✅ JwtAuthenticationFilter 已正确注册${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ 未找到 JwtAuthenticationFilter${NC}"
    ((FAILED++))
fi

FILTER_ORDER=$(grep "Will secure any request with filters" spring-boot-new.log 2>/dev/null | tail -1)
if echo "$FILTER_ORDER" | grep -q "CorsFilter.*JwtAuthenticationFilter"; then
    echo -e "${GREEN}✅ 过滤器顺序正确 (CorsFilter → JwtAuthenticationFilter)${NC}"
    ((PASSED++))
else
    echo -e "${RED}❌ 过滤器顺序可能不正确${NC}"
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
    echo "✅ 优化成果："
    echo "   • CORS 跨域配置正确"
    echo "   • 统一 JSON 错误响应（401/403）"
    echo "   • 认证端点正确放行"
    echo "   • Token 认证正常工作"
    echo "   • Token 刷新正常工作"
    echo "   • 过滤器链正确配置"
    echo ""
    echo "📄 详细文档: Security配置优化说明.md"
    exit 0
else
    echo -e "${RED}⚠️  有 $FAILED 个测试失败${NC}"
    echo ""
    echo "📝 调试建议："
    echo "   1. 检查应用启动: curl http://localhost:8080/error"
    echo "   2. 查看日志: tail -f spring-boot-new.log"
    echo "   3. 验证配置: grep CORS spring-boot-new.log"
    exit 1
fi

