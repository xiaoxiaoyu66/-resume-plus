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
          <el-icon><Operation /></el-icon>
        </el-button>
        <span class="editor-title">简历编辑器</span>
      </div>
      <div class="toolbar-center">
        <el-select v-model="resumeStore.templateId" size="small" style="width: 120px" @change="onTemplateChange">
          <el-option label="现代模板" value="modern" />
          <el-option label="经典模板" value="classic" />
          <el-option label="极简模板" value="minimal" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <span v-if="resumeStore.saving" class="save-status">保存中...</span>
        <span v-else-if="resumeStore.lastSaved" class="save-status">已保存</span>
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
        <el-button type="primary" size="small" @click="handleExport">
          导出PDF
        </el-button>
      </div>
    </div>

    <!-- 可切换的侧边栏 -->
    <div class="editor-body">
      <!-- 左侧模块导航 -->
      <ModuleNav v-show="showSidebar" />

      <!-- 中间表单编辑区 -->
      <div v-show="showSidebar" class="edit-form-wrapper">
        <EditForm />
      </div>

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
        <PreviewPane v-show="previewTab === 'preview'" />
        <AIPanel
          v-show="previewTab === 'ai'"
          :clinic-result="clinicResult"
          :clinic-loading="clinicLoading"
          @re-diagnose="handleClinic"
        />
      </div>
    </div>

    <!-- 导出弹窗 -->
    <ExportModal v-model="showExportModal" />

    <!-- 样式面板 -->
    <StylePanel />

    <!-- 撤回面板 -->
    <UndoPanel />

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
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Upload, Document, Loading, WarningFilled, MagicStick, EditPen, CircleCheck, Operation, View, Promotion } from '@element-plus/icons-vue'
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
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

const goBack = () => {
  router.push('/resume')
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
        major: profile.major || '',
        degree: profile.education || '',
        start: '',
        end: profile.graduationYear || '',
        gpa: ''
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

  try {
    await ElMessageBox.confirm(
      '导入将覆盖当前编辑内容，确认继续？',
      '确认导入',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    target.value = ''
    return
  }

  importing.value = true
  try {
    const res = await parseResumeFile(file) as any
    const data = res.data || res
    if (data && data.baseInfo) {
      Object.assign(resumeStore.content, data)
      ElMessage.success('导入成功，请检查并补充信息')
    } else {
      ElMessage.error('解析结果异常，请重试')
    }
  } catch (err: any) {
    console.error('导入简历失败:', err)
    ElMessage.error('导入失败：' + (err.message || '未知错误'))
  } finally {
    importing.value = false
    target.value = ''
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
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.editor-toolbar {
  height: 50px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
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
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.sidebar-toggle {
  color: #999;
  transition: color 0.2s;
}
.sidebar-toggle:hover {
  color: #409eff;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.save-status {
  font-size: 12px;
  color: #999;
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
  padding: 20px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
}

.preview-wrapper {
  width: 595px;
  flex-shrink: 0;
  overflow-y: auto;
  padding: 20px;
  background: #e8e8e8;
  transition: width 0.25s ease;
}

.preview-wrapper.preview-full {
  flex: 1;
  width: auto;
}

.preview-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 12px;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
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
  color: #888;
  background: transparent;
  transition: all 0.2s;
}

.preview-tab:hover {
  color: #409eff;
  background: rgba(64, 158, 255, 0.06);
}

.preview-tab.active {
  color: #fff;
  background: #409eff;
}

@media (max-width: 1200px) {
  .preview-wrapper {
    display: none;
  }
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
