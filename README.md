# Resume+ — AI 中文简历诊断平台

<p align="center">
  <img src="https://raw.githubusercontent.com/xiaoxiaoyu66/-resume-plus/master/ruoyi-front/public/resume-plus-logo.svg" alt="Resume+ Logo" width="130" />
</p>

---

## 在线体验

<p align="center" style="font-size:20px;padding:16px;background:#e8f4fd;border-radius:10px;border:2px solid #667eea;">
  🔗 <strong><a href="https://he2wa.xin" target="_blank">https://he2wa.xin</a></strong>
</p>

> **⚠️ 服务器是 2核4G 学生机，负载能力有限，请勿上传真实敏感信息！**
>
> 已有 20+ 用户使用，如果加载慢说明何二娃的服务器又在喘气了。

<p align="center">
  <a href="https://he2wa.xin" target="_blank">
    <img src="https://img.shields.io/badge/在线体验-HTTPS_🔒-success?style=flat-square" alt="Online" />
  </a>
  <img src="https://img.shields.io/badge/版本-v1.0.0-blue?style=flat-square" alt="Version" />
  <img src="https://img.shields.io/badge/license-MIT-green?style=flat-square" alt="License" />
  <img src="https://img.shields.io/github/actions/workflow/status/xiaoxiaoyu66/-resume-plus/backend-ci.yml?style=flat-square&label=backend" alt="Backend CI" />
  <img src="https://img.shields.io/github/actions/workflow/status/xiaoxiaoyu66/-resume-plus/frontend-ci.yml?style=flat-square&label=frontend" alt="Frontend CI" />
</p>

---

## 演示视频

<p align="center">
  <a href="https://he2wa.xin/profile/demo-video/demo.mp4" target="_blank">
    <img src="https://he2wa.xin/profile/demo-video/demo.mp4" alt="点击播放演示视频" width="80%" style="max-width:720px;border-radius:8px;border:2px solid #667eea;" onerror="this.style.display='none'">
  </a>
  <br/>
  <sub>📹 点击上方图片播放演示视频（<a href="https://he2wa.xin/profile/demo-video/demo.mp4">直接下载 mp4</a>）</sub>
  <br/>
  <sub>简历解析 → AI 诊断 → 面试模拟 → 岗位匹配</sub>
</p>

> **GitHub 不支持内嵌视频播放。** 点击上方图片或 [此链接](https://he2wa.xin/profile/demo-video/demo.mp4) 在线观看。
> 如果加载慢说明服务器在喘气，请耐心等待或下载后观看。


---

## 项目起源

一根重庆学生在大热天改简历改到烦躁，心想能不能有个工具——告诉它要求，它直接帮忙写简历、匹配岗位、甚至投递。说白了就是懒。于是自己动手做，就有了 Resume+。

基于 RuoYi v3.9.2 深度定制，聚焦中文简历解析与 AI 辅助诊断，将**简历编辑 → AI 诊断 → PDF/Word 导出 → 岗位匹配 → 面试辅导**串联成完整求职链路。

> 作为学生项目，在工程规范上仍有提升空间。
>
> 注：本项目在开发过程中使用了 AI 辅助编程工具（Claude Code），但**核心业务逻辑架构与系统设计均由人工完成**。每一行代码都经过理解和修改。

<p align="center">
  <a href="#-功能矩阵">功能矩阵</a> •
  <a href="#-技术栈">技术栈</a> •
  <a href="#-系统架构">架构</a> •
  <a href="#-设计备忘">设计备忘</a> •
  <a href="#-安全体系">安全</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-项目结构">结构</a> •
  <a href="#-路线图">路线图</a>
</p>

---

## 功能矩阵

| 模块 | 功能 | 说明 |
|------|------|------|
| **AI 引擎** | DeepSeek SSE 流式对话（千文免费模型兜底） | 首 token ≈ 800ms–2s（2核4G学生机实测） |
| | 双模式面试：HR 行为面 + 专业面试 | HR 面关注软素质/稳定性，专业面按目标岗位动态出题 |
| | 面试追问逻辑 | 连续追问 2–3 轮挖透话题，非一次性出题 |
| | 滑动上下文窗口（5 轮 + 3000 token） | 平衡记忆深度与 token 消耗 |
| | 双层缓存（Caffeine L1 + Redis L2） | 关键词前缀匹配（前 15 字 MD5），非语义相似度 |
| | PDF 排版还原（CoordinateTextStripper） | y 坐标排序 + 两栏布局检测 |
| | AI 解析结果校验重试 | 自动检查关键字段，缺失时重问 |
| **简历管理** | 模块化编辑器（7 大模块） | 基本信息 / 教育 / 经历 / 项目 / 技能 / 意向 / 评价 |
| | 三套模板渲染 + 配色主题 | 经典 / 简约 / 现代，支持自定义颜色、纸张质感 |
| | Undo/Redo（50 层快照） | 全局状态回溯 |
| | AI 四维诊断 | 评分 + 建议 + 润色 + 关键词 |
| | PDF / PNG / Word 导出 | Gotenberg 渲染 PDF，html2canvas 生成 PNG，POI 生成 Word |
| | 文件解析 | PDF/Word 上传 → 排版还原 → AI 提取结构化 JSON → 校验重试 |
| **岗位匹配** | PGVector 向量存储 | ivfflat 索引，语义级技能匹配 |
| | BGE-M3 嵌入 + 余弦相似度 | 基于简历内容自动计算岗位匹配度 |
| **用户系统** | JWT 鉴权 | Spring Security 集成 |
| | 多方式登录 | 密码 / 短信（需企业资质）/ 微信扫码（需企业资质） |
| **基础设施** | Docker Compose 编排 | 一键启动所有依赖服务 |
| | GitHub Actions CI | 前端 + 后端自动构建测试 |

---

## 技术栈

### 后端

| 技术 | 用途 |
|------|------|
| Java 17 + Spring Boot 4.0.3 | 运行时 + 应用框架 |
| Spring Security 6.x + JWT | 认证授权 |
| MyBatis + Druid + PageHelper | 数据访问 |
| DeepSeek V4 API | 大语言模型（V4-Flash / V4-Pro） |
| PGVector + BGE-M3 | 向量存储与嵌入 |
| Caffeine + Redis | 双层语义缓存 |
| Gotenberg | Chromium PDF 导出 |
| MinIO | 对象存储 |
| RabbitMQ | 异步任务队列 |
| PDFBox + Apache POI | 文件解析 |

### 前端

| 技术 | 用途 |
|------|------|
| Vue 3.4 + Vite 5 | 前端框架 + 构建 |
| Element Plus 2.5 | UI 组件库 |
| Pinia + Vue Router 4 | 状态管理 + 路由 |
| TypeScript | 类型安全 |
| Axios | HTTP 客户端 |
| html2canvas | PNG 导出 |
| Vitest | 单元测试 |

### 关键选型理由

| 决策 | 理由 |
|------|------|
| **RuoYi vs 自研** | 复用成熟的 RBAC、代码生成、定时任务基础设施 |
| **Gotenberg vs iText** | Chromium 渲染 HTML 转 PDF，精确还原 CSS，免商业许可 |
| **PGVector vs Pinecone** | PostgreSQL 扩展，无需额外维护向量数据库 |
| **DeepSeek vs OpenAI** | 价格约为 GPT-4 的 1/20，中文理解优秀 |
| **SSE vs WebSocket** | 单向流满足 AI 对话，实现简单、自动重连 |
| **BGE-M3 vs OpenAI Embeddings** | 开源嵌入模型，可本地部署，768 维向量兼顾精度与性能 |

---

## 系统架构

![系统架构图](https://raw.githubusercontent.com/xiaoxiaoyu66/-resume-plus/master/docs/architecture.svg)

---

## 设计备忘

### PDF 排版还原：两栏布局检测算法

简历 PDF 最常见的排版是左栏（技能/语言/自我评价）+ 右栏（经历/项目），但 PDFBox 按页面渲染顺序吐出字符，左右栏文字交错。直接拼接会得到 "技能：Python 2020-2023 后端开发..." 这样的乱序文本。

**方案**：继承 PDFBox 的 `PDFTextStripper`，在字符级别截获坐标，先按 Y 排序恢复行序，再检测两栏布局并重构阅读顺序。检测两栏的核心判断：超过 20% 的行内部存在大于该行跨度 30% 的空隙则判定为两栏。阈值通过十几份不同排版简历验证。

**翻车记录**：
- Mac 和 Windows 上同份 PDF 行高不一致——改为统计相邻字符 Y 差值的众数估算行高
- 窄栏宽度不足 30px 时易误判——跳过短行
- 字符 Y 坐标微小抖动（±1px）——合并行时使用 70% 行高容差

完整源码见 `CoordinateTextStripper.java`，约 220 行。

### 双层缓存

配置了 `similarity-threshold: 0.85`，但当前实现是取用户输入的前 15 个字符（去标点）做 MD5 索引，命中直接返回。不是真向量语义相似度。

**为什么不做真语义**：向量缓存需要每次查询多一次 BGE-M3 推理 + 一次 PGVector ANN 检索。对于每天配额 500 条的学生项目，大部分重复提问是同一句话重发（前端重试/用户刷新），前缀匹配已经够用。0.85 配置留给将来换真实语义相似度的占位。

**架构**：L1 Caffeine（进程内，1000 条，10 分钟 TTL）+ L2 Redis（分布式，60 分钟 TTL）。命中链 L1 → L2 → 相似度匹配，L2 命中回写到 L1。`/ai/chat/cache/stats` 可查看命中率。

---

## 安全体系

| 特性 | 说明 |
|------|------|
| **JWT + Spring Security RBAC** | 完整的角色–权限–菜单三级权限控制 |
| **XSS 全局过滤** | 所有用户输入自动转义 |
| **防盗链** | Referer 白名单校验 |
| **Druid SQL 防火墙** | Wall 防 SQL 注入 + 慢 SQL 监控 |
| **密码安全策略** | 连续错误 5 次锁定 10 分钟 |
| **环境变量驱动** | 所有密钥使用 `${VAR}` 占位 |

### 安全审计（实战修复）

| 漏洞 | 风险 | 修复方案 |
|------|------|---------|
| **SSE userId 越权** | 可冒充他人调用 AI 对话 | 改为从 JWT 解析，拒绝客户端参数 |
| **XSS 存储型** | 简历内容可注入 `<script>` | 前端双层过滤 + 白名单模式 |
| **AccessKey 硬编码** | API 密钥泄露即失控 | 全部迁出到 `${}` 占位 |
| **跨域过度开放** | `@CrossOrigin(origins = "*")` | 删除注解，由 Nginx 统一管理 |
| **SQL 注入** | URL 参数可注入 `union select`、`drop table` 等恶意 SQL | Nginx 层正则过滤关键词：`union.*select`、`sleep()`、`waitfor`、`benchmark`、`create/drop table`，命中直接 `return 444`（连接断开），不给任何响应。Druid Wall 二层拦截，文件上传 10MB 硬上限防任意文件写入。 |

---

## Token 消耗（2核4G学生机，单用户）

| 环节 | 输入 tokens | 说明 |
|------|------------|------|
| 简历内容提取 | ~500–800 | PDF/Word 解析后的结构化文本 |
| AI 诊断回复 | ~400–600 | 四维评分 + 具体修改建议 |
| **一轮诊断合计** | **~1200–1800** | 约半页中文 |
| **整场会话** | **~2000–3000** | 被 3000 token 硬上限卡死 |

按 DeepSeek V4-Flash 缓存命中价 **¥0.02/百万 tokens** 计算，一次诊断 ≈ 万分之 0.3 分钱，随便用。

---

## 已知限制

| 限制 | 说明 | 未来计划 |
|------|------|---------|
| **单用户无并发** | 2核4G 学生机，未做并发压测 | 压测后调优连接池 |
| **语义缓存是前缀匹配** | 取前 15 字做 MD5 索引，非向量语义 | 用量上去后换真实向量检索 |
| **PDF 模板仅 3 套** | 现代 / 经典 / 简约 | 社区贡献模板 |
| **面试仅支持中文** | 不可中英文混合面试 | 多语言面试 |
| **Gotenberg 依赖 Docker** | PDF 渲染依赖 Chromium 容器 | 前端 jsPDF 兜底已就绪 |

---

## 快速开始

### 环境要求
- Docker 24+（可只装 MySQL/Redis）
- Node.js 18+（推荐 20 LTS）
- JDK 17+
- Maven 3.8+

### 1. 启动依赖服务（推荐 Docker Compose）

```bash
# 一条命令启动所有依赖（MySQL + Redis + PostgreSQL + Gotenberg）
docker compose -f docker-compose.prod.yml up -d mysql redis postgres gotenberg
```

或用 `docker run` 逐个启动：

```bash
# MySQL 8.0
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8

# Redis 7
docker run -d --name redis -p 6379:6379 redis:7

# PostgreSQL + PGVector（不用岗位匹配可跳过）
docker run -d --name postgres -e POSTGRES_PASSWORD=your-password -p 5433:5432 pgvector/pgvector:pg16

# MinIO（可选，文件存储用）
docker run -d --name minio -p 9000:9000 -p 9001:9001 minio/minio server /data --console-address ":9001"

# Gotenberg（可选，前端有 jsPDF 兜底）
docker run -d --name gotenberg -p 3000:3000 gotenberg/gotenberg:8
```

### 2. 初始化数据库

```bash
mysql -h 127.0.0.1 -u root -p < sql/resume_table.sql
```

### 3. 配置环境变量

```bash
export DEEPSEEK_API_KEY=sk-your-key
export MYSQL_PASSWORD=root
export JWT_SECRET=$(openssl rand -base64 32)
```

完整变量见[环境变量参考](#环境变量参考)。Windows 用户推荐使用 `restart-back.bat`。

### 4. 启动后端

```bash
cd ruoyi-backend
mvn spring-boot:run -DskipTests
```

后端启动后监听 `http://localhost:8080`。

### 5. 启动前端

```bash
cd ruoyi-front
npm install && npm run dev
```

> npm 慢可换 cnpm：`npm install -g cnpm --registry=https://registry.npmmirror.com && cnpm install`

### 6. 访问

打开 `http://localhost:3000`，使用 `admin / admin123` 登录。

---

## 环境变量参考

| 变量 | 用途 | 获取方式 |
|------|------|---------|
| `DEEPSEEK_API_KEY` | DeepSeek 大模型 API | [platform.deepseek.com](https://platform.deepseek.com/) |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 文件存储 | MinIO 控制台 |
| `PGVECTOR_HOST` / `PGVECTOR_PASSWORD` | 向量数据库 | PostgreSQL 连接信息 |
| `EMBEDDING_API_KEY` | BGE-M3 嵌入服务 | SiliconFlow API |
| `JWT_SECRET` | Token 签名 | 任意随机字符串 |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 业务数据库 | MySQL 凭据 |
| `ALIYUN_SMS_*` | 短信登录（可选） | 阿里云 SMS |
| `WX_APP_SECRET` | 微信扫码（可选） | 微信开放平台 |

> 不要在代码里硬编码密钥！现有代码已全部使用 `${VAR}` 占位。

---

## 一键部署

```bash
sudo ./deploy.sh
```

详见 [docs/DEPLOY.md](./docs/DEPLOY.md)。

| 服务 | 内存 | 必选 |
|------|------|------|
| MySQL 8.0 | ~300MB | ✅ |
| Redis 7 | ~50MB | ✅ |
| PostgreSQL + PGVector | ~200MB | ✅ |
| Gotenberg | ~80MB | ✅ |
| MinIO / ES / RabbitMQ | ~1.2GB | ❌ 可去掉 |

---

## 项目结构

```
resume-plus/
├── ruoyi-backend/             # Spring Boot 多模块
│   ├── ruoyi-admin/           # 启动入口 + AI 控制器（含 CoordinateTextStripper）
│   ├── ruoyi-common/          # 公共工具
│   ├── ruoyi-framework/       # 安全 + 配置
│   ├── ruoyi-system/          # 系统业务
│   ├── ruoyi-generator/       # 代码生成
│   └── ruoyi-quartz/          # 定时任务
│
├── ruoyi-front/               # Vue 3 + Vite + TypeScript
│   ├── src/views/             # 简历编辑、江城聘、聊天等页面
│   ├── src/store/             # Pinia 状态管理
│   ├── src/composables/       # 组合式逻辑
│   ├── src/api/               # API 模块
│   └── public/                # 静态资源
│
├── sql/                       # 数据库建表脚本
├── docs/                      # AI 提示词、技术文档
├── nginx/                     # Nginx 配置
└── docker-compose.yml         # Docker 编排
```

---

## 测试

| 模块 | 用例数 | 覆盖内容 |
|------|--------|---------|
| 前端 composables | 178 | SSE、会话管理、上传、编辑、登录 |
| 后端 service | 39 | 缓存、向量化、匹配、滑动窗口、SSE |

---

## 常见问题

<details>
<summary><b>PDF 导出报 500 / Gotenberg 连不上</b></summary>

检查 Gotenberg 是否在运行：`docker ps | grep gotenberg`。如果没启动：`docker run -d -p 3000:3000 gotenberg/gotenberg:8`。配置文件 `application.yml` 里的 `gotenberg.url` 要指向正确的地址（Docker 内用 `http://gotenberg:3000`，本地用 `http://localhost:3000`）。
</details>

<details>
<summary><b>上传文件解析失败 / MinIO 报错</b></summary>

MinIO 没启动或地址不对。启动 MinIO：`docker run -d -p 9000:9000 -p 9001:9001 minio/minio server /data --console-address ":9001"`。重置密码后同步修改 `application.yml` 中的 `minio.access-key` 和 `minio.secret-key`。
</details>

<details>
<summary><b>npm install 太慢 / 报错</b></summary>
用 cnpm：`npm install -g cnpm --registry=https://registry.npmmirror.com && cnpm install`，或直接配镜像：`npm config set registry https://registry.npmmirror.com`。
</details>

<details>
<summary><b>能跑但不能注册 / 收不到短信</b></summary>

短信功能需要企业资质（阿里云 SMS 审核），学生项目无法通过。可用 `admin / admin123` 登录，或直接在数据库 `sys_user` 表手动加账号。
</details>

---

## 开源 AI 提示词

项目的四组 system prompt（综合助手、简历分析、面试辅导、职业规划）完全公开，见 [docs/AI-PROMPTS.md](./docs/AI-PROMPTS.md)。

Prompt 只是起点，真正的壁垒在于工程实现：上下文窗口管理、语义缓存命中、文件向量化、滑动窗口记忆。

---

## 路线图

- [x] AI 简历诊断 + 描述润色
- [x] 双模式模拟面试（HR 面 + 专业面）
- [x] 岗位匹配（PGVector + BGE-M3）
- [x] PDF / Word / PNG 导出
- [ ] **自动投递** — 对接主流招聘平台 API（需企业资质）
- [ ] **自定义简历样式** — 把选择权交给用户，用户自己设计模板
- [ ] **团队协作** — 多用户协同编辑
- [ ] **线上（含作品集）/ 线下（精华）双模式**

> 画饼不代表能实现，但饼都不画何二娃连起床的动力都没有。

---

## 痛点 & 方向

**简历样式自定义**：目前只有三套模板（不好看）因为我是一个人开发，而不是大中厂有模板库。而且目前 AI 生成的模板都千篇一律，好看的也要钱。不如直接把模板的定义权交给用户，让用户自己设计想要的模板，不愿意设计的用户直接选模板库就好。（目前还未开发，这个需要动底层架构）

**简历推荐匹配的岗位**：没数据源，爬虫又违法。优化方向——做成"你的专业方向指南"，每个专业对标的岗位详解，让各专业的同学知道自己的专业能去哪。如果有更好的建议，何二娃愿意倾听。

---

## 致谢

这项目就是用一堆开源轮子攒出来的。感谢这些大佬们，让我一个学生也能攒出个能跑的东西。

**感谢 DeepSeek**（梁文锋义父二娃现叫了） — API 是真的便宜。V4-Flash 缓存命中 ¥0.02/百万 tokens，一次诊断 ≈ 万分之 0.3 分钱，随便用、随便聊，不用心疼 token。SSE 流式体验很好，首 token 基本秒回。感谢 DeepSeek 的降价，让屁股比脸干净的也能做 AI 应用。

**感谢以下项目（按使用顺序排）**

- **Vue 3 + Vite** — 热更新真的快，改一行代码一秒看到效果
- **RuoYi** — 好用遭了
- **Element Plus** — 简历编辑器 7 大模块拖拖拽拽就出来了
- **PDFBox** — CoordinateTextStripper 调了两周，但没它真搞不定左右栏交错
- **Gotenberg** — PDF 导出一次过
- **PGVector + BGE-M3** — 岗位匹配的核心，向量检索跑起来的那一刻已经在为自己鼓掌了
- **Docker** — 没有 Docker 根本部署不明白
- **Spring Security** — 虽然学的时候很痛苦，但确实比手写拦截器靠谱一万倍
- **[PoleBrief](https://www.polebrief.com/)** — 简历编辑器设计参考

以及感谢所有在 GitHub 上把文档写得清清楚楚的开源作者。你们随手写的一段 README，可能就救了一个学生的一个通宵。

最后，这个项目 MIT 协议，随便抄。**如果你是学生，觉得这个符合你的课题，拿去当课设或毕设完全没问题**（何二娃同意了——点点 star 义父义妈们）。唯一的请求：改出了更好玩的版本，喊何二娃一声，何二娃高低尝尝咸淡。

> "都是同龄人，给我来点酱味大鸡！"

**——何二娃**

---

## 贡献指南

**提 Issue**：Bug / 功能建议直接开 issue，附上复现步骤或截图。

**提 PR**：
1. Fork 后从 `master` 切功能分支
2. 前后端都通过 `npm run lint` / `mvn verify`
3. PR 标题写清楚改了啥，描述附上前后对比截图（如有 UI 变更）
4. 统一风格后再合入

代码规范没多严格，何二娃自己写代码也飞叉叉的，但至少别比现有代码乱。

---

## 许可证

MIT License，基于 [RuoYi v3.9.2](https://ruoyi.vip/) 扩展开发。

---

> 其实能做出一个这个已经很不容易了，做的不好的地方轻点喷兄弟们，**也很感谢我的朋友们提建议**。最后很喜欢某音的话："大大方方地丢脸，兴致勃勃去失败，再昂首挺胸重新开始。"
