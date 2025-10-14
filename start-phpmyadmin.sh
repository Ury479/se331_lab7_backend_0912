#!/bin/bash

echo "========================================"
echo "🚀 启动 phpMyAdmin"
echo "========================================"
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

# 检查 MySQL 容器是否运行
echo -e "${BLUE}📋 检查 MySQL 容器状态...${NC}"
if docker ps | grep -q "se331_lab7_backend_0912-db-1"; then
    echo -e "${GREEN}✅ MySQL 容器正在运行${NC}"
    echo ""
else
    echo -e "${RED}❌ MySQL 容器未运行${NC}"
    echo "请先启动 MySQL: docker-compose up -d db"
    exit 1
fi

# 检查 phpMyAdmin 容器是否已存在
if docker ps -a | grep -q "phpmyadmin"; then
    echo -e "${YELLOW}⚠️  phpMyAdmin 容器已存在${NC}"
    echo "正在停止并移除旧容器..."
    docker stop phpmyadmin > /dev/null 2>&1
    docker rm phpmyadmin > /dev/null 2>&1
    echo ""
fi

# 启动 phpMyAdmin
echo -e "${BLUE}🚀 正在启动 phpMyAdmin...${NC}"
echo ""

docker run --name phpmyadmin -d \
  -p 8081:80 \
  -e PMA_HOST=host.docker.internal \
  -e PMA_PORT=3307 \
  -e PMA_USER=root \
  -e PMA_PASSWORD=cmu123wyh \
  phpmyadmin/phpmyadmin

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ phpMyAdmin 启动成功！${NC}"
    echo ""
    
    # 等待容器启动
    echo -e "${BLUE}⏳ 等待 phpMyAdmin 初始化...${NC}"
    sleep 5
    
    # 检查容器状态
    if docker ps | grep -q "phpmyadmin"; then
        echo -e "${GREEN}✅ phpMyAdmin 容器运行正常${NC}"
        echo ""
        
        echo "========================================"
        echo "📊 访问信息"
        echo "========================================"
        echo ""
        echo -e "${GREEN}🌐 phpMyAdmin URL:${NC} http://localhost:8081"
        echo ""
        echo "登录信息:"
        echo "  用户名: root"
        echo "  密码:   cmu123wyh"
        echo "  数据库: selabdb"
        echo ""
        echo "========================================"
        echo "📋 验证步骤"
        echo "========================================"
        echo ""
        echo "1. 打开浏览器访问: http://localhost:8081"
        echo "2. 使用 root / cmu123wyh 登录"
        echo "3. 选择左侧的 'selabdb' 数据库"
        echo "4. 点击 '_user' 表查看用户数据"
        echo "5. 验证密码字段是否为 BCrypt 格式 (\$2a\$10\$...)"
        echo ""
        echo "========================================"
        echo "🔍 快速查询"
        echo "========================================"
        echo ""
        echo "在 phpMyAdmin 的 SQL 标签中执行以下查询："
        echo ""
        echo "-- 查看用户和密码格式"
        echo "SELECT id, username, email, "
        echo "       SUBSTRING(password, 1, 15) as password_prefix,"
        echo "       LENGTH(password) as password_length"
        echo "FROM _user;"
        echo ""
        echo "-- 查看用户角色"
        echo "SELECT u.id, u.username, GROUP_CONCAT(r.roles) as roles"
        echo "FROM _user u"
        echo "LEFT JOIN user_roles r ON u.id = r.user_id"
        echo "GROUP BY u.id, u.username;"
        echo ""
        echo "========================================"
        echo ""
        echo -e "${GREEN}🎉 phpMyAdmin 已准备就绪！${NC}"
        echo ""
        
        # 尝试在浏览器中打开
        echo -e "${BLUE}正在尝试在浏览器中打开...${NC}"
        if command -v open > /dev/null; then
            open http://localhost:8081
        elif command -v xdg-open > /dev/null; then
            xdg-open http://localhost:8081
        else
            echo "请手动打开浏览器访问: http://localhost:8081"
        fi
        
    else
        echo -e "${RED}❌ phpMyAdmin 容器启动失败${NC}"
        echo "查看日志: docker logs phpmyadmin"
        exit 1
    fi
else
    echo -e "${RED}❌ phpMyAdmin 启动失败${NC}"
    echo ""
    echo "尝试使用备用方法:"
    echo ""
    echo "方法1: 使用 Docker 网络"
    echo "docker run --name phpmyadmin -d \\"
    echo "  --network container:se331_lab7_backend_0912-db-1 \\"
    echo "  -p 8081:80 \\"
    echo "  -e PMA_HOST=127.0.0.1 \\"
    echo "  -e PMA_PORT=3306 \\"
    echo "  phpmyadmin/phpmyadmin"
    echo ""
    echo "方法2: 使用 MySQL Workbench"
    echo "主机: localhost"
    echo "端口: 3307"
    echo "用户: root"
    echo "密码: cmu123wyh"
    exit 1
fi

