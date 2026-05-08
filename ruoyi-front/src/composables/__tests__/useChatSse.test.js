import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { useChatSse } from '../useChatSse'

vi.mock('@/api/file', () => ({
  persistTempFiles: vi.fn(),
}))

vi.mock('@/utils/sse-client', () => ({
  default: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    info: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn(),
  },
}))

// helper to create a mock SSE client
async function createMockSseClient() {
  const { default: SseChatClient } = await import('@/utils/sse-client')
  const mockStreamChat = vi.fn()
  const mockClose = vi.fn()
  const client = { streamChat: mockStreamChat, close: mockClose }
  SseChatClient.mockImplementation(function() { return client })
  return { client, mockStreamChat, mockClose }
}

describe('useChatSse', () => {
  let userInput, messages, isThinking, hasStarted, uploadedFiles, currentSessionId, scrollToBottom, currentScene
  let chat

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()

    userInput = ref('')
    messages = ref([])
    isThinking = ref(false)
    hasStarted = ref(false)
    uploadedFiles = ref([])
    currentSessionId = ref(null)
    currentScene = ref('default')
    scrollToBottom = vi.fn()

    chat = useChatSse({
      userInput,
      messages,
      isThinking,
      hasStarted,
      uploadedFiles,
      currentSessionId,
      scrollToBottom,
      escapeHtml: (s) => s || '',
      getCurrentTime: () => '12:00',
      currentScene,
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('sendMessage', () => {
    it('should add user message and start SSE', async () => {
      const { mockStreamChat } = await createMockSseClient()
      mockStreamChat.mockResolvedValue({ success: true, content: 'Hello back' })

      userInput.value = 'Hello'
      chat.sendMessage()

      await nextTick()

      expect(hasStarted.value).toBe(true)
      expect(messages.value.length).toBe(1)
      expect(messages.value[0].role).toBe('user')
      expect(messages.value[0].content).toBe('Hello')
      expect(isThinking.value).toBe(true)
      expect(userInput.value).toBe('')
    })

    it('should do nothing with empty input and no files', () => {
      chat.sendMessage()
      expect(messages.value.length).toBe(0)
      expect(hasStarted.value).toBe(false)
    })

    it('should send file placeholder when content is empty but files exist', async () => {
      const { mockStreamChat } = await createMockSseClient()
      mockStreamChat.mockResolvedValue({ success: true })

      uploadedFiles.value = [{ fileName: 'test.pdf', isTemp: false }]
      userInput.value = ''
      chat.sendMessage()

      await nextTick()

      expect(messages.value.length).toBe(1)
      expect(messages.value[0].content).toContain('已上传')
    })

    it('should cancel current reply when already thinking', async () => {
      const { ElMessage } = await import('element-plus')

      isThinking.value = true
      chat.sendMessage()

      expect(ElMessage.info).toHaveBeenCalledWith('已取消本次回答')
      expect(isThinking.value).toBe(false)
    })

    it('should handle SSE errors gracefully', async () => {
      const { mockStreamChat } = await createMockSseClient()
      mockStreamChat.mockRejectedValue(new Error('SSE failed'))

      userInput.value = 'Hi'
      const promise = chat.sendMessage()

      await promise
      await nextTick()

      expect(isThinking.value).toBe(false)
      expect(messages.value.length).toBe(2)
      expect(messages.value[1].role).toBe('ai')
      expect(messages.value[1].content).toBe('抱歉，我遇到了一些问题，请稍后重试。')
    })
  })

  describe('SSE token streaming', () => {
    it('should append received tokens to the last ai message', async () => {
      const { default: SseChatClient } = await import('@/utils/sse-client')

      SseChatClient.mockImplementation(function({ onToken }) {
        return {
          streamChat: async () => {
            onToken('Hello', 'Hello')
            onToken(' World', 'Hello World')
            return { success: true, content: 'Hello World' }
          },
          close: vi.fn(),
        }
      })

      userInput.value = 'Hi'
      await chat.sendMessage()
      await nextTick()

      const aiMessages = messages.value.filter((m) => m.role === 'ai')
      expect(aiMessages.length).toBe(1)
      expect(aiMessages[0].content).toBe('Hello World')
    })

    it('should clear thinking state on first token', async () => {
      const { default: SseChatClient } = await import('@/utils/sse-client')

      SseChatClient.mockImplementation(function({ onToken }) {
        return {
          streamChat: async () => {
            onToken('Hi', 'Hi')
            return { success: true, content: 'Hi' }
          },
          close: vi.fn(),
        }
      })

      isThinking.value = true
      userInput.value = 'test'
      await chat.sendMessage()
      await nextTick()

      expect(isThinking.value).toBe(false)
    })

    it('should update sessionId on complete', async () => {
      const { default: SseChatClient } = await import('@/utils/sse-client')

      SseChatClient.mockImplementation(function({ onComplete }) {
        return {
          streamChat: async () => {
            onComplete('Done', { sessionId: 'new-session-42' })
            return { success: true }
          },
          close: vi.fn(),
        }
      })

      userInput.value = 'test'
      await chat.sendMessage()
      await nextTick()

      expect(currentSessionId.value).toBe('new-session-42')
    })

    it('should clear uploaded files on complete', async () => {
      const { default: SseChatClient } = await import('@/utils/sse-client')

      SseChatClient.mockImplementation(function({ onComplete }) {
        return {
          streamChat: async () => {
            onComplete('Done', {})
            return { success: true }
          },
          close: vi.fn(),
        }
      })

      uploadedFiles.value = [{ fileName: 'test.pdf', isTemp: false }]
      userInput.value = 'test'
      await chat.sendMessage()
      await nextTick()

      expect(uploadedFiles.value).toEqual([])
    })
  })

  describe('file persistence', () => {
    it('should persist temp files before sending', async () => {
      const { persistTempFiles } = await import('@/api/file')
      const { mockStreamChat } = await createMockSseClient()
      mockStreamChat.mockResolvedValue({ success: true })

      persistTempFiles.mockResolvedValue({
        code: 200,
        data: [{ success: true, fileName: 'persisted-1.pdf', fileUrl: '/files/persisted-1.pdf' }],
      })

      uploadedFiles.value = [{ fileName: 'temp-1', originalName: 'doc.pdf', isTemp: true }]
      userInput.value = 'analyze'

      await chat.sendMessage()
      await nextTick()

      expect(persistTempFiles).toHaveBeenCalledWith(['temp-1'])
    })

    it('should proceed with original files when persistence fails', async () => {
      const { persistTempFiles } = await import('@/api/file')
      const { mockStreamChat } = await createMockSseClient()
      mockStreamChat.mockResolvedValue({ success: true })

      persistTempFiles.mockResolvedValue({ code: 500, msg: 'Storage error' })

      uploadedFiles.value = [{ fileName: 'temp-1', originalName: 'doc.pdf', isTemp: true }]
      userInput.value = 'analyze'

      await chat.sendMessage()
      await nextTick()

      expect(mockStreamChat).toHaveBeenCalled()
      const callArg = mockStreamChat.mock.calls[0][0]
      expect(callArg.fileNames).toBeDefined()
    })
  })

  describe('stopSse', () => {
    it('should close the SSE client when called', async () => {
      const { mockClose, mockStreamChat } = await createMockSseClient()
      mockStreamChat.mockResolvedValue({ success: true })

      userInput.value = 'test'
      chat.sendMessage()
      await nextTick()

      chat.stopSse()
      expect(mockClose).toHaveBeenCalled()
    })

    it('should not throw when no active SSE client', () => {
      expect(() => chat.stopSse()).not.toThrow()
    })
  })

  describe('first token timeout warning', () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    it('should show warning when first token takes too long', async () => {
      const { ElMessage } = await import('element-plus')
      const { default: SseChatClient } = await import('@/utils/sse-client')

      SseChatClient.mockImplementation(function({ onToken, onComplete }) {
        return {
          streamChat: async () => {
            await vi.advanceTimersByTimeAsync(11000)
            onToken('Late', 'Late')
            onComplete('Late', {})
            return { success: true }
          },
          close: vi.fn(),
        }
      })

      userInput.value = 'test'
      chat.sendMessage()

      await vi.advanceTimersByTimeAsync(500)

      expect(ElMessage.warning).toHaveBeenCalledWith(expect.stringContaining('响应较慢'))
    })

    it('should not show warning when first token arrives quickly', async () => {
      const { ElMessage } = await import('element-plus')
      const { default: SseChatClient } = await import('@/utils/sse-client')

      SseChatClient.mockImplementation(function({ onToken, onComplete }) {
        return {
          streamChat: async () => {
            onToken('Fast', 'Fast')
            onComplete('Fast', {})
            return { success: true }
          },
          close: vi.fn(),
        }
      })

      userInput.value = 'test'
      await chat.sendMessage()
      await vi.advanceTimersByTimeAsync(11000)

      expect(ElMessage.warning).not.toHaveBeenCalled()
    })
  })
})
