# AI 文档内容提取功能实现报告

> 版本: v1.0  
> 实现日期: 2026-04-26  
> 项目: AI-WEB (RuoYi-Vue3 + Spring Boot)

---

## 一、功能概述

用户上传 PDF / Word / TXT 文件后，AI 能自动提取文件内容并基于文档内容回答问题。  
实现方式：前端上传文件至 MinIO → 发送消息时携带文件名 → 后端从 MinIO 下载 → 解析文本 → 拼入 DeepSeek API prompt → AI 理解并回答。

---

## 二、架构图

```
┌─────────────┐    上传文件     ┌──────────────┐    存文件     ┌─────────┐
│  前端 Chat  │ ──────────────→ │  /ai/file/   │ ────────────→ │  MinIO  │
│  (Vue3)     │    multipart     │  upload      │               │ 对象存储 │
└─────────────┘                  └──────────────┘               └─────────┘
       │                                                             │
       │ 发送消息 { message, fileNames }                               │
       ▼                                                             │
┌─────────────┐    解析文件      ┌──────────────┐    下载文件        │
│  ChatCtrl   │ ──────────────→ │ FileContent   │ ←──────────────── │
│  /ai/chat/  │  调用 parseFiles │ Service       │  MinioClient     │
│  ask        │                 └──────┬─────────┘                  │
└─────────────┘                        │                            │
       │                               │ 提取文本内容                │
       │                               ▼                            │
       │                     ┌──────────────────┐                   │
       │                     │ 文本拼入 prompt:  │                   │
       │                     │ 系统提示 + 历史   │                   │
       │                     │ + [文档内容] + 问题│                   │
       │                     └────────┬─────────┘                   │
       │                              │                             │
       ▼                              ▼                             │
┌─────────────────────────────────────────────────────────────┐
│                  DeepSeek API (deepseek-chat)               │
│         "分析以下简历内容，回答用户问题..."                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、实现原理

### 3.1 文件解析层（FileContentService）

**新增文件**: `service/FileContentService.java`

| 文件类型 | 解析引擎 | 实现方式 |
|----------|---------|---------|
| `.pdf` | Apache PDFBox 2.0.31 | `PDDocument.load()` → `PDFTextStripper.getText()` |
| `.docx` | Apache POI 5.2.5 | `XWPFDocument()` → `XWPFWordExtractor.getText()` |
| `.txt` / `.md` | JDK 标准库 | `InputStreamReader` + `BufferedReader` 逐行读取 |

核心代码：

```java
// 从 MinIO 下载 → 解析 → 返回纯文本
public List<FileContent> parseFiles(List<String> fileNames) {
    // 对每个文件：
    // 1. minioClient.getObject() 下载文件流
    // 2. 根据后缀选择解析策略（PDF/Word/Text）
    // 3. 单文件最多 5000 字符，超出截断
    // 4. 返回 fileName + content
}
```

### 3.2 Prompt 构造层（ChatServiceImpl）

修改 `chat()` 方法，接收 `fileNames` 参数：

1. 收到文件名列表 → 调用 `FileContentService.parseFiles()`
2. 解析结果拼入一个带标记的文件内容段
3. 最终发送给 DeepSeek 的 messages 结构：

```
system: "你是实习求职助手..."
user:   "你好"
assistant: "你好！有什么可以帮助你的？"
...
user:   "\n\n【用户上传了以下文件，请分析其内容】
         --- resume.pdf ---
         [PDF 提取的文本内容]
         --- project.docx ---
         [Word 提取的文本内容]
         【用户问题】帮我分析这份简历的优缺点"
```

### 3.3 数据传输层

| 层级 | 改动 |
|------|------|
| 前端 `Chat.vue` | `sendMessage()` 中收集 `uploadedFiles` 的 `fileName` 列表，发送时附加 `fileNames: [...]` |
| 前端 `api/chat.js` | 无改动（透传 data 对象） |
| 后端 `ChatController` | `ask()` 方法从 body 读取 `fileNames` 列表，传给 service |
| 后端 `IChatService` 接口 | `chat()` 签名新增 `List<String> fileNames` 参数 |

### 3.4 Maven 依赖

在 `ruoyi-admin/pom.xml` 新增：

```xml
<!-- PDF 解析 -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.31</version>
</dependency>

<!-- Word 解析 -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

---

## 四、改动的文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `ruoyi-admin/pom.xml` | 修改 | 新增 PDFBox + POI 依赖 |
| `.../service/FileContentService.java` | **新增** | 文件下载+解析服务 |
| `.../controller/ChatController.java` | 修改 | `ask()` 读取 `fileNames` |
| `.../service/IChatService.java` | 修改 | `chat()` 签名新增 `fileNames` |
| `.../service/impl/ChatServiceImpl.java` | 修改 | 注入 `FileContentService`，拼文件内容入 prompt |
| `front/src/views/Chat.vue` | 修改 | `sendMessage()` 携带 `fileNames`，发送后清空列表 |

---

## 五、安全与限制

| 项目 | 说明 |
|------|------|
| 文件大小限制 | 前端+后端双重校验，≤ 10MB |
| 文件类型限制 | 仅允许 pdf/doc/docx/txt/md/xls/xlsx/ppt/pptx |
| 单文件解析长度 | 最多 5000 字符，超出截断（防 prompt 溢出） |
| 文件归属校验 | MinIO 路径带用户ID，删除/获取URL时校验归属 |
| 文件清理 | 消息发送成功后前端自动清空文件列表 |

---

## 六、后续可扩展

- [ ] 支持 `.doc` (旧版 Word) — POI 的 `HWPFDocument`
- [ ] 支持图片 OCR 提取 — 引入 Tesseract
- [ ] 大文件分片上传 + 异步解析
- [ ] 文件内容缓存（相同文件再次提问时不重新解析）
- [ ] 前端显示文件解析状态（解析中...）

---

## 七、实施时间记录

| 时间 | 任务 | 耗时 |
|------|------|------|
| 2026-04-26 | 添加 Maven 依赖（PDFBox + POI） | ~2min |
| 2026-04-26 | 新建 FileContentService（下载+解析） | ~5min |
| 2026-04-26 | 修改 ChatController 接收 fileNames | ~2min |
| 2026-04-26 | 修改 IChatService 接口签名 | ~1min |
| 2026-04-26 | 修改 ChatServiceImpl 集成文件解析 | ~3min |
| 2026-04-26 | 修改前端 Chat.vue 发送时携带 fileNames | ~2min |
| **总计** | | **~15min** |
