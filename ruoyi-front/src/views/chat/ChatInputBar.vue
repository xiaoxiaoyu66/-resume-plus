<template>
  <div :class="['input-box', inputBoxClass]">
    <!-- 场景选择器 -->
    <div class="scene-selector">
      <button class="scene-trigger" @click="$emit('update:sceneMenuOpen', !sceneMenuOpen)" @blur="closeSceneMenu">
        <span class="scene-dot" :style="{ background: sceneColors[currentScene] }"></span>
        <span class="scene-label">{{ sceneOptions.find(s => s.value === currentScene)?.label }}</span>
        <span class="scene-arrow">▾</span>
      </button>
      <div v-if="sceneMenuOpen" class="scene-menu" @mousedown.prevent>
        <button
          v-for="s in sceneOptions"
          :key="s.value"
          :class="['scene-menu-item', { active: currentScene === s.value }]"
          @click="switchScene(s.value)"
        >
          <span class="scene-dot scene-dot-sm" :style="{ background: sceneColors[s.value] }"></span>
          {{ s.label }}
        </button>
      </div>
      <span v-if="hasResume" class="resume-pill" title="已关联简历">📄</span>
    </div>

    <!-- 已上传文件预览 -->
    <div v-if="uploadedFiles.length > 0" class="uploaded-files">
      <div v-for="(file, index) in uploadedFiles" :key="index" class="file-tag">
        <el-icon><Document /></el-icon>
        <span class="file-name">{{ file.originalName }}</span>
        <el-icon class="remove-btn" @click="$emit('remove-file', index)"><Close /></el-icon>
      </div>
    </div>

    <textarea
      ref="textareaRef"
      v-model="inputText"
      :placeholder="placeholder"
      :rows="rows"
      @keydown.enter.prevent="handleEnter"
    />

    <div class="input-actions">
      <div class="left-actions">
        <input
          ref="fileInputEl"
          type="file"
          style="display: none"
          accept=".pdf,.doc,.docx,.txt,.xls,.xlsx,.ppt,.pptx,.md"
          @change="onFileSelect"
        />
        <button
          class="action-btn action-btn-upload"
          title="添加附件"
          :disabled="isUploading"
          @click="fileInputEl?.click()"
        >
          <el-icon v-if="isUploading"><Loading /></el-icon>
          <el-icon v-else><Plus /></el-icon>
          <span class="upload-btn-text">附件</span>
        </button>
        <span v-if="uploadedFiles.length > 0" class="file-count">{{ uploadedFiles.length }} 个文件</span>
      </div>
      <div class="right-actions">
        <button v-if="isThinking" class="stop-btn" @click="$emit('cancel')">
          <svg class="stop-icon" viewBox="0 0 16 16" fill="currentColor" width="14" height="14"><rect x="2" y="2" width="12" height="12" rx="2"/></svg>
          <span>停止生成</span>
        </button>
        <button
          v-else
          class="send-btn"
          :disabled="!inputText.trim() && uploadedFiles.length === 0"
          @click="send"
        >
          <el-icon><Promotion /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { UploadedFile } from '@/types/chat'

const props = defineProps<{
  modelValue: string
  uploadedFiles: UploadedFile[]
  currentScene: string
  sceneMenuOpen: boolean
  isUploading: boolean
  isThinking: boolean
  hasResume?: boolean
  rows?: number
  placeholder?: string
  inputBoxClass?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:sceneMenuOpen': [value: boolean]
  send: []
  cancel: []
  'remove-file': [index: number]
  'switch-scene': [scene: string]
  'file-selected': [files: FileList]
}>()

const fileInputEl = ref<HTMLInputElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)

const inputText = computed({
  get: () => props.modelValue,
  set: (val: string) => emit('update:modelValue', val),
})

const sceneOptions = [
  { label: '综合', value: 'default' },
  { label: '简历诊断', value: 'resume' },
  { label: 'HR面试', value: 'interview-hr' },
  { label: '专业面试', value: 'interview-pro' },
  { label: '职业规划', value: 'career' },
]

const sceneColors: Record<string, string> = {
  default: '#3b82f6',
  resume: '#10b981',
  'interview-hr': '#8b5cf6',
  'interview-pro': '#f59e0b',
  career: '#ec4899',
}

function switchScene(scene: string) {
  emit('switch-scene', scene)
  emit('update:sceneMenuOpen', false)
}

function closeSceneMenu() {
  setTimeout(() => emit('update:sceneMenuOpen', false), 150)
}

function onFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    emit('file-selected', target.files)
    target.value = ''
  }
}

function handleEnter(e: KeyboardEvent) {
  if (!e.shiftKey) {
    emit('send')
  } else {
    emit('update:modelValue', inputText.value + '\n')
  }
}

function send() {
  emit('send')
}

defineExpose({ fileInputEl, textareaRef })
</script>

<style scoped lang="scss">
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #5a5a5a;
$paper-white: #ffffff;

.input-box {
  position: relative;
  background: $paper-white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.3s ease;

  &:focus-within {
    border-color: rgba(0, 0, 0, 0.15);
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
  }

  textarea {
    width: 100%;
    padding: 36px 24px 16px;
    border: none;
    outline: none;
    resize: none;
    font-size: 15px;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    color: $ink-black;
    background: transparent;
    &::placeholder { color: $ink-pale; }
  }
}

/* ---- 场景选择器 ---- */
.scene-selector {
  position: absolute;
  top: 8px;
  left: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 2;

  .scene-trigger {
    display: flex; align-items: center; gap: 3px;
    padding: 2px 8px;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 4px;
    background: transparent;
    color: $ink-pale;
    font-size: 12px;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
    .scene-arrow { font-size: 10px; transition: transform 0.2s ease; }
    &:hover { border-color: #3b82f6; color: #3b82f6; }
  }

  .scene-menu {
    position: absolute;
    top: 100%; left: 0;
    margin-top: 4px;
    background: $paper-white;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    padding: 4px;
    min-width: 130px;
    z-index: 10;

    .scene-menu-item {
      display: flex; align-items: center;
      width: 100%; padding: 6px 12px;
      border: none; border-radius: 4px;
      background: transparent;
      color: $ink-mid;
      font-size: 13px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      cursor: pointer;
      text-align: left;
      transition: all 0.15s ease;
      &:hover { background: rgba(59, 130, 246, 0.08); color: #3b82f6; }
      &.active { color: #3b82f6; font-weight: 600; }
    }
  }

  .resume-pill { font-size: 12px; line-height: 1; cursor: default; opacity: 0.7; }
  .scene-dot {
    width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0;
    &.scene-dot-sm { width: 8px; height: 8px; display: inline-block; vertical-align: middle; margin-right: 4px; }
  }
}

/* ---- 已上传文件预览 ---- */
.uploaded-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 20px 0 120px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  min-height: 32px;

  .file-tag {
    display: flex; align-items: center; gap: 6px;
    padding: 6px 12px;
    background: rgba(10, 10, 10, 0.05);
    border-radius: 8px;
    font-size: 13px;
    color: $ink-black;
    transition: all 0.2s ease;
    &:hover { background: rgba(10, 10, 10, 0.08); }
    .file-name { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .remove-btn { cursor: pointer; color: $ink-pale; font-size: 14px; transition: color 0.2s ease; &:hover { color: #f56c6c; } }
  }
}

/* ---- 操作按钮 ---- */
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);

  .left-actions {
    display: flex; align-items: center; gap: 12px;
    .file-count {
      font-size: 13px; color: $ink-pale;
      padding: 4px 10px;
      background: rgba(10, 10, 10, 0.04);
      border-radius: 6px;
    }
  }

  .right-actions { display: flex; align-items: center; gap: 16px; }

  .action-btn {
    display: flex; align-items: center; justify-content: center;
    width: 36px; height: 36px;
    border: 1px solid rgba(0, 0, 0, 0.08); border-radius: 8px;
    background: transparent; color: $ink-pale;
    cursor: pointer; transition: all 0.3s ease;
    &:hover {
      border-color: rgba(0, 0, 0, 0.15); color: $ink-mid;
      background: rgba(0, 0, 0, 0.02);
    }
    .el-icon { font-size: 18px; }
  }

  .action-btn-upload {
    gap: 4px; width: auto; padding: 0 12px;
    border-color: rgba(59, 130, 246, 0.2); color: #3b82f6;
    background: rgba(59, 130, 246, 0.04);
    &:hover {
      border-color: rgba(59, 130, 246, 0.5); color: #2563eb;
      background: rgba(59, 130, 246, 0.08);
    }
    .upload-btn-text { font-size: 12px; font-weight: 500; }
  }

  .send-btn {
    width: 40px; height: 40px; border-radius: 10px;
    border: none; background: $ink-black; color: $paper-white;
    display: flex; align-items: center; justify-content: center;
    cursor: pointer; transition: all 0.3s ease;
    &:hover:not(:disabled) { background: $ink-deep; transform: scale(1.05); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2); }
    &:disabled { opacity: 0.4; cursor: not-allowed; }
    .el-icon { font-size: 18px; }
  }

  .stop-btn {
    display: flex; align-items: center; gap: 6px;
    padding: 0 16px; height: 40px; border-radius: 10px;
    border: none; background: #ef4444; color: #fff;
    cursor: pointer; font-size: 13px; font-weight: 500; white-space: nowrap;
    transition: all 0.3s ease;
    &:hover { background: #dc2626; box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3); }
    .stop-icon { flex-shrink: 0; }
  }
}
</style>
