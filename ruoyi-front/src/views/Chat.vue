<template>
  <div class="chat-page">
    <!-- 文件上传进度条 -->
    <FileUploadProgress
      :visible="showUploadProgress"
      :file-name="uploadingFileName"
      :file-size="uploadingFileSize"
      :progress="uploadProgress"
      :speed="uploadSpeed"
      :uploaded-size="uploadedSize"
      :remaining-time="remainingTime"
      @cancel="cancelUpload"
    />

    <!-- 侧边栏 + 主区域 -->
    <div class="chat-layout">
      <!-- 侧边栏 -->
      <aside class="chat-sidebar" :class="{ collapsed: !showSidebar }">
        <div class="sidebar-body">
          <!-- 加载中 -->
          <div v-if="sessionsLoading" class="sidebar-loading">
            <div class="sidebar-skeleton" v-for="n in 4" :key="n">
              <div class="skeleton-line w-60"></div>
              <div class="skeleton-line w-30"></div>
            </div>
          </div>

          <!-- 空列表 -->
          <div v-else-if="sessions.length === 0" class="sidebar-empty">
            <p>暂无历史对话</p>
          </div>

          <!-- 会话列表 -->
          <div v-else class="session-list">
            <button
              v-for="s in sessions"
              :key="s.id"
              :class="['session-item', { active: activeSessionId === s.id }]"
              @click="selectSession(s.id)"
            >
              <div class="session-info">
                <span class="session-title">{{ s.sessionTitle || '新对话' }}</span>
                <span class="session-meta">
                  <span class="session-scene">{{ sceneLabel(s.scene) }}</span>
                  <span class="session-dot">·</span>
                  <span class="session-time">{{ formatSessionTime(s.updateTime) }}</span>
                </span>
              </div>
              <button
                class="session-del"
                title="删除"
                @click.stop="deleteSession(s.id)"
              >
                <el-icon><Close /></el-icon>
              </button>
            </button>
          </div>
        </div>

        <!-- 收起按钮 -->
      </aside>

      <button class="sidebar-toggle" @click="showSidebar = !showSidebar">
        <span class="toggle-arrow">{{ showSidebar ? '◀' : '▶' }}</span>
      </button>

      <!-- 主区域 -->
      <main class="chat-main">

      <div v-if="loadingHistory" class="loading-history">
        <el-icon class="loading-spin"><Loading /></el-icon>
        <span>加载对话历史...</span>
      </div>
      <div v-else-if="!hasStarted" class="empty-state">
      <!-- 简历+ Logo -->
      <div class="logo-lishizhen">
        <span class="logo-char" :class="{ 'typing': showChar1 }">简</span>
        <span class="logo-char" :class="{ 'typing': showChar2 }">历</span>
        <span class="logo-char logo-char-last" :class="{ 'typing': showChar3 }">+</span>
      </div>

      <div class="input-wrapper">
        <div class="input-box input-box-empty">
          <!-- 场景选择器 -->
          <div class="scene-selector">
            <button class="scene-trigger" @click="sceneMenuOpen = !sceneMenuOpen" @blur="closeSceneMenu">
              <span class="scene-label">{{ sceneOptions.find(s => s.value === currentScene)?.label }}</span>
              <span class="scene-arrow">▾</span>
            </button>
            <div v-if="sceneMenuOpen" class="scene-menu" @mousedown.prevent>
              <button
                v-for="s in sceneOptions"
                :key="s.value"
                :class="['scene-menu-item', { active: currentScene === s.value }]"
                @click="switchScene(s.value); sceneMenuOpen = false"
              >{{ s.label }}</button>
            </div>
            <span v-if="hasResume" class="resume-pill" title="已关联简历">📄</span>
          </div>

          <!-- 已上传文件预览 -->
          <div v-if="uploadedFiles.length > 0" class="uploaded-files">
            <div v-for="(file, index) in uploadedFiles" :key="index" class="file-tag">
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ file.originalName }}</span>
              <el-icon class="remove-btn" @click="removeFile(index)"><Close /></el-icon>
            </div>
          </div>

          <textarea
            v-model="userInput"
            :placeholder="uploadedFiles.length > 0 ? '输入问题，让简历+分析文件内容...' : '输入消息与简历+对话...'"
            rows="3"
            @keydown.enter.prevent="handleEnter"
          />
          <div class="input-actions">
            <div class="left-actions">
              <!-- 文件上传按钮 -->
              <input
                ref="fileInput"
                type="file"
                style="display: none"
                accept=".pdf,.doc,.docx,.txt,.xls,.xlsx,.ppt,.pptx,.md"
                @change="handleFileSelect"
              />
              <button 
                class="action-btn" 
                title="添加附件"
                :disabled="isUploading"
                @click="triggerFileUpload"
              >
                <el-icon v-if="isUploading"><Loading /></el-icon>
                <el-icon v-else><Plus /></el-icon>
              </button>
              <span v-if="uploadedFiles.length > 0" class="file-count">{{ uploadedFiles.length }} 个文件</span>
            </div>
            <div class="right-actions">
              <button 
                class="send-btn" 
                :title="isThinking ? '取消回答' : '发送消息'"
                :disabled="(!userInput.trim() && uploadedFiles.length === 0) && !isThinking"
                @click="sendMessage"
              >
                <el-icon v-if="isThinking"><Close /></el-icon>
                <el-icon v-else><Promotion /></el-icon>
              </button>
            </div>
          </div>
        </div>
      </div>
      </div>
      <div v-else class="chat-container">
      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef">
        <!-- 历史对话无消息时的提示 -->
        <div v-if="messages.length === 0" class="history-empty-hint">
          <p>暂无对话记录，发送消息开始对话</p>
        </div>
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message-item', msg.role]"
        >
          <div class="message-avatar">
            <div v-if="msg.role === 'ai'" class="avatar ai-avatar lishizhen-avatar">
              <span class="avatar-char">历</span>
            </div>
            <div v-else class="avatar user-avatar">
              <el-icon><UserFilled /></el-icon>
            </div>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <template v-if="msg.role === 'ai'">
                <MarkdownRenderer :content="msg.content" />
              </template>
              <template v-else>
                {{ msg.content }}
              </template>
            </div>
            <span class="message-time">{{ msg.time }}</span>
          </div>
        </div>
        
        <!-- 思考中 -->
        <div v-if="isThinking" class="message-item ai thinking">
          <div class="message-avatar">
            <div class="avatar ai-avatar">
              <span class="avatar-char">历</span>
            </div>
          </div>
          <div class="message-content">
            <div class="thinking-bubble">
              <div class="thinking-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <span class="thinking-text">简历+ 思考中...</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入框 -->
      <div class="bottom-input">
        <div class="input-box">
          <!-- 场景选择器 -->
          <div class="scene-selector">
            <button class="scene-trigger" @click="sceneMenuOpen = !sceneMenuOpen" @blur="closeSceneMenu">
              <span class="scene-label">{{ sceneOptions.find(s => s.value === currentScene)?.label }}</span>
              <span class="scene-arrow">▾</span>
            </button>
            <div v-if="sceneMenuOpen" class="scene-menu" @mousedown.prevent>
              <button
                v-for="s in sceneOptions"
                :key="s.value"
                :class="['scene-menu-item', { active: currentScene === s.value }]"
                @click="switchScene(s.value); sceneMenuOpen = false"
              >{{ s.label }}</button>
            </div>
            <span v-if="hasResume" class="resume-pill" title="已关联简历">📄</span>
          </div>

          <!-- 已上传文件预览 -->
          <div v-if="uploadedFiles.length > 0" class="uploaded-files">
            <div v-for="(file, index) in uploadedFiles" :key="index" class="file-tag">
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ file.originalName }}</span>
              <el-icon class="remove-btn" @click="removeFile(index)"><Close /></el-icon>
            </div>
          </div>

          <textarea
            v-model="userInput"
            :placeholder="uploadedFiles.length > 0 ? '输入问题，让简历+分析文件内容...' : '输入消息...'"
            rows="2"
            @keydown.enter.prevent="handleEnter"
          />
          <div class="input-actions">
            <div class="left-actions">
              <!-- 文件上传按钮 -->
              <input
                ref="fileInputBottom"
                type="file"
                style="display: none"
                accept=".pdf,.doc,.docx,.txt,.xls,.xlsx,.ppt,.pptx,.md"
                @change="handleFileSelect"
              />
              <button 
                class="action-btn" 
                title="添加附件"
                :disabled="isUploading"
                @click="triggerFileUploadBottom"
              >
                <el-icon v-if="isUploading"><Loading /></el-icon>
                <el-icon v-else><Plus /></el-icon>
              </button>
              <span v-if="uploadedFiles.length > 0" class="file-count">{{ uploadedFiles.length }} 个文件</span>
            </div>
            <button 
              class="send-btn" 
              :title="isThinking ? '取消回答' : '发送消息'"
              :disabled="(!userInput.trim() && uploadedFiles.length === 0) && !isThinking"
              @click="sendMessage"
            >
              <el-icon v-if="isThinking"><Close /></el-icon>
              <el-icon v-else><Promotion /></el-icon>
            </button>
          </div>
        </div>
      </div>
      </div>
      </main>
    </div>
</div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import FileUploadProgress from '@/components/FileUploadProgress.vue'
import { useChatUpload } from '@/composables/useChatUpload'
import { useChatSse } from '@/composables/useChatSse'
import { useChatSession } from '@/composables/useChatSession'
import { escapeHtml, getCurrentTime } from '@/utils/format'
import { listHistory, delSession, updateSessionScene } from '@/api/chat'
import type { ChatMessage, ChatSessionItem, UploadedFile } from '@/types/chat'

const route = useRoute()
const router = useRouter()
const userInput = ref('')
const messages = ref<ChatMessage[]>([])
const isThinking = ref(false)
const hasStarted = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const currentSessionId = ref<string | null>(null)
const hasResume = ref(false)
const sceneMenuOpen = ref(false)

function closeSceneMenu() {
  setTimeout(() => { sceneMenuOpen.value = false }, 150)
}

const sceneOptions = [
  { label: '💬 综合', value: 'default' },
  { label: '📄 简历诊断', value: 'resume' },
  { label: '👥 HR面试', value: 'interview-hr' },
  { label: '💼 专业面试', value: 'interview-pro' },
  { label: '🧭 职业规划', value: 'career' },
]
const currentScene = ref('default')

function switchScene(scene: string) {
  currentScene.value = scene
  if (currentSessionId.value) {
    updateSessionScene(currentSessionId.value, scene).catch(() => {})
  }
}

const fileInput = ref<HTMLInputElement | null>(null)
const fileInputBottom = ref<HTMLInputElement | null>(null)
const uploadedFiles = ref<UploadedFile[]>([])
const isUploading = ref(false)

const showChar1 = ref(false)
const showChar2 = ref(false)
const showChar3 = ref(false)

function startLogoAnimation() {
  showChar1.value = true
  setTimeout(() => { showChar2.value = true }, 300)
  setTimeout(() => { showChar3.value = true }, 600)
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}
// 在内容可能还在异步渲染时多试一次
function scrollToBottomDelayed() {
  scrollToBottom()
  setTimeout(() => scrollToBottom(), 100)
}

const {
  showUploadProgress,
  uploadingFileName,
  uploadingFileSize,
  uploadProgress,
  uploadSpeed,
  uploadedSize,
  remainingTime,
  handleFileFromSpace,
  triggerFileUpload,
  triggerFileUploadBottom,
  handleFileSelect,
  cancelUpload,
  removeFile
} = useChatUpload({
  hasStarted,
  uploadedFiles,
  fileInput,
  fileInputBottom,
  isUploading
})

const { sendMessage, stopSse } = useChatSse({
  userInput,
  messages,
  isThinking,
  hasStarted,
  uploadedFiles,
  currentSessionId,
  scrollToBottom,
  escapeHtml,
  getCurrentTime,
  currentScene
})

const { loadSessionHistory, resetChat, loadingHistory } = useChatSession({
  messages,
  hasStarted,
  userInput,
  uploadedFiles,
  currentSessionId,
  currentScene,
  escapeHtml
})

// 侧边栏状态
const showSidebar = ref(true)
const sessions = ref<ChatSessionItem[]>([])
const activeSessionId = ref<number | null>(null)
const sessionsLoading = ref(false)

function sceneLabel(scene: string) {
  const map: Record<string, string> = {
    default: '综合',
    resume: '简历诊断',
    interview: '面试模拟',
    career: '职业规划',
  }
  return map[scene] || '综合'
}

function formatSessionTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function fetchSessions() {
  sessionsLoading.value = true
  try {
    const res = await listHistory() as any
    sessions.value = (res.data || res.rows || []) as ChatSessionItem[]
  } catch (e) {
    console.error('获取会话列表失败', e)
  } finally {
    sessionsLoading.value = false
  }
}

function selectSession(id: number) {
  activeSessionId.value = id
  loadSessionHistory(String(id))
  hasResume.value = !!sessionStorage.getItem('session_resume_' + id)
}

async function deleteSession(id: number) {
  try {
    await delSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (activeSessionId.value === id) {
      resetChat()
      activeSessionId.value = null
    }
  } catch (e) {
    console.error('删除会话失败', e)
  }
}

function startNewChat() {
  resetChat()
  activeSessionId.value = null
  hasResume.value = false
  // 清除 URL 中的 sessionId，避免 route watch 重新加载旧会话
  router.replace({ query: { ...route.query, sessionId: undefined } })
}

// 同步侧边栏选中状态
watch(currentSessionId, (newId) => {
  activeSessionId.value = newId ? Number(newId) : null
})

onMounted(async () => {
  const sessionId = route.query.sessionId as string | undefined
  if (sessionId && sessionId !== 'undefined' && sessionId !== '') {
    await loadSessionHistory(sessionId)
    hasResume.value = !!sessionStorage.getItem('session_resume_' + sessionId)
    nextTick(() => scrollToBottomDelayed())
  }
  fetchSessions()
  startLogoAnimation()
  window.addEventListener('use-file-from-space', handleFileFromSpace)
})

onUnmounted(() => {
  stopSse()
  window.removeEventListener('use-file-from-space', handleFileFromSpace)
})

watch(() => route.query.sessionId, async (newId) => {
  if (newId && newId !== 'undefined' && newId !== '') {
    resetChat()
    await loadSessionHistory(newId as string)
    // 延迟一帧检查 resume 绑定并滚动到底部
    nextTick(() => {
      hasResume.value = !!sessionStorage.getItem('session_resume_' + newId)
      scrollToBottomDelayed()
    })
  } else {
    resetChat()
    hasResume.value = false
  }
})

function handleEnter(e: KeyboardEvent) {
  if (!e.shiftKey) {
    sendMessage()
  } else {
    userInput.value += '\n'
  }
}
</script>

<style scoped lang="scss">
// 水墨黑主题色
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #5a5a5a;
$paper-white: #ffffff;
$paper-cream: #f8f8f6;
$paper-gray: #f5f5f5;
$accent-red: #8b1a1a;

.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $paper-white;
}

/* ---- 加载中 ---- */
.loading-history {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: $ink-pale;
  font-size: 14px;

  .loading-spin {
    font-size: 28px;
    animation: loading-rotate 1s linear infinite;
  }
}

@keyframes loading-rotate {
  to { transform: rotate(360deg); }
}

/* ---- 空状态 ---- */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  padding-top: 80px;

  .logo {
    position: relative;
    margin-bottom: 12px;

    .logo-text {
      font-size: 72px;
      font-weight: 700;
      color: $ink-black;
      font-family: 'Noto Serif SC', 'STSong', serif;
      letter-spacing: 16px;
      position: relative;
      z-index: 2;
    }

    .logo-ink {
      position: absolute;
      bottom: 8px;
      left: 0;
      right: 0;
      height: 12px;
      background: linear-gradient(90deg, transparent, rgba(139, 26, 26, 0.4), transparent);
      filter: blur(6px);
      z-index: 1;
    }
  }

  .logo-subtitle {
    font-size: 14px;
    color: $ink-pale;
    font-family: 'Noto Serif SC', 'STSong', serif;
    letter-spacing: 4px;
    margin-bottom: 48px;
  }

  /* 简历+ Logo */
  .logo-lishizhen {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    margin-bottom: 12px;
    position: relative;

    .logo-char {
      font-size: 64px;
      font-weight: 700;
      color: $ink-black;
      font-family: 'Noto Serif SC', 'STSong', serif;
      line-height: 1;
      opacity: 0;
      transform: translateY(-20px);
      transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);

      &.typing {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .logo-char-last {
      color: $accent-red;

      &.typing {
        opacity: 1;
        transform: translateY(0);
      }
    }
  }

  .input-wrapper {
    width: 100%;
    max-width: 800px;
  }
}

/* ---- 场景选择器（输入框左上角） ---- */
.scene-selector {
  position: absolute;
  top: 8px;
  left: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 2;

  .scene-trigger {
    display: flex;
    align-items: center;
    gap: 3px;
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

    .scene-arrow {
      font-size: 10px;
      transition: transform 0.2s ease;
    }

    &:hover {
      border-color: $accent-red;
      color: $accent-red;
    }
  }

  .scene-menu {
    position: absolute;
    top: 100%;
    left: 0;
    margin-top: 4px;
    background: $paper-white;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    padding: 4px;
    min-width: 130px;
    z-index: 10;

    .scene-menu-item {
      display: block;
      width: 100%;
      padding: 6px 12px;
      border: none;
      border-radius: 4px;
      background: transparent;
      color: $ink-mid;
      font-size: 13px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      cursor: pointer;
      text-align: left;
      transition: all 0.15s ease;

      &:hover {
        background: rgba(196, 92, 72, 0.08);
        color: $accent-red;
      }

      &.active {
        color: $accent-red;
        font-weight: 600;
      }
    }
  }

  .resume-pill {
    font-size: 12px;
    line-height: 1;
    cursor: default;
    opacity: 0.7;
  }
}

/* ---- 输入框样式 ---- */
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

    &::placeholder {
      color: $ink-pale;
    }
  }

  .uploaded-files {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 12px 20px 0;
    border-bottom: 1px solid rgba(0, 0, 0, 0.04);

    .file-tag {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background: rgba(10, 10, 10, 0.05);
      border-radius: 8px;
      font-size: 13px;
      color: $ink-black;
      transition: all 0.2s ease;

      &:hover {
        background: rgba(10, 10, 10, 0.08);
      }

      .file-name {
        max-width: 150px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .remove-btn {
        cursor: pointer;
        color: $ink-pale;
        font-size: 14px;
        transition: color 0.2s ease;

        &:hover {
          color: #f56c6c;
        }
      }
    }
  }

  .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 20px 16px;
    border-top: 1px solid rgba(0, 0, 0, 0.04);

    .left-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .file-count {
        font-size: 13px;
        color: $ink-pale;
        padding: 4px 10px;
        background: rgba(10, 10, 10, 0.04);
        border-radius: 6px;
      }
    }

    .right-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .action-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      background: transparent;
      color: $ink-pale;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        border-color: rgba(0, 0, 0, 0.15);
        color: $ink-mid;
        background: rgba(0, 0, 0, 0.02);
      }

      .el-icon {
        font-size: 18px;
      }
    }

    .send-btn {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      border: none;
      background: $ink-black;
      color: $paper-white;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover:not(:disabled) {
        background: $ink-deep;
        transform: scale(1.05);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
      }

      &:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .el-icon {
        font-size: 18px;
      }
    }
  }
}

/* ---- 对话状态 ---- */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 900px;
  width: 100%;
  margin: 0 auto;
  padding: 80px 20px 0;
  overflow: hidden;

  .message-list {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 20px 0;

    .history-empty-hint {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 60px 20px;
      color: #999;
      font-size: 14px;
    }

    .message-item {
      display: flex;
      gap: 12px;
      padding: 16px 0;
      animation: message-in 0.3s ease;

      &.user {
        flex-direction: row-reverse;

        .message-content {
          align-items: flex-end;
        }

        .message-bubble {
          background: $ink-black;
          color: $paper-white;
          border-radius: 16px 16px 4px 16px;
        }
      }

      &.ai {
        .message-bubble {
          background: $paper-gray;
          color: $ink-black;
          border-radius: 16px 16px 16px 4px;
        }
      }

      .message-avatar {
        flex-shrink: 0;

        .avatar {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 16px;

          &.ai-avatar {
            background: linear-gradient(135deg, $ink-deep 0%, $ink-black 100%);
            color: $paper-white;
            font-family: 'Noto Serif SC', 'STSong', serif;
            font-weight: 600;
          }

          /* 简历+ 头像样式 */
          &.lishizhen-avatar {
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, #0E3265 0%, #0A1E3D 100%);
            font-family: 'Noto Serif SC', 'STSong', serif;
            font-weight: 700;

            .avatar-char {
              font-size: 18px;
              color: #FAF8F5;
            }
          }

          &.user-avatar {
            background: linear-gradient(135deg, $ink-light 0%, $ink-mid 100%);
            color: $paper-white;
          }
        }
      }

      .message-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 4px;
        max-width: calc(100% - 60px);

        .message-bubble {
          padding: 12px 16px;
          font-size: 15px;
          line-height: 1.6;
          word-wrap: break-word;
        }

        .message-time {
          font-size: 12px;
          color: $ink-pale;
        }
      }
    }

    .thinking {
      .thinking-bubble {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px 20px;
        background: $paper-gray;
        border-radius: 16px 16px 16px 4px;

        .thinking-dots {
          display: flex;
          gap: 4px;

          span {
            width: 8px;
            height: 8px;
            background: $ink-pale;
            border-radius: 50%;
            animation: thinking-bounce 1.4s infinite ease-in-out;

            &:nth-child(2) { animation-delay: 0.2s; }
            &:nth-child(3) { animation-delay: 0.4s; }
          }
        }

        .thinking-text {
          font-size: 14px;
          color: $ink-pale;
          font-family: 'Noto Serif SC', 'STSong', serif;
        }
      }
    }
  }

  .bottom-input {
    padding: 20px 0;
  }
}

@keyframes message-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes thinking-bounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ===== 侧边栏布局 ===== */
.chat-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
}

/* ===== 侧边栏 ===== */
.chat-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: $ink-deep;
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.04);

  &.collapsed {
    width: 0;
    border-right: none;
  }
}

.sidebar-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 8px 12px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.08);
    border-radius: 2px;
  }
}

/* Loading skeleton */
.sidebar-loading {
  padding: 8px;
}

.sidebar-skeleton {
  padding: 12px;
  margin-bottom: 8px;

  .skeleton-line {
    height: 12px;
    background: rgba(255, 255, 255, 0.06);
    border-radius: 4px;
    margin-bottom: 8px;
    animation: skeleton-pulse 1.5s ease-in-out infinite;

    &.w-60 { width: 60%; }
    &.w-30 { width: 30%; }
  }
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

/* 空列表 */
.sidebar-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
  font-family: 'Noto Serif SC', 'STSong', serif;
}

/* 会话列表 */
.session-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.06);

    .session-del {
      opacity: 1;
    }
  }

  &.active {
    background: rgba(255, 255, 255, 0.1);
  }

  .session-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  .session-title {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.85);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 1.3;
  }

  .session-meta {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.3);
  }

  .session-scene {
    color: rgba(255, 255, 255, 0.35);
  }

  .session-dot {
    opacity: 0.4;
  }

  .session-del {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    border-radius: 4px;
    background: transparent;
    color: rgba(255, 255, 255, 0.25);
    cursor: pointer;
    opacity: 0;
    transition: all 0.15s ease;
    padding: 0;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: #f56c6c;
    }

    .el-icon {
      font-size: 14px;
    }
  }
}

/* 收起按钮 */
.sidebar-toggle {
  position: absolute;
  left: 280px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  border: none;
  border-radius: 0 8px 8px 0;
  background: $ink-deep;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 5;

  &:hover {
    background: lighten($ink-deep, 5%);
    color: rgba(255, 255, 255, 0.7);
  }

  .toggle-arrow {
    font-size: 10px;
    line-height: 1;
  }
}

.chat-sidebar.collapsed + .sidebar-toggle {
  left: 0;
}
</style>
