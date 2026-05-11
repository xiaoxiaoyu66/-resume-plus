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

    <div class="chat-layout">
      <!-- 侧边栏 -->
      <ChatSidebar
        :sessions="sessions"
        :sessions-loading="sessionsLoading"
        :active-session-id="activeSessionId"
        :show-sidebar="showSidebar"
        @select-session="selectSession"
        @delete-session="deleteSession"
        @update:show-sidebar="showSidebar = $event"
      />

      <!-- 主区域 -->
      <main class="chat-main">
        <!-- 空状态 -->
        <div v-if="!hasStarted && !loadingHistory" class="empty-state">
          <div class="logo-wrapper">
            <span class="logo-char" :class="{ 'typing': showChar1 }">简</span>
            <span class="logo-char" :class="{ 'typing': showChar2 }">历</span>
            <span class="logo-char logo-char-last" :class="{ 'typing': showChar3 }">+</span>
          </div>
          <ChatInputBar
            v-model="userInput"
            v-model:scene-menu-open="sceneMenuOpen"
            :uploaded-files="uploadedFiles"
            :current-scene="currentScene"
            :is-uploading="isUploading"
            :is-thinking="isThinking"
            :has-resume="hasResume"
            :rows="3"
            placeholder="输入消息与简历+对话..."
            input-box-class="input-box-empty"
            @send="sendMessage"
            @cancel="cancelCurrentReply"
            @remove-file="removeFile"
            @switch-scene="switchScene"
            @file-selected="onFileSelected"
          />
        </div>

        <!-- 对话状态 -->
        <div v-else class="chat-container">
          <ChatMessageList
            ref="messageListRef"
            :messages="messages"
            :is-thinking="isThinking"
            :loading-history="loadingHistory"
            :loading-timed-out="loadingTimedOut"
            @retry="retryLoadHistory"
          />
          <div class="bottom-input">
            <ChatInputBar
              v-model="userInput"
              v-model:scene-menu-open="sceneMenuOpen"
              :uploaded-files="uploadedFiles"
              :current-scene="currentScene"
              :is-uploading="isUploading"
              :is-thinking="isThinking"
              :has-resume="hasResume"
              :rows="2"
              placeholder="输入消息..."
              @send="sendMessage"
              @cancel="cancelCurrentReply"
              @remove-file="removeFile"
              @switch-scene="switchScene"
              @file-selected="onFileSelected"
            />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, watch, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FileUploadProgress from '@/components/FileUploadProgress.vue'
import ChatSidebar from '@/views/chat/ChatSidebar.vue'
import ChatMessageList from '@/views/chat/ChatMessageList.vue'
import ChatInputBar from '@/views/chat/ChatInputBar.vue'
import { useChatUpload } from '@/composables/useChatUpload'
import { useChatSse } from '@/composables/useChatSse'
import { useChatSession } from '@/composables/useChatSession'
import { escapeHtml, getCurrentTime } from '@/utils/format'
import { listHistory, delSession, updateSessionScene } from '@/api/chat'
import type { ChatMessage, ChatSessionItem, UploadedFile } from '@/types/chat'

const route = useRoute()
const router = useRouter()

// ====== 对话状态 ======
const userInput = ref('')
const messages = ref<ChatMessage[]>([])
const isThinking = ref(false)
const hasStarted = ref(false)
const currentSessionId = ref<string | null>(null)
const sceneMenuOpen = ref(false)

// ====== 场景 ======
const currentScene = ref('default')

// ====== 文件上传 ======
const uploadedFiles = ref<UploadedFile[]>([])
const isUploading = ref(false)

const upload = useChatUpload({
  hasStarted,
  uploadedFiles,
  fileInput: ref(null),
  fileInputBottom: ref(null),
  isUploading,
})

const {
  showUploadProgress,
  uploadingFileName,
  uploadingFileSize,
  uploadProgress,
  uploadSpeed,
  uploadedSize,
  remainingTime,
  handleFileFromSpace,
  cancelUpload,
  removeFile,
} = upload

// ChatInputBar 的 file-selected 事件 → 接入 composable 的文件上传逻辑
function onFileSelected(files: FileList) {
  ;(upload as any).handleFileSelect({ target: { files, value: '' } } as unknown as Event)
}

// ====== SSE 流式对话 ======
const messageListRef = ref<InstanceType<typeof ChatMessageList> | null>(null)

const scrollToBottom = () => messageListRef.value?.scrollToBottom()
const scrollToBottomDelayed = () => messageListRef.value?.scrollToBottomDelayed()

const { sendMessage, stopSse, cancelCurrentReply } = useChatSse({
  userInput,
  messages,
  isThinking,
  hasStarted,
  uploadedFiles,
  currentSessionId,
  scrollToBottom,
  escapeHtml,
  getCurrentTime,
  currentScene,
})

// ====== 会话管理 ======
const { loadSessionHistory, resetChat, loadingHistory } = useChatSession({
  messages,
  hasStarted,
  userInput,
  uploadedFiles,
  currentSessionId,
  currentScene,
  escapeHtml,
})

// ====== 场景切换 ======
function switchScene(scene: string) {
  currentScene.value = scene
  if (currentSessionId.value) {
    updateSessionScene(currentSessionId.value, scene).catch(() => {})
  }
}

// ====== 侧边栏 ======
const showSidebar = ref(true)
const sessions = ref<ChatSessionItem[]>([])
const activeSessionId = ref<number | null>(null)
const sessionsLoading = ref(false)

// 加载超时检测
const loadingTimedOut = ref(false)
let loadingTimer: ReturnType<typeof setTimeout> | null = null

function startLoadingTimer() {
  loadingTimedOut.value = false
  loadingTimer = setTimeout(() => { loadingTimedOut.value = true }, 15000)
}

function clearLoadingTimer() {
  if (loadingTimer) { clearTimeout(loadingTimer); loadingTimer = null }
  loadingTimedOut.value = false
}

async function fetchSessions() {
  sessionsLoading.value = true
  startLoadingTimer()
  try {
    const res = await listHistory() as any
    sessions.value = (res.data || res.rows || []) as ChatSessionItem[]
    clearLoadingTimer()
  } catch (e) {
    console.error('获取会话列表失败', e)
    clearLoadingTimer()
  } finally {
    sessionsLoading.value = false
  }
}

function retryLoadHistory() { fetchSessions() }

function selectSession(id: number) {
  activeSessionId.value = id
  loadSessionHistory(String(id))
  hasResume.value = !!sessionStorage.getItem('session_resume_' + id)
}

async function deleteSession(id: number) {
  try {
    await delSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (activeSessionId.value === id) { resetChat(); activeSessionId.value = null }
  } catch (e) {
    console.error('删除会话失败', e)
  }
}

// ====== 简历联动 ======
const hasResume = ref(false)

function startNewChat() {
  resetChat()
  activeSessionId.value = null
  hasResume.value = false
  router.replace({ query: {} })
}

// ====== Logo 动画 ======
const showChar1 = ref(false)
const showChar2 = ref(false)
const showChar3 = ref(false)

function startLogoAnimation() {
  showChar1.value = true
  setTimeout(() => { showChar2.value = true }, 300)
  setTimeout(() => { showChar3.value = true }, 600)
}

// ====== 路由参数处理 ======
let _handlingRoute = false

async function handleRouteQuery(query: Record<string, any>) {
  if (_handlingRoute) return
  _handlingRoute = true
  try {
    const sessionId = query.sessionId as string | undefined
    const scene = query.scene as string | undefined
    if (sessionId && sessionId !== 'undefined' && sessionId !== '') {
      resetChat()
      await loadSessionHistory(sessionId)
      nextTick(() => {
        hasResume.value = !!sessionStorage.getItem('session_resume_' + sessionId)
        scrollToBottomDelayed()
      })
    } else if (scene && ['interview-hr', 'interview-pro'].includes(scene)) {
      resetChat()
      uploadedFiles.value = []
      currentScene.value = scene
      userInput.value = '请根据我的简历开始面试'
      await nextTick()
      await sendMessage()
    } else {
      resetChat()
      hasResume.value = false
    }
  } finally {
    _handlingRoute = false
  }
  // 每次处理路由后刷新会话列表，确保侧边栏及时加载
  fetchSessions()
}

// ====== 同步 ======
watch(currentSessionId, (newId) => {
  activeSessionId.value = newId ? Number(newId) : null
})

watch(loadingHistory, (loading) => {
  if (loading) startLoadingTimer(); else clearLoadingTimer()
})

onMounted(async () => {
  await handleRouteQuery(route.query)
  startLogoAnimation()
  window.addEventListener('use-file-from-space', handleFileFromSpace)
})

onUnmounted(() => {
  stopSse()
  window.removeEventListener('use-file-from-space', handleFileFromSpace)
})

watch(() => route.query, async (newQuery) => {
  await handleRouteQuery(newQuery)
})
</script>

<style scoped lang="scss">
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-pale: #5a5a5a;
$paper-white: #ffffff;
$accent-red: #8b1a1a;

.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $paper-white;
}

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

/* ---- 空状态 ---- */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  padding-top: 80px;

  .logo-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    margin-bottom: 12px;

    .logo-char {
      font-size: 64px;
      font-weight: 700;
      color: $ink-black;
      font-family: 'Noto Serif SC', 'STSong', serif;
      line-height: 1;
      opacity: 0;
      transform: translateY(-20px);
      transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
      &.typing { opacity: 1; transform: translateY(0); }
    }
    .logo-char-last {
      color: $accent-red;
      &.typing { opacity: 1; transform: translateY(0); }
    }
  }

  :deep(.input-box-empty) {
    width: 100%;
    max-width: 800px;
  }
}

/* ---- 对话容器 ---- */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 900px;
  width: 100%;
  margin: 0 auto;
  padding: 80px 20px 0;
  overflow: hidden;

  .bottom-input {
    padding: 20px 0;
  }
}
</style>
