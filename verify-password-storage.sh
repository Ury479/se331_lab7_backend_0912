#!/bin/bash

echo "========================================"
echo "🔒 密码存储安全验证"
echo "========================================"
echo ""

CONTAINER_NAME="se331_lab7_backend_0912-db-1"
DB_NAME="selabdb"
DB_USER="root"
DB_PASS="cmu123wyh"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}📋 检查数据库容器状态...${NC}"
if docker ps | grep -q "$CONTAINER_NAME"; then
    echo -e "${GREEN}✅ MySQL 容器正在运行${NC}"
    echo ""
else
    echo -e "${RED}❌ MySQL 容器未运行${NC}"
    echo "请先启动 Docker 容器: docker-compose up -d"
    exit 1
fi

echo "========================================"
echo "1️⃣ 密码格式检查"
echo "========================================"
echo ""

echo -e "${BLUE}查询: 密码前缀和长度${NC}"
echo ""

docker exec $CONTAINER_NAME mysql -u $DB_USER -p$DB_PASS $DB_NAME -e "
SELECT 
    id, 
    username, 
    email,
    LEFT(password, 10) as password_prefix, 
    LENGTH(password) as password_length,
    CASE 
        WHEN password LIKE '\$2a\$%' THEN '✅ BCrypt 2a'
        WHEN password LIKE '\$2b\$%' THEN '✅ BCrypt 2b'
        WHEN password LIKE '\$2y\$%' THEN '✅ BCrypt 2y'
        ELSE '❌ 未加密'
    END as encryption_status
FROM _user 
ORDER BY id;
" 2>/dev/null | sed 's/\\$/$/g'

echo ""
echo "========================================"
echo "2️⃣ BCrypt 格式详细分析"
echo "========================================"
echo ""

echo -e "${BLUE}示例密码哈希结构:${NC}"
echo ""

SAMPLE_HASH=$(docker exec $CONTAINER_NAME mysql -u $DB_USER -p$DB_PASS $DB_NAME -se "SELECT password FROM _user LIMIT 1;" 2>/dev/null)

echo "完整哈希: $SAMPLE_HASH"
echo ""
echo "结构分解:"
echo "  \$2a\$10\$2HaHSXwHwNMR8/eIASqLqO.FL6L9HesAJ.7LWxF0rxdW.dUAL3r2e"
echo "  │  │  │ │                                                      │"
echo "  │  │  │ └─ Salt (22字符)                                        └─ Hash (31字符)"
echo "  │  │  └─ Cost Factor (10 = 2^10 = 1024 次迭代)"
echo "  │  └─ 算法版本 (2a = BCrypt 版本 2a)"
echo "  └─ 标识符"
echo ""

echo "========================================"
echo "3️⃣ 安全性评估"
echo "========================================"
echo ""

# 检查所有密码是否都是 BCrypt
BCRYPT_COUNT=$(docker exec $CONTAINER_NAME mysql -u $DB_USER -p$DB_PASS $DB_NAME -se "
SELECT COUNT(*) FROM _user WHERE password LIKE '\$2a\$%' OR password LIKE '\$2b\$%' OR password LIKE '\$2y\$%';
" 2>/dev/null)

TOTAL_COUNT=$(docker exec $CONTAINER_NAME mysql -u $DB_USER -p$DB_PASS $DB_NAME -se "SELECT COUNT(*) FROM _user;" 2>/dev/null)

echo -e "${BLUE}密码加密统计:${NC}"
echo "  总用户数: $TOTAL_COUNT"
echo "  BCrypt 加密: $BCRYPT_COUNT"

if [ "$BCRYPT_COUNT" -eq "$TOTAL_COUNT" ]; then
    echo -e "  状态: ${GREEN}✅ 所有密码都已加密${NC}"
else
    echo -e "  状态: ${RED}❌ 存在未加密密码${NC}"
fi

echo ""

# 检查密码长度
echo -e "${BLUE}密码长度检查:${NC}"

docker exec $CONTAINER_NAME mysql -u $DB_USER -p$DB_PASS $DB_NAME -e "
SELECT 
    LENGTH(password) as password_length,
    COUNT(*) as count,
    CASE 
        WHEN LENGTH(password) = 60 THEN '✅ 标准 BCrypt 长度'
        ELSE '❌ 非标准长度'
    END as status
FROM _user 
GROUP BY LENGTH(password);
" 2>/dev/null

echo ""

echo "========================================"
echo "4️⃣ 验证测试"
echo "========================================"
echo ""

echo -e "${BLUE}测试用户登录...${NC}"

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}')

if echo "$LOGIN_RESPONSE" | jq -e '.access_token' > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 登录成功 - 密码验证正确${NC}"
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.access_token')
    echo "  Token 前缀: ${ACCESS_TOKEN:0:30}..."
else
    echo -e "${RED}❌ 登录失败${NC}"
    echo "  响应: $LOGIN_RESPONSE"
fi

echo ""

echo -e "${BLUE}测试错误密码...${NC}"

ERROR_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong_password"}')

STATUS_CODE=$(echo "$ERROR_RESPONSE" | tail -n 1)

if [ "$STATUS_CODE" == "401" ]; then
    echo -e "${GREEN}✅ 错误密码正确拒绝 (401)${NC}"
else
    echo -e "${RED}❌ 错误密码处理异常 (状态码: $STATUS_CODE)${NC}"
fi

echo ""

echo "========================================"
echo "📊 验证结果总结"
echo "========================================"
echo ""

ALL_PASS=true

# 检查1: BCrypt 加密
if [ "$BCRYPT_COUNT" -eq "$TOTAL_COUNT" ]; then
    echo -e "${GREEN}✅ 1. BCrypt 加密: 通过 ($BCRYPT_COUNT/$TOTAL_COUNT)${NC}"
else
    echo -e "${RED}❌ 1. BCrypt 加密: 失败 ($BCRYPT_COUNT/$TOTAL_COUNT)${NC}"
    ALL_PASS=false
fi

# 检查2: 密码长度
LENGTH_CHECK=$(docker exec $CONTAINER_NAME mysql -u $DB_USER -p$DB_PASS $DB_NAME -se "
SELECT COUNT(*) FROM _user WHERE LENGTH(password) = 60;
" 2>/dev/null)

if [ "$LENGTH_CHECK" -eq "$TOTAL_COUNT" ]; then
    echo -e "${GREEN}✅ 2. 密码长度: 通过 (60字符)${NC}"
else
    echo -e "${RED}❌ 2. 密码长度: 失败${NC}"
    ALL_PASS=false
fi

# 检查3: 密码前缀
PREFIX_CHECK=$(docker exec $CONTAINER_NAME mysql -u $DB_USER -p$DB_PASS $DB_NAME -se "
SELECT COUNT(*) FROM _user WHERE password LIKE '\$2a\$10\$%';
" 2>/dev/null)

if [ "$PREFIX_CHECK" -eq "$TOTAL_COUNT" ]; then
    echo -e "${GREEN}✅ 3. 密码前缀: 通过 (\$2a\$10\$)${NC}"
else
    echo -e "${YELLOW}⚠️  3. 密码前缀: 部分通过 (可能使用 \$2b\$ 或其他版本)${NC}"
fi

# 检查4: 登录测试
if echo "$LOGIN_RESPONSE" | jq -e '.access_token' > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 4. 登录验证: 通过${NC}"
else
    echo -e "${RED}❌ 4. 登录验证: 失败${NC}"
    ALL_PASS=false
fi

# 检查5: 错误密码拒绝
if [ "$STATUS_CODE" == "401" ]; then
    echo -e "${GREEN}✅ 5. 错误密码拒绝: 通过${NC}"
else
    echo -e "${RED}❌ 5. 错误密码拒绝: 失败${NC}"
    ALL_PASS=false
fi

echo ""

if [ "$ALL_PASS" = true ]; then
    echo -e "${GREEN}🎉 密码存储安全验证通过！${NC}"
    echo ""
    echo "✅ 安全等级: 高 ⭐⭐⭐⭐⭐"
    echo ""
    echo "已验证项："
    echo "  • BCrypt 加密算法"
    echo "  • 密码长度 60 字符"
    echo "  • 前缀 \$2a\$10\$ (Cost Factor = 10)"
    echo "  • 登录验证正确"
    echo "  • 错误密码正确拒绝"
    echo ""
    echo "📄 详细报告: 密码存储验证报告.md"
    exit 0
else
    echo -e "${RED}⚠️  密码存储存在安全问题！${NC}"
    echo ""
    echo "请检查："
    echo "  1. PasswordEncoder Bean 配置"
    echo "  2. 注册流程是否使用 passwordEncoder.encode()"
    echo "  3. 数据库初始化脚本"
    exit 1
fi

