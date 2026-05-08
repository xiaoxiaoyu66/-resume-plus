<template>
  <el-dialog
    v-model="dialogVisible"
    title="导出简历"
    width="420px"
    align-center
    :close-on-click-modal="!exporting"
    :close-on-press-escape="!exporting"
  >
    <!-- 格式选择 -->
    <div v-if="!selectedFormat && !exporting && !exportSuccess" class="format-select">
      <div class="format-grid">
        <div class="format-card pdf-card" @click="startExport('pdf')">
          <div class="format-icon">
            <el-icon :size="36"><Document /></el-icon>
          </div>
          <span class="format-label">PDF</span>
          <span class="format-desc">适合打印、投递</span>
        </div>
        <div class="format-card png-card" @click="startExport('png')">
          <div class="format-icon">
            <el-icon :size="36"><Picture /></el-icon>
          </div>
          <span class="format-label">PNG</span>
          <span class="format-desc">适合预览、分享</span>
        </div>
      </div>
    </div>

    <!-- 导出中 -->
    <div v-if="exporting" class="exporting-state">
      <el-icon class="loading-icon" color="#409eff" :size="48"><Loading /></el-icon>
      <p class="exporting-text">正在生成{{ selectedFormat === 'pdf' ? ' PDF' : ' PNG' }} 文件...</p>
    </div>

    <!-- 导出成功 -->
    <div v-else-if="exportSuccess" class="export-success">
      <el-icon class="success-icon" color="#67c23a" :size="48"><CircleCheck /></el-icon>
      <p class="success-text">{{ selectedFormat === 'pdf' ? 'PDF' : 'PNG' }} 已生成并下载</p>
    </div>

    <!-- 导出失败 -->
    <div v-else-if="exportError" class="export-error">
      <el-icon class="error-icon" color="#f56c6c" :size="48"><Close /></el-icon>
      <p class="error-text">{{ exportError }}</p>
      <el-button size="small" @click="retryExport">重试</el-button>
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
import { CircleCheck, Close, Loading, Document, Picture } from '@element-plus/icons-vue'
import { exportResumePdf } from '@/api/resume'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

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
  } else {
    generatePNG()
  }
}

function startExport(format: string) {
  selectedFormat.value = format
  if (format === 'pdf') {
    generatePDF()
  } else {
    generatePNG()
  }
}

const generatePDF = async () => {
  exporting.value = true
  exportError.value = ''
  exportSuccess.value = false

  try {
    const preview = document.getElementById('resume-preview')
    if (!preview) throw new Error('未找到简历预览区域')

    const clone = preview.cloneNode(true) as HTMLElement
    clone.querySelectorAll('.editable-text-overlay, .editable-overlay').forEach(el => el.remove())

    const styles = Array.from(document.querySelectorAll('style'))
      .map(s => s.innerHTML)
      .join('\n')

    const html = `<!DOCTYPE html>
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

    const blob = await exportResumePdf({ html }) as any

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '简历.pdf'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(url), 3000)

    exportSuccess.value = true
  } catch (e: any) {
    console.error('PDF导出失败:', e)
    exportError.value = 'PDF 生成失败: ' + (e.message || '未知错误')
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
    link.download = '简历.png'
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
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.format-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 28px 20px;
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
  font-size: 16px;
  font-weight: 600;
  color: #0a0a0a;
}

.format-card .format-desc {
  font-size: 12px;
  color: #909399;
}

.exporting-state,
.export-success,
.export-error {
  text-align: center;
  padding: 16px 0;
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
