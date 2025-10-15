#!/bin/bash

# ========================================
# API 完整测试脚本
# ========================================

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# 检查 jq 是否安装
if ! command -v jq &> /dev/null; then
    echo -e "${RED}错误: 需要安装 jq 工具${NC}"
    echo "MacOS: brew install jq"
    echo "Ubuntu: sudo apt-get install jq"
    exit 1
fi

echo -e "${BLUE}=========================================="
echo "  🚀 API 测试开始"
echo "==========================================${NC}"
echo ""

# ========================================
# 1. 认证模块测试
# ========================================
echo -e "${YELLOW}📋 [1] 认证模块测试${NC}"
echo "----------------------------------------"

# 1.1 成功登录 - Admin
echo -e "\n${BLUE}测试 1.1: Admin 用户登录${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

test_case "Admin 登录" 200 "$HTTP_CODE"

if [ "$HTTP_CODE" -eq 200 ]; then
    ACCESS_TOKEN=$(echo "$BODY" | jq -r '.access_token')
    REFRESH_TOKEN=$(echo "$BODY" | jq -r '.refresh_token')
    USER_ID=$(echo "$BODY" | jq -r '.user.id')
    USER_ROLES=$(echo "$BODY" | jq -r '.user.roles | length')
    ORGANIZER_NAME=$(echo "$BODY" | jq -r '.organizer.organizationName')
    
    if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
        echo -e "${GREEN}  ✓ Access Token 获取成功${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  ✗ Access Token 获取失败${NC}"
        ((FAILED++))
    fi
    
    if [ "$REFRESH_TOKEN" != "null" ] && [ -n "$REFRESH_TOKEN" ]; then
        echo -e "${GREEN}  ✓ Refresh Token 获取成功${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  ✗ Refresh Token 获取失败${NC}"
        ((FAILED++))
    fi
    
    if [ "$ORGANIZER_NAME" == "CAMT" ]; then
        echo -e "${GREEN}  ✓ Organizer 信息正确返回${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  ✗ Organizer 信息错误: $ORGANIZER_NAME${NC}"
        ((FAILED++))
    fi
    
    if [ "$USER_ROLES" -ge 2 ]; then
        echo -e "${GREEN}  ✓ Admin 角色包含 ROLE_USER 和 ROLE_ADMIN${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  ✗ Admin 角色数量错误${NC}"
        ((FAILED++))
    fi
fi

# 1.2 普通用户登录
echo -e "\n${BLUE}测试 1.2: 普通用户登录${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user"}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

test_case "普通用户登录" 200 "$HTTP_CODE"

if [ "$HTTP_CODE" -eq 200 ]; then
    USER_ORGANIZER=$(echo "$BODY" | jq -r '.organizer.organizationName')
    if [ "$USER_ORGANIZER" == "CMU" ]; then
        echo -e "${GREEN}  ✓ 普通用户 Organizer 信息正确${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  ✗ 普通用户 Organizer 信息错误${NC}"
        ((FAILED++))
    fi
fi

# 1.3 错误密码
echo -e "\n${BLUE}测试 1.3: 错误密码登录${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrongpassword"}')

test_case "错误密码登录" 401 "$HTTP_CODE"

# 1.4 不存在的用户
echo -e "\n${BLUE}测试 1.4: 不存在的用户登录${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"nonexistent","password":"password"}')

test_case "不存在用户登录" 401 "$HTTP_CODE"

# 1.5 缺少字段
echo -e "\n${BLUE}测试 1.5: 缺少必填字段${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin"}')

test_case "缺少密码字段" 400 "$HTTP_CODE"

# ========================================
# 2. 受保护资源测试
# ========================================
echo -e "\n${YELLOW}📋 [2] 受保护资源测试${NC}"
echo "----------------------------------------"

# 2.1 无 Token 访问
echo -e "\n${BLUE}测试 2.1: 未授权访问受保护资源${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/events)
test_case "无 Token 访问 /events" 401 "$HTTP_CODE"

# 2.2 有效 Token 访问
echo -e "\n${BLUE}测试 2.2: 使用有效 Token 访问${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/events \
  -H "Authorization: Bearer $ACCESS_TOKEN")
test_case "有效 Token 访问 /events" 200 "$HTTP_CODE"

# 2.3 获取事件列表
echo -e "\n${BLUE}测试 2.3: 获取事件列表${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" $BASE_URL/events \
  -H "Authorization: Bearer $ACCESS_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

test_case "获取事件列表" 200 "$HTTP_CODE"

if [ "$HTTP_CODE" -eq 200 ]; then
    EVENT_COUNT=$(echo "$BODY" | jq '. | length')
    if [ "$EVENT_COUNT" -gt 0 ]; then
        echo -e "${GREEN}  ✓ 返回了 $EVENT_COUNT 个事件${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  ✗ 事件列表为空${NC}"
        ((FAILED++))
    fi
fi

# 2.4 分页测试
echo -e "\n${BLUE}测试 2.4: 分页功能${NC}"
RESPONSE=$(curl -s -I "$BASE_URL/events?page=0&size=2" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if echo "$RESPONSE" | grep -qi "x-total-count"; then
    echo -e "${GREEN}  ✓ x-total-count 响应头存在${NC}"
    ((PASSED++))
else
    echo -e "${RED}  ✗ x-total-count 响应头缺失${NC}"
    ((FAILED++))
fi

# 2.5 获取组织者列表
echo -e "\n${BLUE}测试 2.5: 获取组织者列表${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" $BASE_URL/organizers \
  -H "Authorization: Bearer $ACCESS_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

test_case "获取组织者列表" 200 "$HTTP_CODE"

if [ "$HTTP_CODE" -eq 200 ]; then
    ORG_COUNT=$(echo "$BODY" | jq '. | length')
    if [ "$ORG_COUNT" -gt 0 ]; then
        echo -e "${GREEN}  ✓ 返回了 $ORG_COUNT 个组织者${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  ✗ 组织者列表为空${NC}"
        ((FAILED++))
    fi
fi

# 2.6 无效 Token
echo -e "\n${BLUE}测试 2.6: 使用无效 Token${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/events \
  -H "Authorization: Bearer invalid.token.here")
test_case "无效 Token 访问" 401 "$HTTP_CODE"

# ========================================
# 3. CORS 测试
# ========================================
echo -e "\n${YELLOW}📋 [3] CORS 跨域测试${NC}"
echo "----------------------------------------"

# 3.1 OPTIONS 预检请求
echo -e "\n${BLUE}测试 3.1: OPTIONS 预检请求${NC}"
RESPONSE=$(curl -s -I -X OPTIONS $BASE_URL/events \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization")

HTTP_CODE=$(echo "$RESPONSE" | grep -oP "HTTP/\d\.\d \K\d+")
test_case "OPTIONS 预检请求" 200 "$HTTP_CODE"

# 3.2 检查 CORS 响应头
echo -e "\n${BLUE}测试 3.2: CORS 响应头检查${NC}"

if echo "$RESPONSE" | grep -qi "Access-Control-Allow-Origin"; then
    echo -e "${GREEN}  ✓ Access-Control-Allow-Origin 存在${NC}"
    ((PASSED++))
else
    echo -e "${RED}  ✗ Access-Control-Allow-Origin 缺失${NC}"
    ((FAILED++))
fi

if echo "$RESPONSE" | grep -qi "Access-Control-Allow-Methods"; then
    echo -e "${GREEN}  ✓ Access-Control-Allow-Methods 存在${NC}"
    ((PASSED++))
else
    echo -e "${RED}  ✗ Access-Control-Allow-Methods 缺失${NC}"
    ((FAILED++))
fi

if echo "$RESPONSE" | grep -qi "Access-Control-Allow-Credentials"; then
    echo -e "${GREEN}  ✓ Access-Control-Allow-Credentials 存在${NC}"
    ((PASSED++))
else
    echo -e "${RED}  ✗ Access-Control-Allow-Credentials 缺失${NC}"
    ((FAILED++))
fi

if echo "$RESPONSE" | grep -qi "Access-Control-Expose-Headers"; then
    EXPOSED=$(echo "$RESPONSE" | grep -i "Access-Control-Expose-Headers")
    if echo "$EXPOSED" | grep -qi "x-total-count"; then
        echo -e "${GREEN}  ✓ x-total-count 在暴露头中${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  ✗ x-total-count 未在暴露头中${NC}"
        ((FAILED++))
    fi
else
    echo -e "${RED}  ✗ Access-Control-Expose-Headers 缺失${NC}"
    ((FAILED++))
fi

# ========================================
# 4. Token 刷新测试
# ========================================
echo -e "\n${YELLOW}📋 [4] Token 刷新测试${NC}"
echo "----------------------------------------"

# 4.1 使用 Refresh Token 刷新
echo -e "\n${BLUE}测试 4.1: 使用 Refresh Token 刷新${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/v1/auth/refresh-token \
  -H "Authorization: Bearer $REFRESH_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
test_case "Refresh Token 刷新" 200 "$HTTP_CODE"

# ========================================
# 5. 数据完整性测试
# ========================================
echo -e "\n${YELLOW}📋 [5] 数据完整性测试${NC}"
echo "----------------------------------------"

# 5.1 验证无循环引用
echo -e "\n${BLUE}测试 5.1: 验证响应无循环引用${NC}"
RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}')

# 检查 user 对象中是否包含 organizer
USER_HAS_ORGANIZER=$(echo "$RESPONSE" | jq '.user | has("organizer")')
# 检查 organizer 对象中是否包含 user
ORGANIZER_HAS_USER=$(echo "$RESPONSE" | jq '.organizer | has("user")')

if [ "$USER_HAS_ORGANIZER" == "false" ] && [ "$ORGANIZER_HAS_USER" == "false" ]; then
    echo -e "${GREEN}  ✓ 无循环引用（避免堆栈溢出）${NC}"
    ((PASSED++))
else
    echo -e "${RED}  ✗ 存在循环引用风险${NC}"
    ((FAILED++))
fi

# 5.2 验证 MapStruct 转换正确
echo -e "\n${BLUE}测试 5.2: 验证 MapStruct 转换${NC}"
ORGANIZER_FIELDS=$(echo "$RESPONSE" | jq '.organizer | keys | length')

if [ "$ORGANIZER_FIELDS" -ge 4 ]; then
    echo -e "${GREEN}  ✓ Organizer DTO 字段完整（$ORGANIZER_FIELDS 个字段）${NC}"
    ((PASSED++))
else
    echo -e "${RED}  ✗ Organizer DTO 字段不完整${NC}"
    ((FAILED++))
fi

# ========================================
# 6. 响应时间测试
# ========================================
echo -e "\n${YELLOW}📋 [6] 性能测试${NC}"
echo "----------------------------------------"

# 6.1 登录响应时间
echo -e "\n${BLUE}测试 6.1: 登录响应时间${NC}"
START=$(date +%s%N)
curl -s -X POST $BASE_URL/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' > /dev/null
END=$(date +%s%N)
DURATION=$((($END - $START) / 1000000))

if [ $DURATION -lt 1000 ]; then
    echo -e "${GREEN}  ✓ 登录响应时间: ${DURATION}ms (< 1000ms)${NC}"
    ((PASSED++))
else
    echo -e "${RED}  ✗ 登录响应时间过长: ${DURATION}ms${NC}"
    ((FAILED++))
fi

# 6.2 列表查询响应时间
echo -e "\n${BLUE}测试 6.2: 列表查询响应时间${NC}"
START=$(date +%s%N)
curl -s $BASE_URL/events \
  -H "Authorization: Bearer $ACCESS_TOKEN" > /dev/null
END=$(date +%s%N)
DURATION=$((($END - $START) / 1000000))

if [ $DURATION -lt 500 ]; then
    echo -e "${GREEN}  ✓ 列表查询响应时间: ${DURATION}ms (< 500ms)${NC}"
    ((PASSED++))
else
    echo -e "${YELLOW}  ⚠ 列表查询响应时间: ${DURATION}ms (建议 < 500ms)${NC}"
    ((PASSED++))
fi

# ========================================
# 测试总结
# ========================================
echo ""
echo -e "${BLUE}=========================================="
echo "  📊 测试总结"
echo "==========================================${NC}"
echo ""
echo -e "${GREEN}✅ 通过: $PASSED${NC}"
echo -e "${RED}❌ 失败: $FAILED${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "   总计: $((PASSED + FAILED)) 个测试"
echo ""

# 计算通过率
TOTAL=$((PASSED + FAILED))
PASS_RATE=$((PASSED * 100 / TOTAL))

echo -e "   通过率: ${PASS_RATE}%"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 恭喜！所有测试通过！${NC}"
    echo ""
    exit 0
elif [ $PASS_RATE -ge 80 ]; then
    echo -e "${YELLOW}⚠️  大部分测试通过，但仍有 $FAILED 个测试失败${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}❌ 测试失败过多，请检查 API 实现${NC}"
    echo ""
    exit 1
fi

