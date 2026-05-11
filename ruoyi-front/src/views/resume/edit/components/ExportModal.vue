<template>
  <el-dialog
    v-model="dialogVisible"
    title="导出简历"
    width="420px"
    align-center
    :close-on-click-modal="!exporting"
    :close-on-press-escape="!exporting"
  >
    <div class="export-body-wrap">
      <!-- 导出中全屏遮罩 -->
      <div v-if="exporting" class="export-full-overlay">
        <div class="export-full-inner">
          <el-icon class="loading-icon" color="#2563eb" :size="48"><Loading /></el-icon>
          <p class="export-full-text">正在生成 {{ formatLabel }} 文件...</p>
        </div>
      </div>

      <!-- 导出成功 -->
      <div v-else-if="exportSuccess" class="export-success">
        <el-icon class="success-icon" color="#67c23a" :size="48"><CircleCheck /></el-icon>
        <p class="success-text">{{ formatLabel }} 已生成并下载</p>
      </div>

      <!-- 导出失败 -->
      <div v-else-if="exportError" class="export-error">
        <el-icon class="error-icon" color="#f56c6c" :size="48"><Close /></el-icon>
        <p class="error-text">{{ exportError }}</p>
        <el-button size="small" @click="retryExport">重试</el-button>
      </div>

      <!-- 格式选择 -->
      <div v-else class="format-select">
        <div class="format-grid">
          <div class="format-card" @click="startExport('pdf')">
            <div class="format-icon">
              <el-icon :size="36"><Document /></el-icon>
            </div>
            <span class="format-label">PDF</span>
            <span class="format-desc">适合打印、投递</span>
          </div>
          <div class="format-card" @click="startExport('word')">
            <div class="format-icon">
              <el-icon :size="36"><Notebook /></el-icon>
            </div>
            <span class="format-label">Word</span>
            <span class="format-desc">适合编辑、修改</span>
          </div>
          <div class="format-card" @click="startExport('png')">
            <div class="format-icon">
              <el-icon :size="36"><Picture /></el-icon>
            </div>
            <span class="format-label">PNG</span>
            <span class="format-desc">适合预览、分享</span>
          </div>
        </div>
      </div>

    </div>

    <!-- 投递链接 -->
    <div v-if="exportSuccess" class="job-links">
      <p class="links-title">接下来，去这些地方投递：</p>
      <div class="links-grid">
        <el-button
          v-for="site in jobSites"
          :key="site.name"
          size="default"
          @click="openSite(site.url)"
        >
          {{ site.name }}
        </el-button>
      </div>
    </div>

    <template #footer>
      <el-button v-if="!exporting" @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { CircleCheck, Close, Loading, Document, Picture, Notebook } from '@element-plus/icons-vue'
import { exportResumePdf, exportResumeWord } from '@/api/resume'
import { useResumeStore } from '@/store/resume'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const resumeStore = useResumeStore()

const fileName = computed(() => {
  const c = resumeStore.content
  const name = c.baseInfo?.name || ''
  const school = (Array.isArray(c.education) && c.education[0]?.school) || ''
  const position = c.intention?.position || ''
  const parts = [name, school, position].filter(Boolean)
  return parts.length ? parts.join('-') : '简历'
})

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => {
    emit('update:modelValue', val)
    if (!val) {
      selectedFormat.value = null
      exportSuccess.value = false
      exportError.value = ''
    }
  }
})

const selectedFormat = ref<string | null>(null)
const exporting = ref(false)
const exportSuccess = ref(false)
const exportError = ref('')

const formatLabel = computed(() => {
  const labels: Record<string, string> = { pdf: 'PDF', word: 'Word', png: 'PNG' }
  return labels[selectedFormat.value || ''] || ''
})

const jobSites = [
  { name: 'BOSS直聘', url: 'https://www.zhipin.com' },
  { name: '拉勾网', url: 'https://www.lagou.com' },
  { name: '智联招聘', url: 'https://www.zhaopin.com' },
  { name: '前程无忧', url: 'https://www.51job.com' },
  { name: '猎聘网', url: 'https://www.liepin.com' },
  { name: '实习僧', url: 'https://www.shixiseng.com' }
]

const openSite = (url: string) => {
  window.open(url, '_blank')
}

const retryExport = () => {
  exportError.value = ''
  if (selectedFormat.value === 'pdf') {
    generatePDF()
  } else if (selectedFormat.value === 'word') {
    generateWord()
  } else {
    generatePNG()
  }
}

function startExport(format: string) {
  selectedFormat.value = format
  if (format === 'pdf') {
    generatePDF()
  } else if (format === 'word') {
    generateWord()
  } else {
    generatePNG()
  }
}

/** 从预览 DOM 中采集 HTML + CSS，PDF 和 Word 共用 */
function collectExportHtml(): string {
  const preview = document.getElementById('resume-preview')
  if (!preview) throw new Error('未找到简历预览区域')

  const clone = preview.cloneNode(true) as HTMLElement
  clone.querySelectorAll('.editable-text-overlay, .editable-overlay').forEach(el => el.remove())

  const styles = Array.from(document.querySelectorAll('style'))
    .map(s => s.innerHTML)
    .join('\n')

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<style>
${styles}
.a4-paper {
  box-shadow: none !important;
  padding: 15mm 18mm !important;
  width: auto !important;
  min-height: auto !important;
}
.preview-pane {
  display: block !important;
  min-height: auto !important;
}
[contenteditable] {
  cursor: default !important;
  outline: none !important;
  border: none !important;
}
</style>
</head>
<body>
${clone.outerHTML}
</body>
</html>`
}

const generatePDF = async () => {
  exporting.value = true
  exportError.value = ''
  exportSuccess.value = false

  try {
    // 优先走 Gotenberg 服务端渲染
    try {
      const html = collectExportHtml()
      const blob = await exportResumePdf({ html }) as any
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName.value + '.pdf'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      setTimeout(() => URL.revokeObjectURL(url), 3000)
      exportSuccess.value = true
      return
    } catch (e: any) {
      console.warn('服务端PDF导出失败，尝试前端兜底:', e.message)
    }

    // 兜底：html2canvas + jsPDF 前端渲染
    const preview = document.getElementById('resume-preview')
    if (!preview) throw new Error('未找到简历预览区域')

    const { default: html2canvas } = await import('html2canvas')
    const { default: jsPDF } = await import('jspdf')

    // 先用较低 scale 试一次，捕获到 canvas 尺寸异常时降级
    const canvas = await html2canvas(preview, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false
    })

    if (!canvas.width || !canvas.height) {
      throw new Error('预览区域尺寸无效，请确保简历模板已渲染')
    }

    const imgData = canvas.toDataURL('image/jpeg', 0.95)
    const pdf = new jsPDF('p', 'mm', 'a4')
    const pdfWidth = pdf.internal.pageSize.getWidth()
    const pdfHeight = (canvas.height * pdfWidth) / canvas.width
    if (!isFinite(pdfHeight) || pdfHeight <= 0) {
      throw new Error('计算 PDF 尺寸失败，请调整简历内容后重试')
    }
    pdf.addImage(imgData, 'JPEG', 0, 0, pdfWidth, pdfHeight)
    pdf.save(fileName.value + '.pdf')

    exportSuccess.value = true
  } catch (e: any) {
    console.error('PDF导出失败:', e)
    exportError.value = 'PDF 生成失败: ' + (e.message || '未知错误')
  } finally {
    exporting.value = false
  }
}

const generateWord = async () => {
  exporting.value = true
  exportError.value = ''
  exportSuccess.value = false

  try {
    // 把结构化简历数据发给后端，用 POI 生成带格式的 docx
    const blob = await exportResumeWord({ content: resumeStore.content }) as any

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName.value + '.docx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(url), 3000)

    exportSuccess.value = true
  } catch (e: any) {
    console.error('Word导出失败:', e)
    exportError.value = 'Word 生成失败: ' + (e.message || '未知错误')
  } finally {
    exporting.value = false
  }
}

const generatePNG = async () => {
  exporting.value = true
  exportError.value = ''
  exportSuccess.value = false

  try {
    const { default: html2canvas } = await import('html2canvas')

    const preview = document.getElementById('resume-preview')
    if (!preview) throw new Error('未找到简历预览区域')

    const canvas = await html2canvas(preview, {
      scale: 3,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false
    })

    const link = document.createElement('a')
    link.download = fileName.value + '.png'
    link.href = canvas.toDataURL('image/png')
    link.click()
    exportSuccess.value = true
  } catch (e: any) {
    console.error('PNG导出失败:', e)
    exportError.value = 'PNG 生成失败，请尝试截图保存'
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.format-select {
  padding: 8px 0;
}

.format-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.format-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 12px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fafafa;
}

.format-card:hover {
  border-color: #0a0a0a;
  background: #f5f5f5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.format-card .format-icon {
  color: #1f1f1f;
}

.format-card .format-label {
  font-size: 15px;
  font-weight: 600;
  color: #0a0a0a;
}

.format-card .format-desc {
  font-size: 11px;
  color: #909399;
}

.export-success,
.export-error {
  text-align: center;
  padding: 16px 0;
}

/* 导出内容容器 */
.export-body-wrap {
  position: relative;
  min-height: 120px;
}

/* 导出全屏遮罩 */
.export-full-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  animation: overlayFade 0.2s ease;
}
@keyframes overlayFade {
  from { opacity: 0; }
  to { opacity: 1; }
}
.export-full-inner {
  text-align: center;
}
.export-full-text {
  margin-top: 16px;
  font-size: 15px;
  color: #0f172a;
  font-weight: 500;
}

.loading-icon {
  animation: rotating 1.5s linear infinite;
  margin-bottom: 12px;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.success-icon,
.error-icon {
  margin-bottom: 12px;
}

.exporting-text,
.success-text,
.error-text {
  font-size: 15px;
  color: #333;
  margin: 0;
}

.error-text {
  color: #f56c6c;
  font-size: 13px;
  margin-bottom: 12px;
}

.job-links {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.links-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
</style>
