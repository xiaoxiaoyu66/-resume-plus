package com.ruoyi.web.controller.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.web.controller.ai.domain.Resume;
import com.ruoyi.web.controller.ai.service.DeepSeekService;
import com.ruoyi.web.controller.ai.service.ChatCacheService;
import com.ruoyi.web.controller.ai.service.CoordinateTextStripper;
import com.ruoyi.web.controller.ai.service.IResumeService;
import com.ruoyi.web.controller.ai.service.ResumeJobMatchService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简历Controller
 */
@RestController
@RequestMapping("/resume")
public class ResumeController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    @Autowired
    private IResumeService resumeService;

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private ChatCacheService chatCacheService;

    @Autowired
    private ResumeJobMatchService resumeJobMatchService;

    @Value("${gotenberg.url:http://gotenberg:3000}")
    private String gotenbergUrl;

    /** 系统提示词：简历解析 */
    private static final String PARSE_SYSTEM_PROMPT = "简历解析助手。从原始文本提取结构化 JSON，仅输出有效 JSON，不要 markdown，不要添加额外字段。\n"
            + "字段: baseInfo(name/phone/email/gender/birth/city), intention(position/city/salary/entryTime), education[](school/major/degree/start/end/gpa), experience[](company/position/start/end/desc), projects[](name/role/start/end/desc), skills[], evaluation\n"
            + "规则: 姓名中英文均可；手机号 11 位以 1 开头；邮箱小写含 @；日期格式 YYYY.MM；完整保留描述中的技术栈和指标；仅提取硬技能；空字段 = \"\" 或 []; 不编造数据；明显 OCR 错误需修正";

    /**
     * 获取当前用户的简历列表
     */
    @GetMapping("/list")
    public AjaxResult list() {
        List<Resume> list = resumeService.selectCurrentUserResumeList();
        return AjaxResult.success(list);
    }

    /**
     * 根据ID获取简历详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        Resume resume = resumeService.selectResumeById(id);
        if (resume == null) {
            return AjaxResult.error("简历不存在或无权访问");
        }
        return AjaxResult.success(resume);
    }

    /**
     * 新增简历
     */
    @PostMapping
    public AjaxResult add(@RequestBody Resume resume) {
        int result = resumeService.insertResume(resume);
        return result > 0 ? AjaxResult.success("创建成功", resume) : AjaxResult.error("创建失败");
    }

    /**
     * 更新简历
     */
    @PutMapping
    public AjaxResult edit(@RequestBody Resume resume) {
        int result = resumeService.updateResume(resume);
        return result > 0 ? AjaxResult.success("更新成功") : AjaxResult.error("更新失败");
    }

    /**
     * 删除简历
     */
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        int result = resumeService.deleteResume(id);
        return result > 0 ? AjaxResult.success("删除成功") : AjaxResult.error("删除失败");
    }

    /**
     * 设置默认简历
     */
    @PutMapping("/default/{id}")
    public AjaxResult setDefault(@PathVariable Long id) {
        int result = resumeService.setDefaultResume(id);
        return result > 0 ? AjaxResult.success("设置成功") : AjaxResult.error("设置失败");
    }

    // ── AI 增强功能 ──────────────────────────────────────────────

    /** 系统提示词：简历描述优化 */
    private static final String POLISH_SYSTEM_PROMPT = "简历文本润色。让描述更专业、更成果导向、更有冲击力。\n"
            + "规则: 1) 用强动词(主导/重构/搭建/优化) > 弱表达(负责/参与) 2) 补充量化指标(用户数/QPS/团队规模/延迟) 3) 明确写出技术栈名(\"Spring Boot\" 不是 \"框架\") 4) 聚焦个人贡献 5) 不编造 6) 每行 = 做了什么 + 怎么做 + 什么结果\n"
            + "同时推荐 3-5 个相关技术关键词。严格按以下结构输出JSON，不要添加额外字段：\n"
            + "{\"polished\":\"...\",\"keywords\":[\"...\"]}";

    /** 系统提示词：简历评分（已移除 — 2026-05-04） */

    /** 系统提示词：JD适配 */
    private static final String JD_SYSTEM_PROMPT = "简历-JD 匹配器。根据岗位描述优化简历以更好匹配。规则(严格): 1) 不编造 2) 突出与 JD 相关的经历，精简(不删除)无关内容 3) 术语与 JD 对齐 4) JD 要求的技能优先展示，其他放后面 5) 按 JD 重写自我评价 6) 隐式技能显式化(如项目用容器就写 Docker)\n"
            + "adapted 包含全部字段: baseInfo(name/phone/email/gender/birth/city), intention(position/city/salary/entryTime), education[](school/major/degree/start/end/gpa), experience[](company/position/start/end/desc), projects[](name/role/start/end/desc), skills[], evaluation\n"
            + "仅输出有变更的字段，未变更字段可省略。严格按上述结构输出JSON，不要添加额外字段，不要 markdown。\n"
            + "{\"adapted\":{...},\"summary\":\"简要适配说明\"}";

    /**
     * AI 简历增强功能
     * <p>
     * action=polish — 优化描述文本，返回润色结果+关键词
     * action=jd      — 根据岗位描述适配简历，返回适配后简历
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/ai")
    public AjaxResult aiAction(@RequestBody Map<String, Object> params) {
        String action = (String) params.get("action");
        if (action == null) {
            return AjaxResult.error("缺少 action 参数");
        }

        try {
            switch (action) {
                case "polish": {
                    String text = (String) params.get("text");
                    if (text == null || text.trim().isEmpty()) {
                        return AjaxResult.error("缺少待优化文本");
                    }

                    // 检查缓存
                    String cached = chatCacheService.getAiCache("polish", text);
                    if (cached != null) {
                        log.debug("polish 缓存命中");
                        return AjaxResult.success(JSON.parseObject(cached));
                    }

                    JSONArray messages = buildMessages(POLISH_SYSTEM_PROMPT,
                            "Polish:\n\n" + text);
                    String response = deepSeekService.callDeepSeek(messages);
                    String json = extractJson(response);

                    JSONObject result;
                    try {
                        result = JSON.parseObject(json);
                    } catch (Exception e) {
                        // AI 返回格式异常时降级返回
                        result = new JSONObject();
                        result.put("polished", text);
                        result.put("keywords", new JSONArray());
                    }
                    // 确保关键词字段存在
                    if (!result.containsKey("keywords")) {
                        result.put("keywords", new JSONArray());
                    }

                    // 写入缓存
                    chatCacheService.putAiCache("polish", text, result.toJSONString());
                    return AjaxResult.success(result);
                }

                case "jd": {
                    Object resumeContent = params.get("resumeContent");
                    String jd = (String) params.get("jd");
                    if (resumeContent == null || jd == null || jd.trim().isEmpty()) {
                        return AjaxResult.error("缺少简历内容或岗位描述");
                    }

                    // 缓存键：完整 resumeContent + jd
                    String cacheInput = JSON.toJSONString(resumeContent) + "|||" + jd;
                    String cached = chatCacheService.getAiCache("jd", cacheInput);
                    if (cached != null) {
                        log.debug("jd 缓存命中");
                        JSONObject cachedResult = JSON.parseObject(cached);
                        if (!cachedResult.containsKey("adapted")) {
                            cachedResult.put("adapted", resumeContent);
                        }
                        return AjaxResult.success(cachedResult);
                    }

                    // 只提取 relevant 字段：skills + projects/experience desc（减少 60% token）
                    JSONObject full = JSON.parseObject(JSON.toJSONString(resumeContent));
                    JSONObject leanInput = new JSONObject();
                    leanInput.put("skills", full.getJSONArray("skills"));
                    leanInput.put("projects", extractDescArray(full.getJSONArray("projects"), "desc"));
                    leanInput.put("experience", extractDescArray(full.getJSONArray("experience"), "desc"));

                    JSONArray messages = buildMessages(JD_SYSTEM_PROMPT,
                            "JD:\n" + jd + "\n\nResume:\n" + leanInput.toJSONString());
                    String response = deepSeekService.callDeepSeek(messages);
                    String json = extractJson(response);

                    JSONObject result;
                    try {
                        result = JSON.parseObject(json);
                    } catch (Exception e) {
                        result = new JSONObject();
                        result.put("adapted", resumeContent);
                        result.put("summary", "适配处理失败，已返回原始内容");
                    }
                    if (!result.containsKey("adapted")) {
                        result.put("adapted", resumeContent);
                    }

                    chatCacheService.putAiCache("jd", cacheInput, result.toJSONString());
                    return AjaxResult.success(result);
                }

                case "quote": {
                    String text = (String) params.get("text");
                    if (text == null || text.trim().isEmpty()) {
                        return AjaxResult.error("缺少 prompt");
                    }
                    // Quote gen: compact humor, ~20 chars, no quotes/prefix/explanation
                    JSONArray messages = buildMessages(
                            "Generate a funny self-deprecating or encouraging 1-liner(~20字) about resume quality. Chinese. Return ONLY the line, no quotes/prefix/punctuation.",
                            text);
                    String response = deepSeekService.callDeepSeek(messages);
                    String clean = response.replaceAll("[\"'\n\r]", "").trim();
                    JSONObject result = new JSONObject();
                    result.put("quote", clean);
                    return AjaxResult.success(result);
                }

                case "optimize": {
                    Object resumeContent = params.get("resumeContent");
                    Object suggestions = params.get("suggestions");
                    if (resumeContent == null || suggestions == null) {
                        return AjaxResult.error("缺少简历内容或改进建议");
                    }

                    String cacheInput = JSON.toJSONString(resumeContent) + "::" + JSON.toJSONString(suggestions);
                    String cached = chatCacheService.getAiCache("optimize", cacheInput);
                    if (cached != null) {
                        log.debug("optimize 缓存命中");
                        JSONObject cachedResult = JSON.parseObject(cached);
                        if (!cachedResult.containsKey("_original")) {
                            cachedResult.put("_original", JSON.parseObject(JSON.toJSONString(resumeContent)));
                        }
                        return AjaxResult.success(cachedResult);
                    }

                    // 只提取需要优化的文本部分（减少 token）
                    JSONObject full = JSON.parseObject(JSON.toJSONString(resumeContent));
                    JSONObject optInput = new JSONObject();
                    optInput.put("evaluation", full.getString("evaluation"));
                    optInput.put("skills", full.getJSONArray("skills"));
                    optInput.put("experience", extractDescArray(full.getJSONArray("experience"), "desc"));
                    optInput.put("projects", extractDescArray(full.getJSONArray("projects"), "desc"));

                    String prompt = "简历优化助手。根据改进建议专业地优化简历内容。\n"
                            + "规则: 1) 经历/项目用 STAR 格式(情境→任务→行动→指标) 2) 强动词(主导/搭建/优化) > 弱表达(参与) 3) 补充量化指标(百分比/用户数/QPS/团队规模) 4) 明确技术栈名称 5) 扩写不缩写 6) 不编造 7) 技能按相关度排序 8) 自我评价 3-4 点每点有依据\n"
                            + "仅输出有变更的字段。未变更的项保持数组长度和结构不变。严格按以下结构输出JSON，不要添加额外字段：\n"
                            + "{\"evaluation\":\"...\",\"skills\":[],\"experience\":[],\"projects\":[]}";
                    JSONArray msgs = buildMessages(prompt,
                            "改进建议:\n" + JSON.toJSONString(suggestions)
                            + "\n\n简历内容:\n" + optInput.toJSONString());
                    String response = deepSeekService.callDeepSeek(msgs);
                    log.info("AI优化原始响应: {}", response);
                    String json = extractJson(response);

                    JSONObject result;
                    try {
                        result = JSON.parseObject(json);
                    } catch (Exception e) {
                        log.warn("AI优化响应解析失败, 使用原始内容: {}", e.getMessage());
                        result = new JSONObject();
                        result.put("changes", new JSONArray());
                        // 即使解析失败，也返回可用的标记
                        result.put("_fallback", true);
                    }
                    if (!result.containsKey("changes")) {
                        result.put("changes", new JSONArray());
                    }
                    // 附上原始 resumeContent 以便前端合并
                    result.put("_original", full);

                    chatCacheService.putAiCache("optimize", cacheInput, result.toJSONString());
                    return AjaxResult.success(result);
                }

                case "star": {
                    String projectName = (String) params.getOrDefault("projectName", "");
                    String projectDesc = (String) params.getOrDefault("projectDesc", "");
                    String projectRole = (String) params.getOrDefault("projectRole", "");
                    if (projectDesc == null || projectDesc.trim().isEmpty()) {
                        return AjaxResult.error("缺少项目描述");
                    }

                    String cacheInput = projectName + "||" + (projectRole != null ? projectRole : "") + "||" + projectDesc;
                    String cached = chatCacheService.getAiCache("star", cacheInput);
                    if (cached != null) {
                        log.debug("star 缓存命中");
                        return AjaxResult.success(JSON.parseObject(cached));
                    }

                    String starPrompt = "Rewrite project experience in STAR format(Situation/Task/Action/Result). Rules: don't fabricate; use strong verbs; add metrics; output as 1 coherent paragraph(≤200字).\n"
                            + "Output JSON: {\"starText\":\"...\"}";

                    String userMsg = "Project:" + projectName + "\n"
                            + "Role:" + (projectRole != null ? projectRole : "") + "\n"
                            + "Desc:" + (projectDesc != null ? projectDesc : "");

                    JSONArray messages = buildMessages(starPrompt, userMsg);
                    String response = deepSeekService.callDeepSeek(messages);
                    String json = extractJson(response);

                    JSONObject result;
                    try {
                        result = JSON.parseObject(json);
                    } catch (Exception e) {
                        result = new JSONObject();
                        result.put("starText", projectDesc);
                    }
                    if (!result.containsKey("starText")) {
                        result.put("starText", projectDesc);
                    }
                    chatCacheService.putAiCache("star", cacheInput, result.toJSONString());
                    return AjaxResult.success(result);
                }

                case "generate": {
                    String field = (String) params.get("field");
                    Object resumeContent = params.get("resumeContent");
                    if (field == null || resumeContent == null) {
                        return AjaxResult.error("缺少 field 或 resumeContent 参数");
                    }

                    // 缓存键含 field + resumeContent
                    String cacheInput = field + "::" + JSON.toJSONString(resumeContent);
                    String cached = chatCacheService.getAiCache("generate", cacheInput);
                    if (cached != null) {
                        log.debug("generate 缓存命中: field={}", field);
                        return AjaxResult.success(JSON.parseObject(cached));
                    }

                    // 只提取相关字段，减少 token
                    JSONObject full = JSON.parseObject(JSON.toJSONString(resumeContent));
                    JSONObject leanInput = new JSONObject();
                    leanInput.put("skills", full.getJSONArray("skills"));
                    leanInput.put("projects", extractDescArray(full.getJSONArray("projects"), "desc"));
                    leanInput.put("experience", extractDescArray(full.getJSONArray("experience"), "desc"));

                    JSONObject result = new JSONObject();

                    switch (field) {
                        case "evaluation": {
                            JSONArray msgs = buildMessages(
                                    "Generate a confident professional self-evaluation(3-5 sentences) based on resume. Highlight core skills, experience, traits. Don't fabricate.\n"
                                    + "Output JSON: {\"text\":\"...\"}",
                                    leanInput.toJSONString());
                            String response = deepSeekService.callDeepSeek(msgs);
                            String json = extractJson(response);
                            try {
                                JSONObject parsed = JSON.parseObject(json);
                                if (parsed.containsKey("text")) {
                                    result.put("text", parsed.getString("text"));
                                } else {
                                    result.put("text", json);
                                }
                            } catch (Exception e) {
                                result.put("text", json);
                            }
                            break;
                        }
                        case "skills": {
                            JSONArray msgs = buildMessages(
                                    "Extract 8-12 tech skills from resume(work/project experience). Sort by relevance. Hard skills only, no soft skills. Don't fabricate.\n"
                                    + "Output JSON: {\"skills\":[\"Java\",\"...\"]}",
                                    leanInput.toJSONString());
                            String response = deepSeekService.callDeepSeek(msgs);
                            String json = extractJson(response);
                            try {
                                JSONObject parsed = JSON.parseObject(json);
                                if (parsed.containsKey("skills")) {
                                    result.put("skills", parsed.getJSONArray("skills"));
                                } else {
                                    result.put("skills", new JSONArray());
                                }
                            } catch (Exception e) {
                                result.put("skills", new JSONArray());
                            }
                            break;
                        }
                        default:
                            return AjaxResult.error("未知的 generate field: " + field);
                    }

                    chatCacheService.putAiCache("generate", cacheInput, result.toJSONString());
                    return AjaxResult.success(result);
                }

                case "clinic": {
                    Object resumeContent = params.get("resumeContent");
                    if (resumeContent == null) {
                        return AjaxResult.error("缺少简历内容");
                    }
                    String targetPosition = (String) params.get("targetPosition");

                    JSONObject full = JSON.parseObject(JSON.toJSONString(resumeContent));

                    // 指纹加入目标岗位，岗位不同缓存不同
                    String clinicFingerprint = generateClinicFingerprint(full);
                    if (targetPosition != null && !targetPosition.trim().isEmpty()) {
                        clinicFingerprint += "|pos=" + targetPosition.trim().toLowerCase();
                    }
                    String cached = chatCacheService.getAiCache("clinic", clinicFingerprint);
                    if (cached != null) {
                        log.debug("clinic 缓存命中 (fingerprint)");
                        return AjaxResult.success(JSON.parseObject(cached));
                    }

                    JSONObject leanInput = buildClinicLeanInput(full);

                    // 岗位定向诊断 → 修改 prompt
                    String positionDirective = "";
                    if (targetPosition != null && !targetPosition.trim().isEmpty()) {
                        positionDirective = "针对目标岗位【" + targetPosition.trim() + "】定向诊断：\n"
                                + "1) missing: 结合该岗位常见要求，指出与岗位不匹配的缺失项\n"
                                + "2) generated.evaluation: 面向该岗位重写，突出岗位所需能力\n"
                                + "3) generated.skills: 按该岗位相关度排序和提取\n"
                                + "4) projectTips: 让项目/经历描述更贴合该岗位\n";
                    }

                    String prompt = "资深简历评审师。全面分析简历：\n"
                            + positionDirective
                            + "1) missing: fields 中标记为 false 的字段就是缺失项，直接列出(如无自我评价、项目缺量化指标、技能弱、无联系方式)\n"
                            + "2) generated: 撰写专业自我评价(3-5句) + 提取8-12个核心技术技能\n"
                            + "3) projectTips: 对每个项目/经历写出可直接替换的优化段落，务必补充STAR结构、量化数据、强动词，不要给建议——直接给出可用替换文本。module字段区分\"experience\"（工作经历）或\"projects\"（项目经验）\n"
                            + "严格按以下结构输出JSON，不要添加额外字段：\n"
                            + "{\"missing\":[\"缺失项\"],\"generated\":{\"evaluation\":\"...\",\"skills\":[]},\"projectTips\":[{\"index\":0,\"module\":\"experience\",\"tip\":\"优化后的段落...\"}]}";

                    JSONArray msgs = buildMessages(prompt, leanInput.toJSONString());
                    String response = deepSeekService.callDeepSeek(msgs);
                    String json = extractJson(response);

                    JSONObject result;
                    try {
                        result = JSON.parseObject(json);
                    } catch (Exception e) {
                        result = new JSONObject();
                    }
                    if (!result.containsKey("missing")) result.put("missing", new JSONArray());
                    if (!result.containsKey("generated")) result.put("generated", new JSONObject());
                    if (!result.containsKey("projectTips")) result.put("projectTips", new JSONArray());

                    // 标记深化目标岗位（前端用于展示）
                    if (targetPosition != null && !targetPosition.trim().isEmpty()) {
                        result.put("_deepenedFor", targetPosition.trim());
                    }

                    chatCacheService.putAiCache("clinic", clinicFingerprint, result.toJSONString());
                    return AjaxResult.success(result);
                }

                case "clinic-followup": {
                    Object resumeContent = params.get("resumeContent");
                    Object diagnosisResult = params.get("diagnosisResult");
                    String followUpMessage = (String) params.get("followUpMessage");
                    if (resumeContent == null || diagnosisResult == null || followUpMessage == null || followUpMessage.trim().isEmpty()) {
                        return AjaxResult.error("缺少简历内容、诊断结果或追问信息");
                    }

                    // 读取目标岗位（从 currentDiagnosis 的 _deepenedFor 字段获取）
                    String targetPosition = null;
                    try {
                        JSONObject diag = JSON.parseObject(JSON.toJSONString(diagnosisResult));
                        if (diag.containsKey("_deepenedFor")) {
                            targetPosition = diag.getString("_deepenedFor");
                        }
                    } catch (Exception ignored) {}

                    JSONObject full = JSON.parseObject(JSON.toJSONString(resumeContent));
                    JSONObject lastDiagnosis = JSON.parseObject(JSON.toJSONString(diagnosisResult));

                    // 构建简历摘要
                    JSONObject leanInput = buildClinicLeanInput(full);

                    String targetDirective = "";
                    if (targetPosition != null && !targetPosition.trim().isEmpty()) {
                        targetDirective = "当前诊断基于目标岗位【" + targetPosition.trim() + "】，追问也围绕该岗位要求进行调整。\n";
                    }

                    String prompt = "你是资深简历评审师。用户对你之前的诊断结果不满意，发来了追问。\n"
                            + targetDirective
                            + "请结合追问内容，只调整受影响的维度，其他维度保持原样。\n"
                            + "规则：\n"
                            + "1) missing: 如果追问涉及缺失项判断，重新评估并返回完整 missing 列表；否则返回 null\n"
                            + "2) generated.evaluation: 如果追问涉及自我评价，重写后返回；否则返回 null\n"
                            + "3) generated.skills: 如果追问涉及技能判断，重新提取后返回；否则返回 null\n"
                            + "4) projectTips: 如果追问涉及某个项目/经历，只更新对应的那条（index 保持一致）；否则返回 null\n"
                            + "只输出有变化的部分，没变化的部分返回 null。\n"
                            + "严格按以下结构输出JSON，不要添加额外字段：\n"
                            + "{\"missing\":[\"缺失项\"]或null,\"generated\":{\"evaluation\":\"...\"或null,\"skills\":[]或null},\"projectTips\":[{\"index\":0,\"module\":\"experience\",\"tip\":\"...\"}]或null}";

                    JSONArray msgs = buildMessages(prompt,
                            "## 上一次诊断结果\n" + lastDiagnosis.toJSONString()
                            + "\n\n## 简历内容\n" + leanInput.toJSONString()
                            + "\n\n## 用户追问\n" + followUpMessage);

                    String response = deepSeekService.callDeepSeek(msgs);
                    String json = extractJson(response);

                    JSONObject patch;
                    try {
                        patch = JSON.parseObject(json);
                        // 确保所有字段都存在（即使是 null）
                        if (!patch.containsKey("missing")) patch.put("missing", null);
                        if (!patch.containsKey("generated")) patch.put("generated", null);
                        if (!patch.containsKey("projectTips")) patch.put("projectTips", null);
                    } catch (Exception e) {
                        patch = new JSONObject();
                        patch.put("missing", null);
                        patch.put("generated", null);
                        patch.put("projectTips", null);
                    }
                    return AjaxResult.success(patch);
                }

                case "matchJobs": {
                    Object resumeContent = params.get("resumeContent");
                    if (resumeContent == null) {
                        return AjaxResult.error("缺少简历内容");
                    }
                    JSONObject full = JSON.parseObject(JSON.toJSONString(resumeContent));

                    @SuppressWarnings("unchecked")
                    List<String> diagnosisMissing = (List<String>) params.get("diagnosisMissing");

                    List<Map<String, Object>> matches = resumeJobMatchService.matchJobs(full, diagnosisMissing);
                    return AjaxResult.success(matches);
                }

                default:
                    return AjaxResult.error("未知的 action: " + action);
            }
        } catch (Exception e) {
            log.error("AI增强功能失败: action={}", action, e);
            return AjaxResult.error("AI处理失败: " + e.getMessage());
        }
    }

    /**
     * 导出 PDF - 接收前端采集的 HTML，经 Gotenberg(Chromium) 渲染后返回 PDF
     */
    @PostMapping("/export/pdf")
    public void exportPdf(@RequestBody Map<String, String> params, HttpServletResponse response) throws Exception {
        String html = params.get("html");
        if (html == null || html.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("缺少 html 参数");
            return;
        }

        log.info("导出PDF, HTML长度: {}", html.length());

        // 构建 multipart body，调用 Gotenberg
        String boundary = "----" + System.currentTimeMillis();
        byte[] body = buildGotenbergBody(html, boundary);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(gotenbergUrl + "/forms/chromium/convert/html"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<byte[]> gotenbergResp;
        try {
            gotenbergResp = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (java.net.http.HttpTimeoutException e) {
            log.error("Gotenberg 超时: url={}, timeout=30s", gotenbergUrl);
            response.setStatus(500);
            response.getWriter().write("PDF 生成超时，请简化简历内容后重试");
            return;
        } catch (java.net.ConnectException e) {
            log.error("Gotenberg 连接被拒绝: url={}", gotenbergUrl);
            response.setStatus(500);
            response.getWriter().write("PDF 生成失败：无法连接到 PDF 渲染服务");
            return;
        } catch (Exception e) {
            log.error("Gotenberg 请求异常: url={}, error={}", gotenbergUrl, e.getMessage());
            response.setStatus(500);
            response.getWriter().write("PDF 生成失败：" + e.getMessage());
            return;
        }

        if (gotenbergResp.statusCode() != 200) {
            log.error("Gotenberg 返回错误: status={}, body={}", gotenbergResp.statusCode(),
                    new String(gotenbergResp.body(), StandardCharsets.UTF_8));
            response.setStatus(500);
            response.getWriter().write("PDF 生成失败（Gotenberg 返回 " + gotenbergResp.statusCode() + "）");
            return;
        }

        // 返回 PDF
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=resume.pdf");
        response.setContentLength(gotenbergResp.body().length);
        response.getOutputStream().write(gotenbergResp.body());
    }

    /**
     * 导出 Word - 接收简历 JSON 内容，服务端生成 .docx 文件
     */
    @PostMapping("/export/word")
    @SuppressWarnings("unchecked")
    public void exportWord(@RequestBody Map<String, Object> params, HttpServletResponse response) throws Exception {
        Object contentObj = params.get("content");
        if (contentObj == null) {
            response.setStatus(400);
            response.getWriter().write("缺少 content 参数");
            return;
        }

        JSONObject content;
        if (contentObj instanceof Map) {
            content = JSON.parseObject(JSON.toJSONString(contentObj));
        } else {
            content = JSON.parseObject(contentObj.toString());
        }

        log.info("导出Word, content fields: {}", content.keySet());

        try (XWPFDocument doc = new XWPFDocument()) {
            // 标题：姓名
            JSONObject baseInfo = content.getJSONObject("baseInfo");
            String name = baseInfo != null ? baseInfo.getString("name") : "";
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(name.isEmpty() ? "简历" : name);
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setFontFamily("微软雅黑");

            // 联系方式
            if (baseInfo != null) {
                XWPFParagraph contactPara = doc.createParagraph();
                contactPara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun contactRun = contactPara.createRun();
                StringBuilder contact = new StringBuilder();
                if (isNotEmpty(baseInfo.getString("phone"))) contact.append(" ").append(baseInfo.getString("phone"));
                if (isNotEmpty(baseInfo.getString("email"))) contact.append(" | ").append(baseInfo.getString("email"));
                if (isNotEmpty(baseInfo.getString("city"))) contact.append(" | ").append(baseInfo.getString("city"));
                contactRun.setText(contact.length() > 0 ? contact.substring(1) : "");
                contactRun.setFontSize(10);
                contactRun.setFontFamily("微软雅黑");
                contactRun.setColor("666666");
            }

            // 求职意向
            JSONObject intention = content.getJSONObject("intention");
            if (intention != null && isNotEmpty(intention.getString("position"))) {
                XWPFParagraph intentPara = doc.createParagraph();
                XWPFRun intentRun = intentPara.createRun();
                StringBuilder intent = new StringBuilder("【求职意向】");
                intent.append(intention.getString("position"));
                if (isNotEmpty(intention.getString("city"))) intent.append(" · ").append(intention.getString("city"));
                if (isNotEmpty(intention.getString("salary"))) intent.append(" · ").append(intention.getString("salary"));
                intentRun.setText(intent.toString());
                intentRun.setFontSize(11);
                intentRun.setFontFamily("微软雅黑");
            }

            // 教育经历
            JSONArray education = content.getJSONArray("education");
            if (education != null && !education.isEmpty()) {
                addSectionTitle(doc, "教育经历");
                for (int i = 0; i < education.size(); i++) {
                    JSONObject edu = education.getJSONObject(i);
                    XWPFParagraph p = doc.createParagraph();
                    XWPFRun r = p.createRun();
                    String school = edu.getString("school");
                    String major = edu.getString("major");
                    String degree = edu.getString("degree");
                    String period = buildPeriod(edu.getString("start"), edu.getString("end"));
                    String gpa = edu.getString("gpa");
                    StringBuilder line = new StringBuilder();
                    if (isNotEmpty(school)) line.append(school);
                    if (isNotEmpty(major)) line.append(" | ").append(major);
                    if (isNotEmpty(degree)) line.append(" | ").append(degree);
                    if (isNotEmpty(period)) line.append(" | ").append(period);
                    if (isNotEmpty(gpa)) line.append(" | GPA: ").append(gpa);
                    r.setText(line.toString());
                    r.setFontSize(10);
                    r.setFontFamily("微软雅黑");
                }
            }

            // 工作经历
            JSONArray experience = content.getJSONArray("experience");
            if (experience != null && !experience.isEmpty()) {
                addSectionTitle(doc, "工作经历");
                for (int i = 0; i < experience.size(); i++) {
                    JSONObject exp = experience.getJSONObject(i);
                    XWPFParagraph p = doc.createParagraph();
                    XWPFRun r = p.createRun();
                    String company = exp.getString("company");
                    String position = exp.getString("position");
                    String period = buildPeriod(exp.getString("start"), exp.getString("end"));
                    StringBuilder line = new StringBuilder();
                    if (isNotEmpty(company)) line.append(company);
                    if (isNotEmpty(position)) line.append(" · ").append(position);
                    if (isNotEmpty(period)) line.append("  (").append(period).append(")");
                    r.setText(line.toString());
                    r.setBold(true);
                    r.setFontSize(10);
                    r.setFontFamily("微软雅黑");

                    String desc = exp.getString("desc");
                    if (isNotEmpty(desc)) {
                        XWPFParagraph descP = doc.createParagraph();
                        XWPFRun descR = descP.createRun();
                        descR.setText(desc);
                        descR.setFontSize(10);
                        descR.setFontFamily("微软雅黑");
                    }
                }
            }

            // 项目经验
            JSONArray projects = content.getJSONArray("projects");
            if (projects != null && !projects.isEmpty()) {
                addSectionTitle(doc, "项目经验");
                for (int i = 0; i < projects.size(); i++) {
                    JSONObject proj = projects.getJSONObject(i);
                    XWPFParagraph p = doc.createParagraph();
                    XWPFRun r = p.createRun();
                    String projectName = proj.getString("name");
                    String role = proj.getString("role");
                    String period = buildPeriod(proj.getString("start"), proj.getString("end"));
                    StringBuilder line = new StringBuilder();
                    if (isNotEmpty(projectName)) line.append(projectName);
                    if (isNotEmpty(role)) line.append(" · ").append(role);
                    if (isNotEmpty(period)) line.append("  (").append(period).append(")");
                    r.setText(line.toString());
                    r.setBold(true);
                    r.setFontSize(10);
                    r.setFontFamily("微软雅黑");

                    String desc = proj.getString("desc");
                    if (isNotEmpty(desc)) {
                        XWPFParagraph descP = doc.createParagraph();
                        XWPFRun descR = descP.createRun();
                        descR.setText(desc);
                        descR.setFontSize(10);
                        descR.setFontFamily("微软雅黑");
                    }
                }
            }

            // 技能特长
            JSONArray skills = content.getJSONArray("skills");
            if (skills != null && !skills.isEmpty()) {
                addSectionTitle(doc, "技能特长");
                XWPFParagraph p = doc.createParagraph();
                XWPFRun r = p.createRun();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < skills.size(); i++) {
                    if (i > 0) sb.append("、");
                    sb.append(skills.getString(i));
                }
                r.setText(sb.toString());
                r.setFontSize(10);
                r.setFontFamily("微软雅黑");
            }

            // 自我评价
            String evaluation = content.getString("evaluation");
            if (isNotEmpty(evaluation)) {
                addSectionTitle(doc, "自我评价");
                XWPFParagraph p = doc.createParagraph();
                XWPFRun r = p.createRun();
                r.setText(evaluation);
                r.setFontSize(10);
                r.setFontFamily("微软雅黑");
            }

            // 输出文件
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=resume.docx");
            doc.write(response.getOutputStream());
        }
    }

    /** 添加章节标题 */
    private void addSectionTitle(XWPFDocument doc, String title) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(title);
        r.setBold(true);
        r.setFontSize(12);
        r.setFontFamily("微软雅黑");
        r.setColor("1a1a1a");
    }

    /** 拼接时间段 */
    private String buildPeriod(String start, String end) {
        StringBuilder sb = new StringBuilder();
        if (isNotEmpty(start)) sb.append(start);
        if (isNotEmpty(end)) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(end);
        }
        return sb.toString();
    }

    /** 构建 Gotenberg multipart 请求体 */
    private byte[] buildGotenbergBody(String html, String boundary) throws Exception {
        String nl = "\r\n";
        String body = "--" + boundary + nl
                + "Content-Disposition: form-data; name=\"files\"; filename=\"index.html\"" + nl
                + "Content-Type: text/html; charset=utf-8" + nl + nl
                + html + nl
                + "--" + boundary + nl
                + "Content-Disposition: form-data; name=\"paperWidth\"" + nl + nl + "8.27" + nl
                + "--" + boundary + nl
                + "Content-Disposition: form-data; name=\"paperHeight\"" + nl + nl + "11.7" + nl
                + "--" + boundary + nl
                + "Content-Disposition: form-data; name=\"marginTop\"" + nl + nl + "0" + nl
                + "--" + boundary + nl
                + "Content-Disposition: form-data; name=\"marginBottom\"" + nl + nl + "0" + nl
                + "--" + boundary + nl
                + "Content-Disposition: form-data; name=\"marginLeft\"" + nl + nl + "0" + nl
                + "--" + boundary + nl
                + "Content-Disposition: form-data; name=\"marginRight\"" + nl + nl + "0" + nl
                + "--" + boundary + nl
                + "Content-Disposition: form-data; name=\"preferCssPageSize\"" + nl + nl + "true" + nl
                + "--" + boundary + "--";
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private JSONArray buildMessages(String systemPrompt, String userMessage) {
        JSONArray messages = new JSONArray();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        return messages;
    }

    /** 从 AI 响应中提取 JSON（去除 markdown 代码块标记） */
    private String extractJson(String response) {
        String json = response;
        if (json.contains("```json")) {
            json = json.substring(json.indexOf("```json") + 7);
            if (json.contains("```")) {
                json = json.substring(0, json.indexOf("```"));
            }
        } else if (json.contains("```")) {
            json = json.substring(json.indexOf("```") + 3);
            if (json.contains("```")) {
                json = json.substring(0, json.indexOf("```"));
            }
        }
        return json.trim();
    }

    /** 从 JSONArray 中提取指定字段名 + 完整结构（减少 token） */
    private JSONArray extractDescArray(JSONArray arr, String field) {
        JSONArray result = new JSONArray();
        if (arr == null) return result;
        for (int i = 0; i < arr.size(); i++) {
            JSONObject item = arr.getJSONObject(i);
            JSONObject out = new JSONObject();
            // 保留关键结构和 desc 字段
            for (String key : item.keySet()) {
                if (key.equals(field) || key.equals("company") || key.equals("name")
                        || key.equals("position") || key.equals("role")) {
                    out.put(key, item.get(key));
                }
            }
            result.add(out);
        }
        return result;
    }

    /** 生成简历语义指纹（用于缓存键，降低全量JSON变更导致的miss率） */
    private String generateClinicFingerprint(JSONObject resume) {
        StringBuilder sb = new StringBuilder("v1|");

        // 技能标签（排序后稳定）
        JSONArray skills = resume.getJSONArray("skills");
        if (skills != null && !skills.isEmpty()) {
            java.util.List<String> sorted = new java.util.ArrayList<>();
            for (int i = 0; i < skills.size(); i++) {
                String s = skills.getString(i);
                if (s != null) sorted.add(s.trim().toLowerCase());
            }
            java.util.Collections.sort(sorted);
            sb.append("s=").append(String.join(",", sorted)).append("|");
        }

        // 项目数 + 经历数（结构特征）
        JSONArray projects = resume.getJSONArray("projects");
        JSONArray experience = resume.getJSONArray("experience");
        sb.append("p=").append(projects != null ? projects.size() : 0).append("|");
        sb.append("e=").append(experience != null ? experience.size() : 0).append("|");

        // 自我评价摘要（取前100字符）
        String eval = resume.getString("evaluation");
        if (eval != null && !eval.isEmpty()) {
            sb.append("ev=").append(eval.length() > 100 ? eval.substring(0, 100) : eval).append("|");
        }

        // 期望岗位
        JSONObject intention = resume.getJSONObject("intention");
        if (intention != null) {
            String pos = intention.getString("position");
            if (pos != null) sb.append("pos=").append(pos.trim().toLowerCase());
        }

        return sb.toString();
    }

    /**
     * 构建简历诊断用结构化摘要（减少 token）
     */
    private JSONObject buildClinicLeanInput(JSONObject full) {
        JSONObject leanInput = new JSONObject();

        JSONObject fields = new JSONObject();
        JSONObject baseInfo = full.getJSONObject("baseInfo");
        fields.put("hasName", baseInfo != null && isNotEmpty(baseInfo.getString("name")));
        fields.put("hasPhone", baseInfo != null && isNotEmpty(baseInfo.getString("phone")));
        fields.put("hasEmail", baseInfo != null && isNotEmpty(baseInfo.getString("email")));
        fields.put("hasAvatar", baseInfo != null && isNotEmpty(baseInfo.getString("avatar")));
        JSONObject intention = full.getJSONObject("intention");
        fields.put("hasPosition", intention != null && isNotEmpty(intention.getString("position")));
        JSONArray eduArr = full.getJSONArray("education");
        fields.put("educationCount", eduArr != null ? eduArr.size() : 0);
        JSONArray expArr = full.getJSONArray("experience");
        fields.put("experienceCount", expArr != null ? expArr.size() : 0);
        JSONArray projArr = full.getJSONArray("projects");
        fields.put("projectCount", projArr != null ? projArr.size() : 0);
        JSONArray skillsArr = full.getJSONArray("skills");
        fields.put("skillCount", skillsArr != null ? skillsArr.size() : 0);
        String evaluation = full.getString("evaluation");
        fields.put("hasEvaluation", isNotEmpty(evaluation));
        leanInput.put("fields", fields);

        leanInput.put("skills", skillsArr);
        if (isNotEmpty(evaluation)) {
            leanInput.put("evaluation", evaluation.length() > 300 ? evaluation.substring(0, 300) : evaluation);
        }
        if (expArr != null && !expArr.isEmpty()) {
            leanInput.put("experience", extractDescArray(expArr, "desc"));
        }
        if (projArr != null && !projArr.isEmpty()) {
            leanInput.put("projects", extractDescArray(projArr, "desc"));
        }
        return leanInput;
    }

    /** 非空判断（避免引入 StringUtils 依赖） */
    private boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    /**
     * 校验AI解析结果的关键字段，缺失时重试一次
     */
    private void validateAndReAsk(String originalText, JSONObject result) {
        JSONObject baseInfo = result.getJSONObject("baseInfo");
        JSONArray education = result.getJSONArray("education");
        JSONArray skills = result.getJSONArray("skills");

        List<String> missingLabels = new ArrayList<>();
        if (baseInfo == null || !isNotEmpty(baseInfo.getString("name"))) missingLabels.add("姓名");
        if (baseInfo == null || !isNotEmpty(baseInfo.getString("phone"))) missingLabels.add("手机号");
        if (baseInfo == null || !isNotEmpty(baseInfo.getString("email"))) missingLabels.add("邮箱");
        if (education == null || education.isEmpty()) missingLabels.add("教育经历");
        if (skills == null || skills.isEmpty()) missingLabels.add("技能");

        if (missingLabels.isEmpty()) {
            return;
        }

        log.info("AI解析缺失字段({}), 进行重试: {}", missingLabels.size(), String.join(", ", missingLabels));

        StringBuilder reAskPrompt = new StringBuilder();
        reAskPrompt.append("简历解析补充。以下字段在之前未提取到，请根据原始文本补充。\n");
        reAskPrompt.append("缺失: ").append(String.join(", ", missingLabels)).append("\n\n");
        reAskPrompt.append("原始文本:\n").append(originalText).append("\n\n");
        reAskPrompt.append("输出完整JSON，字段结构同前。未缺失字段保持原样。不要markdown。");

        JSONArray messages = buildMessages(PARSE_SYSTEM_PROMPT, reAskPrompt.toString());
        try {
            String response = deepSeekService.callDeepSeek(messages);
            String json = extractJson(response);
            JSONObject reAskParsed = JSON.parseObject(json);
            mergeReAskResult(result, reAskParsed);
            log.info("重试解析合并完成");
        } catch (Exception e) {
            log.warn("重试解析失败: {}", e.getMessage());
        }
    }

    /**
     * 合并重试解析结果（仅覆盖空缺字段）
     */
    private void mergeReAskResult(JSONObject target, JSONObject patch) {
        if (patch == null) return;

        // baseInfo: 逐字段填补空缺
        JSONObject targetBase = target.getJSONObject("baseInfo");
        JSONObject patchBase = patch.getJSONObject("baseInfo");
        if (targetBase != null && patchBase != null) {
            for (String key : patchBase.keySet()) {
                if (!isNotEmpty(targetBase.getString(key))) {
                    targetBase.put(key, patchBase.getString(key));
                }
            }
        }

        // intention: 逐字段填补
        JSONObject targetInt = target.getJSONObject("intention");
        JSONObject patchInt = patch.getJSONObject("intention");
        if (targetInt != null && patchInt != null) {
            for (String key : patchInt.keySet()) {
                if (!isNotEmpty(targetInt.getString(key))) {
                    targetInt.put(key, patchInt.getString(key));
                }
            }
        }

        // 数组字段：target 为空时才用 patch 的
        String[] arrayKeys = {"education", "experience", "projects", "skills"};
        for (String key : arrayKeys) {
            JSONArray targetArr = target.getJSONArray(key);
            if (targetArr == null || targetArr.isEmpty()) {
                JSONArray patchArr = patch.getJSONArray(key);
                if (patchArr != null && !patchArr.isEmpty()) {
                    target.put(key, patchArr);
                }
            }
        }

        // evaluation: target 为空时使用 patch
        if (!isNotEmpty(target.getString("evaluation"))) {
            String patchEval = patch.getString("evaluation");
            if (isNotEmpty(patchEval)) {
                target.put("evaluation", patchEval);
            }
        }
    }

    /**
     * 解析上传的简历文件（支持 txt / md / docx / pdf）
     * 通过 AI 提取结构化字段并返回
     */
    @PostMapping("/parse")
    public AjaxResult parseResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return AjaxResult.error("文件不能为空");
        }

        try {
            // 1. 提取文本
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
            }
            String text = extractText(file, ext);
            if (text == null || text.trim().isEmpty()) {
                return AjaxResult.error("无法读取文件内容，请确认文件格式正确");
            }

            // 限制文本长度（AI 上下文窗口）
            if (text.length() > 30000) {
                text = text.substring(0, 30000);
            }

            // 检查缓存（基于文件内容的哈希）
            String cached = chatCacheService.getAiCache("parse", text);
            if (cached != null) {
                log.debug("parse 缓存命中: file={}", originalName);
                return AjaxResult.success("解析成功", JSON.parseObject(cached));
            }

            // 2. 调用 AI 解析
            String systemPrompt = PARSE_SYSTEM_PROMPT;
            String userMessage = "Parse:\n\n" + text;

            JSONArray messages = new JSONArray();
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            String aiResponse = deepSeekService.callDeepSeek(messages);

            // 3. 提取 JSON（AI 可能返回 markdown 代码块）
            String jsonStr = aiResponse;
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
                }
            }
            jsonStr = jsonStr.trim();

            JSONObject parsed = JSON.parseObject(jsonStr);

            // 4. 合并默认值（确保字段完整）
            JSONObject result = new JSONObject();
            JSONObject defaultBaseInfo = JSON.parseObject("{\"name\":\"\",\"phone\":\"\",\"email\":\"\",\"avatar\":\"\",\"gender\":\"\",\"birth\":\"\",\"city\":\"\"}");
            JSONObject baseInfo = parsed.getJSONObject("baseInfo");
            if (baseInfo != null) {
                defaultBaseInfo.putAll(baseInfo);
            }
            result.put("baseInfo", defaultBaseInfo);

            JSONObject defaultIntention = JSON.parseObject("{\"position\":\"\",\"city\":\"\",\"salary\":\"\",\"entryTime\":\"\"}");
            JSONObject intention = parsed.getJSONObject("intention");
            if (intention != null) {
                defaultIntention.putAll(intention);
            }
            result.put("intention", defaultIntention);

            result.put("education", parsed.getJSONArray("education") != null ? parsed.getJSONArray("education") : new JSONArray());
            result.put("experience", parsed.getJSONArray("experience") != null ? parsed.getJSONArray("experience") : new JSONArray());
            result.put("projects", parsed.getJSONArray("projects") != null ? parsed.getJSONArray("projects") : new JSONArray());
            result.put("skills", parsed.getJSONArray("skills") != null ? parsed.getJSONArray("skills") : new JSONArray());
            result.put("evaluation", parsed.containsKey("evaluation") ? parsed.getString("evaluation") : "");

            // 5. 校验关键字段，缺失则重试一次
            validateAndReAsk(text, result);

            log.info("简历解析成功: file={}, name={}", originalName, defaultBaseInfo.getString("name"));

            // 写入缓存
            chatCacheService.putAiCache("parse", text, result.toJSONString());

            return AjaxResult.success("解析成功", result);

        } catch (Exception e) {
            log.error("简历解析失败: {}", e.getMessage(), e);
            return AjaxResult.error("解析失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件类型提取文本内容
     */
    private String extractText(MultipartFile file, String ext) throws Exception {
        switch (ext) {
            case "txt":
            case "md":
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
                try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                    StringBuilder sb = new StringBuilder();
                    for (XWPFParagraph para : doc.getParagraphs()) {
                        sb.append(para.getText()).append("\n");
                    }
                    return sb.toString();
                }
            case "pdf":
                try (PDDocument doc = PDDocument.load(file.getInputStream())) {
                    CoordinateTextStripper stripper = new CoordinateTextStripper();
                    return stripper.getReconstructedText(doc);
                }
            default:
                return null;
        }
    }
}
