<template>
  <div class="personal-space" :class="{ open: isOpen }">
    <!-- 遮罩层 -->
    <div v-if="isOpen" class="overlay" @click="close"></div>
    
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="header-title">
          <el-icon><User /></el-icon>
          <span>个人空间</span>
        </div>
        <button class="close-btn" @click="close">
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
      
      <!-- 标签页 -->
      <div class="tabs">
        <div 
          v-for="tab in tabs" 
          :key="tab.key"
          :class="['tab-item', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          <el-icon>
            <component :is="tab.icon" />
          </el-icon>
          <span>{{ tab.label }}</span>
        </div>
      </div>
      
      <!-- 文件列表区域 -->
      <div v-if="activeTab === 'files'" class="files-section">
        <!-- 筛选栏 -->
        <div class="filter-bar">
          <div class="search-box">
            <el-icon><Search /></el-icon>
            <input 
              v-model="searchKeyword"
              type="text"
              placeholder="搜索文件..."
              @input="handleSearch"
            />
          </div>
          
          <div class="time-filter">
            <select v-model="timeRange" @change="handleTimeFilterChange">
              <option value="">全部时间</option>
              <option value="1hour">最近1小时</option>
              <option value="today">今天</option>
              <option value="week">本周</option>
              <option value="month">本月</option>
              <option value="custom">自定义</option>
            </select>
          </div>
          
          <!-- 自定义时间范围 -->
          <div v-if="timeRange === 'custom'" class="custom-time-range">
            <el-date-picker
              v-model="startTime"
              type="datetime"
              placeholder="开始时间"
              size="small"
              value-format="YYYY-MM-DDTHH:mm:ss"
              @change="handleTimeFilterChange"
            />
            <span class="time-separator">至</span>
            <el-date-picker
              v-model="endTime"
              type="datetime"
              placeholder="结束时间"
              size="small"
              value-format="YYYY-MM-DDTHH:mm:ss"
              @change="handleTimeFilterChange"
            />
          </div>
        </div>
        
        <!-- 文件列表 -->
        <div class="file-list" v-loading="loading">
          <div v-if="fileList.length === 0" class="empty-state">
            <el-icon><FolderOpened /></el-icon>
            <p>暂无文件</p>
          </div>
          
          <div 
            v-for="file in fileList" 
            :key="file.fileName"
            class="file-item"
          >
            <div class="file-icon-wrapper">
              <el-icon class="file-icon"><Document /></el-icon>
            </div>
            
            <div class="file-info">
              <div class="file-name" :title="file.originalName || getFileName(file.fileName)">
                {{ file.originalName || getFileName(file.fileName) }}
              </div>
              <div class="file-meta">
                <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
                <span class="file-time">{{ formatTime(file.uploadTime) }}</span>
              </div>
            </div>
            
            <div class="file-actions">
              <button class="action-btn" title="下载" @click="downloadFile(file)">
                <el-icon><Download /></el-icon>
              </button>
              <button class="action-btn delete" title="删除" @click="deleteFileItem(file)">
                <el-icon><Delete /></el-icon>
              </button>
            </div>
          </div>
        </div>
        
        <!-- 分页 -->
        <div v-if="total > 0" class="pagination">
          <button 
            class="page-btn" 
            :disabled="pageNum <= 1"
            @click="changePage(pageNum - 1)"
          >
            <el-icon><ArrowLeft /></el-icon>
          </button>
          
          <span class="page-info">{{ pageNum }} / {{ totalPages }}</span>
          
          <button 
            class="page-btn" 
            :disabled="pageNum >= totalPages"
            @click="changePage(pageNum + 1)"
          >
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
      
      <!-- 快捷操作 -->
      <div v-else-if="activeTab === 'session'" class="files-section">
        <div class="session-info">
          <p>从下方选择文件添加到当前对话</p>
        </div>
        
        <div class="file-list" v-loading="loading">
          <div v-if="fileList.length === 0" class="empty-state">
            <el-icon><Document /></el-icon>
            <p>暂无文件，请先上传</p>
          </div>
          
          <div 
            v-for="file in fileList.slice(0, 5)" 
            :key="file.fileName"
            class="file-item"
          >
            <div class="file-icon-wrapper">
              <el-icon class="file-icon"><Document /></el-icon>
            </div>
            
            <div class="file-info">
              <div class="file-name" :title="file.originalName || getFileName(file.fileName)">
                {{ file.originalName || getFileName(file.fileName) }}
              </div>
              <div class="file-meta">
                <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
              </div>
            </div>
            
            <div class="file-actions">
              <button class="action-btn" title="使用此文件" @click="useFile(file)">
                <el-icon><Plus /></el-icon>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { 
  User, ArrowRight, Document, Search, FolderOpened, 
  Download, Delete, ArrowLeft, Plus 
} from '@element-plus/icons-vue'
import { listFiles, deleteFile, getFileUrl } from '@/api/file'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'use-file'])

// 标签页
const tabs = [
  { key: 'files', label: '全部文件', icon: 'FolderOpened' },
  { key: 'session', label: '快捷使用', icon: 'Plus' }
]
const activeTab = ref('files')

// 文件列表
const loading = ref(false)
const fileList = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const timeRange = ref('')
const startTime = ref('')
const endTime = ref('')



const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 监听侧边栏打开状态
watch(() => props.isOpen, (val) => {
  if (val) {
    loadFileList()
  }
})

function close() {
  emit('close')
}

// 加载文件列表
async function loadFileList() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      timeRange: timeRange.value,
      keyword: searchKeyword.value
    }
    
    // 如果是自定义时间范围，传递起止时间
    if (timeRange.value === 'custom' && startTime.value && endTime.value) {
      params.startTime = startTime.value
      params.endTime = endTime.value
    }
    
    const res = await listFiles(params)
    if (res.code === 200) {
      const data = res.data as Record<string, any> | undefined
      fileList.value = data?.list || []
      total.value = data?.total || 0
    }
  } catch (e) {
    console.error('加载文件列表失败', e)
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}



// 搜索
function handleSearch() {
  pageNum.value = 1
  loadFileList()
}

// 时间筛选
function handleTimeFilterChange() {
  pageNum.value = 1
  loadFileList()
}

// 分页
function changePage(page) {
  pageNum.value = page
  loadFileList()
}

// 删除文件
async function deleteFileItem(file) {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.originalName || getFileName(file.fileName)}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await deleteFile(file.fileName)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadFileList()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除文件失败', e)
      ElMessage.error('删除失败')
    }
  }
}

// 下载文件
async function downloadFile(file) {
  try {
    const res = await getFileUrl(file.fileName)
    if (res.code === 200 && res.data) {
      window.open(res.data as string, '_blank')
    } else {
      ElMessage.error('获取文件链接失败')
    }
  } catch (e) {
    console.error('下载文件失败', e)
    ElMessage.error('下载失败')
  }
}

// 使用文件
function useFile(file) {
  emit('use-file', file)
  close()
}

// 工具函数
function getFileName(filePath) {
  if (!filePath) return ''
  const parts = filePath.split('/')
  return parts[parts.length - 1]
}

function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatTime(time: string | number | Date) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = Number(now) - Number(date)
  
  // 小于1小时显示相对时间
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000)
    if (minutes < 1) return '刚刚'
    return minutes + '分钟前'
  }
  
  // 今天
  if (date.toDateString() === now.toDateString()) {
    return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  // 昨天
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  // 其他日期
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  if (props.isOpen) {
    loadFileList()
  }
})
</script>

<style scoped lang="scss">
// 水墨主题色
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #5a5a5a;
$paper-white: #ffffff;
$paper-cream: #f8f8f6;
$paper-gray: #f5f5f5;
$accent-red: #8b1a1a;

.personal-space {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1500;
  
  &.open {
    pointer-events: auto;
  }
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(2px);
}

.sidebar {
  position: absolute;
  top: 0;
  left: 0;
  width: 380px;
  height: 100%;
  background: $paper-white;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  transform: translateX(-100%);
  transition: transform 0.3s ease;
  
  .personal-space.open & {
    transform: translateX(0);
  }
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  
  .header-title {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 18px;
    font-weight: 600;
    color: $ink-black;
    font-family: 'Noto Serif SC', 'STSong', serif;
    
    .el-icon {
      font-size: 24px;
      color: $accent-red;
    }
  }
  
  .close-btn {
    width: 36px;
    height: 36px;
    border: none;
    background: rgba(0, 0, 0, 0.04);
    color: $ink-pale;
    cursor: pointer;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    
    &:hover {
      background: rgba(0, 0, 0, 0.08);
      color: $ink-black;
    }
  }
}

.tabs {
  display: flex;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  
  .tab-item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px;
    font-size: 14px;
    color: $ink-pale;
    cursor: pointer;
    transition: all 0.2s ease;
    border-bottom: 2px solid transparent;
    margin-bottom: -1px;
    
    &:hover {
      color: $ink-mid;
      background: rgba(0, 0, 0, 0.02);
    }
    
    &.active {
      color: $ink-black;
      border-bottom-color: $ink-black;
      font-weight: 500;
    }
  }
}

.files-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.filter-bar {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  gap: 12px;
  
  .search-box {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: rgba(0, 0, 0, 0.03);
    border-radius: 8px;
    border: 1px solid transparent;
    transition: all 0.2s ease;
    
    &:focus-within {
      border-color: rgba(0, 0, 0, 0.1);
      background: $paper-white;
    }
    
    .el-icon {
      color: $ink-pale;
      font-size: 16px;
    }
    
    input {
      flex: 1;
      border: none;
      background: transparent;
      font-size: 14px;
      color: $ink-black;
      outline: none;
      
      &::placeholder {
        color: $ink-pale;
      }
    }
  }
  
  .time-filter {
    select {
      padding: 10px 14px;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      background: $paper-white;
      font-size: 14px;
      color: $ink-mid;
      cursor: pointer;
      outline: none;
      
      &:focus {
        border-color: rgba(0, 0, 0, 0.15);
      }
    }
  }
  
  .custom-time-range {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .time-separator {
      color: $ink-pale;
      font-size: 14px;
    }
    
    :deep(.el-date-picker) {
      width: 180px;
    }
  }
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: $ink-pale;
  
  .el-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
  }
  
  p {
    font-size: 14px;
  }
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
  
  &:hover {
    background: rgba(0, 0, 0, 0.03);
    
    .file-actions {
      opacity: 1;
    }
  }
  
  .file-icon-wrapper {
    width: 40px;
    height: 40px;
    background: rgba(10, 10, 10, 0.05);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    
    .file-icon {
      font-size: 20px;
      color: $ink-mid;
    }
  }
  
  .file-info {
    flex: 1;
    min-width: 0;
    
    .file-name {
      font-size: 14px;
      color: $ink-black;
      margin-bottom: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .file-meta {
      display: flex;
      gap: 12px;
      font-size: 12px;
      color: $ink-pale;
    }
  }
  
  .file-actions {
    display: flex;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.2s ease;
    
    .action-btn {
      width: 32px;
      height: 32px;
      border: none;
      background: transparent;
      color: $ink-pale;
      cursor: pointer;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s ease;
      
      &:hover {
        background: rgba(0, 0, 0, 0.06);
        color: $ink-mid;
      }
      
      &.delete:hover {
        background: rgba(245, 108, 108, 0.1);
        color: #f56c6c;
      }
    }
  }
}

.session-info {
  padding: 12px 20px;
  background: rgba(139, 26, 26, 0.04);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  
  p {
    font-size: 13px;
    color: $accent-red;
    margin: 0;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  
  .page-btn {
    width: 32px;
    height: 32px;
    border: 1px solid rgba(0, 0, 0, 0.08);
    background: $paper-white;
    color: $ink-mid;
    cursor: pointer;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    
    &:hover:not(:disabled) {
      border-color: rgba(0, 0, 0, 0.15);
      color: $ink-black;
    }
    
    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }
  
  .page-info {
    font-size: 14px;
    color: $ink-mid;
  }
}
</style>
