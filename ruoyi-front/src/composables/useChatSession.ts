/**
 * 会话历史管理 composable
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSessionDetail } from '@/api/chat'

export function useChatSession(options) {
  const { messages, hasStarted, userInput, uploadedFiles, currentSessionId, currentScene, escapeHtml } = options

  const loadingHistory = ref(false)

  function resetChat() {
    currentSessionId.value = null
    messages.value = []
    hasStarted.value = false
    userInput.value = ''
    uploadedFiles.value = []
    if (currentScene) currentScene.value = 'default'
  }

  async function loadSessionHistory(sessionId) {
    // 加载前重置状态，避免显示旧会话
    messages.value = []
    hasStarted.value = false
    loadingHistory.value = true
    try {
      const res = await getSessionDetail(sessionId) as any
      let session = res.data
      if (res.data && res.data.data) {
        session = res.data.data
      }
      if (session) {
        currentSessionId.value = sessionId
        hasStarted.value = true
        // 同步场景
        if (currentScene && session.scene) {
          currentScene.value = session.scene
        }
        // 同步 resumeId 到 sessionStorage
        if (session.resumeId) {
          sessionStorage.setItem('session_resume_' + sessionId, session.resumeId)
        } else {
          sessionStorage.removeItem('session_resume_' + sessionId)
        }
        // 加载消息（可能为空）
        messages.value = (session.messages || []).map(msg => ({
          role: msg.role === 1 ? 'user' : 'ai',
          content: escapeHtml(msg.content),
          time: formatTime(msg.createTime)
        }))
      } else {
        console.warn('会话数据为空:', res)
      }
    } catch (e) {
      console.error('加载会话历史失败', e)
      ElMessage.error('加载历史记录失败')
      // 失败时确保回到初始空状态
      hasStarted.value = false
      messages.value = []
    } finally {
      loadingHistory.value = false
    }
  }

  return {
    loadSessionHistory,
    resetChat,
    loadingHistory
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}
