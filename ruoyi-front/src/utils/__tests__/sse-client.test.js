import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'

// Mock auth module
vi.mock('@/utils/auth', () => ({
  getToken: () => 'mock-token'
}))

// Import after mocking
import SseChatClient from '@/utils/sse-client'

function createMockResponse(chunks) {
  let index = 0
  const encoder = new TextEncoder()
  return {
    ok: true,
    body: {
      getReader: () => ({
        read: () => {
          if (index < chunks.length) {
            return Promise.resolve({ done: false, value: encoder.encode(chunks[index++]) })
          }
          return Promise.resolve({ done: true, value: undefined })
        },
        cancel: vi.fn()
      })
    }
  }
}

function createErrorResponse(status) {
  return { ok: false, status }
}

describe('SseChatClient', () => {
  let client
  let onToken, onComplete, onError, onStart

  beforeEach(() => {
    onToken = vi.fn()
    onComplete = vi.fn()
    onError = vi.fn()
    onStart = vi.fn()
    client = new SseChatClient({
      baseUrl: '',
      onToken,
      onComplete,
      onError,
      onStart
    })
    globalThis.fetch = vi.fn()
  })

  afterEach(() => {
    delete globalThis.fetch
  })

  it('should parse token events and build full content', async () => {
    const sseData = [
      'event: token\ndata: {"token":"Hello"}\n\n',
      'event: token\ndata: {"token":" World"}\n\n',
      'event: complete\ndata: {"token":"","status":"completed"}\n\n'
    ]
    globalThis.fetch.mockResolvedValue(createMockResponse(sseData))

    const result = await client.streamChat({
      userId: 1,
      message: 'Hi',
      scene: 'default'
    })

    expect(onToken).toHaveBeenCalledTimes(2)
    expect(onToken).toHaveBeenNthCalledWith(1, 'Hello', 'Hello')
    expect(onToken).toHaveBeenNthCalledWith(2, ' World', 'Hello World')
    expect(onComplete).toHaveBeenCalledTimes(1)
    expect(result.success).toBe(true)
    expect(result.content).toBe('Hello World')
  })

  it('should handle [DONE] termination signal', async () => {
    const sseData = [
      'event: token\ndata: {"token":"Done"}\n\n',
      'data: [DONE]\n\n'
    ]
    globalThis.fetch.mockResolvedValue(createMockResponse(sseData))

    const result = await client.streamChat({
      userId: 1,
      message: 'test',
      scene: 'default'
    })

    expect(onToken).toHaveBeenCalledWith('Done', 'Done')
    expect(onComplete).toHaveBeenCalled()
    expect(result.success).toBe(true)
    expect(result.content).toBe('Done')
  })

  it('should call onError on error events', async () => {
    const sseData = [
      'event: error\ndata: {"message":"API rate limit exceeded"}\n\n'
    ]
    globalThis.fetch.mockResolvedValue(createMockResponse(sseData))

    await expect(client.streamChat({
      userId: 1,
      message: 'test',
      scene: 'default'
    })).rejects.toThrow('API rate limit exceeded')

    expect(onError).toHaveBeenCalledWith('API rate limit exceeded')
    expect(onComplete).not.toHaveBeenCalled()
  })

  it('should handle HTTP errors', async () => {
    globalThis.fetch.mockResolvedValue(createErrorResponse(401))

    await expect(client.streamChat({
      userId: 1,
      message: 'test',
      scene: 'default'
    })).rejects.toThrow('HTTP error! status: 401')

    expect(onError).toHaveBeenCalled()
  })

  it('should call onStart when start event is received', async () => {
    const sseData = [
      'event: start\ndata: {"sessionId":"abc-123"}\n\n',
      'event: token\ndata: {"token":"Hello"}\n\n',
      'event: complete\ndata: {}\n\n'
    ]
    globalThis.fetch.mockResolvedValue(createMockResponse(sseData))

    await client.streamChat({
      userId: 1,
      message: 'test',
      scene: 'default'
    })

    expect(onStart).toHaveBeenCalledWith(expect.objectContaining({ sessionId: 'abc-123' }))
  })

  it('should abort the fetch signal and clean up when close() is called', async () => {
    globalThis.fetch.mockImplementation((url, opts) => {
      return new Promise((resolve) => {
        // Register the abort handler so the promise doesn't hang
        const onAbort = () => resolve({ ok: false })
        if (opts.signal.aborted) {
          resolve({ ok: false })
          return
        }
        opts.signal.addEventListener('abort', onAbort)
      })
    })

    client.streamChat({
      userId: 1,
      message: 'test',
      scene: 'default'
    }).catch(() => {})

    expect(client.abortController).not.toBeNull()
    expect(client.abortController.signal.aborted).toBe(false)

    client.close()

    expect(client.abortController).toBeNull()
  })

  it('should pass fileNames as query params', async () => {
    globalThis.fetch.mockResolvedValue(createMockResponse([]))

    await client.streamChat({
      userId: 1,
      message: 'analyze',
      fileNames: ['file1.pdf', 'file2.docx'],
      scene: 'default'
    }).catch(() => {}) // ignore the empty response

    const callUrl = globalThis.fetch.mock.calls[0][0]
    expect(callUrl).toContain('fileNames=file1.pdf')
    expect(callUrl).toContain('fileNames=file2.docx')
  })

  it('should pass sessionId as query param when provided', async () => {
    globalThis.fetch.mockResolvedValue(createMockResponse([]))

    await client.streamChat({
      userId: 1,
      sessionId: 42,
      message: 'continue',
      scene: 'default'
    }).catch(() => {})

    const callUrl = globalThis.fetch.mock.calls[0][0]
    expect(callUrl).toContain('sessionId=42')
  })

  it('should construct correct URL', async () => {
    globalThis.fetch.mockResolvedValue(createMockResponse([]))

    await client.streamChat({
      userId: 1,
      message: 'hello',
      scene: 'default'
    }).catch(() => {})

    const callUrl = globalThis.fetch.mock.calls[0][0]
    expect(callUrl).toContain('/api/ai/chat/stream')
    expect(callUrl).toContain('userId=1')
    expect(callUrl).toContain('message=hello')
  })

  it('should include auth header', async () => {
    globalThis.fetch.mockResolvedValue(createMockResponse([]))

    await client.streamChat({
      userId: 1,
      message: 'test',
      scene: 'default'
    }).catch(() => {})

    const headers = globalThis.fetch.mock.calls[0][1].headers
    expect(headers['Authorization']).toBe('Bearer mock-token')
  })

  it('should handle chunk boundary splitting', async () => {
    // Simulate SSE data split across chunk boundaries
    const sseData = [
      'event: token\nda',
      'ta: {"token":"Hello"}\n\n'
    ]
    globalThis.fetch.mockResolvedValue(createMockResponse(sseData))

    const result = await client.streamChat({
      userId: 1,
      message: 'test',
      scene: 'default'
    })

    expect(onToken).toHaveBeenCalledWith('Hello', 'Hello')
    expect(result.content).toBe('Hello')
  })

  it('should handle unrecognized data as plain text', async () => {
    const sseData = [
      'data: plain text line\n\n'
    ]
    globalThis.fetch.mockResolvedValue(createMockResponse(sseData))

    const result = await client.streamChat({
      userId: 1,
      message: 'test',
      scene: 'default'
    })

    expect(onToken).toHaveBeenCalledWith('plain text line', 'plain text line')
  })
})
