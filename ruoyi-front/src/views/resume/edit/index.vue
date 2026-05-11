<template>
  <div class="resume-editor-fullscreen">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-button text @click="showSidebar = !showSidebar" class="sidebar-toggle">
          <el-icon><View v-if="showSidebar" /><Hide v-else /></el-icon>
          {{ showSidebar ? '隐藏编辑器' : '显示编辑器' }}
        </el-button>
        <span class="editor-title">简历编辑器</span>
      </div>
      <div class="toolbar-center">
        <div class="template-selector" ref="templatePickerRef">
          <span class="template-current" @click="showTemplatePicker = !showTemplatePicker">
            <el-icon><CopyDocument /></el-icon>
            {{ templateLabel }}
            <el-icon :class="['chevron', { open: showTemplatePicker }]"><ArrowDown /></el-icon>
          </span>
          <Teleport to="body">
            <div v-if="showTemplatePicker" class="template-picker-dropdown" :style="pickerStyle" @click.stop>
              <div class="picker-arrow" />
              <div class="picker-cards">
                <div
                  v-for="tpl in templates"
                  :key="tpl.id"
                  class="template-card"
                  :class="{ active: resumeStore.templateId === tpl.id }"
                  @click.stop="selectTemplate(tpl.id)"
                >
                  <div class="template-preview" v-html="tpl.preview"></div>
                  <div class="template-card-label">{{ tpl.label }}</div>
                </div>
              </div>
            </div>
          </Teleport>
        </div>
      </div>
      <div class="toolbar-right">
        <div class="completion-indicator" :title="completionHint">
          <el-progress type="circle" :percentage="completionPercent" :width="28" :stroke-width="3" />
        </div>
        <div v-if="!onePageOk" class="page-warn" title="内容超过一页 A4，建议精简">
          <el-icon color="#e6a23c"><WarningFilled /></el-icon>
        </div>
        <div class="save-indicator" :class="saveStatusClass" :title="saveStatusTip">
          <span class="save-dot" />
          <span class="save-label">{{ saveStatusLabel }}</span>
        </div>
        <el-button size="small" @click="triggerImport" :loading="importing" :disabled="importing">
          <el-icon><Upload /></el-icon>
          导入简历
        </el-button>
        <input
          ref="importInputRef"
          type="file"
          accept=".txt,.md,.docx,.pdf"
          style="display:none"
          @change="onImportFile"
        />
        <el-button size="small" @click="showJdDialog = true">
          <el-icon><Document /></el-icon>
          JD 适配
        </el-button>
        <!-- 主题色快速切换 -->
        <div class="toolbar-theme-dots" v-if="showSidebar">
          <div
            v-for="t in toolbarThemes"
            :key="t.id"
            class="theme-dot"
            :class="{ active: isToolbarThemeActive(t) }"
            :style="{ background: t.primaryColor }"
            :title="t.label"
            @click="applyToolbarTheme(t)"
          />
        </div>

        <el-button size="small" :icon="Brush" @click="showStylePanel = !showStylePanel">
          样式
        </el-button>

        <el-button
          size="small"
          :type="editMode ? 'primary' : 'default'"
          :icon="EditPen"
          @click="editMode = !editMode"
        >
          {{ editMode ? '退出编辑' : '编辑预览' }}
        </el-button>
        <el-button type="primary" size="small" @click="handleExport" :disabled="showExportModal">
          导出 PDF/Word
        </el-button>
      </div>
    </div>

    <!-- 可切换的侧边栏 -->
    <div class="editor-body">
      <!-- 左侧模块导航 -->
      <ModuleNav v-show="showSidebar" />

      <!-- 中间表单编辑区 -->
      <div v-show="showSidebar" class="edit-form-wrapper" :style="editFormStyle">
        <!-- empty guide content... -->
        <div v-if="isEmptyResume" class="empty-guide">
          <div class="empty-guide-icon">
            <el-icon :size="40"><Document /></el-icon>
          </div>
          <h2 class="empty-guide-title">开始创建你的简历</h2>
          <p class="empty-guide-desc">填写基本信息、教育经历、工作经验和技能，AI 将辅助优化你的简历内容。</p>
          <div class="empty-guide-actions">
            <el-button type="primary" @click="scrollToSection('baseInfo')">填写基本信息</el-button>
            <el-button @click="scrollToSection('education')">添加教育经历</el-button>
          </div>
          <div class="empty-guide-tips">
            <div class="guide-tip">
              <span class="tip-icon"><el-icon color="#2563eb"><MagicStick /></el-icon></span>
              <span>AI 诊断优化你的简历内容</span>
            </div>
            <div class="guide-tip">
              <span class="tip-icon"><el-icon color="#10b981"><EditPen /></el-icon></span>
              <span>多模板切换，实时预览</span>
            </div>
            <div class="guide-tip">
              <span class="tip-icon"><el-icon color="#e6a23c"><Upload /></el-icon></span>
              <span>支持导入现有简历文件</span>
            </div>
          </div>
        </div>
        <EditForm />
      </div>

      <!-- 可拖拽分隔条 -->
      <div v-show="showSidebar" class="resize-handle" @mousedown="onResizeStart" />

      <!-- 右侧预览区 / AI 面板 -->
      <div class="preview-wrapper" :class="{ 'preview-full': !showSidebar }">
        <div class="preview-tabs">
          <button
            class="preview-tab"
            :class="{ active: previewTab === 'preview' }"
            @click="previewTab = 'preview'"
          >
            <el-icon><View /></el-icon> 预览
          </button>
          <button
            class="preview-tab"
            :class="{ active: previewTab === 'ai' }"
            @click="previewTab = 'ai'; fetchClinicResult()"
          >
            <el-icon><MagicStick /></el-icon> AI
          </button>
        </div>
        <PreviewPane
          v-show="previewTab === 'preview'"
          :annotations="previewAnnotations"
          :focused-section="focusedSection"
          :edit-mode="editMode"
          @section-click="onSectionClick"
          @preview-module-click="onPreviewModuleClick"
        />
        <AIPanel
          v-show="previewTab === 'ai'"
          :clinic-result="clinicResult"
          :clinic-loading="clinicLoading"
          :active-section="activeSection"
          @re-diagnose="handleClinic"
          @focus-section="onFocusSection"
        />
      </div>
    </div>

    <!-- 导出弹窗 -->
    <ExportModal v-model="showExportModal" />

    <!-- 样式面板 -->
    <StylePanel :visible="showStylePanel" @update:visible="showStylePanel = $event" />

    <!-- 撤回面板 -->
    <UndoPanel />

    <!-- 导入遮罩 -->
    <div v-if="importStep || importError" class="import-overlay">
      <div class="import-overlay-inner">
        <template v-if="importStep && !importError">
          <div class="import-spinner">
            <el-icon :size="48" class="spinner-icon"><Loading /></el-icon>
          </div>
          <p class="import-step-text">{{ importStep }}</p>
          <div class="import-progress-bar">
            <div class="import-progress-fill" :class="{ wide: importStep.includes('解析'), full: importStep.includes('完成') }" />
          </div>
        </template>
        <template v-else-if="importError">
          <div class="import-error-icon">
            <el-icon :size="48" color="#f56c6c"><Close /></el-icon>
          </div>
          <p class="import-error-text">{{ importError }}</p>
          <div class="import-error-actions">
            <el-button type="primary" @click="retryImport">重试</el-button>
            <el-button @click="importError = ''; importing = false; importStep = ''">取消</el-button>
          </div>
        </template>
      </div>
    </div>

    <!-- AI 诊断弹窗 -->
    <el-dialog v-model="showClinicDialog" title="AI 简历诊断" width="620px" :close-on-click-modal="false" destroy-on-close>
      <template v-if="clinicLoading">
        <div class="ai-dialog-loading">
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          <p>AI 正在分析简历...</p>
        </div>
      </template>
      <template v-else-if="clinicResult">
        <!-- 缺失项 -->
        <div v-if="clinicResult.missing?.length" class="clinic-section">
          <h3 class="clinic-section-title">
            <el-icon color="#e6a23c"><WarningFilled /></el-icon>
            待改进
          </h3>
          <ul class="clinic-missing-list">
            <li v-for="(item, i) in clinicResult.missing" :key="i">{{ item }}</li>
          </ul>
        </div>

        <!-- AI 生成内容 -->
        <div v-if="clinicResult.generated" class="clinic-section">
          <h3 class="clinic-section-title">
            <el-icon color="#409eff"><MagicStick /></el-icon>
            AI 生成内容
          </h3>
          <div v-if="clinicResult.generated.evaluation" class="clinic-gen-item">
            <div class="gen-label">自我评价</div>
            <div class="gen-text">{{ clinicResult.generated.evaluation }}</div>
            <el-button size="small" type="primary" @click="applyClinicField('evaluation', clinicResult.generated.evaluation)">应用</el-button>
          </div>
          <div v-if="clinicResult.generated.skills?.length" class="clinic-gen-item">
            <div class="gen-label">技能标签</div>
            <div class="gen-tags">
              <el-tag v-for="sk in clinicResult.generated.skills" :key="sk" size="small">{{ sk }}</el-tag>
            </div>
            <el-button size="small" type="primary" @click="applyClinicField('skills', clinicResult.generated.skills)">应用</el-button>
          </div>
        </div>

        <!-- 项目优化建议 -->
        <div v-if="clinicResult.projectTips?.length" class="clinic-section">
          <h3 class="clinic-section-title">
            <el-icon color="#67c23a"><EditPen /></el-icon>
            项目描述优化
          </h3>
          <div v-for="(tip, i) in clinicResult.projectTips" :key="i" class="clinic-tip-item">
            <div class="tip-index">#{{ Number(i) + 1 }}</div>
            <div class="tip-text">{{ tip.tip }}</div>
            <el-button size="small" type="success" @click="applyClinicProjectTip(tip)">替换</el-button>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="ai-dialog-loading">
          <el-icon :size="28" color="#999"><WarningFilled /></el-icon>
          <p>诊断失败，请重试</p>
        </div>
      </template>
      <template #footer>
        <el-button @click="showClinicDialog = false">关闭</el-button>
        <el-button v-if="clinicResult" type="primary" @click="applyAllClinic">全部应用</el-button>
      </template>
    </el-dialog>

    <!-- JD 岗位适配弹窗 -->
    <el-dialog v-model="showJdDialog" title="JD 岗位适配" width="680px" :close-on-click-modal="false" destroy-on-close>
      <template v-if="!jdResult">
        <p class="jd-hint">粘贴目标岗位描述，AI 将根据 JD 优化您的简历内容。<strong>不会编造经历</strong>，仅对已有内容调整措辞和重点。</p>
        <el-input v-model="jdText" type="textarea" :rows="8" placeholder="请粘贴岗位描述（Job Description），如：&#10;&#10;职位：Java后端开发工程师&#10;职责：负责核心业务系统的设计与开发...&#10;要求：熟悉Spring Boot、MySQL、Redis..." />
      </template>
      <template v-else>
        <div class="jd-result-header">
          <el-icon color="#67c23a" :size="20"><CircleCheck /></el-icon>
          <span>适配完成</span>
        </div>
        <p class="jd-summary">{{ jdResult.summary }}</p>
        <el-alert
          title="预览适配后的简历内容。点击「应用更改」将覆盖当前编辑内容。"
          type="info"
          :closable="false"
          show-icon
        />
      </template>
      <template #footer>
        <el-button @click="closeJdDialog">取消</el-button>
        <el-button v-if="!jdResult" type="primary" :loading="jdLoading" @click="doJdAdapt">
          开始适配
        </el-button>
        <template v-else>
          <el-button @click="closeJdDialog">取消</el-button>
          <el-button type="primary" @click="applyJdAdapt">应用更改</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Upload, Document, Loading, WarningFilled, MagicStick, EditPen, CircleCheck, Close, Operation, View, Hide, Promotion, ArrowDown, CopyDocument, Brush } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { parseResumeFile, aiResumeAction } from '@/api/resume'
import { getProfile } from '@/api/profile'
import { useResumeStore } from '@/store/resume'
import ModuleNav from './components/ModuleNav.vue'
import EditForm from './components/EditForm.vue'
import PreviewPane from './components/PreviewPane.vue'
import AIPanel from './components/AIPanel.vue'
import ExportModal from './components/ExportModal.vue'
import StylePanel from './components/StylePanel.vue'
import UndoPanel from './components/UndoPanel.vue'

const route = useRoute()
const router = useRouter()
const resumeStore = useResumeStore()
const showExportModal = ref(false)
const importing = ref(false)
const importInputRef = ref<HTMLElement | null>(null)
const showSidebar = ref(false)
const importStep = ref('')
const importError = ref('')
const editMode = ref(false)
const showStylePanel = ref(false)

// AI 面板（右侧切换）
const previewTab = ref('preview')

// 切换至 AI 面板时，如果已有诊断结果直接展示
function fetchClinicResult() {
  // clinicResult 已由 handleClinic 填充，无需额外请求
  // 如果从未诊断过，AIPanel 内会显示空状态引导用户诊断
}

// AI 诊断
const showClinicDialog = ref(false)
const clinicLoading = ref(false)
const clinicResult = ref<any>(null)

// ========== 预览 ↔ AI 交互 ==========

interface Annotation {
  id: string
  module: string
  index?: number
  type: 'warning' | 'improve' | 'success' | 'ai'
  message: string
}

function guessMissingModule(text: string): string | null {
  const kw: [string, string][] = [
    ['基本信息', 'baseInfo'], ['姓名', 'baseInfo'], ['电话', 'baseInfo'], ['邮箱', 'baseInfo'],
    ['教育', 'education'],
    ['工作', 'experience'], ['经历', 'experience'],
    ['项目', 'projects'],
    ['技能', 'skills'],
    ['评价', 'evaluation'], ['自我', 'evaluation'],
    ['意向', 'intention'], ['期望', 'intention'],
    ['奖项', 'awards'], ['荣誉', 'awards'],
    ['证书', 'certificates'],
    ['校园', 'campus']
  ]
  for (const [keyword, module] of kw) {
    if (text.includes(keyword)) return module
  }
  return null
}

/** 将诊断结果转换为预览区的标注 */
const previewAnnotations = computed<Annotation[]>(() => {
  const result = clinicResult.value
  if (!result) return []
  const list: Annotation[] = []
  const add = (id: string, module: string, type: Annotation['type'], message: string, index?: number) => {
    list.push({ id, module, type, message, index })
  }

  // missing → warning badges
  if (Array.isArray(result.missing)) {
    for (const item of result.missing) {
      const mod = guessMissingModule(item)
      if (mod) {
        add('miss-' + list.length, mod, 'warning', item)
      }
    }
  }

  // projectTips → improve badges
  if (Array.isArray(result.projectTips)) {
    for (const tip of result.projectTips) {
      add(
        'tip-' + tip.module + '-' + tip.index,
        tip.module,
        'improve',
        tip.tip,
        tip.index
      )
    }
  }

  // generated evaluation → ai badge on evaluation section
  if (result.generated?.evaluation) {
    add('gen-eval', 'evaluation', 'ai', 'AI 生成了自我评价建议，点击查看')
  }

  return list
})

/** 当前在 AIPanel 中被选中的区块（由预览区点击触发） */
const activeSection = ref<{ module: string; index?: number } | null>(null)

/** 当前在预览区应高亮的区块（由 AIPanel 建议点击触发） */
const focusedSection = ref<{ module: string; index?: number } | null>(null)

/** 预览区标注被点击 → 切到 AI tab 并高亮相关建议 */
function onSectionClick(ann: Annotation) {
  activeSection.value = { module: ann.module, index: ann.index }
  if (previewTab.value !== 'ai') {
    previewTab.value = 'ai'
  }
}

/** 编辑模式下，点击预览区的模块 → 滚动 EditForm 到对应 section */
function onPreviewModuleClick(module: string) {
  const el = document.getElementById('section-' + module)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    // 闪烁高亮
    el.classList.add('section-highlight')
    setTimeout(() => el.classList.remove('section-highlight'), 1500)
  }
}

/** AIPanel 的建议被点击 → 切到预览 tab 并滚动到相关区块 */
function onFocusSection(payload: { module: string; index?: number }) {
  focusedSection.value = null // reset first for same-value trigger
  focusedSection.value = { module: payload.module, index: payload.index }
  if (previewTab.value !== 'preview') {
    previewTab.value = 'preview'
  }
}

// JD 适配
const showJdDialog = ref(false)
const jdText = ref('')
const jdLoading = ref(false)
const jdResult = ref<any>(null)

const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'z') {
    if (e.shiftKey) {
      e.preventDefault()
      resumeStore.redo()
    } else {
      e.preventDefault()
      resumeStore.undo()
    }
  }
}

// 简历完成度计算
const completionPercent = computed(() => {
  const c = resumeStore.content
  let score = 0
  const total = 12
  if (c.baseInfo.name) score++
  if (c.baseInfo.phone) score++
  if (c.baseInfo.email) score++
  if (c.intention.position) score++
  if (c.education.some(e => e.school && e.major)) score++
  if (c.experience.some(e => e.company && e.desc)) score++
  if (c.campus.some(e => e.organization && e.desc)) score++
  if (c.projects.some(e => e.name && e.desc)) score++
  if (c.awards.length) score++
  if (c.certificates.length) score++
  if (c.skills.length >= 3) score++
  if (c.evaluation) score++
  return Math.round((score / total) * 100)
})

const completionHint = computed(() => {
  const hints: string[] = []
  const c = resumeStore.content
  if (!c.baseInfo.name || !c.baseInfo.phone || !c.baseInfo.email) hints.push('基本信息')
  if (!c.intention.position) hints.push('求职意向')
  if (!c.education.some(e => e.school && e.major)) hints.push('教育经历')
  if (!c.experience.some(e => e.company && e.desc) && !c.campus.some(e => e.organization && e.desc) && !c.projects.some(e => e.name && e.desc)) hints.push('经历/项目')
  if (c.skills.length < 3) hints.push('技能')
  if (!c.evaluation) hints.push('自我评价')
  return `完成度 ${completionPercent.value}%` + (hints.length ? ` · 建议补充 ${hints.join('、')}` : '')
})

// 保存状态指示器
const saveStatusClass = computed(() => {
  if (resumeStore.saving) return 'is-saving'
  if (resumeStore.dirty) return 'is-dirty'
  return 'is-saved'
})
const saveStatusLabel = computed(() => {
  if (resumeStore.saving) return '保存中'
  if (resumeStore.dirty) return '未保存'
  return '已保存'
})
const saveStatusTip = computed(() => {
  if (resumeStore.saving) return '正在自动保存...'
  if (resumeStore.dirty) return '有未保存的修改'
  if (resumeStore.lastSaved) return '上次保存：' + resumeStore.lastSaved.toLocaleTimeString()
  return ''
})

// 空状态检测
const isEmptyResume = computed(() => {
  const c = resumeStore.content
  return !c.baseInfo.name && !c.baseInfo.phone && !c.baseInfo.email
})

// 编辑区/预览区拖拽分隔
const editFormWidth = ref('')
const isResizing = ref(false)
const editFormStyle = computed(() => {
  if (editFormWidth.value) {
    return { width: editFormWidth.value, flex: 'none', minWidth: '300px' }
  }
  return {}
})
function onResizeStart(e: MouseEvent) {
  isResizing.value = true
  const startX = e.clientX
  const startWidth = editFormWidth.value || (document.querySelector('.edit-form-wrapper') as HTMLElement)?.offsetWidth + 'px'
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  function onMove(ev: MouseEvent) {
    if (!isResizing.value) return
    const delta = ev.clientX - startX
    const currentW = parseInt(startWidth) + delta
    if (currentW > 300 && currentW < 1200) {
      editFormWidth.value = currentW + 'px'
    }
  }
  function onUp() {
    isResizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

// 一页检测
const onePageOk = ref(true)

function checkPageOverflow() {
  nextTick(() => {
    const el = document.getElementById('resume-preview')
    if (el) {
      const a4Height = 297 // mm
      // Convert mm to px (1mm ≈ 3.78px at 96dpi)
      const thresholdPx = a4Height * 3.78
      onePageOk.value = el.scrollHeight <= thresholdPx + 5 // 5px tolerance
    }
  })
}

watch(() => resumeStore.content, () => checkPageOverflow(), { deep: true })

onMounted(async () => {
  const id = route.params.id as string
  if (id && id !== 'new') {
    await resumeStore.loadResume(id as any)
  } else {
    resumeStore.initNewResume()
    await prefillFromProfile()
  }
  if (route.query.export === 'true') {
    setTimeout(() => handleExport(), 800)
  }
  setTimeout(() => checkPageOverflow(), 500)
  window.addEventListener('keydown', handleKeydown)
  document.addEventListener('click', onClickOutsidePicker, true)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('click', onClickOutsidePicker, true)
})

// 模板预览选择器
const showTemplatePicker = ref(false)
const templatePickerRef = ref<HTMLElement | null>(null)
function onClickOutsidePicker(e: MouseEvent) {
  if (!showTemplatePicker.value) return
  const dropdown = document.querySelector('.template-picker-dropdown')
  if (templatePickerRef.value && !templatePickerRef.value.contains(e.target as Node) &&
      (!dropdown || !dropdown.contains(e.target as Node))) {
    showTemplatePicker.value = false
  }
}
const pickerStyle = computed(() => {
  if (!showTemplatePicker.value) return { display: 'none' }
  const el = templatePickerRef.value
  if (!el) return { display: 'none' }
  const rect = el.getBoundingClientRect()
  return {
    position: 'fixed' as const,
    top: (rect.bottom + 4) + 'px',
    left: Math.max(8, Math.min(rect.left + rect.width / 2 - 164, window.innerWidth - 336)) + 'px',
    zIndex: 99999
  }
})
const templateLabel = computed(() => {
  const labels: Record<string, string> = { modern: '现代模板', classic: '经典模板', minimal: '极简模板' }
  return labels[resumeStore.templateId] || '选择模板'
})
const templates = [
  {
    id: 'modern',
    label: '现代模板',
    preview: '<svg viewBox="0 0 120 160" xmlns="http://www.w3.org/2000/svg"><rect width="120" height="160" fill="#fff"/><rect x="10" y="10" width="100" height="18" rx="2" fill="#e8f0fe"/><rect x="10" y="34" width="60" height="6" rx="2" fill="#2563eb" opacity=".15"/><rect x="10" y="46" width="100" height="4" rx="1" fill="#e2e8f0"/><rect x="10" y="54" width="80" height="4" rx="1" fill="#e2e8f0"/><rect x="10" y="68" width="40" height="5" rx="1" fill="#2563eb" opacity=".2"/><rect x="10" y="78" width="100" height="4" rx="1" fill="#e2e8f0"/><rect x="10" y="86" width="90" height="4" rx="1" fill="#e2e8f0"/><rect x="10" y="100" width="40" height="5" rx="1" fill="#2563eb" opacity=".2"/><rect x="10" y="110" width="100" height="4" rx="1" fill="#e2e8f0"/><rect x="10" y="118" width="70" height="4" rx="1" fill="#e2e8f0"/></svg>'
  },
  {
    id: 'classic',
    label: '经典模板',
    preview: '<svg viewBox="0 0 120 160" xmlns="http://www.w3.org/2000/svg"><rect width="120" height="160" fill="#faf9f8"/><rect x="8" y="8" width="104" height="20" rx="1" fill="#1a1a1a"/><rect x="8" y="34" width="60" height="4" rx="1" fill="#1a1a1a" opacity=".5"/><rect x="8" y="44" width="104" height="3" rx="1" fill="#d4d4d4"/><rect x="8" y="50" width="90" height="3" rx="1" fill="#d4d4d4"/><rect x="8" y="62" width="60" height="4" rx="1" fill="#1a1a1a" opacity=".5"/><rect x="8" y="70" width="104" height="3" rx="1" fill="#d4d4d4"/><rect x="8" y="76" width="85" height="3" rx="1" fill="#d4d4d4"/><rect x="8" y="88" width="60" height="4" rx="1" fill="#1a1a1a" opacity=".5"/><rect x="8" y="96" width="104" height="3" rx="1" fill="#d4d4d4"/><rect x="8" y="102" width="75" height="3" rx="1" fill="#d4d4d4"/></svg>'
  },
  {
    id: 'minimal',
    label: '极简模板',
    preview: '<svg viewBox="0 0 120 160" xmlns="http://www.w3.org/2000/svg"><rect width="120" height="160" fill="#fff"/><rect x="10" y="12" width="50" height="6" rx="3" fill="#333"/><rect x="10" y="22" width="70" height="3" rx="1.5" fill="#ccc"/><rect x="10" y="28" width="40" height="3" rx="1.5" fill="#ccc"/><rect x="10" y="42" width="100" height="3" rx="1" fill="#e5e5e5"/><rect x="10" y="50" width="100" height="3" rx="1" fill="#e5e5e5"/><rect x="10" y="64" width="80" height="3" rx="1" fill="#e5e5e5"/><rect x="10" y="72" width="60" height="3" rx="1" fill="#e5e5e5"/><rect x="10" y="86" width="100" height="3" rx="1" fill="#e5e5e5"/><rect x="10" y="94" width="70" height="3" rx="1" fill="#e5e5e5"/></svg>'
  }
]
function selectTemplate(id: string) {
  resumeStore.setTemplate(id)
  showTemplatePicker.value = false
}

const goBack = () => {
  router.push('/resume')
}

// ====== 工具栏主题快速切换 ======
const toolbarThemes = [
  { id: 'ink',     label: '墨韵', primaryColor: '#1a365d', color: '#2c3e50' },
  { id: 'cinnabar',label: '朱砂', primaryColor: '#8b1a1a', color: '#2c3e50' },
  { id: 'blue',    label: '青花', primaryColor: '#1e40af', color: '#2c3e50' },
  { id: 'green',   label: '竹青', primaryColor: '#2d6a4f', color: '#2c3e50' },
  { id: 'gold',    label: '鎏金', primaryColor: '#b8860b', color: '#2c3e50' },
  { id: 'rose',    label: '玫瑰', primaryColor: '#9d174d', color: '#2c3e50' },
]

function isToolbarThemeActive(t: typeof toolbarThemes[0]): boolean {
  return resumeStore.style.primaryColor === t.primaryColor && resumeStore.style.color === t.color
}

function applyToolbarTheme(t: typeof toolbarThemes[0]) {
  resumeStore.style.primaryColor = t.primaryColor
  resumeStore.style.color = t.color
  resumeStore.scheduleAutoSave()
}

/** Scroll to an edit form section (used by empty guide) */
function scrollToSection(module: string) {
  const el = document.getElementById('section-' + module)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const onTemplateChange = (val: string) => {
  resumeStore.setTemplate(val)
}

const handleExport = () => {
  resumeStore.saveResume().then(() => {
    showExportModal.value = true
  })
}

const triggerImport = () => {
  importInputRef.value?.click()
}

async function prefillFromProfile() {
  try {
    const res = await getProfile() as any
    const profile = res.data || res
    if (!profile || !profile.name) return

    const content = resumeStore.content
    if (profile.name) content.baseInfo.name = profile.name
    if (profile.phone) content.baseInfo.phone = profile.phone
    if (profile.email) content.baseInfo.email = profile.email
    if (profile.selfIntro) content.evaluation = profile.selfIntro

    if (profile.skills) {
      const skills = typeof profile.skills === 'string'
        ? JSON.parse(profile.skills)
        : Array.isArray(profile.skills) ? profile.skills : []
      if (skills.length) content.skills = skills
    }

    if (profile.school || profile.major) {
      content.education = [{
        school: profile.school || '',
        schoolId: null,
        major: profile.major || '',
        degree: profile.education || '',
        start: '',
        end: profile.graduationYear || '',
        gpa: '',
        courses: []
      }]
    }

    if (profile.projects?.length) {
      content.projects = profile.projects.map((p: any) => ({
        name: p.projectName || '',
        role: p.role || '',
        start: p.startDate || '',
        end: p.endDate || '',
        desc: p.description || ''
      }))
    }
  } catch (e: any) {
    console.log('无档案数据可预填', e.message)
  }
}

const onImportFile = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  importStep.value = '确认导入...'
  importError.value = ''

  try {
    await ElMessageBox.confirm(
      '导入将覆盖当前编辑内容，确认继续？',
      '确认导入',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    target.value = ''
    importStep.value = ''
    return
  }

  importing.value = true
  importStep.value = '正在读取文件...'
  importError.value = ''

  try {
    // Small delay so the overlay renders before the potentially slow parse
    await new Promise(r => setTimeout(r, 100))

    importStep.value = '正在解析内容...'
    const res = await parseResumeFile(file) as any
    const data = res.data || res

    importStep.value = '正在填充简历...'
    await new Promise(r => setTimeout(r, 200))

    if (data && data.baseInfo) {
      Object.assign(resumeStore.content, data)
	      resumeStore.autoShowModules()
      importStep.value = '导入完成 ✓'
      await new Promise(r => setTimeout(r, 800))
      ElMessage.success('导入成功，请检查并补充信息')
    } else {
      importError.value = '解析结果异常，请重试'
    }
  } catch (err: any) {
    console.error('导入简历失败:', err)
    importError.value = err.message || '导入失败，请重试'
  } finally {
    if (!importError.value) {
      importing.value = false
      importStep.value = ''
    }
    target.value = ''
  }
}

async function retryImport() {
  importError.value = ''
  importStep.value = '正在解析内容...'
  importing.value = true
  try {
    const input = importInputRef.value as HTMLInputElement
    const file = input?.files?.[0]
    if (!file) { importError.value = '请重新选择文件'; return }

    const res = await parseResumeFile(file) as any
    const data = res.data || res

    importStep.value = '正在填充简历...'
    await new Promise(r => setTimeout(r, 200))

    if (data && data.baseInfo) {
      Object.assign(resumeStore.content, data)
	      resumeStore.autoShowModules()
      importStep.value = '导入完成 ✓'
      await new Promise(r => setTimeout(r, 800))
      ElMessage.success('导入成功，请检查并补充信息')
      importing.value = false
      importStep.value = ''
    } else {
      importError.value = '解析结果异常，请重试'
    }
  } catch (err: any) {
    importError.value = err.message || '导入失败，请重试'
  } finally {
    if (!importError.value) {
      importing.value = false
      importStep.value = ''
    }
  }
}

async function handleClinic() {
  showClinicDialog.value = true
  clinicLoading.value = true
  clinicResult.value = null
  try {
    const resumeContent = JSON.parse(JSON.stringify(resumeStore.content))
    const res = await aiResumeAction({ action: 'clinic', resumeContent }) as any
    const data = res.data || res
    clinicResult.value = data || null
  } catch (err: any) {
    console.error('AI诊断失败:', err)
    ElMessage.error('诊断失败: ' + (err.message || '未知错误'))
  } finally {
    clinicLoading.value = false
  }
}

function applyClinicField(field: string, value: any) {
  const content = resumeStore.content
  if (field in content) {
    content[field] = JSON.parse(JSON.stringify(value))
    ElMessage.success('已应用')
    resumeStore.scheduleAutoSave()
  }
}

function applyClinicProjectTip(tip: any) {
  const { index, module: mod, tip: tipText } = tip
  const items = mod === 'experience' ? resumeStore.content.experience : resumeStore.content.projects
  if (items && items[index]) {
    items[index].desc = tipText
    ElMessage.success(mod === 'experience' ? '已替换工作描述' : '已替换项目描述')
    resumeStore.scheduleAutoSave()
  }
}

function applyAllClinic() {
  if (!clinicResult.value) return
  const result = clinicResult.value
  let count = 0

  if (result.generated) {
    if (result.generated.evaluation && resumeStore.content.evaluation !== undefined) {
      resumeStore.content.evaluation = result.generated.evaluation
      count++
    }
    if (result.generated.skills?.length) {
      resumeStore.content.skills = JSON.parse(JSON.stringify(result.generated.skills))
      count++
    }
  }

  if (result.projectTips?.length) {
    result.projectTips.forEach((tip: any) => {
      const mod = tip.module || 'projects'
      const items = mod === 'experience' ? resumeStore.content.experience : resumeStore.content.projects
      if (items && items[tip.index]) {
        items[tip.index].desc = tip.tip
        count++
      }
    })
  }

  if (count > 0) {
    ElMessage.success(`已应用 ${count} 项优化`)
    resumeStore.scheduleAutoSave()
  }
  showClinicDialog.value = false
}

async function doJdAdapt() {
  if (!jdText.value.trim()) {
    ElMessage.warning('请粘贴岗位描述')
    return
  }
  jdLoading.value = true
  try {
    const resumeContent = JSON.parse(JSON.stringify(resumeStore.content))
    const res = await aiResumeAction({ action: 'jd', resumeContent, jd: jdText.value }) as any
    const data = res.data || res
    jdResult.value = data || null
  } catch (err: any) {
    console.error('JD适配失败:', err)
    ElMessage.error('适配失败: ' + (err.message || '未知错误'))
  } finally {
    jdLoading.value = false
  }
}

function applyJdAdapt() {
  if (!jdResult.value?.adapted) return
  const adapted = jdResult.value.adapted
  Object.keys(adapted).forEach((key: string) => {
    if (key in resumeStore.content) {
      resumeStore.content[key] = adapted[key]
    }
  })
  ElMessage.success('已应用适配内容')
  closeJdDialog()
  resumeStore.scheduleAutoSave()
}

function closeJdDialog() {
  showJdDialog.value = false
  jdText.value = ''
  jdResult.value = null
}
</script>

<style scoped>
.resume-editor-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1000;
  background: #f0f7ff;
  display: flex;
  flex-direction: column;
}

.editor-toolbar {
  height: 52px;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(37, 99, 235, 0.10);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.editor-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e40af;
  letter-spacing: 0.3px;
}

.sidebar-toggle {
  color: #93c5fd;
  transition: color 0.2s;
}
.sidebar-toggle:hover {
  color: #2563eb;
}

/* 工具栏主题色圆点 */
.toolbar-theme-dots {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  border-right: 1px solid #e2e8f0;
  margin-right: 12px;
}

.theme-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  border: 2.5px solid transparent;
  transition: all 0.15s ease;
  flex-shrink: 0;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.06);
}
.theme-dot:hover {
  transform: scale(1.2);
}
.theme-dot.active {
  border-color: #fff;
  box-shadow: 0 0 0 2px #2563eb, 0 0 0 1px rgba(0,0,0,0.06);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.save-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 20px;
  background: rgba(255,255,255,0.6);
  border: 1px solid rgba(37, 99, 235, 0.08);
  transition: all 0.3s ease;
  cursor: default;
}
.save-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  transition: background 0.3s ease, box-shadow 0.3s ease;
}
.save-label {
  color: #64748b;
}
/* saved: green dot */
.save-indicator.is-saved .save-dot {
  background: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4);
}
/* saving: blue spinning dot */
.save-indicator.is-saving .save-dot {
  background: #2563eb;
  animation: save-pulse 0.8s ease-in-out infinite alternate;
}
@keyframes save-pulse {
  from { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.4); }
  to { box-shadow: 0 0 0 4px rgba(37, 99, 235, 0); }
}
/* dirty: orange dot */
.save-indicator.is-dirty .save-dot {
  background: #e6a23c;
  box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.4);
  animation: save-pulse 1.2s ease-in-out infinite;
}
.save-indicator.is-saving .save-label { color: #2563eb; }
.save-indicator.is-dirty .save-label { color: #e6a23c; }
.save-indicator.is-saved .save-label { color: #10b981; }

.completion-indicator {
  display: flex;
  align-items: center;
  cursor: help;
}

.page-warn {
  display: flex;
  align-items: center;
  cursor: help;
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.edit-form-wrapper {
  flex: 1;
  min-width: 400px;
  overflow-y: auto;
  padding: 24px 20px;
  background: #f0f7ff;
  border-right: 1px solid rgba(37, 99, 235, 0.08);
}

.preview-wrapper {
  width: 595px;
  flex-shrink: 0;
  overflow-y: auto;
  padding: 20px;
  background: #e2e8f0;
  transition: width 0.25s ease;
}

.preview-wrapper.preview-full {
  flex: 1;
  width: auto;
}

.preview-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 12px;
  background: rgba(255,255,255,0.5);
  border-radius: 10px;
  padding: 3px;
  flex-shrink: 0;
  border: 1px solid rgba(37, 99, 235, 0.08);
}

.preview-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: #60a5fa;
  background: transparent;
  border-radius: 7px;
  transition: all 0.2s;
}

.preview-tab:hover {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.06);
}

.preview-tab.active {
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
}

/* ── 空状态引导 ── */
.empty-guide {
  text-align: center;
  padding: 60px 32px 40px;
  background: #fff;
  border-radius: 16px;
  margin-bottom: 20px;
  border: 1px dashed rgba(37, 99, 235, 0.15);
}
.empty-guide-icon {
  margin-bottom: 16px;
  color: #93c5fd;
}
.empty-guide-title {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 8px;
}
.empty-guide-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 24px;
  line-height: 1.6;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}
.empty-guide-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 32px;
}
.empty-guide-tips {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}
.guide-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}
.tip-icon {
  display: flex;
  align-items: center;
}

/* ── 拖拽分隔条 ── */
.resize-handle {
  width: 4px;
  flex-shrink: 0;
  cursor: col-resize;
  background: transparent;
  position: relative;
  z-index: 5;
  transition: background 0.15s;
}
.resize-handle:hover,
.resize-handle:active {
  background: rgba(37, 99, 235, 0.25);
}

/* 预览区点击定位到表单 section 时的闪烁高亮 */
.edit-form-wrapper .section-highlight {
  animation: form-highlight 1.5s ease;
  border-radius: 8px;
}

@keyframes form-highlight {
  0%   { box-shadow: inset 0 0 0 0 rgba(37, 99, 235, 0.15); }
  50%  { box-shadow: inset 0 0 0 3px rgba(37, 99, 235, 0.12); }
  100% { box-shadow: inset 0 0 0 0 rgba(37, 99, 235, 0); }
}

/* ── 模板选择器 ── */
.template-selector {
  user-select: none;
}
.template-current {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #475569;
  border: 1px solid rgba(37, 99, 235, 0.1);
  background: rgba(255,255,255,0.6);
  transition: all 0.15s;
}
.template-current:hover {
  border-color: rgba(37, 99, 235, 0.25);
  background: #fff;
}
.chevron {
  font-size: 12px;
  transition: transform 0.2s;
}
.chevron.open {
  transform: rotate(180deg);
}
/* Teleported to body — no scoping issues */
.template-picker-dropdown {
  padding: 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.18);
  animation: picker-in 0.15s ease;
}
@keyframes picker-in {
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: translateY(0); }
}
.picker-arrow {
  position: absolute;
  top: -5px;
  left: 50%;
  margin-left: -5px;
  width: 10px;
  height: 10px;
  background: #fff;
  transform: rotate(45deg);
  box-shadow: -2px -2px 4px rgba(0,0,0,0.04);
}
.picker-cards {
  display: flex;
  gap: 8px;
}
.template-card {
  width: 100px;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e2e8f0;
  transition: all 0.2s;
  background: #fafafa;
}
.template-card:hover {
  border-color: #93c5fd;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.10);
}
.template-card.active {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}
.template-preview {
  padding: 8px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.template-preview svg {
  width: 80px;
  height: auto;
  display: block;
}
.template-card-label {
  text-align: center;
  font-size: 11px;
  color: #475569;
  padding: 6px 0;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  font-weight: 500;
}
.template-card.active .template-card-label {
  color: #2563eb;
  background: #eff6ff;
}

@media (max-width: 1200px) {
  .preview-wrapper {
    display: none;
  }
}

/* ── 导入遮罩 ── */
.import-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
  animation: overlayFadeIn 0.25s ease;
}

@keyframes overlayFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.import-overlay-inner {
  background: #fff;
  border-radius: 16px;
  padding: 48px 56px;
  text-align: center;
  min-width: 320px;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.3);
  animation: overlayContentIn 0.3s ease;
}

@keyframes overlayContentIn {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.import-spinner {
  margin-bottom: 16px;
}

.spinner-icon {
  animation: importSpin 1s linear infinite;
  color: #2563eb;
}

@keyframes importSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.import-step-text {
  font-size: 15px;
  color: #0f172a;
  margin: 0 0 16px;
  font-weight: 500;
}

.import-progress-bar {
  width: 200px;
  height: 4px;
  background: #dbeafe;
  border-radius: 2px;
  overflow: hidden;
  margin: 0 auto;
}

.import-progress-fill {
  height: 100%;
  width: 20%;
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  border-radius: 2px;
  transition: width 0.6s ease;
  animation: progressIndeterminate 1.5s ease-in-out infinite;
}

.import-progress-fill.wide {
  width: 60%;
}

.import-progress-fill.full {
  width: 100%;
  animation: none;
  background: linear-gradient(90deg, #10b981, #34d399);
}

@keyframes progressIndeterminate {
  0% { width: 15%; }
  50% { width: 55%; }
  100% { width: 15%; }
}

.import-error-icon {
  margin-bottom: 12px;
}

.import-error-text {
  font-size: 15px;
  color: #ef4444;
  margin: 0 0 20px;
  font-weight: 500;
}

.import-error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

</style>

<style>
/* el-dialog teleports to body, so these need :global / non-scoped */
/* AI 弹窗样式 */
.ai-dialog-loading {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.ai-dialog-loading p {
  margin-top: 12px;
  font-size: 14px;
}

	.clinic-section {
	  margin-bottom: 20px;
	}

	.clinic-section-title {
	  display: flex;
	  align-items: center;
	  gap: 8px;
	  font-size: 15px;
	  font-weight: 600;
	  color: #333;
	  margin: 0 0 12px;
	}

	.clinic-missing-list {
	  padding-left: 20px;
	  margin: 0;
	}

	.clinic-missing-list li {
	  font-size: 14px;
	  color: #e6a23c;
	  line-height: 1.8;
	  margin-bottom: 4px;
	}

	.clinic-gen-item {
	  background: #f8faff;
	  border: 1px solid #e8f0fe;
	  border-radius: 8px;
	  padding: 12px 16px;
	  margin-bottom: 12px;
	}

	.gen-label {
	  font-size: 13px;
	  font-weight: 600;
	  color: #409eff;
	  margin-bottom: 8px;
	}

	.gen-text {
	  font-size: 14px;
	  color: #555;
	  line-height: 1.7;
	  margin-bottom: 10px;
	}

	.gen-tags {
	  display: flex;
	  flex-wrap: wrap;
	  gap: 6px;
	  margin-bottom: 10px;
	}

	.clinic-tip-item {
	  display: flex;
	  align-items: flex-start;
	  gap: 12px;
	  padding: 12px 16px;
	  background: #f6ffed;
	  border: 1px solid #d9f7be;
	  border-radius: 8px;
	  margin-bottom: 10px;
	}

	.tip-index {
	  flex-shrink: 0;
	  width: 28px;
	  height: 28px;
	  border-radius: 50%;
	  background: #52c41a;
	  color: #fff;
	  display: flex;
	  align-items: center;
	  justify-content: center;
	  font-size: 13px;
	  font-weight: 600;
	}

	.tip-text {
	  flex: 1;
	  font-size: 14px;
	  color: #555;
	  line-height: 1.7;
	}

	.jd-hint {
	  font-size: 13px;
	  color: #666;
	  margin: 0 0 12px;
	  line-height: 1.6;
	}

	.jd-result-header {
	  display: flex;
	  align-items: center;
	  gap: 8px;
	  font-size: 16px;
	  font-weight: 600;
	  color: #67c23a;
	  margin-bottom: 12px;
	}

	.jd-summary {
	  font-size: 14px;
	  color: #555;
	  line-height: 1.8;
	  margin: 0 0 16px;
	  padding: 12px;
	  background: #f9f9f9;
	  border-radius: 6px;
	}

	/* ====== 打印样式 ====== */
	@page {
	  margin: 0;
	}
	@media print {
	  .resume-editor-fullscreen {
	    background: #fff !important;
	  }
	  .editor-toolbar,
	  .module-nav,
	  .edit-form-wrapper,
	  .style-panel,
	  .undo-panel,
	  .sidebar-toggle,
	  .el-overlay,
	  .el-dialog__wrapper {
	    display: none !important;
	  }
	  .editor-body {
	    overflow: visible !important;
	  }
	  .preview-wrapper,
	  .preview-wrapper.preview-full {
	    width: auto !important;
	    padding: 0 !important;
	    background: #fff !important;
	    overflow: visible !important;
	  }
	  .preview-pane {
	    display: block !important;
	    padding: 0 !important;
	    min-height: auto !important;
	  }
	  .a4-paper {
	    box-shadow: none !important;
	    width: auto !important;
	    min-height: auto !important;
	    padding: 15mm 18mm !important;
	    margin: 0 !important;
	  }
	  .sec-title,
	  h3.sec-title {
	    page-break-after: avoid;
	  }
	  .section,
	  .exp-block,
	  .edu-row,
	  .item-card {
	    page-break-inside: avoid;
	  }
	}
</style>
