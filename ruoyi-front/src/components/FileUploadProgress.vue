<template>
  <div v-if="visible" class="upload-progress-overlay">
    <div class="upload-progress-panel">
      <div class="upload-header">
        <span class="upload-title">正在上传</span>
        <button class="close-btn" @click="handleCancel">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      
      <div class="upload-content">
        <div class="file-info">
          <el-icon class="file-icon"><Document /></el-icon>
          <div class="file-details">
            <div class="file-name">{{ fileName }}</div>
            <div class="file-size">{{ formatFileSize(fileSize) }}</div>
          </div>
        </div>
        
        <div class="progress-section">
          <div class="progress-bar-container">
            <div class="progress-bar" :style="{ width: progress + '%' }">
              <div class="progress-glow"></div>
            </div>
          </div>
          
          <div class="progress-info">
            <span class="progress-percent">{{ progress }}%</span>
            <span class="upload-speed">{{ speed }}</span>
          </div>
        </div>
        
        <div class="upload-stats">
          <span class="uploaded-size">{{ formatFileSize(uploadedSize) }} / {{ formatFileSize(fileSize) }}</span>
          <span v-if="remainingTime > 0" class="remaining-time">剩余 {{ formatTime(remainingTime) }}</span>
        </div>
      </div>
      
      <div class="upload-actions">
        <button class="cancel-btn" @click="handleCancel">
          <el-icon><CircleClose /></el-icon>
          取消上传
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Document, Close, CircleClose } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  fileName: {
    type: String,
    default: ''
  },
  fileSize: {
    type: Number,
    default: 0
  },
  progress: {
    type: Number,
    default: 0
  },
  speed: {
    type: String,
    default: '0 KB/s'
  },
  uploadedSize: {
    type: Number,
    default: 0
  },
  remainingTime: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['cancel'])

function handleCancel() {
  emit('cancel')
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatTime(seconds) {
  if (seconds < 60) {
    return seconds + '秒'
  } else if (seconds < 3600) {
    return Math.ceil(seconds / 60) + '分钟'
  } else {
    return Math.ceil(seconds / 3600) + '小时'
  }
}
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

.upload-progress-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.upload-progress-panel {
  background: $paper-white;
  border-radius: 16px;
  width: 480px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.upload-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  
  .upload-title {
    font-size: 16px;
    font-weight: 600;
    color: $ink-black;
    font-family: 'Noto Serif SC', 'STSong', serif;
  }
  
  .close-btn {
    width: 32px;
    height: 32px;
    border: none;
    background: transparent;
    color: $ink-pale;
    cursor: pointer;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    
    &:hover {
      background: rgba(0, 0, 0, 0.04);
      color: $ink-black;
    }
  }
}

.upload-content {
  padding: 24px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  
  .file-icon {
    width: 48px;
    height: 48px;
    background: rgba(10, 10, 10, 0.04);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: $ink-mid;
  }
  
  .file-details {
    flex: 1;
    min-width: 0;
    
    .file-name {
      font-size: 15px;
      font-weight: 500;
      color: $ink-black;
      margin-bottom: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .file-size {
      font-size: 13px;
      color: $ink-pale;
    }
  }
}

.progress-section {
  margin-bottom: 16px;
}

.progress-bar-container {
  height: 8px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, $ink-mid 0%, $ink-black 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
  position: relative;
  overflow: hidden;
  
  .progress-glow {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(255, 255, 255, 0.3) 50%,
      transparent 100%
    );
    animation: shimmer 1.5s infinite;
  }
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .progress-percent {
    font-size: 20px;
    font-weight: 700;
    color: $ink-black;
  }
  
  .upload-speed {
    font-size: 13px;
    color: $accent-red;
    font-weight: 500;
    padding: 4px 10px;
    background: rgba(139, 26, 26, 0.08);
    border-radius: 6px;
  }
}

.upload-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: $ink-pale;
  
  .remaining-time {
    color: $ink-mid;
  }
}

.upload-actions {
  padding: 16px 24px 24px;
  display: flex;
  justify-content: center;
}

.cancel-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background: transparent;
  color: $ink-mid;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    border-color: #f56c6c;
    color: #f56c6c;
    background: rgba(245, 108, 108, 0.04);
  }
}
</style>
