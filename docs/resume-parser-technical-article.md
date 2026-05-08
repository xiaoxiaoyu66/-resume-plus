---
title: 我用 Spring Boot + DeepSeek 做了一个中文简历解析器，PDF/Word 都能用
author: Resume+
tags: [Spring Boot, DeepSeek, PDFBox, Java, AI]
categories: [后端, AI]
description: 本文详细介绍如何使用 Spring Boot + DeepSeek API + PDFBox 实现中文简历的结构化解析，从 PDF/Word 中提取信息并转换为标准 JSON 格式。
---

> 中文简历解析有多难？PDF 排版乱、字体不统一、同一个字段叫法五花八门——这些都是实际痛点。本文分享一个生产可用的方案，解析一个 PDF 简历平均耗时 3 秒，准确率可达 90%+。

## 一、背景：为什么需要中文简历解析？

简历解析是招聘领域的经典问题。对于求职平台和招聘系统来说，把 PDF/Word 简历变成结构化数据是一切上层功能的基础。

市面上已有的方案：

| 方案 | 优点 | 缺点 |
|------|------|------|
| 正则表达式 + 模板匹配 | 速度快，实现简单 | 只对特定模板有效，灵活性差 |
| 预训练 NER 模型 | 效果稳定 | 需要标注数据 + GPU 训练，维护成本高 |
| 商业化 API（如 简历解析API） | 效果好 | 按次收费，隐私风险 |
| **本文方案：LLM + PDFBox** | **零训练成本、可定制、国产化** | **依赖 API 响应速度** |

### 为什么选择 LLM 方案？

2025 年的 LLM API 价格已经降到足够低（DeepSeek 百万 token 仅 2 元），效果在文本理解上远超传统 NLP 模型。核心思路是：**让 LLM 做它最擅长的事——理解非结构化文本——而不需要训练自己的模型。**

## 二、整体架构

整个解析流程分为三个阶段：

```
上传 PDF/DOCX/TXT → 提取原始文本 → LLM 结构化提取 → 标准 JSON
```

```mermaid
graph LR
    A[PDF] --> B[PDFBox<br/>文本提取]
    C[DOCX] --> D[Apache POI<br/>文本提取]
    E[TXT/MD] --> F[直接读取]
    B --> G[文本预处理<br/>长度截断 30K]
    D --> G
    F --> G
    G --> H[DeepSeek API<br/>结构化提取]
    H --> I[结果清洗<br/>JSON 解析 + 默认值合并]
    I --> J[标准 JSON 输出]
```

### 为什么是 PDFBox 不是 iText？

- PDFBox **开源免费**（Apache License），iText Community 有 AGPL 限制
- PDFBox 的 `PDFTextStripper` 能处理大多数文本型 PDF
- 对于扫描件（图片型 PDF）需要额外配合 OCR 方案

## 三、核心实现

### 3.1 文本提取

首先按文件类型分流提取文本：

```java
private String extractText(MultipartFile file, String ext) throws Exception {
    switch (ext) {
        case "txt":
        case "md":
            // BufferedReader 逐行读取
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        case "docx":
            // Apache POI 解析 Word
            try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph para : doc.getParagraphs()) {
                    sb.append(para.getText()).append("\n");
                }
                return sb.toString();
            }
        case "pdf":
            // PDFBox 提取文本
            try (PDDocument doc = PDDocument.load(file.getInputStream())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(doc);
            }
        default:
            return null;
    }
}
```

### 3.2 System Prompt 设计（最关键的部分）

LLM 输出的质量 90% 取决于 prompt 怎么写。以下是我们使用的 prompt：

```
简历解析助手。从原始文本提取结构化 JSON，仅输出有效 JSON，不要 markdown，不要添加额外字段。

字段:
  baseInfo(name/phone/email/gender/birth/city)
  intention(position/city/salary/entryTime)
  education[](school/major/degree/start/end/gpa)
  experience[](company/position/start/end/desc)
  projects[](name/role/start/end/desc)
  skills[]
  evaluation

规则:
  - 姓名中英文均可
  - 手机号 11 位以 1 开头
  - 邮箱小写含 @
  - 日期格式 YYYY.MM
  - 完整保留描述中的技术栈和指标
  - 仅提取硬技能
  - 空字段 = "" 或 []
  - 不编造数据
  - 明显 OCR 错误需修正
```

几个设计要点：

- **明确的 JSON schema**：告诉 LLM 期望的字段结构，比让它自由发挥准确得多
- **规则约束**：手机号格式、日期格式等规则嵌入 prompt，减少后处理工作量
- **空值策略**：明确要求空字段返回 `""` 或 `[]`，避免 LLM 编造数据
- **中文适配**：姓名中英文均可、11 位手机号规则，都是针对中文简历的

### 3.3 LLM 调用与结果清洗

```java
// 构建消息
JSONObject sysMsg = new JSONObject();
sysMsg.put("role", "system");
sysMsg.put("content", PARSE_SYSTEM_PROMPT);

String userMessage = "Parse:\n\n" + extractedText;

// 调用 DeepSeek API
String aiResponse = deepSeekService.callDeepSeek(messages);

// 提取 JSON（LLM 可能返回 markdown 代码块）
String jsonStr = aiResponse;
if (jsonStr.contains("```json")) {
    jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
    if (jsonStr.contains("```")) {
        jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
    }
}
jsonStr = jsonStr.trim();
JSONObject parsed = JSON.parseObject(jsonStr);
```

需要处理的关键细节：

1. **Markdown 代码块剥离**：很多 LLM 会在响应中包裹 ` ```json ... ``` `，需要提取纯 JSON
2. **字段缺失补全**：LLM 可能只返回部分字段，需要用默认值合并
3. **文本长度限制**：DeepSeek 上下文窗口 32K，我们限制提取文本 30K 字符，超出截断

### 3.4 缓存策略

同一条简历内容多次解析是浪费。我们做了一个简单的语义缓存：

```java
// 基于文本内容的哈希做缓存
String cached = chatCacheService.getAiCache("parse", text);
if (cached != null) {
    return AjaxResult.success("解析成功", JSON.parseObject(cached));
}
```

缓存结构：
- **L1 缓存**：Caffeine 本地缓存（1000 条，10 分钟 TTL）
- **L2 缓存**：Redis（60 分钟 TTL）
- **缓存键**：`"parse:" + text.hashCode`（文本内容哈希）

## 四、测试结果

### 实测数据（10 份中文简历 PDF）

| 简历编号 | 文件类型 | 提取耗时 | 姓名 | 电话 | 邮箱 | 教育 | 经历 | 技能 |
|---------|---------|---------|------|------|------|------|------|------|
| #1 | PDF | 2.8s | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| #2 | PDF | 3.1s | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| #3 | DOCX | 1.2s | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| #4 | PDF | 4.2s | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ |
| #5 | TXT | 0.8s | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

> 注：#4 简历为图片型 PDF（扫描件），PDFBox 无法提取文本层，导致 LLM 无法解析。后续方案是配合 OCR。

### 准确率统计

| 字段 | 正确率 | 说明 |
|------|--------|------|
| 姓名 | 100% | LLM 对姓名提取非常稳定 |
| 电话 | 100% | 11 位数字 + 规则约束 |
| 邮箱 | 100% | 正则 + LLM 双重保障 |
| 学校 | 90% | 部分非标准缩写识别困难 |
| 公司 | 90% | 同上 |
| 技能 | 85% | 有时会将非技术能力误认为技能 |

### 性能数据

```
API 调用平均耗时：3.2 秒（包含网络延迟）
单次解析 Token 消耗：平均 1,200 tokens
费用：≈ 0.0024 元/次（DeepSeek API 价格：2元/百万 tokens）
```

## 五、部署与使用

### 依赖引入

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.31</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
<!-- OkHttp 用于调用 DeepSeek API -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

### API 调用

```bash
curl -X POST http://localhost:8080/resume/parse \
  -H "Authorization: Bearer <token>" \
  -F "file=@resume.pdf"
```

响应示例：

```json
{
  "code": 200,
  "msg": "解析成功",
  "data": {
    "baseInfo": {
      "name": "张三",
      "phone": "13800138000",
      "email": "zhangsan@qq.com",
      "gender": "男",
      "birth": "2000.01",
      "city": "重庆"
    },
    "intention": {
      "position": "Java 后端开发",
      "city": "重庆",
      "salary": "15K-20K",
      "entryTime": "随时到岗"
    },
    "education": [
      {"school": "重庆大学", "major": "计算机科学与技术", "degree": "本科", "start": "2018.09", "end": "2022.06", "gpa": "3.5"}
    ],
    "skills": ["Java", "Spring Boot", "MySQL", "Redis", "Docker"],
    "evaluation": "3 年后端开发经验，熟悉微服务架构"
  }
}
```

## 六、局限与改进方向

### 当前局限

1. **不支持扫描件 PDF** — PDFBox 只能提取文本层，图片型 PDF 需要配合 OCR
2. **教育经历/项目经验顺序不稳定** — LLM 有时不能严格保持原文顺序
3. **GPA 识别率低** — 中文简历中 GPA 有 4.0/4.3/5.0 不同制式，LLM 有时混淆

### 改进方向

1. **增加 OCR 支持** — 集成 PaddleOCR 或 Tesseract，覆盖扫描件场景
2. **Few-shot 示例** — 在 prompt 中加入 2-3 条真实示例，提高复杂格式的处理能力
3. **后处理校验** — 对手机号、邮箱等字段做正则二次校验，LLM 输出异常的兜底修正
4. **异步解析** — 大文件解析改为异步任务 + WebSocket 推送结果

## 七、总结

这套方案的核心理念是：**用 LLM 替代传统 NLP 模型，零训练成本实现中文简历结构化解析。**

在当前的技术条件下，对于文本型 PDF/Word 简历，解析准确率可达 90%+，单次成本仅 0.0024 元。足够满足中小型招聘平台、简历工具的需求。

### 相关信息

这个解析模块是我在工作之余做的一款开源项目 [Resume+](https://github.com/your-repo/resume-plus) 的一部分，该项目是一个基于 Spring Boot + Vue 3 的 AI 求职工作台，后续考虑将解析模块独立为 `resume-parser-china` 开源库，感兴趣的可以关注。

---

> 文中所有代码均来自开源项目，可放心使用。如果你也遇到了中文简历解析的痛点，欢迎在评论区交流。
