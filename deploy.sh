#!/bin/bash
# ============================================
# Resume+ 一键部署脚本
# 适用: 阿里云/腾讯云学生机 (2核4G)
# 系统: Ubuntu 22.04 / Debian 12
# 用法: chmod +x deploy.sh && sudo ./deploy.sh
# ============================================

set -e

# ---- 配置（改成你自己的）----
DOMAIN="localhost"           # 你的域名或服务器 IP
MYSQL_PASSWORD="resume_plus_pass"  # 改掉！
JWT_SECRET="resume_plus_jwt_secret_change_me"
DEEPSEEK_API_KEY="sk-your-key-here"

# ---- 颜色 ----
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ---- 检查 root ----
[[ $EUID -eq 0 ]] || { error "请使用 sudo 运行"; exit 1; }

info "========= Resume+ 部署开始 ========="

# ---- 1. 安装 Docker ----
if ! command -v docker &>/dev/null; then
    info "安装 Docker..."
    curl -fsSL https://get.docker.com | bash
    systemctl enable docker && systemctl start docker
else
    info "Docker 已安装，跳过"
fi

# ---- 2. 安装 Docker Compose ----
if ! command -v docker-compose &>/dev/null && ! docker compose version &>/dev/null; then
    info "安装 Docker Compose..."
    apt update && apt install -y docker-compose-plugin
fi

# ---- 3. 安装 Java 17 ----
if ! command -v java &>/dev/null; then
    info "安装 Java 17..."
    apt update && apt install -y openjdk-17-jdk maven
else
    info "Java 已安装: $(java -version 2>&1 | head -1)"
fi

# ---- 4. 安装 Node.js 20 ----
if ! command -v node &>/dev/null; then
    info "安装 Node.js 20..."
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
    apt install -y nodejs
else
    info "Node.js 已安装: $(node -v)"
fi

# ---- 5. 启动依赖服务（Docker Compose）----
info "启动数据库和中间件..."
cat > docker-compose.prod.yml << 'COMPOSE'
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: resume-plus-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_DATABASE: ry_ai
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    ports:
      - "127.0.0.1:3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./sql/resume_table.sql:/docker-entrypoint-initdb.d/01-resume.sql
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: resume-plus-redis
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - redis-data:/data
    restart: unless-stopped

  postgres:
    image: pgvector/pgvector:pg17
    container_name: resume-plus-pgvector
    environment:
      POSTGRES_DB: ruoyi_vector
      POSTGRES_USER: ruoyi
      POSTGRES_PASSWORD: ${PGVECTOR_PASSWORD}
    ports:
      - "127.0.0.1:5433:5432"
    volumes:
      - pgvector-data:/var/lib/postgresql/data
    restart: unless-stopped

  gotenberg:
    image: gotenberg/gotenberg:8
    container_name: resume-plus-gotenberg
    ports:
      - "127.0.0.1:3000:3000"
    command:
      - "gotenberg"
      - "--chromium-disable-javascript=true"
      - "--chromium-allow-list=^file:///.*$|^http://localhost:.*$"
    restart: unless-stopped

volumes:
  mysql-data:
  redis-data:
  pgvector-data:
COMPOSE

# 设置环境变量
export MYSQL_PASSWORD="$MYSQL_PASSWORD"
export PGVECTOR_PASSWORD="$MYSQL_PASSWORD"  # 可以用同一个

docker compose -f docker-compose.prod.yml up -d

# 等待 MySQL 就绪
info "等待 MySQL 就绪..."
for i in {1..30}; do
    docker exec resume-plus-mysql mysqladmin ping -h localhost --silent && break
    sleep 2
done

# ---- 6. 构建前端 ----
info "构建前端..."
cd ruoyi-front
npm install --production
npm run build
cd ..

# ---- 7. 构建后端 ----
info "构建后端..."
cd ruoyi-backend
mvn clean package -DskipTests -q
cd ..

# ---- 8. 配置 Nginx ----
info "配置 Nginx..."
apt install -y nginx

cat > /etc/nginx/sites-available/resume-plus << 'NGINX'
server {
    listen 80;
    server_name _;
    client_max_body_size 100M;

    # 前端静态文件
    root /opt/resume-plus/ruoyi-front/dist;
    index index.html;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml image/svg+xml;
    gzip_min_length 1024;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }

    # SSE 流式对话
    location /ai/chat/stream {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
        gzip off;
    }

    # 上传文件（本地存储模式）
    location /uploads/ {
        alias /opt/resume-plus/uploads/;
        expires 7d;
    }
}
NGINX

ln -sf /etc/nginx/sites-available/resume-plus /etc/nginx/sites-enabled/default
systemctl restart nginx

# ---- 9. 配环境变量 ----
info "配置环境变量..."
mkdir -p /opt/resume-plus/uploads

cat > /opt/resume-plus/.env << ENV
DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
EMBEDDING_API_KEY=${DEEPSEEK_API_KEY}
PGVECTOR_PASSWORD=${MYSQL_PASSWORD}
JWT_SECRET=${JWT_SECRET}
MYSQL_USERNAME=root
MYSQL_PASSWORD=${MYSQL_PASSWORD}
ENV

# ---- 10. 创建 systemd 服务 ----
info "创建后端 systemd 服务..."
cat > /etc/systemd/system/resume-plus-backend.service << 'SERVICE'
[Unit]
Description=Resume+ Backend
After=network.target docker.service
Wants=docker.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/resume-plus/ruoyi-backend
EnvironmentFile=/opt/resume-plus/.env
ExecStart=/usr/bin/java -jar -Xms256m -Xmx512m \
    -Dspring.profiles.active=druid \
    /opt/resume-plus/ruoyi-backend/ruoyi-admin/target/ruoyi-admin.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable resume-plus-backend
systemctl start resume-plus-backend

# ---- 11. 加防火墙 ----
if command -v ufw &>/dev/null; then
    ufw allow 80/tcp
    ufw allow 22/tcp
fi

# ---- 完成 ----
IP=$(curl -s ifconfig.me 2>/dev/null || echo "your-server-ip")
echo ""
info "======================================"
info " 部署完成！"
info " 访问地址: http://${IP}"
info " 登录账号: admin / admin123"
info ""
info " 后端日志: journalctl -u resume-plus-backend -f"
info " 重启后端: systemctl restart resume-plus-backend"
info "======================================"
