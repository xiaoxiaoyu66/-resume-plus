import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import { useChatSession } from '../useChatSession'

vi.mock('@/api/chat', () => ({
  getSessionDetail: vi.fn(),
}))

describe('useChatSession', () => {
  let messages, hasStarted, userInput, uploadedFiles, currentSessionId, currentScene
  let session

  beforeEach(() => {
    vi.clearAllMocks()
    messages = ref([])
    hasStarted = ref(false)
    userInput = ref('')
    uploadedFiles = ref([])
    currentSessionId = ref(null)
    currentScene = ref('default')
    session = useChatSession({
      messages,
      hasStarted,
      userInput,
      uploadedFiles,
      currentSessionId,
      currentScene,
      escapeHtml: (s) => s || ''
    })
  })

  describe('resetChat', () => {
    it('should reset all state to defaults', () => {
      messages.value = [{ role: 'user', content: 'hi', time: '12:00' }]
      hasStarted.value = true
      userInput.value = 'some text'
      uploadedFiles.value = [{ fileName: 'test.pdf' }]
      currentSessionId.value = '42'
      currentScene.value = 'interview'

      session.resetChat()

      expect(messages.value).toEqual([])
      expect(hasStarted.value).toBe(false)
      expect(userInput.value).toBe('')
      expect(uploadedFiles.value).toEqual([])
      expect(currentSessionId.value).toBeNull()
      expect(currentScene.value).toBe('default')
    })
  })

  describe('loadSessionHistory', () => {
    const mockSession = {
      messages: [
        { role: 1, content: 'Hello', createTime: '2026-05-08T10:00:00' },
        { role: 0, content: 'Hi there', createTime: '2026-05-08T10:00:05' },
      ],
      scene: 'interview',
    }

    it('should parse and load messages on success', async () => {
      const { getSessionDetail } = await import('@/api/chat')
      getSessionDetail.mockResolvedValue({ data: mockSession })

      await session.loadSessionHistory('42')

      expect(messages.value.length).toBe(2)
      expect(messages.value[0].role).toBe('user')
      expect(messages.value[0].content).toBe('Hello')
      expect(messages.value[1].role).toBe('ai')
      expect(messages.value[1].content).toBe('Hi there')
      expect(hasStarted.value).toBe(true)
      expect(currentSessionId.value).toBe('42')
    })

    it('should sync scene from session', async () => {
      const { getSessionDetail } = await import('@/api/chat')
      getSessionDetail.mockResolvedValue({ data: mockSession })

      await session.loadSessionHistory('42')
      expect(currentScene.value).toBe('interview')
    })

    it('should handle nested response data', async () => {
      const { getSessionDetail } = await import('@/api/chat')
      getSessionDetail.mockResolvedValue({ data: { data: mockSession } })

      await session.loadSessionHistory('42')
      expect(messages.value.length).toBe(2)
    })

    it('should bind resumeId to sessionStorage', async () => {
      const { getSessionDetail } = await import('@/api/chat')
      getSessionDetail.mockResolvedValue({
        data: { ...mockSession, resumeId: '123' }
      })

      await session.loadSessionHistory('42')
      expect(sessionStorage.getItem('session_resume_42')).toBe('123')
    })

    it('should remove resumeId from sessionStorage when not present', async () => {
      sessionStorage.setItem('session_resume_42', 'old-id')
      const { getSessionDetail } = await import('@/api/chat')
      getSessionDetail.mockResolvedValue({ data: mockSession })

      await session.loadSessionHistory('42')
      expect(sessionStorage.getItem('session_resume_42')).toBeNull()
    })

    it('should handle API error gracefully', async () => {
      const { getSessionDetail } = await import('@/api/chat')
      getSessionDetail.mockRejectedValue(new Error('Network error'))

      await session.loadSessionHistory('999')

      expect(messages.value).toEqual([])
      expect(hasStarted.value).toBe(false)
    })

    it('should set started when session data exists even without messages', async () => {
      const { getSessionDetail } = await import('@/api/chat')
      getSessionDetail.mockResolvedValue({
        data: { messages: [], scene: 'default' }
      })

      await session.loadSessionHistory('42')

      expect(hasStarted.value).toBe(true)
      expect(currentSessionId.value).toBe('42')
      expect(messages.value).toEqual([])
    })
  })
})
