# 项目问题记录与解决方案

> Resume+ AI 求职助手 — 开发与运维过程中遇到的问题及解决方案

## 目录

- [一、环境配置问题](#一环境配置问题)
- [二、依赖冲突问题](#二依赖冲突问题)
- [三、性能瓶颈问题](#三性能瓶颈问题)
- [四、部署问题](#四部署问题)
- [五、安全漏洞问题](#五安全漏洞问题)
- [附录](#附录)

---

## 一、环境配置问题

### 问题 1：DeepSeek API 返回 401 认证失败

- **问题描述**：调用 AI 对话或简历解析功能时，后端日志报错 `DeepSeek API 调用失败: 401`，前端显示"AI处理失败"。
- **错误日志**：

  ```
  DeepSeek API 调用失败: 401
  ```

- **根因分析**：项目的 `application-windows.yml` 中 DeepSeek API Key 配置为 `your-api-key-here`（占位符），未被替换为真实 Key。同时 `application.yml` 配置从环境变量 `${DEEPSEEK_API_KEY}` 读取，但 `windows` profile 中硬编码的假 Key 覆盖了环境变量配置。
- **解决方案**：

  方案 A（直接改配置文件）：
  1. 在 `application-windows.yml` 中找到 `deepseek.api.key` 字段
  2. 将 `your-api-key-here` 替换为真实 Key

  方案 B（环境变量，推荐）：
  1. 将 `application-windows.yml` 中的 `key: your-api-key-here` 改为 `key: ${DEEPSEEK_API_KEY}`
  2. 设置环境变量：
     ```powershell
     [System.Environment]::SetEnvironmentVariable('DEEPSEEK_API_KEY', 'sk-真实Key', 'User')
     ```
  3. 重启终端或重启后端进程

- **预防措施**：
  - 永远不要将占位符 Key 提交到 Git（当前已修复）
  - 使用环境变量而非硬编码配置
  - 在 CI 中增加配置检查：验证 `api-key` 不包含 "your-" 前缀

- **解决时间**：2026-05-05

---

### 问题 2：验证码始终提示失效 / 无法关闭验证码

- **问题描述**：即使将 `application-windows.yml` 中 `captcha.enabled` 设为 `false`，登录时仍然提示"验证码失效"。
- **错误日志**：

  ```
  {"msg": "验证码失效", "code": 500}
  ```

- **根因分析**：RuoYi 的验证码开关实际上存储在**数据库** `sys_config` 表的 `sys.account.captchaEnabled` 字段中，而非配置文件中。配置文件中的 `captcha.enabled` 仅用于 CaptchaController 的返回标记，不影响登录校验。

  ```java
  // SysLoginService.java
  boolean captchaEnabled = configService.selectCaptchaEnabled();
  // 该方法读取数据库 sys_config 表，而非 yml 配置
  ```

- **解决方案**：

  1. 通过管理员登录后进入「系统管理 → 参数设置」，找到 `sys.account.captchaEnabled`，将值改为 `false`
  2. 或直接操作数据库：
     ```sql
     UPDATE sys_config SET config_value = 'false'
     WHERE config_key = 'sys.account.captchaEnabled';
     ```
  3. 或临时在代码中跳过验证码校验（仅开发调试用）：
     ```java
     // SysLoginService.java
     public void validateCaptcha(...) {
         boolean captchaEnabled = configService.selectCaptchaEnabled();
         // if (captchaEnabled) { ... }  // 临时注释掉
     }
     ```

- **预防措施**：
  - 了解 RuoYi 框架的配置优先级：数据库 > yml > 环境变量
  - 首次部署后及时通过管理后台关闭验证码
  - 在开发环境数据库初始化脚本中预置 `captchaEnabled = false`

- **解决时间**：2026-05-05

---

### 问题 3：PGVector 连接失败 — `password authentication failed`

- **问题描述**：后端启动时报错 `FATAL: password authentication failed for user "ruoyi"`，无法连接到 PostgreSQL 数据库。
- **错误日志**：

  ```
  Caused by: org.postgresql.util.PSQLException: FATAL: password authentication failed for user "ruoyi"
  ```

- **根因分析**：`application.yml` 中 `pgvector.password` 配置为 `${PGVECTOR_PASSWORD:f39718ef552b38ad69f34f9a}`，但该段配置前有一行重复的 `password: ruoyi`（无效配置）。YAML 中后出现的同名字段会覆盖前者，但实际生效的值是空字符串，因为源文件中 `# 密码` 注释导致解析异常。

  ```yaml
  # 问题代码 (application.yml)
  pgvector:
    username: ruoyi
    password: ruoyi           # ← 被下面一行覆盖
    password: ${PGVECTOR_PASSWORD:f39718ef552b38ad69f34f9a}  # ← 实际生效
  ```

- **解决方案**：
  1. 清理配置文件中的重复字段，只保留一行：
     ```yaml
     pgvector:
       username: ruoyi
       password: ${PGVECTOR_PASSWORD:f39718ef552b38ad69f34f9a}
     ```
  2. 通过命令行参数覆盖：
     ```bash
     java -jar ruoyi-admin.jar --pgvector.password=f39718ef552b38ad69f34f9a
     ```

- **预防措施**：
  - 配置文件中避免出现重复字段
  - 启动前验证数据库连接：`PGPASSWORD=your_pw psql -h host -U user -d db -c "SELECT 1"`
  - 将敏感密码通过环境变量注入，而非硬编码

- **解决时间**：2026-05-05

---

### 问题 4：Gotenberg PDF 导出返回 404 Not Found

- **问题描述**：调用 PDF 导出接口时返回 HTTP 500，后端日志显示 Gotenberg 返回 404。
- **错误日志**：

  ```
  Gotenberg 返回错误: status=404, body=Not Found
  ```

- **根因分析**：代码中请求 Gotenberg 的端点为 `/forms/chromium`，但 Gotenberg 8 版本的 API 路径发生了变化。Gotenberg 7 及更早版本使用 `/forms/chromium`，Gotenberg 8 改为 `/forms/chromium/convert/html`。Docker 拉取的镜像是 `gotenberg/gotenberg:8`（最新 8.x 版本）。

  ```java
  // 问题代码
  .uri(URI.create(gotenbergUrl + "/forms/chromium"))  // ← Gotenberg 8 中不存在

  // 修复后
  .uri(URI.create(gotenbergUrl + "/forms/chromium/convert/html"))  // ← Gotenberg 8 的正确端点
  ```

- **解决方案**：
  1. 确认 Gotenberg 版本：`curl http://localhost:3000/health`
  2. 将代码中的端点路径改为 `/forms/chromium/convert/html`
  3. 也可以直接验证端点可用性：
     ```bash
     curl -X POST http://localhost:3000/forms/chromium/convert/html \
       -F "files=@index.html"
     ```

- **预防措施**：
  - 固定 Gotenberg 镜像的小版本号（如 `gotenberg/gotenberg:8.0`）而非使用 `:8` 大版本标签
  - 每次 Gotenberg 版本升级时查阅 Changelog 确认 API 变更
  - 在集成测试中包含 PDF 导出链路的验证

- **解决时间**：2026-05-05

---

### 问题 5：`/resume/export/pdf` 接口 500 — No static resource

- **问题描述**：前端调用 PDF 导出时返回 500，错误信息为 `No static resource resume/export/pdf for request '/resume/export/pdf'`。
- **错误日志**：

  ```
  No static resource resume/export/pdf for request '/resume/export/pdf'.
  ```

- **根因分析**：启动的 JAR 文件是较早构建的（5月2日），而 `ResumeController.java` 中的 `exportPdf()` 方法在 5月4日才添加。JAR 中不包含该接口的定义，导致 Spring Boot 无法找到对应的 `@PostMapping`，将其当作静态资源请求处理。

  ```
  ruoyi-admin.jar 构建时间: 2026-05-02 20:03
  ResumeController.java 修改时间: 2026-05-04 21:29  ← 更新于 JAR 构建之后
  ```

- **解决方案**：
  1. 重新编译项目：`mvn clean package -DskipTests -pl ruoyi-admin -am`
  2. 确认新 JAR 构建时间正确：`ls -la target/ruoyi-admin.jar`
  3. 验证接口存在：`curl -X POST http://localhost:8080/resume/export/pdf`

- **预防措施**：
  - 开发时尽量从 IDE 直接运行（使用编译的 class 而非 JAR），避免 JAR 过旧
  - Maven 构建命令中加入时间戳输出，便于排查
  - 在 CI 中每次构建时自动运行

- **解决时间**：2026-05-05

---

### 问题 6：Gotenberg 服务未启动

- **问题描述**：PDF 导出返回 500，后端日志显示 `Gotenberg 连接失败`。
- **错误日志**：

  ```
  Gotenberg 连接失败: url=http://192.168.132.136:3000, error=Connection refused
  ```

- **根因分析**：Gotenberg 是一个独立 Docker 容器，需要手动启动。项目的 `application-windows.yml` 配置了 Gotenberg 地址为 Kali 虚拟机的 `192.168.132.136:3000`，但该容器未运行。

- **解决方案**：

  1. 在 Kali 虚拟机中启动 Gotenberg：
     ```bash
     docker run -d \
       --name gotenberg \
       --restart unless-stopped \
       -p 3000:3000 \
       gotenberg/gotenberg:8 \
       gotenberg --chromium-disable-javascript=true \
                 --chromium-allow-list="^file:///.*$|^http://localhost:.*$"
     ```
  2. 验证服务状态：
     ```bash
     curl http://192.168.132.136:3000/health
     # 预期输出: {"status":"up","details":{"chromium":{"status":"up"},...}}
     ```

- **预防措施**：
  - 将 Gotenberg 加入 Docker Compose 的统一编排
  - 在后端启动时增加 Gotenberg 健康检查
  - 在项目文档中明确列出所有需要预先启动的外部服务

- **解决时间**：2026-05-05

---

### 问题 7：Maven JAR 进程占用导致编译失败

- **问题描述**：执行 `mvn clean package` 时失败，错误信息为无法删除 `target/ruoyi-admin.jar`。
- **错误日志**：

  ```
  Failed to clean project: Failed to delete D:\...\ruoyi-admin\target\ruoyi-admin.jar
  ```

- **根因分析**：Spring Boot 可执行 JAR 正在被 Java 进程使用（后端正在运行），Windows 系统锁定文件，Maven 的 clean 阶段无法删除。

- **解决方案**：

  ```bash
  # 1. 查找占用 JAR 的 Java 进程
  netstat -ano | grep ":8080 "
  # 或
  tasklist | findstr java

  # 2. 结束进程
  taskkill //F //PID <进程ID>

  # 3. 重新编译
  mvn clean package -DskipTests -pl ruoyi-admin -am
  ```

- **预防措施**：
  - 编译前先停掉正在运行的后端
  - 使用 `mvn package -DskipTests`（不带 clean）跳过删除步骤，直接覆盖

- **解决时间**：2026-05-05

---

## 二、依赖冲突问题

> 暂无记录。建议补充：`mvn dependency:tree` 输出中发现的冲突及解决方案。

---

## 三、性能瓶颈问题

> 建议补充以下性能数据：

### 预期需要关注的问题

1. **SSE 流式对话内存占用** — 长对话场景下，`fullResponse` 累加可能导致 GC 压力
2. **PDF 导出大 HTML** — 简历内容复杂时（含 base64 图片），Gotenberg 渲染时间可能超过 10s
3. **文件向量化吞吐** — RabbitMQ 消费者处理能力与嵌入 API 限速的平衡
4. **Redis 缓存命中率** — 语义缓存阈值 0.85 是否合理，需要实际数据验证

---

## 四、部署问题

### 问题 1：Docker 镜像国内下载慢

- **问题描述**：`docker pull gotenberg/gotenberg:8` 等镜像下载速度极慢或超时。
- **根因分析**：Docker Hub 官方源在中国大陆访问受限。
- **解决方案**：

  ```bash
  # 配置 Docker 镜像加速器
  sudo mkdir -p /etc/docker
  sudo tee /etc/docker/daemon.json <<-'EOF'
  {
    "registry-mirrors": [
      "https://docker.1ms.run",
      "https://docker.xuanyuan.me"
    ]
  }
  EOF
  sudo systemctl restart docker

  # 或使用镜像源拉取后重新打标签
  docker pull docker.1ms.run/gotenberg/gotenberg:8
  docker tag docker.1ms.run/gotenberg/gotenberg:8 gotenberg/gotenberg:8
  ```

- **预防措施**：在 CI/CD 中使用国内镜像源

---

### 问题 2：MySQL 远程连接拒绝

- **问题描述**：从 Windows 主机连接 Kali 虚拟机的 MySQL 时报错 `Access denied for user 'root'@'192.168.132.1'`。
- **错误日志**：

  ```
  ERROR 1045 (28000): Access denied for user 'root'@'192.168.132.1' (using password: YES)
  ```

- **根因分析**：MySQL 8.0 默认 `root` 用户只允许从 `localhost` 连接，不允许从远程主机登录。虽然应用通过 JDBC 可以连接，但直接命令行/客户端连接被拒绝。

- **解决方案**：

  ```bash
  # 在 Kali 虚拟机中执行，授权 root 远程访问
  docker exec -it share_mysql mysql -u root -proot123 \
    -e "CREATE USER 'root'@'192.168.132.%%' IDENTIFIED BY 'root123'; \
        GRANT ALL PRIVILEGES ON *.* TO 'root'@'192.168.132.%%'; \
        FLUSH PRIVILEGES;"
  ```

- **预防措施**：
  - 使用 Docker 部署 MySQL 时设置 `MYSQL_ROOT_HOST=%` 环境变量
  - 创建专用的远程访问用户而非直接开放 root

---

## 五、安全漏洞问题

### 问题 1：API Key 硬编码在配置文件中

- **问题描述**：`application-windows.yml` 中 DeepSeek API Key、`application.yml` 中 MinIO 密钥等敏感信息以明文方式存储在代码中。
- **风险等级**：高
- **根因分析**：开发时为了方便直接填入 Key，但配置文件会被提交到 Git 仓库。
- **解决方案**：
  1. 将所有密钥迁移到环境变量：
     ```yaml
     # 好方式
     deepseek:
       api:
         key: ${DEEPSEEK_API_KEY}

     # 差方式
     deepseek:
       api:
         key: sk-xxxxx  # 不要这样做
     ```
  2. 使用 `.env` 文件管理环境变量（需加入 `.gitignore`）
  3. 检查 Git 历史中是否有泄露的 Key：`git log -p | grep "sk-"`

- **预防措施**：
  - 提交前使用 `git diff --check` 检查是否包含敏感信息
  - 在 CI 中增加密钥泄露扫描
  - 使用 `.gitignore` 排除所有 `.env` 和 `*credentials*` 文件
  - 如果 Key 已泄露，立即到 DeepSeek/MinIO 控制台吊销并重新生成

---

### 问题 2：配置文件中包含数据库密码

- **问题描述**：`application-windows.yml` 和 `application-druid.yml` 中明文存储 MySQL/PostgreSQL 密码。
- **风险等级**：中
- **解决方案**：
  ```yaml
  # 改为环境变量方式
  spring:
    datasource:
      password: ${MYSQL_PASSWORD}
  ```
- **预防措施**：生产环境使用外部密钥管理服务（如阿里云 KMS、HashiCorp Vault）

---

## 附录

### 常用调试命令

```bash
# ===== 后端 =====

# 查看日志（实时）
tail -f logs/backend.log

# 查看错误日志
tail -f logs/sys-error.log

# 检查端口占用
netstat -ano | grep ":8080 "

# 查找 Java 进程
ps aux | grep java        # Linux
tasklist | findstr java   # Windows

# 健康检查
curl http://localhost:8080/

# 测试登录
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","code":"","uuid":""}'

# 测试 PDF 导出
curl -X POST http://localhost:8080/resume/export/pdf \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"html":"<h1>Test</h1>"}' -o test.pdf

# ===== Docker =====

# 检查 Gotenberg 状态
curl http://192.168.132.136:3000/health

# 查看容器日志
docker logs gotenberg

# 进入容器
docker exec -it gotenberg sh

# ===== 数据库 =====

# 直接测试 PGVector 连接
PGPASSWORD="your_pw" psql -h 192.168.132.136 \
  -p 5433 -U ruoyi -d ruoyi_vector -c "SELECT 1"

# 查询验证码开关
SELECT config_key, config_value FROM sys_config
WHERE config_key = 'sys.account.captchaEnabled';

# ===== Redis =====

# 查看验证码缓存
redis-cli -h 192.168.132.136 KEYS "captcha_codes:*"
```

### 后端启动参数备忘

```bash
# 完整启动命令（Windows 开发环境）
java -jar ruoyi-admin.jar \
  --spring.profiles.active=druid,windows \
  --pgvector.password=f39718ef552b38ad69f34f9a

# 通过环境变量传递敏感信息
DEEPSEEK_API_KEY="sk-xxx" \
  java -jar ruoyi-admin.jar \
  --spring.profiles.active=druid,windows
```

### 常见错误码速查

| HTTP 状态码 | 场景 | 常见原因 |
|-------------|------|----------|
| 401 | 登录/API 调用 | Token 过期 / API Key 无效 / 验证码错误 |
| 404 | API 调用 | JAR 过旧 / Gotenberg 端点不正确 |
| 500 | 导出 | Gotenberg 未运行 / HTML 格式问题 |
| 500 | AI 对话 | DeepSeek API 调用失败 / 配额不足 |
| 502 | 网关 | Nginx 代理后端失败 / 后端未启动 |
| 503 | 服务 | 依赖服务（MySQL/Redis）不可达 |
