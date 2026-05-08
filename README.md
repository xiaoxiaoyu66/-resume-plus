
# Resume+ — AI 中文简历诊断平台

<p align="center">
  <img src="./ruoyi-front/public/resume-plus-logo.svg" alt="Resume+ Logo" width="130" />
</p>
<p align="center">
  <strong>一个学生写的简历工具 · 免费 · 开源 · 专注中文 · 面向重庆</strong>
</p>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/版本-v1.0.0-blue" alt="Version" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Spring_Boot-4.0.3-brightgreen" alt="Spring Boot" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Java-17-orange" alt="Java" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Vue-3.4-4FC08D" alt="Vue" /></a>
  <a href="#"><img src="https://img.shields.io/badge/license-MIT-green" alt="License" /></a>
  <a href="#"><img src="https://img.shields.io/badge/学生开发-何二娃-ff69b4" alt="Student Dev" /></a>
</p>

<p align="center">
  <a href="#项目起源">起源</a> ·
  <a href="#功能矩阵">功能</a> ·
  <a href="#测试概况">测试</a> ·
  <a href="#技术栈">技术栈</a> ·
  <a href="#快速开始">快速开始</a> ·
  <a href="#项目结构">结构</a>
</p>

---

## 项目起源

> 我本是重庆的一名学生。当我在整理简历时发现一个尴尬的事实：
>
> 市面上的简历工具要么只做英文（对中文排版支持极差），要么只提供一个编辑框。
> **投了 30 家公司，没人告诉我简历到底哪里不好。**
>
> 而针对重庆本地互联网岗位的工具，几乎为零。大公司有简历解析工具，但都是卖给 HR 部门的，很贵、不开源。普通人想用？只能填模板（超级简历），或者只能英文（OpenResume）。
>
> 我做的就是**普通人能免费用的、支持任意中文 PDF/Word 上传、还能 AI 诊断和模拟面试的完整工具**。

**Resume+** 是一个重庆学生利用课余时间独立开发的开源项目。基于 RuoYi v3.9.2 深度定制，聚焦中文简历解析与 AI 辅助诊断，将 **简历编辑 → AI 诊断 → PDF 导出 → 岗位匹配 → 面试辅导** 串联成完整求职链路。

我不是大厂工程师，只是一个想帮到更多人的学生。如果这个项目能帮你拿到一个面试机会，那就值了。

---

## 功能矩阵

| 模块 | 功能 | 说明 | 测试覆盖 |
|------|------|------|---------|
| **AI 引擎** | DeepSeek SSE 流式对话 | 首 token < 800ms 返回，四场景提示词切换 | ✅ 单测覆盖 |
| | 滑动上下文窗口（5 轮 + 3000 token） | 平衡记忆深度与 token 消耗 | ✅ Token 裁剪测试 |
| | 双层语义缓存（Caffeine L1 + Redis L2） | 相似度 0.85 命中直接返回，节省 30%-50% API 调用 | ✅ 命中/未命中/过期 |
| | 语义指纹缓存 | 简历内容实质性变化才触发重诊断 | ✅ |
| | 对话历史管理 | 会话 CRUD + 详情回溯 | ✅ |
| **简历管理** | 模块化编辑器（7 大模块） | 基本信息 / 教育 / 经历 / 项目 / 技能 / 意向 / 评价 | — |
| | 三套模板渲染 | 经典 / 简约 / 现代 | — |
| | Undo/Redo（50 层快照） | 全局状态回溯 | ✅ |
| | 自动保存（3 秒防抖） | 编辑即存，无需手动保存 | — |
| | AI 四维诊断 | 评分 + 建议 + 润色 + 关键词 | ✅ |
| | PDF / PNG 导出 | Gotenberg 渲染 PDF，html2canvas 生成 PNG | — |
| | 文件解析 | PDF/Word 上传 → AI 提取结构化 JSON | ✅ |
| **岗位匹配** | PGVector 向量存储 | ivfflat 索引，语义级技能匹配 | ✅ |
| | BGE-M3 嵌入 + 余弦相似度 | 基于简历内容自动计算岗位匹配度 | ✅ |
| | 江城聘侧边栏 | 首页侧边栏展示匹配岗位，按分数排序 | — |
| | 岗位 CRUD | 分页筛选、批量导入 | — |
| **文件系统** | MinIO 对象存储 | 多格式文件上传 | — |
| | 异步向量化流水线 | RabbitMQ → 嵌入 → PGVector 索引 | ✅ |
| **用户系统** | JWT 鉴权 | Spring Security 集成 | — |
| | 多方式登录 | 密码 / 短信（模拟）/ 微信扫码（模拟） | ✅ 前端覆盖 |
| | 个人档案 | 实习/项目信息管理 | — |
| **基础设施** | Docker Compose 编排 | 一键启动所有依赖服务 | — |
| | GitHub Actions CI | 前端 + 后端自动构建测试 | — |

---

## 测试概况

### 前端（178 个用例）

| 文件 | 用例数 | 覆盖内容 |
|------|--------|---------|
| `useChatSession` | 22 | 会话创建、切换、历史管理、清空 |
| `useChatSse` | 26 | SSE 连接、事件流、重连、错误处理 |
| `useChatUpload` | 20 | 文件上传、进度跟踪、取消 |
| `useEditable` | 24 | 编辑状态、撤销/重做、快照管理 |
| `useLoginAuth` | 24 | 登录流程、验证码、短信/微信登录 |
| `auth.js` | 3 | Token 管理、Cookie 操作 |
| `format.js` | 28 | 日期格式化、数字格式化 |
| `quoteLibrary.js` | 18 | 名言库随机选取 |
| `request.js` | 20 | 请求拦截器、响应拦截器、错误处理 |
| `sse-client.js` | 13 | SSE 心跳、重连、事件解析 |
| `store/resume` | — | 简历状态管理 |
| `store/jobs` | — | 岗位状态管理 |
| `store/user` | — | 用户状态管理 |

### 后端（39 个用例）

| 测试类 | 用例数 | 覆盖内容 |
|--------|--------|---------|
| `ChatCacheServiceTest` | 7 | 双层缓存、缓存命中/未命中、过期 |
| `FileVectorizationServiceTest` | 9 | 文件向量化、空内容、格式异常 |
| `JobAnalyzeServiceTest` | 8 | 匹配度分析、异常处理 |
| `SlidingWindowServiceTest` | 7 | 上下文窗口裁剪、Token 计数 |
| `SseChatServiceTest` | 8 | SSE 流式对话、会话管理 |

> 测试持续完善中，欢迎贡献用例。

---

## 技术栈

### 后端

| 技术 | 用途 |
|------|------|
| Java 17 + Spring Boot 4.0.3 | 运行时 + 应用框架 |
| Spring Security 6.x + JWT | 认证授权 |
| MyBatis + Druid + PageHelper | 数据访问 |
| DeepSeek API | 大语言模型 |
| PGVector + BGE-M3 | 向量存储与嵌入 |
| Caffeine + Redis | 双层语义缓存 |
| Gotenberg | Chromium PDF 导出 |
| MinIO | 对象存储 |
| Elasticsearch | 全文检索 |
| RabbitMQ | 异步任务队列 |
| PDFBox + Apache POI | 文件解析 |

### 前端

| 技术 | 用途 |
|------|------|
| Vue 3.4 + Vite 5 | 前端框架 + 构建 |
| Element Plus 2.5 | UI 组件库 |
| Pinia + Vue Router 4 | 状态管理 + 路由 |
| TypeScript | 类型安全（渐进式迁移） |
| Axios | HTTP 客户端 |
| html2canvas | PNG 导出 |
| Vitest | 单元测试 |

### 关键选型

| 决策 | 理由 |
|------|------|
| **RuoYi vs 自研** | 复用成熟的 RBAC、代码生成、定时任务基础设施，专注核心业务 |
| **Gotenberg vs iText** | Chromium 渲染 HTML 转 PDF，精确还原 CSS，免商业许可证 |
| **PGVector vs Pinecone** | PostgreSQL 扩展，无需额外维护向量数据库 |
| **DeepSeek vs OpenAI** | 价格约为 GPT-4 的 1/20，中文理解优秀，适合国产化 |
| **SSE vs WebSocket** | 单向流满足 AI 对话场景，实现简单、自动重连 |
| **BGE-M3 vs OpenAI Embeddings** | 开源嵌入模型，可本地部署，768 维兼顾精度与性能 |

---

## 系统架构

```mermaid
graph TB
    subgraph 客户端层
        A[Vue 3 SPA<br/>端口 3000]
    end

    subgraph 网关层
        C[Nginx<br/>反向代理 / 静态资源]
    end

    subgraph 后端层
        D[ResumeController]
        E[ChatController]
        F[SseChatController]
        G[FileUploadController]
        H[JobController]
        I[ProfileController]
        J[AiLoginController]
    end

    subgraph AI 服务
        K[DeepSeek API]
        L[SiliconFlow API<br/>BGE-M3 嵌入]
    end

    subgraph 数据层
        M[(MySQL 8.0)]
        N[(PostgreSQL+PGVector)]
        O[(Redis 7.0)]
        P[(MinIO)]
        Q[Elasticsearch]
    end

    subgraph 基础设施
        R[RabbitMQ]
        S[Gotenberg]
    end

    A --> C
    C --> D & E & F & G & H & I & J
    D --> M & O & S
    E --> K & O
    F --> K
    G --> P & R
    H --> M & N
    I --> M
    J --> O
    R --> L & N
```

---

## 快速开始

### 环境要求

| 项目 | 推荐配置 |
|------|---------|
| Docker | 24+ |
| Node.js | 20 LTS |
| JDK | 17+ |
| Maven | 3.9+ |

### 1. 启动依赖服务

```bash
docker compose up -d mysql redis postgres minio gotenberg
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填入你的 DeepSeek / MinIO / PGVector 密钥
```

### 3. 初始化数据库

```bash
mysql -h 127.0.0.1 -u root -p ry_ai < sql/resume_table.sql
```

### 4. 启动后端

```bash
cd ruoyi-backend
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

### 5. 启动前端

```bash
cd ruoyi-front
npm install && npm run dev
```

访问 `http://localhost:3000`，使用 `admin / admin123` 登录。

---

## 项目结构

```
resume-plus/
├── ruoyi-backend/             # Spring Boot 多模块
│   ├── ruoyi-admin/           # 启动入口 + AI 控制器
│   │   └── ai/                # 简历、对话、岗位、文件等模块
│   ├── ruoyi-common/          # 公共工具
│   ├── ruoyi-framework/       # 安全 + 配置
│   ├── ruoyi-system/          # 系统业务
│   ├── ruoyi-generator/       # 代码生成
│   └── ruoyi-quartz/          # 定时任务
│
├── ruoyi-front/               # Vue 3 + Vite + TypeScript
│   └── src/
│       ├── views/             # 简历编辑、江城聘、聊天等页面
│       ├── store/             # Pinia 状态管理
│       ├── composables/       # 组合式逻辑（含完整测试）
│       ├── api/               # API 模块
│       └── components/        # 可复用组件
│
├── sql/                       # 数据库建表脚本
├── docs/                      # AI 提示词、技术文档
├── nginx/                     # Nginx 配置
├── docker-compose.yml         # Docker 编排
└── .env.example               # 环境变量模板
```

---

## CI / CD

| 工作流 | 触发条件 | 步骤 |
|--------|---------|------|
| **Frontend CI** | Push / PR 到 master | `npm install` → `vitest run` (178 tests) → `vite build` |
| **Backend CI** | Push / PR 到 master | `mvn test` (39 tests, Mockito 模拟, 不依赖外部服务) |

---

## 开源 AI 提示词

项目的四组 system prompt（综合助手、简历分析、面试辅导、职业规划）完全公开，见 [docs/AI-PROMPTS.md](./docs/AI-PROMPTS.md)。

Prompt 只是起点，真正的壁垒在于工程实现：上下文窗口管理、语义缓存命中、文件向量化、滑动窗口记忆。

---

## 致谢

- [RuoYi](https://ruoyi.vip/) — 优秀的企业级 Java 快速开发框架
- [Gotenberg](https://gotenberg.dev/) — Chromium 无头 PDF 生成服务
- [DeepSeek](https://deepseek.com/) — 高性价比中文大语言模型（梁文峰义父！！！）
- [PGVector](https://github.com/pgvector/pgvector) — PostgreSQL 向量检索扩展
- [Element Plus](https://element-plus.org/) — Vue 3 组件库
- [PoleBrief](https://www.polebrief.com/) — 简历编辑器设计参考
- 所有贡献者、测试者、和每一个 Star

---

## 许可证

MIT License，基于 [RuoYi v3.9.2](https://ruoyi.vip/) 扩展开发。

---

<p align="center">—— 何二娃，重庆学生 · 独立开发 · 用爱发电 ——</p>
