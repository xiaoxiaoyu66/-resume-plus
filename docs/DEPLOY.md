# Resume+ 学生部署指南

> 学生开发、学生预算、生产标准。
> 目标：**最低 ¥9/月，跑完整套简历 AI 服务。**

---

## 一、服务器选购

### 推荐方案

| 平台 | 套餐 | 配置 | 价格 | 购买方式 |
|------|------|------|------|---------|
| **阿里云** | 云翼计划 | 2核2G | **¥9/月** | 学生认证后购买 |
| **阿里云** | 云翼计划 | 2核4G | **¥114/年** | 学生认证后购买 |
| **腾讯云** | 轻量服务器 | 2核4G | **¥108/年** | 学生认证后购买 |
| **华为云** | 云创计划 | 2核4G | **¥98/年** | 学生认证后购买 |

> **建议**：选 2核4G，一年一百出头，比一杯奶茶钱都便宜。阿里云/腾讯云的学生认证用学信网在线验证就行。

### 系统选择

**Ubuntu 22.04 LTS** — 文档最多、遇到问题好搜。

---

## 二、架构总览

```
用户 → Nginx (80端口)
         ├── / → 前端静态文件 (Vue 构建产物)
         ├── /api/ → 后端 API (Java, localhost:8080)
         └── /ai/chat/stream → SSE 流式 (禁用缓冲)
```

**后端依赖（全部 Docker 运行）：**

```
MySQL 8.0    → 业务数据（简历、用户、会话）
Redis 7      → 缓存 + Session
PostgreSQL   → PGVector 向量数据（岗位匹配）
Gotenberg    → Chromium PDF 导出
```

### 相比开发环境去掉了什么

| 组件 | 去掉原因 | 影响 |
|------|---------|------|
| **MinIO** | 改用 Redis TempFileStorage 临时存储 + 本地文件系统 | 不影响简历编辑和 AI 诊断，影响文件长期存储 |
| **Elasticsearch** | 太吃内存（1G+），部署初期用不到 | 全文搜索功能暂不可用 |
| **RabbitMQ** | 异步向量化在单用户场景无必要 | 文件上传改为同步处理 |

> 去掉这些后，空载内存占用从 ~2.8G 降到 **~1.2G**，2核4G 机器跑得很轻松。

---

## 三、部署方式（二选一）

### 方式一：一键部署脚本（推荐）

适合 Ubuntu 22.04 / Debian 12，已适配阿里云/腾讯云学生机。

```bash
# 1. 上传代码到服务器
scp -r ./* root@你的服务器IP:/opt/resume-plus/

# 2. SSH 登录服务器
ssh root@你的服务器IP

# 3. 编辑配置
cd /opt/resume-plus
vim deploy.sh   # 修改顶部的 DEEPSEEK_API_KEY 和密码

# 4. 运行部署脚本
chmod +x deploy.sh
sudo ./deploy.sh

# 5. 等 3-5 分钟，部署完成
```

部署脚本自动完成：
- 安装 Docker + Docker Compose
- 安装 Java 17 + Maven + Node.js 20
- 启动 MySQL / Redis / PostgreSQL / Gotenberg
- 构建前端并打包
- 构建后端 Jar 包
- 配置 Nginx 反向代理
- 创建 systemd 服务（开机自启 + 崩溃重启）
- 配置防火墙

### 方式二：手动分步部署

如果想自己控制每一步：

#### 1. 安装依赖

```bash
# Docker
curl -fsSL https://get.docker.com | bash
sudo apt install -y docker-compose-plugin

# Java 17 + Maven
sudo apt install -y openjdk-17-jdk maven

# Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo bash -
sudo apt install -y nodejs

# Nginx
sudo apt install -y nginx
```

#### 2. 启动依赖服务

```bash
cd /opt/resume-plus
docker compose -f docker-compose.prod.yml up -d
```

#### 3. 构建前端

```bash
cd ruoyi-front
npm install
npm run build
# 构建产物在 dist/ 目录
```

#### 4. 构建后端

```bash
cd ruoyi-backend
mvn clean package -DskipTests
# Jar 包在 ruoyi-admin/target/ruoyi-admin.jar
```

#### 5. 启动后端

```bash
# 启环境变量
export DEEPSEEK_API_KEY="sk-xxxxx"
export PGVECTOR_PASSWORD="your-password"
export JWT_SECRET="your-jwt-secret"
export MYSQL_PASSWORD="your-password"

# 启动
java -jar -Xms256m -Xmx512m ruoyi-admin/target/ruoyi-admin.jar
```

#### 6. 配置 Nginx

```bash
sudo cp nginx-default.conf /etc/nginx/sites-available/resume-plus
sudo ln -sf /etc/nginx/sites-available/resume-plus /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl restart nginx
```

---

## 四、环境变量清单

| 变量 | 必填 | 说明 |
|------|------|------|
| `DEEPSEEK_API_KEY` | ✅ | DeepSeek API 密钥 |
| `EMBEDDING_API_KEY` | ✅ | SiliconFlow API 密钥（可复用 DeepSeek key） |
| `PGVECTOR_PASSWORD` | ✅ | PostgreSQL 密码 |
| `JWT_SECRET` | ✅ | JWT 签名密钥（生成方法见下） |
| `MYSQL_PASSWORD` | ✅ | MySQL root 密码 |
| `MYSQL_USERNAME` | ❌ | 默认 root |
| `MINIO_ACCESS_KEY` | ❌ | 部署模式不需要 MinIO |
| `MINIO_SECRET_KEY` | ❌ | 部署模式不需要 MinIO |

> 生成安全的 JWT_SECRET：
> ```bash
> openssl rand -base64 32
> ```

---

## 五、服务器安全

### 防火墙（必做）

```bash
# 只开放 80(HTTP) 和 22(SSH)
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw enable
```

### 数据库不暴露外网

docker-compose.prod.yml 中所有数据库端口都绑定在 `127.0.0.1`，只有本机能访问：

```yaml
ports:
  - "127.0.0.1:3306:3306"   # 不会暴露到公网
```

### 开启 HTTPS（必做）

没有 HTTPS，面试官打开你项目第一眼就是「不安全」三个大字。Let's Encrypt 免费，配一次管三个月自动续。

```bash
# 1. 安装 certbot
sudo apt install -y certbot python3-certbot-nginx

# 2. 申请证书（需要有域名指向你的服务器 IP）
sudo certbot --nginx -d your-domain.com

# 3. 测试自动续期
sudo certbot renew --dry-run
```

> **没有域名？** 买个最便宜的，`.xyz` 首年 ¥6，`.top` 首年 ¥7。阿里云/腾讯云都能买。
> Certbot 会自动修改 Nginx 配置并添加定时续期任务，配完就不用管了。

验证 HTTPS 是否生效：

```bash
curl -I https://your-domain.com
# 返回 200 且 SSL 证书信息正确即可
```

---

## 六、日志管理（防磁盘写满）

学生服务器最常见的故障就是日志把磁盘撑爆。一键配好，永不担心。

### Nginx 日志轮转

系统自带 logrotate，默认已配 Nginx，检查一下：

```bash
cat /etc/logrotate.d/nginx
# 应该有：
# /var/log/nginx/*.log {
#     weekly          # 每周轮转
#     rotate 4        # 保留 4 周
#     compress        # 压缩旧日志
# }
```

### 后端日志轮转

```bash
sudo cat > /etc/logrotate.d/resume-plus-backend << 'EOF'
/opt/resume-plus/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    copytruncate
}
EOF

# 手动测试
sudo logrotate -d /etc/logrotate.d/resume-plus-backend
```

### Docker 容器日志（最大 100MB，保留 3 份）

```bash
# 全局配置所有新容器不超过 100MB
sudo cat > /etc/docker/daemon.json << 'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  }
}
EOF

# 重启 Docker 使配置生效
sudo systemctl restart docker

# 对已运行的容器手动清理（可选）
docker system prune -f --volumes
```

> 配好以后登录服务器看一眼 `df -h`，磁盘使用率应该长期稳定在 60% 以下。

---

## 七、数据库备份（防数据丢失）

简历数据是用户的心血。自动备份，每天一次，保留 7 天。

### 备份脚本

```bash
sudo mkdir -p /opt/resume-plus/backups

sudo cat > /opt/resume-plus/backup.sh << 'BACKUP'
#!/bin/bash
# Resume+ 自动备份脚本
# 备份 MySQL + PostgreSQL，保留 7 天

BACKUP_DIR="/opt/resume-plus/backups"
RETENTION_DAYS=7
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 加载环境变量
source /opt/resume-plus/.env 2>/dev/null

# 备份 MySQL
echo "[$(date)] 开始备份 MySQL..."
docker exec resume-plus-mysql mysqldump -u root -p"${MYSQL_PASSWORD}" --all-databases \
  | gzip > "${BACKUP_DIR}/mysql_${TIMESTAMP}.sql.gz"

# 备份 PostgreSQL（向量数据）
echo "[$(date)] 开始备份 PostgreSQL..."
docker exec resume-plus-postgres pg_dump -U ruoyi ruoyi_vector \
  | gzip > "${BACKUP_DIR}/pgsql_${TIMESTAMP}.sql.gz"

# 删除过期备份
find "${BACKUP_DIR}" -name "mysql_*.sql.gz" -mtime +${RETENTION_DAYS} -delete
find "${BACKUP_DIR}" -name "pgsql_*.sql.gz" -mtime +${RETENTION_DAYS} -delete

# 记录备份大小
echo "[$(date)] 备份完成，大小："
du -sh "${BACKUP_DIR}/mysql_${TIMESTAMP}.sql.gz"
du -sh "${BACKUP_DIR}/pgsql_${TIMESTAMP}.sql.gz"
BACKUP

sudo chmod +x /opt/resume-plus/backup.sh
```

### 加入定时任务（每天凌晨 3 点）

```bash
sudo crontab -e
# 加入这行：
0 3 * * * /opt/resume-plus/backup.sh >> /opt/resume-plus/backups/backup.log 2>&1
```

### 远程备份（可选，建议多一层保障）

```bash
# 安装 rclone（支持阿里云OSS/腾讯云COS/S3）
sudo apt install -y rclone

# 配好之后每天同步到云端
# crontab 再加一行：
30 3 * * * rclone sync /opt/resume-plus/backups remote:resume-plus-backups
```

> **备份恢复测试：**
> ```bash
> # MySQL 恢复
> gunzip < /opt/resume-plus/backups/mysql_20260508_030001.sql.gz | docker exec -i resume-plus-mysql mysql -u root -p"${MYSQL_PASSWORD}"
>
> # PostgreSQL 恢复
> gunzip < /opt/resume-plus/backups/pgsql_20260508_030001.sql.gz | docker exec -i resume-plus-postgres psql -U ruoyi ruoyi_vector
> ```

---

## 八、监控告警（防宕机不知情）

### UptimeRobot（免费，5 分钟检测一次）

```bash
# 1. 去 https://uptimerobot.com 注册
# 2. 添加监控器：
#    - Monitor Type: HTTP(s)
#    - URL: https://你的域名
#    - Interval: 5 minutes
# 3. 设置告警方式：Email 免费，Telegram/Slack 也支持
```

配上之后，服务挂了 5 分钟内你会收到邮件。

### 自建健康检查端点

项目本身有 `/health` 端点，Nginx 直接返回：

```bash
# 测试
curl http://localhost/health
# 返回: healthy
```

### 自定义监控脚本（可选）

```bash
sudo cat > /opt/resume-plus/monitor.sh << 'MONITOR'
#!/bin/bash
# Resume+ 健康检查脚本
# 检查后端、数据库、磁盘空间

# 1. 检查后端
if ! curl -sf http://localhost/health > /dev/null; then
    echo "[$(date)] 后端无响应，尝试重启..." >> /opt/resume-plus/monitor.log
    systemctl restart resume-plus-backend
fi

# 2. 检查磁盘（超过 85% 发告警）
USAGE=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$USAGE" -gt 85 ]; then
    echo "[$(date)] 磁盘使用率 ${USAGE}%，需要关注" >> /opt/resume-plus/monitor.log
fi
MONITOR

sudo chmod +x /opt/resume-plus/monitor.sh

# 每 10 分钟跑一次
sudo crontab -e
# 加入：
*/10 * * * * /opt/resume-plus/monitor.sh
```

---

## 九、日常维护

### 查看日志

```bash
# 后端日志
journalctl -u resume-plus-backend -f

# Docker 服务日志
docker logs resume-plus-mysql -f
docker logs resume-plus-redis -f

# Nginx 日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

### 重启服务

```bash
# 重启后端
sudo systemctl restart resume-plus-backend

# 重启全部
sudo systemctl restart resume-plus-backend
docker compose -f docker-compose.prod.yml restart
sudo systemctl restart nginx
```

### 更新代码

```bash
cd /opt/resume-plus
git pull

# 构建后端
cd ruoyi-backend && mvn clean package -DskipTests && cd ..

# 构建前端
cd ruoyi-front && npm install && npm run build && cd ..

# 重启后端
sudo systemctl restart resume-plus-backend
```

---

## 十、故障排查

| 问题 | 排查方法 |
|------|---------|
| **页面白屏 / 404** | `sudo nginx -t` 检查 Nginx 配置，`sudo systemctl status nginx` 检查运行状态 |
| **API 返回 502** | 后端没启动：`systemctl status resume-plus-backend` 查看 |
| **登录失败** | MySQL 是否就绪：`docker exec resume-plus-mysql mysqladmin ping` |
| **AI 对话无响应** | `journalctl -u resume-plus-backend -f` 看后端日志是否有 API 报错 |
| **PDF 导出失败** | Gotenberg 是否启动：`curl http://127.0.0.1:3000/health` |
| **内存不足** | 查看内存：`free -h`。如果不够就 `docker restart` 重启不需要的服务 |

---

## 十、费用总结

以阿里云 2核4G 学生机为例：

| 项目 | 费用 |
|------|------|
| 服务器 | ¥9.5/月（学生优惠） |
| 域名（可选） | ¥0-¥30/年 |
| DeepSeek API | ~¥0.1/月（重度使用） |
| **合计** | **≈ ¥10/月** |

> 一杯奶茶钱，跑一个能帮你拿 Offer 的工具。
> 这就是 Resume+ 存在的意义。
